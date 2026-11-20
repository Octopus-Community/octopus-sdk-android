package com.octopuscommunity.sdk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "root_feeds")
data class RootFeedEntity(
    @PrimaryKey val id: String,
    val label: String,
    val relatedTopicId: String?,
    val position: Int
)