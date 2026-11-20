package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.TrackRequestKt.clickOnCta
import com.octopuscommunity.TrackRequestKt.clickOnTopicCta
import com.octopuscommunity.TrackRequestKt.clientApp
import com.octopuscommunity.TrackRequestKt.clientSetABTestInfo
import com.octopuscommunity.TrackRequestKt.customEvent
import com.octopuscommunity.TrackRequestKt.enteringApp
import com.octopuscommunity.TrackRequestKt.enteringOctopus
import com.octopuscommunity.TrackRequestKt.event
import com.octopuscommunity.TrackRequestKt.leavingApp
import com.octopuscommunity.TrackRequestKt.leavingOctopus
import com.octopuscommunity.TrackRequestKt.octopusDrivenLogin
import com.octopuscommunity.TrackRequestKt.openClientObjectFromBridge
import com.octopuscommunity.TrackRequestKt.origin
import com.octopuscommunity.TrackRequestKt.postOpened
import com.octopuscommunity.TrackRequestKt.propertyValue
import com.octopuscommunity.TrackRequestKt.sdk
import com.octopuscommunity.TrackRequestKt.sessionSummary
import com.octopuscommunity.TrackRequestKt.videoPlayed
import com.octopuscommunity.TrackRequestKt.viewTranslationButtonHit
import com.octopuscommunity.TrackingServiceProto.TrackRequest
import com.octopuscommunity.sdk.data.local.entity.TrackerEventEntity
import com.octopuscommunity.sdk.domain.model.OctopusDrivenLoginAction
import com.octopuscommunity.sdk.domain.model.TrackerEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit.SECONDS

open class TrackerEventMapper : OctopusMapper {
    fun TrackerEvent.toEntity() =
        TrackerEventEntity(
            timestamp = date.time,
            appSessionId = appSessionId,
            octopusSessionId = octopusSessionId,
            data =
                when (this) {
                    is TrackerEvent.EnteringApp ->
                        TrackerEventEntity.Data.EnteringApp(
                            isFirstSession = isFirstSession,
                        )

                    is TrackerEvent.LeavingApp ->
                        TrackerEventEntity.Data.LeavingApp(
                            sessionStartTime = sessionStart.time,
                            sessionEndTime = sessionEnd.time,
                            isFirstSession = isFirstSession,
                        )

                    is TrackerEvent.EnteringOctopus ->
                        TrackerEventEntity.Data.EnteringOctopus(
                            isFirstSession = isFirstSession,
                        )

                    is TrackerEvent.LeavingOctopus ->
                        TrackerEventEntity.Data.LeavingOctopus(
                            sessionStartTime = sessionStart.time,
                            sessionEndTime = sessionEnd.time,
                            isFirstSession = isFirstSession,
                        )

                    is TrackerEvent.PostOpened ->
                        TrackerEventEntity.Data.PostOpened(
                            success = success,
                            origin =
                                when (val origin = origin) {
                                    is TrackerEvent.PostOpened.Origin.ClientApp ->
                                        TrackerEventEntity.Data.PostOpened.Origin.ClientApp

                                    is TrackerEvent.PostOpened.Origin.Sdk ->
                                        TrackerEventEntity.Data.PostOpened.Origin.Sdk(
                                            hasFeaturedComment = origin.hasFeaturedComment,
                                        )
                                },
                        )

                    is TrackerEvent.ClientObjectOpened -> TrackerEventEntity.Data.ClientObjectOpened

                    is TrackerEvent.TranslationButtonClicked ->
                        TrackerEventEntity.Data.TranslationButtonClicked(
                            translationDisplayed = translationDisplayed,
                        )

                    is TrackerEvent.VideoPlayed ->
                        TrackerEventEntity.Data.VideoPlayed(
                            contentId = contentId,
                            videoId = videoId,
                            watchedTime = watchedTime.inWholeMilliseconds,
                            videoDuration = videoDuration.inWholeMilliseconds,
                        )

                    is TrackerEvent.CtaButtonClicked ->
                        TrackerEventEntity.Data.CtaButtonClicked(
                            postId = postId,
                        )

                    is TrackerEvent.TopicCtaButtonClicked ->
                        TrackerEventEntity.Data.TopicCtaButtonClicked(
                            topicId = topicId,
                        )

                    is TrackerEvent.ClientSetABTest ->
                        TrackerEventEntity.Data.ClientSetABTest(
                            hasAccessToCommunity = hasAccessToCommunity,
                        )

                    is TrackerEvent.Custom ->
                        TrackerEventEntity.Data.Custom(
                            name = name,
                            properties = properties.mapValues { it.value.value },
                        )

                    is TrackerEvent.OctopusDrivenLogin ->
                        TrackerEventEntity.Data.OctopusDrivenLogin(
                            action = action.name,
                        )
                },
        )

