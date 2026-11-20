@file:Suppress("ktlint:standard:max-line-length")

package com.octopuscommunity.sdk.test.mock

import com.octopuscommunity.sdk.domain.model.Author
import com.octopuscommunity.sdk.domain.model.Notification
import java.util.Date
import java.util.UUID
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit

/** Mock [Notification]s. */
object MockNotifications {
    val default =
        create(
            text = "ValentinSalomon a aimé votre réponse sur : \"WE ARE SO BACK.\"",
            authors = listOf(MockMinimalProfiles.default),
            action = Notification.Action.Post(MockPosts.default.id),
            isRead = false,
        )

    val all =
        listOf(
            default,
            create(
                text = "***Giulietta*** a aimé commenté sur : \"L’été indien arrive plus tôt.\"",
                authors = listOf(MockMinimalProfiles.all.random()),
                action =
                    MockComments.all.random().let { comment ->
                        Notification.Action.Comment(itemId = comment.id)
                    },
                isRead = true,
            ),
            create(
                text = "***Giulietta et 4 autres personnes*** ont aimé votre réponse sur :  \"\uD83C\uDFF8 Est-ce que quelqu'un connait un endroit pour réserver...\"",
                authors =
                    listOf(
                        MockMinimalProfiles.all.random(),
                        MockMinimalProfiles.all.random(),
                    ),
                action =
                    MockReplies.all.random().let { reply ->
                        Notification.Action.Reply(
                            itemId = reply.id,
                            parentId = reply.comment!!.id,
                        )
                    },
                isRead = false,
            ),
            create(
                text = "Votre publication a été modérée pour non-respect des règles de la communauté.",
                authors = emptyList(),
                action = null,
                isRead = false,
            ),
        )

    fun create(
        id: String = "notification_${UUID.randomUUID()}",
        updateDate: Date =
            Date(
                System.currentTimeMillis() -
                    Random.nextLong(
                        0,
                        30.days.toLong(DurationUnit.MILLISECONDS),
                    ),
            ),
        authors: List<Author> = listOf(MockMinimalProfiles.default),
        text: String = "***ValentinSalomon*** a aimé votre réponse sur : \"WE ARE SO BACK.\"",
        action: Notification.Action? = Notification.Action.Post(MockPosts.all.random().id),
        isRead: Boolean = Random.nextBoolean(),
    ) = Notification(id, updateDate, authors, text, action, isRead)
}
