package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.sdk.data.local.data.SessionsData
import com.octopuscommunity.sdk.data.local.data.SessionsData.SessionData
import com.octopuscommunity.sdk.domain.model.Sessions
import kotlin.time.Duration.Companion.milliseconds

class SessionMapper : OctopusMapper {
    fun SessionsData.toDomain() = Sessions(
        appSession = appSession?.toDomain(),
        octopusSession = octopusSession?.toDomain()
    )

    fun SessionData.App.toDomain() = Sessions.Session.App(
        id = id,
        isFirstSession = isFirstSession,
        startTime = startTime,
        lastAliveTime = lastAliveTime,
        stopTime = stopTime,
        toggledTranslations = toggledTranslations,
        playedVideos = playedVideos.mapValues { (_, playedVideo) -> playedVideo.toDomain() },
        videosMuted = videosMuted
    )

    fun SessionData.App.PlayedVideo.toDomain() = Sessions.Session.App.PlayedVideo(
        videoId = videoId,
        itemId = itemId,
        position = position.milliseconds,
        duration = duration.milliseconds,
        watchedTime = watchedTime.milliseconds
    )

    fun SessionData.Octopus.toDomain() = Sessions.Session.Octopus(
        id = id,
        isFirstSession = isFirstSession,
        startTime = startTime,
        lastAliveTime = lastAliveTime,
        stopTime = stopTime,
        appSessionId = appSessionId,
        startCount = startCount,
        displayedGamifications = displayedGamifications
    )

    fun Sessions.toData() = SessionsData(
        appSession = appSession?.toData(),
        octopusSession = octopusSession?.toData(),
    )

    fun Sessions.Session.App.toData() = SessionData.App(
        id = id,
        isFirstSession = isFirstSession,
        startTime = startTime,
        lastAliveTime = lastAliveTime,
        stopTime = stopTime,
        toggledTranslations = toggledTranslations,
        playedVideos = playedVideos.mapValues { (_, playedVideo) -> playedVideo.toData() },
        videosMuted = videosMuted
    )

    fun Sessions.Session.App.PlayedVideo.toData() = SessionData.App.PlayedVideo(
        videoId = videoId,
        itemId = itemId,
        position = position.inWholeMilliseconds,
        duration = duration.inWholeMilliseconds,
        watchedTime = watchedTime.inWholeMilliseconds
    )

    fun Sessions.Session.Octopus.toData() = SessionData.Octopus(
        id = id,
        isFirstSession = isFirstSession,
        startTime = startTime,
        lastAliveTime = lastAliveTime,
        stopTime = stopTime,
        appSessionId = appSessionId,
        startCount = startCount,
        displayedGamifications = displayedGamifications
    )
}