    fun TrackerEventEntity.toProto() =
        event {
            timestamp = this@toProto.timestamp
            this@toProto.appSessionId?.let { appSessionId = it }
            this@toProto.octopusSessionId?.let { octoSessionId = it }
            when (val data = this@toProto.data) {
                is TrackerEventEntity.Data.EnteringApp ->
                    enteringApp =
                        enteringApp {
                            firstSession = data.isFirstSession
                        }

                is TrackerEventEntity.Data.LeavingApp ->
                    leavingApp =
                        leavingApp {
                            sessionSummary =
                                sessionSummary {
                                    startedAt = data.sessionStartTime
                                    endedAt = data.sessionEndTime
                                    duration = data.sessionDuration
                                    firstSession = data.isFirstSession
                                }
                        }

                is TrackerEventEntity.Data.EnteringOctopus -> {
                    enteringOctopus =
                        enteringOctopus {
                            firstSession = data.isFirstSession
                        }
                }

                is TrackerEventEntity.Data.LeavingOctopus -> {
                    leavingOctopus =
                        leavingOctopus {
                            sessionSummary =
                                sessionSummary {
                                    startedAt = data.sessionStartTime
                                    endedAt = data.sessionEndTime
                                    duration = data.sessionDuration
                                    firstSession = data.isFirstSession
                                }
                        }
                }

                is TrackerEventEntity.Data.PostOpened -> {
                    postOpened =
                        postOpened {
                            success = data.success
                            origin =
                                origin {
                                    when (val origin = data.origin) {
                                        is TrackerEventEntity.Data.PostOpened.Origin.ClientApp -> {
                                            fromClientApp = clientApp {}
                                        }

                                        is TrackerEventEntity.Data.PostOpened.Origin.Sdk -> {
                                            fromOctopusSdk =
                                                sdk {
                                                    hasFeaturedComment = origin.hasFeaturedComment
                                                }
                                        }
                                    }
                                }
                        }
                }

                TrackerEventEntity.Data.ClientObjectOpened -> {
                    openClientObjectFromBridge = openClientObjectFromBridge {}
                }

                is TrackerEventEntity.Data.TranslationButtonClicked -> {
                    viewTranslationButtonHit =
                        viewTranslationButtonHit {
                            viewTranslated = data.translationDisplayed
                        }
                }

                is TrackerEventEntity.Data.VideoPlayed -> {
                    videoPlayed =
                        videoPlayed {
                            objectId = data.contentId
                            watchTimeSec = data.watchedTime.milliseconds.toInt(SECONDS)
                            videoDurationSec = data.videoDuration.milliseconds.toInt(SECONDS)
                            videoId = data.videoId
                        }
                }

                is TrackerEventEntity.Data.CtaButtonClicked -> {
                    clickOnCta =
                        clickOnCta {
                            objectId = data.postId
                        }
                }

                is TrackerEventEntity.Data.TopicCtaButtonClicked -> {
                    clickOnTopicCta =
                        clickOnTopicCta {
                            objectId = data.topicId
                        }
                }

                is TrackerEventEntity.Data.ClientSetABTest -> {
                    clientSetABTestInfo =
                        clientSetABTestInfo {
                            hasAccessToCommunity = data.hasAccessToCommunity
                        }
                }

                is TrackerEventEntity.Data.Custom -> {
                    customEvent =
                        customEvent {
                            name = data.name
                            properties.putAll(
                                data.properties.mapValues { value ->
                                    propertyValue {
                                        this.value = value.value
                                    }
                                },
                            )
                        }
                }

                is TrackerEventEntity.Data.OctopusDrivenLogin -> {
                    octopusDrivenLogin =
                        octopusDrivenLogin {
                            action = OctopusDrivenLoginAction.valueOf(data.action).toProtoAction()
                        }
                }
            }
        }

    private fun OctopusDrivenLoginAction.toProtoAction(): TrackRequest.OctopusDrivenLogin.Action =
        when (this) {
            OctopusDrivenLoginAction.POST -> TrackRequest.OctopusDrivenLogin.Action.POST
            OctopusDrivenLoginAction.COMMENT -> TrackRequest.OctopusDrivenLogin.Action.COMMENT
            OctopusDrivenLoginAction.REPLY -> TrackRequest.OctopusDrivenLogin.Action.REPLY
            OctopusDrivenLoginAction.REACTION -> TrackRequest.OctopusDrivenLogin.Action.REACTION
            OctopusDrivenLoginAction.VOTE -> TrackRequest.OctopusDrivenLogin.Action.VOTE
            OctopusDrivenLoginAction.MODERATION -> TrackRequest.OctopusDrivenLogin.Action.MODERATION
            OctopusDrivenLoginAction.BLOCK_USER -> TrackRequest.OctopusDrivenLogin.Action.BLOCK_USER
            OctopusDrivenLoginAction.VIEW_OWN_PROFILE -> TrackRequest.OctopusDrivenLogin.Action.VIEW_OWN_PROFILE
        }
}
