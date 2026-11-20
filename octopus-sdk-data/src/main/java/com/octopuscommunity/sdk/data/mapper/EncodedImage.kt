package com.octopuscommunity.sdk.data.mapper

import com.google.protobuf.ByteString

/**
 * A photo ready for upload: [bytes] typed [uploadContentType]. [isOptimized] tells the backend the
 * bytes need no further optimisation; [name] and [contentType] describe the photo in the post.
 */
internal data class EncodedImage(
    val bytes: ByteString,
    val isOptimized: Boolean,
    val name: String,
    val contentType: String,
    val uploadContentType: String,
)

/** The type of an image read from its first [bytes], or `null` for a format it does not know. */
internal fun sniffImageMimeType(bytes: ByteArray): String? {
    fun startsWith(
        offset: Int,
        vararg values: Int,
    ) = bytes.size >= offset + values.size && values.indices.all { bytes[offset + it] == values[it].toByte() }

    fun ascii(
        offset: Int,
        text: String,
    ) = startsWith(offset, *text.map { it.code }.toIntArray())
    return when {
        startsWith(0, 0xFF, 0xD8, 0xFF) -> "image/jpeg"
        startsWith(0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> "image/png"
        ascii(0, "GIF8") -> "image/gif"
        ascii(0, "RIFF") && ascii(8, "WEBP") -> "image/webp"
        ascii(4, "ftypheic") || ascii(4, "ftypheix") || ascii(4, "ftypmif1") -> "image/heic"
        else -> null
    }
}
