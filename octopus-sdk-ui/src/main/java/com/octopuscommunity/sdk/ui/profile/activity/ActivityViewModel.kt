package com.octopuscommunity.sdk.ui.profile.activity

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.OctopusEvent.CommentCreated
import com.octopuscommunity.sdk.domain.model.OctopusEvent.CommentDeleted
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostCreated
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostDeleted
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReactionModified
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReplyCreated
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReplyDeleted
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ScreenDisplayed
import com.octopuscommunity.sdk.domain.model.Profile
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.getOrNull
import com.octopuscommunity.sdk.domain.repository.GroupRepository
import com.octopuscommunity.sdk.domain.repository.ProfileRepository
import com.octopuscommunity.sdk.domain.repository.canCreateAnyPostFlow
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.common.ErrorChannel
import com.octopuscommunity.sdk.ui.common.ScreenStateFailure
import com.octopuscommunity.sdk.ui.common.loadFailureChannel
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel backing the [ActivityScreen] (Unified Profile, internal ticket).
 *
 * It surfaces a user's community activity — posts feed and (for the connected user) notifications —
 * without the profile-summary chrome. Two modes, resolved from [userId] against the connected user:
 * - **Connected user** ([userId] `null`, or equal to the connected user's id): the connected user's
 *   own activity, backed by [currentUserProfile]. Emits [ScreenDisplayed.Activity].
 * - **Other user** ([userId] set to another member): that member's posts, fetched via
 *   [ProfileRepository.getProfileFlow]. Emits [ScreenDisplayed.OtherUserPosts].
 *
 * It deliberately does NOT reuse `CurrentUserProfileSummaryViewModel` / `ProfileSummaryViewModel`:
 * the Activity screen is a distinct destination, so it must emit its own screen event (not
 * [ScreenDisplayed.Profile] / [ScreenDisplayed.OtherUserProfile]) and must not trigger the profile's
 * gamification sheet. Gamification is intentionally not displayed here (it is exposed to the host
 * via the read-only community-data API).
 */
class ActivityViewModel(
    commonParameters: CommonParameters,
    groupRepository: GroupRepository,
    private val profileRepository: ProfileRepository,
    private val userId: String?,
    private val clientUserId: String? = null,
) : OctopusViewModel(commonParameters) {
    @Immutable
    data class UiState(
        val profile: Profile? = null,
        val communityConfig: CommunityConfig? = null,
        /**
         * `true` iff at least one accessible topic allows post creation. Gates the create-post
         * incentive chips on the empty-posts placeholder. Defaults to `true` so existing behavior
         * is preserved until the BE-served topic permissions resolve.
         */
        val canCreatePost: Boolean = true,
        /**
         * `true` when this screen shows another member's posts (opened on demand via
         * `navigateToOctopusActivity`); `false` for the connected user's own activity. Drives the
         * "other user" chrome: a single Posts tab, the "Publications de {author}" title, and no
         * notifications tab / overflow menu.
         */
        val isOtherUser: Boolean = false,
        /**
         * `true` when the connected user is a guest. Guests have no editable profile, so the
         * overflow menu's "View my profile" / "Edit my profile" actions are hidden for them.
         */
        val isGuest: Boolean = false,
        /**
         * `true` while another member's profile is still being preloaded (other-user mode, first
         * fetch, nothing cached). Drives a loader instead of the empty "'s Posts" header + Powered-by
         * chrome; cleared once the profile arrives or the fetch completes.
         */
        val isLoading: Boolean = false,
        /**
         * Set when the member's profile could not be fetched and none was cached. Without it the
         * screen renders its header over nothing at all: the posts list needs the profile's feed id,
         * so it draws no state of its own to report the failure (Screen states spec).
         */
        val hasProfileLoadFailure: Boolean = false,
        val loadFailureIsNoNetwork: Boolean = false,
        /**
         * The tab the connected-user Activity screen lands on when a `userId` / `clientUserId`
         * entry point resolves to the connected user (a self-tap): the unseen-notifications rule —
         * Notifications (0) when there are unseen notifications, Posts (2) otherwise (unlike the
         * bell button, which always lands on Notifications). `null` when no id was requested
         * (the floating-button path, whose destination already carries the resolved tab) or until
         * the mode resolves; the screen then keeps the destination's own `selectedTabIndex`. Set
         * atomically with [isOtherUser], and applied one-shot by the screen (the tabs first compose
         * only after connected-user mode is entered), so a late async resolution never overrides a
         * tab the user already picked. Mirrors iOS `ActivityViewModel.connectedUserInitialTab`.
         */
        val connectedUserInitialTabIndex: Int? = null,
    )

    private val _state =
        MutableStateFlow(
            UiState(
                isOtherUser = userId != null || clientUserId != null,
                isLoading = userId != null || clientUserId != null,
            ),
        )
    val state = _state.asStateFlow()

    /**
     * Id of the member whose posts to fetch on [refresh], or `null` for the connected user.
     * Resolved once in [init] against the connected user's id — held as a field rather than derived
     * from [userId] so a host that passes the connected user's own id is treated as the connected
     * user (contract). Confined to the main thread (all [viewModelScope] launches run on
     * `Dispatchers.Main.immediate`), so no synchronization is needed. Provisionally seeded with
     * [userId] so an early [onResume] hits a sensible endpoint before resolution completes.
     */
    private var otherUserId: String? = userId

    /**
     * `true` while the [clientUserId] entry point's async id lookup is still in flight (the real
     * Octopus id is unknown until it resolves). Gates [refresh] to a no-op until then, so an early
     * [onResume] can't fall through to refreshing the connected user before the target is known.
     * Only the `clientUserId` path resolves asynchronously; the plain [userId] path knows its id
     * synchronously, so it is never "resolving".
     */
    private var isResolvingByClientUserId: Boolean = userId == null && clientUserId != null

    init {
        viewModelScope.launch {
            configRepository.getCommunityConfigFlow().collectLatest { communityConfig ->
                _state.update { it.copy(communityConfig = communityConfig) }
            }
        }
        viewModelScope.launch {
            eventRepository.getEventsFlow().collect { event ->
                when (event) {
                    is PostCreated, is CommentCreated, is ReplyCreated,
                    is PostDeleted, is CommentDeleted, is ReplyDeleted,
                    is ReactionModified,
                    -> refresh()

                    else -> {}
                }
            }
        }
        viewModelScope.launch {
            groupRepository.canCreateAnyPostFlow().collect { canCreate ->
                _state.update { it.copy(canCreatePost = canCreate) }
            }
        }
        // Resolve self vs. other from the local, non-suspending connected profile — a host may pass
        // the connected user's own id (→ connected-user mode, per contract) or none at all — then
        // wire the matching posts source and emit the matching screen event. `getProfile()` is a
        // local read: a not-connected host resolves to `null` → "other user" and still emits its
        // event, instead of parking a coroutine forever waiting on the profile flow.
        viewModelScope.launch {
            // Unified Profile: a client-user-id entry point (navigateToOctopusActivityByClientUserId)
            // is resolved to its Octopus id first, through the debug-aware repository lookup. A plain
            // [userId] entry point skips the lookup.
            val requestedUserId =
                userId ?: clientUserId?.let {
                    profileRepository.fetchProfileByClientUserId(it).getOrNull()?.userId
                }
            val connectedProfile = connectionRepository.getProfile()
            val connectedUserId = connectedProfile?.userId
            // The async id lookup (if any) has settled — let [refresh] run again.
            isResolvingByClientUserId = false

            // Resolve the screen mode now that any async lookup has settled. A client-user-id that
            // did not resolve (unknown/deleted mapping, network failure, or the community no longer
            // exposing client ids) must NOT fall back to the connected user's own activity — the host
            // asked for a specific member.
            val mode =
                resolveActivityMode(
                    userId = userId,
                    clientUserId = clientUserId,
                    requestedUserId = requestedUserId,
                    connectedUserId = connectedUserId,
                )
            otherUserId = if (mode == ActivityMode.OTHER_USER) requestedUserId else null
            _state.update {
                it.copy(
                    isOtherUser = mode != ActivityMode.CONNECTED_USER,
                    // Stop the preload loader on a failed lookup so it doesn't spin forever.
                    isLoading = if (mode == ActivityMode.CLIENT_LOOKUP_FAILED) false else it.isLoading,
                    // Same update as the mode flip, so the tabs' very first composition already
                    // sees the resolved landing tab (one-shot by construction).
                    connectedUserInitialTabIndex =
                        if (mode == ActivityMode.CONNECTED_USER) {
                            connectedUserInitialTabIndex(
                                hadRequestedId = userId != null || clientUserId != null,
                                hasUnseenNotifications =
                                    (connectedProfile?.notSeenNotificationsCount ?: 0) > 0,
                            )
                        } else {
                            null
                        },
                )
            }
            when (mode) {
                ActivityMode.OTHER_USER -> {
                    val resolvedId = requireNotNull(requestedUserId)
                    collectOtherUserProfile(resolvedId)
                    eventRepository.emitEvent(ScreenDisplayed.OtherUserPosts(profileId = resolvedId))
                }

                ActivityMode.CLIENT_LOOKUP_FAILED -> {
                    // No id to fetch: the screen shows the empty other-user posts state (no
                    // connected-user chrome, no self overflow menu); loader already cleared above.
                }

                ActivityMode.CONNECTED_USER -> {
                    collectConnectedUserProfile()
                    eventRepository.emitEvent(ScreenDisplayed.Activity)
                }
            }
        }
    }

    private fun collectConnectedUserProfile() {
        viewModelScope.launch {
            currentUserProfile.collectLatest { profile ->
                _state.update { it.copy(profile = profile, isGuest = profile?.isGuest ?: false) }
            }
        }
    }

    private fun collectOtherUserProfile(userId: String) {
        viewModelScope.launch {
            profileRepository.getProfileFlow(userId).collect { profile ->
                _state.update {
                    it.copy(
                        profile = profile,
                        isLoading = if (profile != null) false else it.isLoading,
                    )
                }
            }
        }
    }

    fun onResume() {
        refresh()
    }

    /**
     * Re-runs the other-member profile load, from the error state's CTA. Raises [UiState.isLoading]
     * and clears [UiState.hasProfileLoadFailure] before refreshing, because the screen renders the
     * loader before the error state: without it the error state — Retry button included — stays
     * frozen on screen for the whole round trip and the button looks dead. Mirrors
     * `GroupsListViewModel.retryFirstLoad`.
     *
     * Only reachable from the other-user error state, which composes after the mode resolved, so
     * [isResolvingByClientUserId] is already `false` and [otherUserId] is set — the guard is kept
     * anyway so the loader is never raised over a [refresh] that would return early.
     */
    fun retryProfileLoad() {
        if (isResolvingByClientUserId) return
        _state.update { it.copy(isLoading = true, hasProfileLoadFailure = false) }
        refresh()
    }

    fun refresh() =
        viewModelScope.launch {
            // While the clientUserId lookup is still resolving, the target id is unknown — skip
            // rather than fall through to refreshing the connected user (the seeded fallback).
            if (isResolvingByClientUserId) return@launch
            val otherUserId = otherUserId
            if (otherUserId != null) {
                when (val result = profileRepository.fetchProfile(otherUserId)) {
                    is OctopusResult.Success -> _state.update { it.copy(hasProfileLoadFailure = false) }
                    is OctopusResult.Failure ->
                        // One error, one channel: the screen state below when there is no profile to
                        // draw the screen from, the snackbar over a profile already on screen.
                        if (loadFailureChannel(hasVisibleContent = _state.value.profile != null) ==
                            ErrorChannel.Transient
                        ) {
                            showError(result)
                        } else {
                            _state.update {
                                it.copy(
                                    hasProfileLoadFailure = true,
                                    loadFailureIsNoNetwork =
                                        ScreenStateFailure.from(result) == ScreenStateFailure.NoNetwork,
                                )
                            }
                        }
                }
                // First fetch settled (success or failure) — stop showing the preload loader even if
                // no profile came back, so we don't spin forever on an error.
                _state.update { it.copy(isLoading = false) }
            } else if (!_state.value.isOtherUser) {
                // Connected-user mode only. (An unresolved clientUserId leaves isOtherUser = true with
                // no otherUserId — there is nothing to refresh, and we must not fetch the connected
                // user's profile on a screen that was opened for someone else.)
                when (val result = connectionRepository.fetchProfile()) {
                    is OctopusResult.Success -> {}
                    is OctopusResult.Failure ->
                        if (loadFailureChannel(hasVisibleContent = _state.value.profile != null) ==
                            ErrorChannel.Transient
                        ) {
                            showError(result)
                        }
                }
            }
        }
}

/**
 * Whether the Activity screen shows another member's posts rather than the connected user's own
 * activity. `true` only when a specific [requestedUserId] was opened on demand and it differs from
 * the [connectedUserId]. Pure so the self/other routing stays unit-testable independently of the
 * ViewModel wiring (mirrors `profileDestination` / `currentUserActivityDestination`).
 */
internal fun isOtherUserActivity(
    requestedUserId: String?,
    connectedUserId: String?,
): Boolean = requestedUserId != null && requestedUserId != connectedUserId

/**
 * The three modes the Activity screen can run in, resolved once the (possibly async) clientUserId →
 * Octopus-id lookup has settled.
 */
internal enum class ActivityMode { CONNECTED_USER, OTHER_USER, CLIENT_LOOKUP_FAILED }

/**
 * Resolves the [ActivityMode] from the entry-point ids and the resolved target. Pure so the
 * self / other / lookup-failed routing stays unit-testable independently of the ViewModel wiring
 * (mirrors [isOtherUserActivity]).
 * - [ActivityMode.CLIENT_LOOKUP_FAILED]: a [clientUserId] was requested (no plain [userId]) but did
 *   not resolve to an Octopus id — the caller asked for a specific member, so the screen must show
 *   an empty other-user state, never the connected user's own activity.
 * - [ActivityMode.OTHER_USER]: a resolved id that differs from the connected user.
 * - [ActivityMode.CONNECTED_USER]: no id requested, or it resolved to the connected user.
 */
internal fun resolveActivityMode(
    userId: String?,
    clientUserId: String?,
    requestedUserId: String?,
    connectedUserId: String?,
): ActivityMode =
    when {
        userId == null && clientUserId != null && requestedUserId == null ->
            ActivityMode.CLIENT_LOOKUP_FAILED

        isOtherUserActivity(requestedUserId = requestedUserId, connectedUserId = connectedUserId) ->
            ActivityMode.OTHER_USER

        else -> ActivityMode.CONNECTED_USER
    }

/**
 * The tab index the connected-user Activity screen lands on once [ActivityMode.CONNECTED_USER] is
 * resolved, or `null` to keep the destination's own `selectedTabIndex`.
 *
 * When a `userId` / `clientUserId` entry point resolves to the connected user (a self-tap,
 * [hadRequestedId] `true`), those entry points never carry an explicitly-chosen tab — so the
 * unseen-notifications rule applies: Notifications (0) when [hasUnseenNotifications], Posts (2)
 * otherwise (the bell button itself always lands on Notifications, see
 * `currentUserActivityDestination`). With no requested id (the floating-button path),
 * the destination's `selectedTabIndex` was already resolved upstream by
 * [com.octopuscommunity.sdk.ui.profile.current.avatar.currentUserActivityDestination] and stays
 * authoritative → `null`. Pure so the routing stays unit-testable independently of the ViewModel
 * wiring (mirrors iOS `ActivityViewModel.connectedUserInitialTab`).
 *
 * @param hadRequestedId whether the screen was opened for a specific member (`userId` or
 * `clientUserId` entry point) that then resolved to the connected user.
 * @param hasUnseenNotifications whether the connected user has unseen internal notifications.
 */
internal fun connectedUserInitialTabIndex(
    hadRequestedId: Boolean,
    hasUnseenNotifications: Boolean,
): Int? =
    when {
        !hadRequestedId -> null
        hasUnseenNotifications -> OctopusDestination.Activity.Notifications.selectedTabIndex
        else -> OctopusDestination.Activity.Posts.selectedTabIndex
    }
