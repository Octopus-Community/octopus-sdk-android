package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource

internal val LocalOctopusTopAppBar = staticCompositionLocalOf<OctopusTopAppBar?> { null }

/**
 * Top app bar customization for Octopus SDK UI components.
 *
 * @property modifier The [Modifier] to be applied to this top app bar
 * @property title The title to be displayed in the top app bar
 * @property navigationIcon The navigation icon displayed at the start of the top app bar.
 * This should typically be an [IconButton] or [IconToggleButton].
 * @property actions The actions displayed at the end of the top app bar. This should typically be
 * [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @property colors [TopAppBarColors] that will be used to resolve the colors used for this top app
 * bar in different states. See [TopAppBarDefaults.topAppBarColors].
 */
data class OctopusTopAppBar
    @OptIn(ExperimentalMaterial3Api::class)
    constructor(
        val modifier: Modifier,
        val title: @Composable (text: String?) -> Unit,
        val navigationIcon: @Composable (type: NavigationIconType, onClick: () -> Unit) -> Unit,
        val actions: @Composable (mainActions: @Composable () -> Unit, dropdownMenuItems: List<OctopusMenuItem>) -> Unit,
        val colors: TopAppBarColors,
    )

/**
 * Represents a menu item for the top app bar dropdown menu.
 *
 * @property text The display text for the menu item
 * @property onClick Callback invoked when the menu item is clicked
 * @property leadingIcon Optional icon displayed at the start of the menu item
 * @property trailingIcon Optional icon displayed at the end of the menu item
 * @property enabled Whether the menu item is enabled and clickable
 */
data class OctopusMenuItem(
    val text: String,
    val onClick: () -> Unit,
    val leadingIcon: @Composable (() -> Unit)? = null,
    val trailingIcon: @Composable (() -> Unit)? = null,
    val enabled: Boolean = true,
)

