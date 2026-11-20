package com.octopuscommunity.sdk

import android.content.Context
import android.util.Log
import com.octopuscommunity.sdk.OctopusSDK.connectUser
import com.octopuscommunity.sdk.OctopusSDK.connectionState
import com.octopuscommunity.sdk.OctopusSDK.fetchOrCreateClientObjectRelatedPost
import com.octopuscommunity.sdk.OctopusSDK.initialize
import com.octopuscommunity.sdk.OctopusSDK.isUserConnected
import com.octopuscommunity.sdk.OctopusSDK.notSeenNotificationsCount
import com.octopuscommunity.sdk.OctopusSDK.overrideCommunityAccess
import com.octopuscommunity.sdk.OctopusSDK.stop
import com.octopuscommunity.sdk.OctopusSDK.switchCommunity
import com.octopuscommunity.sdk.OctopusSDK.topics
import com.octopuscommunity.sdk.OctopusSDK.trackAccessToCommunity
import com.octopuscommunity.sdk.data.event.LifecycleEventsImpl
import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.data.remote.client.OctopusGrpcClient
import com.octopuscommunity.sdk.data.remote.client.SdkIdentity
import com.octopuscommunity.sdk.di.dataStoreModule
import com.octopuscommunity.sdk.di.databaseModule
import com.octopuscommunity.sdk.di.eventsModule
import com.octopuscommunity.sdk.di.mapperModule
import com.octopuscommunity.sdk.di.networkModule
import com.octopuscommunity.sdk.di.octopusImageModule
import com.octopuscommunity.sdk.di.octopusUseCaseModule
import com.octopuscommunity.sdk.di.repositoryModule
import com.octopuscommunity.sdk.domain.BridgeShareTokenProviderHolder
import com.octopuscommunity.sdk.domain.event.LifecycleEvents
import com.octopuscommunity.sdk.domain.model.ClientPost
import com.octopuscommunity.sdk.domain.model.ClientUser
import com.octopuscommunity.sdk.domain.model.CommunityAccessDeniedReason
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.ConnectionMode
import com.octopuscommunity.sdk.domain.model.OctopusCommunityData
import com.octopuscommunity.sdk.domain.model.OctopusEvent
import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.domain.model.OctopusPost
import com.octopuscommunity.sdk.domain.model.OctopusReactionKind
import com.octopuscommunity.sdk.domain.model.ProfileFieldsLock
import com.octopuscommunity.sdk.domain.model.SDKEvent
import com.octopuscommunity.sdk.domain.model.SyncFollowGroupAction
import com.octopuscommunity.sdk.domain.model.SyncFollowGroupResult
import com.octopuscommunity.sdk.domain.model.SyncFollowGroupStatus
import com.octopuscommunity.sdk.domain.model.Topic
import com.octopuscommunity.sdk.domain.model.TrackerEvent
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.getOrNull
import com.octopuscommunity.sdk.domain.network.mapSuccess
import com.octopuscommunity.sdk.domain.repository.ClientPostError
import com.octopuscommunity.sdk.domain.repository.ClientUserError
import com.octopuscommunity.sdk.domain.repository.CommentRepository
import com.octopuscommunity.sdk.domain.repository.CommunityClosureRepository
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository.OverrideCommunityAccessError
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.GroupFollowUnfollowError
import com.octopuscommunity.sdk.domain.repository.GroupRepository
import com.octopuscommunity.sdk.domain.repository.LanguageRepository
import com.octopuscommunity.sdk.domain.repository.NotificationRepository
import com.octopuscommunity.sdk.domain.repository.PostRepository
import com.octopuscommunity.sdk.domain.repository.ProfileRepository
import com.octopuscommunity.sdk.domain.repository.ReactionsRepository
import com.octopuscommunity.sdk.domain.repository.RefreshEntitlementsError
import com.octopuscommunity.sdk.domain.repository.RootFeedRepository
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import com.octopuscommunity.sdk.domain.repository.SetReactionError
import com.octopuscommunity.sdk.domain.repository.TopicRepository
import com.octopuscommunity.sdk.domain.repository.TrackerRepository
import com.octopuscommunity.sdk.domain.repository.UserCommentRepository
import com.octopuscommunity.sdk.domain.repository.UserRepository
import com.octopuscommunity.sdk.domain.usecase.ConnectUseCase
import com.octopuscommunity.sdk.domain.usecase.DisconnectUseCase
import com.octopuscommunity.sdk.domain.usecase.OverrideDefaultLocaleUseCase
import com.octopuscommunity.sdk.domain.usecase.RefreshEntitlementsUseCase
import com.octopuscommunity.sdk.domain.usecase.StartCollectingDrivenLoginUseCase
import com.octopuscommunity.sdk.domain.usecase.StartCollectingSessionsUseCase
import com.octopuscommunity.sdk.model.OctopusNotification
import com.octopuscommunity.sdk.model.getOctopusNotification
import com.octopuscommunity.sdk.model.isOctopusNotification
import com.octopuscommunity.sdk.test.di.octopusTestModule
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.error.ClosedScopeException
import org.koin.core.error.NoDefinitionFoundException
import org.koin.core.logger.Level
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * Main entry point for the Octopus SDK.
 *
 * ## Basic Usage:
 * ```kotlin
 * // Initialize the SDK
 * OctopusSDK.initialize(
 *     context = applicationContext,
 *     apiKey = "your-api-key",
 *     connectionMode = ConnectionMode.SSO()
 * )
 *
 * // Connect a user (for SSO mode)
 * val result = OctopusSDK.connectUser(user) { getTokenFromServer() }
 * ```
 *
 * ## Multi-Community Support:
 * If your app manages several communities (different API keys), use [switchCommunity] to
 * cleanly transition between them. The current user will be disconnected and all locally
 * cached data will be cleared before reinitializing with the new configuration:
 * ```kotlin
 * // Switch to another community
 * OctopusSDK.switchCommunity(
 *     context = applicationContext,
 *     apiKey = "other-community-api-key",
 *     connectionMode = ConnectionMode.SSO()
 * )
 *
 * // Then reconnect the user on the new community
 * OctopusSDK.connectUser(user) { getTokenFromServer() }
 * ```
 *
 * @see initialize
 * @see switchCommunity
 * @see stop
 */
object OctopusSDK {
    /**
     * Base path for deep links into the Octopus SDK.
     * Used as a prefix for all deep links created within the SDK.
     */
    const val INTERNAL_DEEP_LINK_BASE_PATH = "octopus-sdk://"

    /**
     * Flow that emits the SDK initialization state.
     *
     * Emits `true` when the SDK is initialized, `false` otherwise.
     * Useful for reactive UI components that need to respond to SDK initialization status.
     */
    val isInitialisedFlow = MutableStateFlow(false)

    /**
     * Whether the SDK is currently initialized.
     *
     * Returns `true` after [initialize] or [switchCommunity] has been called,
     * `false` after [stop] or before any initialization.
     *
     * @see isInitialisedFlow
     */
    val isInitialised: Boolean get() = isInitialisedFlow.value

    /**
     * Repository for managing comments within the Octopus community.
     * Provides methods for creating, reading, updating, and deleting comments.
     *
     * Only meaningful while the SDK is initialized. Accessing it earlier does **not** throw: it
     * resolves against the preview/test container ([koinTestApp]), whose fakes hold no real data.
     * After [stop], this getter throws [SDKNotInitializedException]. Call [initialize] again
     * before using SDK repositories.
     */
    val commentRepository: CommentRepository get() = dependency()

    /**
     * Repository for managing SDK configuration.
     * Handles community access rules and SDK settings.
     *
     * Only meaningful while the SDK is initialized. Accessing it earlier does **not** throw: it
     * resolves against the preview/test container ([koinTestApp]), whose fakes hold no real data.
     * After [stop], this getter throws [SDKNotInitializedException]. Call [initialize] again
     * before using SDK repositories.
     */
    val communityConfigRepository: CommunityConfigRepository get() = dependency()

    /**
     * Repository for managing connection state.
     * Handles user authentication, session management, and connectivity.
     *
     * Only meaningful while the SDK is initialized. Accessing it earlier does **not** throw: it
     * resolves against the preview/test container ([koinTestApp]), whose fakes hold no real data.
     * After [stop], this getter throws [SDKNotInitializedException]. Call [initialize] again
     * before using SDK repositories.
     */
    val connectionRepository: ConnectionRepository get() = dependency()

    /**
     * Repository for managing SDK events.
     * Provides access to the event bus for emitting and observing SDK-wide events.
     *
     * Only meaningful while the SDK is initialized. Accessing it earlier does **not** throw: it
     * resolves against the preview/test container ([koinTestApp]), whose fakes hold no real data.
     * After [stop], this getter throws [SDKNotInitializedException]. Call [initialize] again
     * before using SDK repositories.
     */
    val eventRepository: EventRepository get() = dependency()

    /**
     * Repository for managing notifications.
     * Provides functionality for registering devices and handling notification preferences.
     *
     * Only meaningful while the SDK is initialized. Accessing it earlier does **not** throw: it
     * resolves against the preview/test container ([koinTestApp]), whose fakes hold no real data.
     * After [stop], this getter throws [SDKNotInitializedException]. Call [initialize] again
     * before using SDK repositories.
     */
    val notificationRepository: NotificationRepository get() = dependency()

    /**
     * Repository for managing posts within the Octopus community.
     * Provides methods for creating, reading, updating, and deleting posts.
     *
     * Only meaningful while the SDK is initialized. Accessing it earlier does **not** throw: it
     * resolves against the preview/test container ([koinTestApp]), whose fakes hold no real data.
     * After [stop], this getter throws [SDKNotInitializedException]. Call [initialize] again
     * before using SDK repositories.
     */
    val postRepository: PostRepository get() = dependency()

    /**
     * Repository for managing user profiles.
     * Handles profile information, preferences, and user data management.
     *
     * Only meaningful while the SDK is initialized. Accessing it earlier does **not** throw: it
     * resolves against the preview/test container ([koinTestApp]), whose fakes hold no real data.
     * After [stop], this getter throws [SDKNotInitializedException]. Call [initialize] again
     * before using SDK repositories.
     */
    val profileRepository: ProfileRepository get() = dependency()

    /**
     * Repository for managing root feeds.
     * Provides access to the main content feeds and streams.
     *
     * Only meaningful while the SDK is initialized. Accessing it earlier does **not** throw: it
     * resolves against the preview/test container ([koinTestApp]), whose fakes hold no real data.
     * After [stop], this getter throws [SDKNotInitializedException]. Call [initialize] again
     * before using SDK repositories.
     */
    val rootFeedRepository: RootFeedRepository get() = dependency()

    /**
     * Repository for managing user sessions.
     * Handles app and Octopus session lifecycle, gamification state, and video playback tracking.
     *
     * Only meaningful while the SDK is initialized. Accessing it earlier does **not** throw: it
     * resolves against the preview/test container ([koinTestApp]), whose fakes hold no real data.
     * After [stop], this getter throws [SDKNotInitializedException]. Call [initialize] again
     * before using SDK repositories.
     */
    val sessionRepository: SessionRepository get() = dependency()

    /**
     * Repository for managing groups.
     * Handles categorization and organization of content by group.
     *
     * Only meaningful while the SDK is initialized. Accessing it earlier does **not** throw: it
     * resolves against the preview/test container ([koinTestApp]), whose fakes hold no real data.
     * After [stop], this getter throws [SDKNotInitializedException]. Call [initialize] again
     * before using SDK repositories.
     */
    val groupRepository: GroupRepository get() = dependency()

    /** @suppress */
    @Deprecated("Use groupRepository", ReplaceWith("groupRepository"))
    val topicRepository: TopicRepository get() = groupRepository

    /**
     * Repository for managing the current user's profile.
     * Handles current user profile information, preferences, and customization.
     *
     * Only meaningful while the SDK is initialized. Accessing it earlier does **not** throw: it
     * resolves against the preview/test container ([koinTestApp]), whose fakes hold no real data.
     * After [stop], this getter throws [SDKNotInitializedException]. Call [initialize] again
     * before using SDK repositories.
     */
    val userRepository: UserRepository get() = dependency()

    /**
     * Repository for the "Comments" profile tab: the comments and replies a member has authored, most
     * recent first.
     *
     * Only meaningful while the SDK is initialized. Accessing it earlier does **not** throw: it
     * resolves against the preview/test container ([koinTestApp]), whose fakes hold no real data.
     * After [stop], this getter throws [SDKNotInitializedException]. Call [initialize] again
     * before using SDK repositories.
     *
     * Internal SDK API (see [InternalOctopusApi]): it backs the SDK's own profile Comments tab
     * and is **not** part of the supported public API. iOS exposes no public equivalent; access
     * requires `@OptIn(InternalOctopusApi::class)`.
     */
    @InternalOctopusApi
    val userCommentRepository: UserCommentRepository get() = dependency()

    /**
     * Repository listing the members who reacted to a post, a comment or a reply, one page at a time
     * (the "Who reacted" list). Stateless: the caller keeps one cursor per reaction kind.
     *
     * Only meaningful while the SDK is initialized. Accessing it earlier does **not** throw: it
     * resolves against the preview/test container ([koinTestApp]), whose fakes hold no real data.
     * After [stop], this getter throws [SDKNotInitializedException]. Call [initialize] again
     * before using SDK repositories.
     *
     * Internal SDK API (see [InternalOctopusApi]): it backs the SDK's own "Who reacted" list and
     * is **not** part of the supported public API. iOS exposes no public equivalent; access
     * requires `@OptIn(InternalOctopusApi::class)`.
     */
    @InternalOctopusApi
    val reactionsRepository: ReactionsRepository get() = dependency()

