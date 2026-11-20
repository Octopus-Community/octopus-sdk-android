package com.octopuscommunity.sdk.ui.common.medias.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.comments.medias.CommentMediasViewModel
import com.octopuscommunity.sdk.ui.comments.replies.medias.ReplyMediasViewModel
import com.octopuscommunity.sdk.ui.common.medias.MediasViewModel
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusProgressIndicator
import com.octopuscommunity.sdk.ui.components.OctopusScreenContent
import com.octopuscommunity.sdk.ui.posts.blocks.CarouselPositions
import com.octopuscommunity.sdk.ui.posts.blocks.LocalCarouselPositions
import com.octopuscommunity.sdk.ui.posts.medias.PostMediasViewModel
import com.octopuscommunity.sdk.ui.profile.avatar.ProfileAvatarViewModel
import kotlinx.coroutines.delay
import org.koin.compose.currentKoinScope
import org.koin.core.parameter.parametersOf

/** The images handed over by the thumbnail that opens the viewer: all the photos of its content. */
@Stable
internal class ImageViewerOpenRequest(
    val medias: List<Media.Image>,
    val source: ImageViewerSource?,
)

/**
 * Opens and closes the full-screen image viewer of one screen. Only the [key] is saved, with the
 * page being read; the [request] (the thumbnail's images and position) lives as long as the
 * composition, so a restored viewer reloads its images from the repositories and opens without a
 * hero transition, on the page it was left on. A post's closing page goes to [carouselPositions].
 */
@Stable
internal class ImageViewerController(
    private val keyState: MutableState<ImageViewerKey?>,
    private val carouselPositions: CarouselPositions?,
) {
    /** Key of the open viewer, or null when none is open. */
    val key: ImageViewerKey? get() = keyState.value

    /** What the thumbnail handed over when the viewer opened; null after a restoration. */
    var request: ImageViewerOpenRequest? by mutableStateOf(null)
        private set

    /**
     * Opens the viewer on photo [ImageViewerKey.index] of [medias], the photos of [key]'s content.
     * Ignored while a viewer is already open: one opening at a time.
     */
    fun open(
        key: ImageViewerKey,
        medias: List<Media.Image>,
        source: ImageViewerSource?,
    ) {
        if (keyState.value != null) return
        request = medias.takeIf { it.isNotEmpty() }?.let { ImageViewerOpenRequest(medias = it, source = source) }
        keyState.value = key
    }

    /** Opens the viewer on [key]'s single image [media]. */
    fun open(
        key: ImageViewerKey,
        media: Media.Image?,
        source: ImageViewerSource?,
    ) = open(key = key, medias = listOfNotNull(media), source = source)

    /** Saves [index] as the page being read, so a restored viewer reopens on it. */
    fun onPageChanged(index: Int) {
        keyState.value = keyState.value?.copy(index = index) ?: return
    }

    /**
     * Records that the viewer starts closing on page [closingIndex], before its photo flies back or
     * fades: a post's carousel, hidden meanwhile, is moved to that photo.
     */
    fun onClosing(closingIndex: Int) {
        val key = keyState.value ?: return
        if (key.kind == ImageViewerKey.Kind.Post) carouselPositions?.set(key.id, closingIndex)
    }

    /** Records that the viewer closed. */
    fun onClosed() {
        request?.source?.isHidden = false
        request = null
        keyState.value = null
    }
}

/** The viewer controller of the enclosing Octopus screen, if any. */
internal val LocalImageViewerController = staticCompositionLocalOf<ImageViewerController?> { null }

/**
 * Opens the viewer on [key] through this controller, or runs [fallback] (typically navigating to
 * the matching media destination) when there is no controller — a card rendered outside an
 * Octopus screen.
 */
internal fun ImageViewerController?.openOrElse(
    key: ImageViewerKey,
    media: Media.Image?,
    source: ImageViewerSource?,
    fallback: () -> Unit,
) = openOrElse(key = key, medias = listOfNotNull(media), source = source, fallback = fallback)

/** [openOrElse] for content holding several photos, [medias]. */
internal fun ImageViewerController?.openOrElse(
    key: ImageViewerKey,
    medias: List<Media.Image>,
    source: ImageViewerSource?,
    fallback: () -> Unit,
) {
    if (this == null) fallback() else open(key = key, medias = medias, source = source)
}

/**
 * Hosts the full-screen image viewer for [content]: provides [LocalImageViewerController] and
 * shows the viewer in its own window when one is open. Nested hosts reuse the outer controller.
 */
@Composable
internal fun ImageViewerHost(content: @Composable () -> Unit) {
    if (LocalImageViewerController.current != null) {
        content()
        return
    }
    val keyState =
        rememberSaveable(stateSaver = ImageViewerKeySaver) {
            mutableStateOf<ImageViewerKey?>(null)
        }
    // The carousels' pages live above the lazy lists and the screens, so a card keeps its page when
    // recycled, the viewer follows it, and a post's carousel shows the same photo on every screen of
    // the SDK host. Outside a host, this viewer keeps its own.
    val koinScope = currentKoinScope()
    val carouselPositions = remember(koinScope) { koinScope.getOrNull<CarouselPositions>() ?: CarouselPositions() }
    val controller = remember(keyState, carouselPositions) { ImageViewerController(keyState, carouselPositions) }
    CompositionLocalProvider(
        LocalImageViewerController provides controller,
        LocalCarouselPositions provides carouselPositions,
    ) {
        content()
        controller.key?.let { viewerKey ->
            // Not keyed on the index: it follows the page being read, which must not rebuild the viewer.
            key(viewerKey.kind, viewerKey.id) {
                StateDrivenImageViewer(
                    viewerKey = viewerKey,
                    request = controller.request,
                    onPageChanged = controller::onPageChanged,
                    onClosing = controller::onClosing,
                    onClosed = controller::onClosed,
                )
            }
        }
    }
}

