//
//  Copyright © 2026 Octopus Community. All rights reserved.
//
package com.octopuscommunity.sdk.ui.profile.comments

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.UserComment
import com.octopuscommunity.sdk.test.mock.MockComments
import com.octopuscommunity.sdk.test.mock.MockPosts
import com.octopuscommunity.sdk.test.mock.MockReplies
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.posts.PostCard
import com.octopuscommunity.sdk.ui.posts.PostDisplayMode
import com.octopuscommunity.sdk.ui.posts.common.openComments
import com.octopuscommunity.sdk.ui.posts.list.PostCommentViewModel
import com.octopuscommunity.sdk.ui.responses.ResponseCard
import com.octopuscommunity.sdk.ui.responses.ResponseDisplayMode
import com.octopuscommunity.sdk.ui.responses.blocks.ResponseSeeOtherReplies

/** Width of the avatar column every `ResponseCard` reserves before its bubble (32dp avatar + 12dp gap). */
private val avatarColumnWidth = 44.dp
private val cellHorizontalPadding = 16.dp

/**
 * One entry of the profile Comments tab (internal ticket): the parent post as context, then the member's
 * authored comment or reply — preceded by the parent comment, for a reply.
 *
 * - **Top-level comment:** the member's comment, highlighted, with its native "See N replies" row
 *   (`ResponseCard`'s own `showsRepliesRow`).
 * - **Reply:** the parent comment (plain) above the member's reply (highlighted), the reply indented
 *   under a `↳` arrow in a [avatarColumnWidth] gutter, followed by a "See N other replies" row
 *   (the parent's other replies, i.e. `replyCount - 1`) aligned under the reply's own bubble.
 *
 * Navigation (internal ticket): the post card body opens the post detail at its top. Its comment count
 * opens the latest comments without focusing the composer; its Comment action opens the same latest
 * comments with the composer focused, matching iOS's shared `PostSummaryView`. A card that is a *comment on
 * a post* — the member's own top-level comment, or a reply's parent-comment context bubble — opens the
 * post detail scrolled to that comment ([onOpenPost] with its id). A *reply* card opens its *parent*
 * comment's detail, scrolled to the reply ([onOpenComment]). "See N (other) replies" opens the comment
 * detail through [onSeeReplies], which also reports the tap; "Reply" opens the comment detail.
 */
@Composable
internal fun UserCommentCell(
    userComment: UserComment,
    postViewModel: ProfileCommentPostViewModel,
    commentViewModel: PostCommentViewModel,
    replyViewModel: ProfileCommentReplyViewModel,
    onOpenPost: (postId: String, commentId: String?) -> Unit,
    onOpenComment: (commentId: String, replyId: String?) -> Unit,
    onSeeReplies: (commentId: String) -> Unit,
    modifier: Modifier = Modifier,
) = Column(modifier = modifier.fillMaxWidth()) {
    // Non-null by construction: entries whose anchor post the server did not ship are dropped by the
    // repository, which is what [UserComment.post] being a non-nullable `Post` records.
    val post = userComment.post

    // `PostCard` leaves the card-surface tap to its caller, as the feed does (`PostsList`): without
    // this wrapper only the group link, the counts and the comment button would react.
    Box(modifier = Modifier.fillMaxWidth().clickable { onOpenPost(post.id, null) }) {
        PostCard(
            modifier = Modifier.fillMaxWidth(),
            viewModel = postViewModel,
            post = post,
            displayMode =
                PostDisplayMode.Summary(
                    onCardTap = { onOpenPost(post.id, null) },
                    onChildrenTap = { postViewModel.openComments(post, focusInputField = false) },
                    displayGroupName = true,
                ),
            playVideo = false,
            enableProfileClick = true,
            onAccessibilityClick = { onOpenPost(post.id, null) },
            onCommentButtonTap = { postViewModel.openComments(post, focusInputField = true) },
        )
    }

    when (val content = userComment.content) {
        is UserComment.Content.OfComment -> {
            ResponseCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = cellHorizontalPadding),
                viewModel = commentViewModel,
                comment = content.comment,
                displayMode =
                    ResponseDisplayMode.Summary(
                        // A comment opens the post detail scrolled to that comment.
                        onCardTap = { onOpenPost(post.id, content.comment.id) },
                    ),
                showsRepliesRow = true,
                containerColor = OctopusTheme.colorScheme.primaryLow,
                onReplyClick = { commentViewModel.onReplyButtonClicked(content.comment) },
                onSeeRepliesClick = { onSeeReplies(content.comment.id) },
                onAccessibilityClick = null,
            )
        }

        is UserComment.Content.OfReply -> {
            val parentComment = userComment.parentComment
            val parentCommentId = parentComment?.id ?: content.reply.parentId

            parentComment?.let {
                ResponseCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = cellHorizontalPadding),
                    viewModel = commentViewModel,
                    comment = it,
                    displayMode =
                        ResponseDisplayMode.Summary(
                            // The parent comment is itself a comment on the post: same rule as a comment.
                            onCardTap = { onOpenPost(post.id, parentCommentId) },
                        ),
                    showsRepliesRow = false,
                    onReplyClick = { commentViewModel.onReplyButtonClicked(it) },
                    onSeeRepliesClick = {},
                    onAccessibilityClick = null,
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = cellHorizontalPadding, end = cellHorizontalPadding),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(avatarColumnWidth)
                            .padding(top = 10.dp),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    ReplyArrow(modifier = Modifier.size(width = 13.dp, height = 14.dp))
                }
                ResponseCard(
                    modifier = Modifier.weight(1f),
                    viewModel = replyViewModel,
                    reply = content.reply,
                    displayMode =
                        ResponseDisplayMode.Summary(
                            onCardTap = { onOpenComment(parentCommentId, content.reply.id) },
                        ),
                    containerColor = OctopusTheme.colorScheme.primaryLow,
                    onAccessibilityClick = null,
                )
            }

            ResponseSeeOtherReplies(
                modifier =
                    Modifier.padding(
                        start = cellHorizontalPadding + avatarColumnWidth + avatarColumnWidth,
                        end = cellHorizontalPadding,
                    ),
                otherReplyCount = (parentComment?.replyCount ?: 0) - 1,
                onClick = { onSeeReplies(parentCommentId) },
            )
        }
    }
}

