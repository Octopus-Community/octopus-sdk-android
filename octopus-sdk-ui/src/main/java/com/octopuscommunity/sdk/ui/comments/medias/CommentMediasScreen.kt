package com.octopuscommunity.sdk.ui.comments.medias

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.test.mock.MockComments
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerDestination
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusContent
import org.koin.core.parameter.parametersOf

/**
 * Full-screen view of the image attached to the comment [commentId].
 *
 * The image opens in the SDK's full-screen zoom viewer, in its own window over this destination;
 * closing the viewer navigates up.
 */
@Composable
fun CommentMediasScreen(
    navController: NavHostController,
    commentId: String,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
) = OctopusContent(navController) {
    ImageViewerDestination(
        modifier = modifier,
        navController = navController,
        viewModel =
            octopusViewModel<CommentMediasViewModel>(
                key = "commentMedias_$commentId",
                parameters = { parametersOf(commentId) },
            ),
        selectedIndex = selectedIndex,
    )
}

@Preview
@Composable
private fun CommentMediasScreenPreview() {
    OctopusTheme {
        CommentMediasScreen(
            navController = rememberNavController(),
            commentId = MockComments.all.random().id,
        )
    }
}
