package com.octopuscommunity.sdk.ui.common.medias

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerDestination

/**
 * Former full-screen media screen. It now shows the SDK's full-screen zoom viewer, in its own
 * window, and navigates up when the viewer closes; [title] is ignored — the viewer has no top bar.
 */
@Deprecated(
    message =
        "The media destinations now open the SDK's full-screen zoom viewer. Use PostMediasScreen, " +
            "CommentMediasScreen, ReplyMediasScreen or ProfileAvatarScreen, matching the content type " +
            "the MediasViewModel was built for.",
    level = DeprecationLevel.WARNING,
)
@Composable
fun MediasScreen(
    navController: NavHostController,
    viewModel: MediasViewModel,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    @Suppress("UNUSED_PARAMETER") title: String? = "",
) = ImageViewerDestination(
    navController = navController,
    viewModel = viewModel,
    modifier = modifier,
    selectedIndex = selectedIndex,
)

/**
 * Former body of [MediasScreen]. It now shows the SDK's full-screen zoom viewer, in its own window,
 * and navigates up when the viewer closes; the viewer toggles its own controls, so
 * [onToggleTopBarVisibility] is never called.
 */
@Deprecated(
    message =
        "The media destinations now open the SDK's full-screen zoom viewer. Use PostMediasScreen, " +
            "CommentMediasScreen, ReplyMediasScreen or ProfileAvatarScreen, matching the content type " +
            "the MediasViewModel was built for.",
    level = DeprecationLevel.WARNING,
)
@Composable
fun MediasContent(
    navController: NavHostController,
    viewModel: MediasViewModel,
    @Suppress("UNUSED_PARAMETER") onToggleTopBarVisibility: () -> Unit,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
) = ImageViewerDestination(
    navController = navController,
    viewModel = viewModel,
    modifier = modifier,
    selectedIndex = selectedIndex,
)
