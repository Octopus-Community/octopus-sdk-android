package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.Sessions.Session
import com.octopuscommunity.sdk.domain.model.Sessions.Session.App.PlayedVideo
import com.octopuscommunity.sdk.domain.network.OctopusResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for the stored app and Octopus sessions.
 *
 * The `update*` functions replace the stored session with what their transform returns, and a
 * `null` result erases it.
 */
interface SessionRepository {
    /** Outcome of [startOctopusSession]. */
    data class SessionStartResult(
        val gamificationActions: List<Gamification.Action>,
    )

    /** Returns the stored app session. */
    suspend fun getAppSession(): Session.App?

    /** Observes the stored app session. */
    fun getAppSessionFlow(): Flow<Session.App?>

    /** Returns the stored Octopus session. */
    suspend fun getOctopusSession(): Session.Octopus?

    /** Observes the stored Octopus session. */
    fun getOctopusSessionFlow(): Flow<Session.Octopus?>

    /** Replaces the app session with the result of [transform], and returns it. */
    suspend fun updateAppSession(transform: suspend (Session.App?) -> Session.App?): Session.App?

    /**
     * Replaces the Octopus session with the result of [transform], which also receives the app
     * session, and returns it.
     */
    suspend fun updateOctopusSession(transform: suspend (Session.App?, Session.Octopus?) -> Session.Octopus?): Session.Octopus?

    /** Whether the gamification of [action] was already displayed in this Octopus session. */
    suspend fun hasDisplayedGamification(action: Gamification.Action) =
        getOctopusSession()?.displayedGamifications?.contains(action) ?: false

    /** Records that the gamification of [action] was displayed in this Octopus session. */
    suspend fun setGamificationDisplayed(action: Gamification.Action) {
        updateOctopusSession { _, octopusSession ->
            octopusSession?.copy(
                displayedGamifications = octopusSession.displayedGamifications + action,
            )
        }
    }

    /** Updates the last-alive time of the running sessions. */
    suspend fun updateSessionsLastAlive()

    /** Notifies the backend that the user entered the community. */
    suspend fun startOctopusSession(): OctopusResult<SessionStartResult, Nothing>

    /** Observes the videos played in this app session. */
    fun getPlayedVideosFlow() = getAppSessionFlow().map { it?.playedVideos ?: emptyMap() }

    /** Observes the played video stored under [itemId] in this app session. */
    fun getPlayedVideoFlow(itemId: String) = getPlayedVideosFlow().map { it[itemId] }

    /** Observes whether videos are muted. Defaults to `true`. */
    fun getVideosMutedFlow() = getAppSessionFlow().map { it?.videosMuted ?: true }

    /** Inserts or updates the played video stored under [videoId] with [transform]. */
    suspend fun upsertPlayedVideo(
        videoId: String,
        transform: (t: PlayedVideo?) -> PlayedVideo,
    ) = updateAppSession { appSession ->
        appSession?.copy(
            playedVideos = appSession.playedVideos + (videoId to transform(appSession.playedVideos[videoId])),
        )
    }

    /** Mutes or unmutes the videos for this app session. */
    suspend fun setVideosMuted(mute: Boolean) = updateAppSession { it?.copy(videosMuted = mute) }
}
