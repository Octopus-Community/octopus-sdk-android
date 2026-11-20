package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.SessionEvent
import com.octopuscommunity.sdk.domain.model.Sessions.Session
import com.octopuscommunity.sdk.domain.model.TrackerEvent
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import com.octopuscommunity.sdk.domain.repository.TrackerRepository
import kotlin.time.Duration.Companion.seconds

/**
 * Starts the app session when the host app comes to the foreground.
 *
 * Closes any running session, then starts a new session with a new id that keeps the
 * toggled translations, the muted state and the played videos (with their watched time reset).
 * Emits [SessionEvent.AppStarted] and tracks [TrackerEvent.EnteringApp].
 * `AppCreated` resets per-process state and resolves access; no session is stored before this point.
 */
class StartAppSessionUseCase(
    val sessionRepository: SessionRepository,
    val eventRepository: EventRepository,
    val trackerRepository: TrackerRepository,
) {
    private val stopAppSessionUseCase =
        StopAppSessionUseCase(sessionRepository, eventRepository, trackerRepository)

    /** Outcome of [StartAppSessionUseCase]. */
    data class Result(
        /** The new app session. Never `null` in practice. */
        val session: Session.App?,
    )

    suspend operator fun invoke(): Result {
        // Close before replacing the stored record, including after a wall-clock rollback.
        // Reusing its id would attribute already reported visits to the next foreground run.
        stopAppSessionUseCase()
        return Result(
            sessionRepository
                .updateAppSession { session ->
                    Session.App(
                        isFirstSession = session == null,
                        toggledTranslations = session?.toggledTranslations ?: emptyMap(),
                        playedVideos =
                            session?.playedVideos?.mapValues { (_, video) ->
                                video.copy(watchedTime = 0.seconds)
                            } ?: emptyMap(),
                        videosMuted = session?.videosMuted ?: true,
                    )
                }?.also {
                    eventRepository.emitEvent(SessionEvent.AppStarted(it))
                    trackerRepository.trackEvent(TrackerEvent.EnteringApp(it))
                },
        )
    }
}
