package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.UserServiceProto
import com.octopuscommunity.sdk.data.local.dao.ProfileDao
import com.octopuscommunity.sdk.data.local.dao.UserConfigDao
import com.octopuscommunity.sdk.data.local.data.ClientUserData
import com.octopuscommunity.sdk.data.local.data.UserData
import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.data.local.datastore.SecuredDataStore
import com.octopuscommunity.sdk.data.local.entity.CurrentUserProfileEntity
import com.octopuscommunity.sdk.data.mapper.ProfileMapper
import com.octopuscommunity.sdk.data.mapper.UserConfigMapper
import com.octopuscommunity.sdk.data.mapper.UserMapper
import com.octopuscommunity.sdk.data.remote.service.NotificationService
import com.octopuscommunity.sdk.data.remote.service.UserService
import com.octopuscommunity.sdk.data.utils.call
import com.octopuscommunity.sdk.domain.model.ClientUser
import com.octopuscommunity.sdk.domain.model.CommunityAccessDenial
import com.octopuscommunity.sdk.domain.model.ConnectionMode
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.domain.model.UserConfig
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.OctopusResult.Failure
import com.octopuscommunity.sdk.domain.network.OctopusResult.Failure.ContentUnavailable
import com.octopuscommunity.sdk.domain.network.OctopusResult.Failure.InvalidArguments
import com.octopuscommunity.sdk.domain.network.OctopusResult.Success
import com.octopuscommunity.sdk.domain.network.ServerError
import com.octopuscommunity.sdk.domain.repository.ClientUserError
import com.octopuscommunity.sdk.domain.repository.ClientUserError.MissingToken
import com.octopuscommunity.sdk.domain.repository.ClientUserError.ProfileError
import com.octopuscommunity.sdk.domain.repository.CommunityClosureRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionResult
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState.Connected
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState.NotConnected
import com.octopuscommunity.sdk.domain.repository.DeletedAccountRecovery
import com.octopuscommunity.sdk.domain.repository.GuestError
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateResult
import com.octopuscommunity.sdk.domain.repository.RefreshEntitlementsError
import com.octopuscommunity.sdk.domain.repository.SsoBanState
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

