package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.sdk.data.local.dao.FeedDao
import com.octopuscommunity.sdk.data.mapper.FeedMapper
import com.octopuscommunity.sdk.data.remote.service.FeedService
import com.octopuscommunity.sdk.data.utils.call
import com.octopuscommunity.sdk.domain.model.RootFeed
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.RootFeedRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RootFeedRepositoryImpl(
    private val feedDao: FeedDao,
    private val feedService: FeedService,
    private val feedMapper: FeedMapper,
) : RootFeedRepository {
    override fun getRootFeeds(): Flow<List<RootFeed>> =
        with(feedMapper) {
            feedDao.getAllRootFeeds().map { it.map { it.toDomain() } }
        }

    override suspend fun fetchRootFeeds(): OctopusResult<List<RootFeed>, Nothing> =
        call(mapper = feedMapper) {
            val session = feedService.rootFeedSession
            val generation = session?.snapshot()
            feedService
                .getRootFeedsInfo()
                .feedsInfoList
                .mapIndexed { index, feedInfo ->
                    feedInfo.toEntity(position = index)
                }.also { feeds ->
                    if (session != null && generation != null) {
                        session.commit(generation) { feedDao.replaceAllRootFeeds(feeds) }
                    } else {
                        currentCoroutineContext().ensureActive()
                        feedDao.replaceAllRootFeeds(feeds)
                    }
                }.map { it.toDomain() }
        }
}
