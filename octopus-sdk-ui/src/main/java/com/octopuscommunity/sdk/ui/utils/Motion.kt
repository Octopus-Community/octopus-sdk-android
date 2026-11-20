package com.octopuscommunity.sdk.ui.utils

import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException

/**
 * Motion durations shared by the SDK screens, aligned with the iOS SDK so both platforms move at
 * the same pace: [SHORT_DURATION_MS] for small in-place changes (a text swap, a legal notice
 * showing up), [MEDIUM_DURATION_MS] for elements entering or leaving from a screen edge.
 */
internal object OctopusMotion {
    const val SHORT_DURATION_MS = 200
    const val MEDIUM_DURATION_MS = 300

    /**
     * Animate the full distance only up to this many items. For longer scrolls, jump near the
     * target first, then animate the last screen of items (also capped at this limit).
     */
    const val MAX_ANIMATED_SCROLL_ITEMS = 10
}

/**
 * A short [LazyListState.animateScrollToItem] that returns normally when the user interrupts it.
 *
 * Targets within [OctopusMotion.MAX_ANIMATED_SCROLL_ITEMS] animate the full distance. Farther
 * targets are approached with an instant jump first, leaving at most one screen of items
 * (capped at that same limit) to animate, rather than composing the whole intervening feed.
 *
 * A user drag cancels the running scroll mutation with a [CancellationException] while the
 * calling coroutine stays active: swallowing it there lets the caller still run its follow-up
 * (resetting a one-shot scroll request). When the caller itself is cancelled (an effect key
 * changed, the screen left), the exception is rethrown so cancellation stays cooperative.
 */
internal suspend fun LazyListState.animateScrollToItemOrInterrupt(index: Int) {
    try {
        scrollAnimationStartIndex(
            currentIndex = firstVisibleItemIndex,
            targetIndex = index,
            visibleItemCount = layoutInfo.visibleItemsInfo.size,
        )?.let { startIndex -> scrollToItem(index = startIndex) }
        animateScrollToItem(index = index)
    } catch (e: CancellationException) {
        if (!currentCoroutineContext().isActive) throw e
    }
}

/** Returns a near-target jump position, or null when the full distance is short enough to animate. */
internal fun scrollAnimationStartIndex(
    currentIndex: Int,
    targetIndex: Int,
    visibleItemCount: Int,
): Int? {
    val finalGlideItems = visibleItemCount.coerceIn(1, OctopusMotion.MAX_ANIMATED_SCROLL_ITEMS)
    val distance = targetIndex.toLong() - currentIndex
    return when {
        distance > OctopusMotion.MAX_ANIMATED_SCROLL_ITEMS -> targetIndex - finalGlideItems
        distance < -OctopusMotion.MAX_ANIMATED_SCROLL_ITEMS -> targetIndex + finalGlideItems
        else -> null
    }
}
