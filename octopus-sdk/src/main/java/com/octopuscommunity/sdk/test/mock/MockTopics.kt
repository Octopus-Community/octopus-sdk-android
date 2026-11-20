package com.octopuscommunity.sdk.test.mock

import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.Status
import java.util.Date
import java.util.UUID
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit

/** Mock groups and group sections. */
object MockTopics {
    val default =
        create(
            id = "topic_technology",
            name = "Technology",
            description =
                List(10) {
                    "Explore technology and science together. Share projects, discuss discoveries and learn from other members' experiences. "
                }.joinToString("\n\n"),
            creationDate = Date(1_750_000_000_000L),
            isFollowed = true,
            canChangeFollowStatus = false,
        )

    val sections =
        listOf(
            OctopusGroup.Section(id = "section_tech", label = "Tech & Science"),
            OctopusGroup.Section(id = "section_life", label = "Lifestyle"),
        )

    val all =
        listOf(
            default,
            create(
                id = "topic_sport",
                creationDate = Date(1_750_000_000_000L),
                name = "Sport",
                isFollowed = true,
                sections = listOf(sections[1]),
            ),
            create(id = "topic_finance", creationDate = Date(1_750_000_000_000L), name = "Finance", sections = listOf(sections[0])),
            create(id = "topic_art", creationDate = Date(1_750_000_000_000L), name = "Art & Culture", sections = listOf(sections[1])),
        )

    fun create(
        id: String = "topic_${UUID.randomUUID()}",
        parentId: String = "octopus_sample",
        creationDate: Date =
            Date(
                System.currentTimeMillis() -
                    Random.nextLong(
                        0,
                        30.days.toLong(DurationUnit.MILLISECONDS),
                    ),
            ),
        updateDate: Date = creationDate,
        status: Status = Status.Published,
        name: String = "Sample Topic ${('A'..'Z').random()}${('A'..'Z').random()}",
        description: String = "This is a sample topic description. It can be customized as needed.",
        isFollowed: Boolean = false,
        canChangeFollowStatus: Boolean = true,
        sections: List<OctopusGroup.Section> = emptyList(),
        postsFeeds: OctopusItem.Feeds = OctopusItem.Feeds(descendingId = "mock_topic_${id}_desc"),
    ) = OctopusGroup(
        id,
        parentId,
        creationDate,
        localLanguage = "en-us",
        updateDate,
        status,
        name,
        description,
        isFollowed = isFollowed,
        canChangeFollowStatus = canChangeFollowStatus,
        sections = sections,
        postsFeeds = postsFeeds,
    )
}
