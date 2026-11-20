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
 * Updates the current user's profile through [ProfileRepository.updateProfile].
 *
 * On success, emits [ProfileModified], and [GamificationPointsGained] when the update earned
 * gamification points.
 */
class UpdateProfileUseCase(
    val profileRepository: ProfileRepository,
    val configRepository: CommunityConfigRepository,
    val eventRepository: EventRepository,
) {
    /** Outcome of a successful [UpdateProfileUseCase] call. */
    data class Result(
        val profileUpdate: ProfileUpdateResult,
        val gamificationToast: Gamification.Toast?,
    )

    /**
     * Updates the profile from [oldProfile] to [newProfile]. With [findAvailableNickname], the backend
     * picks a free nickname when the requested one is missing or taken.
     */
    suspend operator fun invoke(
        newProfile: CurrentUserProfile,
        oldProfile: CurrentUserProfile?,
        findAvailableNickname: Boolean = false,
    ) = invoke(newProfile, oldProfile, findAvailableNickname, wantNicknameSuggestions = false)

    /**
     * Same as the update above; with [wantNicknameSuggestions], a taken nickname comes back with
     * available suggestions. Only the nickname choice screen asks for them.
     */
    suspend operator fun invoke(
        newProfile: CurrentUserProfile,
        oldProfile: CurrentUserProfile?,
        findAvailableNickname: Boolean,
        wantNicknameSuggestions: Boolean,
    ) = profileRepository
        .updateProfile(
            newProfile = newProfile,
            previousProfile = oldProfile,
            findAvailableNickname = findAvailableNickname,
            wantNicknameSuggestions = wantNicknameSuggestions,
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
