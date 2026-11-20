package com.octopuscommunity.sdk.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "feed_items",
    primaryKeys = ["feedId", "itemId"],
    indices = [Index(value = ["feedId", "position"])],
)
data class FeedItemEntity(
    val feedId: String,
    val itemId: String,
    val updateTimestamp: Long,
    val position: Int,
    val featuredChildId: String?,
)
