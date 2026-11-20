package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.OctopusEvent
import com.octopuscommunity.sdk.domain.model.OctopusEvent.GamificationPointsGained
import com.octopuscommunity.sdk.domain.model.SessionEvent
import com.octopuscommunity.sdk.domain.model.Sessions.Session
import com.octopuscommunity.sdk.domain.model.TrackerEvent
import com.octopuscommunity.sdk.domain.network.OctopusResult.Failure
import com.octopuscommunity.sdk.domain.network.mapSuccess
import com.octopuscommunity.sdk.domain.network.onFailure
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.GroupRepository
import com.octopuscommunity.sdk.domain.repository.PreferenceRepository
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import com.octopuscommunity.sdk.domain.repository.TrackerRepository
import com.octopuscommunity.sdk.domain.repository.UserRepository

/**
 * Starts an Octopus session when the community UI is opened.
 *
 * Does nothing and returns `null` when no live app session exists. Otherwise emits
 * [OctopusEvent.SessionStarted], checks the connection and notifies the backend. On success it
 * refreshes the groups, emits [SessionEvent.OctopusStarted] and the gamification toasts, and tracks
 * [TrackerEvent.EnteringOctopus]. On a [Failure.PermissionDenied], it refetches community access.
 */
class StartOctopusSessionUseCase(
    val sessionRepository: SessionRepository,
    val configRepository: CommunityConfigRepository,
    val preferenceRepository: PreferenceRepository,
    val connectionRepository: ConnectionRepository,
    val groupRepository: GroupRepository,
    val userRepository: UserRepository,
    val eventRepository: EventRepository,
    val trackerRepository: TrackerRepository,
) {
    /** Outcome of a successful [StartOctopusSessionUseCase] call. */
    data class Result(
        /** The started Octopus session. */
        val session: Session.Octopus,
        /** Whether to show the gamification sheet: from the third session on, until it has been displayed. */
        val showGamificationSheet: Boolean,
        /** Toasts for the points the backend reported on session start. */
        val gamificationToasts: List<Gamification.Toast>,
    )

    /**
     * Starts a new Octopus session anchored to the live app session, and returns it — or `null`
     * when no session should be started.
     *
     * An Octopus session only exists inside an app session, so starting one without a live app
     * session would anchor it to an empty `appSessionId` and let it run unattached, fabricating
     * community time and producing rows with no app session to be bound to (internal ticket).
     *
     * The decision runs inside the store transform, which is handed both sessions from one
     * snapshot, so it cannot be made on a stale read. The transform returns the stored session
     * unchanged on a refusal — never `null`, which would erase it, and it carries the `startCount`
     * the gamification sheet depends on. This function itself returns `null` on a refusal, which
     * is what stops the caller from emitting a start event for a session it did not open.
     */
    private suspend fun startNewSession(): Session.Octopus? {
        var started: Session.Octopus? = null
        sessionRepository.updateOctopusSession { appSession, octopusSession ->
            val liveAppSession = appSession?.takeIf { !it.isStopped }
            if (liveAppSession == null) {
                octopusSession
            } else {
                Session
                    .Octopus(
                        isFirstSession = octopusSession == null,
                        startCount = (octopusSession?.startCount ?: 0) + 1,
                        appSessionId = liveAppSession.id,
                    ).also { started = it }
            }
        }
        return started
    }

    /** Starts the session, or returns `null` when no live app session exists. */
    suspend operator fun invoke() =
        startNewSession()?.let { session ->
            eventRepository.emitEvent(OctopusEvent.SessionStarted(sessionId = session.id))
            connectionRepository
                .checkConnection()
                .mapSuccess {
                    sessionRepository
                        .startOctopusSession()
                        .mapSuccess { (gamificationActions) ->
                            Result(
                                session = session,
                                showGamificationSheet =
                                    !preferenceRepository.hasDisplayedGamificationSheet &&
                                        session.startCount >= 3,
                                gamificationToasts =
                                    gamificationActions.mapNotNull {
                                        configRepository.getGamificationToast(it)
                                    },
                            )
                        }.onSuccess { (session, showGamificationSheet, gamificationToasts) ->
                            groupRepository.fetchGroups()
                            eventRepository.emitEvent(
                                SessionEvent.OctopusStarted(
                                    session = session,
                                    showGamificationSheet = showGamificationSheet,
                                    gamificationToasts = gamificationToasts,
                                ),
                            )
                            gamificationToasts.forEach {
                                eventRepository.emitEvent(GamificationPointsGained(it))
                            }
                            trackerRepository.trackEvent(TrackerEvent.EnteringOctopus(session))
                        }
                }.onFailure { failure ->
                    when (failure) {
                        is Failure.PermissionDenied -> userRepository.fetchCommunityAccess()
                        else -> {}
                    }
                }
        }
}
