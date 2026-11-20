package com.octopuscommunity.sdk.ui.common.pageindicator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource

private val CounterHorizontalPadding = 10.dp
private val CounterVerticalPadding = 4.dp

/** The capsule stays dark over the photo, whatever the color scheme. */
private val CounterBackground = Color.Black.copy(alpha = 0.5f)

/**
 * The « current / total » capsule over a carousel or the full-screen viewer, [current] being
 * 0-based. Hidden from accessibility: the position is read from the pager's state description.
 */
@Composable
internal fun OctopusPageCounter(
    current: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.post_carousel_counter, current + 1, count),
        modifier =
            modifier
                .background(color = CounterBackground, shape = CircleShape)
                .padding(horizontal = CounterHorizontalPadding, vertical = CounterVerticalPadding)
                .semantics { hideFromAccessibility() },
        color = Color.White,
        style = OctopusTheme.typography.caption1,
    )
}

/**
 * Whether [pagerState]'s last page change went forward, for [PageIndicatorState.make]: `true` until
 * it moves, then updated each time its current page changes. [key] restarts the tracking.
 */
@Composable
internal fun rememberLastMoveForward(
    pagerState: PagerState,
    key: Any? = null,
): State<Boolean> {
    val lastMoveForward = remember(key) { mutableStateOf(true) }
    LaunchedEffect(pagerState, key) {
        var previousPage = pagerState.currentPage
        // `currentPage` turns to the next page once it passes half the width.
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page != previousPage) lastMoveForward.value = page > previousPage
            previousPage = page
        }
    }
    return lastMoveForward
}
