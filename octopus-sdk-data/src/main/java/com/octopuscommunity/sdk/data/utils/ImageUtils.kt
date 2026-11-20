package com.octopuscommunity.sdk.data.utils

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import java.io.InputStream

class ImageRequestBuilder(
    private val context: Context,
) {
    /** Opens the bytes behind [uri] as they are, or `null` when its provider has none. */
    internal fun openInputStream(uri: Uri): InputStream? = context.contentResolver.openInputStream(uri)

    /** The type its provider declares for [uri], or `null`. */
    internal fun mimeType(uri: Uri): String? = context.contentResolver.getType(uri)

    fun create(
        data: Any?,
        enableCache: Boolean = true,
        crossFade: Boolean = false,
    ): ImageRequest.Builder =
        ImageRequest
            .Builder(context)
            .apply {
                if (enableCache) {
                    (data as? String)?.let { url ->
                        val cacheKey = url.getImageCacheKey()
                        memoryCacheKey(cacheKey)
                        diskCacheKey(cacheKey)
                    }
                } else {
                    diskCachePolicy(CachePolicy.DISABLED)
                    memoryCachePolicy(CachePolicy.DISABLED)
                }

                data(data)
                if (crossFade) crossfade(250)
            }
}

fun String.getImageCacheKey() =
    runCatching {
        // Url contains parameter that can change over the time (token that expire) in order to have an
        // identifier for the cache, remove those params
        toUri()
            .buildUpon()
            .query("") // Remove query
            .toString()
    }.getOrElse { this }
