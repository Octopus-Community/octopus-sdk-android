package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.Sessions
import com.octopuscommunity.sdk.domain.model.TrackerEvent
import com.octopuscommunity.sdk.domain.model.TrackerEvent.PostOpened
import com.octopuscommunity.sdk.domain.network.OctopusResult

/** Repository that sends analytics events to the Octopus backend. */
interface TrackerRepository {
    /** Tracks a post opening from [origin], and whether it succeeded. */
    suspend fun trackPostOpened(
        success: Boolean,
        origin: PostOpened.Origin,
    ): OctopusResult<Unit, Nothing>

    /** Tracks the opening of a client object post. */
    suspend fun trackClientObjectOpened(): OctopusResult<Unit, Nothing>

    /** Tracks a click on the translation button. [translationDisplayed] is the state after the click. */
    suspend fun trackTranslationButtonClicked(translationDisplayed: Boolean): OctopusResult<Unit, Nothing>

    /** Tracks a click on the call-to-action button of the post [postId]. */
    suspend fun trackCtaButtonClicked(postId: String): OctopusResult<Unit, Nothing>

    /** Tracks a click on the call-to-action button of the group [topicId]. */
    suspend fun trackTopicCtaButtonClicked(topicId: String): OctopusResult<Unit, Nothing>

    /**
     * Tracks the community access the host app set for an A/B test. The default implementation
     * tracks nothing.
     */
    suspend fun trackClientSetABTest(hasAccessToCommunity: Boolean): OctopusResult<Unit, Nothing> = OctopusResult.Success(Unit)

    /** Tracks the played [videos]. */
    suspend fun trackVideosPlayed(videos: Collection<Sessions.Session.App.PlayedVideo>): OctopusResult<Unit, Nothing>

    /** Tracks a custom event of the host app. */
    suspend fun trackCustomEvent(customEvent: TrackerEvent.Custom): OctopusResult<Unit, Nothing>

    /** Tracks [event]. */
    suspend fun trackEvent(event: TrackerEvent): OctopusResult<Unit, Nothing>

    /** Tracks [events]. */
    suspend fun trackEvents(events: List<TrackerEvent>): OctopusResult<Unit, Nothing>
}
