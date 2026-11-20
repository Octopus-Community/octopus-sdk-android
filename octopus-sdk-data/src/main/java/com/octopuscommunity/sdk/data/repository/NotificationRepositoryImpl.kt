package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.sdk.data.local.dao.NotificationDao
import com.octopuscommunity.sdk.data.local.dao.updateNotificationsReadStatusChunked
import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.data.local.datastore.SecuredDataStore
import com.octopuscommunity.sdk.data.local.datastore.SettingsDataStore
import com.octopuscommunity.sdk.data.mapper.NotificationMapper
import com.octopuscommunity.sdk.data.mapper.SettingsMapper
import com.octopuscommunity.sdk.data.remote.service.NotificationService
import com.octopuscommunity.sdk.data.utils.call
import com.octopuscommunity.sdk.domain.model.Notification
import com.octopuscommunity.sdk.domain.model.Settings
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

open class NotificationRepositoryImpl(
    protected val securedDataStore: SecuredDataStore,
    protected val preferencesDataStore: PreferencesDataStore,
    private val settingsDataStore: SettingsDataStore,
    private val notificationDao: NotificationDao,
    private val notificationService: NotificationService,
    private val settingsMapper: SettingsMapper,
    private val notificationMapper: NotificationMapper
) : NotificationRepository {

    override fun getNotificationsSettingsFlow(): Flow<Settings.Notifications> =
        with(settingsMapper) {
            settingsDataStore.notificationsSettings.map { it.toDomain() }
        }

    override suspend fun fetchNotificationsSettings(): OctopusResult<Settings.Notifications, Nothing> =
        call(settingsMapper) {
            notificationService.getNotificationSettings().settings
                .toData()
                .also { settingsDataStore.updateNotificationsSettings(it) }
                .toDomain()
        }

    override suspend fun setNotificationsSettings(
        settings: Settings.Notifications
    ): OctopusResult<Settings.Notifications, Nothing> = call(settingsMapper) {
        notificationService.setNotificationSettings(settings.toProto()).settings
            .toData()
            .also { settingsDataStore.updateNotificationsSettings(it) }
            .toDomain()
    }

    override fun getNotificationsFlow(): Flow<List<Notification>> = with(notificationMapper) {
        notificationDao.getAllNotifications().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun fetchNotifications(markdownText: Boolean): OctopusResult<List<Notification>, Nothing> =
        call(notificationMapper) {
            val userId = securedDataStore.getUser()?.id ?: return@call emptyList()
            notificationService.getUserNotifications(userId, markdownText).notificationsList
                .map { it.toEntity() }
                .also { notificationDao.replaceAllNotifications(it) }
                .map { it.toDomain() }
        }

    override suspend fun deleteNotifications() {
        notificationDao.deleteAllNotifications()
    }

    override suspend fun markAsRead(
        notifications: List<Notification>,
        isRead: Boolean
    ): OctopusResult<Unit, Nothing> = call {
        notificationService.markNotificationsAsRead(notifications.map { it.id }, isRead).also {
            notificationDao.updateNotificationsReadStatusChunked(
                ids = it.notificationIdsList,
                isRead = isRead
            )
        }
    }
}