package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.sdk.data.local.dao.TrackerEventDao
import com.octopuscommunity.sdk.data.local.data.SessionsData
import com.octopuscommunity.sdk.data.local.datastore.SessionsDataStore
import com.octopuscommunity.sdk.data.mapper.SessionMapper
import com.octopuscommunity.sdk.data.mapper.TrackerEventMapper
import com.octopuscommunity.sdk.data.remote.service.TrackingService
import com.octopuscommunity.sdk.data.utils.call
import com.octopuscommunity.sdk.domain.model.Sessions
import com.octopuscommunity.sdk.domain.model.TrackerEvent
import com.octopuscommunity.sdk.domain.model.TrackerEvent.PostOpened.Origin
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.CommunityClosureRepository
import com.octopuscommunity.sdk.domain.repository.TrackerRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class TrackerRepositoryImpl(
    protected val sessionsDataStore: SessionsDataStore,
    protected val trackerEventDao: TrackerEventDao,
    protected val trackingService: TrackingService,
    protected val trackerEventMapper: TrackerEventMapper,
    protected val closureRepository: CommunityClosureRepository = OpenCommunityClosureRepository,
) : TrackerRepository {
    private val sendEventsMutex = Mutex()

    override suspend fun trackPostOpened(
        success: Boolean,
        origin: Origin,
    ) = trackEvent {
        TrackerEvent.PostOpened(
            appSessionId = appSession?.id,
            octopusSessionId = octopusSession?.id,
            success = success,
            origin = origin,
        )
    }

    override suspend fun trackClientObjectOpened() =
        trackEvent {
            TrackerEvent.ClientObjectOpened(
                appSessionId = appSession?.id,
                octopusSessionId = octopusSession?.id,
            )
        }

    override suspend fun trackTranslationButtonClicked(translationDisplayed: Boolean) =
        trackEvent {
            TrackerEvent.TranslationButtonClicked(
                appSessionId = appSession?.id,
                octopusSessionId = octopusSession?.id,
                translationDisplayed = translationDisplayed,
            )
        }

    override suspend fun trackCtaButtonClicked(postId: String) =
        trackEvent {
            TrackerEvent.CtaButtonClicked(
                appSessionId = appSession?.id,
                octopusSessionId = octopusSession?.id,
                postId = postId,
            )
        }

    override suspend fun trackTopicCtaButtonClicked(topicId: String) =
        trackEvent {
            TrackerEvent.TopicCtaButtonClicked(
                appSessionId = appSession?.id,
                octopusSessionId = octopusSession?.id,
                topicId = topicId,
            )
        }

    override suspend fun trackClientSetABTest(hasAccessToCommunity: Boolean) =
        trackEvent {
            TrackerEvent.ClientSetABTest(
                appSessionId = appSession?.id,
                octopusSessionId = octopusSession?.id,
                hasAccessToCommunity = hasAccessToCommunity,
            )
        }

    override suspend fun trackVideosPlayed(videos: Collection<Sessions.Session.App.PlayedVideo>) =
        trackEvents {
            videos.map {
                TrackerEvent.VideoPlayed(
                    appSessionId = appSession?.id,
                    octopusSessionId = octopusSession?.id,
                    contentId = it.itemId,
                    videoId = it.videoId,
                    videoDuration = it.duration,
                    watchedTime = it.watchedTime,
                )
            }
        }

    override suspend fun trackCustomEvent(customEvent: TrackerEvent.Custom) =
        trackEvent {
            customEvent.apply {
                appSessionId = appSession?.id
                octopusSessionId = octopusSession?.id
            }
        }

    // Nothing is recorded while the community is closed: it would leave all at once when it reopens.
    override suspend fun trackEvent(event: TrackerEvent): OctopusResult<Unit, Nothing> =
        if (closureRepository.isClosed) {
            OctopusResult.Success(Unit)
        } else {
            recordEvent(event)
        }

    private suspend fun recordEvent(event: TrackerEvent) =
        with(trackerEventMapper) {
            if (event is TrackerEvent.LeavingApp) {
                trackerEventDao.enqueueAppSessionEnd(event.toEntity())
            } else {
                trackerEventDao.upsertEvent(event.toEntity())
            }
            sendPendingEvents()
        }

    suspend fun trackEvent(event: SessionsData.() -> TrackerEvent): OctopusResult<Unit, Nothing> =
        sessionsDataStore.getSessions()?.let { trackEvent(event.invoke(it)) }
            ?: OctopusResult.Failure.ContentUnavailable

    override suspend fun trackEvents(events: List<TrackerEvent>): OctopusResult<Unit, Nothing> =
        if (closureRepository.isClosed) {
            OctopusResult.Success(Unit)
        } else {
            recordEvents(events)
        }

    private suspend fun recordEvents(events: List<TrackerEvent>) =
        with(trackerEventMapper) {
            events.filterIsInstance<TrackerEvent.LeavingApp>().forEach {
                trackerEventDao.enqueueAppSessionEnd(it.toEntity())
            }
            trackerEventDao.upsertEvents(events.filterNot { it is TrackerEvent.LeavingApp }.map { it.toEntity() })
            sendPendingEvents()
        }

    suspend fun trackEvents(event: SessionsData.() -> List<TrackerEvent>): OctopusResult<Unit, Nothing> =
        sessionsDataStore.getSessions()?.let { trackEvents(event.invoke(it)) }
            ?: OctopusResult.Failure.ContentUnavailable

    suspend fun sendPendingEvents(): OctopusResult<Unit, Nothing> =
        sendEventsMutex.withLock {
            call(trackerEventMapper) {
                enqueueJournaledAppSessionEnds()
                val events =
                    trackerEventDao.getAllEvents().takeIf {
                        it.isNotEmpty()
                    } ?: return@call
                trackingService.track(events.map { it.toProto() })
                trackerEventDao.deleteEvents(events)
            }
        }

    private suspend fun enqueueJournaledAppSessionEnds() {
        val pending = sessionsDataStore.getSessions()?.pendingAppSessionEnds.orEmpty()
        if (pending.isEmpty()) return
        val mapper = SessionMapper()
        with(trackerEventMapper) {
            pending.forEach { session ->
                val event = TrackerEvent.LeavingApp(with(mapper) { session.toDomain() })
                trackerEventDao.enqueueAppSessionEnd(event.toEntity())
            }
        }
        sessionsDataStore.acknowledgeAppSessionEnds(pending.map { it.id }.toSet())
    }
}
