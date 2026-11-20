package com.octopuscommunity.sdk.ui.comments.replies.list

import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.Comment.Reply
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReplyCreated
import com.octopuscommunity.sdk.domain.repository.ReplyRepository
import com.octopuscommunity.sdk.domain.repository.TranslationRepository
import com.octopuscommunity.sdk.domain.usecase.SetReactionUseCase
import com.octopuscommunity.sdk.domain.usecase.ToggleTranslationUseCase
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemsListViewModel
import kotlinx.coroutines.launch

class RepliesListViewModel(
    commonParameters: CommonParameters,
    replyRepository: ReplyRepository,
    translationRepository: TranslationRepository,
    toggleTranslationUseCase: ToggleTranslationUseCase,
    setReactionUseCase: SetReactionUseCase,
    val comment: Comment,
    scrollToLatest: Boolean = false,
    scrollToId: String? = null,
    pagesSize: Int? = 20,
) : OctopusItemsListViewModel<Reply>(
        commonParameters,
        replyRepository,
        translationRepository,
        toggleTranslationUseCase,
        setReactionUseCase,
        comment.repliesFeeds,
        scrollToLatest,
        scrollToId,
        pagesSize,
        // The server serves anchors on a post's comments only, and rejects them on a comment's
        // replies: opened on a reply, the list walks the replies up to it.
        opensOnItem = false,
    ) {
    init {
        viewModelScope.launch {
            eventRepository.getEventsFlow().collect { event ->
                when (event) {
                    is ReplyCreated ->
                        if (event.commentId == comment.id) {
                            scrollToLatestItem()
                        }

                    else -> {}
                }
            }
        }
    }
}
