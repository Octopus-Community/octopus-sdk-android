package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.sdk.data.model.AnchoredFeed
import com.octopuscommunity.sdk.domain.model.Feed
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.mapSuccess
import com.octopuscommunity.sdk.domain.repository.OctopusItemRepository

/** Implementation capability used by the SDK UI to page a feed window stored apart from other lists. */
interface WindowFeedRepository<Item : OctopusItem> {
    /**
     * Fetches a page of [feedId] from [pageCursor], caching it under [storageFeedId].
     * The returned feed observes that separate cache; [pageSize] limits the requested page.
     */
    suspend fun fetchItems(
        feedId: String,
        storageFeedId: String,
        pageSize: Int? = null,
        pageCursor: String? = null,
    ): OctopusResult<Feed<Item>, Nothing>

    /**
     * Fetches the page of [feedId] around [anchorItemId], cached under [storageFeedId].
     * [pageSize] limits the page. If the item cannot be served there, the first page is returned
     * with [AnchoredFeed.isAnchored] false. The returned cursors page in both directions.
     */
    suspend fun fetchItemsAround(
        feedId: String,
        anchorItemId: String,
        pageSize: Int? = null,
        storageFeedId: String = feedId,
    ): OctopusResult<AnchoredFeed<Item>, Nothing>

    /**
     * Fetches items of [feedId] before the loaded window, closest first, from [pageCursor].
     * [pageSize] limits the page, cached alongside the window under [storageFeedId].
     * The returned [Feed.nextPageCursor] continues toward the start of the feed.
     */
    suspend fun fetchPreviousItems(
        feedId: String,
        pageCursor: String,
        pageSize: Int? = null,
        storageFeedId: String = feedId,
    ): OctopusResult<Feed<Item>, Nothing>

    /** Deletes cached references for [feedId]; orphaned items are cleaned up separately. */
    suspend fun deleteLocalFeed(feedId: String)

    /**
     * Deletes cached feed references whose ids contain [idMarker], except those also containing
     * [keptIdMarker]. An empty [idMarker] deletes nothing. Items are cleaned up separately.
     */
    suspend fun deleteLocalFeeds(
        idMarker: String,
        keptIdMarker: String,
    )
}

/**
 * Returns this repository's window capability, or the original first-page fallback for repositories
 * that do not implement it. The fallback neither anchors nor stores or deletes separate windows.
 */
@Suppress("UNCHECKED_CAST")
fun <Item : OctopusItem> OctopusItemRepository<Item, *, *>.windowFeeds(): WindowFeedRepository<Item> =
    this as? WindowFeedRepository<Item> ?: DefaultWindowFeedRepository(this)

private class DefaultWindowFeedRepository<Item : OctopusItem>(
    private val repository: OctopusItemRepository<Item, *, *>,
) : WindowFeedRepository<Item> {
    override suspend fun fetchItems(
        feedId: String,
        storageFeedId: String,
        pageSize: Int?,
        pageCursor: String?,
    ): OctopusResult<Feed<Item>, Nothing> = repository.fetchItems(feedId = feedId, pageSize = pageSize, pageCursor = pageCursor)

    override suspend fun fetchItemsAround(
        feedId: String,
        anchorItemId: String,
        pageSize: Int?,
        storageFeedId: String,
    ): OctopusResult<AnchoredFeed<Item>, Nothing> =
        fetchItems(feedId = feedId, storageFeedId = storageFeedId, pageSize = pageSize)
            .mapSuccess { feed -> AnchoredFeed(feed = feed) }

    override suspend fun fetchPreviousItems(
        feedId: String,
        pageCursor: String,
        pageSize: Int?,
        storageFeedId: String,
    ): OctopusResult<Feed<Item>, Nothing> =
        OctopusResult.Success(Feed(id = storageFeedId, items = repository.getLocalItemsFlow(storageFeedId)))

    override suspend fun deleteLocalFeed(feedId: String) = Unit

    override suspend fun deleteLocalFeeds(
        idMarker: String,
        keptIdMarker: String,
    ) = Unit
}
