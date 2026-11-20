package com.octopuscommunity.sdk.data.mapper

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.security.MessageDigest

/**
 * Computes the bridge-share fingerprint that authorises a prefilled (Bridge / Share-in-game) post
 * carrying images in a community that forbids member pictures.
 *
 * Must match the backend `PrefilledShareVerifier` **byte-for-byte**:
 * a compact JSON object of the fields below in **alphabetical key order**, empty/absent fields
 * omitted, hashed with SHA-256 and rendered as lowercase hex. The host backend signs this value
 * into the `bridge_fingerprint` claim of the client token.
 *
 * A CTA label given per language adds two keys: `ctaTextLanguage`, the code of the default language
 * (the language of `ctaText`), and `ctaTextTranslations`, an object of the other languages' code to
 * label. Both are omitted for an auto-translated label, whose fingerprint is the one it had before
 * they existed.
 *
 * ⚠️ The key order IS the wire contract — reordering silently breaks every signed share. Keep keys
 * alphabetical (ctaTargetLink, ctaText, ctaTextLanguage, ctaTextTranslations, image, text), those of
 * `ctaTextTranslations` included ([sortedByUtf8]); `BridgeShareFingerprintTest` pins this against
 * the canonical vector from the docs so a reorder fails the build.
 */
internal object BridgeShareFingerprint {
    fun compute(
        ctaTargetLink: String?,
        ctaText: String?,
        images: List<String>,
        text: String?,
        ctaTextLanguage: String? = null,
        ctaTextTranslations: Map<String, String> = emptyMap(),
    ): String {
        val json =
            buildJsonObject {
                ctaTargetLink?.takeIf { it.isNotEmpty() }?.let { put("ctaTargetLink", it) }
                ctaText?.takeIf { it.isNotEmpty() }?.let { put("ctaText", it) }
                ctaTextLanguage?.takeIf { it.isNotEmpty() }?.let { language ->
                    put("ctaTextLanguage", language)
                    (ctaTextTranslations - language).takeIf { it.isNotEmpty() }?.let { translations ->
                        putJsonObject("ctaTextTranslations") {
                            translations.sortedByUtf8().forEach { (code, label) -> put(code, label) }
                        }
                    }
                }
                images
                    .filter { it.isNotEmpty() }
                    .takeIf { it.isNotEmpty() }
                    ?.let { put("image", it.joinToString(IMAGE_SEPARATOR)) }
                text?.takeIf { it.isNotEmpty() }?.let { put("text", it) }
            }.toString()
        return MessageDigest
            .getInstance("SHA-256")
            .digest(json.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * Joins the values of several images under the single `image` key, in display order. One image
     * gives its value alone, so a single-image share hashes exactly as before multi-image posts.
     */
    const val IMAGE_SEPARATOR = ", "
}

/**
 * The entries of this map in the order a signed content lists them: by ascending bytes of the UTF-8
 * form of their key, which is the ascending order of its Unicode code points (`en` < `en-US` <
 * `es-419` < `fr`). Not the order of `String.compareTo`, which compares UTF-16 units and so places
 * a character outside the Basic Multilingual Plane before U+E000.
 */
internal fun Map<String, String>.sortedByUtf8(): List<Map.Entry<String, String>> =
    entries.sortedWith { first, second -> compareUtf8(first.key.toByteArray(), second.key.toByteArray()) }

private fun compareUtf8(
    first: ByteArray,
    second: ByteArray,
): Int {
    for (index in 0 until minOf(first.size, second.size)) {
        val difference = (first[index].toInt() and 0xFF) - (second[index].toInt() and 0xFF)
        if (difference != 0) return difference
    }
    return first.size - second.size
}
