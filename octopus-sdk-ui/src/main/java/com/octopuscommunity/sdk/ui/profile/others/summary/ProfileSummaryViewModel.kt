package com.octopuscommunity.sdk.ui.profile.others.summary

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.OctopusEvent.CommentCreated
import com.octopuscommunity.sdk.domain.model.OctopusEvent.CommentDeleted
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostCreated
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostDeleted
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReactionModified
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReplyCreated
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReplyDeleted
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ScreenDisplayed
import com.octopuscommunity.sdk.domain.model.OtherUserProfile
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.getOrNull
import com.octopuscommunity.sdk.domain.repository.ProfileRepository
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
 * ViewModel backing the [ProfileSummaryScreen] (another member's public, read-only profile).
 *
 * Two entry points, resolved from [userId] / [clientUserId]:
 * - **Plain [userId]** (in-app profile taps, [com.octopuscommunity.sdk.ui.navigateToOctopusProfile]):
 *   the id is known synchronously, no lookup needed — but a stale/unknown/deleted id still fetches
 *   no profile; [refresh]'s first settled fetch then shows the generic error state too (see
 *   [profileNotFound]), instead of spinning forever (internal ticket follow-up).
 * - **[clientUserId]** (Unified Profile,
 *   [com.octopuscommunity.sdk.ui.navigateToOctopusProfileByClientUserId]): resolved to its Octopus id
 *   via [ProfileRepository.fetchProfileByClientUserId]. A [clientUserId] that does not resolve
 *   (unknown/deleted mapping, network failure, or the community not exposing client ids) shows the
 *   generic error state (see [profileClientLookupFailed]) instead of an indefinite spinner — it never
 *   falls back to the connected user's own profile. Mirrors
 *   `com.octopuscommunity.sdk.ui.profile.activity.ActivityViewModel`'s `CLIENT_LOOKUP_FAILED` mode.
 */
