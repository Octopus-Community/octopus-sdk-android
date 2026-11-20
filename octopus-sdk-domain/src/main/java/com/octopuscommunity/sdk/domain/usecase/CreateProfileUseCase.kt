package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.OctopusEvent.GamificationPointsGained
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ProfileModified
import com.octopuscommunity.sdk.domain.network.mapSuccess
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.ProfileRepository
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateResult

/**
 * Creates the current user's profile through [ProfileRepository.createProfile].
 *
 * On success, emits [ProfileModified], and [GamificationPointsGained] when the creation earned
 * gamification points.
 */
class CreateProfileUseCase(
    val profileRepository: ProfileRepository,
    val configRepository: CommunityConfigRepository,
    val eventRepository: EventRepository,
) {
    /** Outcome of a successful [CreateProfileUseCase] call. */
    data class Result(
        val profileUpdate: ProfileUpdateResult,
        val gamificationToast: Gamification.Toast?,
    )

    /** Creates [profile] on the backend. */
    suspend operator fun invoke(profile: CurrentUserProfile) =
        profileRepository
            .createProfile(
                profile = profile,
            ).mapSuccess { profileUpdate ->
                Result(
                    profileUpdate = profileUpdate,
                    gamificationToast =
                        profileUpdate.gamificationAction?.let {
                            configRepository.getGamificationToast(it)
                        },
                )
            }.onSuccess { (profileUpdate, gamificationToast) ->
                eventRepository.emitEvent(
                    ProfileModified(
                        previousProfile = profileUpdate.previousProfile,
                        newProfile = profileUpdate.newProfile,
                    ),
                )
                gamificationToast?.let {
                    eventRepository.emitEvent(GamificationPointsGained(it))
                }
            }
}
