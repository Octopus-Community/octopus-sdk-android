package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.SessionEvent
import com.octopuscommunity.sdk.domain.model.Sessions.Session
import com.octopuscommunity.sdk.domain.model.TrackerEvent
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import com.octopuscommunity.sdk.domain.repository.TrackerRepository
import kotlin.time.Duration.Companion.seconds

/**
 * Stops the app session when the host app goes to the background.
 *
 * Idempotent: an already stopped session is left as is. Otherwise tracks the played videos and
 * [TrackerEvent.LeavingApp], and emits [SessionEvent.AppStopped]. As on iOS, a wall-clock rollback
 * preserves the raw start/end bounds and closes the session; serialization floors its duration
 * to zero. It does not bound the app interval to previously reported Octopus visits, so a visit
 * can outlast its app session under rollback (shared iOS limitation).
 */
class StopAppSessionUseCase(
    val sessionRepository: SessionRepository,
    val eventRepository: EventRepository,
    val trackerRepository: TrackerRepository,
) {
    /** Outcome of [StopAppSessionUseCase]. */
    data class Result(
        /** The stored app session after the call, stopped or not. */
        val session: Session.App?,
    )

    /** Stops the app session at [endTime]. */
    suspend operator fun invoke(endTime: Long = System.currentTimeMillis()): Result {
        var stopped: Session.App? = null
        val session =
            sessionRepository.updateAppSession { appSession ->
                if (appSession == null || appSession.isStopped) {
                    appSession
                } else {
                    appSession.copy(stopTime = endTime).also { stopped = it }
                }
            }
        // Commit the stop before any tracking side effect. The storage implementation also
        // journals the end in this write, so cancellation/process death here cannot lose it.
        stopped?.let {
            trackerRepository.trackVideosPlayed(
                videos = it.playedVideos.values.filter { video -> video.watchedTime > 0.seconds },
            )
            trackerRepository.trackEvent(TrackerEvent.LeavingApp(it))
            eventRepository.emitEvent(SessionEvent.AppStopped(it))
        }
        return Result(session)
    }
}
