package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.sdk.data.local.data.keptAliveAt
import com.octopuscommunity.sdk.data.local.datastore.SessionsDataStore
import com.octopuscommunity.sdk.data.mapper.SessionMapper
import com.octopuscommunity.sdk.data.remote.service.UserService
import com.octopuscommunity.sdk.data.utils.call
import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.Sessions
import com.octopuscommunity.sdk.domain.model.Sessions.Session
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.CommunityClosureRepository
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import kotlinx.coroutines.flow.map

class SessionRepositoryImpl(
    private val sessionsDataStore: SessionsDataStore,
    private val userService: UserService,
    private val sessionMapper: SessionMapper,
    private val closureRepository: CommunityClosureRepository = OpenCommunityClosureRepository,
) : SessionRepository {
    override suspend fun getAppSession() =
        with(sessionMapper) {
            sessionsDataStore.getAppSession()?.toDomain()
        }

    override fun getAppSessionFlow() =
        with(sessionMapper) {
            sessionsDataStore.appSessionFlow.map { it?.toDomain() }
        }

    override suspend fun getOctopusSession() =
        with(sessionMapper) {
            sessionsDataStore.getOctopusSession()?.toDomain()
        }

    override fun getOctopusSessionFlow() =
        with(sessionMapper) {
            sessionsDataStore.octopusSessionFlow.map { it?.toDomain() }
        }

    override suspend fun startOctopusSession(): OctopusResult<SessionRepository.SessionStartResult, Nothing> =
        // The server is not told the Octopus UI is displayed while the community is closed.
        if (closureRepository.isClosed) {
            OctopusResult.Success(SessionRepository.SessionStartResult(gamificationActions = emptyList()))
        } else {
            enterOctopus()
        }

    private suspend fun enterOctopus() =
        call(sessionMapper) {
            userService.enteringOctopus().let { response ->
                SessionRepository.SessionStartResult(
                    gamificationActions =
                        buildList {
                            if (response.shouldDisplayGamificationLoginToast) {
                                add(Gamification.Action.DAILY_SESSION)
                            }
                            if (response.shouldDisplayGamificationAnswerToast) {
                                add(Gamification.Action.POST_COMMENTED)
                            }
                        },
                )
            }
        }

    private suspend fun updateSessions(transform: suspend (Sessions) -> Sessions) =
        with(sessionMapper) {
            sessionsDataStore
                .update { sessions ->
                    val updated = transform(sessions.toDomain()).toData()
                    val stopped =
                        updated.appSession?.takeIf {
                            it.id == sessions.appSession?.id &&
                                sessions.appSession?.stopTime == null &&
                                it.stopTime != null
                        }
                    // A session ended while the community is closed is not journaled: it would be sent at reopening.
                    val journaled = stopped.takeUnless { closureRepository.isClosed }
                    updated.copy(pendingAppSessionEnds = sessions.pendingAppSessionEnds + listOfNotNull(journaled))
                }.toDomain()
        }

    override suspend fun updateAppSession(transform: suspend (Session.App?) -> Session.App?) =
        updateSessions {
            it.copy(appSession = transform(it.appSession))
        }.appSession

    override suspend fun updateOctopusSession(transform: suspend (Session.App?, Session.Octopus?) -> Session.Octopus?) =
        updateSessions {
            it.copy(octopusSession = transform(it.appSession, it.octopusSession))
        }.octopusSession

    override suspend fun updateSessionsLastAlive() {
        sessionsDataStore.update { it.keptAliveAt(System.currentTimeMillis()) }
    }
}