    /**
     * Repository for managing tracking events.
     * Provides methods for logging and tracking user interactions.
     *
     * Only meaningful while the SDK is initialized. Accessing it earlier does **not** throw: it
     * resolves against the preview/test container ([koinTestApp]), whose fakes hold no real data.
     * After [stop], this getter throws [SDKNotInitializedException]. Call [initialize] again
     * before using SDK repositories.
     */
    val trackerRepository: TrackerRepository get() = dependency()

    /**
     * Repository for managing the SDK language/locale.
     * Handles locale override for all SDK operations.
     *
     * Only meaningful while the SDK is initialized. Accessing it earlier does **not** throw: it
     * resolves against the preview/test container ([koinTestApp]), whose fakes hold no real data.
     * After [stop], this getter throws [SDKNotInitializedException]. Call [initialize] again
     * before using SDK repositories.
     */
    val languageRepository: LanguageRepository get() = dependency()

    /**
     * Number of notifications from the notification center (i.e. internal notifications) that have
     * not been seen yet.
     *
     * Always 0 if the user is not connected to Octopus Community.
     *
     * Can be collected before [initialize]: nothing is emitted until the SDK is initialized, then
     * the Flow mirrors the real state and re-binds itself on [switchCommunity] and on any later
     * [initialize]. Since nothing is emitted before then, `first()` — or `stateIn` without an
     * initial value — suspends until initialization instead of returning a default.
     */
    val notSeenNotificationsCount: Flow<Int>
        get() =
            whileInitialised {
                get<ProfileRepository>()
                    .getProfileFlow()
                    .map { it?.notSeenNotificationsCount ?: 0 }
            }.distinctUntilChanged()

    /**
     * The connected user's public profile, or `null` if not connected.
     *
     * Emits updates when the profile changes — including when entitlements are refreshed via
     * [refreshEntitlements]. Mirrors iOS `OctopusSDK.profile`.
     *
     * Can be collected before [initialize]: nothing is emitted until the SDK is initialized, then
     * the Flow mirrors the real state and re-binds itself on [switchCommunity] and on any later
     * [initialize]. Since nothing is emitted before then, `first()` — or `stateIn` without an
     * initial value — suspends until initialization instead of returning a default.
     */
    val profile: Flow<OctopusProfile?>
        get() =
            whileInitialised {
                octopusProfileFlow(
                    connectionRepository = get(),
                    profileRepository = get(),
                )
            }

    /**
     * Event Flow.
     *
     * This Flow emits an event each time the user does something particular.
     * See [OctopusEvent] to have a full list of all events that can be emitted.
     *
     * If you have your own tracking/analytics service, you can use this Flow to listen to events
     * that occurred inside the Octopus UI and feed your tracking service with them.
     *
     * Example usage:
     * ```kotlin
     * scope.launch {
     *     OctopusSDK.events.collect { event ->
     *         when (event) {
     *             is OctopusEvent.PostCreated -> {
     *                 // Track post creation in your analytics
     *             }
     *             is OctopusEvent.ScreenDisplayed.MainFeed -> {
     *                 // Track screen view
     *             }
     *             // Handle other events...
     *         }
     *     }
     * }
     * ```
     *
     * Can be collected before [initialize] and re-binds itself on [switchCommunity] and on any later
     * [initialize]. Events are not replayed: only what happens while a collector is attached *and*
     * the SDK is initialized is delivered.
     */
    val events: Flow<OctopusEvent>
        get() = whileInitialised { get<EventRepository>().events.filterIsInstance<OctopusEvent>() }

    /**
     * Flow that emits the current connection state of the user.
     *
     * This Flow provides detailed information about the user's connection status.
     *
     * Possible states:
     * - [ConnectionState.NotConnected] - No user is connected to the platform
     * - [ConnectionState.Connected] - User is authenticated (check `isGuest` property to
     * distinguish between guest and regular users)
     *
     * Use this when you need to:
     * - Distinguish between guest and authenticated users via `ConnectionState.Connected.isGuest`
     * - Handle connection state changes in your UI
     *
     * For a simple boolean check of whether a non-guest user is connected, use [isUserConnected]
     * instead.
     *
     * Can be collected before [initialize]: nothing is emitted until the SDK is initialized, then
     * the Flow mirrors the real state and re-binds itself on [switchCommunity] and on any later
     * [initialize]. Since nothing is emitted before then, `first()` — or `stateIn` without an
     * initial value — suspends until initialization instead of returning a default.
     *
     * @see isUserConnected
     * @see ConnectionState
     */
    val connectionState: Flow<ConnectionState>
        get() = whileInitialised { get<ConnectionRepository>().getConnectionStateFlow() }

    /**
     * Flow that indicates whether a fully authenticated (non-guest) user is connected to the
     * Octopus platform.
     *
     * Emits `true` only when a regular authenticated user is connected.
     * Emits `false` when no user is connected OR when a guest user is connected.
     *
     * This is a simplified version of [connectionState] that filters out guest users.
     * Use this when you only care about whether a full authenticated user (not a guest) is logged
     * in.
     *
     * Can be collected before [initialize]: nothing is emitted until the SDK is initialized, then
     * the Flow mirrors the real state — a collector registered before [initialize] receives `true`
     * as soon as [connectUser] succeeds. It re-binds itself on [switchCommunity] and on any later
     * [initialize], so a single collector started at app launch spans the whole SDK lifecycle.
     *
     * @see connectionState for more granular control including guest user detection
     */
    val isUserConnected: Flow<Boolean>
        get() = connectionState.map { it is ConnectionState.Connected && !it.isGuest }

    /**
     * Flow that indicates whether the current user has access to the community features.
     *
     * This respects both the SDK's internal A/B test configuration and any status
     * set via [overrideCommunityAccess].
     *
     * Emits `true` when the user can access community features, `false` otherwise.
     *
     * Every member logout clears the cached access decision and emits `false` until the guest
     * access check returns. It remains `false` while offline or if that check fails, even if the
     * member could access the community before logout.
     * Disconnecting an SSO client user while already a guest keeps that guest's cached access.
     *
     * See [communityAccessDeniedReason] for why the access is denied.
     *
     * Can be collected before [initialize]: nothing is emitted until the SDK is initialized, then
     * the Flow mirrors the real state and re-binds itself on [switchCommunity] and on any later
     * [initialize]. Since nothing is emitted before then, `first()` — or `stateIn` without an
     * initial value — suspends until initialization instead of returning a default.
     */
    val hasAccessToCommunity: Flow<Boolean>
        get() =
            whileInitialised {
                get<UserRepository>().getUserConfigFlow().map { it?.canAccessCommunity == true }
            }

    /**
     * Flow of the reason why the current user has no access to the community.
     *
     * Emits `null` while the access is granted or not known yet, else the reason — the one the SDK
     * screen shown instead of the community is built from. While the community is closed or
     * temporarily disabled, the reason is that closure, whatever the user's own access is.
     *
     * [CommunityAccessDeniedReason.ACCOUNT_DELETED] is how the host app learns that the account of
     * its member was deleted, and can sign that member out on its side.
     *
     * Can be collected before [initialize]: nothing is emitted until the SDK is initialized, then
     * the Flow mirrors the real state and re-binds itself on [switchCommunity] and on any later
     * [initialize]. Since nothing is emitted before then, `first()` — or `stateIn` without an
     * initial value — suspends until initialization instead of returning a default.
     *
     * @see hasAccessToCommunity
     */
    val communityAccessDeniedReason: Flow<CommunityAccessDeniedReason?>
        get() =
            whileInitialised {
                // The connection repository is the one the screen reads: both show the same reason.
                get<ConnectionRepository>()
                    .getUserConfigFlow()
                    .map { it?.accessDenial?.reason }
                    .distinctUntilChanged()
            }

    /**
     * Flow of all groups available in the community.
     *
     * A Flow that emits the current list of groups available in the community.
     * Groups are used to categorize and organize content, making it easier for users to find
     * relevant posts and discussions.
     *
     * The Flow will emit updates whenever the groups list changes, such as when new groups
     * are added or existing groups are modified by community administrators.
     *
     * Can be collected before [initialize]: nothing is emitted until the SDK is initialized, then
     * the Flow mirrors the real state and re-binds itself on [switchCommunity] and on any later
     * [initialize]. Since nothing is emitted before then, `first()` — or `stateIn` without an
     * initial value — suspends until initialization instead of returning a default.
     */
    val groups: Flow<List<OctopusGroup>> get() = whileInitialised { get<GroupRepository>().getGroupsFlow() }

    @Deprecated("Use groups instead", ReplaceWith("groups"))
    val topics: Flow<List<Topic>> get() = groups

    /**
     * Koin application instance for dependency injection.
     *
     * This instance manages all the dependency injection modules and provides
     * access to registered components throughout the SDK.
     *
     * **Must not be read before [initialize].** It is `lateinit`, so reading it on an SDK that was
     * never initialized throws `UninitializedPropertyAccessException` — unlike the repository
     * getters above, there is no preview/test fallback here. Guard on [isInitialised] (or observe
     * [isInitialisedFlow]) rather than on the property itself: [initialize] assigns this container
     * before announcing the SDK as initialized, and [stop] announces the stop before closing it,
     * so [isInitialised] is `true` exactly while the container is both assigned and open. After
     * [stop] the property stays assigned and its container is closed, and resolving from it raises
     * Koin's `ClosedScopeException`.
     */
    @Volatile
    lateinit var koinApp: KoinApplication

    /**
     * Test Koin application instance used for Compose previews and testing.
     *
     * This provides a minimal Koin configuration with fake implementations
     * for UI preview and testing purposes without requiring full SDK initialization.
     */
    val koinTestApp by lazy { createTestContainer() }

    internal fun createTestContainer() = koinApplication { modules(octopusTestModule) }

    /**
     * Access to the Koin instance.
     *
     * Provides access to the dependency injection container for retrieving
     * registered components and services.
     *
     * Only meaningful while the SDK is initialized. Accessing it earlier does **not** throw: it
     * resolves against the preview/test container ([koinTestApp]), whose fakes hold no real data.
     * After [stop], reading this property still returns the last container. Resolving dependencies
     * directly from a closed Koin is unsupported; SDK repository getters instead report
     * [SDKNotInitializedException].
     */
    val koin get() = if (isKoinInitialised) koinApp.koin else koinTestApp.koin

    /**
     * An SDK-level CoroutineScope with SupervisorJob.
     *
     * This scope is used for long-running SDK operations and background tasks.
     * It uses a SupervisorJob to ensure that failure in one coroutine doesn't
     * affect others, and runs on the Default dispatcher for CPU-intensive work.
     *
     * **Must not be read before [initialize].** Like [koinApp] it is `lateinit`, so reading it on an
     * SDK that was never initialized throws `UninitializedPropertyAccessException`. Guard on
     * [isInitialised] (or observe [isInitialisedFlow]) rather than on the property itself:
     * [initialize] assigns this scope before announcing the SDK as initialized, so [isInitialised]
     * being `true` always implies an assigned scope. After [stop] the property stays assigned and
     * its job is cancelled, so launching into it does not throw — it simply never runs the
     * coroutine, which a `lateinit` check would not catch.
     */
    @Volatile
    lateinit var sdkScope: CoroutineScope

    /**
     * List of application-provided deep link base paths.
     *
     * These paths are provided during initialization and are used in combination
     * with the SDK's internal deep link path for routing purposes.
     */
    var appDeepLinkBasePaths: List<String> = emptyList()

    private const val LOG_TAG = "OctopusSDK"

    @OptIn(InternalOctopusApi::class)
    private val lifecycleLock = OctopusLifecycle.lock
    private val initializationLock = Any()
    private var lifecycleVersion = 0L
    private var buildingScope: CoroutineScope? = null
    private val storageCleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile internal var lastRetirement: Deferred<Unit>? = null

    @Volatile internal var lastStoragePurge: Job? = null

    @Volatile internal var initializationRetry: Job? = null

    // Once cache clearing starts, replacements must wait for its IO writes before closing the
    // captured graph or reopening the same storage. Event delivery is outside this barrier.
    private var resetCleanup: Deferred<Unit>? = null

    private inline fun <T> withLifecycleLock(block: () -> T): T {
        while (true) {
            val cleanup =
                synchronized(lifecycleLock) {
                    val pending = resetCleanup
                    if (pending == null || pending.isCompleted) {
                        resetCleanup = null
                        return block()
                    }
                    pending
                }
            // Lifecycle entry points are synchronous. Cleanup runs independently on IO and never
            // needs this thread or the lifecycle monitor to finish.
            runBlocking { cleanup.join() }
        }
    }

    private data class InitializationConfiguration(
        val context: Context,
        val apiKey: String,
        val connectionMode: ConnectionMode,
        val deepLinksBasePaths: List<String>,
        val serverHost: String,
        val serverPort: Int,
    )

    private var initializationConfiguration: InitializationConfiguration? = null

    private val isKoinInitialised get() = this::koinApp.isInitialized

