package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.GrpcModelProto
import com.octopuscommunity.sdk.data.local.entity.SectionEntity
import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sectionOrNull

class SectionMapper : OctopusMapper {
    fun GrpcModelProto.OctoObject.toEntity() = SectionEntity(
        id = id,
        label = content.sectionOrNull?.label ?: ""
    )

    fun SectionEntity.toDomain() = OctopusGroup.Section(
        id = id,
        label = label
    )
}