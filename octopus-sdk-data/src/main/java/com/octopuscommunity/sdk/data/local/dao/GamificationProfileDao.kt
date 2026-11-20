package com.octopuscommunity.sdk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.octopuscommunity.sdk.data.local.entity.PublicProfileEntity

/** Room-only ordered writes, kept outside the public DAO's implementation contract. */
@Dao
internal abstract class GamificationProfileDao : ProfileDao {
    // Accessed only within this DAO's Room transactions; never persisted.
    private val lastWrittenGamificationTickets = mutableMapOf<String, Long>()

    @Query("SELECT * FROM public_profiles WHERE userId = :userId")
    internal abstract suspend fun getCachedPublicProfile(userId: String): PublicProfileEntity?

    /** A late public response still refreshes fields other than level and total points. */
    @Transaction
    internal open suspend fun upsertPublicProfile(
        profile: PublicProfileEntity,
        gamificationTicket: Long,
    ) {
        val cached = getCachedPublicProfile(profile.userId)
        val acceptsGamification = acceptsGamification(profile.userId, gamificationTicket)
        upsertPublicProfile(
            if (acceptsGamification || cached == null) {
                profile
            } else {
                profile.copy(
                    gamificationLevelIndex = cached.gamificationLevelIndex,
                    gamificationScore = cached.gamificationScore,
                )
            },
        )
        // Like iOS, a missing row is written even when a newer reference survived its eviction.
        // That reference is never lowered; a fresh profile's first accepted write claims its ticket.
        if (acceptsGamification) lastWrittenGamificationTickets[profile.userId] = gamificationTicket
    }

    /** Claims even unchanged private values, but never enables gamification on its own. */
    @Transaction
    internal open suspend fun updatePublicProfileGamification(
        userId: String,
        gamificationLevelIndex: Int?,
        gamificationScore: Int?,
        gamificationTicket: Long,
    ) {
        val cached = getCachedPublicProfile(userId) ?: return
        if (cached.gamificationLevelIndex == null || !acceptsGamification(userId, gamificationTicket)) return
        if (cached.gamificationLevelIndex != gamificationLevelIndex || cached.gamificationScore != gamificationScore) {
            updatePublicProfileGamification(userId, gamificationLevelIndex, gamificationScore)
        }
        lastWrittenGamificationTickets[userId] = gamificationTicket
    }

    private fun acceptsGamification(
        userId: String,
        ticket: Long,
    ): Boolean = lastWrittenGamificationTickets[userId]?.let { ticket >= it } ?: true
}

/** Room orders writes; custom non-Room implementations retain their existing write contract. */
internal suspend fun ProfileDao.upsertPublicProfile(
    profile: PublicProfileEntity,
    gamificationTicket: Long,
) {
    if (this is GamificationProfileDao) {
        upsertPublicProfile(profile, gamificationTicket)
    } else {
        upsertPublicProfile(profile)
    }
}

/** Keeps custom DAO implementations source/binary compatible with the existing interface. */
internal suspend fun ProfileDao.updatePublicProfileGamification(
    userId: String,
    gamificationLevelIndex: Int?,
    gamificationScore: Int?,
    gamificationTicket: Long,
) {
    if (this is GamificationProfileDao) {
        updatePublicProfileGamification(userId, gamificationLevelIndex, gamificationScore, gamificationTicket)
    } else {
        updatePublicProfileGamification(userId, gamificationLevelIndex, gamificationScore)
    }
}
