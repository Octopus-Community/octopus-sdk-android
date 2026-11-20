package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.Gamification.Action
import com.octopuscommunity.sdk.domain.model.OctopusEvent.CommentDeleted
import com.octopuscommunity.sdk.domain.model.OctopusEvent.GamificationPointsRemoved
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.CommentRepository
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.EventRepository

/**
 * Deletes a comment written by the current user.
 *
 * On success, emits [CommentDeleted], then [GamificationPointsRemoved] when the community config
 * awards points for commenting.
 */
class DeleteCommentUseCase(
    val commentRepository: CommentRepository,
    val configRepository: CommunityConfigRepository,
    val eventRepository: EventRepository,
) {
    /** Deletes [comment]. */
    suspend operator fun invoke(comment: Comment) =
        commentRepository
            .deleteComment(
                comment = comment,
            ).onSuccess {
                eventRepository.emitEvent(CommentDeleted(comment))
                configRepository
                    .getGamificationPoints(Action.COMMENT)
                    ?.value
                    ?.takeIf { it > 0 }
                    ?.let { points ->
                        eventRepository.emitEvent(
                            GamificationPointsRemoved(
                                points = points,
                                action = Action.COMMENT,
                            ),
                        )
                    }
            }
}
