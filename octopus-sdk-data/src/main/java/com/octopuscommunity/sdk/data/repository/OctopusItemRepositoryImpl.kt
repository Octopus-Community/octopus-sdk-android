package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.FeedServiceProto.FeedItemInfo
import com.octopuscommunity.OctoObjectServiceProto.GetResponse
import com.octopuscommunity.reactionOrNull
import com.octopuscommunity.sdk.data.local.dao.FeedDao
import com.octopuscommunity.sdk.data.local.dao.ProfileDao
import com.octopuscommunity.sdk.data.local.dao.UserConfigDao
import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.data.local.datastore.SecuredDataStore
import com.octopuscommunity.sdk.data.local.entity.OctopusEntity
import com.octopuscommunity.sdk.data.mapper.FeedMapper
import com.octopuscommunity.sdk.data.mapper.ProfileMapper
import com.octopuscommunity.sdk.data.mapper.ReactionMapper
import com.octopuscommunity.sdk.data.mapper.UserConfigMapper
import com.octopuscommunity.sdk.data.mapper.UserMapper
import com.octopuscommunity.sdk.data.model.AnchoredFeed
import com.octopuscommunity.sdk.data.remote.service.FeedService
import com.octopuscommunity.sdk.data.remote.service.NotificationService
import com.octopuscommunity.sdk.data.remote.service.ObjectService
import com.octopuscommunity.sdk.data.remote.service.UserService
import com.octopuscommunity.sdk.data.utils.call
import com.octopuscommunity.sdk.domain.model.Feed
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.FetchRequest
import com.octopuscommunity.sdk.domain.repository.OctopusItemRepository
import com.octopuscommunity.sdk.domain.repository.SetReactionError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest

abstract class OctopusItemRepositoryImpl<
    Item : OctopusItem,
    Draft : OctopusItem.Draft,
    Error : OctopusItem.CreationError,
