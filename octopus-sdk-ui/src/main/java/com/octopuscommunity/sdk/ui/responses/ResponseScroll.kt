package com.octopuscommunity.sdk.ui.responses

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyLayoutScrollScope
import androidx.compose.foundation.lazy.LazyListState
import com.octopuscommunity.sdk.ui.common.viewmodel.ScrollTo
import com.octopuscommunity.sdk.ui.utils.OctopusMotion
import com.octopuscommunity.sdk.ui.utils.scrollAnimationStartIndex
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException

/** Resolve by identity again after paging/prepending rather than retaining a stale lazy-list index. */
internal fun responseScrollIndex(
    target: ScrollTo?,
    itemIds: List<String>,
    hasOlderItems: Boolean,
): Int? {
    val itemIndex =
        when (target) {
            is ScrollTo.Item -> itemIds.indexOf(target.id)
            ScrollTo.Latest -> itemIds.lastIndex
            null -> -1
        }
    return itemIndex.takeIf { it >= 0 }?.plus(if (hasOlderItems) 2 else 1)
}

/** Bottom alignment leaves the whole item above the composer, or its end if taller than the viewport. */
internal fun responseBottomOffset(
    itemOffset: Int,
    itemSize: Int,
    viewportEnd: Int,
    composerPadding: Int,
): Int = itemOffset + itemSize - (viewportEnd - composerPadding)

/** A bounded glide, with measured alignment after layout. A drag always wins the scroll mutation. */
internal suspend fun LazyListState.scrollResponseIntoView(
    index: Int,
    animate: Boolean,
): Boolean {
    try {
        scroll {
            val scope = LazyLayoutScrollScope(this@scrollResponseIntoView, this)
            with(scope) {
                if (animate) {
                    scrollAnimationStartIndex(firstVisibleItemIndex, index, layoutInfo.visibleItemsInfo.size)
                        ?.let { snapToItem(it) }
                    val info = layoutInfo
                    val target = info.visibleItemsInfo.firstOrNull { it.index == index }
                    val distance =
                        if (target != null) {
                            responseBottomOffset(target.offset, target.size, info.viewportEndOffset, info.afterContentPadding).toFloat()
                        } else {
                            val estimatedSize =
                                info.visibleItemsInfo
                                    .map { it.size }
                                    .average()
                                    .takeUnless { it.isNaN() } ?: 0.0
                            val bottomSpace = (info.viewportEndOffset - info.afterContentPadding - estimatedSize).coerceAtLeast(0.0)
                            (calculateDistanceTo(index) - bottomSpace).toFloat()
                        }
                    var previous = 0f
                    animate(0f, distance, animationSpec = tween(OctopusMotion.SHORT_DURATION_MS)) { value, _ ->
                        previous += scrollBy(value - previous)
                    }
                }
                // Resolves estimated distances and measures off-screen targets without a long traversal.
                if (layoutInfo.visibleItemsInfo.none { it.index == index }) snapToItem(index)
                val info = layoutInfo
                val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return@scroll
                val delta = responseBottomOffset(item.offset, item.size, info.viewportEndOffset, info.afterContentPadding)
                scrollBy(delta.toFloat())
            }
        }
        return true
    } catch (e: CancellationException) {
        if (!currentCoroutineContext().isActive) throw e
        return false
    }
}
