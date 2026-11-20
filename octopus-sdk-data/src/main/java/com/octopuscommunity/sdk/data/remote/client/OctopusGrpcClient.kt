package com.octopuscommunity.sdk.data.remote.client

import android.content.Context
import android.os.Build
import android.util.Log
import com.octopuscommunity.sdk.data.BuildConfig
import com.octopuscommunity.sdk.data.repository.UserRepositoryImpl
import com.octopuscommunity.sdk.domain.repository.LanguageRepository
import com.octopuscommunity.sdk.domain.repository.PreferenceRepository
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import com.octopuscommunity.sdk.domain.repository.UserRepository
import io.grpc.CallCredentials
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.CompositeCallCredentials
import io.grpc.ForwardingClientCall
import io.grpc.ForwardingClientCallListener
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.Metadata.ASCII_STRING_MARSHALLER
import io.grpc.Metadata.Key
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.android.AndroidChannelBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This class manages the gRPC services used by the Octopus SDK for messaging.
 * It handles channel creation, stub initialization, and token management.
 */
class OctopusGrpcClient internal constructor(
    private val appPackageName: () -> String,
    private val osVersion: () -> String,
    private val sdkIdentity: () -> SdkIdentity,
    apiKey: String,
    userRepositoryLazy: Lazy<UserRepository>,
    preferencesRepositoryLazy: Lazy<PreferenceRepository>,
    sessionRepositoryLazy: Lazy<SessionRepository>,
    languageRepositoryLazy: Lazy<LanguageRepository>,
    dispatcher: CoroutineDispatcher,
    private val logFailure: (String?, Status) -> Unit,
    channelFactory: (ClientInterceptor) -> ManagedChannel,
) {
    /** Creates a TLS channel with the SDK's request headers and token refresh handling. */
    constructor(
        context: Context,
        serverHost: String,
        serverPort: Int,
        apiKey: String,
        userRepositoryLazy: Lazy<UserRepository>,
        preferencesRepositoryLazy: Lazy<PreferenceRepository>,
        sessionRepositoryLazy: Lazy<SessionRepository>,
        languageRepositoryLazy: Lazy<LanguageRepository>,
        sdkIdentity: () -> SdkIdentity,
    ) : this(
        appPackageName = { context.packageName },
        osVersion = { "${Build.VERSION.SDK_INT}" },
        sdkIdentity = sdkIdentity,
        apiKey = apiKey,
        userRepositoryLazy = userRepositoryLazy,
        preferencesRepositoryLazy = preferencesRepositoryLazy,
        sessionRepositoryLazy = sessionRepositoryLazy,
        languageRepositoryLazy = languageRepositoryLazy,
        dispatcher = Dispatchers.IO,
        logFailure = { methodName, status ->
            if (BuildConfig.DEBUG && !status.isOk) {
                Log.e(
                    "OctopusGrpc",
                    "gRPC $methodName failed: ${status.code} — ${status.description}",
                    status.cause,
                )
            }
        },
        channelFactory = { interceptor ->
            AndroidChannelBuilder
                .forAddress(serverHost, serverPort)
                .context(context)
                .intercept(interceptor)
                .executor(Dispatchers.IO.asExecutor())
                .useTransportSecurity()
                .build()
        },
    )

    /**
     * The constructor this class had before [sdkIdentity] existed, kept so direct callers keep
     * compiling and linking. Its requests report the native SDK family and version.
     */
    @Deprecated(
        "Pass sdkIdentity, so requests report the SDK family and version the host integrates.",
        ReplaceWith(
            "OctopusGrpcClient(context, serverHost, serverPort, apiKey, userRepositoryLazy, " +
                "preferencesRepositoryLazy, sessionRepositoryLazy, languageRepositoryLazy) " +
                "{ SdkIdentity.NATIVE }",
        ),
    )
    constructor(
        context: Context,
        serverHost: String,
        serverPort: Int,
        apiKey: String,
        userRepositoryLazy: Lazy<UserRepository>,
        preferencesRepositoryLazy: Lazy<PreferenceRepository>,
        sessionRepositoryLazy: Lazy<SessionRepository>,
        languageRepositoryLazy: Lazy<LanguageRepository>,
    ) : this(
        context = context,
        serverHost = serverHost,
        serverPort = serverPort,
        apiKey = apiKey,
        userRepositoryLazy = userRepositoryLazy,
        preferencesRepositoryLazy = preferencesRepositoryLazy,
        sessionRepositoryLazy = sessionRepositoryLazy,
        languageRepositoryLazy = languageRepositoryLazy,
        sdkIdentity = { SdkIdentity.NATIVE },
    )

    private val stopped = AtomicBoolean()
    private val credentialsScope = CoroutineScope(SupervisorJob() + dispatcher)

    val userRepository by userRepositoryLazy
    val preferencesRepository by preferencesRepositoryLazy
    val sessionRepository by sessionRepositoryLazy
    val languageRepository by languageRepositoryLazy

    var hasAccessToCommunity: Boolean? = null

    /**
     * Called, off the call's thread, when the server answers `UNAUTHENTICATED` to a call sent
     * with a JWT; receives that JWT so the handler can tell the current session from a replaced one.
     */
    @Volatile
    internal var onSessionRejected: (suspend (rejectedJwt: String) -> Unit)? = null

    /**
     * Called, off the caller's thread, when the server reads the access of a session it ended; receives that
     * session's user id. Not its JWT: the answer that ends a session also renews its token, stored before the
     * answer is read.
     */
    @Volatile
    internal var onSessionEnded: (suspend (endedUserId: String) -> Unit)? = null

    /**
     * Whether calls to the server are blocked, while the community is closed. A blocked call is closed locally with
     * `FAILED_PRECONDITION`, before any credential is read, unless it carries [BYPASS_CALLS_GATE].
     */
    @Volatile
    var callsBlocked: Boolean = false

    private val communityClosedRefusalsFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * Emits each time the server refuses a call because the community is closed or temporarily disabled: a community
     * closed while the app runs first shows that way. The server refuses with the same `PERMISSION_DENIED` for other
     * reasons (a hidden group, an excluded user, a failing A/B score), and its description is a translated text for the
     * user: it names the reason in the `octopus-reason` trailer. Refusals arriving while a value waits for its collector
     * are merged into it.
     */
    val communityClosedRefusals: SharedFlow<Unit> = communityClosedRefusalsFlow.asSharedFlow()

    private val accessRefusalsFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * Emits each time the server refuses a call because the user themself has no access to the community (banned,
     * deleted, excluded, out of the audience it is opened to): an access taken away while the app runs first shows
     * that way. The server names that reason in the same `octopus-reason` trailer; a hidden group, or a right the
     * user does not have, is refused with the same code and no reason. Refusals arriving while a value waits for
     * its collector are merged into it.
     */
    val accessRefusals: SharedFlow<Unit> = accessRefusalsFlow.asSharedFlow()

    private val sessionEndedRefusalsFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * Emits each time the server refuses a call because it ended the session the call was sent with (the session of
     * a banned installation, once the ban is lifted). Kept apart from [accessRefusals]: the access of such a session
     * is read even while the stored one is denied, and a merged value must not lose that. Refusals arriving while a
     * value waits for its collector are merged into it.
     */
    val sessionEndedRefusals: SharedFlow<Unit> = sessionEndedRefusalsFlow.asSharedFlow()

    private val interceptor =
        object : ClientInterceptor {
            val apiKeyKey = Key.of("apikey", ASCII_STRING_MARSHALLER)
            val appKey = Key.of("app", ASCII_STRING_MARSHALLER)
            val platformKey = Key.of("platform", ASCII_STRING_MARSHALLER)
            val osVersionKey = Key.of("osversion", ASCII_STRING_MARSHALLER)
            val sdkVersionKey = Key.of("sdkversion", ASCII_STRING_MARSHALLER)
            val sdkFamilyKey = Key.of("sdkfamily", ASCII_STRING_MARSHALLER)
            val sdkFamilyVersionKey = Key.of("sdkfamilyversion", ASCII_STRING_MARSHALLER)
            val installIdKey = Key.of("installid", ASCII_STRING_MARSHALLER)
            val hasCommunityAccessIdKey = Key.of("hascommunityaccess", ASCII_STRING_MARSHALLER)

            val accessTokenKey = Key.of("access-token", ASCII_STRING_MARSHALLER)
            val octopusReasonKey = Key.of("octopus-reason", ASCII_STRING_MARSHALLER)
            val communityClosedReasons = setOf("community_closed", "community_temporarily_disabled")
            val accessRefusedReasons = setOf("account_banned", "account_deleted", "member_excluded", "no_access_cohort")
            val sessionEndedReason = "session_ended"

            override fun <ReqT : Any?, RespT : Any?> interceptCall(
                method: MethodDescriptor<ReqT, RespT>?,
                callOptions: CallOptions?,
                next: Channel,
            ): ClientCall<ReqT, RespT> {
                val options = callOptions ?: CallOptions.DEFAULT
                // Before anything else: a call that is not sent has no credentials to read and no session to reject.
                val bypassesCallsGate = options.getOption(BYPASS_CALLS_GATE)
                if (callsBlocked && !bypassesCallsGate) return BlockedCall()
                val credentials = RepositoryCallCredentials(bypassesCallsGate, options.getOption(WITHOUT_SESSION))
                val combinedCredentials =
                    options.credentials?.let { CompositeCallCredentials(credentials, it) } ?: credentials
                return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                    next.newCall(method, options.withCallCredentials(combinedCredentials)),
                ) {
                    override fun start(
                        responseListener: Listener<RespT>,
                        headers: Metadata,
                    ) {
                        // ApiKey
                        headers.put(apiKeyKey, apiKey)

                        // App
                        headers.put(appKey, appPackageName())

                        // Platform
                        headers.put(platformKey, "Android")

                        // OS Version
                        headers.put(osVersionKey, osVersion())

                        // SDK Version
                        headers.put(sdkVersionKey, BuildConfig.SDK_VERSION_NAME)

                        // SDK family and the version of the SDK the host integrates
                        val identity = sdkIdentity()
                        headers.put(sdkFamilyKey, identity.family.wireValue)
                        headers.put(sdkFamilyVersionKey, identity.version)

                        // InstallId
                        headers.put(installIdKey, preferencesRepository.installId)

                        // HasCommunityAccess
                        headers.put(
                            hasCommunityAccessIdKey,
                            "${hasAccessToCommunity ?: "not_provided"}",
                        )

                        val wrappedListener =
                            object : ForwardingClientCallListener<RespT>() {
                                override fun delegate(): Listener<RespT> = responseListener

                                override fun onHeaders(headers: Metadata) {
                                    headers[accessTokenKey]?.takeIf { it != credentials.jwt }?.let { newJwt ->
                                        try {
                                            runBlocking(checkNotNull(credentialsScope.coroutineContext[Job])) {
                                                rollJwt(sentJwt = credentials.jwt, newJwt = newJwt)
                                            }
                                        } catch (cancelled: CancellationException) {
                                            if (credentialsScope.isActive) throw cancelled
                                        }
                                    }
                                    super.onHeaders(headers)
                                }

                                // Surface the server-side gRPC error (status code + description)
                                // in logcat on a failed call. Without this the SDK swallows the
                                // status into a typed Failure and the UI only shows a generic
                                // message, leaving no diagnostic trace. DEBUG builds only.
                                override fun onClose(
                                    serverStatus: Status,
                                    trailers: Metadata,
                                ) {
                                    credentials.cancel()
                                    // gRPC turns the status credentials fail with into INTERNAL: a call blocked while
                                    // its headers were read closes as one blocked before.
                                    val status = if (credentials.blockedBeforeSending) CALLS_BLOCKED_STATUS else serverStatus
                                    logFailure(method?.fullMethodName, status)
                                    // iOS gate (`ServiceClient`): only an expired session, or one whose
                                    // expiry can't be read, is over. A valid JWT refused (ban) keeps it.
                                    if (status.code == Status.Code.UNAUTHENTICATED) {
                                        credentials.jwt
                                            ?.takeIf { Jwt.isExpired(it) ?: true }
                                            ?.let(::notifySessionRejected)
                                    }
                                    if (status.code == Status.Code.PERMISSION_DENIED) {
                                        when (trailers[octopusReasonKey]) {
                                            in communityClosedReasons -> communityClosedRefusalsFlow.tryEmit(Unit)
                                            in accessRefusedReasons -> accessRefusalsFlow.tryEmit(Unit)
                                            sessionEndedReason -> sessionEndedRefusalsFlow.tryEmit(Unit)
                                        }
                                    }
                                    super.onClose(status, trailers)
                                }
                            }
                        super.start(wrappedListener, headers)
                    }

                    override fun cancel(
                        message: String?,
                        cause: Throwable?,
                    ) {
                        try {
                            super.cancel(message, cause)
                        } finally {
                            credentials.cancel()
                        }
                    }
                }
            }
        }

    val unaryChannel: ManagedChannel = channelFactory(interceptor)

    /**
     * Stores the JWT a response rolled. When the call carried one, only the session it was sent with
     * is rolled: a session replaced meanwhile (logout, another member, guest fallback) is left alone.
     */
    private suspend fun rollJwt(
        sentJwt: String?,
        newJwt: String,
    ) {
        val repository = userRepository
        if (sentJwt != null && repository is UserRepositoryImpl) {
            repository.updateJwt(sentJwt = sentJwt, newJwt = newJwt)
        } else {
            repository.updateJwt(newJwt)
        }
    }

    private fun notifySessionRejected(rejectedJwt: String) = notify(onSessionRejected, rejectedJwt, "a rejected session")

    /**
     * Runs [onSessionEnded] with [endedUserId]. Launched, not run by the caller: it replaces the session, and the
     * caller can be the storing of a session, which holds what that replacement waits for.
     */
    internal fun notifySessionEnded(endedUserId: String) = notify(onSessionEnded, endedUserId, "an ended session")

    private fun notify(
        handler: (suspend (session: String) -> Unit)?,
        session: String,
        what: String,
    ) {
        handler ?: return
        credentialsScope.launch {
            try {
                handler(session)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w("OctopusGrpc", "Handling $what failed", error)
            }
        }
    }

    // One instance per call: both cancellation and the JWT comparison belong to that call.
    private inner class RepositoryCallCredentials(
        private val bypassesCallsGate: Boolean,
        private val withoutSession: Boolean = false,
    ) : CallCredentials() {
        private val callJob = SupervisorJob(credentialsScope.coroutineContext[Job])

        @Volatile
        var jwt: String? = null
            private set

        /** Whether calls were blocked while the headers were read, so that the call never left. */
        @Volatile
        var blockedBeforeSending = false
            private set

        override fun applyRequestMetadata(
            requestInfo: RequestInfo,
            appExecutor: Executor,
            applier: MetadataApplier,
        ) {
            val applied = AtomicBoolean()

            fun fail(error: Throwable) {
                if (applied.compareAndSet(false, true)) {
                    val status =
                        when (error) {
                            is TimeoutCancellationException ->
                                Status.DEADLINE_EXCEEDED.withDescription("Unable to prepare request headers")
                            is CancellationException ->
                                Status.CANCELLED.withDescription("Unable to prepare request headers")
                            else -> Status.INTERNAL
                        }
                    applier.fail(status.withCause(error))
                }
            }

            val task =
                credentialsScope.launch(callJob) {
                    try {
                        // Bound repository reads even for calls without a gRPC deadline.
                        val headers =
                            withTimeout(30_000L) {
                                Metadata().apply {
                                    put(Key.of("accept-language", ASCII_STRING_MARSHALLER), languageRepository.getLocaleIdentifier())
                                    sessionRepository.getAppSession()?.id?.let {
                                        put(Key.of("appsessionid", ASCII_STRING_MARSHALLER), it)
                                    }
                                    sessionRepository.getOctopusSession()?.id?.let {
                                        put(Key.of("octosessionid", ASCII_STRING_MARSHALLER), it)
                                    }
                                    jwt = if (withoutSession) null else userRepository.getUser()?.jwt
                                    jwt?.let { put(Key.of("authorization", ASCII_STRING_MARSHALLER), "Bearer $it") }
                                }
                            }
                        // Checked again now that the call is about to leave: the community may have closed while its
                        // headers were read.
                        if (callsBlocked && !bypassesCallsGate) {
                            if (applied.compareAndSet(false, true)) {
                                blockedBeforeSending = true
                                applier.fail(CALLS_BLOCKED_STATUS)
                            }
                            return@launch
                        }
                        if (applied.compareAndSet(false, true)) {
                            applier.apply(headers)
                        }
                    } catch (error: Throwable) {
                        fail(error)
                    }
                }
            // A cancelled scope may prevent the launch body from ever starting.
            task.invokeOnCompletion { error -> error?.let(::fail) }
        }

        fun cancel() {
            callJob.cancel()
        }
    }

    /** A call closed locally, without reaching the server, because calls are blocked. */
    private class BlockedCall<ReqT, RespT> : ClientCall<ReqT, RespT>() {
        override fun start(
            responseListener: Listener<RespT>,
            headers: Metadata,
        ) {
            responseListener.onClose(CALLS_BLOCKED_STATUS, Metadata())
        }

        override fun request(numMessages: Int) = Unit

        override fun cancel(
            message: String?,
            cause: Throwable?,
        ) = Unit

        override fun halfClose() = Unit

        override fun sendMessage(message: ReqT) = Unit
    }

    /**
     * Shuts down the gRPC channels and releases resources.
     * This should be called when the services are no longer needed.
     */
    @Synchronized
    fun shutdown() {
        retire()
        // Koin's IO onClose and stop() can enter together. A caller must not observe the
        // one-shot flag and return while the winning caller has yet to shut down the channel.
        if (stopped.compareAndSet(false, true)) unaryChannel.shutdownNow()
    }

    /** Cancels graph-owned credentials and session callbacks before physical channel teardown. */
    fun retire() {
        onSessionRejected = null
        onSessionEnded = null
        credentialsScope.cancel()
    }

    internal companion object {
        /**
         * Sends the call even though calls are blocked: set by the check that learns whether a closed community has
         * reopened, the one call let through.
         */
        val BYPASS_CALLS_GATE: CallOptions.Key<Boolean> = CallOptions.Key.createWithDefault("bypassCallsGate", false)

        /**
         * Sends the call without the stored session: set by the calls that ask for a session meant to replace the
         * stored one without being tied to it.
         */
        val WITHOUT_SESSION: CallOptions.Key<Boolean> = CallOptions.Key.createWithDefault("withoutSession", false)

        private val CALLS_BLOCKED_STATUS: Status =
            Status.FAILED_PRECONDITION.withDescription("Calls are blocked while the community is closed")
    }
}
