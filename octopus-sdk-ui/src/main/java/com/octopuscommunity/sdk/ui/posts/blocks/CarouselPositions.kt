package com.octopuscommunity.sdk.ui.posts.blocks

import android.util.Size
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.octopuscommunity.sdk.ui.components.OctopusImageDefaults
import kotlin.math.max

/**
 * The page each post's carousel was left on, one for all the screens of an SDK host (scoped to its
 * navigation host, and dropped with it), kept above the lazy lists: a card scrolled away and back, or
 * recomposed, shows the same photo, the full-screen viewer stays in step with the card, and a post
 * shows the same photo in the feed and in its detail.
 */
@Stable
internal class CarouselPositions {
    private val indexes = mutableStateMapOf<String, Int>()

    /** The page of [postId]'s carousel, `0` until it moves. */
    fun index(postId: String): Int = indexes[postId] ?: 0

    /** The page recorded for [postId]'s carousel, or `null` when none was. */
    fun indexOrNull(postId: String): Int? = indexes[postId]

    /** Records that [postId]'s carousel shows page [index]. */
    fun set(
        postId: String,
        index: Int,
    ) {
        indexes[postId] = index
    }
}

/**
 * The carousel positions of the SDK screens, provided with their image viewer; `null` outside one,
 * where a card keeps its own.
 */
internal val LocalCarouselPositions = staticCompositionLocalOf<CarouselPositions?> { null }

/**
 * Aspect ratio (width / height) of a carousel zone: the ratio of its first image, capped at
 * [OctopusImageDefaults.ITEM_MAX_ASPECT_RATIO] like a single image, so the zone never changes height
 * while swiping. A first image whose size is unknown or empty gets a square zone.
 */
internal fun carouselAspectRatio(firstImage: Size?): Float {
    if (firstImage == null || firstImage.width <= 0 || firstImage.height <= 0) return 1f
    return max(firstImage.width.toFloat() / firstImage.height.toFloat(), OctopusImageDefaults.ITEM_MAX_ASPECT_RATIO)
}
