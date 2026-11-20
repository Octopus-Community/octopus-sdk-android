package com.octopuscommunity.sdk.data.local.data

import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.utils.toBase64
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SessionsData(
    val appSession: SessionData.App? = null,
    val octopusSession: SessionData.Octopus? = null,
    // Written atomically with the stop; retained until Room has durably queued the end.
    val pendingAppSessionEnds: List<SessionData.App> = emptyList(),
) {
    @Serializable
    sealed interface SessionData {
        @Serializable
        data class App(
            override val id: String = UUID.randomUUID().toBase64(),
            override val isFirstSession: Boolean = true,
            override val startTime: Long = System.currentTimeMillis(),
            override val lastAliveTime: Long = startTime,
            override val stopTime: Long? = null,
            val toggledTranslations: Map<String, Boolean> = emptyMap(),
            val playedVideos: Map<String, PlayedVideo> = emptyMap(),
            val videosMuted: Boolean = true,
        ) : SessionData {
            @Serializable
            data class PlayedVideo(
                val videoId: String,
                val itemId: String,
                val position: Long,
                val duration: Long,
                val watchedTime: Long,
            )
        }

        @Serializable
        data class Octopus(
            override val id: String = UUID.randomUUID().toBase64(),
            override val isFirstSession: Boolean = true,
            override val startTime: Long = System.currentTimeMillis(),
            override val lastAliveTime: Long = startTime,
            override val stopTime: Long? = null,
            val appSessionId: String = "",
            val startCount: Int = 0,
            val displayedGamifications: List<Gamification.Action> = listOf(),
        ) : SessionData

        val id: String
        val isFirstSession: Boolean
        val startTime: Long
        val lastAliveTime: Long
        val stopTime: Long?
    }
}
