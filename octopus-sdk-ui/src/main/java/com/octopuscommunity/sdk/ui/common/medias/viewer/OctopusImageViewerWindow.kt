package com.octopuscommunity.sdk.ui.common.medias.viewer

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.memory.MemoryCache
import com.octopuscommunity.sdk.data.utils.ImageRequestBuilder
import com.octopuscommunity.sdk.data.utils.getImageCacheKey
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.components.OctopusLocaleWrapper
import com.octopuscommunity.sdk.ui.components.OctopusProgressIndicator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.saket.telephoto.ExperimentalTelephotoApi
import me.saket.telephoto.flick.FlickToDismiss
import me.saket.telephoto.flick.FlickToDismissState
import me.saket.telephoto.flick.rememberFlickToDismissState
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import org.koin.compose.koinInject
import kotlin.math.min
import kotlin.math.roundToInt

private const val ENTER_DURATION_MS = 300
private const val EXIT_DURATION_MS = 250
private const val SPINNER_DELAY_MS = 400L
private const val ZOOM_STEP = 1.5f
private const val PAN_STEP_FRACTION = 0.25f
private const val FADE_MIN_SCALE = 0.9f
private const val BACK_MIN_SCALE = 0.9f

// Never Type.systemBars(): that mask also contains the captionBar, and requesting it visible
// makes the window lay content out below the status-bar frame on API 35+.
private val STATUS_AND_NAVIGATION_BARS =
    WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()

/** What is drawn at a given moment of the viewer's life. */
internal enum class ViewerMode {
    /** The hero bitmap flies between the thumbnail and the fitted image. */
    Flying,

    /** The zoomable image fades (and slightly scales) in or out. */
    Fading,

    /** The zoomable image is shown, interactive. */
    Settled,
}

/** Last layout coordinates of the hero canvas, read at draw time without recomposing. */
private class CoordinatesHolder {
    var value: LayoutCoordinates? = null
}

/**
 * The full-screen image viewer: its own edge-to-edge dialog window with a black palette, so the
 * host's window, system-bar colours and visibility are never touched. It pages through [medias],
 * opening on [initialIndex]; pinch, double-tap and pan zoom the current photo, and a zoomed photo
 * cannot be swiped away; a vertical flick at fit scale, back (predictive on API 33+) or Close
 * dismisses.
 *
 * The controls — Close, the counter and dots above one photo, and the system bars — are shown when
 * the viewer opens; a tap hides or shows them, with no timer, and they hide while the photo is
 * zoomed. Close stays in the semantics tree while hidden, so accessibility services always reach it.
 *
 * @param source the thumbnail it opens from, for the hero transition; null to fade.
 * @param restored true when reopened after a recreation: no hero, the thumbnail is gone.
 * @param contentDescription the description of a photo.
 * @param onPageChanged called with the page the pager settles on.
 * @param onClosing called with the page being read as soon as the viewer starts closing.
 * @param onDismissed called once, when the viewer has finished closing.
 */