@Composable
private fun StateDrivenImageViewer(
    viewerKey: ImageViewerKey,
    request: ImageViewerOpenRequest?,
    onPageChanged: (Int) -> Unit,
    onClosing: (Int) -> Unit,
    onClosed: () -> Unit,
) {
    // The media view models live as long as the viewer, not as long as the screen behind it.
    val owner =
        remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    val viewModel = mediasViewModelFor(viewerKey, owner)
    ImageViewerFor(
        viewModel = viewModel,
        index = viewerKey.index,
        initialMedias = request?.medias,
        source = request?.source,
        contentDescription = viewerContentDescription(viewerKey.kind),
        onPageChanged = onPageChanged,
        onClosing = onClosing,
        onClosed = onClosed,
        pending = {},
    )
}

@Composable
private fun mediasViewModelFor(
    viewerKey: ImageViewerKey,
    owner: ViewModelStoreOwner,
): MediasViewModel {
    val id = viewerKey.id
    return when (viewerKey.kind) {
        ImageViewerKey.Kind.Post ->
            octopusViewModel<PostMediasViewModel>(
                viewModelStoreOwner = owner,
                key = "postMedias_$id",
                parameters = { parametersOf(id) },
            )

        ImageViewerKey.Kind.Comment ->
            octopusViewModel<CommentMediasViewModel>(
                viewModelStoreOwner = owner,
                key = "commentMedias_$id",
                parameters = { parametersOf(id) },
            )

        ImageViewerKey.Kind.Reply ->
            octopusViewModel<ReplyMediasViewModel>(
                viewModelStoreOwner = owner,
                key = "replyMedias_$id",
                parameters = { parametersOf(id) },
            )

        ImageViewerKey.Kind.Avatar ->
            octopusViewModel<ProfileAvatarViewModel>(
                viewModelStoreOwner = owner,
                key = "profileAvatar_$id",
                parameters = { parametersOf(id) },
            )
    }
}

/** How the viewer describes a photo of [kind]'s content: its name, or « profile picture ». */
@Composable
private fun viewerContentDescription(kind: ImageViewerKey.Kind): (Media.Image) -> String? {
    val profilePicture = stringResource(R.string.accessibility_profile_picture)
    return { media ->
        when (kind) {
            ImageViewerKey.Kind.Avatar -> profilePicture
            else -> media.name.ifEmpty { null }
        }
    }
}

/**
 * Shows the images of [viewModel] in the full-screen viewer window, opening on image [index], and
 * closes it cleanly when the images are gone (deleted content) or never come (a restored viewer
 * whose content is not cached).
 *
 * @param initialMedias the images handed over by the thumbnail; null after a restoration.
 * @param onPageChanged called with the page the viewer settles on.
 * @param onClosing called with the page being read when the viewer starts closing.
 * @param pending shown behind the window-less wait of a restored viewer.
 */
@Composable
internal fun ImageViewerFor(
    viewModel: MediasViewModel,
    index: Int,
    initialMedias: List<Media.Image>?,
    source: ImageViewerSource?,
    contentDescription: (Media.Image) -> String?,
    onPageChanged: (Int) -> Unit,
    onClosing: (Int) -> Unit,
    onClosed: () -> Unit,
    pending: @Composable () -> Unit,
) {
    val state by viewModel.mediaState.collectAsStateWithLifecycle()
    val loaded = state.medias?.images
    var hadLoaded by remember { mutableStateOf(false) }
    if (!loaded.isNullOrEmpty() && !hadLoaded) hadLoaded = true
    // The page the viewer opens on; later pages only reach the saved key.
    val startIndex = remember { index }
    var timedOut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(IMAGE_VIEWER_RESTORE_TIMEOUT)
        timedOut = true
    }

    when (
        val content =
            resolveImageViewerContent(
                loaded = loaded,
                initial = initialMedias,
                hadLoaded = hadLoaded,
                timedOut = timedOut,
            )
    ) {
        ImageViewerContent.Pending -> pending()
        ImageViewerContent.Close -> LaunchedEffect(Unit) { onClosed() }
        is ImageViewerContent.Show ->
            OctopusImageViewerWindow(
                medias = content.medias,
                initialIndex = startIndex,
                source = source,
                restored = initialMedias == null,
                contentDescription = contentDescription,
                onPageChanged = onPageChanged,
                onClosing = onClosing,
                onDismissed = onClosed,
            )
    }
}

/**
 * The body of the four media destinations (post, comment, reply, avatar): a black screen under
 * the full-screen viewer window, which navigates up when the viewer closes. Kept for deep links,
 * host bridges that open a destination directly, and cards shown outside an Octopus screen.
 * In previews the image is drawn inline: a preview has no window to open.
 */
@Composable
internal fun ImageViewerDestination(
    navController: NavHostController,
    viewModel: MediasViewModel,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    contentDescription: String? = null,
) = OctopusScreenContent(
    modifier = modifier,
    navController = navController,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (LocalInspectionMode.current) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(R.drawable.ic_octopus_content_not_available),
                contentDescription = contentDescription,
                contentScale = ContentScale.Inside,
            )
        } else {
            ImageViewerFor(
                viewModel = viewModel,
                index = selectedIndex,
                initialMedias = null,
                source = null,
                contentDescription = { media -> contentDescription ?: media.name.ifEmpty { null } },
                // A destination is not restored on its page, and has no carousel to give it back to.
                onPageChanged = {},
                onClosing = {},
                onClosed = { viewModel.navigateUp() },
                pending = { OctopusProgressIndicator() },
            )
        }
    }
}
