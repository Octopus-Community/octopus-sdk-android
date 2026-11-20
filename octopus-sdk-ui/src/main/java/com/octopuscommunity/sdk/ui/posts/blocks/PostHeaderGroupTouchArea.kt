package com.octopuscommunity.sdk.ui.posts.blocks

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/** Coordinates are read at gesture time; updating them never measures or moves the header. */
internal class PostHeaderGroupTouchArea {
    var input: LayoutCoordinates? = null
    var header: LayoutCoordinates? = null
    var column: LayoutCoordinates? = null
    var label: LayoutCoordinates? = null
    var author: LayoutCoordinates? = null
    var avatar: LayoutCoordinates? = null
    var menu: LayoutCoordinates? = null
    var translationToggle: LayoutCoordinates? = null

    fun labelBounds(): Rect = relativeBounds(label) ?: Rect.Zero

    fun bounds(configuration: PostHeaderGroupTouchConfiguration): Rect {
        val header = relativeBounds(header) ?: return Rect.Zero
        val column = relativeBounds(column) ?: return Rect.Zero
        val label = relativeBounds(label) ?: return Rect.Zero
        val author = relativeBounds(author) ?: return Rect.Zero
        val top = max(label.top - configuration.groupGap, author.bottom + configuration.authorGap)
        val bottom = if (configuration.extendToHeaderBottom) header.bottom else label.bottom
        val left = max(header.left, column.left)
        val right = min(header.right, column.right)
        return if (right > left && bottom > top) Rect(left, top, right, bottom) else Rect.Zero
    }

    fun contains(
        configuration: PostHeaderGroupTouchConfiguration,
        position: Offset,
    ): Boolean =
        bounds(configuration).contains(position) &&
            relativeBounds(translationToggle)?.contains(position) != true &&
            controlBounds(avatar, configuration)?.containsIncludingEdges(position) != true &&
            controlBounds(menu, configuration)?.containsIncludingEdges(position) != true

    // Compose includes the outer edge of a minimum-touch expansion. The translation Row
    // uses its actual layout bounds instead and keeps Rect.contains' exclusive end edges.
    private fun Rect.containsIncludingEdges(position: Offset): Boolean =
        position.x >= left && position.x <= right && position.y >= top && position.y <= bottom

    private fun controlBounds(
        coordinates: LayoutCoordinates?,
        configuration: PostHeaderGroupTouchConfiguration,
    ): Rect? {
        val bounds = relativeBounds(coordinates) ?: return null
        val horizontal = (configuration.minimumTouchWidth - bounds.width).coerceAtLeast(0f) / 2f
        val vertical = (configuration.minimumTouchHeight - bounds.height).coerceAtLeast(0f) / 2f
        return Rect(bounds.left - horizontal, bounds.top - vertical, bounds.right + horizontal, bounds.bottom + vertical)
    }

    private fun relativeBounds(coordinates: LayoutCoordinates?): Rect? {
        val input = input?.takeIf { it.isAttached } ?: return null
        val coordinates = coordinates?.takeIf { it.isAttached } ?: return null
        return input.localBoundingBoxOf(coordinates, clipBounds = false)
    }
}

internal data class PostHeaderGroupTouchConfiguration(
    val authorGap: Int,
    val groupGap: Int,
    val minimumTouchWidth: Float,
    val minimumTouchHeight: Float,
    val extendToHeaderBottom: Boolean,
)

internal class PostHeaderGroupTouchTarget(
    val area: PostHeaderGroupTouchArea,
    val configuration: PostHeaderGroupTouchConfiguration,
    val source: MutableInteractionSource,
    val onClick: State<(() -> Unit)?>,
) {
    val enabled: Boolean get() = configuration.extendToHeaderBottom
}

@Composable
internal fun rememberPostHeaderGroupTouchTarget(
    onClick: (() -> Unit)?,
    extendToHeaderBottom: Boolean,
    groupName: String?,
): PostHeaderGroupTouchTarget {
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val gap = with(density) { 2.dp.roundToPx() }
    val enabled = extendToHeaderBottom && groupName != null && onClick != null
    return PostHeaderGroupTouchTarget(
        area = remember { PostHeaderGroupTouchArea() },
        configuration =
            PostHeaderGroupTouchConfiguration(
                authorGap = gap / 2,
                groupGap = gap - gap / 2,
                minimumTouchWidth = with(density) { viewConfiguration.minimumTouchTargetSize.width.toPx() },
                minimumTouchHeight = with(density) { viewConfiguration.minimumTouchTargetSize.height.toPx() },
                extendToHeaderBottom = enabled,
            ),
        source = remember { MutableInteractionSource() },
        onClick = rememberUpdatedState(onClick),
    )
}

/**
 * Extra input only: the Text keeps the sole button semantics and drawing bounds. Handle the
 * initial pass on the common post ancestor, reserving adjacent controls' full touch bounds;
 * downs outside the remaining header area stay unconsumed for text, selection and controls.
 */
internal fun Modifier.postHeaderGroupTouchArea(target: PostHeaderGroupTouchTarget): Modifier =
    pointerInput(target.area, target.configuration, target.source) {
        val area = target.area
        val configuration = target.configuration
        val source = target.source
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.isConsumed || !area.contains(configuration, down.position)) return@awaitEachGesture
            down.consume()
            val press = PressInteraction.Press(down.position - area.labelBounds().topLeft)
            source.tryEmit(press)
            var released = false
            try {
                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                if (up != null && area.contains(configuration, up.position)) {
                    up.consume()
                    released = true
                    target.onClick.value?.invoke()
                }
            } finally {
                source.tryEmit(if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press))
            }
        }
    }