abstract class ConnectionRepositoryImpl(
    securedDataStore: SecuredDataStore,
    preferencesDataStore: PreferencesDataStore,
    userService: UserService,
    notificationService: NotificationService,
    userConfigDao: UserConfigDao,
    profileDao: ProfileDao,
    userMapper: UserMapper,
    userConfigMapper: UserConfigMapper,
    profileMapper: ProfileMapper,
    protected val clearUserCaches: suspend () -> Unit = {},
    closureRepository: CommunityClosureRepository = OpenCommunityClosureRepository,
) : ProfileRepositoryImpl(
        securedDataStore,
        preferencesDataStore,
        userConfigDao,
        profileDao,
        userService,
        notificationService,
        userMapper,
        userConfigMapper,
        profileMapper,
        closureRepository,
    ),
    ConnectionRepository {
    override val appManagedFields = emptySet<ProfileField>()

    override suspend fun getConnectionState() = getConnectionState(getProfile())

    override fun getConnectionStateFlow() =
        getProfileFlow().map { profile ->
            getConnectionState(profile = profile)
        }

    fun getConnectionState(profile: CurrentUserProfile?) =
        if (profile != null) {
            Connected(profile = profile)
        } else {
            NotConnected
        }

    /**
     * Held while a session's profile and user are written together, so those two writes never
     * interleave with another session's (connection, guest fallback, rejected session logout).
     * Shared with root-feed cache commits; a session replacement or logout purge cannot cross
     * their generation check and write. Never held across a network call or the host's token provider.
     */
    internal val sessionWrites = rootFeedSession.writes

    /**
     * Held while a guest or a client user JWT is fetched and stored, so the two never cross
     * (internal ticket): an exchange sent while the first guest is being created carries no guest JWT, and
     * the backend creates the member another account, with a new A/B cohort, instead of upgrading
     * that guest — whose late answer then replaces the member's session. As iOS, whose `connectUser`
     * waits for the guest connection to end. Never held across the host's token provider.
     */
    internal val connectionCalls = Mutex()

    override suspend fun connectAsGuest(): OctopusResult<ConnectionResult, GuestError> = connectAsGuest(onlyIfSignedOut = false)

    /**
     * Connects a guest session. With [onlyIfSignedOut], the guest is stored only if no session was
     * stored while its JWT was being fetched: a member connected meanwhile is kept. A session stored
     * while this call waited for [connectionCalls] is kept too, and no guest is created. With
     * [replacingJwt], the guest replaces the session of that JWT, if it is still the stored one.
     */
    internal suspend fun connectAsGuest(
        onlyIfSignedOut: Boolean,
        replacingJwt: String? = null,
    ): OctopusResult<ConnectionResult, GuestError> {
        // No connection while the community is closed: the guest is connected once it reopens.
        if (closureRepository.isClosed) return Success(ConnectionResult(getConnectionState()))
        val storedJwt = securedDataStore.getUser()?.jwt
        return connectionCalls.withLock {
            if (securedDataStore.getUser()?.jwt != storedJwt) return Success(ConnectionResult(getConnectionState()))
            call(profileMapper) {
                // A guest meant to replace a session is asked without it, and that session is given up only now
                // that the guest is in hand.
                val response = if (replacingJwt != null) userService.getGuestJwtWithoutSession() else userService.getGuestJwt()
                when {
                    response.hasSuccess() ->
                        response.success.let { result ->
                            replacingJwt?.let { giveUpSession(it) }
                            val profile = result.profile.toEntity()
                            if (saveSession(profile, result.toData(), onlyIfSignedOut)) {
                                ConnectionResult(getConnectionState(profile = profile.toDomain()))
                            } else {
                                ConnectionResult(getConnectionState())
                            }
                        }

                    else -> return InvalidArguments(
                        errors = response.fail.errorsList.map { it.toDomain() },
                    )
                }
            }
        }
    }

    /**
     * Stores [profile] and [user] as the current session under [sessionWrites] — unless
     * [onlyIfSignedOut] and a session is already stored, or [expectedUserId] no longer matches
     * the stored user — and returns whether it did.
     */
    internal suspend fun saveSession(
        profile: CurrentUserProfileEntity,
        user: UserData,
        onlyIfSignedOut: Boolean = false,
        expectedUserId: String? = null,
    ): Boolean = saveSessionReadingAccess(profile, user, onlyIfSignedOut, expectedUserId).saved

    /**
     * What storing a session gave: whether it was stored, and the community access the server answered for it —
     * `null` when the session was not stored or the access could not be read.
     */
    internal class SessionSave(
        val saved: Boolean,
        val access: UserConfig?,
    )

    /**
     * [saveSession], also returning the access read with the new session: the answer of that call, never the
     * stored row, which can still be the one of the previous user when the call fails.
     */
    internal suspend fun saveSessionReadingAccess(
        profile: CurrentUserProfileEntity,
        user: UserData,
        onlyIfSignedOut: Boolean = false,
        expectedUserId: String? = null,
    ): SessionSave {
        val saved =
            sessionWrites.withLock {
                if (onlyIfSignedOut && securedDataStore.getUser() != null) return@withLock false
                if (expectedUserId != null && securedDataStore.getUser()?.id != expectedUserId) return@withLock false
                val previous = profileDao.getCurrentUserProfile()
                // The first session is the initial load's baseline; only replacing a session
                // triggers a reload. Logout/rejection already invalidated before deleting it.
                if (previous != null && (previous.userId != profile.userId || previous.isGuest != profile.isGuest)) {
                    rootFeedSession.invalidate()
                }
                // Profile observers can start authenticated calls as soon as this row is emitted.
                securedDataStore.updateUser { user }
                profileDao.upsertCurrentUserProfile(profile)
                true
            }
        val access = if (saved) (onUserSaved() as? Success)?.data else null
        return SessionSave(saved, access)
    }

    /**
     * Stores [profile] and [user], the answer of a token exchange, as the current session — unless
     * [expectedUserId] no longer matches the stored user. With [replacingJwt], the session of that JWT is given up
     * and replaced under the same lock, and nothing is written when it is no longer the stored one: a logout ended
     * meanwhile stands. The user is stored first and its access read with it; the profile is stored only when that
     * access is not the one of a banned member, whose session reads its access and nothing else: such a member is
     * never reported as connected.
     */
    internal suspend fun saveExchangedSession(
        profile: CurrentUserProfileEntity,
        user: UserData,
        expectedUserId: String? = null,
        replacingJwt: String? = null,
        onStored: () -> Unit = {},
    ): SessionSave {
        val stored =
            sessionWrites.withLock {
                if (expectedUserId != null && securedDataStore.getUser()?.id != expectedUserId) return@withLock false
                if (replacingJwt != null && !giveUpSessionLocked(replacingJwt)) return@withLock false
                // Read first, as storing any session does: a connection cancelled meanwhile (its community was
                // switched, its database closed) stops here, before anything is written or asked for it.
                profileDao.getCurrentUserProfile()
                securedDataStore.updateUser { user }
                // Under the same lock: what goes with this session is written only once it is the stored one.
                onStored()
                true
            }
        if (!stored) return SessionSave(saved = false, access = null)
        val access = (onUserSaved() as? Success)?.data
        sessionWrites.withLock {
            // Replaced while its access was read: the profile row is no longer this session's to write.
            if (securedDataStore.getUser() != user) return@withLock
            val previous = profileDao.getCurrentUserProfile()
            if (access?.bannedMessageOrNull() != null) {
                if (previous != null) {
                    rootFeedSession.invalidate()
                    deleteProfile()
                }
            } else {
                if (previous != null && (previous.userId != profile.userId || previous.isGuest != profile.isGuest)) {
                    rootFeedSession.invalidate()
                }
                profileDao.upsertCurrentUserProfile(profile)
            }
        }
        return SessionSave(saved = true, access = access)
    }

    /**
     * Forgets the session of [jwt] and its caches, if it is still the stored one, keeping the client user the
     * purge forgets too — without the profile synced to that session: the next one starts empty, and what the
     * host gives must be sent to it again. The access of that session stays published until the next one has its
     * own.
     */
    internal suspend fun giveUpSession(jwt: String) {
        sessionWrites.withLock { giveUpSessionLocked(jwt) }
    }

    /** [giveUpSession], with [sessionWrites] already held; returns whether the session of [jwt] was the stored one. */
    private suspend fun giveUpSessionLocked(jwt: String): Boolean {
        if (securedDataStore.getUser()?.jwt != jwt) return false
        val clientUser = securedDataStore.getClientUser()
        heldAccess.value = userConfigDao.getUserConfig()
        rootFeedSession.invalidate()
        deleteProfile()
        securedDataStore.updateUser { null }
        clearUserCaches()
        clientUser?.let { kept ->
            securedDataStore.updateClientUser { ClientUserData(kept.id, ClientUserData.Profile(null, null, null)) }
        }
        return true
    }

    // SSO
    override suspend fun getClientUser(): ClientUser? = throw NotImplementedError()

    override fun getClientUserFlow(): Flow<ClientUser?> = throw NotImplementedError()

    override suspend fun connectUser(
        clientUser: ClientUser,
        tokenProvider: (suspend () -> String),
    ): OctopusResult<ConnectionResult, ClientUserError> = throw NotImplementedError()

    override suspend fun logout() {
        val connectionState = getConnectionState()
        if (connectionState is Connected && connectionState.isGuest) return
        sessionWrites.withLock {
            rootFeedSession.invalidate()
            deleteProfile()
            securedDataStore.updateUser { null }
            clearUserCaches()
        }
        // While the community is closed, the logout stays local: the token and the guest wait for the reopening.
        if (closureRepository.isClosed) return
        // Rebind the device token even when fetching a new guest fails.
        checkPushNotificationsToken()
        if (connectionState is Connected && !connectionState.isGuest) {
            connectAsGuest()
        }
    }
}