@OptIn(ExperimentalTelephotoApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun OctopusImageViewerWindow(
    medias: List<Media.Image>,
    initialIndex: Int,
    source: ImageViewerSource?,
    restored: Boolean,
    contentDescription: (Media.Image) -> String?,
    onPageChanged: (Int) -> Unit,
    onClosing: (closingIndex: Int) -> Unit,
    onDismissed: () -> Unit,
) {
    DisposableEffect(Unit) {
        ImageViewerSession.onOpened()
        onDispose {
            ImageViewerSession.onClosed()
            source?.isHidden = false
        }
    }

    val context = LocalContext.current
    val hostLayoutDirection = LocalLayoutDirection.current
    val configuration = LocalConfiguration.current
    val imageLoader = koinInject<ImageLoader>()
    val imageRequestBuilder = koinInject<ImageRequestBuilder>()

    val pagerState = rememberPagerState(initialPage = clampViewerIndex(initialIndex, medias.size)) { medias.size }
    // Everything the transition depends on is read once, synchronously, so the very first frame
    // is already drawn at the thumbnail's place.
    val openState =
        remember {
            val bitmap = if (restored) null else heroBitmap(imageLoader, medias.getOrNull(pagerState.currentPage)?.cacheKey())
            val bounds = if (restored) null else source?.boundsInScreen()
            val metrics = context.resources.displayMetrics
            val screen = Rect(0f, 0f, metrics.widthPixels.toFloat(), metrics.heightPixels.toFloat())
            val intrinsic = bitmap?.let { Size(it.width.toFloat(), it.height.toFloat()) }
            OpenState(
                heroBitmap = bitmap,
                transition =
                    resolveImageViewerTransition(
                        durationScale = animatorDurationScale(context),
                        heroUsable = isHeroGeometryUsable(bounds, screen, intrinsic),
                        restored = restored,
                    ),
                sourceBounds = bounds,
                orientation = configuration.orientation,
                screenWidthDp = configuration.screenWidthDp,
                screenHeightDp = configuration.screenHeightDp,
            )
        }
    val transition = openState.transition

    val progress = remember { Animatable(if (transition == ImageViewerTransition.Instant) 1f else 0f) }
    var mode by remember {
        mutableStateOf(
            when (transition) {
                ImageViewerTransition.Hero -> ViewerMode.Flying
                ImageViewerTransition.Fade -> ViewerMode.Fading
                ImageViewerTransition.Instant -> ViewerMode.Settled
            },
        )
    }
    var heroBitmap by remember { mutableStateOf(openState.heroBitmap) }
    var heroSourceBounds by remember { mutableStateOf(openState.sourceBounds) }
    // Where a flick left the photo when it starts flying home, and how far it was flicked then: the
    // flight starts there and the backdrop fades from there, while the flick carries on unseen. Set
    // only for that flight: a flick that carries the photo off keeps the backdrop following it.
    var releasedFlickOffset by remember { mutableStateOf(Offset.Zero) }
    var releasedFlickFraction by remember { mutableStateOf<Float?>(null) }
    var chromeVisible by rememberSaveable { mutableStateOf(true) }
    var systemBarsShown by remember { mutableStateOf(true) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var exiting by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Each page holds its own zoom; the window reads the current page's.
    val pages = remember { PagesState() }
    val currentZoomFraction = pages.zoom[pagerState.currentPage]?.zoomFraction

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect(onPageChanged)
    }

    suspend fun finish() {
        if (dismissed) return
        dismissed = true
        // The thumbnail is drawn again before the window goes: the photo has landed on it, so for
        // that frame they overlap exactly, instead of an empty place for the frames the window
        // takes to disappear.
        source?.isHidden = false
        withFrameNanos { }
        onDismissed()
    }

    /**
     * Closes the viewer: the photo flies home, or fades. [flickDuration] is set when a flick
     * dismisses it: the photo then flies home from where the flick released it, or, when it cannot,
     * goes on with the flick's own animation, which lasts that long.
     */
    fun exit(flickDuration: Long? = null) {
        if (exiting || dismissed) return
        val systemBarsWereHidden = !systemBarsShown
        exiting = true
        chromeVisible = false
        // First, so a post's carousel, hidden, moves to this photo before the photo flies to it.
        onClosing(pagerState.currentPage)
        scope.launch {
            // The carousel takes that page, and the screen behind lays out again with the system
            // bars if they come back now; then the photo's place is read where it is.
            repeat(framesBeforeFlightHome(systemBarsWereHidden)) { withFrameNanos { } }
            val liveBounds = source?.boundsInScreen()
            // The photo flies home from the page being read, so with its own thumbnail.
            val closingBitmap = heroBitmap(imageLoader, medias.getOrNull(pagerState.currentPage)?.cacheKey())
            val canFlyBack =
                canFlyBack(
                    transition = transition,
                    hasHeroBitmap = closingBitmap != null,
                    hasSourceBounds = liveBounds != null,
                    atFitScale = isAtFitScale(currentZoomFraction),
                    sameConfiguration =
                        configuration.orientation == openState.orientation &&
                            configuration.screenWidthDp == openState.screenWidthDp &&
                            configuration.screenHeightDp == openState.screenHeightDp,
                )
            // A completed back gesture lands here with `backProgress` left where the gesture
            // released it: the hero starts from that shrunken frame (see HeroCanvas) and the
            // backdrop keeps its half-faded alpha, so the image flies home from where the gesture
            // left it, exactly like Close — instead of the plain fade it used to fall back to.
            when (resolveClosingMotion(transition, canFlyBack, flicked = flickDuration != null)) {
                ClosingMotion.Fly -> {
                    if (flickDuration != null) {
                        // From where the photo is drawn now, the flick having moved it; the backdrop
                        // fades from how far it was flicked, while the flick goes on unseen.
                        val closingPage = pagerState.currentPage
                        releasedFlickOffset = pages.flickDrawnOffset[closingPage] ?: Offset.Zero
                        releasedFlickFraction = pages.flick[closingPage]?.offsetFraction
                    }
                    heroBitmap = closingBitmap
                    heroSourceBounds = liveBounds
                    mode = ViewerMode.Flying
                    progress.animateTo(0f, tween(EXIT_DURATION_MS, easing = FastOutSlowInEasing))
                }
                ClosingMotion.FlickOff -> {
                    // The flick carries the photo off screen, the backdrop fading with it: nothing
                    // lands on the thumbnail.
                    source?.isHidden = false
                    delay(flickDuration ?: 0L)
                }
                ClosingMotion.Fade -> {
                    // Nothing lands on the thumbnail: it comes back as the fade starts, as on iOS.
                    source?.isHidden = false
                    mode = ViewerMode.Fading
                    progress.animateTo(0f, tween(EXIT_DURATION_MS, easing = FastOutSlowInEasing))
                }
                ClosingMotion.None -> Unit
            }
            finish()
        }
    }

    LaunchedEffect(Unit) {
        // The thumbnail stays hidden while the viewer is open, flick and back gestures included,
        // where the screen behind shows: an empty place, never the wrong photo nor the same one
        // twice. It is hidden once the window draws (see the Dialog below), and finish() shows it
        // again, once the photo has landed.
        if (transition != ImageViewerTransition.Instant) {
            progress.animateTo(1f, tween(ENTER_DURATION_MS, easing = FastOutSlowInEasing))
        }
        if (!exiting) mode = ViewerMode.Settled
    }

    // Keyed on a boolean, not on the gesture state itself: with animations off or sped up, flick
    // reaches `Dismissed` before the `Dismissing` delay elapses, and a state-keyed effect was
    // cancelled right there — the window stayed open with the image off screen and Back ignored.
    val flickState = pages.flick[pagerState.currentPage]
    val gestureState = flickState?.gestureState
    val flickDismissing =
        gestureState is FlickToDismissState.GestureState.Dismissing ||
            gestureState is FlickToDismissState.GestureState.Dismissed
    LaunchedEffect(flickDismissing) {
        if (!flickDismissing) return@LaunchedEffect
        val dismissing = flickState?.gestureState as? FlickToDismissState.GestureState.Dismissing
        exit(flickDuration = dismissing?.animationDuration?.inWholeMilliseconds ?: 0L)
    }

    Dialog(
        onDismissRequest = ::exit,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnClickOutside = false,
            ),
    ) {
        val dialogView = LocalView.current
        LaunchedEffect(Unit) {
            // The window takes a frame or two to appear: hiding the thumbnail before it draws left an
            // empty, white place on screen meanwhile. Once it has drawn, the photo it starts from
            // covers the thumbnail exactly.
            repeat(2) { withFrameNanos { } }
            if (!dismissed) source?.isHidden = true
        }
        val window = (dialogView.parent as? DialogWindowProvider)?.window
        DisposableEffect(window) {
            window?.applyViewerWindowStyle()
            onDispose { }
        }
        val controlsShown =
            !exiting &&
                areControlsShown(
                    chromeVisible = chromeVisible,
                    zoomFraction = currentZoomFraction,
                    failed = pages.failed[pagerState.currentPage] == true,
                )
        val insetsController =
            remember(window) { window?.let { WindowCompat.getInsetsController(it, it.decorView) } }
        val barsShown = areSystemBarsShown(controlsShown = controlsShown, exiting = exiting)
        LaunchedEffect(insetsController, barsShown) {
            systemBarsShown = barsShown
            insetsController?.apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
                if (barsShown) show(STATUS_AND_NAVIGATION_BARS) else hide(STATUS_AND_NAVIGATION_BARS)
            }
        }

        // The dialog's back dispatcher, so the handler below outranks the dialog's own callback
        // even when the host provided a dispatcher owner of its own.
        val backOwner = dialogView.findViewTreeOnBackPressedDispatcherOwner()
        val locals =
            listOfNotNull(
                LocalLayoutDirection provides hostLayoutDirection,
                backOwner?.let { LocalOnBackPressedDispatcherOwner provides it },
            ).toTypedArray()
        CompositionLocalProvider(*locals) {
            OctopusLocaleWrapper {
                PredictiveBackHandler(enabled = !exiting && !dismissed) { events ->
                    try {
                        events.collect { backProgress = it.progress }
                        exit()
                    } catch (e: CancellationException) {
                        scope.launch {
                            animate(backProgress, 0f) { value, _ -> backProgress = value }
                        }
                        throw e
                    }
                }

                ViewerContent(
                    pageCount = medias.size,
                    pagerState = pagerState,
                    pages = pages,
                    heroBitmap = heroBitmap,
                    heroSourceBounds = { heroSourceBounds },
                    releasedFlickOffset = { releasedFlickOffset },
                    releasedFlickFraction = releasedFlickFraction,
                    isCircle = source?.isCircle == true,
                    mode = mode,
                    progress = { progress.value },
                    backProgress = { backProgress },
                    controlsShown = controlsShown,
                    onRevealControls = { chromeVisible = true },
                    onClose = ::exit,
                ) { page ->
                    ViewerPage(
                        media = medias[page],
                        page = page,
                        pagerState = pagerState,
                        pages = pages,
                        contentDescription = contentDescription(medias[page]),
                        imageLoader = imageLoader,
                        imageRequestBuilder = imageRequestBuilder,
                        onClick = { chromeVisible = !chromeVisible },
                    )
                }
            }
        }
    }
}

