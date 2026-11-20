package com.octopuscommunity.sdk.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val LocalOctopusColorScheme = staticCompositionLocalOf<OctopusColorScheme?> { null }

/**
 * Color scheme configuration for Octopus SDK UI components.
 *
 * @property primary Primary brand color
 * @property primaryLow Low contrast version of the primary color
 * @property primaryHigh High contrast version of the primary color
 * @property onPrimary Color for content on primary surfaces
 * @property background Background color
 * @property gray100 Lightest gray shade
 * @property gray200 Lighter gray shade
 * @property gray300 Light gray shade
 * @property gray500 Medium gray shade
 * @property gray700 Dark gray shade
 * @property gray800 Darker gray shade
 * @property gray900 Darkest gray shade
 * @property alert Color for error states
 * @property alertLow Low contrast version of the alert color
 * @property alertHigh High contrast version of the alert color
 * @property success Color for success states
 * @property successLow Low contrast version of the success color
 * @property successHigh High contrast version of the success color
 * @property onHover Color for hover states
 * @property disabled Background color for disabled elements
 * @property link Color for clickable links
 * @property moderated Color for moderated posts
 */
data class OctopusColorScheme(
    val primary: Color,
    val primaryLow: Color,
    val primaryHigh: Color,
    val onPrimary: Color,
    val background: Color,
    val gray100: Color,
    val gray200: Color,
    val gray300: Color,
    val gray500: Color,
    val gray700: Color,
    val gray800: Color,
    val gray900: Color,
    val alert: Color,
    val alertLow: Color,
    val alertHigh: Color,
    val success: Color,
    val successLow: Color,
    val successHigh: Color,
    val onHover: Color,
    val disabled: Color,
    val link: Color,
    val moderated: Color,
)

/**
 * Creates an Octopus color scheme based on the current system theme.
 * Returns dark color scheme for dark theme, light color scheme for light theme.
 */
@Composable
fun octopusColorScheme() =
    if (isSystemInDarkTheme()) {
        octopusDarkColorScheme()
    } else {
        octopusLightColorScheme()
    }

/**
 * Defines the light color palette for the UI components.
 *
 * @param primary Primary brand color
 * @param primaryLow Low contrast version of the primary color
 * @param primaryHigh High contrast version of the primary color
 * @param onPrimary Color for content displayed on primary surfaces
 * @param background Background color (defaults to [gray100])
 * @param gray100 Lightest gray shade
 * @param gray200 Lighter gray shade
 * @param gray300 Light gray shade
 * @param gray500 Medium gray shade
 * @param gray700 Dark gray shade
 * @param gray800 Darker gray shade
 * @param gray900 Darkest gray shade
 * @param alert Color for error states
 * @param alertLow Low contrast version of the alert color
 * @param alertHigh High contrast version of the alert color
 * @param success Color for success states
 * @param successLow Low contrast version of the success color
 * @param successHigh High contrast version of the success color
 * @param onHover Color for hover states
 * @param disabled Background color for disabled elements (defaults to [gray200])
 * @param link Color for clickable links (defaults to [primary])
 * @param moderated Color for moderated posts (defaults to [alertLow])
 */
fun octopusLightColorScheme(
    primary: Color = Color(0xFF141414), // Octo-light main
    primaryLow: Color = Color(0xFFEBEBEB), // Octo-light low_contrast
    primaryHigh: Color = Color(0xFFD2D2D2), // Octo-light high_contrast
    onPrimary: Color = Color(0xFFFFFFFF), // White on primary
    gray100: Color = Color(0xFFFFFFFF), // Octo-light grey 100
    gray200: Color = Color(0xFFF4F4F4), // Octo-light grey 200
    gray300: Color = Color(0xFFE1E1E1), // Octo-light grey 300 (borders)
    gray500: Color = Color(0xFF6D6E78), // Octo-light grey 500
    gray700: Color = Color(0xFF616172), // Octo-light grey 700 (secondary)
    gray800: Color = Color(0xFF313235), // Octo-light grey 800
    gray900: Color = Color(0xFF141414), // Octo-light grey 900 (primary)
    background: Color = gray100, // White background
    alert: Color = Color(0xFFFF3366), // Octo-light alert main
    alertLow: Color = Color(0xFFFFE7EF), // Octo-light alert low_contrast
    alertHigh: Color = Color(0xFF9E243F), // Octo-light alert high_contrast
    success: Color = Color(0xFF30B653), // Octo-light success main
    successLow: Color = Color(0xFFD5F6DD), // Octo-light success low_contrast
    successHigh: Color = Color(0xFF189437), // Octo-light success high_contrast
    onHover: Color = Color(0xFFFFFFFF), // Octo-light white on_hover
    disabled: Color = gray200,
    link: Color = Color(0xFF1D9BD1),
    moderated: Color = alertLow,
) = OctopusColorScheme(
    primary = primary,
    primaryLow = primaryLow,
    primaryHigh = primaryHigh,
    onPrimary = onPrimary,
    background = background,
    gray100 = gray100,
    gray200 = gray200,
    gray300 = gray300,
    gray500 = gray500,
    gray700 = gray700,
    gray800 = gray800,
    gray900 = gray900,
    alert = alert,
    alertLow = alertLow,
    alertHigh = alertHigh,
    disabled = disabled,
    success = success,
    successLow = successLow,
    successHigh = successHigh,
    onHover = onHover,
    link = link,
    moderated = moderated,
)

