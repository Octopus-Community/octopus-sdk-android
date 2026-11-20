package com.octopuscommunity.sdk.ui.posts.medias

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.test.mock.MockPosts
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerDestination
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusContent
import org.koin.core.parameter.parametersOf

/**
 * Full-screen view of the image attached to the post [postId].
 *
 * The image opens in the SDK's full-screen zoom viewer, in its own window over this destination;
 * closing the viewer navigates up.
 */
@Composable
fun PostMediasScreen(
    navController: NavHostController,
    postId: String,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
) = OctopusContent(navController) {
    ImageViewerDestination(
        modifier = modifier,
        navController = navController,
        viewModel =
            octopusViewModel<PostMediasViewModel>(
                key = "postMedias_$postId",
                parameters = { parametersOf(postId) },
            ),
        selectedIndex = selectedIndex,
    )
}

@Preview
@Composable
private fun PostMediasScreenPreview() {
    OctopusTheme {
        PostMediasScreen(
            navController = rememberNavController(),
            postId = MockPosts.all.random().id,
        )
    }
}