/** What the viewer read once, when it opened. */
private class OpenState(
    val heroBitmap: ImageBitmap?,
    val transition: ImageViewerTransition,
    val sourceBounds: Rect?,
    val orientation: Int,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
)

/** What each composed page publishes to the window: its zoom, its flick, and whether its image failed. */
@Stable
internal class PagesState {
    val zoom = mutableStateMapOf<Int, ZoomableState>()
    val flick = mutableStateMapOf<Int, FlickToDismissState>()

    /** How far each photo is drawn from its place by its flick; read when the flight home starts. */
    val flickDrawnOffset = HashMap<Int, Offset>()
    val failed = mutableStateMapOf<Int, Boolean>()
}

/**
 * What the viewer window draws: the backdrop, the pager of [pageCount] photos drawn by [page], the
 * hero flight and Close. Window-less, so it is tested on its own.
 */
@OptIn(ExperimentalTelephotoApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun ViewerContent(
    pageCount: Int,
    pagerState: PagerState,
    pages: PagesState,
    heroBitmap: ImageBitmap?,
    heroSourceBounds: () -> Rect?,
    isCircle: Boolean,
    releasedFlickOffset: () -> Offset = { Offset.Zero },
    releasedFlickFraction: Float? = null,
    mode: ViewerMode,
    progress: () -> Float,
    backProgress: () -> Float,
    controlsShown: Boolean,
    onRevealControls: () -> Unit,
    onClose: () -> Unit,
    page: @Composable (index: Int) -> Unit,
) {
    var pressedPointers by remember { mutableIntStateOf(0) }
    val currentZoomFraction = pages.zoom[pagerState.currentPage]?.zoomFraction
    // How far the current photo is flicked away, read at draw time.
    val flickFraction = {
        (releasedFlickFraction ?: pages.flick[pagerState.currentPage]?.offsetFraction ?: 0f).coerceIn(0f, 1f)
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            pressedPointers = event.changes.count { it.pressed }
                        }
                    }
                }.drawWithContent {
                    val backdrop = progress() * (1f - flickFraction()) * (1f - backProgress() / 2f)
                    drawRect(Color.Black.copy(alpha = backdrop.coerceIn(0f, 1f)))
                    drawContent()
                },
    ) {
        ImageViewerPager(
            pagerState = pagerState,
            pagingEnabled = mode == ViewerMode.Settled && isPagingEnabled(currentZoomFraction, pageCount, pressedPointers),
            controlsShown = controlsShown,
            chromeAlpha = { if (mode == ViewerMode.Settled) 1f - flickFraction() else 0f },
        ) { index ->
            // Each photo has its own flick, inside the pager: one around the whole pager took the
            // horizontal swipes too, so the photos could no longer be swiped (telephoto's own
            // gallery nests them the same way).
            // No rotation and no rubber banding: the photo flies home from where it is drawn, as a
            // straight frame that follows the finger.
            val flickState =
                rememberFlickToDismissState(
                    rotateOnDrag = false,
                    rubberBandingSpec = FlickToDismissState.RubberBandingSpec.Disabled,
                )
            // Where the photo sits before its flick moves it, and where the flick draws it.
            val placeInRoot = remember { arrayOf(Offset.Zero) }
            DisposableEffect(index, flickState) {
                pages.flick[index] = flickState
                onDispose { pages.flick.remove(index) }
            }
            var flickEnabled by remember { mutableStateOf(true) }
            flickEnabled =
                nextFlickEnabled(
                    current = flickEnabled,
                    pressedPointers = pressedPointers,
                    zoomFraction = pages.zoom[index]?.zoomFraction,
                    pagerAtRest = !pagerState.isScrollInProgress,
                )
            FlickToDismiss(
                state = flickState,
                enabled = mode == ViewerMode.Settled && flickEnabled,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { placeInRoot[0] = it.positionInRoot() }
                        .graphicsLayer {
                            val fade = if (mode == ViewerMode.Fading) progress() else 1f
                            val scale =
                                (if (mode == ViewerMode.Fading) FADE_MIN_SCALE + (1f - FADE_MIN_SCALE) * fade else 1f) *
                                    backScale(backProgress())
                            alpha = if (mode == ViewerMode.Flying) 0f else fade
                            scaleX = scale
                            scaleY = scale
                        },
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { pages.flickDrawnOffset[index] = it.positionInRoot() - placeInRoot[0] },
                ) { page(index) }
            }
        }

        if (mode == ViewerMode.Flying && heroBitmap != null) {
            HeroCanvas(
                bitmap = heroBitmap,
                sourceBounds = heroSourceBounds,
                isCircle = isCircle,
                progress = progress,
                fittedScale = { backScale(backProgress()) },
                fittedOffset = releasedFlickOffset,
            )
        }

        CloseButton(
            visible = controlsShown,
            onReveal = onRevealControls,
            onClose = onClose,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(ViewerControlsInsets)
                    .padding(ViewerControlInset),
        )
    }
}

