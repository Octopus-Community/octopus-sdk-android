package com.octopuscommunity.sdk.domain.model

import com.octopuscommunity.sdk.domain.model.Sessions.Session
import java.util.Date
import kotlin.time.Duration

/** An analytics event sent by [TrackerRepository]. */
sealed interface TrackerEvent {
    data class EnteringApp(
        override val date: Date = Date(),
        override val appSessionId: String,
        val isFirstSession: Boolean,
    ) : TrackerEvent {
        constructor(session: Session.App) : this(
            date = Date(),
            appSessionId = session.id,
            isFirstSession = session.isFirstSession,
        )
    }

    data class LeavingApp(
        override val date: Date = Date(),
        override val appSessionId: String,
        val sessionStart: Date,
        val sessionEnd: Date = Date(),
        val isFirstSession: Boolean,
    ) : TrackerEvent {
        constructor(session: Session) : this(
            date = Date(),
            appSessionId = session.id,
            sessionStart = Date(session.startTime),
            sessionEnd = Date(session.stopTime ?: session.lastAliveTime),
            isFirstSession = session.isFirstSession,
        )
    }

    data class EnteringOctopus(
        override val date: Date = Date(),
        override val appSessionId: String,
        override val octopusSessionId: String,
        val isFirstSession: Boolean,
    ) : TrackerEvent {
        constructor(session: Session.Octopus) : this(
            date = Date(),
            appSessionId = session.appSessionId,
            octopusSessionId = session.id,
            isFirstSession = session.isFirstSession,
        )
    }

    data class LeavingOctopus(
        override val date: Date = Date(),
        override val appSessionId: String,
        override val octopusSessionId: String,
        val sessionStart: Date,
        val sessionEnd: Date = Date(),
        val isFirstSession: Boolean,
    ) : TrackerEvent {
        constructor(session: Session.Octopus) : this(
            date = Date(),
            appSessionId = session.appSessionId,
            octopusSessionId = session.id,
            sessionStart = Date(session.startTime),
            sessionEnd = Date(session.stopTime ?: session.lastAliveTime),
            isFirstSession = session.isFirstSession,
        )
    }

    data class PostOpened(
        override val appSessionId: String?,
        override val octopusSessionId: String?,
        val success: Boolean,
        val origin: Origin,
    ) : TrackerEvent {
        sealed interface Origin {
            data object ClientApp : Origin

            data class Sdk(
                val hasFeaturedComment: Boolean = false,
            ) : Origin
        }

        override val date: Date = Date()
    }

    data class ClientObjectOpened(
        override val appSessionId: String?,
        override val octopusSessionId: String?,
    ) : TrackerEvent {
        override val date: Date = Date()
    }

    data class TranslationButtonClicked(
        override val appSessionId: String?,
        override val octopusSessionId: String?,
        val translationDisplayed: Boolean,
    ) : TrackerEvent {
        override val date: Date = Date()
    }

    data class VideoPlayed(
        override val appSessionId: String?,
        override val octopusSessionId: String?,
        val contentId: String,
        val videoId: String,
        val videoDuration: Duration,
        val watchedTime: Duration,
    ) : TrackerEvent {
        override val date: Date = Date()
    }

    data class CtaButtonClicked(
        override val appSessionId: String?,
        override val octopusSessionId: String?,
        val postId: String,
    ) : TrackerEvent {
        override val date: Date = Date()
    }

    data class TopicCtaButtonClicked(
        override val appSessionId: String?,
        override val octopusSessionId: String?,
        val topicId: String,
    ) : TrackerEvent {
        override val date: Date = Date()
    }

    data class ClientSetABTest(
        override val appSessionId: String?,
        override val octopusSessionId: String?,
        val hasAccessToCommunity: Boolean,
    ) : TrackerEvent {
        override val date: Date = Date()
    }

    data class Custom(
        override val date: Date = Date(),
        val name: String,
        val properties: Map<String, Property> = emptyMap(),
    ) : TrackerEvent {
        data class Property(
            val value: String,
        )

        override var appSessionId: String? = null
        override var octopusSessionId: String? = null
    }

    /**
     * Login attributed to Octopus: the user hit a login-gated action in the SDK and then logged in
     * during the same app run. Emitted via the dedicated gRPC `octopusDrivenLogin` field.
     */
    data class OctopusDrivenLogin(
        override val date: Date = Date(),
        val action: OctopusDrivenLoginAction,
    ) : TrackerEvent {
        override var appSessionId: String? = null
        override var octopusSessionId: String? = null
    }

    /** When the event happened. */
    val date: Date

    /** Id of the app session the event happened in. */
    val appSessionId: String?

    /** Id of the Octopus session the event happened in, if any. */
    val octopusSessionId: String? get() = null
}
