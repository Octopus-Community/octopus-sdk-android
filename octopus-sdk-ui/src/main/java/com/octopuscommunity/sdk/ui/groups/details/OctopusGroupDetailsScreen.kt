package com.octopuscommunity.sdk.ui.groups.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.test.mock.MockTopics
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.components.OctopusNavigationHandler
import com.octopuscommunity.sdk.ui.components.UrlOpeningStrategy

/**
 * Public composable that displays a specific group/topic in bridge mode.
 *
 * Bridge mode opens the group feed in isolation — the user cannot navigate to
 * other groups or the community home. A close button is displayed to return
 * to the client app.
 *
 * This is the group equivalent of [OctopusPostDetailsScreen].
 *
 * @param navController Navigation controller for handling back navigation
 * @param groupId Unique identifier of the group/topic to display
 * @param modifier Optional [Modifier] for the root composable
 */
@Composable
fun OctopusGroupDetailsScreen(
    navController: NavHostController,
    groupId: String,
    modifier: Modifier = Modifier,
) = GroupDetailsScreen(
    modifier = modifier,
    navController = navController,
    groupId = groupId,
    origin = OctopusDestination.Origin.CLIENT_APP,
)

/**
 * Public composable that displays a specific group/topic in bridge mode
 * with navigation callbacks for SSO integration.
 *
 * Bridge mode opens the group feed in isolation — the user cannot navigate to
 * other groups or the community home. A close button is displayed to return
 * to the client app.
 *
 * This is the group equivalent of [OctopusPostDetailsContent].
 *
 * @param navController Navigation controller for handling back navigation
 * @param groupId Unique identifier of the group/topic to display
 * @param modifier Optional [Modifier] for the root composable
 * @param onNavigateToLogin Callback when login is required
 * @param onNavigateToProfileEdit Callback when profile editing is requested
 * @param onNavigateToUrl Callback when a URL is tapped
 * @param onNavigateToProfile Called when a user taps a member's profile (including their own),
 * with that member's `clientUserId`, so the host can open its own profile screen. Unified Profile
 * requires BOTH the community's backend flag (`CommunityConfig.exposeClientUserId`) and this
 * callback. When active, members without a client id (guest / BO / admin) open the Octopus activity
 * screen; this callback never receives a `null` id. With the flag off or this callback absent,
 * the SDK keeps native profiles.
 * @param onBack Called when the user navigates back (Android system back) while this screen is the
 * root of the SDK's back stack — i.e. bridge mode, where you embed this composable directly (not via
 * `octopusComposables`) so there is nothing left to pop internally. Use it to dismiss your own
 * container (e.g. pop your route or finish the screen). If the user has navigated deeper inside the
 * SDK from here, back pops the internal back stack instead and this is not called.
 */
@Composable
fun OctopusGroupDetailsContent(
    navController: NavHostController,
    groupId: String,
    modifier: Modifier = Modifier,
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToProfileEdit: ((fieldToEdit: ProfileField?) -> Unit)? = null,
    onNavigateToUrl: ((url: String) -> UrlOpeningStrategy) = { UrlOpeningStrategy.HandledByOctopus },
    onNavigateToProfile: ((clientUserId: String) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) = OctopusNavigationHandler(
    onNavigateToLogin = onNavigateToLogin,
    onNavigateToProfileEdit = onNavigateToProfileEdit,
    onNavigateToUrl = onNavigateToUrl,
    onNavigateToProfile = onNavigateToProfile,
    onBack = onBack,
) {
    GroupDetailsContent(
        modifier = modifier,
        navController = navController,
        groupId = groupId,
        origin = OctopusDestination.Origin.CLIENT_APP,
    )
}

@Preview
@Composable
private fun OctopusGroupDetailsScreenPreview() {
    OctopusTheme {
        OctopusGroupDetailsScreen(
            navController = rememberNavController(),
            groupId = MockTopics.default.id,
        )
    }
}
