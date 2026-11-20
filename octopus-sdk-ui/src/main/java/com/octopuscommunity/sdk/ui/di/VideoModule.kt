package com.octopuscommunity.sdk.ui.di

import android.annotation.SuppressLint
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.dsl.onClose
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@get:SuppressLint("UnsafeOptInUsageError")
internal val octopusVideoModule get() =
    module {
        single<Cache> {
            SharedVideoCaches.acquire(androidContext())
        } onClose {
            it?.release()
        }

        factory<Player> {
            ExoPlayer
                .Builder(androidContext())
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(
                        CacheDataSource.Factory().setCache(get()),
                    ),
                ).setLoadControl(
                    DefaultLoadControl
                        .Builder()
                        .setBufferDurationsMs(
                            1.seconds.toInt(DurationUnit.MILLISECONDS),
                            50.seconds.toInt(DurationUnit.MILLISECONDS),
                            1.seconds.toInt(DurationUnit.MILLISECONDS),
                            1.seconds.toInt(DurationUnit.MILLISECONDS),
                        ).setPrioritizeTimeOverSizeThresholds(true)
                        .build(),
                ).build()
                .apply {
                    repeatMode = Player.REPEAT_MODE_OFF
                }
        }
    }

internal val octopusFakeVideoModule get() =
    module {
        factory<Player> {
            @OptIn(UnstableApi::class)
            object : SimpleBasePlayer(Looper.getMainLooper()) {
                override fun getState() =
                    State
                        .Builder()
                        .setPlaybackState(STATE_IDLE)
                        .build()
            }
        }
    }

/**
 * Returns this cache with a [Cache.release] that runs [closeDatabaseProvider] once the cache is
 * released.
 *
 * `SimpleCache.release()` writes its index through its database provider but never closes it, so
 * without this every SDK graph teardown (`OctopusSDK.stop()`) left one SQLite connection open. The
 * provider is closed after the release, which still needs it, and even when the release throws.
 *
 * Takes a lambda rather than the provider itself so the ordering is testable on the JVM, without an
 * Android database.
 */
@OptIn(UnstableApi::class)
internal fun Cache.closingOnRelease(closeDatabaseProvider: () -> Unit): Cache {
    val cache = this
    return object : Cache by cache {
        override fun release() {
            try {
                cache.release()
            } finally {
                closeDatabaseProvider()
            }
        }
    }
}

/** Reference counts video caches while a retired container finishes its last composed frame. */
@OptIn(UnstableApi::class)
internal object SharedVideoCaches {
    private class Entry(
        val cache: Cache,
        var users: Int = 0,
    )

    private val entries = mutableMapOf<File, Entry>()

    @Synchronized
    fun acquire(context: android.content.Context): Cache {
        val directory = File(context.cacheDir, "octopus_medias_cache").canonicalFile
        val entry =
            entries.getOrPut(directory) {
                val provider = StandaloneDatabaseProvider(context.applicationContext)
                Entry(
                    SimpleCache(directory, LeastRecentlyUsedCacheEvictor(500L * 1024L * 1024L), provider)
                        .closingOnRelease { provider.close() },
                )
            }
        entry.users++
        return object : Cache by entry.cache {
            private var released = false

            override fun release() {
                synchronized(this@SharedVideoCaches) {
                    if (released) return
                    released = true
                    if (--entry.users == 0) {
                        entries.remove(directory)
                        entry.cache.release()
                    }
                }
            }
        }
    }
}
