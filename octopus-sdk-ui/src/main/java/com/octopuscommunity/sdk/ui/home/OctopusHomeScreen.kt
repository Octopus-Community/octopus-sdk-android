package com.octopuscommunity.sdk.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ClientPost
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.NavigationIconType
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusNavigationHandler
import com.octopuscommunity.sdk.ui.components.OctopusScaffold
import com.octopuscommunity.sdk.ui.components.UrlOpeningStrategy
import com.octopuscommunity.sdk.ui.octopusComposables

/**
 * Main home screen with the original Android navigation options.
 *
 * Keeps the existing source and binary entry point. For iOS-style host actions or navigation mode
 * selection, use the overload with `navigationMode` and `navBarLeadingAction`.
 *
 * **Note:** For integrated navigation only. You need to include the Octopus Navigation by calling
 * [NavGraphBuilder.octopusComposables] inside your NavHost to use this Composable.
 *
 * Missing optional login or host profile-edit callbacks never prevent opening a screen. A requested
 * action without its callback logs the missing parameter once, then throws the documented
 * [com.octopuscommunity.sdk.MissingOnNavigateToLoginException] or
 * [com.octopuscommunity.sdk.MissingOnNavigateToProfileEditException] at action time.
 *
 * @param navController Navigation controller for handling screen transitions
 * @param modifier Optional [Modifier] for customizing the layout
 * @param backIcon Whether to show the back navigation icon (automatically determined from navigation state)
 * @param titleCentered Whether to center the title in the top app bar
 * @param titleText Optional title text override. When non-null (including empty text), takes
 * precedence over both the explicit and themed logo. Otherwise a logo or the SDK default is used.
 * @param logo Optional logo override for the top app bar. When null, falls back to
 * [OctopusImages.logo] from [OctopusTheme]. Used only when [titleText] is null.
 * @param contentPadding Padding values for the bottom floating actions area
 * @param onNavigateToLogin Called when OctopusSDK needs a logged in user. This should launch the
 * navigation to your login composable/screen. Once your login process is done, you should call
 * [OctopusSDK.connectUser] with your retrieved user's information.
 * @param onNavigateToProfileEdit Called when the user tries to modify fields related to their
 * profile. When this block is called, you should open the profile edition screen for the specific
 * field. The parameter indicates which field should be focused, or null for general editing.
 * @param onNavigateToClientObject Called when a user taps on the Button with the label
 * [ClientPost.viewObjectButtonText] that is displayed on a post related to a client object
 * (article, product, etc.). The parameter is the object id you set when creating the post.
 * This allows deep linking back to your app's content.
 * @param onNavigateToUrl Called when a user taps a link inside Octopus content, with that URL.
 * Return [UrlOpeningStrategy.HandledByApp] when your app opened it, or
 * [UrlOpeningStrategy.HandledByOctopus] (default) to let the SDK open it.
 * @param onNavigateToProfile Called when a user taps a member's profile (including their own),
 * with that member's `clientUserId`, so the host can open its own profile screen. Unified Profile
 * requires BOTH the community's backend flag (`CommunityConfig.exposeClientUserId`) and this
 * callback. When active, members without a client id (guest / BO / admin) open the Octopus activity
 * screen; this callback never receives a `null` id. With the flag off or this callback absent,
 * the SDK keeps native profiles.
 * @param onBack Callback invoked when the back navigation icon is clicked
 * @param leadingNavigationIcon Optional override for the leading navigation icon shown on the home
 * root. When non-null, the root displays this icon ([NavigationIconType.Back] or
 * [NavigationIconType.Close]) regardless of [backIcon], and [onBack] is invoked when it is tapped.
 * Use it when you host [OctopusHomeScreen] somewhere the SDK cannot dismiss itself (e.g. inside a
 * modal bottom sheet or dialog) and need to expose a close affordance. When null (default), the
 * leading icon falls back to a back arrow gated by [backIcon].
 */
