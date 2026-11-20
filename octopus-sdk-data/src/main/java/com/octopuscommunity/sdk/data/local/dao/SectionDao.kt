package com.octopuscommunity.sdk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.octopuscommunity.sdk.data.local.entity.SectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections")
    fun getAllSections(): Flow<List<SectionEntity>>

    @Upsert
    suspend fun upsertSections(sections: List<SectionEntity>)

    @Query("DELETE FROM sections")
    suspend fun deleteAllSections()

    @Transaction
    suspend fun replaceAllSections(sections: List<SectionEntity>) {
        deleteAllSections()
        upsertSections(sections)
    }
}