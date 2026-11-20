package com.octopuscommunity.sdk.ui.posts.blocks

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.medias.MediaPinchState
import com.octopuscommunity.sdk.ui.common.medias.mediaPinch
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerSource
import com.octopuscommunity.sdk.ui.common.medias.viewer.imageViewerSource
import com.octopuscommunity.sdk.ui.common.medias.viewer.rememberImageViewerSource
import com.octopuscommunity.sdk.ui.common.pageindicator.OctopusPageCounter
import com.octopuscommunity.sdk.ui.common.pageindicator.OctopusPageIndicator
import com.octopuscommunity.sdk.ui.common.pageindicator.PageIndicatorState
import com.octopuscommunity.sdk.ui.common.pageindicator.rememberLastMoveForward
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.components.OctopusImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val IndicatorTopPadding = 8.dp
private val CounterInset = 8.dp

internal const val CarouselPagerTag = "PostImagesCarouselPager"

/**
 * The images of a post, swiped one at a time in a zone sized by the first image
 * ([carouselAspectRatio]); the others fill it and are cropped. Dots under the zone show the page,
 * and a « current / total » counter joins them from two images. The page the post was left on is
 * kept in [LocalCarouselPositions]. Tapping image `i` hands `i` and the zone, the thumbnail of every
 * image, to [onImageClick].
 */
@Composable
internal fun PostImagesCarousel(
    postId: String,
    images: List<Media.Image>,
    onImageClick: (index: Int, source: ImageViewerSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val positions = LocalCarouselPositions.current ?: remember { CarouselPositions() }
    val pagerState = rememberCarouselPagerState(postId = postId, pageCount = images.size, positions = positions)
    val lastMoveForward by rememberLastMoveForward(pagerState, key = postId)
    val currentPage = pagerState.currentPage
    val positionDescription = stringResource(R.string.accessibility_carousel_position, currentPage + 1, images.size)
    // One source for every page: each fills the zone, and the zone stays where it is whichever photo
    // the viewer closes on.
    val source = rememberImageViewerSource()
    val pinchStates = remember(postId, images.size) { List(images.size) { MediaPinchState() } }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(carouselAspectRatio(images.first().dimensions))
                    .imageViewerSource(source),
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = pinchStates.none { it.isActive },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .testTag(CarouselPagerTag)
                        .semantics { stateDescription = positionDescription },
            ) { page ->
                val image = images[page]
                // The backend sends the photos without a name: TalkBack then reads their position.
                val description =
                    image.name.ifEmpty { stringResource(R.string.accessibility_carousel_position, page + 1, images.size) }
                OctopusImage(
                    data = image.source.data,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .mediaPinch(pinchStates[page]) {
                                // Disabling input alone cancels a drag but still launches its
                                // snap. Hold the pager immediately, including through return.
                                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                    pagerState.scroll(MutatePriority.PreventUserInput) {
                                        snapshotFlow { pinchStates[page].isActive }.first { !it }
                                    }
                                    // A pinch can interrupt a partial swipe. Once the photo is
                                    // back, settle on that same page rather than leaving it offset.
                                    pagerState.animateScrollToPage(page)
                                }
                            }.clickable { onImageClick(page, source) },
                    contentDescription = description,
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(OctopusTheme.colorScheme.gray200),
                )
            }
            if (PageIndicatorState.showsCounter(pageCount = images.size)) {
                OctopusPageCounter(
                    current = currentPage,
                    count = images.size,
                    modifier = Modifier.align(Alignment.TopEnd).padding(CounterInset),
                )
            }
        }
        PageIndicatorState
            .make(
                pageCount = images.size,
                currentIndex = currentPage,
                lastMoveForward = lastMoveForward,
            )?.let { state ->
                OctopusPageIndicator(
                    state = state,
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = IndicatorTopPadding),
                )
            }
    }
}

/**
 * Shows [recorded], the page another carousel of the same post records. A member dragging this one
 * has the last word: the scroll is then refused, and the page they settle on is recorded in turn.
 */
private suspend fun PagerState.followRecordedPage(
    recorded: Int,
    pageCount: Int,
) {
    val page = recorded.coerceIn(0, pageCount - 1)
    if (page == currentPage) return
    try {
        scrollToPage(page)
    } catch (refused: CancellationException) {
        // Refused by the member's drag, unless this carousel is leaving.
        currentCoroutineContext().ensureActive()
    }
}

/**
 * The pager of [postId]'s carousel, kept in step with [positions] both ways: it opens on the page
 * recorded for the post and records the page it shows, and it follows the page another carousel of
 * the same post records — the post's detail, or the full-screen viewer — including one recorded
 * while it was away, which wins over the page it saved. [positions] is read outside composition, so
 * a page recorded for one post never recomposes the carousels of the others.
 */
@Composable
internal fun rememberCarouselPagerState(
    postId: String,
    pageCount: Int,
    positions: CarouselPositions,
): PagerState {
    val initialPage = remember(postId) { Snapshot.withoutReadObservation { positions.index(postId) } }
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, pageCount - 1)) { pageCount }
    LaunchedEffect(pagerState, postId) {
        // A page recorded while this carousel was away comes first; with none recorded (after a
        // process restart, or outside an SDK screen), a restored pager keeps its own.
        positions.indexOrNull(postId)?.let { recorded -> pagerState.followRecordedPage(recorded, pageCount) }
        launch {
            snapshotFlow { pagerState.currentPage }.collect { page -> positions.set(postId = postId, index = page) }
        }
        snapshotFlow { positions.indexOrNull(postId) }.filterNotNull().collect { recorded ->
            pagerState.followRecordedPage(recorded, pageCount)
        }
    }
    return pagerState
}