class SSOConnectionRepositoryImpl(
    override val connectionMode: ConnectionMode.SSO,
    securedDataStore: SecuredDataStore,
    preferencesDataStore: PreferencesDataStore,
    userService: UserService,
    notificationService: NotificationService,
    userConfigDao: UserConfigDao,
    profileDao: ProfileDao,
    userMapper: UserMapper,
    userConfigMapper: UserConfigMapper,
    profileMapper: ProfileMapper,
    clearUserCaches: suspend () -> Unit = {},
    closureRepository: CommunityClosureRepository = OpenCommunityClosureRepository,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ConnectionRepositoryImpl(
        securedDataStore,
        preferencesDataStore,
        userService,
        notificationService,
        userConfigDao,
        profileDao,
        userMapper,
        userConfigMapper,
        profileMapper,
        clearUserCaches,
        closureRepository,
    ),
    SsoBanState,
    DeletedAccountRecovery {
    /** Set under [providerLock], with [pendingEndedUserId]. */
    @Volatile
    private var clientUserTokenProvider: (suspend () -> String)? = null

    /**
     * Guards [clientUserTokenProvider] and [pendingEndedUserId] together: an ended session is kept pending only while
     * no provider is set, and the provider is set knowing whether one is pending.
     */
    private val providerLock = Any()

    /**
     * The user id of a member's session the server ended before the host gave its token provider in this process. It
     * is not replaced by a guest that the exchange of `connectUser` would replace right after: `connectUser` replaces
     * it. An ended session is told by its user id, never by its JWT: the answer that ends it also renews its JWT.
     */
    @Volatile
    private var pendingEndedUserId: String? = null

    /**
     * Held while a session is replaced — an ended one, or a deleted account's — so the replacements that find it
     * ended replace it once.
     */
    private val sessionReplacements = Mutex()
    private val clientUserBan = ClientUserBanState()

    init {
        userService.onSessionRejected(::onSessionRejected)
        userService.onSessionEnded(::onSessionEnded)
    }

    /** When the last session replaced because the server ended it was replaced, as [nowMillis] gives it. */
    private val lastEndedSessionReplacementAtMillis = AtomicLong(NEVER)

    override fun getUserBannedMessage(): String? = clientUserBan.error?.errorMessage

    override val appManagedFields = connectionMode.appManagedFields

    override suspend fun getClientUser() =
        with(userMapper) {
            securedDataStore.getClientUser()?.toDomain()
        }

    override fun getClientUserFlow(): Flow<ClientUser?> =
        with(userMapper) {
            securedDataStore.clientUserFlow.map { it?.toDomain() }
        }

    override suspend fun checkConnection(profile: CurrentUserProfile?): OctopusResult<ConnectionResult, ServerError> =
        // Nothing to check while the community is closed: the connection is resumed once it reopens.
        if (closureRepository.isClosed) {
            Success(ConnectionResult(getConnectionState()))
        } else {
            checkOpenCommunityConnection(profile)
        }

    private suspend fun checkOpenCommunityConnection(profile: CurrentUserProfile?): OctopusResult<ConnectionResult, ServerError> =
        when (profile) {
            null -> {
                when (val clientUser = getClientUser()) {
                    null -> connectAsGuest()
                    else -> reportingStoredBan(clientUser) { connectWithClientUser(clientUser) }
                }
            }

            else -> {
                if (profile.needMigration) {
                    migrateUserToFrictionlessUser(profile)
                }
                when (val clientUser = getClientUser()) {
                    null -> Success(ConnectionResult(getConnectionState()))

                    else ->
                        when (profile.isGuest) {
                            true -> reportingStoredBan(clientUser) { connectWithClientUser(clientUser) }
                            false -> reportingStoredBan(clientUser) { checkProfileUpdates(clientUser) }
                        }
                }
            }
        }

    suspend fun migrateUserToFrictionlessUser(profile: CurrentUserProfile): OctopusResult<CurrentUserProfile, ClientUserError> =
        call(profileMapper) {
            // If nickname is not present, ask the backend to create one.
            val response =
                userService.updateProfile(
                    userId = profile.userId,
                    profileUpdate =
                        if (profile.nickname.isNotEmpty()) {
                            // This is the case of a fully created profile but coming from a
                            // non-frictionless env
                            profile
                                .copy(
                                    hasSeenOnboarding = true,
                                    hasAcceptedCgu = true,
                                    hasConfirmedNickname = true,
                                    hasConfirmedBio = true,
                                    hasConfirmedPicture = true,
                                ).toProto(
                                    previousProfile = profile,
                                    // The picture here is the host's `ClientUser` avatar, not one the member
                                    // picked: a migration must not fail over it (internal change).
                                    failOnPictureEncodingError = false,
                                )
                        } else {
                            // This is the case where the user was logged in before migration
                            // but has never passed the profile creation step.
                            profile
                                .copy(
                                    hasSeenOnboarding = false,
                                    hasAcceptedCgu = false,
                                    hasConfirmedNickname = false,
                                    hasConfirmedBio = false,
                                    hasConfirmedPicture = false,
                                ).toProto(
                                    previousProfile = profile,
                                    findAvailableNickname = true,
                                    failOnPictureEncodingError = false,
                                )
                        },
                )
            when {
                response.hasSuccess() ->
                    response.success.profile
                        .toEntity()
                        .also { profileEntity ->
                            profileDao.upsertCurrentUserProfile(profileEntity)
                        }.toDomain()

                else -> return InvalidArguments(
                    errors =
                        response.fail.errorsList.map { error ->
                            ProfileError(error.toDomain())
                        },
                )
            }
        }

    override suspend fun connectUser(
        clientUser: ClientUser,
        tokenProvider: suspend () -> String,
    ): OctopusResult<ConnectionResult, ClientUserError> {
        val oldClientUser = getClientUser()
        if (oldClientUser?.userId != null && oldClientUser.userId != clientUser.userId) {
            logout()
        }
        synchronized(providerLock) { clientUserTokenProvider = tokenProvider }
        if (closureRepository.isClosed) {
            // Kept as the user to connect once the community reopens: the exchange would be a call.
            securedDataStore.updateClientUser { with(userMapper) { clientUser.toData() } }
            return Success(ConnectionResult(getConnectionState()))
        }
        val pending = synchronized(providerLock) { pendingEndedUserId.also { pendingEndedUserId = null } }
        // Still the stored session of this client user: the exchange replaces it, and nothing else is connected.
        if (pending != null && securedDataStore.getUser()?.id == pending && securedDataStore.getClientUser()?.id == clientUser.userId) {
            return replaceEndedSessionByExchange(clientUser, pending)
        }
        return reportingStoredBan(clientUser) {
            when (val connectionState = getConnectionState()) {
                is NotConnected -> connectWithClientUser(clientUser)
                is Connected ->
                    if (connectionState.profile.isGuest) {
                        connectWithClientUser(clientUser)
                    } else if (clientUser.profile != oldClientUser?.profile) {
                        checkProfileUpdates(clientUser = clientUser)
                    } else {
                        Success(ConnectionResult(connectionState))
                    }
            }
        }
    }

    /**
     * Re-invokes the registered [clientUserTokenProvider] to obtain a fresh client-signed JWT,
     * exchanges it for a new Octopus JWT (which carries the updated entitlement claims), and
     * upserts the cached profile so observers receive the new entitlements.
     */
    override suspend fun refreshEntitlements(): OctopusResult<Unit, RefreshEntitlementsError> {
        val tokenProvider =
            clientUserTokenProvider
                ?: return InvalidArguments(RefreshEntitlementsError.NoClientTokenProvider())

        val state = getConnectionState()
        if (state !is Connected || state.profile.isGuest) {
            return InvalidArguments(RefreshEntitlementsError.UserNotConnected())
        }
        val memberId =
            securedDataStore.getUser()?.id?.takeIf { it == state.profile.userId }
                ?: return InvalidArguments(RefreshEntitlementsError.UserNotConnected())

        return call(profileMapper) {
            val clientUserToken =
                tokenProvider().ifEmpty { null }
                    ?: return InvalidArguments(RefreshEntitlementsError.NoClientTokenProvider())

            val response = userService.getClientJwt(clientUserToken)
            when {
                response.hasSuccess() -> {
                    val result = response.success
                    val profileEntity = result.profile.toEntity()
                    val save = saveExchangedSession(profileEntity, result.toData(), expectedUserId = memberId)
                    if (!save.saved) return InvalidArguments(RefreshEntitlementsError.UserNotConnected())
                    // The exchange of a banned member succeeds, with a session that only reads its access.
                    save.access?.bannedMessageOrNull()?.let { message ->
                        // Retained as the ban of a refused connection is, so that it blocks this user's contributions.
                        getClientUser()?.let { clientUser ->
                            clientUserBan.record(clientUser.userId, InvalidArguments(error = ClientUserError.UserBanned(message)))
                        }
                        return InvalidArguments(RefreshEntitlementsError.UserBanned(message))
                    }
                    val profile = profileEntity.toDomain()

                    if (profile.needMigration) {
                        when (val migrationResult = migrateUserToFrictionlessUser(profile)) {
                            is Success -> Unit
                            is Failure.InvalidArguments -> return InvalidArguments(
                                errors =
                                    migrationResult.errors.map { error ->
                                        RefreshEntitlementsError.ServerError(error.errorMessage)
                                    },
                            )
                            is Failure.NoNetwork ->
                                return InvalidArguments(RefreshEntitlementsError.NoNetwork())

                            is Failure.Connection -> return InvalidArguments(
                                RefreshEntitlementsError.ServerError(
                                    errorMessage =
                                        (migrationResult as? Failure.StatusError)
                                            ?.description ?: "Server error",
                                    throwable = (migrationResult as? Failure.StatusError)?.exception,
                                ),
                            )
                        }
                    }
                    Unit
                }

                else -> return InvalidArguments(
                    errors =
                        response.fail.errorsList.map { error ->
                            when (error.errorCode) {
                                UserServiceProto.GetJwtFromClientSignedTokenResponse.ErrorCode.USER_BANNED ->
                                    RefreshEntitlementsError.UserBanned(error.message)

                                else -> RefreshEntitlementsError.ServerError(error.message)
                            }
                        },
                )
            }
        }.let { result ->
            // Translate connection-level failures (network unavailable, gRPC status errors)
            // into the typed [RefreshEntitlementsError] surface so the caller has a single
            // exhaustive switch on this function's failures.
            when (result) {
                is Success -> result
                is Failure.NoNetwork -> InvalidArguments(RefreshEntitlementsError.NoNetwork())
                is Failure.Connection ->
                    InvalidArguments(
                        RefreshEntitlementsError.ServerError(
                            errorMessage =
                                (result as? Failure.StatusError)?.description
                                    ?: "Server error",
                            throwable = (result as? Failure.StatusError)?.exception,
                        ),
                    )
                is Failure.InvalidArguments<*> -> result
            }
        }
    }

    /**
     * The connection of [clientUser]: the ban of its stored session when there is one, else [connect]. Either way
     * the result replaces the refusal retained for this user. A stored session the server ended is no ban: it is
     * replaced instead ([replaceEndedSessionOf]).
     */
    private suspend fun reportingStoredBan(
        clientUser: ClientUser,
        connect: suspend () -> OctopusResult<ConnectionResult, ClientUserError>,
    ): OctopusResult<ConnectionResult, ClientUserError> =
        when (val stored = storedBan(clientUser)) {
            is StoredBan.Banned -> clientUserBan.record(clientUser.userId, InvalidArguments(error = stored.error))
            is StoredBan.Ended -> replaceEndedSessionOf(clientUser, stored.userId)
            null -> clientUserBan.record(clientUser.userId, connect())
        }

    /** What the stored access of a banned member says once read again. */
    private sealed interface StoredBan {
        /** The member still is banned, or the access could not be read. */
        class Banned(
            val error: ClientUserError.UserBanned,
        ) : StoredBan

        /** The server ended the session of [userId], the one of the ban: it says nothing about the member now. */
        class Ended(
            val userId: String,
        ) : StoredBan
    }

    /**
     * The ban of the stored session, if it is the session of [clientUser] and its stored access is the one of a
     * banned member. Such a session does not go through the token exchange again, and the refusal retained in
     * memory is lost with the process: the access is read again to know whether the member still is banned. When
     * that read fails, the stored access decides — unless the server answered that it ended that session, which
     * happens once the ban is lifted. When the ban was lifted, the connection goes on, by an exchange since no
     * member profile was stored.
     */
    private suspend fun storedBan(clientUser: ClientUser): StoredBan? {
        val stored = getUserConfig()?.takeIf { it.accessDenial is CommunityAccessDenial.AccountBanned } ?: return null
        // The stored access is the one of the stored session: never the ban another user left.
        val user = securedDataStore.getUser() ?: return null
        if (securedDataStore.getClientUser()?.id != clientUser.userId) return null
        val read = readCommunityAccess() ?: return StoredBan.Ended(user.id)
        val access = (read as? Success)?.data ?: stored
        return access.bannedMessageOrNull()?.let { StoredBan.Banned(ClientUserError.UserBanned(it)) }
    }

    /**
     * Replaces the ended session of [endedUserId], the one of [clientUser], by an exchange of its token. Before the
     * host gave the token provider in this process, the session is kept pending for `connectUser` instead: a guest
     * would only be replaced by its exchange right after.
     */
    private suspend fun replaceEndedSessionOf(
        clientUser: ClientUser,
        endedUserId: String,
    ): OctopusResult<ConnectionResult, ClientUserError> {
        if (keepPendingUntilTheProvider(endedUserId)) return Success(ConnectionResult(getConnectionState()))
        return replaceEndedSessionByExchange(clientUser, endedUserId)
    }

    /** Keeps [endedUserId] for `connectUser` when no token provider is set yet; returns whether it did. */
    private fun keepPendingUntilTheProvider(endedUserId: String): Boolean =
        synchronized(providerLock) {
            if (clientUserTokenProvider != null) return@synchronized false
            pendingEndedUserId = endedUserId
            true
        }

    /** The JWT the session of [userId] has now, if it still is the stored session: any call can renew it. */
    private suspend fun currentJwtOf(userId: String): String? = securedDataStore.getUser()?.takeIf { it.id == userId }?.jwt

    /**
     * Replaces the ended session of [endedUserId] by an exchange of [clientUser]'s token, if it still is the stored
     * session. When the exchange fails for a passing reason, a guest stands in, as a connection falls back to one, and
     * the next connection exchanges again; when no guest can be had either, the ended session stays stored and the
     * exchange's failure is returned: the next read of the access finds it again.
     */
    private suspend fun replaceEndedSessionByExchange(
        clientUser: ClientUser,
        endedUserId: String,
    ): OctopusResult<ConnectionResult, ClientUserError> =
        withContext(NonCancellable) {
            sessionReplacements.withLock {
                val endedJwt = currentJwtOf(endedUserId) ?: return@withLock Success(ConnectionResult(getConnectionState()))
                try {
                    val exchange = connectWithClientUser(clientUser, replacingJwt = endedJwt)
                    // A ban is reported as on any exchange, without a guest; a session the exchange gave is the member's.
                    if (exchange is Success || exchange.isBan()) return@withLock exchange
                    val stillEndedJwt = currentJwtOf(endedUserId) ?: return@withLock exchange
                    val guest = connectAsGuest(onlyIfSignedOut = true, replacingJwt = stillEndedJwt)
                    if (guest !is Success) return@withLock exchange
                    // The session ended because the ban was lifted: a ban kept in memory would show it again.
                    clientUserBan.clear(clientUser.userId)
                    Success(ConnectionResult(getConnectionState()))
                } finally {
                    heldAccess.value = null
                }
            }
        }

    /**
     * With [replacingJwt], the exchange replaces the session of that JWT, if it is still the stored one. Its
     * result, a refusal included, is retained as the ban of [clientUser] only while the stored session is still
     * the one of this exchange — the session it replaces, or the one it stored. Once a logout or another user took
     * its place, the exchange is dropped: nothing is retained, and the retained ban of that other user is kept.
     */
    private suspend fun connectWithClientUser(
        clientUser: ClientUser,
        replacingJwt: String? = null,
    ): OctopusResult<ConnectionResult, ClientUserError> {
        if (replacingJwt == null) return clientUserBan.record(clientUser.userId, exchangeClientUser(clientUser, null))
        var exchanged: UserData? = null
        val result = exchangeClientUser(clientUser, replacingJwt) { exchanged = it }
        return sessionWrites.withLock {
            val stored = securedDataStore.getUser()
            if (stored != null && (stored.jwt == replacingJwt || stored == exchanged)) {
                clientUserBan.record(clientUser.userId, result)
            } else {
                null
            }
        } ?: Success(ConnectionResult(getConnectionState()))
    }

    /** [onReplaced] is given the session an exchange with a [replacingJwt] stored, once it is stored. */
    private suspend fun exchangeClientUser(
        clientUser: ClientUser,
        replacingJwt: String?,
        onReplaced: (UserData) -> Unit = {},
    ): OctopusResult<ConnectionResult, ClientUserError> =
        call(profileMapper) {
            val clientUserToken =
                clientUserTokenProvider?.invoke()?.ifEmpty { null }
                    ?: return InvalidArguments(error = MissingToken())

            // After the guest being created, if any, so the exchange carries its JWT (internal ticket).
            var save: SessionSave? = null
            val response =
                connectionCalls.withLock {
                    // An exchange meant to replace a session is sent without it, and that session is given up only
                    // now that the next one is in hand.
                    val exchange =
                        if (replacingJwt != null) {
                            userService.getClientJwtWithoutSession(clientUserToken)
                        } else {
                            userService.getClientJwt(clientUserToken)
                        }
                    exchange.also { response ->
                        if (response.hasSuccess()) {
                            if (replacingJwt == null) {
                                clientUserBan.clear()
                                save = saveExchangedSession(response.success.profile.toEntity(), response.success.toData())
                            } else {
                                val user = response.success.toData()
                                // The retained ban is cleared only with the session that replaces the deleted one:
                                // a dropped exchange leaves the ban of the user who took its place.
                                save =
                                    saveExchangedSession(
                                        profile = response.success.profile.toEntity(),
                                        user = user,
                                        replacingJwt = replacingJwt,
                                        onStored = {
                                            clientUserBan.clear()
                                            onReplaced(user)
                                        },
                                    )
                            }
                        }
                    }
                }
            when {
                // The session it was meant to replace is gone (a logout ended meanwhile): the exchange is dropped.
                replacingJwt != null && save?.saved == false -> return Success(ConnectionResult(getConnectionState()))

                response.hasSuccess() -> {
                    // The exchange of a banned member succeeds, with a session that only reads its access: it is
                    // reported as the refused exchange it used to be, before anything is written for the member.
                    save?.access?.bannedMessageOrNull()?.let { message ->
                        // Kept as the user of this session, so that another host user is not taken for it.
                        securedDataStore.updateClientUser { with(userMapper) { clientUser.toData() } }
                        return InvalidArguments(error = ClientUserError.UserBanned(message))
                    }
                    val profile =
                        response.success.profile
                            .toEntity()
                            .toDomain()

                    if (profile.needMigration) {
                        when (val result = migrateUserToFrictionlessUser(profile)) {
                            is Success -> {}
                            is Failure -> return result
                        }
                    }

                    return checkProfileUpdates(clientUser = clientUser)
                }

                else -> return InvalidArguments(
                    errors = response.fail.errorsList.map { it.toDomain() },
                )
            }
        }

    private suspend fun checkProfileUpdates(clientUser: ClientUser): OctopusResult<ConnectionResult, ClientUserError> =
        call(profileMapper) {
            val forceUpdate =
                when (val connectionState = getConnectionState()) {
                    is NotConnected -> true
                    is Connected -> connectionState.profile.isGuest
                }
            val octopusClientProfile = getClientUser()?.profile?.takeIf { !forceUpdate }
            val octopusProfile = getProfile() ?: return ContentUnavailable
            var findAvailableNickname: Boolean
            var hasUpdated = forceUpdate
            var updatedProfile = octopusProfile.copy()
            val clientNickname = clientUser.profile?.nickname?.ifEmpty { null }
            // Update the field if is appManaged or if the user has not yet confirmed it
            if ((ProfileField.NICKNAME in appManagedFields || !octopusProfile.hasConfirmedNickname) &&
                clientNickname != null &&
                // Check with the original nickname first because the nickname can vary from the one
                // requested
                (
                    (
                        octopusProfile.originalNickname?.ifEmpty { null }
                            ?: octopusProfile.nickname.ifEmpty { null }
                    ) != clientNickname
                )
            ) {
                updatedProfile =
                    updatedProfile.copy(
                        nickname = clientNickname,
                    )
                if (ProfileField.NICKNAME in appManagedFields) {
                    updatedProfile =
                        updatedProfile.copy(
                            hasConfirmedNickname = true,
                        )
                }
                findAvailableNickname = ProfileField.NICKNAME !in appManagedFields
                hasUpdated = true
            } else {
                findAvailableNickname = false
            }
            if (ProfileField.BIO in appManagedFields || !octopusProfile.hasConfirmedBio) {
                updatedProfile =
                    updatedProfile.copy(
                        bio = clientUser.profile?.bio?.ifEmpty { null },
                        hasConfirmedBio = if (ProfileField.BIO in appManagedFields) true else octopusProfile.hasConfirmedBio,
                    )
                hasUpdated = true
            }
            if (ProfileField.PICTURE in appManagedFields || !octopusProfile.hasConfirmedPicture) {
                updatedProfile =
                    updatedProfile.copy(
                        picture = clientUser.profile?.picture,
                        hasConfirmedPicture =
                            if (ProfileField.PICTURE in appManagedFields) {
                                true
                            } else {
                                octopusProfile.hasConfirmedPicture
                            },
                    )
                hasUpdated = true
            }
            val hasFieldsUpdate =
                hasUpdated &&
                    (
                        updatedProfile.nickname != octopusProfile.nickname ||
                            updatedProfile.bio != octopusProfile.bio ||
                            updatedProfile.picture != octopusClientProfile?.picture
                    )

            val profileUpdate =
                if (forceUpdate || hasFieldsUpdate) {
                    updatedProfile
                        .toProto(
                            previousProfile = octopusProfile,
                            findAvailableNickname = findAvailableNickname,
                            // SSO sync: the avatar comes from the host, so an encoding failure must not
                            // fail the login. It no longer resets the stored picture either (internal change).
                            failOnPictureEncodingError = false,
                        ).toSsoProfileUpdateOrNull()
                } else {
                    null
                }

            if (profileUpdate != null) {
                val response =
                    userService.updateProfile(
                        userId = updatedProfile.userId,
                        profileUpdate = profileUpdate,
                    )
                when {
                    response.hasSuccess() ->
                        response.success.let { result ->
                            val profile =
                                result.profile
                                    .toEntity()
                                    .also { profileEntity ->
                                        profileDao.upsertCurrentUserProfile(profileEntity)
                                    }.toDomain()
                            ConnectionResult(
                                connectionState = getConnectionState(profile = profile),
                                profileUpdate =
                                    ProfileUpdateResult(
                                        previousProfile = octopusProfile,
                                        newProfile = profile,
                                        gamificationAction =
                                            if (result.shouldDisplayProfileCompletedGamificationToast) {
                                                Gamification.Action.PROFILE_COMPLETED
                                            } else {
                                                null
                                            },
                                    ),
                            ).also {
                                securedDataStore.updateClientUser {
                                    clientUser.toData()
                                }
                            }
                        }

                    else -> return InvalidArguments(
                        errors = response.fail.errorsList.map { ProfileError(it.toDomain()) },
                    )
                }
            } else {
                ConnectionResult(getConnectionState(profile = octopusProfile)).also {
                    securedDataStore.updateClientUser {
                        clientUser.toData()
                    }
                }
            }
        }

    /**
     * The server answered `UNAUTHENTICATED` to a call sent with [rejectedJwt], an expired member JWT
     * (the client only reports expired or unreadable ones, as iOS). As iOS does, the member is logged
     * out — client user forgotten, user caches purged as on [logout] — and a guest session is
     * connected, so the connection state stops reporting a member and the host's login-required
     * callback runs on the next member action.
     *
     * Every write is conditional, so this never undoes a newer session: the session is forgotten
     * only while [rejectedJwt] is still stored (checked and written under [sessionWrites]), and the
     * guest is stored only if no session was stored meanwhile (a late `connectUser` wins). Other
     * rejections of the same burst, and late ones of a replaced session, find another JWT and do
     * nothing. A guest session is kept as it is (iOS: a guest is never destroyed and re-created).
     */
    internal suspend fun onSessionRejected(rejectedJwt: String) {
        val forgotten =
            sessionWrites.withLock {
                val connectionState = getConnectionState()
                if (connectionState !is Connected || connectionState.isGuest) return
                securedDataStore.forgetSessionIf { it.user?.jwt == rejectedJwt }?.also {
                    rootFeedSession.invalidate()
                    deleteProfile()
                    // As a logout (iOS purges on any session end): the member's caches go before
                    // the lock is released, so the purge never reaches a session stored after it.
                    clearUserCaches()
                }
            } ?: return
        forgotten.clientUser?.let { clientUserBan.clear(clientUserId = it.id) }
        // As a logout: rebind the device token even when fetching a new guest fails.
        checkPushNotificationsToken()
        connectAsGuest(onlyIfSignedOut = true)
    }

    /**
     * The session of a deleted account still reads its access, and nothing else. It is replaced ([replaceSession]):
     * the deleted account is no longer tied to the client user, so the server gives its token a new one.
     */
    override suspend fun recoverFromDeletedAccount(): OctopusResult<ConnectionResult, ServerError> {
        if (closureRepository.isClosed) return Success(ConnectionResult(getConnectionState()))
        // Read again first: a tap on a screen that is about to leave must not replace a session that has its access.
        val access = (fetchCommunityAccess() as? Success)?.data ?: getUserConfig()
        if (access?.accessDenial != CommunityAccessDenial.AccountDeleted) {
            return Success(ConnectionResult(getConnectionState()))
        }
        val connectionState = getConnectionState()
        // A guest session is kept as it is.
        if (connectionState is Connected && connectionState.isGuest) return Success(ConnectionResult(connectionState))
        val deletedJwt = securedDataStore.getUser()?.jwt
        return sessionReplacements.withLock {
            // Replaced while this waited: by another tap, or because the server ended it.
            if (securedDataStore.getUser()?.jwt !=
                deletedJwt
            ) {
                Success(ConnectionResult(getConnectionState()))
            } else {
                replaceSession(deletedJwt)
            }
        }
    }

    /**
     * The server read the access of the session of [endedUserId] and answered that it ended it: the session of an
     * installation that was banned, renewed once the ban is lifted. Told by its user id: that answer also renewed its
     * JWT, and the JWT given up is the one it has when it is replaced. Nothing is shown for it: the session is replaced
     * as the one of a deleted account is, a guest one too, and the access the next session reads replaces the stored
     * one. Not when another session is stored by now, nor while the community is closed.
     *
     * At most one replacement every [MIN_INTERVAL_BETWEEN_ENDED_SESSION_REPLACEMENTS_MILLIS]: the reads of the access
     * that find the same session ended replace it once, and a server that would end each new session at once is not
     * asked for another in a loop. An end skipped that way is found again by the next read of the access.
     */
    internal suspend fun onSessionEnded(endedUserId: String) {
        if (closureRepository.isClosed || securedDataStore.getUser()?.id != endedUserId) return
        // A member's session, before the host gave its token provider: connectUser replaces it by its exchange.
        val memberSession = securedDataStore.getClientUser() != null && profileDao.getCurrentUserProfile()?.isGuest != true
        if (memberSession && keepPendingUntilTheProvider(endedUserId)) return
        val now = nowMillis()
        val last = lastEndedSessionReplacementAtMillis.get()
        // A clock set back must not hold the replacements for longer than the interval.
        if (last != NEVER && now - last in 0 until MIN_INTERVAL_BETWEEN_ENDED_SESSION_REPLACEMENTS_MILLIS) return
        if (!lastEndedSessionReplacementAtMillis.compareAndSet(last, now)) return
        sessionReplacements.withLock {
            // Replaced meanwhile, by connectUser or another read that found it ended.
            currentJwtOf(endedUserId)?.let { replaceSession(it) }
        }
    }

    /**
     * Replaces the session of [replacedJwt]: the client user's token is exchanged when its provider is known, else a
     * guest is connected, as when the exchange fails for a passing reason. The new session is asked without the
     * replaced one, which is given up, with its caches, only once the new one is in hand: when nothing connects,
     * nothing has changed.
     */
    private suspend fun replaceSession(replacedJwt: String?): OctopusResult<ConnectionResult, ServerError> =
        // Not cancellable: the caller can be a screen the user leaves, and a return stopped half-way would leave the
        // session given up and its access unread.
        withContext(NonCancellable) {
            try {
                val clientUser = getClientUser()
                val exchange =
                    clientUser
                        ?.takeIf { clientUserTokenProvider != null }
                        ?.let { connectWithClientUser(it, replacingJwt = replacedJwt) }
                when {
                    exchange is Success -> exchange
                    // A ban is reported as on any exchange, without a guest.
                    exchange != null && exchange.isBan() -> exchange
                    // The exchange gave a session, and failed after it: the member is back.
                    replacedJwt != null && securedDataStore.getUser()?.jwt != replacedJwt ->
                        Success(ConnectionResult(getConnectionState()))
                    else ->
                        connectAsGuest(onlyIfSignedOut = true, replacingJwt = replacedJwt).also {
                            // Whatever the client user was refused before, a guest holds the session now.
                            if (it is Success && clientUser != null) clientUserBan.clear(clientUser.userId)
                        }
                }
            } finally {
                heldAccess.value = null
            }
        }

    private fun OctopusResult<ConnectionResult, ClientUserError>.isBan() =
        this is InvalidArguments && errors.any { it is ClientUserError.UserBanned }

    override suspend fun logout() {
        val hadClientUser = getClientUser() != null
        val connectionState = getConnectionState()
        synchronized(providerLock) {
            clientUserTokenProvider = null
            pendingEndedUserId = null
        }
        clientUserBan.clear()
        if (connectionState is Connected && connectionState.isGuest && !hadClientUser) return
        securedDataStore.updateClientUser { null }
        if (connectionState is Connected && connectionState.isGuest) {
            clearUserCaches()
            if (!closureRepository.isClosed) checkPushNotificationsToken()
        } else {
            super.logout()
        }
    }
}

/** The least time between two replacements of a session the server ended. */
internal const val MIN_INTERVAL_BETWEEN_ENDED_SESSION_REPLACEMENTS_MILLIS = 30_000L

private const val NEVER = Long.MIN_VALUE

/** Retains an SSO refusal in memory; user identity and ban are replaced in one atomic update. */
internal class ClientUserBanState {
    private data class State(
        val clientUserId: String,
        val error: ClientUserError.UserBanned?,
    )

    private val state = AtomicReference<State?>(null)
    val error: ClientUserError.UserBanned? get() = state.get()?.error

    fun record(
        userId: String,
        result: OctopusResult<ConnectionResult, ClientUserError>,
    ): OctopusResult<ConnectionResult, ClientUserError> {
        val ban = (result as? InvalidArguments)?.errors?.filterIsInstance<ClientUserError.UserBanned>()?.firstOrNull()
        do {
            val previous = state.get()
            val retained = previous?.takeIf { it.clientUserId == userId && result !is Success }?.error
            val updated = State(userId, ban ?: retained)
        } while (!state.compareAndSet(previous, updated))
        return result
    }

    fun clear() {
        state.set(null)
    }

    /** Clears the retained refusal only if it is [clientUserId]'s. */
    fun clear(clientUserId: String) {
        state.updateAndGet { retained -> retained?.takeUnless { it.clientUserId == clientUserId } }
    }
}

/** Keeps nickname generation tied to an outgoing nickname, then skips empty SSO updates. */
internal fun UserServiceProto.ProfileUpdate.toSsoProfileUpdateOrNull(): UserServiceProto.ProfileUpdate? {
    // The host nickname can equal the current nickname while differing from originalNickname.
    // The mapper then omits nickname; leaving the option would request an unrelated auto-nickname.
    // Send an explicit false (the shape iOS and the base always send) rather than an absent field.
    val update = if (hasNickname()) this else toBuilder().setOptFindAvailableNickname(false).build()
    return update.takeIf { it.hasProfileChanges() }
}

/** Whether SSO sync has anything to send after tolerant picture encoding. */
internal fun UserServiceProto.ProfileUpdate.hasProfileChanges(): Boolean =
    // The mapper always sets this option, including false. Only true requests a change itself.
    optFindAvailableNickname ||
        toBuilder().clearOptFindAvailableNickname().build() != UserServiceProto.ProfileUpdate.getDefaultInstance()
