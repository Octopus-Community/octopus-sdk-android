package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.SDKEvent
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.repository.EventRepository

/**
 * Logs the current user out through [ConnectionRepository.logout], then emits
 * [SDKEvent.Disconnected].
 */
class DisconnectUseCase(
    val connectionRepository: ConnectionRepository,
    val eventRepository: EventRepository,
) {
    /** Logs out and emits [SDKEvent.Disconnected]. */
    suspend operator fun invoke() =
        connectionRepository.logout().also {
            eventRepository.emitEvent(SDKEvent.Disconnected)
        }
}
