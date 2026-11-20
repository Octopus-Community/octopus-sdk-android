package com.octopuscommunity.sdk.data.local.datastore

import android.content.Context
import com.octopuscommunity.sdk.data.local.data.SessionsData
import com.octopuscommunity.sdk.data.local.data.SessionsData.SessionData
import com.octopuscommunity.sdk.data.utils.dataStoreSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

private val Context.dataStore by contextDataStore(
    fileName = "octopus_sessions.json",
    serializer = SessionsData.serializer().dataStoreSerializer(SessionsData()),
)

class SessionsDataStore(
    val context: Context,
    val sdkScope: CoroutineScope,
) {
    private val queuedAppSessionEndIds = ConcurrentHashMap.newKeySet<String>()

    val sessionsFlow get() = context.dataStore.data

    val appSessionFlow get() = sessionsFlow.map { it.appSession }
    val octopusSessionFlow get() = sessionsFlow.map { it.octopusSession }

    suspend fun getSessions() = sessionsFlow.firstOrNull()

    suspend fun getAppSession() = appSessionFlow.firstOrNull()

    suspend fun getOctopusSession() = octopusSessionFlow.firstOrNull()

    suspend fun update(transform: suspend (t: SessionsData) -> SessionsData): SessionsData {
        val queued = queuedAppSessionEndIds.toSet()
        return context.dataStore
            .updateData { sessions ->
                val updated = transform(sessions)
                updated.copy(pendingAppSessionEnds = updated.pendingAppSessionEnds.filterNot { it.id in queued })
            }.also { queuedAppSessionEndIds.removeAll(queued) }
    }

    // Prune on the next ordinary write. Tracking is also called by existing Octopus-session
    // transforms: writing here would nest updateData and deadlock. After a process death the
    // journal can replay safely because the Room receipt was committed before this signal.
    internal fun acknowledgeAppSessionEnds(ids: Set<String>) {
        queuedAppSessionEndIds.addAll(ids)
    }

    suspend fun updateAppSession(transform: suspend (t: SessionsData) -> SessionData.App?) =
        update { sessions ->
            sessions.copy(appSession = transform(sessions))
        }.appSession

    /** Clears user content and the community session while keeping the host app lifecycle live. */
    suspend fun clearUserData() {
        context.dataStore.updateData { sessions ->
            sessions.copy(
                appSession =
                    sessions.appSession?.copy(
                        toggledTranslations = emptyMap(),
                        playedVideos = emptyMap(),
                        videosMuted = true,
                    ),
                octopusSession = null,
            )
        }
    }

    suspend fun clear() {
        context.dataStore.updateData { SessionsData() }
    }
}
