package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.Gamification.Action
import com.octopuscommunity.sdk.domain.model.OctopusEvent.GamificationPointsGained
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReplyCreated
import com.octopuscommunity.sdk.domain.network.mapSuccess
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.ReplyRepository
import com.octopuscommunity.sdk.domain.repository.TranslationRepository

/**
 * Publishes a reply to a comment.
 *
 * On success, emits [ReplyCreated], then [GamificationPointsGained] when the community config awards
 * points for replying.
 */
class SendReplyUseCase(
    val replyRepository: ReplyRepository,
    val configRepository: CommunityConfigRepository,
    val translationRepository: TranslationRepository,
    val eventRepository: EventRepository,
) {
    /** Outcome of a successful [SendReplyUseCase] call. */
    data class Result(
        val reply: Comment.Reply,
        val gamificationToast: Gamification.Toast?,
    )

    /** Publishes [draftItem]. The parent comment's current translation state is sent along with it. */
    suspend operator fun invoke(draftItem: Comment.Reply.Draft) =
        replyRepository
            .sendReply(
                draftItem = draftItem,
                isParentTranslated = translationRepository.isTranslated(draftItem.parentId),
            ).mapSuccess { reply ->
                Result(
                    reply = reply,
                    gamificationToast = configRepository.getGamificationToast(Action.REPLY),
                )
            }.onSuccess { (reply, gamificationToast) ->
                eventRepository.emitEvent(ReplyCreated(reply))
                gamificationToast?.let {
                    eventRepository.emitEvent(GamificationPointsGained(it))
                }
            }
}