/**
 * Octopus-themed top app bar component with customizable styling and actions.
 *
 * This component extends Material 3's TopAppBar with Octopus SDK theming and additional features
 * like dropdown menus and logo display.
 *
 * @param modifier [Modifier] to be applied to the top app bar
 * @param title Optional title text to display in the app bar
 * @param navigationIcon Type of navigation icon to display, or null for no icon
 * @param onNavigationIconClick Callback invoked when the navigation icon is clicked
 * @param actions Composable content for action buttons in the app bar
 * @param dropdownMenuItems List of items to display in the overflow dropdown menu
 * @param colors [TopAppBarColors] for customizing the app bar appearance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OctopusTopAppBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    navigationIcon: NavigationIconType? = NavigationIconType.Back,
    onNavigationIconClick: () -> Unit = {},
    actions: @Composable () -> Unit = {},
    dropdownMenuItems: List<OctopusMenuItem> = listOf(),
    colors: TopAppBarColors = OctopusTheme.topAppBar.colors,
    contentVisible: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) = OctopusTopAppBar(
    modifier = modifier,
    title = { OctopusTheme.topAppBar.title(if (contentVisible) title else "") },
    navigationIcon = navigationIcon,
    onNavigationIconClick = onNavigationIconClick,
    actions = actions,
    dropdownMenuItems = dropdownMenuItems,
    colors = colors,
    contentVisible = contentVisible,
    scrollBehavior = scrollBehavior,
)

/**
 * Octopus-themed top app bar component with customizable styling and actions.
 *
 * This component extends Material 3's TopAppBar with Octopus SDK theming and additional features
 * like dropdown menus and logo display.
 *
 * @param modifier [Modifier] to be applied to the top app bar
 * @param title Optional title text to display in the app bar
 * @param navigationIcon Type of navigation icon to display, or null for no icon
 * @param onNavigationIconClick Callback invoked when the navigation icon is clicked
 * @param actions Composable content for action buttons in the app bar
 * @param dropdownMenuItems List of items to display in the overflow dropdown menu
 * @param colors [TopAppBarColors] for customizing the app bar appearance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OctopusTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: NavigationIconType? = NavigationIconType.Back,
    onNavigationIconClick: () -> Unit = {},
    actions: @Composable () -> Unit = {},
    dropdownMenuItems: List<OctopusMenuItem> = listOf(),
    colors: TopAppBarColors = OctopusTheme.topAppBar.colors,
    contentVisible: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) = TopAppBar(
    modifier = modifier then OctopusTheme.topAppBar.modifier,
    title = title,
    navigationIcon = {
        if (navigationIcon != null) {
            OctopusTheme.topAppBar.navigationIcon(navigationIcon, onNavigationIconClick)
        }
    },
    actions = {
        if (contentVisible) OctopusTheme.topAppBar.actions(actions, dropdownMenuItems)
    },
    colors = colors.withOctopusDefaults(),
    scrollBehavior = scrollBehavior,
)

/**
 * Octopus-themed center-aligned top app bar component.
 *
 * This component extends Material 3's CenterAlignedTopAppBar with Octopus SDK theming,
 * centering the title between the navigation icon and actions.
 *
 * @param modifier [Modifier] to be applied to the top app bar
 * @param title Optional title text to display in the app bar
 * @param navigationIcon Type of navigation icon to display, or null for no icon
 * @param onNavigationIconClick Callback invoked when the navigation icon is clicked
 * @param actions Composable content for action buttons in the app bar
 * @param dropdownMenuItems List of items to display in the overflow dropdown menu
 * @param colors [TopAppBarColors] for customizing the app bar appearance
 * @param scrollBehavior Optional scroll behavior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OctopusCenteredTopAppBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    navigationIcon: NavigationIconType? = NavigationIconType.Back,
    onNavigationIconClick: () -> Unit = {},
    actions: @Composable () -> Unit = {},
    dropdownMenuItems: List<OctopusMenuItem> = listOf(),
    colors: TopAppBarColors = OctopusTheme.topAppBar.colors,
    contentVisible: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) = OctopusCenteredTopAppBar(
    modifier = modifier,
    title = { OctopusTheme.topAppBar.title(if (contentVisible) title else "") },
    navigationIcon = navigationIcon,
    onNavigationIconClick = onNavigationIconClick,
    actions = actions,
    dropdownMenuItems = dropdownMenuItems,
    colors = colors,
    contentVisible = contentVisible,
    scrollBehavior = scrollBehavior,
)

/**
 * Octopus-themed center-aligned top app bar component.
 *
 * This component extends Material 3's CenterAlignedTopAppBar with Octopus SDK theming,
 * centering the title between the navigation icon and actions.
 *
 * @param modifier [Modifier] to be applied to the top app bar
 * @param title Composable content for the title
 * @param navigationIcon Type of navigation icon to display, or null for no icon
 * @param onNavigationIconClick Callback invoked when the navigation icon is clicked
 * @param actions Composable content for action buttons in the app bar
 * @param dropdownMenuItems List of items to display in the overflow dropdown menu
 * @param colors [TopAppBarColors] for customizing the app bar appearance
 * @param scrollBehavior Optional scroll behavior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OctopusCenteredTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: NavigationIconType? = NavigationIconType.Back,
    onNavigationIconClick: () -> Unit = {},
    actions: @Composable () -> Unit = {},
    dropdownMenuItems: List<OctopusMenuItem> = listOf(),
    colors: TopAppBarColors = OctopusTheme.topAppBar.colors,
    contentVisible: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) = CenterAlignedTopAppBar(
    modifier = modifier then OctopusTheme.topAppBar.modifier,
    title = title,
    navigationIcon = {
        if (navigationIcon != null) {
            OctopusTheme.topAppBar.navigationIcon(navigationIcon, onNavigationIconClick)
        }
    },
    actions = {
        if (contentVisible) OctopusTheme.topAppBar.actions(actions, dropdownMenuItems)
    },
    colors = colors.withOctopusDefaults(),
    scrollBehavior = scrollBehavior,
)

/**
 * Octopus-themed medium top app bar component with collapsing title behavior.
 *
 * This component extends Material 3's MediumTopAppBar with Octopus SDK theming,
 * applying consistent background, navigation icon, title, and action icon colors.
 *
 * @param modifier [Modifier] to be applied to the top app bar
 * @param title Optional title text to display in the app bar
 * @param navigationIcon Type of navigation icon to display, or null for no icon
 * @param onNavigationIconClick Callback invoked when the navigation icon is clicked
 * @param actions Composable content for action buttons in the app bar
 * @param dropdownMenuItems List of items to display in the overflow dropdown menu
 * @param colors [TopAppBarColors] for customizing the app bar appearance
 * @param scrollBehavior Optional scroll behavior for collapsing/expanding the app bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OctopusMediumTopAppBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    navigationIcon: NavigationIconType? = NavigationIconType.Back,
    onNavigationIconClick: () -> Unit = {},
    actions: @Composable () -> Unit = {},
    dropdownMenuItems: List<OctopusMenuItem> = listOf(),
    colors: TopAppBarColors = OctopusTheme.topAppBar.colors,
    expandedHeight: Dp = TopAppBarDefaults.MediumAppBarExpandedHeight,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    contentVisible: Boolean = true,
) = OctopusMediumTopAppBarImpl(
    modifier = modifier,
    title = { OctopusTheme.topAppBar.title(if (contentVisible) title else "") },
    navigationIcon = navigationIcon,
    onNavigationIconClick = onNavigationIconClick,
    actions = actions,
    dropdownMenuItems = dropdownMenuItems,
    colors = colors,
    expandedHeight = expandedHeight,
    contentVisible = contentVisible,
    scrollBehavior = scrollBehavior,
)

/**
 * Octopus-themed medium top app bar component with collapsing title behavior.
 *
 * This component extends Material 3's MediumTopAppBar with Octopus SDK theming,
 * applying consistent background, navigation icon, title, and action icon colors.
 *
 * @param modifier [Modifier] to be applied to the top app bar
 * @param title Optional title text to display in the app bar
 * @param navigationIcon Type of navigation icon to display, or null for no icon
 * @param onNavigationIconClick Callback invoked when the navigation icon is clicked
 * @param actions Composable content for action buttons in the app bar
 * @param dropdownMenuItems List of items to display in the overflow dropdown menu
 * @param colors [TopAppBarColors] for customizing the app bar appearance
 * @param scrollBehavior Optional scroll behavior for collapsing/expanding the app bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OctopusMediumTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: NavigationIconType? = NavigationIconType.Back,
    onNavigationIconClick: () -> Unit = {},
    actions: @Composable () -> Unit = {},
    dropdownMenuItems: List<OctopusMenuItem> = listOf(),
    colors: TopAppBarColors = OctopusTheme.topAppBar.colors,
    contentVisible: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) = OctopusMediumTopAppBarImpl(
    modifier = modifier,
    title = title,
    navigationIcon = navigationIcon,
    onNavigationIconClick = onNavigationIconClick,
    actions = actions,
    dropdownMenuItems = dropdownMenuItems,
    colors = colors,
    expandedHeight = TopAppBarDefaults.MediumAppBarExpandedHeight,
    contentVisible = contentVisible,
    scrollBehavior = scrollBehavior,
)

/**
 * Shared body of both [OctopusMediumTopAppBar] overloads. Only the String overload exposes
 * `expandedHeight`, so neither public overload can delegate to the other without a signature change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OctopusMediumTopAppBarImpl(
    modifier: Modifier,
    title: @Composable () -> Unit,
    navigationIcon: NavigationIconType?,
    onNavigationIconClick: () -> Unit,
    actions: @Composable () -> Unit,
    dropdownMenuItems: List<OctopusMenuItem>,
    colors: TopAppBarColors,
    expandedHeight: Dp,
    contentVisible: Boolean,
    scrollBehavior: TopAppBarScrollBehavior?,
) = MediumTopAppBar(
    modifier = modifier then OctopusTheme.topAppBar.modifier,
    title = title,
    navigationIcon = {
        if (navigationIcon != null) {
            OctopusTheme.topAppBar.navigationIcon(navigationIcon, onNavigationIconClick)
        }
    },
    actions = {
        if (contentVisible) OctopusTheme.topAppBar.actions(actions, dropdownMenuItems)
    },
    colors = colors.withOctopusDefaults(),
    expandedHeight = expandedHeight,
    scrollBehavior = scrollBehavior,
)

/**
 * Fills every [Color.Unspecified] slot of these colors with the Octopus theme default: the
 * background for both containers, gray900 for the navigation icon, title and action icons.
 */
