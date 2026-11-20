package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.FeedServiceProto
import com.octopuscommunity.getRequest
import com.octopuscommunity.sdk.data.local.entity.FeedItemEntity
import com.octopuscommunity.sdk.data.local.entity.FeedsEntity
import com.octopuscommunity.sdk.data.local.entity.RootFeedEntity
import com.octopuscommunity.sdk.domain.model.Feed
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.RootFeed
import com.octopuscommunity.sdk.domain.repository.FetchRequest
import java.util.Date

class FeedMapper : OctopusMapper {
    fun FeedServiceProto.FeedInfo.toEntity(position: Int) = RootFeedEntity(
        id = id,
        label = label,
        relatedTopicId = if (hasRelatedTopicId()) relatedTopicId.ifEmpty { null } else null,
        position = position
    )

    fun RootFeedEntity.toDomain() = RootFeed(
        id = id,
        label = label,
        relatedGroupId = relatedTopicId
    )

    fun FeedServiceProto.FeedItemInfo.toEntity(feedId: String, position: Int) = FeedItemEntity(
        feedId = feedId,
        itemId = octoObjectId,
        position = position,
        updateTimestamp = octoObjectUpdatedAt,
        featuredChildId = highlightedChildId?.ifEmpty { null }
    )

    fun FeedItemEntity.toDomain() = Feed.Item(
        feedId = feedId,
        itemId = itemId,
        updateDate = Date(updateTimestamp),
        featuredChildId = featuredChildId
    )

    fun FeedsEntity.toDomain() = OctopusItem.Feeds(
        descendingId = descendingId,
        ascendingId = ascendingId
    )

    fun OctopusItem.Feeds.toEntity() = FeedsEntity(
        descendingId = descendingId,
        ascendingId = ascendingId
    )

    fun FetchRequest.toProto() = getRequest {
        octoObjectId = this@toProto.itemId
        fetchObject = this@toProto.fetchObject
        fetchAggregate = this@toProto.fetchCounters
        fetchRequesterCtx = this@toProto.fetchCurrentUserActions
        registerView = this@toProto.incrementViewCount
        this@toProto.hasVideo?.let { hasVideo = it }
    }
}