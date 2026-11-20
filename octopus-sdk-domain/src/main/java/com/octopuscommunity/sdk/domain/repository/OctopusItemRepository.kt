package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.Feed
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.ServerError
import kotlinx.coroutines.flow.Flow

/**
 * Common repository for the [OctopusItem]s: posts, comments and replies.
 *
 * `get*Flow` functions observe the local cache; `fetch*` functions call the backend and update it.
 */
interface OctopusItemRepository<Item : OctopusItem, Draft : OctopusItem.Draft, Error : OctopusItem.CreationError> {
    /** Observes the cached item [itemId]. */
    fun getLocalItemFlow(itemId: String): Flow<Item?>

    /** Observes the cached items of the feed [feedId]. */
    fun getLocalItemsFlow(feedId: String): Flow<List<Item>>

    /**
     * Fetches the item [itemId]. [hasVideo] tells whether it carries a video, and
     * [incrementViewCount] whether the fetch counts as a view.
     */
    suspend fun fetchItem(
        itemId: String,
        hasVideo: Boolean?,
        incrementViewCount: Boolean,
    ): OctopusResult<Item?, Nothing>

    /** Fetches a page of the feed [feedId], starting at [pageCursor]. */
    suspend fun fetchItems(
        feedId: String,
        pageSize: Int? = null,
        pageCursor: String? = null,
    ): OctopusResult<Feed<Item>, Nothing>

    /** Fetches the items described by [requests], and their children described by [childRequests]. */
    suspend fun fetchItems(
        requests: List<FetchRequest>,
        childRequests: List<FetchRequest> = emptyList(),
    ): OctopusResult<Unit, Nothing>

    /** Fetches the full items [itemIds]. */
    suspend fun fetchItems(itemIds: List<String>): OctopusResult<Unit, Nothing> =
        fetchItems(requests = itemIds.map { FetchRequest.Full(itemId = it) })

    /** Fetches the counters of the item [itemId]. */
    suspend fun fetchCounters(
        itemId: String,
        hasVideo: Boolean?,
        incrementViewCount: Boolean,
    ): OctopusResult<Unit, Nothing>

    /** Fetches the counters of the item [itemId], without counting a view. */
    suspend fun fetchCounters(itemId: String) =
        fetchCounters(
            itemId = itemId,
            hasVideo = null,
            incrementViewCount = false,
        )

    /**
     * Fetches the counters of [itemIds] and of their children [childIds]. [videoItemIds] lists the
     * items that carry a video, and [incrementViewCount] tells whether the fetch counts as a view.
     */
    suspend fun fetchCounters(
        itemIds: List<String>,
        childIds: List<String> = listOf(),
        videoItemIds: List<String>?,
        incrementViewCount: Boolean,
    ): OctopusResult<Unit, Nothing>

    /** Fetches the counters of [itemIds] and of their children [childIds], without counting a view. */
    suspend fun fetchCounters(
        itemIds: List<String>,
        childIds: List<String> = listOf(),
    ) = fetchCounters(
        itemIds = itemIds,
        childIds = childIds,
        videoItemIds = null,
        incrementViewCount = false,
    )

    /** Sets [reactionKind] on [item], or removes the reaction when it is `null`. */
    suspend fun setReaction(
        item: Item,
        reactionKind: Reaction.Kind?,
        isContentTranslated: Boolean?,
    ): OctopusResult<Reaction?, ServerError>

    /** Deletes the cached items that no cached feed references. */
    suspend fun deleteLocalOrphanedItems()

    /**
     * Marks every cached item as stale so the next access re-resolves it through the network
     * instead of returning a cache hit. Used after the connected user's entitlements change
     * to force a re-fetch of `canAccess` / `canCreateChildren` flags carried by cached items.
     */
    suspend fun resetCache()
}

/** What to fetch for one item in [OctopusItemRepository.fetchItems]. */
sealed class FetchRequest(
    val fetchObject: Boolean = false,
    val fetchCounters: Boolean = false,
    val fetchCurrentUserActions: Boolean = false,
) {
    /** Fetches the item itself, its counters and the current user's actions on it. */
    data class Full(
        override val itemId: String,
        override val hasVideo: Boolean?,
        override val incrementViewCount: Boolean,
    ) : FetchRequest(
            fetchObject = true,
            fetchCounters = true,
            fetchCurrentUserActions = true,
        ) {
        constructor(itemId: String) : this(
            itemId = itemId,
            hasVideo = null,
            incrementViewCount = false,
        )
    }

    /** Fetches the counters of the item and the current user's actions on it. */
    data class Counters(
        override val itemId: String,
        override val hasVideo: Boolean?,
        override val incrementViewCount: Boolean,
    ) : FetchRequest(
            fetchCounters = true,
            fetchCurrentUserActions = true,
        ) {
        constructor(itemId: String) : this(
            itemId = itemId,
            hasVideo = null,
            incrementViewCount = false,
        )
    }

    /** Id of the item to fetch. */
    abstract val itemId: String

    /** Whether the fetch counts as a view. */
    abstract val incrementViewCount: Boolean

    /** Whether the item carries a video, or `null` when unknown. */
    abstract val hasVideo: Boolean?
}