@Composable
private fun TopAppBarColors.withOctopusDefaults(): TopAppBarColors =
    copy(
        containerColor =
            containerColor.takeIf { it != Color.Unspecified }
                ?: OctopusTheme.colorScheme.background,
        scrolledContainerColor =
            scrolledContainerColor.takeIf { it != Color.Unspecified }
                ?: OctopusTheme.colorScheme.background,
        navigationIconContentColor =
            navigationIconContentColor.takeIf { it != Color.Unspecified }
                ?: OctopusTheme.colorScheme.gray900,
        titleContentColor =
            titleContentColor.takeIf { it != Color.Unspecified }
                ?: OctopusTheme.colorScheme.gray900,
        actionIconContentColor =
            actionIconContentColor.takeIf { it != Color.Unspecified }
                ?: OctopusTheme.colorScheme.gray900,
    )

/**
 * Title section for the Octopus top app bar.
 *
 * Displays a logo and/or text title with proper spacing and alignment.
 * When no title text is provided, only the logo is shown (if available).
 *
 * @param logo Optional logo drawable to display
 * @param text Optional title text to display
 * @param textStyle Text style for the title text (defaults to 17sp semibold)
 */
@Composable
fun OctopusTopAppBarTitle(
    logo: (@Composable () -> Painter)? = null,
    text: String? = null,
    textStyle: TextStyle? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        logo?.let {
            Image(
                modifier =
                    Modifier
                        .height(32.dp)
                        .semantics { hideFromAccessibility() },
                painter = it(),
                contentDescription = null,
            )
        }
        text?.takeIf { it.isNotEmpty() }?.let {
            Text(
                text = it,
                style = textStyle ?: LocalTextStyle.current.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

/**
 * Navigation icon button for the Octopus top app bar.
 *
 * Displays either a back arrow or close icon based on the specified type.
 *
 * @param type The type of navigation icon to display
 * @param onClick Callback invoked when the icon is clicked
 */
@Composable
fun OctopusTopAppBarNavigationIcon(
    type: NavigationIconType,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector =
                when (type) {
                    NavigationIconType.Back -> Icons.AutoMirrored.Filled.ArrowBack
                    NavigationIconType.Close -> Icons.Filled.Close
                },
            contentDescription =
                when (type) {
                    NavigationIconType.Back -> stringResource(R.string.accessibility_common_back)
                    NavigationIconType.Close -> stringResource(R.string.common_close)
                },
        )
    }
}

/**
 * Actions section for the Octopus top app bar.
 *
 * Displays main action buttons and an optional overflow menu with additional items.
 * The overflow menu appears when dropdown menu items are provided.
 *
 * @param mainActions Composable content for primary action buttons
 * @param dropdownMenuItems List of items to display in the overflow dropdown menu
 */
@Composable
fun OctopusTopAppBarActions(
    mainActions: @Composable () -> Unit,
    dropdownMenuItems: List<OctopusMenuItem>,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        mainActions()
        if (dropdownMenuItems.isNotEmpty()) {
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.accessibility_common_more),
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                dropdownMenuItems.forEach { menuItem ->
                    DropdownMenuItem(
                        colors =
                            MenuDefaults.itemColors(
                                disabledTextColor = OctopusTheme.colorScheme.gray500,
                                disabledLeadingIconColor = OctopusTheme.colorScheme.gray500,
                                disabledTrailingIconColor = OctopusTheme.colorScheme.gray500,
                            ),
                        text = { Text(menuItem.text) },
                        onClick = {
                            showMenu = false
                            menuItem.onClick()
                        },
                        leadingIcon = menuItem.leadingIcon,
                        trailingIcon = menuItem.trailingIcon,
                        enabled = menuItem.enabled,
                    )
                }
            }
        }
    }
}