class ProfileSummaryViewModel(
    commonParameters: CommonParameters,
    private val profileRepository: ProfileRepository,
    private val userId: String?,
    private val clientUserId: String? = null,
) : OctopusViewModel(commonParameters) {
    @Immutable
    data class UiState(
        val profile: OtherUserProfile? = null,
        val communityConfig: CommunityConfig? = null,
        val currentUserProfile: CurrentUserProfile? = null,
        val isLoading: Boolean = false,
        val showMenuSheet: Boolean = false,
        /**
         * `true` when this screen was opened via `navigateToOctopusProfileByClientUserId` and the
         * `clientUserId` did not resolve to an Octopus id (unknown/deleted mapping, network failure,
         * or the community not exposing client ids). Drives the generic error state instead of an
         * indefinite spinner (internal ticket).
         */
        val lookupFailed: Boolean = false,
        /**
         * `true` when this screen was opened via a plain `userId`
         * ([com.octopuscommunity.sdk.ui.navigateToOctopusProfile]) and [refresh]'s first fetch
         * settled with no profile ever found (stale/unknown/deleted id). Same generic error state as
         * [lookupFailed] — never an indefinite spinner — but driven by a different signal: this path
         * has no async id lookup to fail, only a fetch that can come back empty (internal ticket follow-up).
         */
        val profileNotFound: Boolean = false,
        /** Whether the failure behind [profileNotFound] was a lost connection rather than an unknown id. */
        val loadFailureIsNoNetwork: Boolean = false,
    ) {
        val hasMoreActions =
            profile?.canBeReported(currentUserProfile) == true ||
                profile?.canBeBlocked(currentUserProfile) == true
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    /**
     * Id of the member whose profile to fetch on [refresh], resolved once in [init]. Held as a
     * field (mirrors `ActivityViewModel.otherUserId`) rather than re-derived from [userId], since
     * the [clientUserId] path only knows it once the lookup settles. Confined to the main thread
     * (all [viewModelScope] launches run on `Dispatchers.Main.immediate`), so no synchronization is
     * needed.
     */
    private var resolvedUserId: String? = userId

    /**
     * `true` while the [clientUserId] entry point's async id lookup is still in flight. Gates
     * [refresh] to a no-op until then, so an early [onResume] can't fall through to fetching a stale
     * / absent id. Mirrors `ActivityViewModel.isResolvingByClientUserId`.
     */
    private var isResolvingByClientUserId: Boolean = userId == null && clientUserId != null

    init {
        viewModelScope.launch {
            currentUserProfile.collectLatest { profile ->
                _state.update { it.copy(currentUserProfile = profile) }
            }
        }
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
        // Unified Profile: a client-user-id entry point (navigateToOctopusProfileByClientUserId) is
        // resolved to its Octopus id first, through the debug-aware repository lookup. A plain
        // [userId] entry point skips the lookup entirely (mirrors ActivityViewModel).
        viewModelScope.launch { resolveTarget() }
    }

    /**
     * Resolves [resolvedUserId] from the entry-point ids, then starts collecting that member's
     * profile. Extracted from [init] so [retryFirstLoad] can re-run the lookup itself: a
     * `clientUserId` that never resolved has no id for [refresh] to fetch, so retrying the fetch
     * alone would be a guaranteed no-op.
     *
     * Idempotent by construction: it only starts [collectProfile] and emits
     * [ScreenDisplayed.OtherUserProfile] on the run that actually resolves an id, and once
     * [resolvedUserId] is set [retryFirstLoad] never calls it again — so neither the collector nor
     * the screen event is ever duplicated.
     */
    private suspend fun resolveTarget() {
        val requestedUserId =
            userId ?: clientUserId?.let {
                profileRepository.fetchProfileByClientUserId(it).getOrNull()?.userId
            }
        // The async id lookup (if any) has settled — let [refresh] run again.
        isResolvingByClientUserId = false

        if (profileClientLookupFailed(userId = userId, clientUserId = clientUserId, requestedUserId = requestedUserId)) {
            // No id to fetch: show the generic error state — must NOT fall back to the
            // connected user's own profile, the host asked for a specific member.
            resolvedUserId = null
            _state.update { it.copy(isLoading = false, lookupFailed = true) }
        } else {
            resolvedUserId = requestedUserId
            requestedUserId?.let { resolved ->
                collectProfile(resolved)
                eventRepository.emitEvent(ScreenDisplayed.OtherUserProfile(profileId = resolved))
            }
        }
    }

    private fun collectProfile(userId: String) {
        viewModelScope.launch {
            profileRepository.getProfileFlow(userId).collect { profile ->
                _state.update { it.copy(profile = profile) }
            }
        }
    }

    fun onResume() = refresh()

    fun refresh() =
        viewModelScope.launch {
            // While the clientUserId lookup is still resolving, the target id is unknown — skip rather
            // than fetch a stale/absent id.
            if (isResolvingByClientUserId) return@launch
            val resolvedUserId = resolvedUserId ?: return@launch
            _state.update { it.copy(isLoading = true) }
            val result = profileRepository.fetchProfile(resolvedUserId)
            // The profile's own feed carries a screen state for the same outage, so reporting it
            // here too would say it twice. Only speak up over a profile already on screen.
            if (result is OctopusResult.Failure &&
                loadFailureChannel(hasVisibleContent = _state.value.profile != null) ==
                ErrorChannel.Transient
            ) {
                showError(result)
            }
            // First fetch settled (success or failure) — stop showing the spinner even if no profile
            // came back. Read the fetch's OWN result here, never the flow-derived `it.profile` in
            // state: collectProfile()'s Room-backed flow re-emits asynchronously and is not guaranteed
            // to have caught up yet, which previously caused a false "not found" flash on a valid,
            // freshly-fetched profile (cold-cache race). The cast is safe, not a shortcut: this fetch
            // overload's declared return type is the base `Profile` interface (unlike the sibling
            // getProfileFlow(userId), which is already typed `OtherUserProfile?`), but
            // ProfileRepositoryImpl.fetchProfile(userId) only ever maps a success payload through
            // PublicProfileEntity.toDomain(), which always constructs an OtherUserProfile. On the
            // plain-userId path (never clientUserId, which has its own lookupFailed routing above) a
            // fetch that came back with no profile means a stale/unknown/deleted id: show the generic
            // error state instead of spinning forever.
            _state.update {
                it.copy(
                    isLoading = false,
                    profileNotFound = userIdProfileNotFound(userId = userId, profile = result.getOrNull() as? OtherUserProfile),
                    loadFailureIsNoNetwork =
                        result is OctopusResult.Failure &&
                            ScreenStateFailure.from(result) == ScreenStateFailure.NoNetwork,
                )
            }
        }

    /**
     * Re-runs the first load, from the error state's CTA. Raises [UiState.isLoading] and clears
     * [UiState.lookupFailed] / [UiState.profileNotFound] before fetching, because the screen renders
     * the error state before the loader: without it the error state — Retry button included — stays
     * frozen on screen for the whole round trip and the button looks dead. Mirrors
     * `GroupsListViewModel.retryFirstLoad`.
     *
     * A `clientUserId` that never resolved has no [resolvedUserId] for [refresh] to fetch, so
     * [refresh] would return early and the retry would do literally nothing — retry the id lookup
     * itself first in that case.
     */
    fun retryFirstLoad() {
        viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true, lookupFailed = false, profileNotFound = false)
            }
            if (resolvedUserId == null) resolveTarget()
            refresh().join()
        }
    }

    fun showMenuSheet(show: Boolean) {
        _state.update { it.copy(showMenuSheet = show) }
    }
}

/**
 * Whether the [ProfileSummaryScreen]'s `clientUserId` entry point failed to resolve to an Octopus
 * id — a [clientUserId] was requested (no plain [userId]) but the lookup came back empty (unknown /
 * deleted mapping, network failure, or the community not exposing client ids). Pure so the routing
 * stays unit-testable independently of the ViewModel wiring (mirrors
 * `com.octopuscommunity.sdk.ui.profile.activity.resolveActivityMode`'s `CLIENT_LOOKUP_FAILED`
 * branch).
 */
internal fun profileClientLookupFailed(
    userId: String?,
    clientUserId: String?,
    requestedUserId: String?,
): Boolean = userId == null && clientUserId != null && requestedUserId == null

/**
 * Whether the [ProfileSummaryScreen]'s plain-`userId` entry point must show the generic error state
 * because [ProfileSummaryViewModel.refresh]'s first fetch settled with no profile ever found (a
 * stale/unknown/deleted id). Unlike [profileClientLookupFailed], this path has no async id lookup to
 * fail — the id is known synchronously — so the signal is a settled fetch with nothing to show
 * instead. Pure so the routing stays unit-testable independently of the ViewModel wiring.
 */
internal fun userIdProfileNotFound(
    userId: String?,
    profile: OtherUserProfile?,
): Boolean = userId != null && profile == null
