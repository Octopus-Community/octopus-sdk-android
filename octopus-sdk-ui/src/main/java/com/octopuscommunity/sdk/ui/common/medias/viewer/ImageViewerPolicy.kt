package com.octopuscommunity.sdk.ui.common.medias.viewer

import androidx.compose.runtime.Immutable
import com.octopuscommunity.sdk.domain.model.Media
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** How the viewer enters and leaves the screen. */
internal enum class ImageViewerTransition {
    /** No motion: system animations are off, or the viewer is reopened after a recreation. */
    Instant,

    /** The image flies from (and back to) its thumbnail. */
    Hero,

    /** Cross-fade, when the thumbnail's position or the image is unknown. */
    Fade,
}

/**
 * Picks the transition: [Instant][ImageViewerTransition.Instant] when the animator duration scale
 * is zero (animations disabled in the system settings) or when [restored] — the viewer was already
 * on screen before a rotation or a process death, so it reappears as it was —
 * [Hero][ImageViewerTransition.Hero] when its geometry is usable, [Fade][ImageViewerTransition.Fade]
 * otherwise.
 */
internal fun resolveImageViewerTransition(
    durationScale: Float,
    heroUsable: Boolean,
    restored: Boolean = false,
): ImageViewerTransition =
    when {
        durationScale <= 0f || restored -> ImageViewerTransition.Instant
        heroUsable -> ImageViewerTransition.Hero
        else -> ImageViewerTransition.Fade
    }

/** Zoom fraction under which the image counts as shown at fit scale. */
internal const val FIT_SCALE_ZOOM_FRACTION = 0.01f

/**
 * True when a vertical fling may dismiss the viewer: only at fit scale — when zoomed in the same
 * gesture pans the image — and never while the pager moves between photos, so a swipe that drifts
 * vertically pages instead of closing. A pinch never dismisses: the zoom consumes its moves, and the
 * flick only starts on a vertical move nothing else consumed.
 *
 * @param zoomFraction the photo's zoom fraction; null before the image is measured, which counts as
 *   fit scale.
 * @param pagerAtRest false while the pager is dragged or settles on a page.
 */
internal fun isFlickToDismissEnabled(
    zoomFraction: Float?,
    pagerAtRest: Boolean,
): Boolean = isAtFitScale(zoomFraction) && pagerAtRest

/**
 * Whether a photo's flick-to-dismiss is on, from [current]: [isFlickToDismissEnabled] once no
 * finger is down, unchanged while [pressedPointers] are. Switching it mid-gesture removes its
 * gesture handler, which stops a pinch: one started at fit scale keeps the flick on until its
 * fingers are up, and turns it off then if the photo stayed zoomed.
 */
internal fun nextFlickEnabled(
    current: Boolean,
    pressedPointers: Int,
    zoomFraction: Float?,
    pagerAtRest: Boolean,
): Boolean = if (pressedPointers > 0) current else isFlickToDismissEnabled(zoomFraction, pagerAtRest)

/** [index] within the [count] photos of a viewer: `0` when there are none. */
internal fun clampViewerIndex(
    index: Int,
    count: Int,
): Int = if (count <= 0) 0 else index.coerceIn(0, count - 1)

/**
 * True when the reader may swipe to another photo: there is more than one, the current one is at
 * fit scale — a zoomed photo is zoomed back out before changing page, as on iOS — and at most one
 * finger is down: a pinch spreads its fingers horizontally, and would otherwise page.
 */
internal fun isPagingEnabled(
    zoomFraction: Float?,
    pageCount: Int,
    pressedPointers: Int,
): Boolean = pageCount > 1 && isAtFitScale(zoomFraction) && pressedPointers <= 1

/**
 * True when the viewer's controls (close button, counter, dots, system bars) are shown: when the
 * reader has not hidden them with a tap and the current photo is not zoomed, or whenever its image
 * failed to load, so Close and Retry stay reachable.
 */
internal fun areControlsShown(
    chromeVisible: Boolean,
    zoomFraction: Float?,
    failed: Boolean,
): Boolean = failed || (chromeVisible && isAtFitScale(zoomFraction))

/**
 * True when photo [page] goes back to fit scale: once the pager rests on another one
 * ([settledPage]), so the photos around the one being read are never left zoomed.
 */
internal fun shouldResetZoom(
    page: Int,
    settledPage: Int,
    scrollInProgress: Boolean,
): Boolean = page != settledPage && !scrollInProgress

/**
 * True when the closing photo can fly back to its thumbnail instead of fading: the viewer animates
 * ([transition] is not [Instant][ImageViewerTransition.Instant]), the photo's thumbnail is in the
 * memory cache and on screen, the photo is at fit scale, and the window has not changed since the
 * opening (orientation and size).
 */
