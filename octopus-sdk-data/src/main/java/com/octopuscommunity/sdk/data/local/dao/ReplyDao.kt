package com.octopuscommunity.sdk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.octopuscommunity.sdk.data.local.entity.ReplyEntity
import com.octopuscommunity.sdk.data.local.entity.ReplyEntity.CountersEntity
import com.octopuscommunity.sdk.data.local.entity.ReplyWithCommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReplyDao {
    @Query("SELECT * FROM replies WHERE id IN (:ids)")
    @Transaction
    fun getRepliesByIds(ids: List<String>): Flow<List<ReplyWithCommentEntity>>

    @Query("SELECT * FROM replies WHERE id = :id")
    @Transaction
    fun getReply(id: String): Flow<ReplyWithCommentEntity?>

    @Upsert
    suspend fun upsertReply(reply: ReplyEntity)

    @Upsert
    suspend fun upsertReplies(replies: List<ReplyEntity>)

    @Update(ReplyEntity::class)
    suspend fun updateReplyCounters(counters: CountersEntity.Update)

    @Update(ReplyEntity::class)
    suspend fun updateRepliesCounters(counters: List<CountersEntity.Update>)

    @Query("DELETE FROM replies WHERE id = :id")
    suspend fun deleteReply(id: String)

    /** Deletes unreferenced replies atomically; an empty feed cache preserves all rows. */
    @Query(
        "DELETE FROM replies WHERE " +
            "EXISTS (SELECT 1 FROM feed_items) AND id NOT IN (SELECT itemId FROM feed_items)",
    )
    suspend fun deleteOrphanedReplies()

    @Query("UPDATE replies SET updatedAt = 0")
    suspend fun resetUpdatedAt()
}
