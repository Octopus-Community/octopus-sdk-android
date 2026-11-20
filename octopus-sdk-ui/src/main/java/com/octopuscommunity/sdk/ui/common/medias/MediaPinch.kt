package com.octopuscommunity.sdk.ui.common.medias

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** The original media owns the entire pointer stream; the window overlay only draws. */
@Composable
internal fun Modifier.mediaPinch(
    state: MediaPinchState,
    onPinchStarted: () -> Unit = {},
): Modifier {
    if (!MediaPinchEnabled) return this
    val source = remember { MediaPinchSource() }
    val currentOnPinchStarted by rememberUpdatedState(onPinchStarted)
    // Idle media only retain coordinates. Allocate rendering resources on demand, and
    // release them once the return animation finishes.
    val layer =
        if (state.isActive) {
            val root = LocalView.current.rootView
            val activeLayer = rememberGraphicsLayer()
            val density = LocalDensity.current
            val direction = LocalLayoutDirection.current
            val overlay =
                remember(root, activeLayer, density, direction, state, source) {
                    MediaPinchOverlay(root, activeLayer, density, direction, state, source)
                }
            DisposableEffect(overlay) {
                overlay.setBounds(0, 0, root.width, root.height)
                overlay.startObserving()
                root.overlay.add(overlay)
                onDispose {
                    root.overlay.remove(overlay)
                    overlay.stopObserving()
                }
            }
            activeLayer
        } else {
            null
        }
    DisposableEffect(state) { onDispose { state.reset() } }

    return this
        .onGloballyPositioned { source.coordinates = it }
        .drawWithContent {
            if (layer != null && state.isActive) {
                layer.record { this@drawWithContent.drawContent() }
            } else {
                drawContent()
            }
        }.pointerInput(state) {
            coroutineScope {
                var returnAnimation: Job? = null
                try {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        var claimed = false
                        var released = false
                        try {
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val fingers = event.changes.count { it.pressed }
                                if (!claimed && fingers >= 2) {
                                    returnAnimation?.cancel()
                                    state.begin()
                                    currentOnPinchStarted()
                                    claimed = true
                                }
                                if (claimed) {
                                    if (!released && fingers >= 2) {
                                        state.transform(
                                            centroid = event.calculateCentroid(useCurrent = false),
                                            pan = event.calculatePan(),
                                            zoom = event.calculateZoom(),
                                        )
                                    } else if (!released) {
                                        released = true
                                        returnAnimation = launch { state.release() }
                                    }
                                    // Consume in Initial, ahead of the pager, feed scroller and tap.
                                    // Keep consuming the last finger until up, including during return.
                                    event.changes.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                        } finally {
                            if (claimed && !released) returnAnimation = launch { state.release() }
                        }
                    }
                } finally {
                    state.reset()
                }
            }
        }
}
