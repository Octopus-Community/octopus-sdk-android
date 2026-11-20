package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.model.Sessions
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [SessionRepository]; no session is stored at start. */
class FakeSessionRepository :
    FakeTrackerRepository(),
    SessionRepository {
    private val _appSession = MutableStateFlow<Sessions.Session.App?>(null)
    private val _octopusSession = MutableStateFlow<Sessions.Session.Octopus?>(null)

    override suspend fun getAppSession(): Sessions.Session.App? = _appSession.value

    override fun getAppSessionFlow(): Flow<Sessions.Session.App?> = _appSession.asStateFlow()

    override suspend fun getOctopusSession(): Sessions.Session.Octopus? = _octopusSession.value

    override fun getOctopusSessionFlow(): Flow<Sessions.Session.Octopus?> = _octopusSession.asStateFlow()

    override suspend fun updateAppSession(transform: suspend (Sessions.Session.App?) -> Sessions.Session.App?): Sessions.Session.App? {
        var result: Sessions.Session.App? = null
        _appSession.update { current ->
            val transformed = kotlinx.coroutines.runBlocking { transform(current) }
            result = transformed
            transformed
        }
        return result
    }

    override suspend fun updateOctopusSession(
        transform: suspend (Sessions.Session.App?, Sessions.Session.Octopus?) -> Sessions.Session.Octopus?,
    ): Sessions.Session.Octopus? {
        var result: Sessions.Session.Octopus? = null
        _octopusSession.update { current ->
            val transformed =
                kotlinx.coroutines.runBlocking {
                    transform(_appSession.value, current)
                }
            result = transformed
            transformed
        }
        return result
    }

    override suspend fun updateSessionsLastAlive() {
        // No-op for fake implementation
    }

    override suspend fun startOctopusSession(): OctopusResult<SessionRepository.SessionStartResult, Nothing> {
        val appSessionId = _appSession.value?.id ?: "app-session-${System.currentTimeMillis()}"
        val session =
            Sessions.Session.Octopus(
                id = "community-session-${System.currentTimeMillis()}",
                isFirstSession = false,
                appSessionId = appSessionId,
                startCount = 1,
                displayedGamifications = emptyList(),
            )
        _octopusSession.update { session }
        return OctopusResult.Success(
            SessionRepository.SessionStartResult(
                gamificationActions = emptyList(),
            ),
        )
    }
}
