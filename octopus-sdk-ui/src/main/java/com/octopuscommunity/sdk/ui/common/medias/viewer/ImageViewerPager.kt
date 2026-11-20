package com.octopuscommunity.sdk.ui.common.medias.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.pageindicator.OctopusPageCounter
import com.octopuscommunity.sdk.ui.common.pageindicator.OctopusPageIndicator
import com.octopuscommunity.sdk.ui.common.pageindicator.PageIndicatorState
import com.octopuscommunity.sdk.ui.common.pageindicator.rememberLastMoveForward
import com.octopuscommunity.sdk.ui.common.stringResource

private val IndicatorBottomPadding = 16.dp

/** Space between the viewer's controls (Close, the counter) and the edges of [ViewerControlsInsets]. */
internal val ViewerControlInset = 8.dp

/** Height of the row of the viewer's top controls: Close's tap target, the counter centered on it. */
internal val ViewerControlSize = 48.dp

/**
 * Where the viewer's controls stay clear of: the safe drawing area and the system bars, even hidden,
 * so the controls do not move when a tap hides or shows the bars with them.
 */
@OptIn(ExperimentalLayoutApi::class)
internal val ViewerControlsInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.union(WindowInsets.systemBarsIgnoringVisibility)

/** The other dots over the black backdrop: the gray300 of Octopus's dark palette. */
private val DarkGray300 = Color(0xFF4D4D5A)

internal const val ImageViewerPagerTag = "ImageViewerPager"

/**
 * The photos of the full-screen viewer, swiped one at a time, edge to edge. Above one photo, a
 * « current / total » counter sits in the top trailing corner, centered on the height of Close in
 * the leading one, and the dots at the bottom, inside [ViewerControlsInsets], drawn only while
 * [controlsShown]; TalkBack reads the position from the pager,
 * also while they are hidden. The current photo and its two neighbours are composed, so only they
 * load their image.
 *
 * @param pagingEnabled false while the current photo is zoomed, or with a single photo.
 * @param chromeAlpha the counter's and dots' opacity, read at draw time (they fade while a flick
 *   drags the photo away).
 * @param page draws photo `index`.
 */
@Composable
internal fun ImageViewerPager(
    pagerState: PagerState,
    pagingEnabled: Boolean,
    controlsShown: Boolean,
    modifier: Modifier = Modifier,
    chromeAlpha: () -> Float = { 1f },
    page: @Composable (index: Int) -> Unit,
) {
    val pageCount = pagerState.pageCount
    val currentPage = pagerState.currentPage
    val lastMoveForward by rememberLastMoveForward(pagerState)
    val positionDescription = stringResource(R.string.accessibility_carousel_position, currentPage + 1, pageCount)

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag(ImageViewerPagerTag)
                    .semantics { if (pageCount > 1) stateDescription = positionDescription },
            beyondViewportPageCount = 1,
            userScrollEnabled = pagingEnabled,
            key = { it },
        ) { index -> page(index) }

        if (pageCount > 1) {
            // Skipping the drawing keeps the layout still when the controls come and go.
            val hiddenUnlessShown =
                Modifier
                    .graphicsLayer { alpha = chromeAlpha().coerceIn(0f, 1f) }
                    .drawWithContent { if (controlsShown) drawContent() }
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(ViewerControlsInsets)
                        .padding(ViewerControlInset)
                        .height(ViewerControlSize),
                contentAlignment = Alignment.CenterEnd,
            ) {
                OctopusPageCounter(current = currentPage, count = pageCount, modifier = hiddenUnlessShown)
            }
            PageIndicatorState
                .make(pageCount = pageCount, currentIndex = currentPage, lastMoveForward = lastMoveForward)
                ?.let { state ->
                    OctopusPageIndicator(
                        state = state,
                        // The backdrop is always black, whatever the theme: a light theme's primary
                        // (often a dark brand color) would vanish on it, and its light gray300 would
                        // look like the current dot. iOS forces the viewer dark, so do the same.
                        selectedColor = Color.White,
                        color = DarkGray300,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .windowInsetsPadding(ViewerControlsInsets)
                                .padding(bottom = IndicatorBottomPadding)
                                .then(hiddenUnlessShown),
                    )
                }
        }
    }
}
