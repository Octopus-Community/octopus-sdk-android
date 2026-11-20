package com.octopuscommunity.sdk.data.local.entity

import androidx.room.Embedded
import kotlinx.serialization.Serializable

@Serializable
data class MediasEntity(
    val images: List<MediaEntity.ImageEntity>,
    @Embedded(prefix = "video_") val video: MediaEntity.VideoEntity?
)