    /**
     * The container the lifecycle Flows currently resolve from, or `null` while the SDK is stopped.
     *
     * Deliberately keyed on the *container*, not on [isInitialisedFlow]. A `StateFlow<Boolean>`
     * conflates by equality, so a `stop()` immediately followed by an `initialize()` would write
     * `false` then `true` with no suspension point in between, and a collector already sitting on
     * `true` would observe no change at all, staying bound to a container that has since been
     * closed. That stretch is [initialize]'s own `if (isInitialised) stop()` and the container
     * rebuild right after it — nothing suspends between the two — which is also the path
     * [switchCommunity] takes, its own `reset()` having left the SDK initialized. An [initialize]
     * that changes configuration builds a fresh [KoinApplication] and publishes its new identity;
     * an identical initialization preserves the current container and its collectors.
     *
     * `internal` rather than `private` only so the module's unit tests can drive a stop/restart
     * without an Android `Context`; it is not part of the published surface.
     */
    internal val activeKoin = MutableStateFlow<Koin?>(null)

    /**
     * The FCM registration token handed to [registerNotificationsToken] before the SDK was ready,
     * waiting for the next [initialize] to send it.
     *
     * Holds at most one token, and the newest one wins: FCM issues a single current registration
     * token per app instance, so a token queued behind a newer one is already stale. `null` means
     * nothing is waiting. A token stays queued until registration succeeds, so cancellation during
     * [stop] leaves it available for the next initialization.
     *
     * An `AtomicReference` rather than a plain field because the two sides genuinely run on
     * different threads: `FirebaseMessagingService.onNewToken` delivers on Firebase's own executor
     * while [initialize] is typically called from the main thread.
     *
     * `internal` rather than `private` only so the module's unit tests can observe the queue without
     * an Android `Context`; it is not part of the published surface.
     */
    internal val pendingNotificationsToken = AtomicReference<String?>(null)

    /**
     * Emits the SDK's "you called this before `initialize()`" warnings.
     *
     * A replaceable seam rather than a direct `Log.w` call, because `android.util.Log` is a stub
     * that throws `RuntimeException("Method w in android.util.Log not mocked")` in a plain JVM unit
     * test, and this module's `src/test` is pure JVM by convention. Without the seam the pre-init
     * entry points could not be called from a test at all — that is, the exact regression this
     * guards could not be pinned.
     *
     * `internal`, so it stays off the published API surface.
     */
    internal var logWarning: (String) -> Unit = { message -> Log.w(LOG_TAG, message) }

    /**
     * Sends the token [registerNotificationsToken] queued, if any, and clears it only after
     * successful registration. Cancellation or failure leaves it queued for the next initialization;
     * a newer token received during registration is never cleared by the older registration.
     *
     * Called from [initialize] *before* `UserRepository.checkPushNotificationsToken()`, which is
     * what makes the queue cost nothing: registering writes the token to the secured data store,
     * and `checkPushNotificationsToken` then reads back that same value and finds the registration
     * payload unchanged, so it does not hit the network a second time.
     *
     * `internal` rather than `private` so the module's unit tests can drive the flush without an
     * Android `Context`.
     */
    internal suspend fun flushPendingNotificationsToken(repository: UserRepository = userRepository) {
        val token = pendingNotificationsToken.get() ?: return
        val result = repository.registerPushNotificationsToken(token)
        currentCoroutineContext().ensureActive()
        if (result is OctopusResult.Success) pendingNotificationsToken.compareAndSet(token, null)
    }

    /**
     * Publishes [koin] as the container the lifecycle Flows resolve from, then announces the SDK as
     * initialized.
     *
     * The order is deliberate: a host observing [isInitialisedFlow] may resolve a lifecycle Flow the
     * moment it turns `true`, and that Flow binds through [activeKoin]. Announcing first would give
     * that host a Flow that emits nothing until the very next instruction — `whileInitialised` is a
     * `flatMapLatest` on [activeKoin], so it does re-bind on its own; the window is transient, not
     * fatal. Publishing first removes it entirely, and costs nothing.
     *
     * Split out of [initialize] — taking the container as a parameter rather than building it — so
     * the ordering is exercisable from a plain JVM unit test, which cannot reach [initialize] itself
     * (it needs an Android `Context` and a live Room/DataStore/gRPC stack). `internal`, so it stays
     * off the published API surface — like [activeKoin] itself, its JVM name is mangled and it is
     * absent from `api/octopus-sdk.api`.
     *
     * @param koin the freshly built container the lifecycle Flows should resolve from.
     *
     * @see retireContainer
     */
    internal fun publishContainer(koin: Koin) {
        activeKoin.value = koin
        isInitialisedFlow.update { true }
    }

    /**
     * Unbinds the lifecycle Flows from the current container and announces the SDK as stopped, then
     * releases that container's resources through [releaseContainer].
     *
     * Both announcements happen before [releaseContainer], and that is the whole point of this
     * function: a collector still bound when the container closes would resolve from a closed Koin
     * and get Koin's own `ClosedScopeException`. Between the two announcements the clear comes first,
     * mirroring [publishContainer] — a host reacting to [isInitialisedFlow] turning `false` should not
     * find a container still published behind it. `SDKEvent.Stopped` is emitted by [stop] *before*
     * this call, while the container is still live, and it is not an [OctopusEvent] — so the public
     * [events] Flow is unaffected either way.
     *
     * [releaseContainer] is injected rather than reached for directly so a plain JVM unit test can
     * observe [activeKoin] at the exact moment the container would be closed; [stop] passes the real
     * scope cancellation and `koinApp.close()`. `internal`, so it stays off the published API
     * surface, exactly as [publishContainer] does.
     *
     * @param releaseContainer releases the container's resources — runs last, once nothing is bound
     * to it any more.
     *
     * @see publishContainer
     */
    internal fun retireContainer(releaseContainer: () -> Unit) {
        activeKoin.value = null
        isInitialisedFlow.update { false }
        releaseContainer()
    }

    /**
     * Binds a lifecycle Flow to the SDK's active container instead of resolving its source right
     * away.
     *
     * Resolving a repository only makes sense while the SDK is initialized: before the first
     * [initialize] call [koin] falls back to the preview fakes, and after [stop] the real container
     * is closed. A getter that resolves eagerly therefore captures the wrong source for the whole
     * lifetime of the collection — a collector registered before [initialize] observes the fakes'
     * state forever and never sees the connected user.
     *
     * Deferring the resolution until a container is published, and re-resolving it against the new
     * one on every re-initialization ([switchCommunity]), means a collector can be registered at any
     * time: it emits nothing while the SDK is not ready, then mirrors the real state. [source]
     * receives that container as its receiver rather than reading [koin] again, so the Flow is bound
     * to the exact container the emission announced.
     *
     * This covers the *lifecycle* Flows only — the SDK-global state a host observes from app launch.
     * The id-scoped query Flows ([getClientObjectRelatedPostFlow], [communityDataFlow],
     * [communityDataFlowByClientUserId]) are deliberately left resolving eagerly: they are called
     * from a screen inside an already-initialized SDK, they must keep resolving the fakes so Compose
     * previews and the sample's test mode work, and [communityDataFlowByClientUserId] is documented
     * to *complete* when the lookup resolves no member — a gate built on a `StateFlow` never
     * completes.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> whileInitialised(source: Koin.() -> Flow<T>): Flow<T> =
        activeKoin.flatMapLatest { container ->
            try {
                if (container != null && activeKoin.value === container) container.source() else emptyFlow()
            } catch (failure: org.koin.core.error.ClosedScopeException) {
                if (activeKoin.value === container) throw failure else emptyFlow()
            }
        }

    @Volatile private var stoppedKoin: Koin? = null
    private var retiringClient: OctopusGrpcClient? = null // Guarded by lifecycleLock.

    @OptIn(org.koin.core.annotation.KoinInternalApi::class)
    private fun dependencyContainer(): Koin {
        val container = if (isKoinInitialised) koinApp.koin else koinTestApp.koin
        // Keep open, explicitly injected test graphs usable before initialize(). An actual stop
        // rejects access even while a final UI frame still holds the retired graph open.
        if (container === stoppedKoin || container.scopeRegistry.rootScope.closed) throw SDKNotInitializedException()
        return container
    }

    @Volatile private var containerDependencies: Pair<Koin, ContainerDependencies>? = null

    private inline fun <reified T : Any> dependency(): T {
        val snapshot = containerDependencies
        if (snapshot != null && snapshot.first !== stoppedKoin) return snapshot.second.get(T::class)
        val container = dependencyContainer()
        return try {
            container.get<T>()
        } catch (failure: org.koin.core.error.ClosedScopeException) {
            throw SDKNotInitializedException()
        } catch (failure: org.koin.core.error.InstanceCreationException) {
            if (generateSequence<Throwable>(failure) { it.cause }.any { it is org.koin.core.error.ClosedScopeException }) {
                throw SDKNotInitializedException()
            }
            throw failure
        }
    }

    private val grpcClient: OctopusGrpcClient get() = dependency()

    private val connectUseCase: ConnectUseCase get() = dependency()
    private val disconnectUseCase: DisconnectUseCase get() = dependency()
    private val preferencesDataStore: PreferencesDataStore get() = dependency()

    private val overrideDefaultLocaleUseCase: OverrideDefaultLocaleUseCase get() = dependency()
    private val refreshEntitlementsUseCase: RefreshEntitlementsUseCase get() = dependency()

    /**
     * Initializes the Octopus SDK with required configuration.
     *
     * This method must be called before using any other SDK functionality.
     * It is recommended to call it as early as possible in the application lifecycle,
     * typically in `Application.onCreate()`.
     *
     * If the SDK is already initialized with the same API key and configuration, this method
     * does nothing and preserves the running session. Changing the configuration stops the current
     * instance before reinitializing. If you need to switch to a different community
     * (different API key) at runtime, prefer using [switchCommunity]. A host that changes its
     * API key automatically starts with fresh user, session, content, configuration and image
     * storage before startup. A first run without a stored fingerprint preserves existing data.
     *
     * The container is available synchronously when this method returns. The key check reads only
     * small installation metadata. A changed key selects isolated storage and removes the old
     * fixed set of storage files; image-file deletion continues in the background. If cleanup
     * fails, the new community remains isolated and a later initialization retries metadata.
     * If a [reset] cleanup is in progress, this call waits for its IO writes before replacing
     * the container. It does not wait for reset event delivery.
     *
     * Initialization failures are logged and leave [isInitialised] false.
     * Observe [isInitialisedFlow] to wait for readiness. If module isolation is unavailable in the host's
     * Koin version, original modules are used; a replacement can finish asynchronously after the
     * previous graph closes, while [isInitialised] remains false until publication.
     * Incompatible Koin runtimes are diagnosed before graph creation. The declared application
     * process is always allowed to use SDK storage. Other processes require exclusive storage
     * ownership; if refused, a later [initialize] call can succeed. An unavailable process name or storage
     * lock failure logs a warning and allows initialization.
     *
     * @param context Application context used for Android-specific functionality
     * @param apiKey The API key that identifies your community
     * @param connectionMode The required host authentication configuration ([ConnectionMode.SSO]).
     * @param deepLinksBasePaths List of deep link base paths for routing within your application
     * @param apiServer Optional server endpoint the SDK targets. When `null` (default), the SDK
     * uses the Octopus default endpoint. When set, the SDK routes gRPC traffic to the provided
     * host and port over TLS. See [ApiServer] for the validation rules.
     *
     * @see switchCommunity
     */
    @OptIn(InternalOctopusApi::class)
    fun initialize(
        context: Context,
        apiKey: String,
        connectionMode: ConnectionMode,
        deepLinksBasePaths: List<String> = emptyList(),
        apiServer: ApiServer? = null,
    ) {
        val requestVersion = synchronized(lifecycleLock) { lifecycleVersion }
        try {
            initializeCommunity(context, apiKey, connectionMode, deepLinksBasePaths, apiServer)
        } catch (waiting: OctopusLifecycle.AwaitModuleRetirement) {
            val version = waiting.expectedVersion ?: requestVersion
            initializationRetry =
                storageCleanupScope.launch {
                    waiting.retirement.join()
                    try {
                        initializeCommunity(context, apiKey, connectionMode, deepLinksBasePaths, apiServer, expectedVersion = version)
                    } catch (_: IntegrationRefused) {
                        logWarning("SDK initialization refused by the host integration guard; see the preceding error")
                    } catch (_: StaleInitialization) {
                        // A later stop or initialize superseded this deferred compatibility retry.
                    } catch (failure: Exception) {
                        logWarning("SDK initialization failed (${failure.javaClass.simpleName})")
                    } catch (failure: LinkageError) {
                        logWarning("SDK dependency compatibility failure (${failure.javaClass.simpleName})")
                    }
                }
        } catch (_: IntegrationRefused) {
            logWarning("SDK initialization refused by the host integration guard; see the preceding error")
        } catch (failure: Exception) {
            // Eager graph validation moves constructor work out of the startup coroutine. Preserve
            // initialize's host contract: report startup failure without a new synchronous throw.
            logWarning("SDK initialization failed (${failure.javaClass.simpleName})")
        } catch (failure: LinkageError) {
            logWarning("SDK dependency compatibility failure (${failure.javaClass.simpleName})")
        }
    }

    private class StaleInitialization : RuntimeException()

    private class IntegrationRefused : RuntimeException()

