package com.octopuscommunity.sdk.ui.posts.details

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.OctopusEvent.CommentCreated
import com.octopuscommunity.sdk.domain.model.OctopusEvent.CommentDeleted
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostDeleted
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ProfileModified
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ScreenDisplayed
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.model.TrackerEvent
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.PostRepository
import com.octopuscommunity.sdk.domain.repository.TranslationRepository
import com.octopuscommunity.sdk.domain.usecase.PollVoteUseCase
import com.octopuscommunity.sdk.domain.usecase.SetReactionUseCase
import com.octopuscommunity.sdk.domain.usecase.ToggleTranslationUseCase
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.common.ErrorChannel
import com.octopuscommunity.sdk.ui.common.ScreenStateFailure
import com.octopuscommunity.sdk.ui.common.errorText
import com.octopuscommunity.sdk.ui.common.loadFailureChannel
import com.octopuscommunity.sdk.ui.posts.common.PostViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostDetailsViewModel(
    commonParameters: CommonParameters,
    translationRepository: TranslationRepository,
    toggleTranslationUseCase: ToggleTranslationUseCase,
    setReactionUseCase: SetReactionUseCase,
    pollVoteUseCase: PollVoteUseCase,
    private val postRepository: PostRepository,
    private val postId: String,
    private val origin: OctopusDestination.Origin,
    private val hasFeaturedComment: Boolean,
) : PostViewModel(
        commonParameters,
        translationRepository,
        toggleTranslationUseCase,
        setReactionUseCase,
        pollVoteUseCase,
    ) {
    @Immutable
    data class UiState(
        val post: Post? = null,
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val draftComment: Comment.Draft? = null,
        val showUnsavedChangesDialog: Boolean = false,
        /**
         * `true` when the post is unavailable (deleted server-side, moderated, or in a
         * group the connected user does not have access to and is not the author of — the
         * "locked other" case). The screen swaps in an in-screen empty state instead of
         * the post body and comments. Mirrors iOS `postNotAvailable` flag combined with
         * `LockedContentState.lockedOther` in internal change commit d655011.
         */
        val isContentUnavailable: Boolean = false,
        /**
         * GDPR carve-out: the connected user authored this post in a group they no longer
         * have access to. The BE returns `canAccess = true` + `canCreateChildren = false`,
         * with empty children and zeroed counts server-side. The post body stays visible
         * to the author but the comment feed and the "add a comment" input are hidden.
         * Mirrors iOS `LockedContentState.lockedOwnContent` in internal change commit d655011.
         */
        val isLockedOwnContent: Boolean = false,
        /**
         * Set when a load failed with nothing on screen behind it, which is what the inline screen
         * state renders (illustration, message, Retry). A failure over a post already displayed
         * goes to the snackbar instead — one error, one channel, see
         * [com.octopuscommunity.sdk.ui.common.loadFailureChannel].
         */
        val hasLoadFailure: Boolean = false,
        val loadFailureIsNoNetwork: Boolean = false,
    ) {
        val hasUnsavedChanges
            get() = draftComment?.hasContent == true

        /**
         * `true` when the comment list below the post is on screen *and* able to render this
         * screen's load failure inline on its own.
         *
         * Three things have to hold. The post must be displayed, since the list is only composed in
         * that branch. It must not be the locked-own carve-out, whose screen hides the list
         * entirely. And its comment feed must have an id: `OctopusItemsListViewModel` returns
         * before fetching anything when `feeds.ascendingId` is null (a post predating the feed-id
         * rollout), so the list renders its empty placeholder and never surfaces a failure at all.
         *
         * This is what [PostDetailsViewModel.fetchPost] defers to before staying quiet — the
         * question is whether an inline surface exists, not what the post's own state is.
         */
        internal val hasCommentListSurface
            get() =
                post != null &&
                    !isLockedOwnContent &&
                    post.commentsFeeds.ascendingId != null

        /**
         * `true` when the SDK can show the "add a comment" input below the post. `false`
         * for the GDPR carve-out (own content in a now non-accessible group), or when the
         * BE signals `canCreateChildren = false` for any other reason (internal ticket write
         * restrictions). Mirrors iOS `lockedState != .lockedOwnContent` gate around
         * `CreateCommentView` in d655011.
         */
        val canCreateComment get() = !isLockedOwnContent && post?.canCreateChildren != false
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var hasFetched = false

    init {
        viewModelScope.launch {
            // Combined with the connected user rather than reading `currentUserProfile.value`: that
            // one is a `stateIn(WhileSubscribed, initialValue = null)` whose upstream starts a
            // dispatcher turn later, so a local emission arriving first resolved an own
            // non-accessible post as *locked other* — and, read synchronously, was never
            // re-evaluated when the profile did arrive (internal change).
            combine(
                postRepository.getLocalItemFlow(itemId = postId),
                currentUserProfile,
            ) { post, profile -> post to profile }.collect { (post, profile) ->
                // A cache miss is not a completed load. Keep the initial loader until fetch ends.
                if (post == null) return@collect
                // Resolve the iOS LockedContentState mirror inline (d655011):
                //   canAccess = true                              → unlocked
                //   canAccess = false + author == connected user  → lockedOwnContent
                //                                                   (post stays visible,
                //                                                   feed + input hidden)
                //   canAccess = false otherwise                   → lockedOther
                //                                                   (in-screen empty state)
                val currentUserId = profile?.userId
                val isOwn = currentUserId != null && post.author?.userId == currentUserId
                when {
                    post.canAccess ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                post = post,
                                isContentUnavailable = false,
                                isLockedOwnContent = false,
                            )
                        }

                    isOwn ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                post = post,
                                isContentUnavailable = false,
                                isLockedOwnContent = true,
                            )
                        }

                    else ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                post = null,
                                isContentUnavailable = true,
                                isLockedOwnContent = false,
                            )
                        }
                }
            }
        }

        viewModelScope.launch {
            currentUserProfile.mapNotNull { it?.blockedProfiles }.collectLatest { blockedProfiles ->
                _state.value.post?.let { post ->
                    if (post.author?.userId in blockedProfiles.map { it.userId }) {
                        navigateUp()
                    }
                }
            }
        }

        viewModelScope.launch {
            eventRepository.getEventsFlow().collect { event ->
                when (event) {
                    // Parent-only trigger: OctopusItemsListViewModel's own ProfileModified handler
                    // (fetchItems(itemIds)) discards its result, so the list surfaces nothing here.
                    is ProfileModified -> fetchPost()
                    is CommentCreated, is CommentDeleted ->
                        if (event.postId == postId) {
                            fetchCounters()
                        }

                    is PostDeleted -> if (event.postId == postId) navigateUp()
                    else -> {}
                }
            }
        }
        // The logout, or the closure of the community, deleted the cached post: the screen keeps
        // showing it, and asks the server whether it is still there. The comment list reloads on
        // its own, in both cases.
        viewModelScope.launch { displayedContentCleared.collect { fetchPost() } }

        viewModelScope.launch {
            eventRepository.emitEvent(ScreenDisplayed.PostDetail(postId = postId))
        }
    }

    /**
     * `true` until the first [onResume]: the screen's initial ON_RESUME fires while the
     * CommentsList ViewModel is doing its own initial fetch, so that first resume IS in lockstep
     * with the list. Every later resume (back from a pushed screen, app foregrounded) is
     * parent-only — the list's onResume() only restarts autoFetchLatest, which swallows failures.
     */
    private var isFirstResume = true

    fun onResume() {
        val errorSurfacedByList = isFirstResume
        isFirstResume = false
        viewModelScope.launch { fetchPost(incrementViewCount = true, errorSurfacedByList = errorSurfacedByList) }
    }

    fun onPause() {
    }

    fun onBack() {
        if (!forcedBack && _state.value.hasUnsavedChanges) {
            _state.update { it.copy(showUnsavedChangesDialog = true) }
        } else {
            navigateUp()
        }
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            // Lockstep with CommentsList: PostDetailsScreen's pull-to-refresh calls both this and
            // CommentsList's own viewModel.refresh() (CommentsList.kt), which already surfaces
            // this same failure inline or via its own snackbar.
            fetchPost(errorSurfacedByList = true)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun updateDraftComment(comment: Comment.Draft) {
        _state.update { it.copy(draftComment = comment) }
    }

    fun dismissUnsavedChangesDialog(navigateUp: Boolean) {
        _state.update { it.copy(showUnsavedChangesDialog = false) }
        if (navigateUp) {
            navigateUp()
        }
    }

    private suspend fun fetchCounters() = postRepository.fetchCounters(itemId = postId)

    /**
     * @param errorSurfacedByList `true` only from the call sites where the comment list
     * (`OctopusItemsListViewModel`) fetches in lockstep and already surfaces the same failure —
     * [refresh] and the first [onResume] (screen entry). Later resumes and the `ProfileModified`
     * handler are parent-only triggers the list does not mirror, so they keep the default `false`.
     */
    private suspend fun fetchPost(
        incrementViewCount: Boolean = false,
        errorSurfacedByList: Boolean = false,
    ) {
        val result =
            postRepository.fetchItem(
                itemId = postId,
                hasVideo = _state.value.post?.hasVideo,
                incrementViewCount = incrementViewCount,
            )
        if (result !is OctopusResult.Success || result.data == null) {
            _state.update { it.copy(isLoading = false) }
        }
        if (!hasFetched) {
            trackerRepository.trackPostOpened(
                success = result.isSuccess,
                origin =
                    when (origin) {
                        OctopusDestination.Origin.SDK ->
                            TrackerEvent.PostOpened.Origin.Sdk(
                                hasFeaturedComment = hasFeaturedComment,
                            )

                        OctopusDestination.Origin.CLIENT_APP -> TrackerEvent.PostOpened.Origin.ClientApp
                    },
            )
        }
        when (result) {
            is OctopusResult.Success -> {
                // The fetch upserts the item, but local reads can still hide a blocked author.
                val isLocallyVisible = result.data != null && postRepository.getLocalItemFlow(postId).first() != null
                _state.update {
                    it.copy(
                        isLoading = isLocallyVisible && it.isLoading,
                        hasLoadFailure = false,
                        loadFailureIsNoNetwork = false,
                        isContentUnavailable = !isLocallyVisible || it.isContentUnavailable,
                        isLockedOwnContent = isLocallyVisible && it.isLockedOwnContent,
                        post = if (isLocallyVisible) it.post else null,
                    )
                }
            }

            // Mirrors iOS `postNotAvailable = true` empty state branch in d655011: when
            // the BE confirms the post is gone (deleted / moderated), surface the same
            // in-screen empty state used for the locked-other case rather than an alert.
            is OctopusResult.Failure.ContentUnavailable ->
                _state.update {
                    it.copy(
                        post = null,
                        isContentUnavailable = true,
                        isLockedOwnContent = false,
                        hasLoadFailure = false,
                        loadFailureIsNoNetwork = false,
                    )
                }

            is OctopusResult.Failure -> handleLoadFailure(result, errorSurfacedByList)
        }
        hasFetched = true
    }

    /**
     * Routes a load failure exactly like `OctopusItemsListViewModel.handleFetchFailure` does:
     * inline with nothing on screen, snackbar with Retry over a post already displayed, and never
     * the modal alert — that one is reserved for actions the user explicitly triggered, which is
     * why this does not go through `showError` (that path escalates an authentication failure
     * carrying a server reason to a dialog, on what may be a background refresh).
     *
     * Before either channel, one suppression. The comment list underneath fetches in lockstep on
     * pull-to-refresh and on screen entry (CommentsList.kt's onRefresh calls both this and its own
     * `viewModel.refresh()`), and routes the same failure to its own inline state or snackbar. When
     * it does, saying it here too would say it twice — but only when the list actually has a
     * surface to say it on, which is what [UiState.hasCommentListSurface] answers. Deliberately
     * inverted vs iOS d655011, which keeps the parent toast and instead hides the child's inline
     * state once comments are non-empty.
     */
    private fun handleLoadFailure(
        result: OctopusResult.Failure<*>,
        errorSurfacedByList: Boolean,
    ) {
        if (errorSurfacedByList && _state.value.hasCommentListSurface) return

        val isNoNetwork = ScreenStateFailure.from(result) == ScreenStateFailure.NoNetwork
        // One error, one channel (see loadFailureChannel): the post is the visible content here,
        // exactly as the item list is on the list paths.
        val channel = loadFailureChannel(hasVisibleContent = _state.value.post != null)

        _state.update {
            it.copy(
                hasLoadFailure = channel == ErrorChannel.Inline,
                loadFailureIsNoNetwork = isNoNetwork,
            )
        }

        if (channel == ErrorChannel.Transient) {
            showErrorSnackbar(
                message = result.errorText,
                isNoNetwork = isNoNetwork,
            ) { refresh() }
        }
    }
}
