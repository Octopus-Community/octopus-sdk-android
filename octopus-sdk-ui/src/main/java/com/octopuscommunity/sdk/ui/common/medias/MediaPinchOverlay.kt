package com.octopuscommunity.sdk.ui.common.medias

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.View
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Canvas as ComposeCanvas

/** Keep the layout handle during ordinary scrolling; convert it only while zooming. */
internal class MediaPinchSource {
    var coordinates: LayoutCoordinates? = null
}

/**
 * Decor-view overlay: outside every card, pager, lazy list and app bar. No extra window or
 * Compose semantics tree, no bitmap copy, and no interception of the in-flight touch stream.
 */
internal class MediaPinchOverlay(
    private val root: View,
    private val layer: GraphicsLayer,
    private val density: Density,
    private val direction: LayoutDirection,
    private val state: MediaPinchState,
    private val source: MediaPinchSource,
) : Drawable() {
    private val rootLocation = IntArray(2)
    private val drawScope = CanvasDrawScope()
    private val observer = SnapshotStateObserver { it() }
    private val onStateChanged: (MediaPinchOverlay) -> Unit = { it.invalidateSelf() }
    private var nativeCanvas: Canvas? = null
    private var composeCanvas: androidx.compose.ui.graphics.Canvas? = null

    fun startObserving() = observer.start()

    fun stopObserving() {
        observer.stop()
        observer.clear()
    }

    override fun draw(canvas: Canvas) {
        observer.observeReads(this, onStateChanged) {
            if (!state.isActive) return@observeReads
            val coordinates = source.coordinates?.takeIf { it.isAttached } ?: return@observeReads
            // Resolve from the retained handle even when density/direction recreates this
            // drawable without a new layout callback. Unknown bounds never mean window (0, 0).
            val positionInWindow = coordinates.localToWindow(Offset.Zero)
            root.getLocationInWindow(rootLocation)
            if (nativeCanvas !== canvas) {
                nativeCanvas = canvas
                composeCanvas = ComposeCanvas(canvas)
            }
            drawScope.draw(
                density,
                direction,
                requireNotNull(composeCanvas),
                Size(root.width.toFloat(), root.height.toFloat()),
            ) {
                drawRect(Color.Black, alpha = state.dimAlpha)
                canvas.save()
                canvas.translate(
                    positionInWindow.x - rootLocation[0] + state.offset.x,
                    positionInWindow.y - rootLocation[1] + state.offset.y,
                )
                canvas.scale(state.scale, state.scale)
                drawLayer(layer)
                canvas.restore()
            }
        }
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
