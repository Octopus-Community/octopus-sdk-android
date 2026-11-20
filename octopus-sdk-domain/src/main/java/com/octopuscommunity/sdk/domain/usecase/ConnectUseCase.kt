package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.ClientUser
import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.OctopusEvent.GamificationPointsGained
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ProfileModified
import com.octopuscommunity.sdk.domain.network.mapSuccess
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateResult

/**
 * Connects a client (SSO) user through [ConnectionRepository.connectUser].
 *
 * On success, emits [ProfileModified] when the connection updated the profile, and
 * [GamificationPointsGained] when that update earned gamification points.
 */
class ConnectUseCase(
    val connectionRepository: ConnectionRepository,
    val configRepository: CommunityConfigRepository,
    val eventRepository: EventRepository,
) {
    /** Outcome of a successful [ConnectUseCase] call. */
    data class Result(
        /** Connection state after the call. */
        val connectionState: ConnectionState,
        /** Profile update performed while connecting, or `null` when the profile was left unchanged. */
        val profileUpdate: ProfileUpdateResult?,
        /** Toast for the points earned by [profileUpdate], or `null` when none were earned. */
        val gamificationToast: Gamification.Toast?,
    )

    /** Connects [clientUser], using [tokenProvider] to obtain the client-signed JWT. */
    suspend operator fun invoke(
        clientUser: ClientUser,
        tokenProvider: (suspend () -> String),
    ) = connectionRepository
        .connectUser(
            clientUser = clientUser,
            tokenProvider = tokenProvider,
        ).mapSuccess { connectionResult ->
            Result(
                connectionState = connectionResult.connectionState,
                profileUpdate = connectionResult.profileUpdate,
                gamificationToast =
                    connectionResult.profileUpdate?.gamificationAction?.let {
                        configRepository.getGamificationToast(it)
                    },
            )
        }.onSuccess { (_, profileUpdate, gamificationToast) ->
            if (profileUpdate != null) {
                eventRepository.emitEvent(
                    ProfileModified(
                        previousProfile = profileUpdate.previousProfile,
                        newProfile = profileUpdate.newProfile,
                    ),
                )
            }
            gamificationToast?.let {
                eventRepository.emitEvent(GamificationPointsGained(it))
            }
        }
}
