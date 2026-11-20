package com.octopuscommunity.sdk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.octopuscommunity.sdk.data.local.entity.TopicEntity
import com.octopuscommunity.sdk.data.local.entity.TopicEntity.FollowStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics")
    fun getAllTopics(): Flow<List<TopicEntity>>

    @Upsert
    suspend fun upsertTopics(topics: List<TopicEntity>)

    @Query("DELETE FROM topics")
    suspend fun deleteAllTopics()

    @Transaction
    suspend fun replaceAllTopics(topics: List<TopicEntity>) {
        deleteAllTopics()
        upsertTopics(topics)
    }

    @Query("UPDATE topics SET updatedAt = 0")
    suspend fun resetUpdatedAt()

    @Query("UPDATE topics SET follow_status = :followStatus WHERE id = :topicId")
    suspend fun updateFollowStatus(topicId: String, followStatus: FollowStatus)
}