    private fun initializeCommunity(
        context: Context,
        apiKey: String,
        connectionMode: ConnectionMode,
        deepLinksBasePaths: List<String>,
        apiServer: ApiServer?,
        resetContainer: Koin? = null,
        expectedVersion: Long? = null,
    ): Koin {
        if (!HostIntegrationGuard.allowsRuntime()) throw IntegrationRefused()
        val (resolvedHost, resolvedPort) =
            resolveServerEndpoint(
                apiServer = apiServer,
                fallbackHost = BuildConfig.OCTOPUS_SERVER_HOST,
                fallbackPort = BuildConfig.OCTOPUS_SERVER_PORT,
            )
        val root = context.applicationContext ?: context
        val configuration =
            InitializationConfiguration(
                context = root,
                apiKey = apiKey,
                connectionMode =
                    when (connectionMode) {
                        is ConnectionMode.SSO -> connectionMode.copy(appManagedFields = connectionMode.appManagedFields.toSet())
                    },
                deepLinksBasePaths = deepLinksBasePaths.toList(),
                serverHost = resolvedHost,
                serverPort = resolvedPort,
            )
        var prepared = ApiKeyStore.Prepared("", false)
        var alreadyReset = false
        var startupDependencies: ContainerDependencies? = null
        // Retain this receiver so JVM lifecycle tests can substitute only the Android factory.
        val sdk = this
        val (initializedScope, initializedKoin) =
            synchronized(initializationLock) {
                // Only the graph actually wiped by switchCommunity counts as reset.
                // A replacement may have written new user data while reset delivered its event.
                alreadyReset = resetContainer != null && sdk.isInitialised && sdk.koinApp.koin === resetContainer
                if (!alreadyReset && sdk.isInitialised && initializationConfiguration == configuration) {
                    return sdk.koinApp.koin
                }
                val build: (List<String>, () -> Unit, (CoroutineScope) -> KoinApplication) -> Pair<CoroutineScope, Koin> =
                    if (expectedVersion == null) {
                        sdk::initializeContainer
                    } else {
                        { paths, prepare, create ->
                            sdk.initializeContainerForVersion(paths, prepare, expectedVersion, create)
                        }
                    }
                build(configuration.deepLinksBasePaths, {
                    // The reset graph can still write after its stores were cleared. A key
                    // change must retire that graph and select isolated storage regardless.
                    prepared = prepareCommunityStorage(root, apiKey)
                }, { newScope ->
                    koinApplication {
                        octopusSdkModules(
                            context = CommunityStorageContext.forStorage(root, prepared.storageId),
                            sdkScope = newScope,
                            serverHost = resolvedHost,
                            serverPort = resolvedPort,
                            linksHost = BuildConfig.OCTOPUS_LINKS_HOST,
                            apiKey = apiKey,
                            connectionMode = configuration.connectionMode,
                        )
                    }
                }).also {
                    initializationConfiguration = configuration
                    startupDependencies = sdk.containerDependencies?.second
                }
            }
        // Keep both the scope and dependencies attached to this call's lifecycle. A replacement
        // cancels this scope, but synchronous steps already running can still resolve dependencies.
        initializedScope.launchUntilStopped {
            runStartupSteps(
                startupDependencies?.get(ConnectionRepository::class) ?: initializedKoin.get(),
                startupDependencies?.get(UserRepository::class) ?: initializedKoin.get(),
                startupDependencies?.get(EventRepository::class) ?: initializedKoin.get(),
                configuration.connectionMode,
            )

            if (prepared.changed && !alreadyReset) {
                (
                    startupDependencies?.get(
                        CommunityConfigRepository::class,
                    ) ?: initializedKoin.get<CommunityConfigRepository>()
                ).fetchCommunityConfig()
            }
            (startupDependencies?.get(StartCollectingSessionsUseCase::class) ?: initializedKoin.get<StartCollectingSessionsUseCase>())(
                (startupDependencies?.get(LifecycleEvents::class) ?: initializedKoin.get<LifecycleEvents>()).getEventsFlow().onStart {
                    // Collection starts only after the use case has stopped previous-process sessions.
                    (
                        startupDependencies?.get(
                            ClientABTestTracker::class,
                        ) ?: initializedKoin.get<ClientABTestTracker>()
                    ).onSessionsRecovered()
                },
            )
        }
        // Separate coroutine: a purge that did not end with the previous run (killed, or failed) must not leave the
        // content of a closed community behind. Off the startup path, which it would hold for as long as it runs.
        // The closure repository is resolved in each coroutine, as every dependency here: a stop() racing this
        // initialization closes the container, and launchUntilStopped is what tells that from a failure. It is already
        // built by the container initialization, which armed the gate before publishing the container.
        initializedScope.launchUntilStopped { initializedKoin.get<CommunityClosureRepository>().resumeInterruptedPurge() }
        // Separate coroutine: once the community reopens, load what the closure skipped without waiting for a launch.
        initializedScope.launchUntilStopped {
            resumeWhenTheCommunityReopens(initializedKoin.get<CommunityClosureRepository>().closureFlow) {
                resumeAfterCommunityReopening(
                    connectionRepository = initializedKoin.get(),
                    userRepository = initializedKoin.get(),
                    groupRepository = initializedKoin.get(),
                    rootFeedRepository = initializedKoin.get(),
                    flushPendingNotificationsToken = { flushPendingNotificationsToken(initializedKoin.get()) },
                )
            }
        }
        // Separate coroutine, for the same reason as below: a closed community is checked again when the app comes back.
        initializedScope.launchUntilStopped {
            checkClosedCommunityWhenTheAppComesBack(
                initializedKoin.get<EventRepository>().getEventsFlow(),
                initializedKoin.get<CommunityClosureRepository>(),
                initializedKoin.get(),
            )
        }
        // Separate coroutine, for the same reason as below: a community closed while the app runs first shows as calls
        // the server refuses because of the closure. A container built from repository fakes (tests, previews) has no
        // gRPC client, so no call to watch.
        initializedScope.launchUntilStopped {
            val grpcClient = initializedKoin.getOrNull<OctopusGrpcClient>() ?: return@launchUntilStopped
            checkTheConfigWhenTheServerNamesAClosure(
                grpcClient.communityClosedRefusals,
                initializedKoin.get<CommunityClosureRepository>(),
                initializedKoin.get(),
            )
        }
        // Separate coroutine, for the same reason as below: an access taken away while the app runs (a ban, an
        // exclusion) first shows as calls the server refuses, naming the user's own reason.
        initializedScope.launchUntilStopped {
            val grpcClient = initializedKoin.getOrNull<OctopusGrpcClient>() ?: return@launchUntilStopped
            readTheAccessWhenTheServerRefusesTheUser(
                grpcClient.accessRefusals,
                initializedKoin.get<CommunityClosureRepository>(),
                initializedKoin.get<UserRepository>(),
                sessionEndedRefusals = grpcClient.sessionEndedRefusals,
            )
        }
        // Separate coroutine: the sessions collector above suspends for the whole app run, so this
        // must not be chained after it. Emits `octopus_driven_login` when Octopus drives a login.
        initializedScope.launchUntilStopped {
            (
                startupDependencies?.get(
                    StartCollectingDrivenLoginUseCase::class,
                ) ?: initializedKoin.get<StartCollectingDrivenLoginUseCase>()
            )()
        }
        return initializedKoin
    }

    private fun prepareCommunityStorage(
        context: Context,
        apiKey: String,
    ): ApiKeyStore.Prepared {
        if (!HostIntegrationGuard.allowsStorage(context)) throw IntegrationRefused()
        return try {
            ApiKeyStore.forContext(context, logWarning).prepare(apiKey) { previous, replacement ->
                if (isInitialised) {
                    try {
                        if (runBlocking { connectionRepository.getUser() } != null) {
                            eventRepository.tryEmitEvent(SDKEvent.Disconnected)
                        }
                    } catch (failure: Exception) {
                        logWarning("Unable to read previous connection (${failure.javaClass.simpleName})")
                    }
                    stop()
                }
                val oldStorage = CommunityStorageContext.forStorage(context, previous)
                val newStorage = CommunityStorageContext.forStorage(context, replacement)
                retainCommunityInstallationIdentity(oldStorage, newStorage)
                val closing = lastRetirement
                lastStoragePurge =
                    storageCleanupScope.launch {
                        try {
                            closing?.join()
                            purgeCommunityStorage(oldStorage, newStorage, logWarning, retainInstallationIdentity = false)
                        } catch (failure: Exception) {
                            logWarning("Unable to purge retired community storage (${failure.javaClass.simpleName})")
                        }
                    }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            logWarning("Unable to read installation metadata (${failure.javaClass.simpleName}); using isolated storage")
            ApiKeyStore.Prepared(
                java.util.UUID
                    .randomUUID()
                    .toString(),
                true,
            )
        }
    }

    /**
     * The network-facing part of [initialize]'s startup: connects a first-launch guest, sends the
     * token queued by [registerNotificationsToken], checks the stored token and announces
     * [SDKEvent.Initialized].
     *
     * Takes its repositories as parameters, and is `internal`, so a plain JVM unit test can pin that
     * the queued token is flushed *before* `checkPushNotificationsToken()` — [initialize] itself
     * needs an Android `Context`. Off the published API surface.
     */
    internal suspend fun runStartupSteps(
        connectionRepository: ConnectionRepository,
        userRepository: UserRepository,
        eventRepository: EventRepository,
        connectionMode: ConnectionMode,
    ) {
        if (connectionRepository.getUser() == null) {
            // First launch
            connectionRepository.connectAsGuest()
        }
        // Sends whatever `registerNotificationsToken` was handed before the SDK was ready, and
        // does so *before* `checkPushNotificationsToken()` so the latter reads the freshly
        // stored token back and skips a second network call. See
        // `flushPendingNotificationsToken`.
        flushPendingNotificationsToken(userRepository)
        userRepository.checkPushNotificationsToken()
        eventRepository.emitEvent(SDKEvent.Initialized(connectionMode))
    }

    /** Builds and publishes a lifecycle, returning its scope and Koin so startup cannot use a replacement. */
    @OptIn(InternalOctopusApi::class)
    internal fun initializeContainer(
        deepLinksBasePaths: List<String> = emptyList(),
        prepare: () -> Unit = {},
        createContainer: (CoroutineScope) -> KoinApplication,
    ): Pair<CoroutineScope, Koin> = initializeContainerForVersion(deepLinksBasePaths, prepare, null, createContainer)

    @OptIn(InternalOctopusApi::class)
    private fun initializeContainerForVersion(
        deepLinksBasePaths: List<String>,
        prepare: () -> Unit,
        expectedVersion: Long?,
        createContainer: (CoroutineScope) -> KoinApplication,
    ): Pair<CoroutineScope, Koin> =
        synchronized(initializationLock) {
            synchronized(lifecycleLock) {
                if (expectedVersion != null && lifecycleVersion != expectedVersion) throw StaleInitialization()
            }
            prepare()
            if (isInitialised) stop()
            val version =
                synchronized(lifecycleLock) {
                    if (expectedVersion != null && lifecycleVersion != expectedVersion) throw StaleInitialization()
                    ++lifecycleVersion
                }
            runBlocking { resetCleanup?.join() }
            runBlocking { lastRetirement?.takeIf { it.isCompleted }?.join() }
            run {
                val handler =
                    CoroutineExceptionHandler { _, failure ->
                        logWarning("SDK background work failed (${failure.javaClass.simpleName})")
                    }
                val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)
                synchronized(lifecycleLock) { if (lifecycleVersion == version) buildingScope = newScope else newScope.cancel() }
                val newContainer =
                    try {
                        createContainer(newScope).also { container ->
                            try {
                                container.koin.getOrNull<ContainerDependencies>()?.resolveDeferredEdges()
                                armCallsGate(container)
                            } catch (failure: Throwable) {
                                newScope.cancel()
                                storageCleanupScope.launch { container.close() }
                                throw failure
                            }
                        }
                    } catch (error: Throwable) {
                        newScope.cancel()
                        if (error is OctopusLifecycle.AwaitModuleRetirement) error.expectedVersion = version
                        throw error
                    }
                // A caller may have passed isInitialised just before stop(). Keep its old, cancelled
                // scope visible until the replacement container is ready. The volatile scope write also
                // publishes koinApp to a worker reading the new scope before resolving dependencies.
                synchronized(lifecycleLock) {
                    if (lifecycleVersion != version) {
                        newScope.cancel()
                        storageCleanupScope.launch { newContainer.close() }
                        return newScope to newContainer.koin
                    }
                    buildingScope = null
                    koinApp = newContainer
                    stoppedKoin = null
                    retiringClient = null
                    sdkScope = newScope
                    containerDependencies = newContainer.koin.getOrNull<ContainerDependencies>()?.let { newContainer.koin to it }
                    appDeepLinkBasePaths = deepLinksBasePaths
                    OctopusLifecycle.publish(newContainer, newScope)
                    publishContainer(newContainer.koin)
                    return newScope to newContainer.koin
                }
            }
        }

    /**
     * Builds the closure of the community in [container], which blocks the calls at once when a previous run stored one:
     * before the container is published, so that nothing can send a call through it first. A container without one
     * (tests, previews) has no call to block. A failure is cleaned up by the caller, as one of the deferred edges.
     */
    private fun armCallsGate(container: KoinApplication) {
        container.koin.getOrNull<CommunityClosureRepository>()
    }