@Composable
fun OctopusHomeScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    backIcon: Boolean = true,
    titleCentered: Boolean = false,
    titleText: String? = null,
    logo: (@Composable () -> Painter)? = null,
    contentPadding: PaddingValues = OctopusHomeDefaults.contentPadding(),
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToProfileEdit: ((fieldToEdit: ProfileField?) -> Unit)? = null,
    onNavigateToClientObject: ((id: String) -> Unit)? = null,
    onNavigateToUrl: ((url: String) -> UrlOpeningStrategy) = { UrlOpeningStrategy.HandledByOctopus },
    onNavigateToProfile: ((clientUserId: String) -> Unit)? = null,
    onBack: () -> Unit = {
        navController.navigateUp()
    },
    leadingNavigationIcon: NavigationIconType? = null,
) = OctopusHomeScreen(
    navController = navController,
    modifier = modifier,
    backIcon = backIcon,
    titleCentered = titleCentered,
    titleText = titleText,
    logo = logo,
    contentPadding = contentPadding,
    onNavigateToLogin = onNavigateToLogin,
    onNavigateToProfileEdit = onNavigateToProfileEdit,
    onNavigateToClientObject = onNavigateToClientObject,
    onNavigateToUrl = onNavigateToUrl,
    onNavigateToProfile = onNavigateToProfile,
    onBack = onBack,
    leadingNavigationIcon = leadingNavigationIcon,
    navigationMode = OctopusNavigationMode.Automatic,
    navBarLeadingAction = null,
)

/**
 * Main home screen composable for the Octopus SDK.
 *
 * **Note:** For integrated navigation only. You need to include the Octopus Navigation by calling
 * [NavGraphBuilder.octopusComposables] inside your NavHost to use this Composable.
 *
 * This screen serves as the primary entry point for the Octopus community experience,
 * displaying feeds of posts with filtering capabilities and creation options. The screen
 * includes a top app bar with optional back navigation and floating actions for user interaction.
 *
 * @param navController Navigation controller for handling screen transitions
 * @param modifier Optional [Modifier] for customizing the layout
 * @param backIcon Whether to show the back navigation icon (automatically determined from navigation state)
 * @param titleCentered Whether to center the title in the top app bar
 * @param titleText Optional title text override. When non-null (including empty text), takes
 * precedence over both the explicit and themed logo. Otherwise a logo or the SDK default is used.
 * @param logo Optional logo override for the top app bar. When null, falls back to
 * [OctopusImages.logo] from [OctopusTheme]. Used only when [titleText] is null.
 * @param contentPadding Padding values for the bottom floating actions area
 * @param onNavigateToLogin Called when OctopusSDK needs a logged in user. This should launch the
 * navigation to your login composable/screen. Once your login process is done, you should call
 * [OctopusSDK.connectUser] with your retrieved user's information.
 * @param onNavigateToProfileEdit Called when the user tries to modify fields related to their
 * profile. When this block is called, you should open the profile edition screen for the specific
 * field. The parameter indicates which field should be focused, or null for general editing.
 * @param onNavigateToClientObject Called when a user taps on the Button with the label
 * [ClientPost.viewObjectButtonText] that is displayed on a post related to a client object
 * (article, product, etc.). The parameter is the object id you set when creating the post.
 * This allows deep linking back to your app's content.
 * @param onNavigateToUrl Called when a user taps a link inside Octopus content, with that URL.
 * Return [UrlOpeningStrategy.HandledByApp] when your app opened it, or
 * [UrlOpeningStrategy.HandledByOctopus] (default) to let the SDK open it.
 * @param onNavigateToProfile Called when a user taps a member's profile (including their own),
 * with that member's `clientUserId`, so the host can open its own profile screen. Unified Profile
 * requires BOTH the community's backend flag (`CommunityConfig.exposeClientUserId`) and this
 * callback. When active, members without a client id (guest / BO / admin) open the Octopus activity
 * screen; this callback never receives a `null` id. With the flag off or this callback absent,
 * the SDK keeps native profiles.
 * @param onBack Callback invoked when the back navigation icon is clicked
 * @param leadingNavigationIcon Optional override for the leading navigation icon shown on the home
 * root. When non-null, the root displays this icon ([NavigationIconType.Back] or
 * [NavigationIconType.Close]) regardless of [backIcon], and [onBack] is invoked when it is tapped.
 * Use it when you host [OctopusHomeScreen] somewhere the SDK cannot dismiss itself (e.g. inside a
 * modal bottom sheet or dialog) and need to expose a close affordance. When null (default), the
 * leading icon falls back to a back arrow gated by [backIcon].
 * @param navigationMode Navigation container preference, matching iOS. It has no effect on Android
 * and is kept for cross-platform parity: both modes keep the supplied [navController] and its
 * stack, including when hosted in a modal.
 * @param navBarLeadingAction Host-driven close/back action. Overrides [leadingNavigationIcon] and
 * [backIcon], invokes its own `onTap` callback and centers the title, matching iOS. Null keeps the
 * existing icon, title placement and [onBack] behavior.
 */
