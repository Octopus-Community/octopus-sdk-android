package com.octopuscommunity.sdk.domain.model

import com.octopuscommunity.sdk.domain.utils.toBase64
import java.util.UUID
import kotlin.time.Duration

/** The stored app and Octopus sessions. */
data class Sessions(
    val appSession: Session.App? = null,
    val octopusSession: Session.Octopus? = null,
) {
    /** A session, alive from [startTime] until [stopTime]. Times are epoch milliseconds. */
    sealed interface Session {
        /** Session of the host app, in the foreground. */
        data class App(
            override val id: String = UUID.randomUUID().toBase64(),
            override val isFirstSession: Boolean = true,
            override val startTime: Long = System.currentTimeMillis(),
            override val lastAliveTime: Long = startTime,
            override val stopTime: Long? = null,
            /** Display mode of the contents the user toggled, by content id; `true` means translated. */
            val toggledTranslations: Map<String, Boolean> = emptyMap(),
            /** Videos played in this session. */
            val playedVideos: Map<String, PlayedVideo> = emptyMap(),
            /** Whether videos are muted. */
            val videosMuted: Boolean = true,
        ) : Session {
            /** Playback state of the video [videoId] of the item [itemId]. */
            data class PlayedVideo(
                val videoId: String,
                val itemId: String,
                val position: Duration,
                val duration: Duration,
                val watchedTime: Duration,
            )
        }

        /** Session spent on Octopus screens, inside the app session [appSessionId]. */
        data class Octopus(
            override val id: String = UUID.randomUUID().toBase64(),
            override val isFirstSession: Boolean = true,
            override val startTime: Long = System.currentTimeMillis(),
            override val lastAliveTime: Long = startTime,
            override val stopTime: Long? = null,
            val appSessionId: String,
            val startCount: Int,
            /** Gamification actions already displayed in this session. */
            val displayedGamifications: List<Gamification.Action> = listOf(),
        ) : Session

        val id: String
        val startTime: Long

        /** Last time the session was known alive. */
        val lastAliveTime: Long
        val stopTime: Long?

        /** Whether no session of this kind was stored before this one. */
        val isFirstSession: Boolean

        /** Whether the session is stopped. */
        val isStopped get() = stopTime != null
    }
}
