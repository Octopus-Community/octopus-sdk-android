package com.octopuscommunity.sdk.ui.groups.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.onFailure
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.GroupRepository
import com.octopuscommunity.sdk.domain.usecase.FollowGroupUseCase
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.ErrorChannel
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.NavigateToGroupAccessDenied
import com.octopuscommunity.sdk.ui.common.ScreenStateFailure
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.errorText
import com.octopuscommunity.sdk.ui.common.loadFailureChannel
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import com.octopuscommunity.sdk.ui.groups.list.GroupsListViewModel.UiState.Group
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupsListViewModel internal constructor(
    commonParameters: CommonParameters,
    private val groupRepository: GroupRepository,
    private val followGroupUseCase: FollowGroupUseCase,
) : OctopusViewModel(commonParameters) {
    @Immutable
    data class UiState(
        val groups: List<Group> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        @Deprecated(
            "Dead since the screen states landed: never read, and no longer written. " +
                "Use hasLoadFailure / loadFailureIsNoNetwork for a failed first load, and the error " +
                "snackbar for a failure over content already on screen. Removed in the next major.",
            level = DeprecationLevel.WARNING,
        )
        val error: UiText? = null,
        /**
         * Set when the first load failed with no group listed behind it. A failure over a list
         * already displayed goes to the snackbar instead (see `loadFailureChannel`).
         */
        val hasLoadFailure: Boolean = false,
        val loadFailureIsNoNetwork: Boolean = false,
    ) {
        @Immutable
        sealed class Group {
            abstract val id: String
            abstract val topics: List<OctopusGroup>

            abstract fun setIsFollowed(
                groupId: String,
                isFollowed: Boolean,
            ): Group

            protected fun List<OctopusGroup>.updateIsFollowed(
                groupId: String,
                isFollowed: Boolean,
            ) = map { if (it.id == groupId) it.copy(isFollowed = isFollowed) else it }

            @Immutable
            data class NoSection(
                override val topics: List<OctopusGroup>,
            ) : Group() {
                override val id = "no_section"

                override fun setIsFollowed(
                    groupId: String,
                    isFollowed: Boolean,
                ) = copy(topics = topics.updateIsFollowed(groupId, isFollowed))
            }

            @Immutable
            data class Section(
                override val id: String,
                val label: UiText,
                override val topics: List<OctopusGroup>,
            ) : Group() {
                override fun setIsFollowed(
                    groupId: String,
                    isFollowed: Boolean,
                ) = copy(topics = topics.updateIsFollowed(groupId, isFollowed))
            }
        }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            loadGroups()
        }
        // Refetch the groups when the connected user's entitlements change — typically after
        // a successful `OctopusSDK.refreshEntitlements()` call. Mirrors iOS `EntitlementMonitor`
        // triggering `topicsRepository.fetchTopics()` on entitlement changes, so locked rows
        // unlock (or new rows surface) without requiring the user to leave and re-open the
        // screen. `distinctUntilChanged` filters identical refreshes and `drop(1)` skips the
        // synthetic startup emission.
        viewModelScope.launch {
            currentUserProfile
                .map { it?.entitlements ?: emptySet() }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    fetchGroups()
                    loadGroups()
                }
        }
        viewModelScope.launch {
            displayedContentCleared.collect {
                fetchGroups()
                loadGroups()
            }
        }
    }

    /**
     * Loads groups from current DB state. Not reactive: follow/unfollow only updates button state
     * locally, groups stay in place. Groups are reloaded on next screen opening or pull-to-refresh.
     */
    private suspend fun loadGroups() {
        val groups = groupRepository.getGroupsFlow().first()
        val sections = groupRepository.getSectionsFlow().first()
        _state.update { it.copy(isLoading = false, groups = buildGroupBlocks(groups, sections)) }
    }

    /**
     * Fetches the groups and reports a failure the way the design asks: as the screen's own state
     * when there is nothing listed behind it, as a snackbar over a list already on screen.
     */
    private suspend fun fetchGroups() {
        when (val result = groupRepository.fetchGroups()) {
            is OctopusResult.Success -> _state.update { it.copy(hasLoadFailure = false) }
            is OctopusResult.Failure -> {
                val channel = loadFailureChannel(hasVisibleContent = _state.value.groups.isNotEmpty())
                _state.update {
                    it.copy(
                        hasLoadFailure = channel == ErrorChannel.Inline,
                        loadFailureIsNoNetwork =
                            ScreenStateFailure.from(result) == ScreenStateFailure.NoNetwork,
                    )
                }
                if (channel == ErrorChannel.Transient) {
                    showErrorSnackbar(
                        message = result.errorText,
                        isNoNetwork = ScreenStateFailure.from(result) == ScreenStateFailure.NoNetwork,
                    ) { refresh() }
                }
            }
        }
    }

    /** Re-runs the first load, from the error state's CTA. */
    fun retryFirstLoad() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, hasLoadFailure = false) }
            fetchGroups()
            loadGroups()
        }
    }

    fun onResume() {
        viewModelScope.launch {
            loadGroups()
        }
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            fetchGroups()
            loadGroups()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun setFollowed(
        group: OctopusGroup,
        follow: Boolean,
    ) {
        // Visible-but-locked group: route the follow tap to the host app's access-denied
        // callback instead of (un)following. Mirrors PRD Rule 13.
        if (!group.canAccess) {
            notifyGroupAccessDenied(group.id)
            return
        }
        if (!follow) {
            val followedCount = countFollowedTopics(_state.value.groups)
            if (followedCount <= 1) {
                showErrorSnackbar(UiText.StringResource(R.string.group_action_unfollow_error_last_followed_group_formal))
                return
            }
        }
        viewModelScope.launch {
            followGroupUseCase(group = group, follow = follow)
                .onSuccess {
                    // Update only the button state locally — groups stay in place
                    _state.update { state ->
                        state.copy(
                            groups =
                                state.groups.map { section ->
                                    section.setIsFollowed(group.id, follow)
                                },
                        )
                    }
                }.onFailure(::showError)
        }
    }

    /**
     * Routes a tap on a visible-but-locked group to the host app's access-denied callback.
     * Called by [GroupsList] when the user taps a row whose [OctopusGroup.canAccess] is `false`.
     */
    fun notifyGroupAccessDenied(groupId: String) {
        viewModelScope.launch {
            navigator.sendNavigationEvent(NavigateToGroupAccessDenied(groupId))
        }
    }
}
