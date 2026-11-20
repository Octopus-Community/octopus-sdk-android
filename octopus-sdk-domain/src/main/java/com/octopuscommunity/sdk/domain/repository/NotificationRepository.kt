package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.Notification
import com.octopuscommunity.sdk.domain.model.Settings
import com.octopuscommunity.sdk.domain.network.OctopusResult
import kotlinx.coroutines.flow.Flow

/** Repository for the in-app notifications of the current user and their settings. */
interface NotificationRepository {
    /** Observes the cached notification settings. */
    fun getNotificationsSettingsFlow(): Flow<Settings.Notifications>

    /** Fetches the notification settings from the backend and updates the cache. */
    suspend fun fetchNotificationsSettings(): OctopusResult<Settings.Notifications, Nothing>

    /** Saves [settings] on the backend. */
    suspend fun setNotificationsSettings(settings: Settings.Notifications): OctopusResult<Settings.Notifications, Nothing>

    /** Observes the cached notifications. */
    fun getNotificationsFlow(): Flow<List<Notification>>

    /**
     * Fetches the notifications of the connected user and replaces the cached ones. [markdownText]
     * asks the backend for Markdown-formatted texts.
     */
    suspend fun fetchNotifications(markdownText: Boolean = true): OctopusResult<List<Notification>, Nothing>

    /** Deletes the cached notifications. */
    suspend fun deleteNotifications()

    /** Marks [notification] as read, or as unread when [isRead] is `false`. */
    suspend fun markAsRead(
        notification: Notification,
        isRead: Boolean = true,
    ): OctopusResult<Unit, Nothing> =
        markAsRead(
            listOf(notification),
            isRead = isRead,
        )

    /** Marks [notifications] as read, or as unread when [isRead] is `false`. */
    suspend fun markAsRead(
        notifications: List<Notification>,
        isRead: Boolean = true,
    ): OctopusResult<Unit, Nothing>
}
