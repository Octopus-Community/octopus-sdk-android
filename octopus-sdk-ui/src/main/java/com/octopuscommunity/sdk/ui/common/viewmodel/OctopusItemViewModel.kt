package com.octopuscommunity.sdk.ui.common.viewmodel

import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReplyButtonClicked
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.Status
import com.octopuscommunity.sdk.domain.network.onFailure
import com.octopuscommunity.sdk.domain.repository.TranslationRepository
import com.octopuscommunity.sdk.domain.usecase.SetReactionUseCase
import com.octopuscommunity.sdk.domain.usecase.ToggleTranslationUseCase
import com.octopuscommunity.sdk.ui.OctopusDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

open class OctopusItemViewModel<T : OctopusItem>(
    commonParameters: CommonParameters,
    translationRepository: TranslationRepository,
    val toggleTranslationUseCase: ToggleTranslationUseCase,
    val setReactionUseCase: SetReactionUseCase,
) : OctopusViewModel(commonParameters) {
    val translations =
        translationRepository.getTranslationsFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap(),
        )

    open fun onItemAppeared(item: T) {
    }

    open fun onItemDisappeared(item: T) {
    }

    fun toggleDisplayedTranslation(item: T) {
        viewModelScope.launch {
            toggleTranslationUseCase(item)
        }
    }

    fun toggleReaction(
        item: T,
        reactionKind: Reaction.Kind,
    ) = checkAuthenticated(UserAction.Reaction) {
        setReactionUseCase(
            item = item,
            reactionKind =
                when (reactionKind) {
                    item.userReaction?.kind -> null
                    else -> reactionKind
                },
        ).onFailure {
            showError(it)
        }
    }

    open fun onReplyButtonClicked(comment: Comment) {
        viewModelScope.launch {
            eventRepository.emitEvent(ReplyButtonClicked(commentId = comment.id))
        }
    }

    protected fun navigateToCommentDetails(
        comment: Comment,
        scrollToLatestReply: Boolean = false,
        scrollToReplyId: String? = null,
        focusReplyInputField: Boolean = false,
    ) {
        if (comment.status is Status.Moderated) return
        navigateTo(
            OctopusDestination.CommentDetails(
                commentId = comment.id,
                scrollToLatestReply = scrollToLatestReply,
                replyId = scrollToReplyId,
                showViewPostButton = false,
                focusReplyInputField = focusReplyInputField,
            ),
        )
    }
}