/**
 * A hand-drawn `↳` corner arrow in the theme's primary color, shown in the gutter to the left of a
 * reply. Drawn (rather than relying on a Unicode glyph, which isn't guaranteed to render consistently
 * across devices/fonts) — iso iOS `ReplyArrowShape`, ported to a Compose `Canvas` rather than copied
 * verbatim.
 */
@Composable
private fun ReplyArrow(modifier: Modifier = Modifier) {
    val color = OctopusTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val strokeWidth = 1.5.dp.toPx()
        val cornerRadius = 4.dp.toPx()
        val arrowHead = 3.5.dp.toPx()
        val startX = strokeWidth
        val topY = strokeWidth
        val bottomY = size.height - strokeWidth
        val tipX = size.width - strokeWidth

        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val corner =
            Path().apply {
                moveTo(startX, topY)
                lineTo(startX, bottomY - cornerRadius)
                quadraticTo(startX, bottomY, startX + cornerRadius, bottomY)
                lineTo(tipX, bottomY)
            }
        drawPath(corner, color = color, style = stroke)

        val head =
            Path().apply {
                moveTo(tipX - arrowHead, bottomY - arrowHead)
                lineTo(tipX, bottomY)
                lineTo(tipX - arrowHead, bottomY + arrowHead)
            }
        drawPath(head, color = color, style = stroke)
    }
}

@Preview(showBackground = true)
@Composable
private fun UserCommentCellCommentPreview() {
    OctopusTheme {
        val post = MockPosts.default
        val comment = MockComments.create(parentId = post.id, post = post, replyCount = 4)
        UserCommentCell(
            userComment =
                UserComment(
                    id = comment.id,
                    updateDate = comment.updateDate,
                    content = UserComment.Content.OfComment(comment),
                    post = post,
                    parentComment = null,
                ),
            postViewModel = octopusViewModel(),
            commentViewModel = octopusViewModel(),
            replyViewModel = octopusViewModel(),
            onOpenPost = { _, _ -> },
            onOpenComment = { _, _ -> },
            onSeeReplies = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserCommentCellReplyPreview() {
    OctopusTheme {
        val post = MockPosts.default
        val parentComment = MockComments.create(parentId = post.id, post = post, replyCount = 4)
        val reply = MockReplies.create(parentId = parentComment.id, comment = parentComment)
        UserCommentCell(
            userComment =
                UserComment(
                    id = reply.id,
                    updateDate = reply.updateDate,
                    content = UserComment.Content.OfReply(reply),
                    post = post,
                    parentComment = parentComment,
                ),
            postViewModel = octopusViewModel(),
            commentViewModel = octopusViewModel(),
            replyViewModel = octopusViewModel(),
            onOpenPost = { _, _ -> },
            onOpenComment = { _, _ -> },
            onSeeReplies = {},
        )
    }
}