/**
 * Defines the dark color palette for the UI components.
 *
 * @param primary Primary brand color
 * @param primaryLow Low contrast version of the primary color
 * @param primaryHigh High contrast version of the primary color
 * @param onPrimary Color for content displayed on primary surfaces
 * @param background Background color (defaults to [gray100])
 * @param gray100 Lightest gray shade
 * @param gray200 Lighter gray shade
 * @param gray300 Light gray shade
 * @param gray500 Medium gray shade
 * @param gray700 Dark gray shade
 * @param gray800 Darker gray shade
 * @param gray900 Darkest gray shade
 * @param alert Color for error states
 * @param alertLow Low contrast version of the alert color
 * @param alertHigh High contrast version of the alert color
 * @param success Color for success states
 * @param successLow Low contrast version of the success color
 * @param successHigh High contrast version of the success color
 * @param onHover Color for hover states
 * @param disabled Background color for disabled elements (defaults to [gray200])
 * @param link Color for clickable links (defaults to [primary])
 * @param moderated Color for moderated posts (defaults to [alertLow])
 */
fun octopusDarkColorScheme(
    primary: Color = Color(0xFFEBEBEB), // Octo-dark main
    primaryLow: Color = Color(0xFF303030), // Octo-dark low_contrast
    primaryHigh: Color = Color(0xFFE0E0E0), // Octo-dark high_contrast
    onPrimary: Color = Color(0xFF141414), // Octo-dark on_primary
    gray100: Color = Color(0xFF141414), // Octo-dark grey 100
    gray200: Color = Color(0xFF313235), // Octo-dark grey 200
    gray300: Color = Color(0xFF4D4D5A), // Octo-dark grey 300 (borders)
    gray500: Color = Color(0xFF6D6E78), // Octo-dark grey 500
    gray700: Color = Color(0xFFE1E1E1), // Octo-dark grey 700 (secondary)
    gray800: Color = Color(0xFFF4F4F4), // Octo-dark grey 800
    gray900: Color = Color(0xFFFFFFFF), // Octo-dark grey 900 (primary)
    background: Color = gray100, // Dark background
    alert: Color = Color(0xFFFF3366), // Octo-dark alert main
    alertLow: Color = Color(0xFF380914), // Octo-dark alert low_contrast
    alertHigh: Color = Color(0xFFFFE7EF), // Octo-dark alert high_contrast
    success: Color = Color(0xFF54DD78), // Octo-dark success main
    successLow: Color = Color(0xFF084116), // Octo-dark success low_contrast
    successHigh: Color = Color(0xFFD5F6DD), // Octo-dark success high_contrast
    onHover: Color = Color(0xFF242526), // Octo-dark white on_hover
    disabled: Color = gray200,
    link: Color = Color(0xFF1D9BD1),
    moderated: Color = alertLow,
) = OctopusColorScheme(
    primary = primary,
    primaryLow = primaryLow,
    primaryHigh = primaryHigh,
    onPrimary = onPrimary,
    background = background,
    gray100 = gray100,
    gray200 = gray200,
    gray300 = gray300,
    gray500 = gray500,
    gray700 = gray700,
    gray800 = gray800,
    gray900 = gray900,
    alert = alert,
    alertLow = alertLow,
    alertHigh = alertHigh,
    disabled = disabled,
    success = success,
    successLow = successLow,
    successHigh = successHigh,
    onHover = onHover,
    link = link,
    moderated = moderated,
)
