package com.octopuscommunity.sdk.ui.posts.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.OctopusEvent
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostClicked
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostCreated
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.model.Status
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.PostRepository
import com.octopuscommunity.sdk.domain.repository.TranslationRepository
import com.octopuscommunity.sdk.domain.usecase.PollVoteUseCase
import com.octopuscommunity.sdk.domain.usecase.SetReactionUseCase
import com.octopuscommunity.sdk.domain.usecase.ToggleTranslationUseCase
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.common.ErrorChannel
import com.octopuscommunity.sdk.ui.common.ScreenStateFailure
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.errorText
import com.octopuscommunity.sdk.ui.common.loadFailureChannel
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemViewModel
import com.octopuscommunity.sdk.ui.posts.common.PostViewModel
import com.octopuscommunity.sdk.ui.posts.common.openComments
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
open class PostsListViewModel internal constructor(
    commonParameters: CommonParameters,
    private val postRepository: PostRepository,
    translationRepository: TranslationRepository,
    toggleTranslationUseCase: ToggleTranslationUseCase,
    setReactionUseCase: SetReactionUseCase,
    pollVoteUseCase: PollVoteUseCase,
    protected val feedId: String,
    private val pagesSize: Int? = 20,
) : PostViewModel(
        commonParameters,
        translationRepository,
        toggleTranslationUseCase,
        setReactionUseCase,
        pollVoteUseCase,
    ) {
    @Immutable
    data class UiState(
        val posts: List<Post> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val nextPageCursor: String? = null,
        val scrollToTop: Boolean = false,
        @Deprecated(
            "Dead since the screen states landed: never read, and no longer written. " +
                "Use hasLoadFailure / loadFailureIsNoNetwork for a failed first load, and the error " +
                "snackbar for a failure over content already on screen. Removed in the next major.",
            level = DeprecationLevel.WARNING,
        )
        val error: UiText? = null,
        /**
         * Set when the first load failed with nothing on screen behind it, which is what the inline
         * screen state renders. Null while content is displayed — that failure goes to the snackbar.
         * A lost connection gets its own illustration and wording, everything else the catch-all.
         */
        val hasLoadFailure: Boolean = false,
        val loadFailureIsNoNetwork: Boolean = false,
    ) {
        val hasMorePosts = nextPageCursor != null
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var isLoadingNextPosts = false
    private val postsToTrackViews = MutableStateFlow<Set<Post>>(setOf())

    init {
        viewModelScope.launch {
            eventRepository.getEventsFlow().collect { event ->
                when (event) {
                    is OctopusEvent.ProfileModified ->
                        postRepository.fetchItems(
                            itemIds = _state.value.posts.map { it.id },
                        )

                    is PostCreated -> reload()
                    else -> {}
                }
            }
        }
        // Reload the feed when the connected user's entitlements change — typically
        // after a successful `OctopusSDK.refreshEntitlements()` call. Mirrors iOS
        // `PostListViewModel`'s reaction to the `.entitlementsChanged` internal event
        // (internal change commit ae03a89). `distinctUntilChanged` filters identical refreshes
        // (e.g. periodic profile re-fetches) and `drop(1)` skips the synthetic startup
        // emission so SDK boot does not unconditionally reload the feed.
        viewModelScope.launch {
            currentUserProfile
                .map { it?.entitlements ?: emptySet() }
                .distinctUntilChanged()
                .drop(1)
                .collect { reload() }
        }
        viewModelScope.launch { displayedContentCleared.collect { reload() } }
        viewModelScope.launch {
            postsToTrackViews
                .debounce(0.5.seconds) // Update every 0.5 seconds
                .collect { posts ->
                    if (posts.isNotEmpty()) {
                        postRepository.fetchCounters(
                            posts = posts,
                            incrementViewCount = true,
                        )
                        postsToTrackViews.update { it - posts }
                    }
                }
        }

        _state.update {
            it.copy(
                isLoading = true,
                hasLoadFailure = false,
                nextPageCursor = null,
            )
        }
        viewModelScope.launch {
            postRepository
                .getLocalItemsFlow(feedId)
                .combine(currentUserProfile) { posts, profile ->
                    // Drop posts the connected user cannot access — except their own.
                    // Own posts in non-accessible groups stay (they resolve to the
                    // `isLockedOwnContent` carve-out in PostDetail, which renders the body
                    // with comments + input hidden), so the current-user profile keeps
                    // showing them. Mirrors iOS internal change commit c59b3b7
                    // (`PostFeedViewModel.swift:93`): `lockedState != .lockedOther` filter.
                    val currentUserId = profile?.userId
                    posts.filter { post ->
                        post.canAccess ||
                            (currentUserId != null && post.author?.userId == currentUserId)
                    }
                }.collect { filteredPosts ->
                    _state.update { it.copy(posts = filteredPosts) }
                }
        }
        reload()
    }

    fun onResume() {
        viewModelScope.launch {
            postRepository.fetchCounters(
                posts = _state.value.posts,
                incrementViewCount = false,
            )
        }
    }

    fun onPause() {
    }

    fun reload(showRefreshing: Boolean = false) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isRefreshing = showRefreshing,
                    isLoading = !showRefreshing,
                    nextPageCursor = null,
                )
            }
            loadFeed(scrollToTop = true)
        }
    }

    fun loadNextPage() {
        if (isLoadingNextPosts) return
        val nextPageCursor = _state.value.nextPageCursor ?: return
        isLoadingNextPosts = true
        viewModelScope.launch {
            loadFeed(pageCursor = nextPageCursor)
            isLoadingNextPosts = false
        }
    }

    override fun onItemAppeared(item: Post) {
        postsToTrackViews.update { it + item }
    }

    override fun onItemDisappeared(item: Post) {
        postsToTrackViews.update { it - item }
    }

    private suspend fun loadFeed(
        pageCursor: String? = null,
        scrollToTop: Boolean = false,
    ) {
        val feedId = feedId.ifEmpty { null } ?: return

        when (
            val result =
                postRepository.fetchItems(
                    feedId = feedId,
                    pageSize = pagesSize,
                    pageCursor = pageCursor,
                )
        ) {
            is OctopusResult.Success -> {
                val feed = result.data
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        hasLoadFailure = false,
                        posts = feed.items.first(),
                        nextPageCursor = feed.nextPageCursor,
                        scrollToTop = scrollToTop,
                    )
                }
            }

            is OctopusResult.Failure -> {
                delay(0.2.seconds)
                // One error, one channel (see loadFailureChannel). With posts already on screen —
                // typically a cached feed whose refresh just failed — the inline slot is taken, so
                // the failure goes to the snackbar and the visible feed is left alone.
                val channel = loadFailureChannel(hasVisibleContent = _state.value.posts.isNotEmpty())
                _state.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        // Stop the pagination spinner: left set, `hasMorePosts` stays true and
                        // octopusLoadingItem() spins forever at the end of the list, re-firing the
                        // load on every small scroll and emitting a snackbar each time. Matches
                        // OctopusItemsListViewModel.handleFetchFailure.
                        nextPageCursor = null,
                        hasLoadFailure = channel == ErrorChannel.Inline,
                        loadFailureIsNoNetwork =
                            ScreenStateFailure.from(result) == ScreenStateFailure.NoNetwork,
                    )
                }
                // Snackbar, not showError: a load failure never escalates to the modal alert —
                // that one is reserved for actions the user explicitly triggered.
                if (channel == ErrorChannel.Transient) {
                    showErrorSnackbar(
                        message = result.errorText,
                        isNoNetwork = ScreenStateFailure.from(result) == ScreenStateFailure.NoNetwork,
                    ) { reload() }
                }
            }
        }
    }

    fun resetScrollToTop() {
        _state.update { it.copy(scrollToTop = false) }
    }

    fun onPostClicked(
        post: Post,
        source: PostClicked.Source,
    ) {
        viewModelScope.launch {
            eventRepository.emitEvent(PostClicked(postId = post.id, source = source))
        }
        navigateToPostDetails(post)
    }

    /**
     * Opens the post on its featured [comment] (internal ticket), rather than on its top: the detail
     * scrolls to that comment, loading only the page around it.
     */
    fun onFeaturedCommentClicked(
        post: Post,
        comment: Comment,
        source: PostClicked.Source,
    ) {
        viewModelScope.launch {
            eventRepository.emitEvent(PostClicked(postId = post.id, source = source))
        }
        navigateToPostDetails(post = post, commentId = comment.id)
    }

    fun onCommentButtonClicked(
        post: Post,
        focusInputField: Boolean,
    ) {
        openComments(post, focusInputField)
    }

    private fun navigateToPostDetails(
        post: Post,
        commentId: String? = null,
        scrollToLatestComment: Boolean = false,
        focusCommentInputField: Boolean = false,
    ) {
        if (post.status is Status.Moderated) return
        navigateTo(
            OctopusDestination.PostDetails(
                postId = post.id,
                commentId = commentId,
                scrollToLatestComment = scrollToLatestComment,
                focusCommentInputField = focusCommentInputField,
                origin = OctopusDestination.Origin.SDK,
                hasFeaturedComment = post.featuredComment != null,
            ),
        )
    }
}

class PostCommentViewModel(
    commonParameters: CommonParameters,
    translationRepository: TranslationRepository,
    toggleTranslation: ToggleTranslationUseCase,
    setReaction: SetReactionUseCase,
) : OctopusItemViewModel<Comment>(
        commonParameters,
        translationRepository,
        toggleTranslation,
        setReaction,
    ) {
    override fun onReplyButtonClicked(comment: Comment) {
        super.onReplyButtonClicked(comment)

        navigateToCommentDetails(
            comment = comment,
            scrollToLatestReply = true,
            focusReplyInputField = true,
        )
    }
}
