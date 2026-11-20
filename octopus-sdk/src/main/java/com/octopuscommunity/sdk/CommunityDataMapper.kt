package com.octopuscommunity.sdk

import com.octopuscommunity.sdk.data.repository.ProfileRepositoryImpl
import com.octopuscommunity.sdk.domain.model.OctopusCommunityData
import com.octopuscommunity.sdk.domain.model.OctopusGamification
import com.octopuscommunity.sdk.domain.model.Profile
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.mapSuccess
import com.octopuscommunity.sdk.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Maps a domain [Profile] (either [com.octopuscommunity.sdk.domain.model.OtherUserProfile] from
 * [com.octopuscommunity.sdk.domain.repository.ProfileRepository.getProfileFlow] or the
 * [Profile] returned by [com.octopuscommunity.sdk.domain.repository.ProfileRepository.fetchProfile])
 * onto the public read-only [OctopusCommunityData].
 *
 * Gamification is `null` when the user carries no gamification level — which is the case when
 * gamification is disabled for the community (the backend then sends no gamification score, leaving
 * [Profile.gamificationLevelIndex] null) — and for a profile tagged admin, which the backend sends
 * without one too.
 */
internal fun Profile.toOctopusCommunityData(): OctopusCommunityData =
    OctopusCommunityData(
        userId = userId,
        messageCount = messageCount,
        gamification =
            gamificationLevelIndex
                ?.takeUnless { Profile.Tag.ADMIN in tags }
                ?.let { level ->
                    OctopusGamification(level = level, score = gamificationScore)
                },
    )

// The data implementation maps Room entities directly. Fakes and custom repositories retain the
// domain mapping, without adding a transport property to the public profile model.
internal fun ProfileRepository.communityDataFlow(userId: String): Flow<OctopusCommunityData?> =
    if (this is ProfileRepositoryImpl) {
        getCommunityDataFlow(userId)
    } else {
        getProfileFlow(userId).map { it?.toOctopusCommunityData() }
    }

internal suspend fun ProfileRepository.fetchCommunityData(userId: String): OctopusResult<OctopusCommunityData?, Nothing> =
    if (this is ProfileRepositoryImpl) {
        fetchCommunityData(userId)
    } else {
        fetchProfile(userId).mapSuccess { it?.toOctopusCommunityData() }
    }

internal suspend fun ProfileRepository.fetchCommunityDataByClientUserId(
    clientUserId: String,
): OctopusResult<OctopusCommunityData?, Nothing> =
    if (this is ProfileRepositoryImpl) {
        fetchCommunityDataByClientUserId(clientUserId)
    } else {
        fetchProfileByClientUserId(clientUserId).mapSuccess { it?.toOctopusCommunityData() }
    }
