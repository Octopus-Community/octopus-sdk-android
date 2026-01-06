package com.octopuscommunity.sample.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.octopuscommunity.sample.MainActivity
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.model.setOctopusContent

/**
 * Firebase Cloud Messaging service implementation for handling Octopus push notifications.
 *
 * This service processes incoming FCM messages, identifies Octopus notifications,
 * and displays them to the user with proper deep linking support. It also handles
 * FCM token refreshes and registers new tokens with the Octopus platform.
 *
 * The service must be registered in the AndroidManifest.xml file with the appropriate
 * intent filter to receive FCM messages.
 */
class MessagingService : FirebaseMessagingService() {
    /**
     * Called when a new FCM token is generated.
     *
     * This method is triggered when the FCM registration token is initially generated
     * or when it changes. The new token is automatically registered with the Octopus
     * platform to enable push notifications for the current device.
     *
     * @param token The new FCM registration token
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Register the new token with Octopus
        OctopusSDK.registerNotificationsToken(token)
    }

    /**
     * Called when a new FCM message is received.
     *
     * This method processes incoming messages, identifies Octopus notifications,
     * and displays them to the user. It sets up proper notification channel,
     * appearance, and deep linking behavior when the notification is tapped.
     *
     * @param remoteMessage The message received from Firebase Cloud Messaging
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Parse the notification data using the Octopus SDK helper
        val octopusNotification = OctopusSDK.getOctopusNotification(data = remoteMessage.data)
        if (octopusNotification != null) {
            // Get the notification manager service
            getSystemService<NotificationManager>()?.let { notificationManager ->
                // Create or ensure the notification channel exists (required for Android O+)
                notificationManager.createChannel(CHANNEL_ID, "Octopus Community")

                // Display the notification
                notificationManager.notify(
                    octopusNotification.id,
                    NotificationCompat.Builder(this, CHANNEL_ID)
                        .setAutoCancel(true)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setOctopusContent(
                            context = this,
                            activityClass = MainActivity::class,
                            octopusNotification = octopusNotification
                        ).build()
                )
            }
        }
    }

    /**
     * Helper extension function to create a notification channel if needed.
     *
     * This function creates a notification channel for Android O and above if
     * it doesn't already exist. The channel is required for displaying notifications
     * on modern Android versions.
     *
     * @param id The channel ID
     * @param name The human-readable channel name shown in settings
     */
    private fun NotificationManager.createChannel(id: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationChannels.none { it.id == CHANNEL_ID }) {
                createNotificationChannel(
                    NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "octopus-sdk"
    }
}