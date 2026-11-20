//
//  Copyright © 2026 Octopus Community. All rights reserved.
//
package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.model.UserComment
import com.octopuscommunity.sdk.domain.model.UserCommentsPage
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.UserCommentRepository
import com.octopuscommunity.sdk.test.mock.MockComments
import com.octopuscommunity.sdk.test.mock.MockPosts
import com.octopuscommunity.sdk.test.mock.MockReplies

/**
 * In-memory [UserCommentRepository] for Koin previews / screenshot tests (mirrors
 * [FakeCommentRepository]). Builds a small fixed set of authored entries — a top-level comment and a
 * reply, each with their parent post (and, for the reply, its parent comment) — so the profile
 * Comments tab has something to render without a live backend.
 */
class FakeUserCommentRepository : UserCommentRepository {
    private val allEntries: List<UserComment> = buildEntries()

    override suspend fun firstPage(
        feedId: String,
        pageSize: Int,
    ): OctopusResult<UserCommentsPage, Nothing> = page(startIndex = 0, pageSize = pageSize)

    override suspend fun nextPage(
        pageCursor: String,
        pageSize: Int,
    ): OctopusResult<UserCommentsPage, Nothing> = page(startIndex = pageCursor.toIntOrNull() ?: allEntries.size, pageSize = pageSize)

    private fun page(
        startIndex: Int,
        pageSize: Int,
    ): OctopusResult<UserCommentsPage, Nothing> {
        val slice = allEntries.drop(startIndex).take(pageSize)
        val nextIndex = startIndex + slice.size
        return OctopusResult.Success(
            UserCommentsPage(
                comments = slice,
                nextPageCursor = nextIndex.takeIf { it < allEntries.size }?.toString(),
            ),
        )
    }

    companion object {
        private fun buildEntries(): List<UserComment> {
            val entries = mutableListOf<UserComment>()
            repeat(4) { index ->
                val post = MockPosts.create(id = "user_comment_post_$index")
                val comment =
                    MockComments.create(
                        id = "user_comment_comment_$index",
                        parentId = post.id,
                        post = post,
                    )
                entries +=
                    UserComment(
                        id = comment.id,
                        updateDate = comment.updateDate,
                        content = UserComment.Content.OfComment(comment),
                        post = post,
                        parentComment = null,
                    )
                val reply =
                    MockReplies.create(
                        id = "user_comment_reply_$index",
                        parentId = comment.id,
                        comment = comment,
                    )
                entries +=
                    UserComment(
                        id = reply.id,
                        updateDate = reply.updateDate,
                        content = UserComment.Content.OfReply(reply),
                        post = post,
                        parentComment = comment,
                    )
            }
            return entries
        }
    }
}
