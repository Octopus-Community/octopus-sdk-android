package com.octopuscommunity.sdk.data.mapper

import kotlinx.serialization.json.JsonPrimitive
import java.security.MessageDigest

/**
 * The value of one image in a fingerprint, handed to `write` in pieces that, joined, make the value:
 * a local image's Base64 is never held as one String.
 */
internal fun interface FingerprintImageValue {
    fun write(piece: (String) -> Unit)
}

/**
 * Computes the fingerprint a host signs to create a client object (bridge) post.
 *
 * A compact JSON object of the fields below, in this insertion order, absent fields omitted, hashed
 * with SHA-256 and rendered as lowercase hex. The backend rebuilds the same JSON to check the
 * signature, so the order IS the wire contract. Several images join their values under the single
 * `image` key with [BridgeShareFingerprint.IMAGE_SEPARATOR], in order: one image hashes exactly as
 * before multi-image posts. `ClientPostFingerprintTest` pins both.
 *
 * A button text given per language adds two keys after `ctaText`: `ctaTextLanguage`, the code of the
 * default language (the language of `ctaText`), and `ctaTextTranslations`, an object of the other
 * languages' code to text, its keys in the order of [sortedByUtf8]. Both are omitted for an
 * auto-translated text, whose fingerprint is the one it had before they existed.
 */
internal object ClientPostFingerprint {
    fun compute(
        catchPhrase: String?,
        clientObjectId: String,
        ctaText: String?,
        images: List<String>,
        text: String,
        topicId: String?,
        ctaTextLanguage: String? = null,
        ctaTextTranslations: Map<String, String> = emptyMap(),
    ): String =
        computeStreamed(
            catchPhrase = catchPhrase,
            clientObjectId = clientObjectId,
            ctaText = ctaText,
            images = images.map { value -> FingerprintImageValue { piece -> piece(value) } },
            text = text,
            topicId = topicId,
            ctaTextLanguage = ctaTextLanguage,
            ctaTextTranslations = ctaTextTranslations,
        )

    /**
     * [compute], hashing the JSON as it is written instead of building it: the bytes are those of
     * `buildJsonObject { … }.toString()`, whose escaping is per character, so escaping each piece of
     * an image value on its own escapes the whole value.
     */
    fun computeStreamed(
        catchPhrase: String?,
        clientObjectId: String,
        ctaText: String?,
        images: List<FingerprintImageValue>,
        text: String,
        topicId: String?,
        ctaTextLanguage: String? = null,
        ctaTextTranslations: Map<String, String> = emptyMap(),
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val write = { part: String -> digest.update(part.toByteArray()) }
        var firstField = true

        fun field(
            key: String,
            writeValue: () -> Unit,
        ) {
            write(if (firstField) "{" else ",")
            firstField = false
            write(quoted(key))
            write(":")
            writeValue()
        }

        fun field(
            key: String,
            value: String,
        ) = field(key) { write(quoted(value)) }

        catchPhrase?.let { field("catchPhrase", it) }
        field("clientObjectId", clientObjectId)
        ctaText?.let { field("ctaText", it) }
        ctaTextLanguage?.let { language ->
            field("ctaTextLanguage", language)
            (ctaTextTranslations - language).takeIf { it.isNotEmpty() }?.let { translations ->
                field("ctaTextTranslations") {
                    translations.sortedByUtf8().forEachIndexed { index, (code, label) ->
                        write(if (index == 0) "{" else ",")
                        write(quoted(code))
                        write(":")
                        write(quoted(label))
                    }
                    write("}")
                }
            }
        }
        if (images.isNotEmpty()) {
            field("image") {
                write("\"")
                images.forEachIndexed { index, image ->
                    if (index > 0) write(escaped(BridgeShareFingerprint.IMAGE_SEPARATOR))
                    image.write { write(escaped(it)) }
                }
                write("\"")
            }
        }
        field("text", text)
        topicId?.let { field("topicId", it) }
        write("}")
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun quoted(value: String) = JsonPrimitive(value).toString()

    private fun escaped(value: String) = quoted(value).let { it.substring(1, it.length - 1) }
}
