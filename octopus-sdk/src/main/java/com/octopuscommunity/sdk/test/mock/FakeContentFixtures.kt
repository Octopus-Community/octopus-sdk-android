package com.octopuscommunity.sdk.test.mock

import com.octopuscommunity.sdk.domain.model.Medias
import com.octopuscommunity.sdk.domain.model.OctopusItem.Feeds
import com.octopuscommunity.sdk.domain.model.TranslatableText
import java.util.Date

/** Stable repository seeds; the public mock builders can still produce arbitrary preview data. */
internal object FakeContentFixtures {
    private val date = Date(1_750_000_000_000L)
    private val authors = MockMinimalProfiles.all.map { it.copy(tags = emptyList(), gamificationLevelIndex = null) }

    val comments =
        MockComments.all.mapIndexed { index, comment ->
            comment.copy(
                id = "fake_comment_$index",
                parentId = "fake_post_3",
                creationDate = date,
                updateDate = date,
                author = authors[(index + 2) % authors.size],
                medias = if (index == 0) MockComments.default.medias else null,
                repliesFeeds = Feeds(descendingId = "fake_replies_$index", ascendingId = "fake_replies_${index}_asc"),
                replyCount = if (index == 0) MockReplies.all.size else 0,
                reactions = emptyList(),
                userReaction = null,
                post = null,
            )
        }

    val replies =
        MockReplies.all.mapIndexed { index, reply ->
            reply.copy(
                id = "fake_reply_$index",
                parentId = "fake_comment_0",
                creationDate = date,
                updateDate = date,
                author = authors[(index + 2) % authors.size],
                medias = null,
                reactions = emptyList(),
                userReaction = null,
                comment = null,
            )
        }

    val posts =
        MockPosts.all.mapIndexed { index, post ->
            val poll =
                when (index) {
                    1 -> MockPostPolls.default
                    5 -> MockPostPolls.yesNoPoll
                    else -> null
                }
            val group = MockTopics.all[index % MockTopics.all.size]
            post.copy(
                id = "fake_post_$index",
                parentId = group.id,
                group = group,
                creationDate = date,
                updateDate = date,
                author = authors[index % authors.size],
                text = if (index == 0) TranslatableText(originalText = longText, originalLanguage = "en") else post.text,
                medias =
                    when (index) {
                        0 -> MockPosts.default.medias
                        4 -> Medias(video = MockMedias.defaultVideo)
                        MockPosts.all.indexOf(MockPosts.withThreeImages) -> MockPosts.withThreeImages.medias
                        MockPosts.all.indexOf(MockPosts.withEightImages) -> MockPosts.withEightImages.medias
                        else -> null
                    },
                poll = poll,
                pollVoteId = if (index == 5) poll?.answers?.first()?.id else null,
                commentsFeeds = Feeds(descendingId = "fake_comments_$index", ascendingId = "fake_comments_${index}_asc"),
                viewCount = 10,
                commentCount = if (index == 3) comments.size else 0,
                reactions = emptyList(),
                userReaction = null,
                featuredComment = if (index == 3) comments.first() else null,
                clientObject = post.clientObject?.copy(id = "fake_client_object_$index"),
            )
        }

    private val longText: String
        get() =
            List(12) { index ->
                "Chapter ${index + 1}: Members share ideas, compare their experiences and plan the next community activity. " +
                    "Take time to read each suggestion and add your own perspective to the conversation."
            }.joinToString("\n\n")
}
