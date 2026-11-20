package com.octopuscommunity.sdk.ui.common.medias.viewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Stable identity of the image shown by the full-screen viewer: what it belongs to and which of
 * its images it is. It is the only thing the viewer saves across a recreation — the media itself
 * is reloaded from the repositories, never persisted.
 */
@Immutable
internal data class ImageViewerKey(
    val kind: Kind,
    val id: String,
    val index: Int = 0,
) {
    /** The owner of the image, which decides the repository it is reloaded from. */
    enum class Kind { Post, Comment, Reply, Avatar }

    /** Encodes the key as `kind|index|id`; [decode] is its inverse. */
    fun encode(): String = "${kind.name}$SEPARATOR$index$SEPARATOR$id"

    companion object {
        private const val SEPARATOR = '|'

        /**
         * Decodes a value written by [encode], or returns null for anything else — an unknown
         * kind (a value saved by a newer SDK), a negative index or an empty id.
         */
        fun decode(value: String?): ImageViewerKey? {
            val parts = value?.split(SEPARATOR, limit = 3) ?: return null
            if (parts.size != 3) return null
            val kind = Kind.entries.firstOrNull { it.name == parts[0] } ?: return null
            val index = parts[1].toIntOrNull()?.takeIf { it >= 0 } ?: return null
            val id = parts[2].takeIf { it.isNotEmpty() } ?: return null
            return ImageViewerKey(kind = kind, id = id, index = index)
        }
    }
}

/** Saves an [ImageViewerKey] as its [ImageViewerKey.encode] string. */
internal val ImageViewerKeySaver: Saver<ImageViewerKey?, String> =
    Saver(
        save = { it?.encode() },
        restore = { ImageViewerKey.decode(it) },
    )

/**
 * The thumbnail an image viewer opens from: where it is on screen (for the hero transition) and
 * whether it is hidden while the image flies to or from it.
 */
@Stable
internal class ImageViewerSource(
    val isCircle: Boolean = false,
) {
    /** Last known layout coordinates of the thumbnail; null until laid out. */
    var coordinates: LayoutCoordinates? = null

    /** Hides the thumbnail while the hero flight draws its image instead. */
    var isHidden by mutableStateOf(false)

    /**
     * The thumbnail's bounds in screen coordinates, or null when they are unknown: not attached,
     * zero-sized, or not mappable to the screen on this platform.
     */
    fun boundsInScreen(): Rect? {
        val layout = coordinates?.takeIf { it.isAttached } ?: return null
        val width = layout.size.width.toFloat()
        val height = layout.size.height.toFloat()
        if (width <= 0f || height <= 0f) return null
        val corners =
            listOf(
                Offset.Zero,
                Offset(width, 0f),
                Offset(0f, height),
                Offset(width, height),
            ).map { layout.localToScreen(it) }
        if (corners.any { !it.isSpecified }) return null
        return boundingRect(corners)
    }
}

/** Remembers an [ImageViewerSource] for one thumbnail. */
@Composable
internal fun rememberImageViewerSource(isCircle: Boolean = false): ImageViewerSource =
    remember(isCircle) { ImageViewerSource(isCircle = isCircle) }

/**
 * Tracks this element as the thumbnail of [source] and hides it while [ImageViewerSource.isHidden]
 * is set. A null [source] leaves the element untouched.
 */
internal fun Modifier.imageViewerSource(source: ImageViewerSource?): Modifier =
    if (source == null) {
        this
    } else {
        this
            .onGloballyPositioned { source.coordinates = it }
            .graphicsLayer { alpha = if (source.isHidden) 0f else 1f }
    }

/**
 * Process-wide count of open image viewers, so inline videos can pause while one is shown: a
 * viewer is a dialog window, which does not stop the host's lifecycle.
 */
internal object ImageViewerSession {
    private val openCount = MutableStateFlow(0)

    /** Number of image viewers currently open. */
    val count: StateFlow<Int> = openCount.asStateFlow()

    /** Records a viewer opening. */
    fun onOpened() = openCount.update { it + 1 }

    /** Records a viewer closing; never goes below zero. */
    fun onClosed() = openCount.update { (it - 1).coerceAtLeast(0) }
}
