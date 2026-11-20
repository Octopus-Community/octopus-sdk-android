package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.model.Sessions
import com.octopuscommunity.sdk.domain.model.TrackerEvent
import com.octopuscommunity.sdk.domain.model.TrackerEvent.ClientObjectOpened
import com.octopuscommunity.sdk.domain.model.TrackerEvent.PostOpened
import com.octopuscommunity.sdk.domain.model.TrackerEvent.TranslationButtonClicked
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.TrackerRepository

/** [TrackerRepository] that sends nothing and always succeeds. */
open class FakeTrackerRepository : TrackerRepository {
    override suspend fun trackPostOpened(
        success: Boolean,
        origin: PostOpened.Origin,
    ): OctopusResult<Unit, Nothing> = trackEvent(TrackerEvent.PostOpened("", "", success, origin))

    override suspend fun trackClientObjectOpened(): OctopusResult<Unit, Nothing> = trackEvent(ClientObjectOpened("", ""))

    override suspend fun trackTranslationButtonClicked(translationDisplayed: Boolean): OctopusResult<Unit, Nothing> =
        trackEvent(TranslationButtonClicked("", "", translationDisplayed))

    override suspend fun trackCtaButtonClicked(postId: String): OctopusResult<Unit, Nothing> =
        trackEvent(TrackerEvent.CtaButtonClicked("", "", postId))

    override suspend fun trackTopicCtaButtonClicked(topicId: String): OctopusResult<Unit, Nothing> =
        trackEvent(TrackerEvent.TopicCtaButtonClicked("", "", topicId))

    override suspend fun trackClientSetABTest(hasAccessToCommunity: Boolean): OctopusResult<Unit, Nothing> =
        trackEvent(TrackerEvent.ClientSetABTest("", "", hasAccessToCommunity))

    override suspend fun trackVideosPlayed(videos: Collection<Sessions.Session.App.PlayedVideo>): OctopusResult<Unit, Nothing> =
        OctopusResult.Success(Unit)

    override suspend fun trackCustomEvent(customEvent: TrackerEvent.Custom): OctopusResult<Unit, Nothing> =
        trackEvent(customEvent as TrackerEvent)

    override suspend fun trackEvent(event: TrackerEvent): OctopusResult<Unit, Nothing> = OctopusResult.Success(Unit)

    override suspend fun trackEvents(events: List<TrackerEvent>): OctopusResult<Unit, Nothing> = OctopusResult.Success(Unit)
}
