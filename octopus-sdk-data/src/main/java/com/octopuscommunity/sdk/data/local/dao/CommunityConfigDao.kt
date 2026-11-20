package com.octopuscommunity.sdk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.octopuscommunity.sdk.data.local.entity.CommunityConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityConfigDao {

    @Query("SELECT * FROM community_configs")
    @Transaction
    suspend fun getCommunityConfig(): CommunityConfigEntity?

    @Query("SELECT * FROM community_configs")
    @Transaction
    fun getCommunityConfigFlow(): Flow<CommunityConfigEntity?>

    @Upsert
    suspend fun upsertCommunityConfig(config: CommunityConfigEntity)
}