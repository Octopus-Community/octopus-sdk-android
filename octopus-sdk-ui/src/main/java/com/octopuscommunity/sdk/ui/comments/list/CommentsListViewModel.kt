package com.octopuscommunity.sdk.ui.comments.list

import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.OctopusEvent.CommentCreated
import com.octopuscommunity.sdk.domain.model.OctopusEvent.SeeRepliesButtonClicked
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.repository.CommentRepository
import com.octopuscommunity.sdk.domain.repository.TranslationRepository
import com.octopuscommunity.sdk.domain.usecase.SetReactionUseCase
import com.octopuscommunity.sdk.domain.usecase.ToggleTranslationUseCase
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemsListViewModel
import kotlinx.coroutines.launch

class CommentsListViewModel(
    commonParameters: CommonParameters,
    commentRepository: CommentRepository,
    translationRepository: TranslationRepository,
    toggleTranslationUseCase: ToggleTranslationUseCase,
    setReaction: SetReactionUseCase,
    val post: Post,
    scrollToLatest: Boolean = false,
    scrollToId: String? = null,
    pagesSize: Int? = 20,
) : OctopusItemsListViewModel<Comment>(
    commonParameters,
    commentRepository,
    translationRepository,
    toggleTranslationUseCase,
    setReaction,
    post.commentsFeeds,
    scrollToLatest,
    scrollToId,
    pagesSize
) {
    init {
        viewModelScope.launch {
            eventRepository.getEventsFlow().collect { event ->
                when (event) {
                    is CommentCreated -> if (event.postId == post.id) scrollToLatestItem()
                    else -> {}
                }
            }
        }
    }

    override fun onReplyButtonClicked(comment: Comment) {
        super.onReplyButtonClicked(comment)

        navigateToCommentDetails(
            comment = comment,
            scrollToLatestReply = true,
            focusReplyInputField = true
        )
    }

    fun onSeeRepliesClicked(comment: Comment) {
        viewModelScope.launch {
            eventRepository.emitEvent(SeeRepliesButtonClicked(commentId = comment.id))
        }

        navigateToCommentDetails(
            comment = comment,
            scrollToLatestReply = false,
            focusReplyInputField = false
        )
    }
}