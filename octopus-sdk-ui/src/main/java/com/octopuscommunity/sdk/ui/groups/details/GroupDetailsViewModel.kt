package com.octopuscommunity.sdk.ui.groups.details

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.OctopusEvent
import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.onFailure
import com.octopuscommunity.sdk.domain.repository.GroupRepository
import com.octopuscommunity.sdk.domain.repository.PostRepository
import com.octopuscommunity.sdk.domain.repository.TranslationRepository
import com.octopuscommunity.sdk.domain.repository.canCreateAnyPostFlow
import com.octopuscommunity.sdk.domain.usecase.FollowGroupUseCase
import com.octopuscommunity.sdk.domain.usecase.PollVoteUseCase
import com.octopuscommunity.sdk.domain.usecase.SetReactionUseCase
import com.octopuscommunity.sdk.domain.usecase.ToggleTranslationUseCase
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.NavigateToGroupAccessDenied
import com.octopuscommunity.sdk.ui.common.UiText.StringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import com.octopuscommunity.sdk.ui.posts.list.PostsListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupDetailsViewModel internal constructor(
    commonParameters: CommonParameters,
    private val groupRepository: GroupRepository,
    private val followGroupUseCase: FollowGroupUseCase,
    private val groupId: String,
) : OctopusViewModel(commonParameters) {
    @Immutable
    data class UiState(
        val group: OctopusGroup? = null,
        val isUpdatingFollow: Boolean = false,
        val hasFetchedGroups: Boolean = false,
        /**
         * `true` when the user landed on this screen via a deeplink/notification that targets
         * a group they cannot access. The screen swaps in the access-denied state (centered
         * text + CTA). Set on first load only — if access is revoked while on screen,
         * [groupAccessLost] takes over and the already-loaded feed stays visible (PRD Rule 5).
         */
        val isAccessDeniedOnArrival: Boolean = false,
        /**
         * Counter bumped when the connected user regains access to this group mid-session
         * (typically an entitlement granted while the screen is open). Included in the post
         * feed VM's Koin key so the VM is recreated and pulls a fresh page from the BE.
         * Mirrors iOS internal change commit `ae03a89`, which resets `postFeedViewModel` on the same
         * transition.
         */
        val feedReloadKey: Int = 0,
        /**
         * `true` iff at least one accessible topic in the community allows post creation by
         * the current user (cross-group check). Used to hide the create-post FAB when no
         * eligible topic exists anywhere — the picker would otherwise show an empty list.
         * Mirrors iOS `GroupDetailViewModel.canCreateAnyPost` (internal change). Defaults to `true`
         * so existing behavior is preserved until topic permissions resolve.
         */
        val canCreateAnyPost: Boolean = true,
        /**
         * `true` when [group] was fetched on its own because the groups list leaves it out: the
         * member may not see it in the list (Visibility right) but may read it (Access right).
         */
        val isOutsideGroupsList: Boolean = false,
    ) {
        val isFollowed get() = group?.isFollowed == true

        /**
         * Hidden for a group outside the groups list: following it there could not be undone from
         * the list, the only other place a follow is managed.
         */
        val showFollowButton get() = group?.canChangeFollowStatus == true && !isOutsideGroupsList
        val groupNotFound get() = hasFetchedGroups && group == null

        /**
         * `true` when the connected user no longer has access to this group — typically because
         * an entitlement was revoked while the screen was open. The already-loaded post feed
         * stays on screen (PRD Rule 5: the user keeps seeing what is already displayed and is
         * not auto-dropped from the screen), but every new action — pull-to-refresh, opening a
         * post, creating a post — routes through the host app's `onGroupAccessDenied` callback
         * instead of executing.
         */
        val groupAccessLost get() = group != null && !group.canAccess
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var isConfirmingGroupDisappearance = false

    init {
        viewModelScope.launch {
            displayedContentCleared.collect {
                val group = _state.value.group
                if (group != null && groupRepository.getGroupFlow(id = groupId).first() == null) {
                    // Even over a check still in flight: that one may have gone out before the
                    // next session was connected.
                    confirmGroupDisappearance(evenIfInFlight = true)
                }
            }
        }
        viewModelScope.launch {
            groupRepository.getGroupFlow(id = groupId).collect { group ->
                val current = _state.value
                when {
                    group != null -> showGroup(group = group, isOutsideGroupsList = false)
                    // First load: the cached list may be stale, or leave out a group the member
                    // can read without seeing it in the list.
                    !current.hasFetchedGroups -> {
                        when (val lookup = lookUpGroup()) {
                            is GroupLookup.Found -> showGroup(lookup.group, lookup.isOutsideGroupsList)
                            GroupLookup.NotAvailable, GroupLookup.Failed -> {
                                _state.update { it.copy(hasFetchedGroups = true) }
                                showAlertDialog(
                                    text = StringResource(R.string.content_detail_not_available),
                                    onDismiss = ::navigateUp,
                                )
                            }
                        }
                    }
                    // A group outside the list is never cached: its absence is expected.
                    current.group != null && !current.isOutsideGroupsList -> confirmGroupDisappearance()
                }
            }
        }
        viewModelScope.launch {
            groupRepository.canCreateAnyPostFlow().collect { canCreate ->
                _state.update { it.copy(canCreateAnyPost = canCreate) }
            }
        }
    }

    /**
     * A group that vanished from the cache is not necessarily gone: the logout also deletes the
     * groups of the previous session, which the next one may still see, and a closure of the
     * community deletes them all. The group stays on screen until a successful lookup confirms it
     * is neither listed nor readable alone; a failed fetch (offline, sent before the next session
     * is connected, or blocked by the closure) keeps it, and [displayedContentCleared] runs the
     * check again once that session exists or the community reopened. Mirrors iOS
     * `GroupDetailViewModel`.
     */
    private fun confirmGroupDisappearance(evenIfInFlight: Boolean = false) {
        if (isConfirmingGroupDisappearance && !evenIfInFlight) return
        isConfirmingGroupDisappearance = true
        viewModelScope.launch {
            val lookup = lookUpGroup()
            isConfirmingGroupDisappearance = false
            when (lookup) {
                // A listed group reaches the screen through the cache the groups fetch refreshed.
                is GroupLookup.Found -> if (lookup.isOutsideGroupsList) showGroup(lookup.group, isOutsideGroupsList = true)
                GroupLookup.NotAvailable -> _state.update { it.copy(group = null, isOutsideGroupsList = false) }
                GroupLookup.Failed -> Unit
            }
        }
    }

    private sealed interface GroupLookup {
        data class Found(
            val group: OctopusGroup,
            val isOutsideGroupsList: Boolean,
        ) : GroupLookup

        /** Neither listed nor readable alone: the group is gone, or the member lacks Access. */
        data object NotAvailable : GroupLookup

        data object Failed : GroupLookup
    }

    /**
     * Looks the group up in a fresh groups list, then on its own when the list leaves it out. The
     * list only holds the groups the member may see (Visibility right); reading a group only takes
     * the Access right, so a group outside the list is kept only when [OctopusGroup.canAccess].
     */
    private suspend fun lookUpGroup(): GroupLookup {
        when (val groups = groupRepository.fetchGroups()) {
            is OctopusResult.Success -> groups.data.firstOrNull { it.id == groupId }?.let { return GroupLookup.Found(it, false) }
            is OctopusResult.Failure -> return GroupLookup.Failed
        }
        return when (val single = groupRepository.fetchGroup(id = groupId)) {
            is OctopusResult.Success ->
                single.data
                    ?.takeIf { it.canAccess }
                    ?.let { GroupLookup.Found(it, isOutsideGroupsList = true) }
                    ?: GroupLookup.NotAvailable
            is OctopusResult.Failure -> GroupLookup.Failed
        }
    }

    private fun showGroup(
        group: OctopusGroup,
        isOutsideGroupsList: Boolean,
    ) {
        val previousGroup = _state.value.group
        val isFirstFetch = !_state.value.hasFetchedGroups
        // Mid-session access transition: the connected user regained access to this group
        // (entitlement granted while the screen was open). Bump the feed reload key so the post
        // feed VM is recreated and pulls a fresh page — mirrors iOS internal change commit ae03a89.
        val groupAccessGained = previousGroup != null && !previousGroup.canAccess && group.canAccess
        _state.update {
            it.copy(
                group = group,
                hasFetchedGroups = true,
                isOutsideGroupsList = isOutsideGroupsList,
                isAccessDeniedOnArrival = it.isAccessDeniedOnArrival || (isFirstFetch && !group.canAccess),
                feedReloadKey = if (groupAccessGained) it.feedReloadKey + 1 else it.feedReloadKey,
            )
        }
    }

    fun navigateToCreatePost() {
        val group = _state.value.group
        // PRD Rule 5: opening create-post from a group whose access has been revoked must route
        // to the access-denied callback rather than navigate. PRD Rule 6 covers the case where
        // access is revoked between picker selection and publish — that is enforced server-side.
        if (group != null && !group.canAccess) {
            notifyGroupAccessDenied()
            return
        }
        navigateToCreatePost(groupId = groupId)
    }

    fun setFollowed(follow: Boolean) {
        val group = _state.value.group ?: return
        if (!group.canAccess) {
            notifyGroupAccessDenied()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isUpdatingFollow = true) }
            followGroupUseCase(group = group, follow = follow)
                .onFailure(::showError)
            _state.update { it.copy(isUpdatingFollow = false) }
        }
    }

    /**
     * Routes a tap that targets this locked group to the host app's access-denied callback.
     * Called when the user attempts a new action (refresh, follow, create post, …) while
     * [UiState.groupAccessLost] is `true`.
     */
    fun notifyGroupAccessDenied() {
        viewModelScope.launch {
            navigator.sendNavigationEvent(NavigateToGroupAccessDenied(groupId))
        }
    }

    /**
     * internal ticket: tap on the Topic CTA button shown in the group detail header. Fires the
     * `TopicCtaButtonClicked` analytics event then routes the operator-provided URL through
     * the host app's `onNavigateToUrl` callback — same surface as the post CTA flow in
     * [OctopusViewModel.navigateToCustomAction].
     */
    @Suppress("DEPRECATION")
    fun navigateToCustomAction(group: OctopusGroup) =
        group.customAction?.targetUrl?.let { url ->
            viewModelScope.launch { trackerRepository.trackTopicCtaButtonClicked(group.id) }
            navigateToUrl(url = url)
        }
}

