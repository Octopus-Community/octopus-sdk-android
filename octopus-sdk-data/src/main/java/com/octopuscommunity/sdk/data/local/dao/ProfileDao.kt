package com.octopuscommunity.sdk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.octopuscommunity.sdk.data.local.entity.CurrentUserProfileEntity
import com.octopuscommunity.sdk.data.local.entity.PublicProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM current_user_profiles")
    @Transaction
    suspend fun getCurrentUserProfile(): CurrentUserProfileEntity?

    @Query("SELECT * FROM current_user_profiles")
    @Transaction
    fun getCurrentUserProfileFlow(): Flow<CurrentUserProfileEntity?>

    @Upsert
    suspend fun upsertCurrentUserProfile(profile: CurrentUserProfileEntity)

    @Update(CurrentUserProfileEntity::class)
    suspend fun updateCurrentUserProfileNotifications(notifications: CurrentUserProfileEntity.NotSeenNotificationsCountUpdate)

    @Query("DELETE FROM current_user_profiles")
    suspend fun deleteCurrentUserProfile()

    @Query("SELECT * FROM public_profiles")
    @Transaction
    fun getPublicProfile(): Flow<PublicProfileEntity?>

    @Query("SELECT * FROM public_profiles WHERE userId = :userId")
    @Transaction
    fun getPublicProfile(userId: String): Flow<PublicProfileEntity?>

    @Upsert
    suspend fun upsertPublicProfile(profile: PublicProfileEntity)

    @Query("DELETE FROM public_profiles WHERE userId = :userId")
    suspend fun deletePublicProfile(userId: String)

    /** Updates an already gamified public profile; a private score alone cannot enable gamification. */
    @Query(
        "UPDATE public_profiles SET gamificationLevelIndex = :gamificationLevelIndex, " +
            "gamificationScore = :gamificationScore WHERE userId = :userId AND gamificationLevelIndex IS NOT NULL",
    )
    suspend fun updatePublicProfileGamification(
        userId: String,
        gamificationLevelIndex: Int?,
        gamificationScore: Int?,
    )
}
