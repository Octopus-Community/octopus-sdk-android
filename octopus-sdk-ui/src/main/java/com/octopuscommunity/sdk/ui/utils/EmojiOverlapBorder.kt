package com.octopuscommunity.sdk.ui.utils

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

/**
 * Draws a colored border that follows the emoji shape by rendering a tinted
 * silhouette at a slightly larger scale behind the actual content.
 * Creates a clean overlap separation effect matching the Figma design.
 */
internal fun Modifier.emojiOverlapBorder(
    borderColor: Color,
    borderScale: Float = 1.15f
): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    // 1. Draw scaled-up silhouette in border color behind content
    val paint = android.graphics.Paint().apply {
        colorFilter = PorterDuffColorFilter(
            borderColor.toArgb(),
            PorterDuff.Mode.SRC_ATOP
        )
    }
    drawContext.canvas.nativeCanvas.saveLayer(null, paint)
    scale(borderScale, pivot = center) {
        this@drawWithContent.drawContent()
    }
    drawContext.canvas.nativeCanvas.restore()

    // 2. Draw actual content on top
    drawContent()
}
