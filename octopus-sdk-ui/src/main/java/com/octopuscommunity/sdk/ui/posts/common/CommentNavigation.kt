package com.octopuscommunity.sdk.ui.posts.common

import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.model.Status
import com.octopuscommunity.sdk.ui.OctopusDestination

/** Shared by feed, profile publications and the contextual post in a profile comment cell. */
internal fun PostViewModel.openComments(
    post: Post,
    focusInputField: Boolean,
) {
    onCommentClicked(post)
    if (post.status is Status.Moderated) return
    navigateTo(post.commentsDestination(focusInputField))
}

internal fun Post.commentsDestination(focusInputField: Boolean) =
    OctopusDestination.PostDetails(
        postId = id,
        scrollToLatestComment = true,
        focusCommentInputField = focusInputField,
        origin = OctopusDestination.Origin.SDK,
        hasFeaturedComment = featuredComment != null,
    )
