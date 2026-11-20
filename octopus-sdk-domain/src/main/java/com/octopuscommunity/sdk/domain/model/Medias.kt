package com.octopuscommunity.sdk.domain.model

/** Medias attached to an item: its [images] and at most one [video]. */
data class Medias(
    val images: List<Media.Image> = listOf(),
    val video: Media.Video? = null,
)
