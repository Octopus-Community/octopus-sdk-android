package com.octopuscommunity.sdk.ui.common.medias.viewer

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.lerp
import kotlin.math.max
import kotlin.math.min

/** Smallest rectangle containing every point of [points], which must not be empty. */
internal fun boundingRect(points: List<Offset>): Rect =
    Rect(
        left = points.minOf { it.x },
        top = points.minOf { it.y },
        right = points.maxOf { it.x },
        bottom = points.maxOf { it.y },
    )

/** Where an image of [intrinsic] size lands in [container] with `ContentScale.Fit`, centered. */
internal fun fitRect(
    container: Rect,
    intrinsic: Size,
): Rect {
    val scale = min(container.width / intrinsic.width, container.height / intrinsic.height)
    return centeredRect(container.center, intrinsic.width * scale, intrinsic.height * scale)
}

/**
 * Where an image of [intrinsic] size is drawn for [bounds] with `ContentScale.Crop`, centered: it
 * covers [bounds] and overflows it on one axis.
 */
internal fun cropRect(
    bounds: Rect,
    intrinsic: Size,
): Rect {
    val scale = max(bounds.width / intrinsic.width, bounds.height / intrinsic.height)
    return centeredRect(bounds.center, intrinsic.width * scale, intrinsic.height * scale)
}

/**
 * [rect] scaled by [scale] about its own center — what a `graphicsLayer` scale with the default
 * centered pivot does to a layer filling [rect].
 */
internal fun scaleAboutCenter(
    rect: Rect,
    scale: Float,
): Rect = centeredRect(rect.center, rect.width * scale, rect.height * scale)

/**
 * The container the photo is fitted in when its flight home starts: [container] shrunk by [scale]
 * as a back gesture leaves it, and moved by [offset] as a flick leaves it, so the photo flies from
 * where the reader let it go.
 */
internal fun flightStartContainer(
    container: Rect,
    scale: Float,
    offset: Offset,
): Rect = scaleAboutCenter(container, scale).translate(offset)

private fun centeredRect(
    center: Offset,
    width: Float,
    height: Float,
): Rect =
    Rect(
        left = center.x - width / 2f,
        top = center.y - height / 2f,
        right = center.x + width / 2f,
        bottom = center.y + height / 2f,
    )

/**
 * One frame of the hero transition: the rectangle the image is drawn into, the clip applied to
 * it, and the clip's corner radius as a percentage of its shorter side (50 = circle).
 */
@Immutable
internal data class HeroFrame(
    val imageRect: Rect,
    val clipRect: Rect,
    val cornerPercent: Float,
)

/**
 * The hero frame at [fraction] (0 = on the thumbnail, 1 = fitted in [container]).
 *
 * At 0 the image is drawn exactly like the thumbnail — cropped to [source] and, for a circular
 * avatar, clipped to a circle. At 1 it is drawn like the zoomable viewer at fit scale, unclipped.
 * All rectangles share the coordinate space of [source] and [container].
 */
internal fun heroFrame(
    source: Rect,
    container: Rect,
    intrinsic: Size,
    isCircle: Boolean,
    fraction: Float,
): HeroFrame {
    val t = fraction.coerceIn(0f, 1f)
    val fitted = fitRect(container, intrinsic)
    return HeroFrame(
        imageRect = lerp(cropRect(source, intrinsic), fitted, t),
        clipRect = lerp(source, fitted, t),
        cornerPercent = if (isCircle) 50f * (1f - t) else 0f,
    )
}

/**
 * Maps a screen rectangle into a local space with [screenToLocal] (typically
 * `LayoutCoordinates::screenToLocal`), or returns null when a corner is not mappable.
 */
internal fun screenRectToLocal(
    rect: Rect,
    screenToLocal: (Offset) -> Offset,
): Rect? {
    val corners =
        listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight).map(screenToLocal)
    if (corners.any { !it.isSpecified }) return null
    return boundingRect(corners)
}

/**
 * True when a hero transition between [source] and [container] can be drawn: both are known and
 * non-empty, the image size is known, and the thumbnail is at least partly inside the container.
 */
internal fun isHeroGeometryUsable(
    source: Rect?,
    container: Rect,
    intrinsic: Size?,
): Boolean =
    source != null &&
        intrinsic != null &&
        source.width > 0f &&
        source.height > 0f &&
        container.width > 0f &&
        container.height > 0f &&
        intrinsic.width > 0f &&
        intrinsic.height > 0f &&
        source.overlaps(container)