/**
 * Default configurations for Octopus top app bar components.
 *
 * Provides factory functions for creating commonly used top app bar configurations
 * with sensible defaults that follow Octopus design guidelines.
 */
object OctopusTopAppBarDefaults {
    /**
     * Creates a default top app bar configuration for Octopus SDK UI components.
     *
     * This function provides the standard layout and behavior for Octopus top app bars,
     * including logo display, navigation icons, and action handling.
     *
     * @param modifier [Modifier] to be applied to the top app bar
     * @param title Composable function for rendering the title area
     * @param navigationIcon Composable function for rendering the navigation icon
     * @param actions Composable function for rendering the actions area
     * @param colors [TopAppBarColors] for styling the app bar
     * @return Configured [OctopusTopAppBar] instance
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun topAppBar(
        modifier: Modifier = Modifier,
        title: @Composable (text: String?) -> Unit = title(),
        navigationIcon: @Composable (type: NavigationIconType, onClick: () -> Unit) -> Unit = navigationIcon(),
        actions: @Composable (mainActions: @Composable () -> Unit, dropdownMenuItems: List<OctopusMenuItem>) -> Unit = actions(),
        colors: TopAppBarColors = colors(),
    ) = OctopusTopAppBar(
        modifier = modifier,
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
    )

    /**
     * Default Title section for the Octopus top app bar.
     *
     * Displays a logo and/or text title with proper spacing and alignment.
     * When no title text is provided, only the logo is shown (if available).
     *
     * @param logo Optional logo drawable to display
     * @param textStyle Text style for the title text (defaults to 17sp semibold)
     * @param text Optional title text to display
     */
    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun title(
        logo: (@Composable () -> Painter)? = null,
        textStyle: TextStyle? = null,
        text: @Composable (String?) -> String? = { octopusText -> octopusText },
    ): @Composable (String?) -> Unit =
        { octopusText ->
            val text = text(octopusText)
            OctopusTopAppBarTitle(
                logo = logo ?: if (text == null) OctopusTheme.images.logo else null,
                text = text,
                textStyle = textStyle,
            )
        }

