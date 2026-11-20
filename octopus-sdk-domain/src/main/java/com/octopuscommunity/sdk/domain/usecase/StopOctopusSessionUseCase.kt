package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.OctopusEvent
import com.octopuscommunity.sdk.domain.model.SessionEvent
import com.octopuscommunity.sdk.domain.model.Sessions.Session
import com.octopuscommunity.sdk.domain.model.TrackerEvent
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import com.octopuscommunity.sdk.domain.repository.TrackerRepository

/**
 * Stops the Octopus session when the community UI is closed.
 *
 * Idempotent: an already stopped session is left as is. Otherwise emits [SessionEvent.OctopusStopped] and
 * [OctopusEvent.SessionStopped], and tracks [TrackerEvent.LeavingOctopus] only when the session
 * belongs to the current run of its app session. For that tracked session, as on iOS, a wall-clock
 * rollback closes it with its raw start/end bounds; serialization floors its duration to zero.
 * No parent end bound is applied, including during recovery, so rollback can leave a visit outside its app session's
 * interval (shared iOS limitation).
 * Pre-upgrade records with reused app ids can be misattributed once, matching iOS rollback handling.
 */
class StopOctopusSessionUseCase(
    val sessionRepository: SessionRepository,
    val eventRepository: EventRepository,
    val trackerRepository: TrackerRepository,
) {
    /** Outcome of [StopOctopusSessionUseCase]. */
    data class Result(
        /** The stored Octopus session after the call, stopped or not. */
        val session: Session.Octopus?,
    )

    /** Stops the Octopus session at [endTime]. */
    suspend operator fun invoke(endTime: Long = System.currentTimeMillis()) =
        Result(
            sessionRepository.updateOctopusSession { appSession, octopusSession ->
                when {
                    // Stopping is idempotent: an already stopped session is returned untouched
                    // (returning null here would erase it). Re-stopping would push stopTime forward
                    // and emit a second, longer LeavingOctopus for one session, inflating the
                    // reported community time (internal ticket). OctopusScreenSessionGate is what keeps the
                    // known caller -- a screen disposed while the app is already in the background --
                    // from asking twice; this guard makes the operation safe for any caller, so a new
                    // call site cannot reintroduce the inflation.
                    octopusSession == null || octopusSession.isStopped -> octopusSession

                    // A missing or replaced parent cannot own this visit's analytics summary.
                    // Close its record and notify the host, without tracking LeavingOctopus.
                    !octopusSession.isInside(appSession) ->
                        octopusSession
                            .copy(stopTime = endTime.coerceAtLeast(octopusSession.startTime))
                            .also {
                                eventRepository.emitEvent(SessionEvent.OctopusStopped(it))
                                eventRepository.emitEvent(
                                    OctopusEvent.SessionStopped(sessionId = it.id),
                                )
                            }

                    else ->
                        octopusSession.copy(stopTime = endTime).also {
                            eventRepository.emitEvent(SessionEvent.OctopusStopped(it))
                            eventRepository.emitEvent(OctopusEvent.SessionStopped(sessionId = it.id))
                            trackerRepository.trackEvent(TrackerEvent.LeavingOctopus(it))
                        }
                }
            },
        )
}

/**
 * Whether this Octopus session names [appSession] as its parent, regardless of wall-clock bounds.
 * A rollback can put the visit's start before the parent's start within the same app run.
 *
 * Deliberately says nothing about whether [appSession] is still running: an Octopus session inside
 * an app session that has already stopped is contained, and must still report its `LeavingOctopus`.
 * `SessionsHeartbeat.keptAliveAt` applies the same id check and additionally requires a live app
 * session; the two use different layers' session types.
 */
private fun Session.Octopus.isInside(appSession: Session.App?): Boolean = appSession != null && appSessionId == appSession.id
