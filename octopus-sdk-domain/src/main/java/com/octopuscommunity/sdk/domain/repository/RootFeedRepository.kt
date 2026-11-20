package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.RootFeed
import com.octopuscommunity.sdk.domain.network.OctopusResult
import kotlinx.coroutines.flow.Flow

/** Repository for the root feeds, the top-level feeds of the community home. */
interface RootFeedRepository {
    /** Observes the cached root feeds. */
    fun getRootFeeds(): Flow<List<RootFeed>>

    /** Fetches the root feeds from the backend and updates the cache. */
    suspend fun fetchRootFeeds(): OctopusResult<List<RootFeed>, Nothing>
}