    /**
     * Launches [block] in this scope, dropping closed-scope or missing-definition errors from Koin
     * when [stop] has cancelled this coroutine.
     *
     * The init collectors resolve their dependencies from the container *inside* the coroutine, on a
     * `Dispatchers.Default` worker. [stop] — called directly, through [switchCommunity], or through a
     * second [initialize] — cancels [sdkScope] and then closes the container, and neither interrupts
     * a resolution already running on that worker: cancellation only lands at the next suspension
     * point, and Koin resolves synchronously. The resolution then hits the closed root scope, and
     * since [sdkScope] carries no exception handler, the exception reached the host's
     * uncaught-exception handler and crashed the app. Koin wraps it in an
     * `InstanceCreationException` per definition on the way up, hence the walk down the cause chain.
     * A lookup already past the closed-scope check can instead miss a definition cleared by close.
     * The same holds for the background work of [registerNotificationsToken] and
     * [trackAccessToCommunity], which go through this helper too.
     *
     * Dropped only once this coroutine is no longer active, that is once [stop] has cancelled it: the
     * SDK is being torn down and the work was about to be abandoned anyway. A closed container seen
     * by a coroutine that is still active would be a real bug, so it is rethrown, as is any other
     * failure.
     *
     * `internal` rather than `private` so the module's unit tests can drive the race without an
     * Android `Context`.
     */
    internal fun CoroutineScope.launchUntilStopped(block: suspend CoroutineScope.() -> Unit): Job =
        launch {
            try {
                block()
            } catch (e: Exception) {
                val closedByStop =
                    !isActive &&
                        generateSequence<Throwable>(e) { it.cause }
                            .any { it is ClosedScopeException || it is NoDefinitionFoundException }
                if (!closedByStop) throw e
            }
        }

    /**
     * Connects a user to the Octopus platform.
     *
     * It authenticates a user with the Octopus platform and establishes their session.
     *
     * @param user The user currently connecting to the platform. `null` to disconnect the Octopus
     * user
     * @param tokenProvider Block called when a user token is needed to authenticate the user.
     * When receiving this callback, you should get a token for this user asynchronously and
     * return it from the lambda. This function is suspended so you can call your server
     * to retrieve the user token.
     *
     * @return [OctopusResult] indicating success or failure of the connection attempt
     */
    suspend fun connectUser(
        user: ClientUser?,
        tokenProvider: suspend () -> String,
    ): OctopusResult<ConnectionState, ClientUserError> =
        if (user != null) {
            connectUseCase(clientUser = user, tokenProvider = tokenProvider)
                .mapSuccess { it.connectionState }
        } else {
            disconnectUser()
            OctopusResult.Success(ConnectionState.NotConnected)
        }

    /**
     * Refreshes the connected user's entitlements with the Octopus backend.
     *
     * Call this when the user's entitlements change in your app (e.g. they upgrade to premium,
     * or you grant them an internal role). The SDK will re-invoke the `tokenProvider` callback
     * registered at [connectUser] time to obtain a fresh client-signed JWT (which should now
     * carry the updated `entitlements` claim), exchange it for a new Octopus JWT, update the
     * cached profile, and refetch the groups so observing screens recompose with the
     * new access decisions returned by the backend.
     *
     * Only supported in [ConnectionMode.SSO] with a `tokenProvider` registered at
     * [connectUser] time. The user must also be connected (non-guest).
     *
     * @return [OctopusResult.Success] on success, or [OctopusResult.Failure.InvalidArguments]
     * wrapping a [RefreshEntitlementsError] — exhaustive switch covers
     * [RefreshEntitlementsError.NoClientTokenProvider],
     * [RefreshEntitlementsError.UserNotConnected],
     * [RefreshEntitlementsError.NoNetwork],
     * [RefreshEntitlementsError.UserBanned] (BE-provided message, suitable for direct
     * display) and [RefreshEntitlementsError.ServerError].
     */
    suspend fun refreshEntitlements(): OctopusResult<Unit, RefreshEntitlementsError> = refreshEntitlementsUseCase()

    private var _groupAccessDeniedCallback: ((groupId: String) -> Unit)? = null

    /**
     * Callback invoked when the connected user attempts to interact with a group they do
     * not have access to (taps a visible-but-locked group, its follow button, or the locked
     * Post/Comment detail CTA). Set via [setGroupAccessDeniedCallback]. The SDK never
     * navigates on the user's behalf — the host app decides what to do (open an upsell, a
     * paywall, …).
     *
     * Mirrors iOS `OctopusSDK.groupAccessDeniedCallback`. Used as a fallback by
     * `octopusComposables(... onGroupAccessDenied = null)`: if no per-screen callback is
     * passed, the SDK-level callback set here is invoked instead.
     */
    val groupAccessDeniedCallback: ((groupId: String) -> Unit)?
        get() = _groupAccessDeniedCallback

    /**
     * Sets the SDK-level [groupAccessDeniedCallback]. Mirrors iOS
     * `OctopusSDK.set(groupAccessDeniedCallback:)`.
     *
     * Host apps that integrate via `octopusComposables(...)` can either pass an
     * `onGroupAccessDenied` per-screen callback OR set a single SDK-level callback once via
     * this method — the per-screen callback takes precedence when both are provided.
     */
    fun setGroupAccessDeniedCallback(callback: (groupId: String) -> Unit) {
        _groupAccessDeniedCallback = callback
    }

    /**
     * Internal plumbing — **not a host-facing API**. Wires the callback that signs a prefilled
     * (Bridge / Share-in-game) post for the current editor session so the server accepts its image
     * in a community configured to forbid member pictures.
     *
     * Hosts supply this through
     * [com.octopuscommunity.sdk.domain.model.CreatePostScreenInfo.bridgeShareTokenProvider] when
     * opening the editor; the create-post entry points
     * ([com.octopuscommunity.sdk.ui.navigateToOctopusCreatePost] /
     * `OctopusCreatePostScreen`) forward it here — a `suspend` lambda can't ride the type-safe nav
     * route, so the holder is the channel. The data layer reads it at publish time only when the
     * creation source is a prefilled client share carrying media. Pass `null` to clear it (every
     * editor open sets it, so a non-share editor resets any previous provider).
     */
    @InternalOctopusApi
    fun setBridgeShareTokenProvider(provider: (suspend (bridgeFingerprint: String) -> String?)?) {
        if (provider == null) {
            val snapshot = containerDependencies
            if (snapshot != null) {
                snapshot.second.get(BridgeShareTokenProviderHolder::class).tokenProvider = null
            } else if (isInitialised && isKoinInitialised && koinApp.koin !== stoppedKoin) {
                try {
                    dependency<BridgeShareTokenProviderHolder>().tokenProvider = null
                } catch (
                    _: SDKNotInitializedException,
                ) {
                    // A concurrent stop already released it.
                }
            }
        } else {
            dependency<BridgeShareTokenProviderHolder>().tokenProvider = provider
        }
    }

    /**
     * Disconnects the current user from the Octopus platform.
     * Clears cached user content and SDK images when ending a member session.
     *
     * An existing guest keeps its credentials, profile, cached content and community access.
     * In SSO mode, a retained client user and token provider are forgotten; client-user preferences
     * and SDK image caches are cleared. A guest without a client user is unchanged.
     *
     * Call this when the user logs out of your application or their session should be terminated.
     */
    suspend fun disconnectUser() {
        disconnectUseCase()
    }

    /**
     * Disconnects the current user and clears all locally cached data and images.
     *
     * This ends the user's active session, removes all data stored by the SDK on the device
     * (user data, community content, cached images), and returns the SDK to a clean state.
     * The SDK remains initialized after this call.
     *
     * You typically don't need to call this directly — [switchCommunity] already handles it.
     * Use this if you need to fully reset without switching community (e.g. for a full
     * app reset).
     *
     * @see switchCommunity
     */
    suspend fun reset() {
        resetCommunity()
    }

