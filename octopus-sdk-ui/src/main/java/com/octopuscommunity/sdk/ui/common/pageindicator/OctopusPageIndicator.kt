package com.octopuscommunity.sdk.ui.common.pageindicator

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme

private val NormalDotSize = 6.dp
private val SmallDotSize = 4.dp
private val DotSpacing = 4.dp

/** Duration of the slide of the row when the window moves by one page, and of the dots' resizing. */
private const val SLIDE_DURATION_MILLIS = 120

/** Test tag of each dot. */
internal const val PageIndicatorDotTag = "PageIndicatorDot"

/**
 * Row of dots showing where the user is in a carousel, drawn from [state]. Hidden from
 * accessibility: the carousel itself announces the page position.
 *
 * Each dot stands for a page, not for a position in the row: when the window moves by one page, the
 * dots slide by one slot, the edge dot leaving shrinks and fades out on one side while a new one grows
 * in on the other.
 *
 * @param selectedColor the current page's dot: the theme's primary color, except over a dark
 *   backdrop the theme does not know of, such as the full-screen viewer's.
 * @param color the other dots: the theme's gray300, with the same exception.
 */
@Composable
internal fun OctopusPageIndicator(
    state: PageIndicatorState,
    modifier: Modifier = Modifier,
    selectedColor: Color = OctopusTheme.colorScheme.primary,
    color: Color = OctopusTheme.colorScheme.gray300,
) {
    Row(
        modifier = modifier.clipToBounds().semantics { hideFromAccessibility() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The page on each side of the window stays composed at zero width, so that a dot entering
        // the window grows from nothing and the one leaving shrinks to nothing as the others slide.
        // Their sizes and gaps add up to the same width all along the slide.
        for (page in state.firstPage - 1..state.firstPage + state.dots.size) {
            key(page) {
                val position = page - state.firstPage
                val dot = state.dots.getOrNull(position)
                val size by animateDpAsState(
                    targetValue =
                        when (dot) {
                            PageIndicatorState.Dot.Normal -> NormalDotSize
                            PageIndicatorState.Dot.Small -> SmallDotSize
                            null -> 0.dp
                        },
                    animationSpec = tween(SLIDE_DURATION_MILLIS),
                    label = "pageIndicatorDotSize",
                )
                val gap by animateDpAsState(
                    targetValue = if (dot != null && position > 0) DotSpacing else 0.dp,
                    animationSpec = tween(SLIDE_DURATION_MILLIS),
                    label = "pageIndicatorDotGap",
                )
                val alpha by animateFloatAsState(
                    targetValue = if (dot != null) 1f else 0f,
                    animationSpec = tween(SLIDE_DURATION_MILLIS),
                    label = "pageIndicatorDotAlpha",
                )
                val color by animateColorAsState(
                    targetValue =
                        if (position == state.selectedPosition) {
                            selectedColor
                        } else {
                            color
                        },
                    label = "pageIndicatorDotColor",
                )
                Box(
                    modifier =
                        Modifier
                            .padding(start = gap)
                            .testTag(PageIndicatorDotTag)
                            .size(size)
                            .graphicsLayer { this.alpha = alpha }
                            .background(color = color, shape = CircleShape),
                )
            }
        }
    }
}
