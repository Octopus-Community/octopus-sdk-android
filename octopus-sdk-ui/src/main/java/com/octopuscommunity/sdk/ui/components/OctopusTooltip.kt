package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.medium
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/** Positioning defaults for [OctopusTooltip]. */
object OctopusTooltipDefaults {
    /**
     * Remembers a tooltip position above its anchor, falling below when space is insufficient.
     *
     * @param alignment Horizontal alignment relative to the anchor; centered tooltips use the window.
     * @param screenPadding Minimum horizontal distance from the window edges.
     * @param verticalOffset Additional upward offset from the Material tooltip position.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun rememberPositionProvider(
        alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
        screenPadding: Dp = 16.dp,
        verticalOffset: Dp = 0.dp,
    ): PopupPositionProvider {
        val screenWidth = LocalWindowInfo.current.containerSize.width
        val screenPaddingPx = with(LocalDensity.current) { screenPadding.roundToPx() }
        val verticalOffsetPx = with(LocalDensity.current) { verticalOffset.roundToPx() }
        val defaultPositionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above)
        return remember {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize,
                ) = defaultPositionProvider
                    .calculatePosition(
                        anchorBounds,
                        windowSize,
                        layoutDirection,
                        popupContentSize,
                    ).let {
                        it.copy(
                            x =
                                max(
                                    screenPaddingPx,
                                    min(
                                        when (alignment) {
                                            Alignment.Start -> anchorBounds.left
                                            Alignment.End -> anchorBounds.right - popupContentSize.width
                                            else -> (screenWidth - popupContentSize.width) / 2
                                        },
                                        screenWidth - screenPaddingPx - popupContentSize.width,
                                    ),
                                ),
                            y = it.y - verticalOffsetPx,
                        )
                    }
            }
        }
    }
}

/**
 * Displays [text] in a tooltip when the anchor [content] is clicked.
 *
 * @param positionProvider Controls the tooltip's position relative to the anchor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OctopusTooltip(
    text: String,
    positionProvider: PopupPositionProvider = OctopusTooltipDefaults.rememberPositionProvider(),
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val state = rememberTooltipState()
    TooltipBox(
        state = state,
        positionProvider = positionProvider,
        tooltip = {
            RichTooltip(
                shape = RoundedCornerShape(4.dp),
                colors =
                    TooltipDefaults.richTooltipColors(
                        containerColor = OctopusTheme.colorScheme.gray800,
                        contentColor = OctopusTheme.colorScheme.gray100,
                    ),
            ) {
                Text(
                    modifier =
                        Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    text = text,
                    style = OctopusTheme.typography.caption1.medium,
                )
            }
        },
    ) {
        Box(
            modifier =
                Modifier.clickable {
                    coroutineScope.launch { state.show() }
                },
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusTooltipPreview() {
    OctopusTheme {
        OctopusTooltip(text = "Lorem ipsum") {
            Text("Action")
        }
    }
}