/**
 * Photo [page] of the viewer: its own zoom, published to the window through [pages] and reset to
 * fit once the pager rests on another photo, its spinner, and its error with Retry.
 */
@OptIn(ExperimentalTelephotoApi::class)
@Composable
private fun ViewerPage(
    media: Media.Image,
    page: Int,
    pagerState: PagerState,
    pages: PagesState,
    contentDescription: String?,
    imageLoader: ImageLoader,
    imageRequestBuilder: ImageRequestBuilder,
    onClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // Pan inertia (iOS parity): telephoto flings a released pan with the platform spline decay, so
    // a zoomed image keeps gliding after a fast pan. Nothing here may consume pointer events or
    // reset the zoom on gesture end, and FlickToDismiss stays disabled while zoomed, or the glide
    // is cut short. The state is saveable: zoom and pan survive a recreation.
    val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 5f))
    var retry by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    DisposableEffect(page, zoomableState) {
        pages.zoom[page] = zoomableState
        onDispose { pages.zoom.remove(page) }
    }
    DisposableEffect(page, failed) {
        pages.failed[page] = failed
        onDispose { pages.failed.remove(page) }
    }
    val reset = shouldResetZoom(page, pagerState.settledPage, pagerState.isScrollInProgress)
    LaunchedEffect(reset) {
        if (reset) zoomableState.resetZoom(animationSpec = SnapSpec())
    }

    val cacheKey = remember(media) { media.cacheKey() }
    val zoomInLabel = stringResource(R.string.accessibility_image_zoom_in)
    val zoomOutLabel = stringResource(R.string.accessibility_image_zoom_out)
    val moveLeftLabel = stringResource(R.string.accessibility_image_move_left)
    val moveRightLabel = stringResource(R.string.accessibility_image_move_right)
    val moveUpLabel = stringResource(R.string.accessibility_image_move_up)
    val moveDownLabel = stringResource(R.string.accessibility_image_move_down)
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        key(retry) {
            val request =
                remember(media, cacheKey) {
                    imageRequestBuilder
                        .create(data = media.source.data, crossFade = false)
                        .apply { cacheKey?.let { placeholderMemoryCacheKey(MemoryCache.Key(it)) } }
                        // The decode size defaults to the viewer's canvas, which caps
                        // animated images (GIF) to the window; still images are sub-sampled
                        // from the disk cache at full resolution.
                        .listener(
                            onError = { _, _ -> failed = true },
                            onSuccess = { _, _ -> failed = false },
                        ).build()
                }
            val imageState = rememberZoomableImageState(zoomableState)
            var showSpinner by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(SPINNER_DELAY_MS)
                showSpinner = true
            }

            ZoomableAsyncImage(
                model = request,
                contentDescription = contentDescription,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { viewport = it.size }
                        .semantics {
                            customActions =
                                listOf(
                                    zoomAction(zoomInLabel, scope) { zoomableState.zoomBy(ZOOM_STEP) },
                                    zoomAction(zoomOutLabel, scope) { zoomableState.zoomBy(1f / ZOOM_STEP) },
                                    zoomAction(moveLeftLabel, scope) {
                                        zoomableState.panBy(Offset(viewport.width * PAN_STEP_FRACTION, 0f))
                                    },
                                    zoomAction(moveRightLabel, scope) {
                                        zoomableState.panBy(Offset(-viewport.width * PAN_STEP_FRACTION, 0f))
                                    },
                                    zoomAction(moveUpLabel, scope) {
                                        zoomableState.panBy(Offset(0f, viewport.height * PAN_STEP_FRACTION))
                                    },
                                    zoomAction(moveDownLabel, scope) {
                                        zoomableState.panBy(Offset(0f, -viewport.height * PAN_STEP_FRACTION))
                                    },
                                )
                        },
                state = imageState,
                imageLoader = imageLoader,
                onClick = { onClick() },
            )

            if (showSpinner && !failed && !imageState.isImageDisplayed && !imageState.isPlaceholderDisplayed) {
                OctopusProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        if (failed) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.error_unknown_formal),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = {
                        failed = false
                        retry++
                    },
                ) {
                    Text(text = stringResource(R.string.common_retry))
                }
            }
        }
    }
}

