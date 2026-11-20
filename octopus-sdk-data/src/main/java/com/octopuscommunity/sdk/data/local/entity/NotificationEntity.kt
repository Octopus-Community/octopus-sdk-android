package com.octopuscommunity.sdk.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val updatedAt: Long,
    val authors: List<MinimalProfileEntity>,
    val text: String,
    @Embedded(prefix = "action_") val action: ActionEntity?,
    val isRead: Boolean
) {
    data class ActionEntity(
        val itemType: ItemType,
        val itemId: String,
        val parentId: String?
    ) {
        enum class ItemType {
            POST, COMMENT, REPLY
        }
    }
}