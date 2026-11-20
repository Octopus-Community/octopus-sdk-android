package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.GrpcModelProto
import com.octopuscommunity.sdk.data.local.entity.StatusEntity
import com.octopuscommunity.sdk.domain.model.Status

class StatusMapper(private val moderationMapper: ModerationMapper) {
    fun StatusEntity.toDomain(): Status = when (value) {
        StatusEntity.Value.UNKNOWN -> Status.Unknown
        StatusEntity.Value.PUBLISHED -> Status.Published
        StatusEntity.Value.MODERATED -> Status.Moderated(
            reasons = with(moderationMapper) { reasons.mapNotNull { it.toDomain() } }
        )
    }

    fun Status.toEntity(): StatusEntity = StatusEntity(
        value = when (this) {
            Status.Unknown -> StatusEntity.Value.UNKNOWN
            Status.Published -> StatusEntity.Value.PUBLISHED
            is Status.Moderated -> StatusEntity.Value.MODERATED
        },
        reasons = when (this) {
            Status.Unknown,
            Status.Published -> emptyList()

            is Status.Moderated -> with(moderationMapper) { reasons.map { it.toEntity() } }
        }
    )

    fun GrpcModelProto.OctoObject.Status.toEntity() = StatusEntity(
        value = when (value) {
            GrpcModelProto.StatusValue.UNKNOWN -> StatusEntity.Value.UNKNOWN
            GrpcModelProto.StatusValue.PUBLISHED -> StatusEntity.Value.PUBLISHED
            GrpcModelProto.StatusValue.MODERATED -> StatusEntity.Value.MODERATED
            else -> StatusEntity.Value.UNKNOWN
        },
        reasons = with(moderationMapper) { reasonsList.map { it.toEntity() } }
    )
}