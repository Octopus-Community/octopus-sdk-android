package com.octopuscommunity.sdk.ui.common.medias

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

// Single internal kill switch for in-place pinch on content photos and videos.
internal const val MediaPinchEnabled = true

private const val MaxScale = 4f
private const val MaxDimAlpha = 0.6f

/** Transform relative to the media's top-left, in untransformed local pixels. */
@Stable
internal class MediaPinchState {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    var isActive by mutableStateOf(false)
        private set

    val dimAlpha: Float get() = ((scale - 1f) / (MaxScale - 1f)).coerceIn(0f, 1f) * MaxDimAlpha

    fun begin() {
        isActive = true
    }

    fun transform(
        centroid: Offset,
        pan: Offset,
        zoom: Float,
    ) {
        val nextScale = (scale * zoom).coerceIn(1f, MaxScale)
        // Keep the image point under the previous centroid under the new centroid. Use the
        // clamped ratio, so reaching either scale limit never makes the media jump.
        offset = centroid + pan - (centroid - offset) * (nextScale / scale)
        scale = nextScale
    }

    suspend fun release() {
        val fromScale = scale
        val fromOffset = offset
        // A critically damped spring returns without undershooting the original size.
        animate(1f, 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) {
            fraction,
            _,
            ->
            scale = 1f + (fromScale - 1f) * fraction
            offset = fromOffset * fraction
        }
        reset()
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
        isActive = false
    }
}
