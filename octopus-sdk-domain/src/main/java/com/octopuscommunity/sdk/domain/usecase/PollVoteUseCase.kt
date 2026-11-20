package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.OctopusEvent.GamificationPointsGained
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PollVote
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.network.mapSuccess
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.PostRepository
import com.octopuscommunity.sdk.domain.repository.TranslationRepository

/**
 * Votes for an answer of a post's poll.
 *
 * On success, returns the updated post paired with the gamification toast for voting (or `null`),
 * and emits [PollVote], then [GamificationPointsGained] when a toast applies.
 */
class PollVoteUseCase(
    val postRepository: PostRepository,
    val translationRepository: TranslationRepository,
    val configRepository: CommunityConfigRepository,
    val eventRepository: EventRepository,
) {
    /** Votes for [answer] in the poll of [post]. */
    suspend operator fun invoke(
        post: Post,
        answer: Post.Poll.Answer,
    ) = postRepository
        .pollVote(
            post = post,
            answer = answer,
            isPostTranslated = translationRepository.isTranslated(post),
        ).mapSuccess { post ->
            post to configRepository.getGamificationToast(Gamification.Action.VOTE)
        }.onSuccess { (post, gamificationToast) ->
            eventRepository.emitEvent(
                PollVote(
                    contentId = post.id,
                    optionId = answer.id,
                ),
            )
            gamificationToast?.let {
                eventRepository.emitEvent(GamificationPointsGained(it))
            }
        }
}