/** The key of this image's thumbnail in Coil's memory cache, or null for a local image. */
private fun Media.Image.cacheKey(): String? = (source.data as? String)?.getImageCacheKey()

@Composable
private fun HeroCanvas(
    bitmap: ImageBitmap,
    sourceBounds: () -> Rect?,
    isCircle: Boolean,
    progress: () -> Float,
    fittedScale: () -> Float,
    fittedOffset: () -> Offset,
) {
    val holder = remember { CoordinatesHolder() }
    Canvas(
        modifier =
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { holder.value = it },
    ) {
        val intrinsic = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
        val container = Rect(Offset.Zero, size)
        val coordinates = holder.value?.takeIf { it.isAttached }
        val local =
            sourceBounds()?.let { screen ->
                coordinates?.let { screenRectToLocal(screen) { point -> it.screenToLocal(point) } }
            }
        val fraction = progress()
        // The fitted end of the flight, shrunk like the zoomable layer when a back gesture is
        // what closes the viewer (1 otherwise), and moved to where a flick released the photo,
        // so the hand-off from that layer does not jump.
        val fittedIn = flightStartContainer(container, fittedScale(), fittedOffset())
        if (isHeroGeometryUsable(local, container, intrinsic) && local != null) {
            val frame = heroFrame(local, fittedIn, intrinsic, isCircle, fraction)
            val radius = min(frame.clipRect.width, frame.clipRect.height) * frame.cornerPercent / 100f
            val clip = Path().apply { addRoundRect(RoundRect(frame.clipRect, CornerRadius(radius))) }
            clipPath(clip) { drawBitmapIn(bitmap, frame.imageRect, alpha = 1f) }
        } else {
            // The thumbnail could not be mapped into this window: fade the fitted image instead.
            drawBitmapIn(bitmap, fitRect(fittedIn, intrinsic), alpha = fraction)
        }
    }
}

