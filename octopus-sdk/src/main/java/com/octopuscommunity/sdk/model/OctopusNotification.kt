package com.octopuscommunity.sdk.model

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.octopuscommunity.sdk.OctopusSDK
import kotlin.reflect.KClass

/**
 * Represents a push notification from the Octopus platform.
 *
 * This data class encapsulates all information related to a notification, including
 * content to display and deep link destination data. It supports notifications for posts,
 * comments, and replies within the Octopus community.
 *
 * @property title The title text to display in the notification
 * @property body The main content text to display in the notification
 * @property linkPath The relative path to navigate to when the notification is tapped
 * @property postId Optional ID of the associated post, if this notification relates to a post
 * @property commentId Optional ID of the associated comment, if this notification relates to a comment
 * @property replyId Optional ID of the associated reply, if this notification relates to a reply
 * @property deepLink The complete URI for deep linking, combining the base path with the link path
 */
data class OctopusNotification(
    val id: Int = System.currentTimeMillis().toInt(),
    val title: String,
    val body: String,
    val linkPath: String,
    val postId: String? = null,
    val commentId: String? = null,
    val replyId: String? = null,
) {
    /**
     * Companion object containing constant keys used for notification data mapping.
     */
    companion object {
        /** Key to identify if a notification originated from Octopus */
        const val IS_OCTOPUS_NOTIFICATION_KEY = "is_octopus_notification"

        /** Key for the notification title */
        const val TITLE_KEY = "title"

        /** Key for the notification body text */
        const val BODY_KEY = "body"

        /** Key for the notification deep link path */
        const val LINK_PATH_KEY = "link_path"

        /** Key for the associated post ID */
        const val POST_ID_KEY = "post_id"

        /** Key for the associated comment ID */
        const val COMMENT_ID_KEY = "comment_id"

        /** Key for the associated reply ID */
        const val REPLY_ID_KEY = "reply_id"
    }

    /**
     * The complete URI for deep linking into the app.
     *
     * Created by combining the [OctopusSDK.INTERNAL_DEEP_LINK_BASE_PATH] from OctopusSDK with the linkPath.
     */
    val deepLink = "${OctopusSDK.INTERNAL_DEEP_LINK_BASE_PATH}$linkPath".toUri()

    /**
     * Constructor that creates an OctopusNotification from a 'RemoteMessage.data' map.
     * Used when parsing notification data from Firebase Cloud Messaging.
     *
     * @param map The key-value map containing notification data
     */
    constructor(map: Map<String, String>) : this(
        title = map[TITLE_KEY] ?: "",
        body = map[BODY_KEY] ?: "",
        linkPath = map[LINK_PATH_KEY] ?: "",
        postId = map[POST_ID_KEY],
        commentId = map[COMMENT_ID_KEY],
        replyId = map[REPLY_ID_KEY],
    )
}

/**
 * Extension property to check if a notification data map is from Octopus.
 *
 * @return True if the notification sender is Octopus, false otherwise.
 */
val Map<String, String>.isOctopusNotification: Boolean
    get() = get(OctopusNotification.IS_OCTOPUS_NOTIFICATION_KEY)?.toBoolean() == true

/**
 * Extension function that extracts an OctopusNotification from a notification data map.
 *
 * @return An OctopusNotification object if the map contains a valid Octopus notification, null otherwise.
 */
fun Map<String, String>.getOctopusNotification(): OctopusNotification? = if (isOctopusNotification) OctopusNotification(this) else null

/**
 * Sets the title, text and tap intent of [octopusNotification]. The tap opens its deep link in
 * [activityClass].
 */
fun NotificationCompat.Builder.setOctopusContent(
    context: Context,
    activityClass: KClass<*>,
    octopusNotification: OctopusNotification,
) = setContentTitle(octopusNotification.title)
    .setContentText(octopusNotification.body)
    .setContentIntent(
        // Create a proper back stack when deep linking
        PendingIntent.getActivity(
            context,
            0,
            Intent(
                Intent.ACTION_VIEW,
                octopusNotification.deepLink,
                context,
                activityClass.java,
            ),
            PendingIntent.FLAG_IMMUTABLE,
        ),
    )

/**
 * Sets the title, text and tap intent of [octopusNotification]. The tap opens its deep link in
 * [activityClass].
 */
fun Notification.Builder.setOctopusContent(
    context: Context,
    activityClass: KClass<*>,
    octopusNotification: OctopusNotification,
) = setContentTitle(octopusNotification.title)
    .setContentText(octopusNotification.body)
    .setContentIntent(
        // Create a proper back stack when deep linking
        PendingIntent.getActivity(
            context,
            0,
            Intent(
                Intent.ACTION_VIEW,
                octopusNotification.deepLink,
                context,
                activityClass.java,
            ),
            PendingIntent.FLAG_IMMUTABLE,
        ),
    )
