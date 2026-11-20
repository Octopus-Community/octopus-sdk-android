package com.octopuscommunity.sdk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.octopuscommunity.sdk.data.local.entity.UserConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserConfigDao {

    @Query("SELECT * FROM user_configs")
    @Transaction
    suspend fun getUserConfig(): UserConfigEntity?

    @Query("SELECT * FROM user_configs")
    @Transaction
    fun getUserConfigFlow(): Flow<UserConfigEntity?>

    @Upsert
    suspend fun upsertUserConfig(config: UserConfigEntity)
}