    /**
     * Navigation icon button for the Octopus top app bar.
     *
     * Displays either a back arrow or close icon based on the specified type.
     */
    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun navigationIcon(): @Composable (NavigationIconType, () -> Unit) -> Unit =
        { octopusType, onClick ->
            OctopusTopAppBarNavigationIcon(type = octopusType, onClick = onClick)
        }

    /**
     * Actions section for the Octopus top app bar.
     *
     * Displays main action buttons and an optional overflow menu with additional items.
     * The overflow menu appears when dropdown menu items are provided.
     *
     * @param mainActions Composable content for primary action buttons
     * @param dropdownMenuItems List of items to display in the overflow dropdown menu
     */
    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun actions(
        mainActions: () -> Unit = {},
        dropdownMenuItems: List<OctopusMenuItem> = emptyList(),
    ): @Composable (@Composable () -> Unit, List<OctopusMenuItem>) -> Unit =
        { octopusMainActions, octopusDropdownMenuItems ->
            OctopusTopAppBarActions(
                mainActions = {
                    mainActions()
                    octopusMainActions()
                },
                dropdownMenuItems = dropdownMenuItems + octopusDropdownMenuItems,
            )
        }

    /**
     * Creates [TopAppBarColors] with simplified color configuration.
     *
     * This convenience function allows setting the container and content colors
     * uniformly across all top app bar elements.
     *
     * @param containerColor Background color for the top app bar
     * @param contentColor Color for text and icons in the top app bar
     * @return Configured [TopAppBarColors] instance
     */
    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun colors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
    ) = colors(
        containerColor = containerColor,
        scrolledContainerColor = containerColor,
        navigationIconContentColor = contentColor,
        titleContentColor = contentColor,
        actionIconContentColor = contentColor,
    )

    /**
     * Creates [TopAppBarColors] with detailed color configuration.
     *
     * This function provides fine-grained control over each color aspect
     * of the top app bar appearance.
     *
     * @param containerColor Background color for the top app bar
     * @param scrolledContainerColor Background color when the content is scrolled
     * @param navigationIconContentColor Color for the navigation icon
     * @param titleContentColor Color for the title and subtitle text
     * @param actionIconContentColor Color for action icons
     * @return Configured [TopAppBarColors] instance
     */
    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun colors(
        containerColor: Color = Color.Unspecified,
        scrolledContainerColor: Color = Color.Unspecified,
        navigationIconContentColor: Color = Color.Unspecified,
        titleContentColor: Color = Color.Unspecified,
        actionIconContentColor: Color = Color.Unspecified,
    ) = TopAppBarColors(
        containerColor = containerColor,
        scrolledContainerColor = scrolledContainerColor,
        navigationIconContentColor = navigationIconContentColor,
        titleContentColor = titleContentColor,
        actionIconContentColor = actionIconContentColor,
        subtitleContentColor = titleContentColor,
    )
}

/**
 * Preview function for the Octopus top app bar component.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun OctopusTopAppBarPreview() {
    OctopusTheme {
        OctopusTopAppBar()
    }
}