    private suspend fun resetCommunity(): Koin? {
        val (container, events, cleanup) =
            synchronized(lifecycleLock) {
                if (!isInitialised) return null
                // A later identical initialize must rebuild and rerun startup after this reset.
                initializationConfiguration = null
                val container = koinApp.koin
                val events = eventRepository
                val cacheClearer = containerDependencies?.takeIf { it.first === container }?.second?.get(UserCacheClearer::class)
                val lifecycle = containerDependencies?.takeIf { it.first === container }?.second?.get(LifecycleEvents::class)
                val previous = resetCleanup?.takeUnless { it.isCompleted }
                val cleanup =
                    sdkScope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
                        previous?.join()
                        // Finish a background still in its grace window while this graph's
                        // tracker and sessions are intact. Keep it inside the admitted cleanup
                        // so replacements wait for both finalization and cache clearing.
                        try {
                            ((lifecycle ?: container.get<LifecycleEvents>()) as? LifecycleEventsImpl)?.flushPendingBackground()
                        } catch (failure: Throwable) {
                            // A cancelled collector reply does not cancel this cleanup job.
                            // Preserve real cleanup cancellation, but never let a best-effort
                            // session flush prevent reset from clearing user data.
                            currentCoroutineContext().ensureActive()
                            logWarning("SDK pending session flush failed (${failure.javaClass.simpleName}); continuing cache cleanup")
                        }
                        if (cacheClearer != null) {
                            cacheClearer.clear(clearInstallationData = true)
                        } else {
                            container.clearUserCaches(clearInstallationData = true)
                        }
                    }
                resetCleanup = cleanup
                cleanup.invokeOnCompletion { failure ->
                    if (failure != null && failure !is kotlinx.coroutines.CancellationException) {
                        logWarning("SDK reset cleanup failed (${failure.javaClass.simpleName})")
                    }
                }
                // Admit and start cleanup before event delivery can suspend. Replacements wait
                // only for this independent IO task, which survives reset caller cancellation.
                cleanup.start()
                Triple(container, events, cleanup)
            }
        // Emit the disconnected event without calling logout() which would trigger
        // an unnecessary connectAsGuest() gRPC call while the data is being cleared.
        events.emitEvent(SDKEvent.Disconnected)
        cleanup.await()
        return container
    }

    /**
     * Switches the SDK to a different community.
     *
     * Use this method when your app supports multiple communities (i.e. different API keys)
     * and needs to switch between them at runtime.
     *
     * This method:
     * - Resets the SDK (disconnects the user and clears all locally cached data and images)
     * - Reinitializes the SDK with the new community configuration
     *
     * After calling this method, the user will need to be reconnected to the new community
     * (e.g. via [connectUser]). Flows already being collected do **not** need to be re-collected:
     * they re-bind themselves to the new community's container.
     *
     * Visible SDK content automatically recreates its scopes and ViewModels for the new community.
     * Hosts do not need to replace SDK navigation entries or rebuild their ViewModel stores.
     *
     * This method is safe to call even if the SDK is not currently initialized.
     *
     * Although this method is suspending, reinitialization can block the calling thread while
     * waiting for another concurrent [reset]'s cleanup. That cleanup runs independently on IO;
     * the wait does not include reset event delivery.
     *
     * ## Usage Example:
     * ```kotlin
     * // Switch to a different community
     * OctopusSDK.switchCommunity(
     *     context = applicationContext,
     *     apiKey = "new-community-api-key",
     *     connectionMode = ConnectionMode.SSO()
     * )
     *
     * // Then reconnect the user on the new community
     * OctopusSDK.connectUser(user) { getTokenFromServer() }
     * ```
     *
     * @param context Application context used for Android-specific functionality
     * @param apiKey The API key that identifies the new community to switch to
     * @param connectionMode The required host authentication configuration ([ConnectionMode.SSO]).
     * @param deepLinksBasePaths List of deep link base paths for routing within your application
     * @param apiServer Optional server endpoint the SDK targets. When `null` (default), the SDK
     * uses the Octopus default endpoint. When set, the SDK routes gRPC traffic to the provided
     * host and port over TLS. See [ApiServer] for the validation rules.
     *
     * @see initialize
     * @see stop
     */
    @OptIn(InternalOctopusApi::class)
    suspend fun switchCommunity(
        context: Context,
        apiKey: String,
        connectionMode: ConnectionMode,
        deepLinksBasePaths: List<String> = emptyList(),
        apiServer: ApiServer? = null,
    ) {
        if (!HostIntegrationGuard.allowsRuntime()) {
            logWarning("Community switch refused by the host integration guard; see the preceding error")
            return
        }
        val resetContainer = resetCommunity()
        val container =
            try {
                initializeCommunity(context, apiKey, connectionMode, deepLinksBasePaths, apiServer, resetContainer)
            } catch (waiting: OctopusLifecycle.AwaitModuleRetirement) {
                val version = checkNotNull(waiting.expectedVersion)
                waiting.retirement.join()
                try {
                    initializeCommunity(context, apiKey, connectionMode, deepLinksBasePaths, apiServer, expectedVersion = version)
                } catch (_: IntegrationRefused) {
                    logWarning("Community switch refused by the host integration guard; see the preceding error")
                    return
                } catch (_: StaleInitialization) {
                    return
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    logWarning("SDK initialization failed (${failure.javaClass.simpleName})")
                    return
                } catch (failure: LinkageError) {
                    logWarning("SDK dependency compatibility failure (${failure.javaClass.simpleName})")
                    return
                }
            } catch (_: IntegrationRefused) {
                logWarning("Community switch refused by the host integration guard; see the preceding error")
                return
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                logWarning("SDK initialization failed (${failure.javaClass.simpleName})")
                return
            } catch (failure: LinkageError) {
                logWarning("SDK dependency compatibility failure (${failure.javaClass.simpleName})")
                return
            }
        // AppCreated does not re-fire during a foreground switch.
        val repository =
            synchronized(lifecycleLock) {
                if (activeKoin.value === container) dependency<CommunityConfigRepository>() else null
            }
        repository?.fetchCommunityConfig()
    }

    /**
     * Updates the [notSeenNotificationsCount] by fetching the latest information from the server.
     *
     * This will fetch the information from the server.
     * Shortly after the function finishes, the [notSeenNotificationsCount] will emit an update
     * if there is a new value.
     *
     * @return [OctopusResult] indicating success or failure of the fetch operation
     */
    suspend fun updateNotSeenNotificationsCount(): OctopusResult<Int, Nothing> = profileRepository.fetchNotificationsBadge()

    /**
     * Registers this device for push notifications from Octopus.
     *
     * Call this function when your FCM (Firebase Cloud Messaging) registration token
     * is updated. This typically happens on first app launch and occasionally when
     * the token is refreshed by Firebase.
     *
     * **Safe to call before [initialize].** Firebase usually hands the registration token over at
     * app startup, often before the host has initialized Octopus, so this function queues a token
     * it receives on a non-initialized SDK and sends it as part of the next [initialize] — the
     * "register the token as soon as Firebase gives it to me" pattern works as written, with no
     * ordering constraint on the host. A warning is logged when that happens, so the ordering is
     * visible in logcat rather than silent. The same applies after [stop]: the token is queued
     * until the SDK is initialized again.
     *
     * Only the most recent token is kept while the SDK is not initialized. FCM issues a single
     * current registration token per app instance, so a token queued behind a newer one is already
     * stale. On an initialized SDK registration starts immediately, but the token stays queued
     * until registration succeeds so [stop] cannot discard an in-flight token.
     *
     * @param fcmRegistrationToken The Firebase Cloud Messaging registration token
     */
    fun registerNotificationsToken(fcmRegistrationToken: String) {
        synchronized(lifecycleLock) {
            pendingNotificationsToken.set(fcmRegistrationToken)
            if (!isInitialised) {
                logWarning(
                    "OctopusSDK.registerNotificationsToken() was called while the SDK is not " +
                        "initialized (initialize() has not been called yet, or stop() has been " +
                        "called since). The token has been kept and will be registered as soon as " +
                        "OctopusSDK.initialize() runs, so push notifications are not lost. Call " +
                        "OctopusSDK.initialize() early in your application lifecycle (typically " +
                        "Application.onCreate()) to register it immediately instead.",
                )
                return
            }
            val repository = userRepository
            sdkScope.launchUntilStopped {
                flushPendingNotificationsToken(repository)
            }
        }
    }

    /**
     * Checks if a notification data map is from Octopus.
     *
     * @param data The data map from a push notification
     * @return True if the notification originated from Octopus, false otherwise
     */
    fun isOctopusNotification(data: Map<String, String>): Boolean = data.isOctopusNotification

    /**
     * Extracts an Octopus notification from a data map.
     *
     * @param data The data map from a push notification
     * @return An [OctopusNotification] object if the map contains a valid Octopus notification,
     * null otherwise
     */
    fun getOctopusNotification(data: Map<String, String>): OctopusNotification? = data.getOctopusNotification()

    /**
     * Overrides the community access status managed by Octopus.
     *
     * This method bypasses both the SDK’s internal A/B test configuration **and** any status
     * previously set via [trackAccessToCommunity]. It explicitly determines whether the user can
     * access the community features, and takes **full precedence** over all other access control
     * mechanisms.
     *
     * Call this if you want Octopus to control access to the community directly,
     * instead of (or in addition to) managing A/B test groups in your own app.
     * Use this method when you need to guarantee that the user’s community access is enforced by
     * Octopus, regardless of analytics-only settings or internal A/B testing rules.
     *
     * @param hasAccess `true` to grant access to the community, `false` to block it.
     * @return [OctopusResult] indicating success or failure of the operation
     *
     * @see trackAccessToCommunity
     */
    suspend fun overrideCommunityAccess(hasAccess: Boolean): OctopusResult<Unit, OverrideCommunityAccessError> =
        userRepository.overrideCommunityAccess(hasAccess)

    /**
     * The SDK the host app integrates, sent on every gRPC call. Process-wide so that it survives
     * [reset], [stop] and [switchCommunity]; none of them may clear it.
     */
    @Volatile
    internal var sdkIdentity: SdkIdentity = SdkIdentity.NATIVE

    /**
     * Declares the wrapper SDK the host app integrates, so the backend can tell it apart from a
     * native integration. Without this call the SDK reports itself as native, with its own version.
     *
     * Call it before the first [initialize] or [switchCommunity]. The value is read on every network
     * call, so a later call applies from the next request, and it survives [switchCommunity]. A
     * later call replaces the value.
     *
     * It is **not** part of the supported public API (see [InternalOctopusApi]): only the Octopus
     * wrapper SDKs use it.
     *
     * @param family the wrapper family.
     * @param version the wrapper's own version (e.g. the Flutter package version), not the native one.
     */
    @InternalOctopusApi
    fun setSdkFamily(
        family: SdkFamily,
        version: String,
    ) {
        sdkIdentity = SdkIdentity(family = family.coreValue, version = version)
    }

    /**
     * Internal test affordance: locally override the per-field [ProfileFieldsLock] applied on top of
     * the community config, without a backend-driven config. Pass `null` to clear the override and
     * fall back to the backend value.
     *
     * Used by the sample app to exercise the per-field profile lock before the backend serves it.
     * It is **not** part of the supported public API (see [InternalOctopusApi]).
     *
     * @param lock the lock to apply, or `null` to restore the backend-provided config.
     */
    @InternalOctopusApi
    fun debugOverrideProfileFieldsLock(lock: ProfileFieldsLock?) {
        communityConfigRepository.debugOverrideProfileFieldsLock(lock)
    }

    /**
     * Internal test affordance: locally override the per-content-type
     * [CommunityConfig.ContentOptions] applied on top of the community config, without a
     * backend-driven config. Pass `null` to clear the override and fall back to the backend value.
     *
     * Used by the sample app to exercise the pictures/polls creation gating before the backend
     * serves it. It is **not** part of the supported public API (see [InternalOctopusApi]).
     *
     * @param options the content options to apply, or `null` to restore the backend-provided config.
     */
    @InternalOctopusApi
    fun debugOverrideContentOptions(options: CommunityConfig.ContentOptions?) {
        communityConfigRepository.debugOverrideContentOptions(options)
    }

    /**
     * Internal test affordance: locally override [CommunityConfig.exposeClientUserId] — the Unified
     * Profile activation flag — applied on top of the community config, without a backend-driven
     * config. Pass `null` to clear the override and fall back to the backend value.
     *
     * Used by the sample app to exercise the Unified Profile activation flag before the backend
     * serves it. It is **not** part of the supported public API (see [InternalOctopusApi]).
     *
     * @param enabled the value to force, or `null` to restore the backend-provided config.
     */
    @InternalOctopusApi
    fun debugOverrideExposeClientUserId(enabled: Boolean?) {
        communityConfigRepository.debugOverrideExposeClientUserId(enabled)
    }

    /**
     * Internal test affordance: locally override [CommunityConfig.termsAcceptanceMode] — the
     * explicit terms-acceptance mode — applied on top of the community config, without a
     * backend-driven config. Pass `null` to clear the override and fall back to the backend value.
     *
     * Used by the sample app to exercise the explicit-consent modes before the backend serves it.
     * It is **not** part of the supported public API (see [InternalOctopusApi]).
     *
     * @param mode the mode to force, or `null` to restore the backend-provided config.
     */
    @InternalOctopusApi
    fun debugOverrideTermsAcceptanceMode(mode: CommunityConfig.TermsAcceptanceMode?) {
        communityConfigRepository.debugOverrideTermsAcceptanceMode(mode)
    }

    /**
     * Internal test affordance: locally override [CommunityConfig.forceNicknameChoice] — whether a
     * member types their nickname before their first contribution — applied on top of the community
     * config, without a backend-driven config. Pass `null` to clear the override and fall back to the
     * backend value.
     *
     * Used by the sample app to exercise the nickname choice screen. It is **not** part of the
     * supported public API (see [InternalOctopusApi]).
     *
     * @param enabled the value to force, or `null` to restore the backend-provided config.
     */
    @InternalOctopusApi
    fun debugOverrideForceNicknameChoice(enabled: Boolean?) {
        communityConfigRepository.debugOverrideForceNicknameChoice(enabled)
    }

    /**
     * Internal test affordance: locally override [CommunityConfig.consentTexts] — the community's own
     * consent wordings — applied on top of the community config, without a backend-driven config.
     * Pass `null` to clear the override and fall back to the backend value.
     *
     * The wordings only show when their case matches the effective
     * [CommunityConfig.termsAcceptanceMode] (see [debugOverrideTermsAcceptanceMode]). Used by the
     * sample app to exercise custom consent wordings independently of the community's dashboard. It
     * is **not** part of the supported public API (see [InternalOctopusApi]).
     *
     * @param texts the wordings to force, or `null` to restore the backend-provided config.
     */
    @InternalOctopusApi
    fun debugOverrideConsentTexts(texts: CommunityConfig.ConsentTexts?) {
        communityConfigRepository.debugOverrideConsentTexts(texts)
    }

    /**
     * Sends a custom tracking event.
     *
     * This event can be integrated into the analytics reports we can deliver to you.
     *
     * @param event The custom event to send
     */
    suspend fun track(event: TrackerEvent.Custom) {
        trackerRepository.trackCustomEvent(event)
    }

    /**
     * Sets whether access to the Octopus community is enabled or not (analytics only).
     *
     * This method is typically used when the host app manages its own A/B testing logic.
     * It tells the Octopus backend whether the user has access to community features, but it does
     * **not** actually grant or restrict access within the SDK itself.
     * Instead, this information is only recorded internally for analytics and tracking, to better
     * understand engagement patterns across test groups.
     *
     * Call this if your app is running its own A/B test and you want Octopus to log whether a given
     * user is in the "community-enabled" or "control" group.
     * This is useful for reporting and engagement analytics, but does not change the SDK’s runtime
     * behavior.
     *
     * **Requires an initialized SDK.** Calling this before [initialize] (or after [stop]) is not a
     * crash: it logs a warning and does nothing. The request header is updated immediately after
     * initialization. The tracking event waits until the app is in the foreground and an app
     * session has started in this process. While backgrounded, only the latest non-null call is
     * kept in memory; pending calls are lost when the process ends. A foreground call records an
     * event even if its value is unchanged. Passing `null` resets only the header and creates no
     * event. Call this on every launch once the SDK is ready, or observe [isInitialisedFlow] if the
     * A/B assignment is resolved earlier.
     *
     * @param hasAccess `true` if the user is part of a group with access to the community (per the
     * host app’s A/B test),`false` otherwise (e.g., control group).
     *
     * @see overrideCommunityAccess
     */
    fun trackAccessToCommunity(hasAccess: Boolean?) {
        synchronized(lifecycleLock) {
            // The tracker belongs to the initialized graph; the preview container has no gRPC
            // client. Keep this guard and the synchronous lookup atomic with teardown.
            if (!isInitialised) {
                logWarning(
                    "OctopusSDK.trackAccessToCommunity() was called while the SDK is not initialized " +
                        "(initialize() has not been called yet, or stop() has been called since), so " +
                        "it did nothing. Call it once OctopusSDK.initialize() has run — observe " +
                        "OctopusSDK.isInitialisedFlow if your A/B assignment is resolved earlier.",
                )
                return
            }
            // Keep the initialized check and synchronous lookup atomic with teardown.
            dependency<ClientABTestTracker>().set(hasAccess)
        }
    }

    /**
     * Override the default (i.e. system or app-based) locale.
     *
     * Some apps do not use the default way of handling the language which provides the system/app
     * defined language by the user. If you have a custom setting inside your app that does not set
     * the system AppLanguage, you can call this function in order to customize the language used
     * (so Octopus does not use the system language but yours instead).
     *
     * That being said, we recommend using the default system way of handling the locale, so system
     * alerts are displayed in the desired language.
     *
     * ## Usage Example:
     * ```kotlin
     * // Override to French
     * OctopusSDK.overrideDefaultLocale(Locale.FRENCH)
     *
     * // Reset to system default
     * OctopusSDK.overrideDefaultLocale(null)
     * ```
     *
     * @param locale The locale that you want to set. Set null if you want Octopus to use the system
     * setting.
     */
    suspend fun overrideDefaultLocale(locale: Locale?) = overrideDefaultLocaleUseCase(locale)

    /**
     * Fetches the latest list of topics from the server.
     *
     * This method performs a network request to retrieve the most up-to-date list of topics
     * available in the community. Unlike [topics], which returns cached data, this method
     * always fetches fresh data from the server.
     *
     * After a successful fetch, the [topics] Flow will emit the updated topics list.
     * This method is useful when you need to ensure you have the latest topics data,
     * such as when refreshing a topic selection screen.
     *
     * ## Usage Example:
     * ```kotlin
     * // Refresh topics when user pulls to refresh
     * val result = OctopusSDK.fetchTopics()
     * when (result) {
     *     is OctopusResult.Success -> {
     *         // Topics updated successfully
     *         // The topics Flow will emit the new data
     *     }
     *     is OctopusResult.Failure -> {
     *         // Handle network or other errors
     *         showError("Failed to refresh topics")
     *     }
     * }
     * ```
     *
     * @return [OctopusResult] containing the list of groups on success, or an error on failure
     */
    suspend fun fetchGroups(): OctopusResult<List<OctopusGroup>, Nothing> = groupRepository.fetchGroups()

    @Deprecated("Use fetchGroups() instead", ReplaceWith("fetchGroups()"))
    suspend fun fetchTopics(): OctopusResult<List<Topic>, Nothing> = fetchGroups()

    /**
     * Follows a group for the current user.
     *
     * This allows the client app to programmatically follow a group on behalf of the user.
     * The follow state will be reflected in the Groups screen and the "For You" feed will
     * include posts from this group.
     *
     * If the group is already followed, the call succeeds silently (idempotent).
     * The user must be connected before calling this method.
     *
     * ## Usage Example:
     * ```kotlin
     * val result = OctopusSDK.followGroup("group_pregnancy")
     * when (result) {
     *     is OctopusResult.Success -> {
     *         // Group followed successfully
     *     }
     *     is OctopusResult.Failure -> {
     *         // Handle error (e.g., group not found, network error)
     *     }
     * }
     * ```
     *
     * If groups have not been fetched yet (e.g., the SDK UI has not been displayed),
     * this method will automatically fetch them before attempting to follow.
     *
     * @param groupId The unique identifier of the group to follow
     * @return [OctopusResult] indicating success or failure with [GroupFollowUnfollowError]
     */
    suspend fun followGroup(groupId: String): OctopusResult<Unit, GroupFollowUnfollowError> = followTopic(groupId)

    /**
     * Unfollows a group for the current user.
     *
     * This is the recommended name. See [unfollowTopic] for backward compatibility.
     *
     * @param groupId The unique identifier of the group to unfollow
     * @return [OctopusResult] indicating success or failure with [GroupFollowUnfollowError]
     */
    suspend fun unfollowGroup(groupId: String): OctopusResult<Unit, GroupFollowUnfollowError> = unfollowTopic(groupId)

    /**
     * Pushes a batch of follow/unfollow actions for the current user, each tagged with the
     * date at which the host app decided to apply it.
     *
     * Use this when your app authoritatively assigns groups to users — e.g. during onboarding,
     * when a premium tier changes, or when the user updates their app-side interests.
     *
     * ## Per-action timestamp, backend-side conflict resolution
     *
     * The backend compares every [SyncFollowGroupAction.actionDate] against the timestamp it
     * has stored for the same (user, group) pair and only applies actions that are strictly
     * more recent. Stale actions are reported as [SyncFollowGroupStatus.Skipped] — not as
     * errors. This means user-driven actions (via the Octopus SDK UI) that happened after
     * your `actionDate` keep priority over this sync.
     *
     * ## Per-action result
     *
     * Each [SyncFollowGroupAction] produces one [SyncFollowGroupResult] with its own
     * [SyncFollowGroupStatus]. Batches may be heterogeneous — some actions applied, some
     * skipped, some rejected (e.g. the group is essential and cannot be unfollowed). See
     * [SyncFollowGroupStatus] for the full list of possible per-action outcomes.
     *
     * ## Empty list
     *
     * Passing an empty [actions] list returns success with an empty result list immediately,
     * without making a network call.
     *
     * ## Batch size
     *
     * The SDK does not cap [actions] size client-side, but very large batches put load on the
     * backend. Aim for batches of a few dozen actions or less. If you need to sync hundreds
     * of groups, split the calls.
     *
     * ## Usage Example:
     * ```kotlin
     * val result = OctopusSDK.syncFollowGroups(
     *     listOf(
     *         SyncFollowGroupAction("group_pregnancy", followed = true, actionDate = Date()),
     *         SyncFollowGroupAction("group_mom_to_be", followed = false, actionDate = Date())
     *     )
     * )
     * when (result) {
     *     is OctopusResult.Success -> result.data.forEach { perAction ->
     *         // Inspect perAction.status — match it back by groupId, not by index.
     *     }
     *     is OctopusResult.Failure -> {
     *         // RPC-level failure (network, not connected, etc.)
     *     }
     * }
     * ```
     *
     * The user must be connected before calling this method. After a successful sync, the
     * local cache is refreshed so the Groups screen and "For You" feed reflect the new
     * follow state immediately.
     *
     * @param actions The follow/unfollow actions to sync.
     * @return [OctopusResult.Success] wrapping a list of [SyncFollowGroupResult] — match back
     * to the input actions by [SyncFollowGroupResult.groupId], not by list index (ordering is
     * not guaranteed) — or an [OctopusResult.Failure] for RPC-level errors.
     *
     * @see SyncFollowGroupAction
     * @see SyncFollowGroupResult
     * @see SyncFollowGroupStatus
     */
    suspend fun syncFollowGroups(actions: List<SyncFollowGroupAction>): OctopusResult<List<SyncFollowGroupResult>, Nothing> =
        groupRepository.syncFollowGroups(actions)

    @Deprecated("Use followGroup(groupId) instead", ReplaceWith("followGroup(topicId)"))
    suspend fun followTopic(topicId: String): OctopusResult<Unit, GroupFollowUnfollowError> {
        val group =
            groupRepository.getGroupFlow(topicId).first()
                ?: run {
                    // Groups may not have been fetched yet — try fetching from server.
                    // Propagate any network failure rather than silently returning MissingGroup.
                    val fetchResult = groupRepository.fetchGroups()
                    if (fetchResult is OctopusResult.Failure) return fetchResult
                    groupRepository.getGroupFlow(topicId).first()
                }
                ?: return OctopusResult.Failure.InvalidArguments(
                    GroupFollowUnfollowError.MissingGroup("Group not found: $topicId"),
                )
        return groupRepository.setFollowed(group, followed = true)
    }

    /**
     * Unfollows a topic for the current user.
     *
     * This allows the client app to programmatically unfollow a topic on behalf of the user.
     * The unfollow state will be reflected in the Groups screen and the "For You" feed will
     * no longer include posts from this topic.
     *
     * If the topic is already unfollowed, the call succeeds silently (idempotent).
     * Force-followed (essential) topics cannot be unfollowed and will return an error.
     * The user must be connected before calling this method.
     *
     * ## Usage Example:
     * ```kotlin
     * val result = OctopusSDK.unfollowTopic("topic_pregnancy")
     * when (result) {
     *     is OctopusResult.Success -> {
     *         // Topic unfollowed successfully
     *     }
     *     is OctopusResult.Failure -> {
     *         // Handle error (e.g., topic is essential and cannot be unfollowed)
     *     }
     * }
     * ```
     *
     * If topics have not been fetched yet (e.g., the SDK UI has not been displayed),
     * this method will automatically fetch them before attempting to unfollow.
     *
     * @param topicId The unique identifier of the topic to unfollow
     * @return [OctopusResult] indicating success or failure with [TopicFollowUnfollowError]
     */
    @Deprecated("Use unfollowGroup(groupId) instead", ReplaceWith("unfollowGroup(topicId)"))
    suspend fun unfollowTopic(topicId: String): OctopusResult<Unit, GroupFollowUnfollowError> {
        val group =
            groupRepository.getGroupFlow(topicId).first()
                ?: run {
                    // Groups may not have been fetched yet — try fetching from server.
                    // Propagate any network failure rather than silently returning MissingGroup.
                    val fetchResult = groupRepository.fetchGroups()
                    if (fetchResult is OctopusResult.Failure) return fetchResult
                    groupRepository.getGroupFlow(topicId).first()
                }
                ?: return OctopusResult.Failure.InvalidArguments(
                    GroupFollowUnfollowError.MissingGroup("Group not found: $topicId"),
                )
        // Guard: the user must always have ≥1 followed group (Followed or ForceFollowed).
        // ForceFollowed groups count toward the total — a client can programmatically unfollow
        // a user's last voluntarily-followed group as long as ForceFollowed groups remain
        // (e.g. a health app unfollows "MomsToBe" and force-follows "Pregnant").
        // This matches the behavior enforced by the built-in Octopus SDK UI.
        val followedCount = groupRepository.getGroupsFlow().first().count { it.isFollowed }
        if (followedCount <= 1) {
            return OctopusResult.Failure.InvalidArguments(
                GroupFollowUnfollowError.LastFollowedGroup(
                    "Cannot unfollow the last followed group",
                ),
            )
        }
        return groupRepository.setFollowed(group, followed = false)
    }

    /**
     * Sets a reaction on the user's behalf on the post identified by [postId].
     *
     * Works on any post the current user can see — bridge posts (client object related posts) or
     * community-originated posts.
     *
     * ## Error surface
     *
     * Reaction-domain errors land in [SetReactionError] (`UnknownReaction`, `PostNotFound`,
     * `ReactionError(message)`) inside [OctopusResult.Failure.InvalidArguments]. Network and
     * authentication failures are surfaced separately as [OctopusResult.Failure.Connection]
     * (e.g. [OctopusResult.Failure.Connection.NoNetwork],
     * [OctopusResult.Failure.Connection.UserNotAuthenticated]) — handle both branches.
     *
     * [SetReactionError.ReactionError] wraps a server-side `message` intended for logs and
     * diagnostics, not direct end-user display.
     *
     * ## Behavior
     *
     * Passing `null` for [reaction] removes the current user's active reaction on the post.
     * Calling with `null` when no reaction is currently set is a silent no-op (returns
     * [OctopusResult.Success]). Setting the same reaction twice is idempotent (single backend
     * write).
     *
     * @param reaction The reaction to set. Pass `null` to remove the current user's reaction.
     * Unknown reactions are not supported and yield [SetReactionError.UnknownReaction].
     * @param postId The id of the post to react on (`OctopusPost.id` / `Post.id`).
     * @return [OctopusResult] indicating success or failure.
     */
    suspend fun setReaction(
        reaction: OctopusReactionKind?,
        postId: String,
    ): OctopusResult<Unit, SetReactionError> =
        postRepository
            .setReaction(
                reactionKind = reaction,
                postId = postId,
            ).mapSuccess { }

    /**
     * Gets a [Flow] on a post given a client object id.
     *
     * The Flow will be updated when you call the [fetchOrCreateClientObjectRelatedPost] function
     * and also sometimes due to internal updates of the post.
     *
     * @param clientObjectId The unique identifier of the client object
     * @return A Flow that emits the associated [OctopusPost] or `null` if no post exists
     */
    fun getClientObjectRelatedPostFlow(clientObjectId: String): Flow<OctopusPost?> = postRepository.getClientObjectPostFlow(clientObjectId)

    /**
     * Gets the Octopus post related to the given object id.
     *
     * If the Octopus post does not exist yet, it will be created. The content will only be used if
     * the post does not exist yet.
     *
     * This function is asynchronous and may take some time, if it is called after a user
     * interaction, you should display a loader.
     *
     * Note: You can use the returned post id to display the post using
     * `OctopusPostDetails(octopusPostId)`
     *
     * @param clientPost the content of the post
     * @param tokenProvider an optional callback invoked with a bridge fingerprint (SHA-256 hash of
     * the post content) when a new post needs to be created. It should return a signed JWT token
     * authorizing the creation, or `null` if no token is available. If the post already exists,
     * this callback is never called.
     * @return [OctopusResult] containing the Octopus post on success, or an error on failure. A
     * [clientPost] carrying more than `Post.Draft.MAX_IMAGE_COUNT` (10) images fails with
     * [OctopusResult.Failure.InvalidArguments] holding a [ClientPostError.FileError.Unknown], before
     * any network call.
     */
    suspend fun fetchOrCreateClientObjectRelatedPost(
        clientPost: ClientPost,
        tokenProvider: (suspend (String) -> String?)? = null,
    ): OctopusResult<OctopusPost, ClientPostError> = postRepository.fetchOrCreateClientObjectPost(clientPost, tokenProvider)

    /**
     * Gets a [Flow] of read-only [OctopusCommunityData] for the given user.
     *
     * Use this to surface a member's community stats (message count, gamification level and total
     * points) inside your own profile screen. The Flow emits the latest cached value and updates
     * whenever the underlying profile is refreshed — e.g. via [fetchCommunityDataResult], which you
     * can call after your app awards points if the server returns totals. Once the connected
     * member's community data has been fetched with gamification enabled, it also updates when the
     * SDK refreshes that member's own profile. Other members' scores are `null` when the server does
     * not send the total. On a server that does not return totals, a public-profile fetch resets the
     * connected member's cached score to `null` until the SDK next refreshes their own profile.
     * Emits `null` when no data is known yet for [userId].
     *
     * @param userId The id of the user whose community data to observe.
     * @return A [Flow] that emits the associated [OctopusCommunityData] or `null` when unknown.
     */
    fun communityDataFlow(userId: String): Flow<OctopusCommunityData?> = profileRepository.communityDataFlow(userId)

    /**
     * Gets a [Flow] of read-only [OctopusCommunityData] for the member identified by the host app's
     * own [clientUserId] (Unified Profile).
     *
     * The client-user-id counterpart of [communityDataFlow]: use it from your own profile screen —
     * which only knows your app's user ids — to observe a member's Octopus community stats reactively
     * without handling Octopus ids. The member is resolved once through the `GetPublicProfile`
     * client-user-id lookup, then the Flow tracks that member's cached data and updates on every
     * refresh (e.g. via [fetchCommunityDataByClientUserIdResult]). Requires the community to expose client
     * user ids ([CommunityConfig.exposeClientUserId]); emits `null` when the member is unknown or the
     * lookup is unsupported.
     *
     * The two cannot be overloads (identical JVM signature), hence the distinct name.
     *
     * @param clientUserId The host's own id for the member whose community data to observe.
     * @return A [Flow] that emits the associated [OctopusCommunityData] or `null` when unknown.
     */
    fun communityDataFlowByClientUserId(clientUserId: String): Flow<OctopusCommunityData?> =
        flow {
            when (
                val octopusUserId =
                    profileRepository.fetchProfileByClientUserId(clientUserId).getOrNull()?.userId
            ) {
                null -> emit(null)
                else -> emitAll(communityDataFlow(octopusUserId))
            }
        }

    /**
     * Refreshes and returns the read-only [OctopusCommunityData] for the given user.
     *
     * This performs a network refresh and updates the cache backing [communityDataFlow], so any
     * active Flow on the same [userId] will emit the refreshed value too.
     *
     * This function is asynchronous; if it is called after a user interaction, you should display a
     * loader.
     *
     * @param userId The id of the user whose community data to fetch.
     * @return The refreshed [OctopusCommunityData], or `null` when the user is unknown or the
     * refresh failed.
     */
    @Deprecated(
        message = "Use fetchCommunityDataResult(userId) to distinguish unknown members from lookup failures",
        level = DeprecationLevel.WARNING,
    )
    suspend fun fetchCommunityData(userId: String): OctopusCommunityData? = fetchCommunityDataResult(userId).getOrNull()

    /**
     * Refreshes and returns the read-only [OctopusCommunityData] for the given Octopus user id.
     *
     * A successful lookup updates the cache backing [communityDataFlow], so active observers on
     * the same [userId] receive the updated cache. When profile requests overlap, the Flow keeps
     * gamification from the request sent most recently, while this result reflects its own response.
     * [OctopusResult.Success] with `null` means the member is unknown. [OctopusResult.Failure] means the lookup failed, for example because of a
     * network, authentication, permission or server-status error; that failure is preserved.
     *
     * The `Nothing` error parameter matches [ProfileRepository.fetchProfile]: there are no typed
     * domain errors, but [OctopusResult.Failure.Connection] failures can still be returned.
     *
     * This function suspends during the network refresh; display a loader when called after a
     * user interaction.
     *
     * @param userId The Octopus id of the member whose community data to fetch.
     * @return [OctopusResult.Success] containing the refreshed data or `null` for an unknown
     * member, or [OctopusResult.Failure] if the lookup failed.
     */
    suspend fun fetchCommunityDataResult(userId: String): OctopusResult<OctopusCommunityData?, Nothing> =
        profileRepository.fetchCommunityData(userId)

    /**
     * Refreshes and returns the read-only [OctopusCommunityData] for the member identified by the
     * host app's own [clientUserId] (Unified Profile).
     *
     * Resolves the member through the `GetPublicProfile` client-user-id lookup, so a host that only
     * knows its own user ids can surface Octopus community stats on its profile screen without ever
     * handling Octopus ids. Requires the community to expose client user ids
     * ([CommunityConfig.exposeClientUserId]); returns `null` when the member is unknown or the lookup
     * is unsupported.
     *
     * This is the client-user-id counterpart of [fetchCommunityData]; the two cannot be overloads
     * (identical JVM signature), hence the distinct name.
     *
     * @param clientUserId The host's own id for the member whose community data to fetch.
     * @return The refreshed [OctopusCommunityData], or `null` when unknown or the refresh failed.
     */
    @Deprecated(
        message = "Use fetchCommunityDataByClientUserIdResult(clientUserId) to distinguish unknown members from lookup failures",
        level = DeprecationLevel.WARNING,
    )
    suspend fun fetchCommunityDataByClientUserId(clientUserId: String): OctopusCommunityData? =
        fetchCommunityDataByClientUserIdResult(clientUserId).getOrNull()

    /**
     * Refreshes and returns the read-only [OctopusCommunityData] for the member identified by the
     * host app's own [clientUserId] (Unified Profile).
     *
     * Resolves the member through the `GetPublicProfile` client-user-id lookup and updates the
     * member's cached profile on success. When profile requests overlap, the Flow keeps gamification
     * from the request sent most recently, while this result reflects its own response.
     * Requires the community to expose client user ids
     * ([CommunityConfig.exposeClientUserId]). [OctopusResult.Success] with `null` means the lookup
     * found no member. [OctopusResult.Failure] means the lookup failed, including a rejected or
     * unsupported lookup; network, authentication, permission and server-status failures are
     * preserved rather than converted to `null`.
     *
     * The `Nothing` error parameter matches [ProfileRepository.fetchProfileByClientUserId]: there
     * are no typed domain errors, but [OctopusResult.Failure.Connection] failures can still be
     * returned. This is the host-id counterpart of [fetchCommunityDataResult]; distinct names
     * avoid identical JVM signatures for the two string parameters.
     *
     * This function suspends during the network refresh; display a loader when called after a
     * user interaction.
     *
     * @param clientUserId The host's own id for the member whose community data to fetch.
     * @return [OctopusResult.Success] containing the refreshed data or `null` when no member was
     * found, or [OctopusResult.Failure] if the lookup failed.
     */
    suspend fun fetchCommunityDataByClientUserIdResult(clientUserId: String): OctopusResult<OctopusCommunityData?, Nothing> =
        profileRepository.fetchCommunityDataByClientUserId(clientUserId)

    /**
     * Stops the SDK and releases all resources.
     *
     * Call this when the SDK is no longer needed, typically when the application is shutting down.
     *
     * After calling this method, the SDK will be in an uninitialized state and
     * [initialize] must be called again before using any SDK functionality.
     * All ongoing operations will be cancelled.
     * If a [reset] cleanup is in progress, this call first waits for its IO writes before
     * closing the container. It does not wait for reset event delivery.
     *
     * SDK content renders nothing while stopped and automatically recreates its scopes and
     * ViewModels on the next [initialize]. Resource closure runs off Main after any admitted
     * composition frame has completed, including when the host Activity is stopped.
     *
     * The lifecycle Flows ([connectionState], [isUserConnected], [profile],
     * [notSeenNotificationsCount], [hasAccessToCommunity], [groups], [events]) do **not** complete:
     * they stop emitting and re-bind themselves to the new container if the SDK is initialized
     * again, so a collector started once at app launch survives a stop/restart cycle.
     *
     * **Note:** If you want to switch to a different community, use [switchCommunity]
     * instead — it also disconnects the user and clears cached data before reinitializing.
     *
     * @see switchCommunity
     * @see initialize
     */
    @OptIn(InternalOctopusApi::class)
    fun stop() {
        var retiredClient: OctopusGrpcClient? = null
        withLifecycleLock {
            lifecycleVersion++
            buildingScope?.cancel()
            initializationRetry?.cancel()
            if (!isInitialised) {
                retiredClient = retiringClient
                return@withLifecycleLock
            }

            initializationConfiguration = null

            try {
                eventRepository.tryEmitEvent(SDKEvent.Stopped)
            } catch (_: SDKNotInitializedException) {
                // An integration may only have set the readiness flag.
            }

            // Clears `activeKoin` and flips `isInitialisedFlow` to false — both *before* running the
            // teardown below, so no collector is left reading a closed Koin. See `retireContainer` for
            // the full rationale; `OctopusSDKLifecycleWiringTest` pins the order.
            stoppedKoin = if (isKoinInitialised) koinApp.koin else null
            retiredClient = if (isKoinInitialised) koinApp.koin.getProperty("octopusGrpcClient") else null
            retiringClient = retiredClient
            retiredClient?.retire()
            containerDependencies = null
            retireContainer {
                // Cancel before close: `launchUntilStopped` only drops a closed-container failure from a
                // coroutine that is already cancelled, so swapping these two brings the crash back.
                if (::sdkScope.isInitialized) {
                    sdkScope.cancel()
                }

                if (isKoinInitialised) {
                    // Instantiated resources are released by their Koin onClose callbacks.
                    val retiredApplication = koinApp
                    lastRetirement = OctopusLifecycle.retire { retiredApplication.close() }
                }
            }
        }
        retiredClient?.shutdown()
        synchronized(lifecycleLock) {
            if (retiringClient === retiredClient) retiringClient = null
        }
    }
}

