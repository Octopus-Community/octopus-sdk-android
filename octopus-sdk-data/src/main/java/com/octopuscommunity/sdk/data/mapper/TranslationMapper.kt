package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.sdk.data.local.entity.TranslatableTextEntity
import com.octopuscommunity.sdk.domain.model.TranslatableText

class TranslationMapper {
    fun TranslatableTextEntity.toDomain() = TranslatableText(
        original = original.toDomain(),
        translated = translated?.toDomain()
    )

    fun TranslatableTextEntity.ContentEntity.toDomain() = TranslatableText.Content(
        // Trim leading/trailing whitespace (incl. newlines) iso iOS
        // `TranslatableText.swift:24-26` — both initializers trim originalText and translatedText.
        // Backend translations sometimes carry trailing newlines that, if not trimmed at the model
        // boundary, leak into the ellipsize predicate (`displayed != raw`) and produce a stale
        // "Read more" affordance on otherwise short content.
        text = text.trim(),
        language = language
    )

    fun TranslatableText.toEntity() = TranslatableTextEntity(
        original = original.toEntity(),
        translated = translated?.toEntity()
    )

    fun TranslatableText.Content.toEntity() = TranslatableTextEntity.ContentEntity(
        text = text,
        language = language
    )
}