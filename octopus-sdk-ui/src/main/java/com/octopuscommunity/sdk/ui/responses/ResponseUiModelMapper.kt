package com.octopuscommunity.sdk.ui.responses

import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.Status

/**
 * Maps a [Comment] domain model to a [ResponseUiModel] for rendering by `ResponseCard`.
 *
 * Equivalent to iOS `ResponseViewData.init(from: DisplayableFeedResponse)` /
 * `init(from: CommentDetailViewModel.CommentDetail)` — on Android a single mapper works for both
 * because the domain [Comment] is used in feed and detail contexts alike.
 */
internal fun Comment.toUiModel(currentUserProfile: CurrentUserProfile?): ResponseUiModel =
    toUiModel(
        kind = ResponseKind.Comment,
        replyCount = replyCount,
        canCreateChildren = canCreateChildren,
        currentUserProfile = currentUserProfile,
    )

/** Maps a [Comment.Reply] domain model to a [ResponseUiModel]. */
internal fun Comment.Reply.toUiModel(currentUserProfile: CurrentUserProfile?): ResponseUiModel =
    toUiModel(
        kind = ResponseKind.Reply,
        replyCount = 0,
        canCreateChildren = canCreateChildren,
        currentUserProfile = currentUserProfile,
    )

private fun OctopusItem.toUiModel(
    kind: ResponseKind,
    replyCount: Int,
    canCreateChildren: Boolean,
    currentUserProfile: CurrentUserProfile?,
): ResponseUiModel = ResponseUiModel(
    id = id,
    kind = kind,
    author = author,
    creationDate = creationDate,
    content = when (val status = status) {
        is Status.Moderated -> ResponseContent.Moderated(reasons = status.reasons)
        is Status.Published, Status.Unknown -> ResponseContent.Published(
            text = text,
            image = image,
        )
    },
    reactions = reactions,
    userReaction = userReaction,
    replyCount = replyCount,
    canBeDeleted = canBeDeleted(currentUserProfile),
    canBeReported = canBeReported(currentUserProfile),
    canCreateChildren = canCreateChildren,
    hasTranslation = text.hasTranslation,
)
