package com.octopuscommunity.sdk.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter

internal val LocalOctopusImages = staticCompositionLocalOf<OctopusImages?> { null }

/**
 * Painter resources configuration for Octopus SDK UI components.
 *
 * @property logo Logo drawable resource, or null if not provided
 * @property icons Icon customization. Use [OctopusIconsDefaults.icons] to build with SDK defaults.
 */
data class OctopusImages(
    val logo: (@Composable () -> Painter)?,
    val icons: OctopusIcons
)

/**
 * Default painters configurations for Octopus SDK UI components.
 */
object OctopusImagesDefaults {
    /**
     * Creates images configuration based on the current system theme.
     */
    fun images(
        logo: (@Composable () -> Painter)? = null,
        icons: OctopusIcons = OctopusIconsDefaults.icons()
    ) = OctopusImages(
        logo = logo,
        icons = icons
    )
}