internal fun canFlyBack(
    transition: ImageViewerTransition,
    hasHeroBitmap: Boolean,
    hasSourceBounds: Boolean,
    atFitScale: Boolean,
    sameConfiguration: Boolean,
): Boolean =
    transition != ImageViewerTransition.Instant &&
        hasHeroBitmap &&
        hasSourceBounds &&
        atFitScale &&
        sameConfiguration

/** True when [zoomFraction] (null = not measured yet) is at fit scale. */
internal fun isAtFitScale(zoomFraction: Float?): Boolean = zoomFraction == null || zoomFraction <= FIT_SCALE_ZOOM_FRACTION

/** How long a restored viewer waits for its media before closing. */
internal val IMAGE_VIEWER_RESTORE_TIMEOUT: Duration = 10.seconds

/** What the viewer shows for the current state of its media. */
@Immutable
internal sealed interface ImageViewerContent {
    /** Nothing to show yet: a restored viewer waiting for its media. */
    data object Pending : ImageViewerContent

    /** Show [medias], never empty. */
    data class Show(
        val medias: List<Media.Image>,
    ) : ImageViewerContent

    /** Close the viewer: its media is gone or never came. */
    data object Close : ImageViewerContent
}

/**
 * Decides what the viewer shows. An empty list counts as none.
 *
 * @param loaded the images the repository currently holds for the viewer's key, if any.
 * @param initial the images handed over by the thumbnail that opened the viewer; null after a
 *   restoration, where only the key survived.
 * @param hadLoaded true once [loaded] has held images: its losing them afterwards means the
 *   content was deleted.
 * @param timedOut true once [IMAGE_VIEWER_RESTORE_TIMEOUT] has elapsed.
 */
internal fun resolveImageViewerContent(
    loaded: List<Media.Image>?,
    initial: List<Media.Image>?,
    hadLoaded: Boolean,
    timedOut: Boolean,
): ImageViewerContent {
    val loadedImages = loaded?.takeIf { it.isNotEmpty() }
    val initialImages = initial?.takeIf { it.isNotEmpty() }
    return when {
        loadedImages != null -> ImageViewerContent.Show(loadedImages)
        // The repository had the image and lost it: the content was deleted.
        hadLoaded -> ImageViewerContent.Close
        // Opened from a thumbnail: it proved the image exists, so a repository that has not
        // caught up yet (or never stores that content) does not close the viewer.
        initialImages != null -> ImageViewerContent.Show(initialImages)
        timedOut -> ImageViewerContent.Close
        else -> ImageViewerContent.Pending
    }
}

/**
 * True when the system bars are shown: with the viewer's controls, and as soon as it closes. The
 * bars are the whole screen's: hidden while the viewer closed, they let the screen behind lay out
 * without them, which then jumped by their height when they came back after the window had gone.
 */
internal fun areSystemBarsShown(
    controlsShown: Boolean,
    exiting: Boolean,
): Boolean = exiting || controlsShown

/**
 * Frames the closing viewer waits before reading where its photo lands: one for the carousel to take
 * the closing page, and two more when the system bars come back with the closing
 * ([systemBarsWereHidden]), for the screen behind to lay out again with them.
 */
internal fun framesBeforeFlightHome(systemBarsWereHidden: Boolean): Int = if (systemBarsWereHidden) 3 else 1

/** How the viewer's photo leaves the screen when it closes. */
internal enum class ClosingMotion {
    /** It flies home to its thumbnail. */
    Fly,

    /** A flick carries it off screen, with its own animation. */
    FlickOff,

    /** It fades out, shrinking slightly. */
    Fade,

    /** It goes at once: the viewer opened without motion, and nothing is under way. */
    None,
}

/**
 * How the photo leaves: it flies home whenever it can ([canFlyBack]); otherwise a flick that
 * dismissed it ([flicked]) runs to its end — also on a viewer that opened without motion (restored,
 * or a media destination), where the window going at once used to cut it — and it fades, or goes at
 * once when the viewer opened without motion.
 */
internal fun resolveClosingMotion(
    transition: ImageViewerTransition,
    canFlyBack: Boolean,
    flicked: Boolean,
): ClosingMotion =
    when {
        canFlyBack -> ClosingMotion.Fly
        flicked -> ClosingMotion.FlickOff
        transition == ImageViewerTransition.Instant -> ClosingMotion.None
        else -> ClosingMotion.Fade
    }

/**
 * True when an inline video paused for the viewer should play again once it closes: only if it
 * was playing (or about to) and had not reached its end.
 */
internal fun shouldResumeAfterViewer(
    playWhenReady: Boolean,
    isEnded: Boolean,
): Boolean = playWhenReady && !isEnded
