package com.octopuscommunity.sdk.domain.model

import java.util.Date

/** An in-app notification of the current user. */
data class Notification(
    val id: String,
    val updateDate: Date,
    /** Users whose activity triggered the notification. */
    val authors: List<Author>,
    val text: String,
    /** What opening the notification shows, or `null` when it opens nothing. */
    val action: Action?,
    /** Whether the user read the notification. */
    val isRead: Boolean,
) {
    /** Item a notification opens. */
    sealed interface Action {
        /** Opens the post [itemId]. */
        data class Post(
            override val itemId: String,
        ) : Action

        /** Opens the comment [itemId]. */
        data class Comment(
            override val itemId: String,
        ) : Action

        /** Opens the reply [itemId] to the comment [parentId]. */
        data class Reply(
            override val itemId: String,
            val parentId: String,
        ) : Action

        /** Id of the item to open. */
        val itemId: String
    }
}
