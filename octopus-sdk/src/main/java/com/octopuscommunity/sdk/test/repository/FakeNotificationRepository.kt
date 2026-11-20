package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.model.Notification
import com.octopuscommunity.sdk.domain.model.Settings
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.NotificationRepository
import com.octopuscommunity.sdk.test.mock.MockNotifications
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [NotificationRepository] serving [MockNotifications]. */
open class FakeNotificationRepository : NotificationRepository {
    private val _notificationsSettings =
        MutableStateFlow(Settings.Notifications(pushEnabled = true))
    private val _notifications = MutableStateFlow(MockNotifications.all)

    override fun getNotificationsSettingsFlow(): Flow<Settings.Notifications> = _notificationsSettings.asStateFlow()

    override suspend fun fetchNotificationsSettings(): OctopusResult<Settings.Notifications, Nothing> =
        OctopusResult.Success(_notificationsSettings.value)

    override suspend fun setNotificationsSettings(settings: Settings.Notifications): OctopusResult<Settings.Notifications, Nothing> {
        _notificationsSettings.update { settings }
        return OctopusResult.Success(settings)
    }

    override fun getNotificationsFlow(): Flow<List<Notification>> = _notifications.asStateFlow()

    override suspend fun fetchNotifications(markdownText: Boolean): OctopusResult<List<Notification>, Nothing> =
        OctopusResult.Success(_notifications.value)

    override suspend fun deleteNotifications() {
    }

    override suspend fun markAsRead(
        notifications: List<Notification>,
        isRead: Boolean,
    ): OctopusResult<Unit, Nothing> {
        _notifications.update { currentNotifications ->
            currentNotifications.map { notification ->
                if (notification in notifications) {
                    notification.copy(isRead = isRead)
                } else {
                    notification
                }
            }
        }
        return OctopusResult.Success(Unit)
    }

    /** Removes every notification. */
    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    /** Appends [notification]. */
    fun addNotification(notification: Notification) {
        _notifications.update { currentList -> currentList + notification }
    }

    /** Replaces the notifications with [notifications]. */
    fun setNotifications(notifications: List<Notification>) {
        _notifications.value = notifications
    }

    /** Returns the number of unread notifications. */
    fun getUnreadCount(): Int = _notifications.value.count { !it.isRead }
}
