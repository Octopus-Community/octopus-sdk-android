package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.Gamification.Action
import com.octopuscommunity.sdk.domain.model.OctopusEvent.GamificationPointsRemoved
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReplyDeleted
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.ReplyRepository

/**
 * Deletes a reply written by the current user.
 *
 * On success, emits [ReplyDeleted], then [GamificationPointsRemoved] when the community config
 * awards points for replying.
 */
class DeleteReplyUseCase(
    val replyRepository: ReplyRepository,
    val configRepository: CommunityConfigRepository,
    val eventRepository: EventRepository,
) {
    /** Deletes [reply]. */
    suspend operator fun invoke(reply: Comment.Reply) =
        replyRepository
            .deleteReply(
                reply = reply,
            ).onSuccess {
                eventRepository.emitEvent(ReplyDeleted(reply))
                configRepository
                    .getGamificationPoints(Action.REPLY)
                    ?.value
                    ?.takeIf { it > 0 }
                    ?.let { points ->
                        eventRepository.emitEvent(
                            GamificationPointsRemoved(
                                points = points,
                                action = Action.REPLY,
                            ),
                        )
                    }
            }
}
