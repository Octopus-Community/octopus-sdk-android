package com.octopuscommunity.sdk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.octopuscommunity.sdk.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Upsert
    suspend fun upsertNotifications(notifications: List<NotificationEntity>)

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()

    @Transaction
    suspend fun replaceAllNotifications(notifications: List<NotificationEntity>) {
        deleteAllNotifications()
        upsertNotifications(notifications)
    }

    @Query("UPDATE notifications SET isRead = :isRead WHERE id IN (:ids)")
    suspend fun updateNotificationsReadStatus(ids: List<String>, isRead: Boolean): Int
}