package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.Gamification.Action
import com.octopuscommunity.sdk.domain.model.OctopusEvent.GamificationPointsRemoved
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostDeleted
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.PostRepository

/**
 * Deletes a post written by the current user.
 *
 * On success, emits [PostDeleted], then [GamificationPointsRemoved] when the community config awards
 * points for posting.
 */
class DeletePostUseCase(
    val postRepository: PostRepository,
    val configRepository: CommunityConfigRepository,
    val eventRepository: EventRepository,
) {
    /** Deletes [post]. */
    suspend operator fun invoke(post: Post) =
        postRepository
            .deletePost(
                post = post,
            ).onSuccess {
                eventRepository.emitEvent(PostDeleted(post))
                configRepository
                    .getGamificationPoints(Action.POST)
                    ?.value
                    ?.takeIf { it > 0 }
                    ?.let { points ->
                        eventRepository.emitEvent(
                            GamificationPointsRemoved(
                                points = points,
                                action = Action.POST,
                            ),
                        )
                    }
            }
}
