package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.model.RootFeed
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.RootFeedRepository
import com.octopuscommunity.sdk.test.mock.MockFeeds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** [RootFeedRepository] serving [MockFeeds.Post.allRoots]. */
class FakeRootFeedRepository : RootFeedRepository {
    private val _feeds = MutableStateFlow(MockFeeds.Post.allRoots)

    override fun getRootFeeds(): Flow<List<RootFeed>> = _feeds.map { it }

    override suspend fun fetchRootFeeds(): OctopusResult<List<RootFeed>, Nothing> = OctopusResult.Success(MockFeeds.Post.allRoots)
}
