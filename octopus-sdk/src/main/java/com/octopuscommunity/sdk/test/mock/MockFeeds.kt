package com.octopuscommunity.sdk.test.mock

import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.RootFeed
import java.util.UUID

/** Mock feeds of posts, comments and replies. */
object MockFeeds {
    object Post {
        val default = create()
        val defaultRoot = createRoot(label = "For you")

        val all =
            listOf(
                default,
                create(),
                create(),
                create(),
            )
        val allRoots =
            listOf(
                defaultRoot,
                createRoot(label = "Sport"),
                createRoot(label = "Finance"),
                createRoot(label = "Art & Culture"),
            )

        fun create(id: String = "feed_posts_${UUID.randomUUID()}") =
            OctopusItem.Feeds(
                descendingId = "${id}_desc",
                ascendingId = "${id}_asc",
            )

        fun createRoot(
            id: String = "feed_posts_${UUID.randomUUID()}",
            label: String = "Sample Feed ${('A'..'Z').random()}${('A'..'Z').random()}",
            relatedTopicId: String? = null,
        ) = RootFeed(id, label, relatedTopicId)
    }

    object Comment {
        val default = create()

        val all =
            listOf(
                default,
                create(),
                create(),
                create(),
            )

        fun create(id: String = "feed_comments_${UUID.randomUUID()}") =
            OctopusItem.Feeds(
                descendingId = "${id}_desc",
                ascendingId = "${id}_asc",
            )
    }

    object Replies {
        val default = create()

        val all =
            listOf(
                default,
                create(),
                create(),
                create(),
            )

        fun create(id: String = "feed_replies_${UUID.randomUUID()}") =
            OctopusItem.Feeds(
                descendingId = "${id}_desc",
                ascendingId = "${id}_asc",
            )
    }
}
