package com.octopuscommunity.sdk.ui.posts

import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.model.Status

/**
 * Maps a [Post] domain model to a [PostUiModel] ready to be rendered by the shared `PostCard`
 * composable in either feed or detail context.
 *
 * Equivalent to iOS `PostUiModel.init(from: DisplayablePost)` / `init(from: PostDetailViewModel.Post)`.
 * Android has a single domain [Post] used in both contexts, so one mapper covers both.
 *
 * @param currentUserProfile used to decide whether the post can be deleted or reported.
 */
internal fun Post.toUiModel(currentUserProfile: CurrentUserProfile?): PostUiModel =
    PostUiModel(
        id = id,
        author = author,
        creationDate = creationDate,
        group = group,
        tags =
            when (status) {
                is Status.Moderated -> setOf(PostTag.Moderated)
                is Status.Published, Status.Unknown -> emptySet()
            },
        content =
            when (val status = status) {
                is Status.Moderated -> PostContent.Moderated(reasons = status.reasons)
                is Status.Published, Status.Unknown ->
                    PostContent.Published(
                        catchPhrase = clientObject?.catchPhrase,
                        text = text,
                        media = mapMedia(),
                        poll = mapPoll(),
                        cta = mapCta(),
                    )
            },
        reactions = reactions,
        userReaction = userReaction,
        commentCount = commentCount,
        viewCount = viewCount,
        canBeDeleted = canBeDeleted(currentUserProfile),
        canBeReported = canBeReported(currentUserProfile),
        canCreateChildren = canCreateChildren,
        // Poll answers count too: a post with no text but translated answers still offers the toggle,
        // which switches the answers (they render through `getText(translated)`). The call to action
        // does not count: it is always shown translated, so the toggle would change nothing.
        hasTranslation =
            text.hasTranslation ||
                poll?.answers?.any { it.text.hasTranslation } == true ||
                clientObject?.catchPhrase?.hasTranslation == true,
    )

/** Every image, in order, else the video — the backend refuses a post carrying both. */
private fun Post.mapMedia(): PostMedia? =
    medias?.images?.takeIf { it.isNotEmpty() }?.let { PostMedia.Images(it) } ?: video?.let { PostMedia.Video(it) }

/** Mapped on its own, whatever the media: a post can carry an image and a poll together. */
private fun Post.mapPoll(): PostPoll? = poll?.let { PostPoll(poll = it, currentUserVote = currentUserVote) }

private fun Post.mapCta(): PostCta? {
    val bridge = clientObject
    if (bridge != null && bridge.ctaText != null) {
        return PostCta(
            text = bridge.ctaText!!,
            action = PostCta.Action.Bridge(clientObjectId = bridge.id),
        )
    }
    val custom = customAction
    if (custom != null && custom.targetUrl != null) {
        return PostCta(
            text = custom.ctaText,
            action = PostCta.Action.OpenUrl(url = custom.targetUrl!!),
        )
    }
    return null
}