@Composable
fun OctopusHomeScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    backIcon: Boolean = true,
    titleCentered: Boolean = false,
    titleText: String? = null,
    logo: (@Composable () -> Painter)? = null,
    contentPadding: PaddingValues = OctopusHomeDefaults.contentPadding(),
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToProfileEdit: ((fieldToEdit: ProfileField?) -> Unit)? = null,
    onNavigateToClientObject: ((id: String) -> Unit)? = null,
    onNavigateToUrl: ((url: String) -> UrlOpeningStrategy) = { UrlOpeningStrategy.HandledByOctopus },
    onNavigateToProfile: ((clientUserId: String) -> Unit)? = null,
    onBack: () -> Unit = {
        navController.navigateUp()
    },
    leadingNavigationIcon: NavigationIconType? = null,
    navigationMode: OctopusNavigationMode = OctopusNavigationMode.Automatic,
    navBarLeadingAction: OctopusNavBarLeadingAction? = null,
) = OctopusNavigationHandler(
    onNavigateToLogin = onNavigateToLogin,
    onNavigateToProfileEdit = onNavigateToProfileEdit,
    onNavigateToClientObject = onNavigateToClientObject,
    onNavigateToUrl = onNavigateToUrl,
    onNavigateToProfile = onNavigateToProfile,
) {
    OctopusHomeScreen(
        modifier = modifier,
        navController = navController,
        backIcon = backIcon,
        leadingNavigationIcon = leadingNavigationIcon,
        navBarLeadingAction = navBarLeadingAction,
        titleCentered = titleCentered,
        titleText = titleText,
        logo = logo,
        contentPadding = contentPadding,
        onBack = onBack,
    )
}

/**
 * Main home screen composable for the Octopus SDK.
 *
 * **Note:** For integrated navigation only. You need to include the Octopus Navigation by calling
 * [NavGraphBuilder.octopusComposables] inside your NavHost to use this Composable.
 *
 * This screen serves as the primary entry point for the Octopus community experience,
 * displaying feeds of posts with filtering capabilities and creation options. The screen
 * includes a top app bar with optional back navigation and floating actions for user interaction.
 *
 * @param navController Navigation controller for handling screen transitions
 * @param modifier Optional [Modifier] for customizing the layout
 * @param backIcon Whether to show the back navigation icon (automatically determined from navigation state)
 * @param titleCentered Whether to center the title in the top app bar
 * @param titleText Optional title text override. When non-null (including empty text), takes
 * precedence over both the explicit and themed logo. Otherwise a logo or the SDK default is used.
 * @param logo Optional logo override for the top app bar. When null, falls back to
 * [OctopusImages.logo] from [OctopusTheme]. Used only when [titleText] is null.
 * @param contentPadding Padding values for the bottom floating actions area
 * @param onBack Callback invoked when the back navigation icon is clicked
 * @param leadingNavigationIcon Optional override for the leading navigation icon on the home root.
 * When non-null, the root displays this icon regardless of [backIcon]; [onBack] handles the tap.
 */
@Composable
internal fun OctopusHomeScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    backIcon: Boolean = true,
    titleCentered: Boolean = false,
    titleText: String? = null,
    logo: (@Composable () -> Painter)? = null,
    contentPadding: PaddingValues = OctopusHomeDefaults.contentPadding(),
    onBack: () -> Unit = {
        navController.navigateUp()
    },
    leadingNavigationIcon: NavigationIconType? = null,
    navBarLeadingAction: OctopusNavBarLeadingAction? = null,
) = OctopusContent(navController) {
    val viewModel = octopusViewModel<OctopusHomeViewModel>()

    OctopusScaffold(
        modifier = modifier,
        viewModel = viewModel,
        topBar = { contentVisible ->
            OctopusHomeTopBar(
                titleCentered = titleCentered,
                titleText = titleText,
                logo = logo,
                backIcon = backIcon,
                leadingNavigationIcon = leadingNavigationIcon,
                navBarLeadingAction = navBarLeadingAction,
                onBack = onBack,
                contentVisible = contentVisible,
            )
        },
    ) { innerPadding ->
        OctopusHomeContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            navController = navController,
            contentPadding = contentPadding,
        )
    }
}

/**
 * Preview function for the Octopus home screen component.
 */
@Preview(showBackground = true)
@Composable
private fun OctopusHomeScreenPreview() {
    OctopusTheme {
        OctopusHomeScreen(navController = rememberNavController())
    }
}
