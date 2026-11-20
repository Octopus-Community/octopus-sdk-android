package com.octopuscommunity.sdk.di

import android.os.Build.VERSION.SDK_INT
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.CachePolicy
import com.octopuscommunity.sdk.data.utils.ImageRequestBuilder
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * Koin module providing the Coil image loader of the SDK, with memory and disk caches and animated
 * image decoding.
 */
val octopusImageModule =
    module {
        // A retired generation may still be finishing its last UI frame. Share the disk cache
        // during this overlap, and release it once neither container can use it.
        single {
            val directory = androidContext().cacheDir.resolve("octopus_image_cache").toOkioPath()
            SharedImageDiskCaches.acquire(directory)
        } onClose { holder ->
            holder?.close()
        }

        single {
            val diskCache = get<ImageDiskCacheHolder>()
            ImageLoader
                .Builder(androidContext())
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .diskCache { diskCache.get() }
                .components {
                    if (SDK_INT >= 28) {
                        add(AnimatedImageDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }.build()
        } onClose { imageLoader ->
            imageLoader?.shutdown()
        }

        factory { ImageRequestBuilder(androidContext()) }
    }

/**
 * Builds the image disk cache on first use and shuts it down on [close].
 *
 * Both run under one lock, so a cache built by a request racing with [close] is still shut down,
 * and a cache that was never used is never built just to be closed. After [close], [get] returns
 * `null` and Coil carries on without a disk cache.
 */
internal class ImageDiskCacheHolder(
    private val release: ((DiskCache?) -> Unit)? = null,
    private val build: () -> DiskCache,
) {
    private var cache: DiskCache? = null
    private var closed = false

    @Synchronized
    fun get(): DiskCache? {
        if (closed) return null
        return cache ?: build().also { cache = it }
    }

    @Synchronized
    fun close() {
        closed = true
        if (release != null) release.invoke(cache) else cache?.shutdown()
        cache = null
    }
}

/** Reference counts cache directories across overlapping UI generations. */
private object SharedImageDiskCaches {
    private class Entry(
        var users: Int = 0,
        var cache: DiskCache? = null,
    )

    private val entries = mutableMapOf<okio.Path, Entry>()

    @Synchronized
    fun acquire(directory: okio.Path): ImageDiskCacheHolder {
        val entry = entries.getOrPut(directory) { Entry() }
        entry.users++
        return ImageDiskCacheHolder(release = {
            synchronized(this) {
                if (--entry.users == 0) {
                    entries.remove(directory)
                    entry.cache?.shutdown()
                }
            }
        }) {
            synchronized(this) {
                entry.cache ?: DiskCache
                    .Builder()
                    .directory(directory)
                    .build()
                    .also { entry.cache = it }
            }
        }
    }
}
