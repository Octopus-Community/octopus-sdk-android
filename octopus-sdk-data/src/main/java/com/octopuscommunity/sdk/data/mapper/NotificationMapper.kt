package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.GrpcModelProto
import com.octopuscommunity.GrpcModelProto.Notification.Action.ContentEntity
import com.octopuscommunity.actionOrNull
import com.octopuscommunity.profileOrNull
import com.octopuscommunity.sdk.data.local.entity.NotificationEntity
import com.octopuscommunity.sdk.data.local.entity.NotificationEntity.ActionEntity
import com.octopuscommunity.sdk.domain.model.Notification
import com.octopuscommunity.sdk.domain.model.Notification.Action
import java.util.Date

class NotificationMapper(
    private val profileMapper: ProfileMapper,
) : OctopusMapper {
    fun NotificationEntity.toDomain() = Notification(
        id = id,
        updateDate = Date(updatedAt),
        authors = with(profileMapper) { authors.map { it.toDomain() } },
        text = text,
        action = when (action?.itemType) {
            ActionEntity.ItemType.POST -> Action.Post(itemId = action.itemId)
            ActionEntity.ItemType.COMMENT -> Action.Comment(itemId = action.itemId)
            ActionEntity.ItemType.REPLY -> action.parentId?.let { parentId ->
                Action.Reply(
                    itemId = action.itemId,
                    parentId = parentId
                )
            }

            else -> null
        },
        isRead = isRead
    )

    fun GrpcModelProto.Notification.toEntity() = NotificationEntity(
        id = id,
        updatedAt = updatedAt,
        authors = with(profileMapper) {
            notificationThumbnailsList.mapNotNull {
                it.profileOrNull?.toEntity()
            }
        },
        text = when (textCase) {
            GrpcModelProto.Notification.TextCase.MD -> md
            GrpcModelProto.Notification.TextCase.RAW -> raw
            GrpcModelProto.Notification.TextCase.TEXT_NOT_SET, null -> ""
        },
        action = actionOrNull?.linkList?.let { links ->
            val link = links.lastOrNull()
            val parentLink = if (links.size >= 2) links[links.size - 2] else null
            when (link?.content) {
                ContentEntity.POST -> ActionEntity(
                    itemType = ActionEntity.ItemType.POST,
                    itemId = link.octoObjectId,
                    parentId = parentLink?.octoObjectId
                )

                ContentEntity.COMMENT -> ActionEntity(
                    itemType = ActionEntity.ItemType.COMMENT,
                    itemId = link.octoObjectId,
                    parentId = parentLink?.octoObjectId
                )

                ContentEntity.REPLY -> ActionEntity(
                    itemType = ActionEntity.ItemType.REPLY,
                    itemId = link.octoObjectId,
                    parentId = parentLink?.octoObjectId
                )

                ContentEntity.UNDEFINED,
                ContentEntity.UNRECOGNIZED,
                null -> null
            }
        },
        isRead = read
    )
}