class GroupPostsListViewModel internal constructor(
    commonParameters: CommonParameters,
    postRepository: PostRepository,
    translationRepository: TranslationRepository,
    toggleTranslationUseCase: ToggleTranslationUseCase,
    setReactionUseCase: SetReactionUseCase,
    pollVoteUseCase: PollVoteUseCase,
    feedId: String,
    private val groupId: String,
    private val origin: OctopusDestination.Origin,
) : PostsListViewModel(
        commonParameters = commonParameters,
        postRepository = postRepository,
        translationRepository = translationRepository,
        toggleTranslationUseCase = toggleTranslationUseCase,
        setReactionUseCase = setReactionUseCase,
        pollVoteUseCase = pollVoteUseCase,
        feedId = feedId,
    ) {
    init {
        viewModelScope.launch {
            eventRepository.emitEvent(
                OctopusEvent.ScreenDisplayed.GroupDetail(
                    groupId = groupId,
                    source =
                        when (origin) {
                            OctopusDestination.Origin.CLIENT_APP -> OctopusEvent.ScreenDisplayed.GroupDetail.Source.BRIDGE
                            OctopusDestination.Origin.SDK -> OctopusEvent.ScreenDisplayed.GroupDetail.Source.COMMUNITY
                        },
                ),
            )
        }
    }
}
