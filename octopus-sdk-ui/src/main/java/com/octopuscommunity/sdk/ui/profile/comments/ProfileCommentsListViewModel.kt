//
//  Copyright © 2026 Octopus Community. All rights reserved.
//
package com.octopuscommunity.sdk.ui.profile.comments

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.OctopusEvent.SeeRepliesButtonClicked
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.model.UserComment
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.CommentRepository
import com.octopuscommunity.sdk.domain.repository.PostRepository
import com.octopuscommunity.sdk.domain.repository.ReplyRepository
import com.octopuscommunity.sdk.domain.repository.UserCommentRepository
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.errorText
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the connected user's profile "Comments" tab (internal ticket): the member's authored comments &
 * replies, each carrying its parent post (and, for a reply, its parent comment) as context.
 *
 * Modeled on [com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemsListViewModel]'s cursor
 * paging, but backed by [UserCommentRepository] instead of a
 * single-type feed repository: each page entry carries up to three nested domain objects (post /
 * parent comment / authored comment or reply), so this cannot be a plain
 * `OctopusItemsListViewModel<Comment>` subclass. Exposes raw [UserComment]s (not a bespoke "ui model"
 * wrapper) because the shared `PostCard`/`ResponseCard` composables already take domain models
 * (`Post`/`Comment`/`Comment.Reply`) directly and derive their own ui models internally — wrapping
 * again here would just duplicate that mapping.
 *
 * Every authored + parent object is persisted by
 * [UserCommentRepositoryImpl][com.octopuscommunity.sdk.data.repository.UserCommentRepositoryImpl]
 * into the same `posts`/`comments`/`replies` Room tables the rest of the app reads through
 * [PostRepository]/[CommentRepository]/[ReplyRepository]. [observeLiveUpdates] exploits that: after
 * every page load it (re)subscribes to the local flow of every id currently on screen and splices
 * fresh values back into [state], so a reaction toggled from this tab (or from another screen showing
 * the same content concurrently) is reflected here without a manual per-call patch — mirrors iOS
 * `ProfileCommentsListViewModel`'s per-id Core observation.
 *
 * Reactions themselves are NOT routed through this view model: the cell renders each role (post /
 * comment / reply) with `PostCard`/`ResponseCard`'s existing VM-aware overloads, backed by small
 * shared, feed-agnostic view models ([ProfileCommentPostViewModel],
 * [com.octopuscommunity.sdk.ui.posts.list.PostCommentViewModel], [ProfileCommentReplyViewModel]) —
 * each already wires `SetReactionUseCase` through
 * [com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemViewModel.toggleReaction]. Re-plumbing
 * reaction dispatch through this view model too (as iOS's single-view-model design does) would just
 * duplicate that already-tested path — the idiomatic Android translation of "reaction routes to the
 * right repo" is reusing it, not re-implementing it.
 */
class ProfileCommentsListViewModel internal constructor(
    commonParameters: CommonParameters,
    private val userCommentRepository: UserCommentRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val replyRepository: ReplyRepository,
    private val feedId: String,
    private val pageSize: Int = 20,
    private val isOwnProfile: Boolean = true,
) : OctopusViewModel(commonParameters) {
    /**
     * @param isLoading a first page is in flight. Deliberately an explicit flag and **not** derived
     * from `comments == null`: a first page that fails leaves `comments` null forever, so a derived
     * flag stayed true, [ProfileCommentsList] kept rendering the spinner and its error branch — the
     * only thing that surfaces [error] and the retry — was unreachable.
     * @param error the failure of a *first* page, i.e. the one that leaves nothing to show. A
     * next-page failure keeps the list on screen and is surfaced by a snackbar instead, so it never
     * lands here.
     */
    @Immutable
    data class UiState(
        val comments: List<UserComment>? = null,
        val isLoading: Boolean = true,
        val hasMore: Boolean = true,
        val error: UiText? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private val _isForbidden = MutableStateFlow(false)

    // Keep a refused other-member feed hidden during retries until a successful first page confirms access.
    internal val isForbidden = _isForbidden.asStateFlow()

    private var nextPageCursor: String? = null
    private var isFetching = false
    private var isLoadingMore = false
    private var observationJob: Job? = null

    init {
        refresh()
        // The logout deleted the cached comments and their reactions: a list already shown loads
        // its first page again for the next session. Mirrors iOS `ProfileCommentsListViewModel`.
        viewModelScope.launch {
            displayedContentCleared.collect { if (_state.value.comments != null) refresh() }
        }
    }

    /**
     * (Re)loads the first page. Called once from [init] and again by the error state's Retry button
     * — the tab's only recovery affordance, since it lives inside the profile pager and has no
     * pull-to-refresh of its own.
     */
    fun refresh() {
        viewModelScope.launch {
            if (isFetching) return@launch
            isFetching = true
            // Spinner up and the previous failure cleared for the whole fetch, so a Retry visibly
            // does something instead of leaving the error on screen until the response lands.
            _state.update { it.copy(isLoading = true, error = null) }
            loadFirstPage()
            _state.update { it.copy(isLoading = false) }
            isFetching = false
        }
    }

    fun onLoadMoreVisible() {
        val cursor = nextPageCursor ?: return
        if (isLoadingMore) return
        isLoadingMore = true
        viewModelScope.launch {
            loadNextPage(cursor)
            isLoadingMore = false
        }
    }

    /** Opens a post's detail. When [commentId] is set, the detail scrolls to that comment. */
    @JvmOverloads
    fun onOpenPost(
        postId: String,
        commentId: String? = null,
    ) {
        navigateTo(
            OctopusDestination.PostDetails(
                postId = postId,
                commentId = commentId,
                origin = OctopusDestination.Origin.SDK,
            ),
        )
    }

    /** Opens a comment with its replies from a replies row, and reports it as [SeeRepliesButtonClicked]. */
    fun onSeeReplies(commentId: String) {
        viewModelScope.launch { eventRepository.emitEvent(SeeRepliesButtonClicked(commentId = commentId)) }
        onOpenComment(commentId = commentId)
    }

    /** Opens a comment's detail. For a reply, [commentId] is its *parent* comment and [replyId] is its own id. */
    fun onOpenComment(
        commentId: String,
        replyId: String? = null,
    ) {
        navigateTo(
            OctopusDestination.CommentDetails(
                commentId = commentId,
                replyId = replyId,
                showViewPostButton = false,
            ),
        )
    }

    private suspend fun loadFirstPage() {
        if (feedId.isEmpty()) {
            _state.update { it.copy(comments = emptyList(), hasMore = false) }
            return
        }
        // A page comes back either non-empty or as the end of the feed ([UserCommentsPage]), so there
        // is no empty-page lookahead to run here — the repository absorbs it.
        when (val result = userCommentRepository.firstPage(feedId = feedId, pageSize = pageSize)) {
            is OctopusResult.Success -> {
                nextPageCursor = result.data.nextPageCursor
                val comments = result.data.comments
                _state.update {
                    it.copy(
                        comments = comments,
                        hasMore = result.data.nextPageCursor != null,
                        error = null,
                    )
                }
                _isForbidden.value = false
                observeLiveUpdates(comments)
            }

            is OctopusResult.Failure -> {
                val hideTab = shouldHideTab(result)
                if (hideTab) {
                    nextPageCursor = null
                    _isForbidden.value = true
                }
                _state.update { state ->
                    state.copy(
                        hasMore = false,
                        error = result.errorText.takeIf { state.comments.isNullOrEmpty() },
                    )
                }
                if (!hideTab) showError(result)
            }
        }
    }

    private suspend fun loadNextPage(cursor: String) {
        when (val result = userCommentRepository.nextPage(pageCursor = cursor, pageSize = pageSize)) {
            is OctopusResult.Success -> {
                nextPageCursor = result.data.nextPageCursor
                val merged = (_state.value.comments ?: emptyList()) + result.data.comments
                _state.update { it.copy(comments = merged, hasMore = result.data.nextPageCursor != null) }
                observeLiveUpdates(merged)
            }

            is OctopusResult.Failure -> {
                val hideTab = shouldHideTab(result)
                if (hideTab) {
                    nextPageCursor = null
                    _isForbidden.value = true
                    _state.update { it.copy(hasMore = false) }
                }
                // For transient failures the cursor is deliberately kept (unlike
                // [com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemsListViewModel], which drops
                // it on failure): that view model recovers through a pull-to-refresh its host screens
                // provide, whereas this list lives inside the profile pager with none — and [refresh]'s
                // Retry button belongs to the error state, which is only on screen when the *first*
                // page failed. So dropping the cursor here would end pagination permanently on a single
                // transient network error. Keeping `hasMore` true leaves the load-more trigger on
                // screen, which re-fires the next time it becomes visible.
                if (!hideTab) showError(result)
            }
        }
    }

    private fun shouldHideTab(failure: OctopusResult.Failure<*>): Boolean =
        !isOwnProfile &&
            (failure is OctopusResult.Failure.PermissionDenied || failure is OctopusResult.Failure.UserNotAuthenticated)

    /**
     * (Re)subscribes to the local DB flow of every post/comment/reply id currently on screen so a
     * reaction (from this tab or another screen showing the same content) is reflected in place.
     * Restarted on every page load since the id set grows as more pages come in.
     */
    private fun observeLiveUpdates(comments: List<UserComment>) {
        observationJob?.cancel()

        val postIds = comments.map { it.post.id }.distinct()
        val commentIds =
            buildSet {
                comments.forEach { entry ->
                    (entry.content as? UserComment.Content.OfComment)?.let { add(it.comment.id) }
                    entry.parentComment?.let { add(it.id) }
                }
            }.toList()
        val replyIds = comments.mapNotNull { (it.content as? UserComment.Content.OfReply)?.reply?.id }.distinct()

        if (postIds.isEmpty() && commentIds.isEmpty() && replyIds.isEmpty()) return

        observationJob =
            viewModelScope.launch {
                combine(
                    idFlow(postIds, postRepository::getLocalItemFlow),
                    idFlow(commentIds, commentRepository::getLocalItemFlow),
                    idFlow(replyIds, replyRepository::getLocalItemFlow),
                ) { posts, freshComments, replies -> Triple(posts, freshComments, replies) }
                    // `combine` re-emits once per upstream emission, and every one of the (up to
                    // 3xN) per-id Room flows replays its current value on subscription — so a page
                    // load alone produces a burst of identical triples. Maps and domain models are
                    // all value types, so equality collapses that burst into a single state update.
                    .distinctUntilChanged()
                    .collect { (posts, freshComments, replies) ->
                        _state.update { state ->
                            state.copy(comments = state.comments?.map { it.refreshed(posts, freshComments, replies) })
                        }
                    }
            }
    }

    private fun <T> idFlow(
        ids: List<String>,
        flowFor: (String) -> Flow<T?>,
    ): Flow<Map<String, T>> =
        if (ids.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(ids.map { id -> flowFor(id).map { id to it } }) { pairs ->
                pairs.mapNotNull { (id, item) -> item?.let { id to it } }.toMap()
            }
        }

    private fun UserComment.refreshed(
        posts: Map<String, Post>,
        comments: Map<String, Comment>,
        replies: Map<String, Comment.Reply>,
    ): UserComment {
        // `content` is a `val` of a public sealed type declared in another module
        // (octopus-sdk-domain) — Kotlin won't smart-cast a cross-module property access chain, so
        // it's captured into a local val first.
        val currentContent = content
        val refreshedContent =
            when (currentContent) {
                is UserComment.Content.OfComment ->
                    comments[currentContent.comment.id]?.let { UserComment.Content.OfComment(it) } ?: currentContent

                is UserComment.Content.OfReply ->
                    replies[currentContent.reply.id]?.let { UserComment.Content.OfReply(it) } ?: currentContent
            }
        return copy(
            post = posts[post.id] ?: post,
            parentComment = parentComment?.let { comments[it.id] ?: it },
            content = refreshedContent,
        )
    }

    override fun onCleared() {
        observationJob?.cancel()
        super.onCleared()
    }
}