/**
 * Resolves the gRPC server endpoint the SDK should target.
 *
 * Resolution order: explicit [apiServer] override → fallback (typically the build's
 * `BuildConfig.OCTOPUS_SERVER_HOST` / `OCTOPUS_SERVER_PORT`). When an [apiServer]
 * is provided, IPv6 brackets are stripped before handing the host off to the gRPC
 * channel builder (`AndroidChannelBuilder.forAddress(...)` expects an unbracketed
 * literal).
 *
 * Mirrors the iOS `GrpcClient.resolveBaseURL(override:infoDictionary:)` helper.
 */
internal fun resolveServerEndpoint(
    apiServer: ApiServer?,
    fallbackHost: String,
    fallbackPort: Int,
): Pair<String, Int> =
    if (apiServer != null) {
        apiServer.unbracketedHost to apiServer.port
    } else {
        fallbackHost to fallbackPort
    }

/**
 * Configures and loads the SDK's Koin dependency injection modules.
 *
 * This internal function sets up all necessary modules for the Octopus SDK, including database,
 * network, repository, and other core components.
 * It configures the Android context and logger, then loads all required modules with the provided
 * configuration parameters.
 *
 * @param context Application context used for Android-specific functionality
 * @param sdkScope Coroutine scope for SDK operations
 * @param serverHost Host for server connections
 * @param serverPort Port for server connections
 * @param linksHost Host for link shortening and processing
 * @param apiKey The API key that identifies the community
 * @param connectionMode The host authentication configuration
 */
