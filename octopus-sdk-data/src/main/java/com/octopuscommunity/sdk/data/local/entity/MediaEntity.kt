package com.octopuscommunity.sdk.data.local.entity

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
sealed interface MediaEntity {
    val source: ResourceEntity
    val width: Int?
    val height: Int?
    val size: Long?

    @Serializable
    data class ImageEntity(
        override val source: ResourceEntity,
        override val width: Int?,
        override val height: Int?,
        override val size: Long?,
        val name: String,
        val contentType: String,
    ) : MediaEntity

    @Serializable
    data class VideoEntity(
        override val source: ResourceEntity,
        override val width: Int?,
        override val height: Int?,
        override val size: Long?,
        val id: String,
        val thumbnailUrl: String? = null,
        val duration: Long? = null
    ) : MediaEntity

    @Serializable
    data class ResourceEntity(val type: Type, val value: String) {
        @Serializable
        enum class Type { LOCAL, REMOTE }

        constructor(url: String) : this(Type.REMOTE, url)
        constructor(localUri: Uri) : this(Type.LOCAL, localUri.toString())
    }
}