/** The zoomable layer's scale for a back gesture at [backProgress] (1 at rest). */
private fun backScale(backProgress: Float): Float = 1f - (1f - BACK_MIN_SCALE) * backProgress

private fun DrawScope.drawBitmapIn(
    bitmap: ImageBitmap,
    rect: Rect,
    alpha: Float,
) {
    drawImage(
        image = bitmap,
        dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
        dstSize = IntSize(rect.width.roundToInt().coerceAtLeast(1), rect.height.roundToInt().coerceAtLeast(1)),
        alpha = alpha.coerceIn(0f, 1f),
    )
}

@Composable
private fun CloseButton(
    visible: Boolean,
    onReveal: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClose,
        modifier =
            modifier
                .size(ViewerControlSize)
                // Hidden, a tap reveals the controls instead of closing. Accessibility services
                // click through the semantics action, which this does not intercept.
                .pointerInput(visible) {
                    if (visible) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial).consume()
                        val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        if (up != null) {
                            up.consume()
                            onReveal()
                        }
                    }
                }.onFocusChanged { if (it.isFocused) onReveal() }
                // Skipping the drawing (not alpha 0) keeps the button in the semantics tree
                // and visible to accessibility services while the controls are hidden.
                .drawWithContent { if (visible) drawContent() },
        colors =
            IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.4f),
                contentColor = Color.White,
            ),
        shape = CircleShape,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.common_close),
        )
    }
}

private fun zoomAction(
    label: String,
    scope: CoroutineScope,
    block: suspend () -> Unit,
) = CustomAccessibilityAction(label) {
    scope.launch { block() }
    true
}

@Suppress("DEPRECATION")
private fun Window.applyViewerWindowStyle() {
    setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    // The viewer animates itself; the platform dialog animation would run on top of it.
    setWindowAnimations(0)
    clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    setDimAmount(0f)
    setBackgroundDrawableResource(android.R.color.transparent)
    statusBarColor = android.graphics.Color.TRANSPARENT
    navigationBarColor = android.graphics.Color.TRANSPARENT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        isStatusBarContrastEnforced = false
        isNavigationBarContrastEnforced = false
    }
}

/** The thumbnail's bitmap from Coil's memory cache, or null when it is not (or no longer) there. */
private fun heroBitmap(
    imageLoader: ImageLoader,
    cacheKey: String?,
): ImageBitmap? {
    val key = cacheKey ?: return null
    val image = imageLoader.memoryCache?.get(MemoryCache.Key(key))?.image as? BitmapImage
    return image?.bitmap?.asImageBitmap()
}

private fun animatorDurationScale(context: Context): Float =
    runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
