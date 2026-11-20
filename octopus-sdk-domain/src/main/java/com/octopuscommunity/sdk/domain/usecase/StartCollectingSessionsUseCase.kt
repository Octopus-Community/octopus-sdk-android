package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.event.LifecycleEvents
import com.octopuscommunity.sdk.domain.event.LifecycleEvents.Event.AppAlive
import com.octopuscommunity.sdk.domain.event.LifecycleEvents.Event.AppCreated
import com.octopuscommunity.sdk.domain.event.LifecycleEvents.Event.AppStarted
import com.octopuscommunity.sdk.domain.event.LifecycleEvents.Event.AppStopped
import com.octopuscommunity.sdk.domain.event.LifecycleEvents.Event.OctopusStarted
import com.octopuscommunity.sdk.domain.event.LifecycleEvents.Event.OctopusStopped
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Drives the app and Octopus sessions from the app lifecycle events.
 *
 * Long-lived collector: it first stops any session left open by a previous run, at its last-alive
 * time, then maps each [LifecycleEvents.Event] to the matching session use case until [invoke]'s
 * flow completes or the caller is cancelled.
 *
 * As on iOS, each session recovers at its own last heartbeat, preserving raw bounds even when
 * the clock moved backwards; serialized durations are floored to zero. Recovery does not apply
 * a parent app-session end bound. A recovered Octopus interval can therefore extend outside its
 * app interval under rollback (shared iOS limitation).
 */
class StartCollectingSessionsUseCase(
    private val sessionRepository: SessionRepository,
    private val createAppSessionUseCase: CreateAppSessionUseCase,
    private val startAppSessionUseCase: StartAppSessionUseCase,
    private val stopAppSessionUseCase: StopAppSessionUseCase,
    private val startOctopusSessionUseCase: StartOctopusSessionUseCase,
    private val stopOctopusSessionUseCase: StopOctopusSessionUseCase,
) {
    /** Collects [events] and starts or stops the sessions accordingly. */
    suspend operator fun invoke(events: Flow<LifecycleEvents.Event>) {
        stopStartedSessions()

        events.collect { event ->
            when (event) {
                is AppCreated -> createAppSessionUseCase()
                is AppStarted -> startAppSessionUseCase()
                is AppStopped -> stopAppSessionUseCase()
                is OctopusStarted -> startOctopusSessionUseCase()
                is OctopusStopped -> stopOctopusSessionUseCase()
                is AppAlive -> sessionRepository.updateSessionsLastAlive()
            }
        }
    }

    private suspend fun stopStartedSessions() {
        // Stop not stopped sessions
        sessionRepository.getAppSession()?.takeIf { !it.isStopped }?.let { appSession ->
            stopAppSessionUseCase(endTime = appSession.lastAliveTime)
        }
        sessionRepository.getOctopusSession()?.takeIf { !it.isStopped }?.let { octopusSession ->
            stopOctopusSessionUseCase(endTime = octopusSession.lastAliveTime)
        }
    }
}
