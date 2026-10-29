@file:OptIn(ExperimentalFoundationApi::class)

package com.octopuscommunity.sdk.sample.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch

// Horizontal scroll containers must retain the platform policy: the bar only obscures the top.
internal val LocalSampleUnobscuredBringIntoViewSpec = compositionLocalOf<BringIntoViewSpec?> { null }

/** Reissues a whole-field request on taps, including when dismissing the IME retained focus. */
internal fun Modifier.sampleVerticalScroll(state: ScrollState): Modifier =
    composed {
        val requester = remember { BringIntoViewRequester() }
        val bounds = remember { FocusedFieldBounds() }
        val scope = rememberCoroutineScope()
        verticalScroll(state)
            // Inside verticalScroll: the requester must send its rectangle through that scroll node.
            .bringIntoViewRequester(requester)
            .onGloballyPositioned { bounds.content = it }
            .onFocusedBoundsChanged { bounds.field = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                    var dragged = false
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        dragged = dragged || (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                        if (!dragged && change.changedToUpIgnoreConsumed()) {
                            val tap = change.position
                            scope.launch {
                                // Let the text field finish its focus/cursor handling before requesting its decoration box.
                                withFrameNanos { }
                                val content = bounds.content?.takeIf { it.isAttached } ?: return@launch
                                val field = bounds.field?.takeIf { it.isAttached } ?: return@launch
                                val rect = content.localBoundingBoxOf(field, clipBounds = false)
                                if (rect.contains(tap)) requester.bringIntoView(rect)
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    }

private class FocusedFieldBounds {
    var content: LayoutCoordinates? = null
    var field: LayoutCoordinates? = null
}

/**
 * Keeps focus and explicit bring-into-view requests below a bar drawn over vertical content.
 * Pair vertical content with [sampleVerticalScroll] so taps also reveal fields that retained focus.
 */
@Composable
internal fun SampleTopBarContent(
    topInset: Dp,
    scrollsUnderBar: Boolean = true,
    content: @Composable () -> Unit,
) {
    val unobscuredSpec = LocalSampleUnobscuredBringIntoViewSpec.current ?: LocalBringIntoViewSpec.current
    // OutlinedTextField requests its input box, excluding the floating label's half-line overhang.
    // Reserve that space too, including font scaling, so the whole field clears the bar.
    val labelLineHeight = MaterialTheme.typography.bodySmall.lineHeight
    val insetPx = with(LocalDensity.current) { topInset.toPx() + labelLineHeight.toPx() / 2f }
    val spec =
        remember(insetPx, scrollsUnderBar, unobscuredSpec) {
            if (scrollsUnderBar) {
                object : BringIntoViewSpec {
                    override fun calculateScrollDistance(
                        offset: Float,
                        size: Float,
                        containerSize: Float,
                    ): Float {
                        val hiddenTop = insetPx.coerceIn(0f, containerSize)
                        return unobscuredSpec.calculateScrollDistance(offset - hiddenTop, size, containerSize - hiddenTop)
                    }
                }
            } else {
                unobscuredSpec
            }
        }
    CompositionLocalProvider(
        LocalSampleTopBarInset provides topInset,
        LocalSampleUnobscuredBringIntoViewSpec provides unobscuredSpec,
        LocalBringIntoViewSpec provides spec,
        content = content,
    )
}
