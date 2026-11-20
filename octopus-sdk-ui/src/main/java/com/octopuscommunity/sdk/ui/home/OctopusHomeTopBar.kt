package com.octopuscommunity.sdk.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.painter.Painter
import com.octopuscommunity.sdk.ui.LocalOctopusImages
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.components.NavigationIconType
import com.octopuscommunity.sdk.ui.components.OctopusCenteredTopAppBar
import com.octopuscommunity.sdk.ui.components.OctopusTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OctopusHomeTopBar(
    titleCentered: Boolean = false,
    titleText: String? = null,
    logo: (@Composable () -> Painter)? = null,
    backIcon: Boolean = true,
    leadingNavigationIcon: NavigationIconType? = null,
    navBarLeadingAction: OctopusNavBarLeadingAction? = null,
    onBack: () -> Unit = {},
    contentVisible: Boolean = true,
) {
    val effectiveLogo = if (titleText != null) null else logo ?: OctopusTheme.images.logo
    val effectiveTitle = titleText ?: if (effectiveLogo == null) stringResource(R.string.community_default_title) else null
    val navigationIcon =
        when (navBarLeadingAction) {
            is OctopusNavBarLeadingAction.Close -> NavigationIconType.Close
            is OctopusNavBarLeadingAction.Back -> NavigationIconType.Back
            null -> leadingNavigationIcon ?: if (backIcon) NavigationIconType.Back else null
        }
    val onNavigationIconClick = navBarLeadingAction?.onTap ?: onBack
    // Route the title through the themed slot, like every other Octopus bar, so a host's
    // `OctopusTopAppBarDefaults.title(...)` style or composable reaches the home too. The home's own
    // logo choice is handed over as the themed logo: the default slot draws it exactly when the text
    // is null, which is the case where `effectiveLogo` is non-null. A slot built with its own
    // `title(logo = ...)` draws that logo instead, next to any text, as on every other screen.
    val images = OctopusTheme.images.copy(logo = effectiveLogo)
    val title: @Composable () -> Unit = {
        CompositionLocalProvider(LocalOctopusImages provides images) {
            // Hidden content keeps the logo (null text) and blanks the text (empty text).
            OctopusTheme.topAppBar.title(effectiveTitle?.let { if (contentVisible) it else "" })
        }
    }
    if (titleCentered || navBarLeadingAction != null) {
        OctopusCenteredTopAppBar(
            navigationIcon = navigationIcon,
            onNavigationIconClick = onNavigationIconClick,
            title = title,
            contentVisible = contentVisible,
        )
    } else {
        OctopusTopAppBar(
            navigationIcon = navigationIcon,
            onNavigationIconClick = onNavigationIconClick,
            title = title,
            contentVisible = contentVisible,
        )
    }
}
