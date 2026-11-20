package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel

/**
 * Navigation icon types available for the top app bar.
 */
enum class NavigationIconType {
    /** Back arrow icon for standard navigation */
    Back,

    /** Close (X) icon for modal dismissal */
    Close
}

/**
 * Octopus-themed scaffold with automatic navigation handling.
 *
 * This convenience function automatically configures the navigation icon and back behavior
 * based on the navigation controller's back stack state. It shows a back button when there's
 * a previous destination and handles navigation automatically.
 *
 * @param navController The navigation controller for handling back navigation
 * @param modifier [Modifier] to be applied to the scaffold
 * @param title Optional title text to display in the top app bar
 * @param actions Composable content for action buttons in the app bar
 * @param dropdownMenuItems List of items to display in the overflow dropdown menu
 * @param content The main content of the screen, receiving inner padding from the scaffold
 */
@Composable
fun OctopusScaffold(
    navController: NavHostController,
    viewModel: OctopusViewModel,
    title: String?,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    dropdownMenuItems: List<OctopusMenuItem> = listOf(),
    content: @Composable (innerPadding: PaddingValues) -> Unit
) = OctopusScaffold(
    modifier = modifier,
    viewModel = viewModel,
    navigationIcon = navController.previousBackStackEntry?.let { NavigationIconType.Back },
    onBack = navController::navigateUp,
    title = title,
    actions = actions,
    dropdownMenuItems = dropdownMenuItems,
    content = content
)

/**
 * Octopus-themed scaffold with manual navigation handling.
 *
 * This is the base scaffold implementation that provides full control over the navigation
 * icon type and back behavior. Use this when you need custom navigation logic or when
 * not using the Navigation Component.
 *
 * @param modifier [Modifier] to be applied to the scaffold
 * @param navigationIcon Type of navigation icon to display, or null for no icon
 * @param onBack Callback invoked when the navigation icon is clicked
 * @param title Optional title text to display in the top app bar
 * @param actions Composable content for action buttons in the app bar
 * @param dropdownMenuItems List of items to display in the overflow dropdown menu
 * @param content The main content of the screen, receiving inner padding from the scaffold
 */
@Composable
fun OctopusScaffold(
    viewModel: OctopusViewModel,
    title: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: NavigationIconType? = NavigationIconType.Back,
    actions: @Composable () -> Unit = {},
    dropdownMenuItems: List<OctopusMenuItem> = listOf(),
    content: @Composable (innerPadding: PaddingValues) -> Unit
) {
    @OptIn(ExperimentalMaterial3Api::class)
    OctopusScaffold(
        modifier = modifier,
        viewModel = viewModel,
        topBar = { contentVisible ->
            OctopusTopAppBar(
                navigationIcon = navigationIcon,
                onNavigationIconClick = onBack,
                title = title,
                actions = actions,
                dropdownMenuItems = dropdownMenuItems,
                contentVisible = contentVisible
            )
        },
        content = content
    )
}

/**
 * Octopus-themed scaffold with custom top bar.
 *
 * This minimal scaffold variant allows complete customization of the top bar
 * while maintaining the consistent Octopus theming and background. Use this
 * when you need a completely custom top bar implementation.
 *
 * @param modifier [Modifier] to be applied to the scaffold
 * @param topBar Composable content for the top app bar area
 * @param content The main content of the screen, receiving inner padding from the scaffold
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OctopusScaffold(
    viewModel: OctopusViewModel,
    modifier: Modifier = Modifier,
    topBar: @Composable (contentVisible: Boolean) -> Unit,
    content: @Composable (innerPadding: PaddingValues) -> Unit
) {
    val blockingContent by viewModel.isBlockingContent.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(OctopusTheme.colorScheme.background),
        containerColor = OctopusTheme.colorScheme.background,
        topBar = { topBar(!blockingContent) }
    ) { innerPadding ->
        content(innerPadding)
    }
}