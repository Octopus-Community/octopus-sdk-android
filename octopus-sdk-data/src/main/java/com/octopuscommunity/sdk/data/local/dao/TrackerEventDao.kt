package com.octopuscommunity.sdk.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.octopuscommunity.sdk.data.local.entity.AppSessionEndReceiptEntity
import com.octopuscommunity.sdk.data.local.entity.TrackerEventEntity

@Dao
interface TrackerEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAppSessionEndReceipt(receipt: AppSessionEndReceiptEntity): Long

    /** The receipt and queue row commit together, including when replaying after a crash. */
    @Transaction
    suspend fun enqueueAppSessionEnd(event: TrackerEventEntity) {
        val receipt = AppSessionEndReceiptEntity(requireNotNull(event.appSessionId))
        if (insertAppSessionEndReceipt(receipt) != -1L) upsertEvent(event)
    }

    @Query("SELECT * FROM tracker_events")
    suspend fun getAllEvents(): List<TrackerEventEntity>

    @Upsert
    suspend fun upsertEvent(event: TrackerEventEntity)

    @Upsert
    suspend fun upsertEvents(events: List<TrackerEventEntity>)

    @Delete
    suspend fun deleteEvents(events: List<TrackerEventEntity>)

    @Query("DELETE FROM tracker_events")
    suspend fun deleteAllEvents()
}
