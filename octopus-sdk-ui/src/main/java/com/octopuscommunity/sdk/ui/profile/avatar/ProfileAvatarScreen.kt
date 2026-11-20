package com.octopuscommunity.sdk.ui.profile.avatar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.test.mock.MockProfiles
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerDestination
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusContent
import org.koin.core.parameter.parametersOf

/**
 * Full-screen view of the profile picture of the user [userId].
 *
 * The image opens in the SDK's full-screen zoom viewer, in its own window over this destination;
 * closing the viewer navigates up.
 */
@Composable
fun ProfileAvatarScreen(
    navController: NavHostController,
    userId: String,
    modifier: Modifier = Modifier,
) = OctopusContent(navController) {
    ImageViewerDestination(
        modifier = modifier,
        navController = navController,
        viewModel =
            octopusViewModel<ProfileAvatarViewModel>(
                key = "profileAvatar_$userId",
                parameters = { parametersOf(userId) },
            ),
        contentDescription = stringResource(R.string.accessibility_profile_picture),
    )
}

@Preview
@Composable
private fun ProfileAvatarScreenPreview() {
    OctopusTheme {
        ProfileAvatarScreen(
            navController = rememberNavController(),
            userId = MockProfiles.all.random().userId,
        )
    }
}
