package com.octopuscommunity.sdk.data.local.entity

import androidx.room.Embedded
import kotlinx.serialization.Serializable

@Serializable
data class TranslatableTextEntity(
    @Embedded(prefix = "original_") val original: ContentEntity,
    @Embedded(prefix = "translated_") val translated: ContentEntity?
) {
    @Serializable
    data class ContentEntity(val text: String, val language: String)

    constructor(
        originalText: String,
        originalLanguage: String,
        translatedText: String?,
        translatedLanguage: String?
    ) : this(
        original = ContentEntity(
            text = originalText,
            language = originalLanguage
        ),
        translated = translatedText?.ifEmpty { null }?.let { translatedText ->
            ContentEntity(
                text = translatedText,
                language = translatedLanguage ?: ""
            )
        }
    )
}