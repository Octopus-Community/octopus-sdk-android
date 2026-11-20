package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.Gamification.Action
import com.octopuscommunity.sdk.domain.model.OctopusEvent.CommentCreated
import com.octopuscommunity.sdk.domain.model.OctopusEvent.GamificationPointsGained
import com.octopuscommunity.sdk.domain.network.mapSuccess
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.CommentRepository
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.TranslationRepository

/**
 * Publishes a comment on a post.
 *
 * On success, emits [CommentCreated], then [GamificationPointsGained] when the community config
 * awards points for commenting.
 */
class SendCommentUseCase(
    val commentRepository: CommentRepository,
    val configRepository: CommunityConfigRepository,
    val translationRepository: TranslationRepository,
    val eventRepository: EventRepository,
) {
    /** Outcome of a successful [SendCommentUseCase] call. */
    data class Result(
        val comment: Comment,
        val gamificationToast: Gamification.Toast?,
    )

    /** Publishes [draftItem]. The parent post's current translation state is sent along with it. */
    suspend operator fun invoke(draftItem: Comment.Draft) =
        commentRepository
            .sendComment(
                draftComment = draftItem,
                isParentTranslated = translationRepository.isTranslated(draftItem.parentId),
            ).mapSuccess { comment ->
                Result(
                    comment = comment,
                    gamificationToast = configRepository.getGamificationToast(Action.COMMENT),
                )
            }.onSuccess { (comment, gamificationToast) ->
                eventRepository.emitEvent(CommentCreated(comment))
                gamificationToast?.let {
                    eventRepository.emitEvent(GamificationPointsGained(it))
                }
            }
}