> internal constructor(
    securedDataStore: SecuredDataStore,
    preferencesDataStore: PreferencesDataStore,
    userConfigDao: UserConfigDao,
    profileDao: ProfileDao,
    private val feedDao: FeedDao,
    userService: UserService,
    notificationService: NotificationService,
    private val feedService: FeedService,
    val objectService: ObjectService,
    userMapper: UserMapper,
    userConfigMapper: UserConfigMapper,
    profileMapper: ProfileMapper,
    private val feedMapper: FeedMapper,
    private val reactionMapper: ReactionMapper,
) : ProfileRepositoryImpl(
        securedDataStore,
        preferencesDataStore,
        userConfigDao,
        profileDao,
        userService,
        notificationService,
        userMapper,
        userConfigMapper,
        profileMapper,
    ),
    OctopusItemRepository<Item, Draft, Error>,
    WindowFeedRepository<Item> {
    @OptIn(ExperimentalCoroutinesApi::class)
    final override fun getLocalItemsFlow(feedId: String): Flow<List<Item>> =
        with(feedMapper) {
            feedDao.getFeedItems(feedId).flatMapLatest { feedItems ->
                getLocalItems(feedItems.map { it.toDomain() })
            }
        }

    final override suspend fun fetchItem(
        itemId: String,
        hasVideo: Boolean?,
        incrementViewCount: Boolean,
    ): OctopusResult<Item?, Nothing> =
        call(feedMapper) {
            upsertLocalItem(
                objectService.get(
                    FetchRequest
                        .Full(
                            itemId = itemId,
                            hasVideo = hasVideo,
                            incrementViewCount = incrementViewCount,
                        ).toProto(),
                ),
            )
        }.also {
            if (it is OctopusResult.Failure.ContentUnavailable) {
                deleteLocalItem(itemId)
            }
        }

    final override suspend fun fetchItems(
        feedId: String,
        pageSize: Int?,
        pageCursor: String?,
    ): OctopusResult<Feed<Item>, Nothing> =
        fetchItems(feedId = feedId, storageFeedId = feedId, pageSize = pageSize, pageCursor = pageCursor)

    final override suspend fun fetchItems(
        feedId: String,
        storageFeedId: String,
        pageSize: Int?,
        pageCursor: String?,
    ): OctopusResult<Feed<Item>, Nothing> =
        call(feedMapper) {
            // Fetch feed page
            val response =
                when (pageCursor) {
                    null -> feedService.initializeFeed(feedId, pageSize)
                    else -> feedService.getFeedPage(pageCursor, pageSize)
                }

            val stored =
                when (pageCursor) {
                    null -> storeFeedPage(storageFeedId, response.itemsList, replace = true) { index -> index }
                    else -> {
                        val startPosition = feedDao.getMaxPosition(storageFeedId)?.plus(1) ?: 0
                        storeFeedPage(storageFeedId, response.itemsList, replace = false) { index -> startPosition + index }
                    }
                }

            if (stored is OctopusResult.Failure) return stored
            return@call Feed(
                id = storageFeedId,
                items = getLocalItemsFlow(storageFeedId),
                nextPageCursor = response.nextPageCursor.ifEmpty { null },
            )
        }

    final override suspend fun fetchItemsAround(
        feedId: String,
        anchorItemId: String,
        pageSize: Int?,
        storageFeedId: String,
    ): OctopusResult<AnchoredFeed<Item>, Nothing> =
        call(feedMapper) {
            val response = feedService.initializeFeed(feedId, pageSize, anchorItemId = anchorItemId)
            val stored = storeFeedPage(storageFeedId, response.itemsList, replace = true) { index -> index }
            if (stored is OctopusResult.Failure) return stored

            // A server that cannot open the feed at the item, or does not know the field, returns
            // the first page instead, without error: the item missing from the page is the signal.
            val isAnchored = response.itemsList.any { it.octoObjectId == anchorItemId }
            return@call AnchoredFeed(
                feed =
                    Feed(
                        id = storageFeedId,
                        items = getLocalItemsFlow(storageFeedId),
                        nextPageCursor = response.nextPageCursor.ifEmpty { null },
                    ),
                previousPageCursor = response.previousPageCursor.takeIf { isAnchored }?.ifEmpty { null },
                isAnchored = isAnchored,
            )
        }

    final override suspend fun fetchPreviousItems(
        feedId: String,
        pageCursor: String,
        pageSize: Int?,
        storageFeedId: String,
    ): OctopusResult<Feed<Item>, Nothing> =
        call(feedMapper) {
            val response = feedService.getFeedPage(pageCursor, pageSize)

            // Closest first: each item goes right before the first one cached, so the feed keeps
            // its order as it grows toward its start (positions below 0 included).
            val firstPosition = feedDao.getMinPosition(storageFeedId) ?: 0
            val stored = storeFeedPage(storageFeedId, response.itemsList, replace = false) { index -> firstPosition - 1 - index }
            if (stored is OctopusResult.Failure) return stored

            return@call Feed(
                id = storageFeedId,
                items = getLocalItemsFlow(storageFeedId),
                nextPageCursor = response.nextPageCursor.ifEmpty { null },
            )
        }

    /**
     * Stores a page of the feed [feedId] at the positions [positionOf] gives each item index, after
     * fetching the items that changed since they were cached, and the counters of the others.
     * [replace] drops the rest of the cached feed, as opening it on a new first page does.
     */
    private suspend fun FeedMapper.storeFeedPage(
        feedId: String,
        items: List<FeedItemInfo>,
        replace: Boolean,
        positionOf: (index: Int) -> Int,
    ): OctopusResult<Unit, Nothing> {
        // Create and store feed items
        val feedItemEntities =
            items.mapIndexed { index, item ->
                item.toEntity(feedId, positionOf(index))
            }
        val feedItems = feedItemEntities.map { it.toDomain() }

        val localItems = getLocalItems(feedItems).first().associateBy { it.id }
        val localeLanguage = preferencesDataStore.getLocaleIdentifier()

        val updatedItems =
            feedItems.filter { feedItem ->
                val localItem = localItems[feedItem.itemId]
                when {
                    localItem == null -> true
                    feedItem.updateDate.after(localItem.updateDate) -> true
                    localItem.localeLanguage != localeLanguage -> true
                    else -> false
                }
            }

        val fetched =
            fetchItems(
                requests =
                    feedItems.map { feedItem ->
                        when (feedItem) {
                            in updatedItems -> FetchRequest.Full(itemId = feedItem.itemId)
                            else -> FetchRequest.Counters(itemId = feedItem.itemId)
                        }
                    },
                childRequests =
                    feedItems.mapNotNull { feedItem ->
                        feedItem.featuredChildId?.let { childId ->
                            FetchRequest.Full(itemId = childId)
                        }
                    },
            )

        if (fetched is OctopusResult.Failure) return fetched
        when (replace) {
            true -> feedDao.replaceFeedItems(feedId, feedItemEntities)
            false -> feedDao.upsertFeedItems(feedItemEntities)
        }
        return OctopusResult.Success(Unit)
    }

    final override suspend fun fetchItems(
        requests: List<FetchRequest>,
        childRequests: List<FetchRequest>,
    ): OctopusResult<Unit, Nothing> =
        call(feedMapper) {
            if (requests.isEmpty() && childRequests.isEmpty()) return@call
            val fullRequestsIds = requests.filterIsInstance<FetchRequest.Full>().map { it.itemId }.toSet()
            val childFullRequestsIds = childRequests.filterIsInstance<FetchRequest.Full>().map { it.itemId }.toSet()
            val countersRequestsIds = requests.filterIsInstance<FetchRequest.Counters>().map { it.itemId }.toSet()
            val childCountersRequestsIds = childRequests.filterIsInstance<FetchRequest.Counters>().map { it.itemId }.toSet()
            // Keep RPC messages bounded, but publish the refresh only after every batch succeeds.
            // Writing each batch separately invalidates Room's whole visited feed (up to 8,000
            // comments) 80 times. Accumulating preserves full-profile refresh semantics and also
            // leaves the cache untouched if a later batch fails.
            val snapshots = mutableMapOf<String, OctopusEntity?>()
            val responses =
                buildList {
                    for (batch in (requests + childRequests).chunked(100)) {
                        // Snapshot before the RPC to also protect writes made while it is in flight.
                        // Repeated ids keep their earliest snapshot, since an earlier response may
                        // still be present in the consolidated write.
                        for ((id, snapshot) in feedDao.getItemSnapshots(batch.map { it.itemId })) {
                            if (id !in snapshots) snapshots[id] = snapshot
                        }
                        addAll(objectService.getBatch(batch.map { it.toProto() }))
                    }
                }
            feedDao.storeUnchangedItems(snapshots) { unchangedIds ->
                val unchanged = responses.filter { it.octoObjectId in unchangedIds }
                upsertLocalItems(
                    items = unchanged.filter { it.octoObjectId in fullRequestsIds },
                    childItems = unchanged.filter { it.octoObjectId in childFullRequestsIds },
                )
                updateLocalCounters(
                    counters = unchanged.filter { it.octoObjectId in countersRequestsIds },
                    childCounters = unchanged.filter { it.octoObjectId in childCountersRequestsIds },
                )
            }
        }

    final override suspend fun fetchCounters(
        itemId: String,
        hasVideo: Boolean?,
        incrementViewCount: Boolean,
    ): OctopusResult<Unit, Nothing> =
        call(feedMapper) {
            updateLocalCounters(
                counters =
                    objectService.get(
                        FetchRequest
                            .Counters(
                                itemId = itemId,
                                hasVideo = hasVideo,
                                incrementViewCount = incrementViewCount,
                            ).toProto(),
                    ),
            )
        }

    final override suspend fun fetchCounters(
        itemIds: List<String>,
        childIds: List<String>,
        videoItemIds: List<String>?,
        incrementViewCount: Boolean,
    ): OctopusResult<Unit, Nothing> =
        fetchItems(
            requests =
                itemIds.map { itemId ->
                    FetchRequest.Counters(
                        itemId = itemId,
                        hasVideo = videoItemIds?.contains(itemId),
                        incrementViewCount = incrementViewCount,
                    )
                },
            childRequests =
                childIds.map { itemId ->
                    FetchRequest.Counters(
                        itemId = itemId,
                        hasVideo = videoItemIds?.contains(itemId),
                        incrementViewCount = incrementViewCount,
                    )
                },
        )

    final override suspend fun setReaction(
        item: Item,
        reactionKind: Reaction.Kind?,
        isContentTranslated: Boolean?,
    ): OctopusResult<Reaction?, SetReactionError> {
        val oldReaction = item.userReaction

        return call(reactionMapper) {
            // Update local database with optimistic state
            updateLocalReaction(
                item = item,
                reaction = reactionKind?.let { Reaction(kind = reactionKind) },
            )

            if (reactionKind != null) {
                val response =
                    objectService.putReaction(
                        parentId = item.id,
                        unicode = reactionKind.unicode,
                        isContentTranslated = isContentTranslated,
                    )
                when {
                    response.hasSuccess() -> response.success?.reactionOrNull?.toDomain()
                    else -> return OctopusResult.Failure.InvalidArguments(
                        errors = response.fail.errorsList.map { it.toDomain() },
                    )
                }
            } else {
                item.userReaction?.id?.takeIf { it.isNotEmpty() }?.let { reactionId ->
                    objectService.deleteReaction(reactionId = reactionId)
                }
                null
            }
        }.also { result ->
            when (result) {
                is OctopusResult.Success -> updateLocalReaction(item = item, reaction = result.data)
                is OctopusResult.Failure -> updateLocalReaction(item = item, reaction = oldReaction)
            }
        }
    }

    private suspend fun updateLocalReaction(
        item: Item,
        reaction: Reaction?,
    ) {
        updateLocalReaction(
            item = item,
            reaction = reaction,
            reactions =
                item.reactions
                    .associateBy { it.reactionKind }
                    .toMutableMap()
                    .apply {
                        item.userReaction?.kind?.let { oldReaction ->
                            this[oldReaction]?.let { existing ->
                                val newCount = existing.count - 1
                                if (newCount > 0) {
                                    this[oldReaction] = existing.copy(count = newCount)
                                } else {
                                    remove(oldReaction)
                                }
                            }
                        }
                        reaction?.let { new ->
                            this[new.kind] = this[new.kind]?.let {
                                it.copy(count = it.count + 1)
                            } ?: ReactionCount(reactionKind = new.kind, count = 1)
                        }
                    }.values
                    .toList()
                    .sortedByDescending { it.count },
        )
    }

    final override suspend fun deleteLocalFeed(feedId: String) {
        feedDao.deleteFeedItems(feedId)
    }

    final override suspend fun deleteLocalFeeds(
        idMarker: String,
        keptIdMarker: String,
    ) {
        // Every feed id contains the empty string: an empty marker would delete the whole cache.
        if (idMarker.isEmpty()) return
        feedDao.deleteFeedItemsMarked(idMarker = idMarker, keptIdMarker = keptIdMarker)
    }

    abstract suspend fun getLocalItems(feedItems: List<Feed.Item>): Flow<List<Item>>

    abstract suspend fun upsertLocalItem(item: GetResponse): Item

    abstract suspend fun upsertLocalItems(
        items: List<GetResponse>,
        childItems: List<GetResponse>,
    )

    abstract suspend fun updateLocalCounters(counters: GetResponse)

    abstract suspend fun updateLocalCounters(
        counters: List<GetResponse>,
        childCounters: List<GetResponse> = emptyList(),
    )

    abstract suspend fun updateLocalReaction(
        item: Item,
        reaction: Reaction?,
        reactions: List<ReactionCount>,
    )

    abstract suspend fun deleteLocalItem(itemId: String)

    abstract override suspend fun deleteLocalOrphanedItems()

    abstract override suspend fun resetCache()
}
