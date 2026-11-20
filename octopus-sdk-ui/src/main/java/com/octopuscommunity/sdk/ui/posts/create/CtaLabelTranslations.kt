package com.octopuscommunity.sdk.ui.posts.create

import com.octopuscommunity.sdk.domain.model.OctopusLocalizedText
import com.octopuscommunity.sdk.domain.model.OctopusPostCTA
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The code of the default language of a per-language CTA label; `null` when it is auto-translated. */
internal val OctopusPostCTA.labelLanguage: String?
    get() = (localizedLabel as? OctopusLocalizedText.PerLanguage)?.defaultLanguageCode

/** The labels of a per-language CTA by language code, the default language excluded; else empty. */
internal val OctopusPostCTA.labelTranslations: Map<String, String>
    get() =
        (localizedLabel as? OctopusLocalizedText.PerLanguage)
            ?.let { it.textsByLanguageCode - it.defaultLanguageCode }
            .orEmpty()

/**
 * The CTA labels of the other languages as the string the `CreatePost` route carries, a route
 * argument being unable to hold a map: a compact JSON object of language code to label, or `null`
 * when there is none.
 */
internal fun encodeCtaLabelTranslations(translations: Map<String, String>): String? =
    translations
        .takeIf { it.isNotEmpty() }
        ?.let { JsonObject(it.mapValues { (_, label) -> JsonPrimitive(label) }).toString() }

/** The reverse of [encodeCtaLabelTranslations]; empty for `null` or a string that is not such an object. */
internal fun decodeCtaLabelTranslations(encoded: String?): Map<String, String> =
    encoded
        ?.let { runCatching { Json.parseToJsonElement(it).jsonObject.mapValues { (_, label) -> label.jsonPrimitive.content } }.getOrNull() }
        .orEmpty()
