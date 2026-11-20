package com.octopuscommunity.sdk.ui.comments.details

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.Comment.Reply
import com.octopuscommunity.sdk.domain.model.OctopusEvent.CommentDeleted
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ProfileModified
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReplyCreated
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReplyDeleted
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ScreenDisplayed
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.CommentRepository
import com.octopuscommunity.sdk.domain.repository.TranslationRepository
import com.octopuscommunity.sdk.domain.usecase.SetReactionUseCase
import com.octopuscommunity.sdk.domain.usecase.ToggleTranslationUseCase
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.common.ErrorChannel
import com.octopuscommunity.sdk.ui.common.ScreenStateFailure
import com.octopuscommunity.sdk.ui.common.errorText
import com.octopuscommunity.sdk.ui.common.loadFailureChannel
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CommentDetailsViewModel(
    commonParameters: CommonParameters,
    private val commentRepository: CommentRepository,
    translationRepository: TranslationRepository,
    toggleTranslationUseCase: ToggleTranslationUseCase,
    setReactionUseCase: SetReactionUseCase,
    private val commentId: String,
    private val replyId: String?,
) : OctopusItemViewModel<Comment>(
        commonParameters,
        translationRepository,
        toggleTranslationUseCase,
        setReactionUseCase,
    ) {
    @Immutable
    data class UiState(
        val comment: Comment? = null,
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val hasLoadFailure: Boolean = false,
        val loadFailureIsNoNetwork: Boolean = false,
        val draftReply: Reply.Draft? = null,
        val showUnsavedChangesDialog: Boolean = false,
        /**
         * `true` when the comment is unavailable (deleted server-side, moderated, or in a
         * group the connected user does not have access to and is not the author of — the
         * "locked other" case). The screen swaps in an in-screen empty state. Mirrors iOS
         * `commentNotAvailable` flag combined with `LockedContentState.lockedOther` in
         * internal change commit 95bb3b6.
         */
        val isContentUnavailable: Boolean = false,
        /**
         * GDPR carve-out: the connected user authored this comment in a group they no
         * longer have access to. The comment body stays visible to its author but the
         * replies feed and the "add a reply" input are hidden. Mirrors iOS
         * `LockedContentState.lockedOwnContent` in internal change commit 95bb3b6.
         */
        val isLockedOwnContent: Boolean = false,
    ) {
        val hasUnsavedChanges
            get() = draftReply?.hasContent == true

        /**
         * `true` when the SDK can show the "add a reply" input below the comment. `false`
         * for the GDPR carve-out (own content in a now non-accessible group), or when the
         * BE signals `canCreateChildren = false` for any other reason.
         */
        val canCreateReply get() = !isLockedOwnContent && comment?.canCreateChildren != false
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var incrementViewCount = true

    init {
        viewModelScope.launch {
            currentUserProfile.mapNotNull { it?.blockedProfiles }.collectLatest { blockedProfiles ->
                _state.value.comment?.let { comment ->
                    if (comment.author?.userId in blockedProfiles.map { it.userId }) {
                        navigateUp()
                    }
                }
            }
        }
        viewModelScope.launch {
            // Combined with the connected user rather than reading `currentUserProfile.value`: that
            // one is a `stateIn(WhileSubscribed, initialValue = null)` whose upstream starts a
            // dispatcher turn later, so a local emission arriving first resolved an own
            // non-accessible comment as *locked other* — and, read synchronously, was never
            // re-evaluated when the profile did arrive (internal change).
            combine(
                commentRepository.getLocalItemFlow(itemId = commentId),
                currentUserProfile,
            ) { comment, profile -> comment to profile }.collect { (comment, profile) ->
                // A cache miss is not a completed load. Keep the initial loader until fetch ends.
                if (comment == null) return@collect
                // Resolve the iOS LockedContentState mirror inline (95bb3b6):
                //   canAccess = true                              → unlocked
                //   canAccess = false + author == connected user  → lockedOwnContent
                //                                                   (comment stays visible,
                //                                                   replies + input hidden)
                //   canAccess = false otherwise                   → lockedOther
                //                                                   (in-screen empty state)
                val currentUserId = profile?.userId
                val isOwn = currentUserId != null && comment.author?.userId == currentUserId
                when {
                    comment.canAccess ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                comment = comment,
                                isContentUnavailable = false,
                                isLockedOwnContent = false,
                            )
                        }

                    isOwn ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                comment = comment,
                                isContentUnavailable = false,
                                isLockedOwnContent = true,
                            )
                        }

                    else ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                comment = null,
                                isContentUnavailable = true,
                                isLockedOwnContent = false,
                            )
                        }
                }
            }
        }

        viewModelScope.launch {
            eventRepository.getEventsFlow().collect { event ->
                when (event) {
                    is CommentDeleted -> if (event.commentId == commentId) navigateUp()
                    is ReplyCreated, is ReplyDeleted ->
                        if (event.commentId == commentId) {
                            fetchCounters()
                        }

                    // Parent-only trigger: OctopusItemsListViewModel's own ProfileModified handler
                    // (fetchItems(itemIds)) discards its result, so the list surfaces nothing here.
                    is ProfileModified -> fetchComment()
                    else -> {}
                }
            }
        }
        // The logout deleted the cached comment: the screen keeps showing it, and asks the server
        // whether the next session still sees it (iOS `confirmCommentDisappearance`). The reply
        // list reloads on its own.
        viewModelScope.launch { displayedContentCleared.collect { fetchComment() } }

        viewModelScope.launch {
            eventRepository.emitEvent(ScreenDisplayed.CommentDetail(commentId = commentId))
        }
    }

    /**
     * `true` until the first [onResume]: the screen's initial ON_RESUME fires while the
     * RepliesList ViewModel is doing its own initial fetch, so that first resume IS in lockstep
     * with the list. Every later resume (back from a pushed screen, app foregrounded) is
     * parent-only — the list's onResume() only restarts autoFetchLatest, which swallows failures.
     */
    private var isFirstResume = true

    fun onResume() {
        val errorSurfacedByList = isFirstResume
        isFirstResume = false
        viewModelScope.launch { fetchComment(errorSurfacedByList = errorSurfacedByList) }
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
            // Lockstep with RepliesList: CommentDetailsScreen's pull-to-refresh calls both this
            // and RepliesList's own viewModel.refresh() (RepliesList.kt), which already surfaces
            // this same failure inline or via its own snackbar.
            fetchComment(errorSurfacedByList = true)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun updateDraftReply(draftReply: Reply.Draft?) {
        _state.update { it.copy(draftReply = draftReply) }
    }

    fun dismissUnsavedChangesDialog(navigateUp: Boolean) {
        _state.update { it.copy(showUnsavedChangesDialog = false) }
        if (navigateUp) {
            navigateUp()
        }
    }

    private suspend fun fetchCounters() = commentRepository.fetchCounters(itemId = commentId)

    /**
     * @param errorSurfacedByList `true` only from the call sites where the replies list
     * (`OctopusItemsListViewModel`) fetches in lockstep and already surfaces the same failure —
     * [refresh] and the first [onResume] (screen entry). Later resumes and the `ProfileModified`
     * handler are parent-only triggers the list does not mirror, so they keep the default `false`.
     */
    private suspend fun fetchComment(errorSurfacedByList: Boolean = false) {
        val result =
            commentRepository.fetchItem(
                itemId = commentId,
                hasVideo = _state.value.comment?.hasVideo,
                incrementViewCount = incrementViewCount,
            )
        if (result !is OctopusResult.Success || result.data == null) {
            _state.update { it.copy(isLoading = false) }
        }
        when (result) {
            // Mirrors iOS `commentNotAvailable = true` empty state branch in 95bb3b6: when
            // the BE confirms the comment is gone (deleted / moderated), surface the same
            // in-screen empty state used for the locked-other case rather than an alert.
            OctopusResult.Failure.ContentUnavailable ->
                _state.update {
                    it.copy(comment = null, isContentUnavailable = true, isLockedOwnContent = false)
                }

            is OctopusResult.Failure -> {
                val state = _state.value
                val listCanSurfaceFailure =
                    state.comment != null &&
                        !state.isLockedOwnContent &&
                        state.comment.repliesFeeds.ascendingId != null
                if (!errorSurfacedByList || !listCanSurfaceFailure) {
                    val channel = loadFailureChannel(hasVisibleContent = state.comment != null)
                    val isNoNetwork = ScreenStateFailure.from(result) == ScreenStateFailure.NoNetwork
                    _state.update {
                        it.copy(hasLoadFailure = channel == ErrorChannel.Inline, loadFailureIsNoNetwork = isNoNetwork)
                    }
                    if (channel == ErrorChannel.Transient) {
                        showErrorSnackbar(message = result.errorText, isNoNetwork = isNoNetwork) { refresh() }
                    }
                }
            }

            is OctopusResult.Success -> {
                incrementViewCount = false
                // The fetch upserts the item, but local reads can still hide a blocked author.
                val isLocallyVisible = result.data != null && commentRepository.getLocalItemFlow(commentId).first() != null
                _state.update {
                    it.copy(
                        isLoading = isLocallyVisible && it.isLoading,
                        hasLoadFailure = false,
                        loadFailureIsNoNetwork = false,
                        isContentUnavailable = !isLocallyVisible || it.isContentUnavailable,
                        isLockedOwnContent = isLocallyVisible && it.isLockedOwnContent,
                        comment = if (isLocallyVisible) it.comment else null,
                    )
                }
            }
        }
    }

    fun navigateToPostDetails() {
        val postId = _state.value.comment?.parentId ?: return
        popBackStackOrNavigateTo(
            OctopusDestination.PostDetails(
                postId = postId,
                commentId = commentId,
                origin = OctopusDestination.Origin.SDK,
            ),
        )
    }
}