@OptIn(InternalOctopusApi::class)
internal fun KoinApplication.octopusSdkModules(
    context: Context,
    sdkScope: CoroutineScope,
    serverHost: String,
    serverPort: Int,
    linksHost: String,
    apiKey: String,
    connectionMode: ConnectionMode,
) {
    androidLogger(Level.ERROR)
    androidContext(context)
    modules(
        listOf(
            databaseModule,
            dataStoreModule(sdkScope = sdkScope),
            *OctopusLifecycle.modulesForContainer(listOf(octopusImageModule)).toTypedArray(),
            mapperModule,
            networkModule(
                sdkScope = sdkScope,
                serverHost = serverHost,
                serverPort = serverPort,
                apiKey = apiKey,
            ),
            repositoryModule(apiKey = apiKey, connectionMode = connectionMode, linksHost = linksHost),
            *OctopusLifecycle.modulesForContainer(listOf(octopusUseCaseModule)).toTypedArray(),
            eventsModule(sdkScope = sdkScope),
            module { single { ContainerDependencies(getKoin()) } },
        ),
    )
}

/**
 * Signals access to a stopped SDK dependency.
 *
 * Repository and use-case access after [OctopusSDK.stop] throws this exception. Before the first
 * initialization, getters retain their preview/test behavior; lifecycle Flows remain silent.
 *
 * To detect an initialization-order mistake, check
 * [OctopusSDK.isInitialised] or observe [OctopusSDK.isInitialisedFlow] instead. Call
 * [OctopusSDK.initialize] early in your application lifecycle, typically from
 * `Application.onCreate()`.
 */
class SDKNotInitializedException :
    Exception(
        "Octopus SDK is not initialized. Please call OctopusSDK.initialize() before any " +
            "SDK usage.",
    )

/**
 * Exception thrown when onNavigateToLogin callback is missing in SSO mode.
 *
 * This exception is thrown when the SDK needs to navigate to the login screen
 * but the onNavigateToLogin callback was not provided during setup.
 *
 * ## Solution:
 * When using [ConnectionMode.SSO], you must provide the onNavigateToLogin callback
 * when setting up Octopus composables to handle navigation to your login screen.
 */
class MissingOnNavigateToLoginException :
    Exception(
        "Missing onNavigateToLogin lambda parameter. You need to provide an access to your " +
            "login callback when using Octopus SDK in SSO Connection Mode.",
    )

/**
 * Exception thrown when onNavigateToProfileEdit callback is missing in SSO mode with app-managed
 * fields.
 *
 * This exception is thrown when the SDK needs to navigate to the profile edit screen
 * but the onNavigateToProfileEdit callback was not provided during setup.
 *
 * ## Solution:
 * When using [ConnectionMode.SSO] with app-managed profile fields, you must provide
 * the onNavigateToProfileEdit callback when setting up Octopus composables to handle
 * navigation to your profile editing screen.
 */
class MissingOnNavigateToProfileEditException :
    Exception(
        "Missing onNavigateToProfileEdit lambda parameter. You need to provide an access " +
            "to your profile edition callback when using Octopus SDK in SSO Connection Mode with " +
            "app managed fields.",
    )
