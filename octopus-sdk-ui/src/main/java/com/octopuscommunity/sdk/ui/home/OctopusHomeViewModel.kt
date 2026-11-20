package com.octopuscommunity.sdk.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.data.repository.windowFeeds
import com.octopuscommunity.sdk.domain.model.OctopusEvent
import com.octopuscommunity.sdk.domain.model.RootFeed
import com.octopuscommunity.sdk.domain.network.onFailure
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.CommentRepository
import com.octopuscommunity.sdk.domain.repository.GroupRepository
import com.octopuscommunity.sdk.domain.repository.PostRepository
import com.octopuscommunity.sdk.domain.repository.RootFeedRepository
import com.octopuscommunity.sdk.domain.repository.TranslationRepository
import com.octopuscommunity.sdk.domain.repository.canCreateAnyPostFlow
import com.octopuscommunity.sdk.domain.usecase.PollVoteUseCase
import com.octopuscommunity.sdk.domain.usecase.SetReactionUseCase
import com.octopuscommunity.sdk.domain.usecase.ToggleTranslationUseCase
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.common.ErrorChannel
import com.octopuscommunity.sdk.ui.common.ScreenStateFailure
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.errorText
import com.octopuscommunity.sdk.ui.common.loadFailureChannel
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import com.octopuscommunity.sdk.ui.common.viewmodel.WindowFeeds
import com.octopuscommunity.sdk.ui.posts.list.PostsListViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OctopusHomeViewModel internal constructor(
    commonParameters: CommonParameters,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val rootFeedRepository: RootFeedRepository,
    groupRepository: GroupRepository,
) : OctopusViewModel(commonParameters) {
    @Immutable
    data class UiState(
        val isLoading: Boolean = true,
        val feed: RootFeed? = null,
        val error: UiText? = null,
        /** Whether the failure behind [error] was a lost connection rather than anything else. */
        val loadFailureIsNoNetwork: Boolean = false,
        /**
         * `true` iff at least one accessible topic allows post creation. Used to hide the
         * cross-group "Create post" FAB when no eligible topic exists. Mirrors iOS
         * `PostListViewModel.canCreatePost` (internal change). Defaults to `true` so existing
         * behavior is preserved until topic permissions resolve.
         */
        val canCreatePost: Boolean = true,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var rootFeedsJob: Job? = null

    init {
        viewModelScope.launch {
            // First the windows an earlier launch left behind (all feeds share one cache), or
            // their items would stay referenced, and never be deleted.
            commentRepository.windowFeeds().deleteLocalFeeds(
                idMarker = WindowFeeds.ID_MARKER,
                keptIdMarker = WindowFeeds.launchIdMarker,
            )
            postRepository.deleteLocalOrphanedItems()
            commentRepository.deleteLocalOrphanedItems()
        }
        viewModelScope.launch {
            rootFeedRepository.getRootFeeds().collect { feeds ->
                val feed = feeds.firstOrNull()
                // The channel below is decided when the network call returns, but the cached feed
                // arrives on its own Room flow: offline on a cold start, the fetch can fail before
                // the cache emits, so the failure legitimately takes the inline slot — and then the
                // feed lands, OctopusHomeContent switches to its `feed != null` branch and stops
                // rendering [error]. The error would be swallowed for good, since fetchRootFeeds()
                // is init-only. Hand it to the snackbar instead of dropping it.
                // Single CAS: the displaced error is read from the state actually exchanged,
                // so a failure landing between a separate read and update cannot be erased
                // unseen — correctness is structural, not dependent on main-thread confinement.
                val previous =
                    _state.getAndUpdate { state ->
                        state.copy(feed = feed, error = state.error.takeIf { feed == null })
                    }
                previous.error?.takeIf { feed != null }?.let {
                    showErrorSnackbar(
                        message = it,
                        isNoNetwork = previous.loadFailureIsNoNetwork,
                    ) { retryFirstLoad() }
                }
            }
        }
        reloadRootFeeds(showLoading = false)
        // A cold start while access is denied can leave the initial root-feed request failed.
        // Restoring access keeps the same profile, so the session collectors below do not retry it.
        viewModelScope.launch {
            connectionRepository
                .getUserConfigFlow()
                .filterNotNull()
                .map { it.canAccessCommunity }
                .distinctUntilChanged()
                .drop(1)
                .filter { it }
                .collect { reloadRootFeeds(showLoading = true) }
        }
        // The logout deleted the cached root feeds with the rest of the member's content: load
        // them again for the next session, or the home has nothing left to draw.
        viewModelScope.launch {
            userContentCleared.collect {
                reloadRootFeeds(showLoading = true)
            }
        }
        // The root feeds are fetched for the session, and the guest's are not the member's. Only
        // they are, so that a sign-in stays cheap: the screen stays as it is while they load, and
        // the rest of what is on it keeps what was loaded for the guest.
        viewModelScope.launch {
            guestBecameMember.collect { reloadRootFeeds(showLoading = false) }
        }
        viewModelScope.launch {
            connectionRepository.fetchProfile()
        }
        viewModelScope.launch {
            groupRepository.canCreateAnyPostFlow().collect { canCreate ->
                _state.update { it.copy(canCreatePost = canCreate) }
            }
        }
    }

    /**
     * Fetches the root feeds and reports a failure the way the design asks: as the screen's own
     * state when there is no feed to draw the screen from, as a snackbar over a topic list already
     * on screen.
     */
    private suspend fun fetchRootFeeds() {
        rootFeedRepository
            .fetchRootFeeds()
            .also { currentCoroutineContext().ensureActive() }
            .onFailure { failure ->
                // One error, one channel (see loadFailureChannel). With a cached feed on screen
                // the inline slot is taken — replacing the topic list with an error message would
                // drop what the user is reading — so the failure goes to the snackbar. It cannot
                // be dropped on the grounds that the feed's own PostsList reports the same
                // outage: a failure specific to the root-feeds endpoint (PermissionDenied,
                // StatusError) leaves the posts endpoint answering fine, and the user would get
                // no signal at all behind a stale topic list.
                val channel = loadFailureChannel(hasVisibleContent = _state.value.feed != null)
                _state.update {
                    it.copy(
                        error = failure.errorText.takeIf { channel == ErrorChannel.Inline },
                        loadFailureIsNoNetwork =
                            ScreenStateFailure.from(failure) == ScreenStateFailure.NoNetwork,
                    )
                }
                // Snackbar, not showError: a load failure never escalates to the modal alert —
                // that one is reserved for actions the user explicitly triggered.
                if (channel == ErrorChannel.Transient) {
                    showErrorSnackbar(
                        message = failure.errorText,
                        isNoNetwork = ScreenStateFailure.from(failure) == ScreenStateFailure.NoNetwork,
                    ) { retryFirstLoad() }
                }
            }.onSuccess {
                _state.update { it.copy(error = null) }
            }
        _state.update { it.copy(isLoading = false) }
    }

    /**
     * Re-runs the root-feed load, from the error state's CTA. Raises [UiState.isLoading] and clears
     * [UiState.error] before fetching, because the screen renders the loader before the error
     * state: without it the error state — Retry button included — would stay frozen on screen for
     * the whole round trip and the button would look dead. Mirrors
     * `GroupsListViewModel.retryFirstLoad`.
     */
    fun retryFirstLoad() {
        reloadRootFeeds(showLoading = true)
    }

    private fun reloadRootFeeds(showLoading: Boolean) {
        rootFeedsJob?.cancel()
        if (showLoading) _state.update { it.copy(isLoading = true, error = null) }
        rootFeedsJob = viewModelScope.launch { fetchRootFeeds() }
    }

    fun onResume() {
    }

    fun navigateToGroups() {
        navigateTo(OctopusDestination.Groups)
    }
}

class HomePostsListViewModel internal constructor(
    commonParameters: CommonParameters,
    postRepository: PostRepository,
    translationRepository: TranslationRepository,
    toggleTranslationUseCase: ToggleTranslationUseCase,
    setReactionUseCase: SetReactionUseCase,
    pollVoteUseCase: PollVoteUseCase,
    val feed: RootFeed,
) : PostsListViewModel(
        commonParameters = commonParameters,
        postRepository = postRepository,
        translationRepository = translationRepository,
        toggleTranslationUseCase = toggleTranslationUseCase,
        setReactionUseCase = setReactionUseCase,
        pollVoteUseCase = pollVoteUseCase,
        feedId = feed.id,
    ) {
    init {
        viewModelScope.launch {
            eventRepository.getEventsFlow().collect {
                when (it) {
                    is OctopusEvent.GroupFollowingChanged -> reload()
                    else -> {}
                }
            }
        }
        viewModelScope.launch {
            eventRepository.emitEvent(
                OctopusEvent.ScreenDisplayed.MainFeed(feedId = feed.id),
            )
        }
    }
}
