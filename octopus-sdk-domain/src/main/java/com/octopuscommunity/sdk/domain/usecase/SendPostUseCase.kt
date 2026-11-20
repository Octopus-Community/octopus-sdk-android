package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.CreationSource
import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.Gamification.Action
import com.octopuscommunity.sdk.domain.model.OctopusEvent.GamificationPointsGained
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostCreated
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.network.mapSuccess
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.PostRepository

/**
 * Publishes a post.
 *
 * On success, emits [PostCreated], then [GamificationPointsGained] when the community config awards
 * points for posting.
 */
class SendPostUseCase(
    val postRepository: PostRepository,
    val configRepository: CommunityConfigRepository,
    val eventRepository: EventRepository,
) {
    /** Outcome of a successful [SendPostUseCase] call. */
    data class Result(
        val post: Post,
        val gamificationToast: Gamification.Toast?,
    )

    /** Publishes [draftItem]. [creationSource] tells whether the user or the host app created it. */
    suspend operator fun invoke(
        draftItem: Post.Draft,
        creationSource: CreationSource = CreationSource.USER,
    ) = postRepository
        .sendPost(
            draftItem = draftItem,
            creationSource = creationSource,
        ).mapSuccess { post ->
            Result(
                post = post,
                gamificationToast = configRepository.getGamificationToast(Action.POST),
            )
        }.onSuccess { (post, gamificationToast) ->
            eventRepository.emitEvent(
                PostCreated(
                    postId = post.id,
                    content = draftItem.eventContent(),
                    groupId = post.parentId,
                    textLength = post.textLength,
                ),
            )
            gamificationToast?.let {
                eventRepository.emitEvent(GamificationPointsGained(it))
            }
        }
}

/**
 * What the submitted draft actually carried, as the [PostCreated.Content] set of the event.
 *
 * The content set is derived from the draft rather than from the post the backend echoes back on
 * publish: media processing is asynchronous server-side, so the echo may or may not carry the
 * media aggregate yet, and an echo-derived set under-reports attachments non-deterministically
 * (internal change). Every other field of the event stays server-derived — the id and the group only
 * exist once the post is published.
 *
 * A post draft can only carry images (`Post.Draft.images` is a list of `Media.Image`), hence the single
 * [PostCreated.Content.IMAGE] mapping, whatever the number of images.
 */
private fun Post.Draft.eventContent(): Set<PostCreated.Content> =
    buildSet {
        if (text.isNotBlank()) add(PostCreated.Content.TEXT)
        if (images.isNotEmpty()) add(PostCreated.Content.IMAGE)
        if (poll != null) add(PostCreated.Content.POLL)
    }
