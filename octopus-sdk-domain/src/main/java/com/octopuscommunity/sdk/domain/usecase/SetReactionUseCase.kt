package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.Gamification.Action
import com.octopuscommunity.sdk.domain.model.OctopusEvent.GamificationPointsGained
import com.octopuscommunity.sdk.domain.model.OctopusEvent.GamificationPointsRemoved
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReactionModified
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.network.mapSuccess
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.CommentRepository
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.PostRepository
import com.octopuscommunity.sdk.domain.repository.ReplyRepository
import com.octopuscommunity.sdk.domain.repository.TrackerRepository
import com.octopuscommunity.sdk.domain.repository.TranslationRepository

/**
 * Sets, changes or removes the current user's reaction on a [Post], [Comment] or [Comment.Reply].
 *
 * On success, emits [ReactionModified]. Adding a first reaction emits [GamificationPointsGained]
 * when reactions earn points; removing the reaction emits [GamificationPointsRemoved].
 */
class SetReactionUseCase(
    val postRepository: PostRepository,
    val commentRepository: CommentRepository,
    val replyRepository: ReplyRepository,
    val configRepository: CommunityConfigRepository,
    val translationRepository: TranslationRepository,
    val eventRepository: EventRepository,
    val trackerRepository: TrackerRepository,
) {
    /** Outcome of a successful [SetReactionUseCase] call. */
    data class Result(
        /** The reaction now set, or `null` when it was removed. */
        val reaction: OctopusItem.Reaction?,
        /** Toast for the points earned by a first reaction, or `null`. */
        val gamificationToast: Gamification.Toast?,
    )

    /**
     * Sets [reactionKind] on [item], or removes the reaction when [reactionKind] is `null`.
     *
     * Throws when [item] is not a [Post], [Comment] or [Comment.Reply].
     */
    suspend operator fun invoke(
        item: OctopusItem,
        reactionKind: OctopusItem.Reaction.Kind?,
    ) = run {
        when (item) {
            is Post ->
                postRepository.setReaction(
                    item = item,
                    reactionKind = reactionKind,
                    isContentTranslated = translationRepository.isTranslated(item),
                )

            is Comment ->
                commentRepository.setReaction(
                    item = item,
                    reactionKind = reactionKind,
                    isContentTranslated = translationRepository.isTranslated(item),
                )

            is Comment.Reply ->
                replyRepository.setReaction(
                    item = item,
                    reactionKind = reactionKind,
                    isContentTranslated = translationRepository.isTranslated(item),
                )

            else -> throw Exception()
        }.mapSuccess { reaction ->
            Result(
                reaction = reaction,
                gamificationToast =
                    if (item.userReaction == null && reactionKind != null) {
                        configRepository.getGamificationPoints(Action.REACTION)?.let { points ->
                            Gamification.Toast(
                                action = Action.REACTION,
                                points = points,
                            )
                        }
                    } else {
                        null
                    },
            )
        }.onSuccess { (reaction, gamificationToast) ->
            eventRepository.emitEvent(ReactionModified(item = item, reaction = reaction))
            if (gamificationToast != null && item.userReaction == null && reaction != null) {
                eventRepository.emitEvent(GamificationPointsGained(gamificationToast))
            } else if (item.userReaction != null && reaction == null) {
                configRepository
                    .getGamificationPoints(Action.REACTION)
                    ?.value
                    ?.takeIf { it > 0 }
                    ?.let {
                        eventRepository.emitEvent(
                            GamificationPointsRemoved(
                                points = it,
                                action = Action.REACTION,
                            ),
                        )
                    }
            }
        }
    }
}
