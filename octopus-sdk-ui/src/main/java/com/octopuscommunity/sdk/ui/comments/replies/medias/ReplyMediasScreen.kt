package com.octopuscommunity.sdk.ui.comments.replies.medias

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.test.mock.MockReplies
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerDestination
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusContent
import org.koin.core.parameter.parametersOf

/**
 * Full-screen view of the image attached to the reply [replyId].
 *
 * The image opens in the SDK's full-screen zoom viewer, in its own window over this destination;
 * closing the viewer navigates up.
 */
@Composable
fun ReplyMediasScreen(
    navController: NavHostController,
    replyId: String,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
) = OctopusContent(navController) {
    ImageViewerDestination(
        modifier = modifier,
        navController = navController,
        viewModel =
            octopusViewModel<ReplyMediasViewModel>(
                key = "replyMedias_$replyId",
                parameters = { parametersOf(replyId) },
            ),
        selectedIndex = selectedIndex,
    )
}

@Preview
@Composable
private fun ReplyMediasScreenPreview() {
    OctopusTheme {
        ReplyMediasScreen(
            navController = rememberNavController(),
            replyId = MockReplies.all.random().id,
        )
    }
}
