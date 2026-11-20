package com.octopuscommunity.sdk.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import com.octopuscommunity.sdk.ui.components.LocalOctopusTopAppBar
import com.octopuscommunity.sdk.ui.components.OctopusLocaleWrapper
import com.octopuscommunity.sdk.ui.components.OctopusTopAppBar
import com.octopuscommunity.sdk.ui.components.OctopusTopAppBarDefaults

/**
 * Theme configuration for Octopus SDK UI components.
 * Provides color scheme, typography, and drawable resources through composition locals.
 */
object OctopusTheme {
    /** Current color scheme based on theme configuration */
    val colorScheme: OctopusColorScheme
        @Composable get() =
            LocalOctopusColorScheme.current ?: MaterialTheme.colorScheme.toOctopusColorScheme()

    /** Typography settings */
    val typography: OctopusTypography
        @Composable @ReadOnlyComposable
        get() =
            LocalOctopusTypography.current
                ?: OctopusTypographyDefaults.typography()

    /** Images resources */
    val images: OctopusImages
        @Composable get() = LocalOctopusImages.current ?: OctopusImagesDefaults.images()

    /** Icon customization */
    val icons: OctopusIcons
        @Composable get() = images.icons

    /** TopAppBar settings resources */
    @OptIn(ExperimentalMaterial3Api::class)
    val topAppBar: OctopusTopAppBar
        @Composable get() = LocalOctopusTopAppBar.current ?: OctopusTopAppBarDefaults.topAppBar()
}

/**
 * Rebuilds an Octopus palette out of a Material scheme, for hosts that only wired a Material theme
 * and never passed an [OctopusColorScheme] to [OctopusTheme].
 *
 * Only reachable where [LocalOctopusColorScheme] is absent — that is, outside every [OctopusTheme]
 * — so the scheme read here is always the host's own, never one produced by [withOctopusPalette].
 * The two functions are therefore not inverses of each other, and deliberately so: this one
 * *distrusts* host slots (see the `onHover` gate below), while [withOctopusPalette] imposes the
 * palette on the slots the SDK owns.
 */
internal fun ColorScheme.toOctopusColorScheme(): OctopusColorScheme {
    // Derive the dark/light palette from the *resolved* background's luminance rather
    // than from isSystemInDarkTheme(). When a host's Material theme diverges from the
    // system setting — e.g. an in-app dark/light toggle decoupled from the OS, or a
    // minimal integration that only wired a light Material theme while the device is in
    // dark mode — keying off the system flag would pair a dark gray palette (gray900 =
    // white) with a light background (or vice-versa), making the top-bar title and all
    // primary text invisible. Tracking the background guarantees the gray-based
    // foreground always contrasts the surface it is drawn on. A background should
    // always be specified; if it somehow is not, fall back to the light palette
    // (dark-on-unknown stays the more legible default).
    val isDarkPalette = background.isSpecified && background.luminance() < 0.5f
    val palette =
        if (isDarkPalette) {
            octopusDarkColorScheme(
                primary = primary,
                primaryLow = primaryContainer,
                primaryHigh = inversePrimary,
                onPrimary = onPrimary,
                background = background,
            )
        } else {
            octopusLightColorScheme(
                primary = primary,
                primaryLow = primaryContainer,
                primaryHigh = inversePrimary,
                onPrimary = onPrimary,
                background = background,
            )
        }
    // `onHover` is the elevated surface behind alert dialogs, the date picker and the
    // video controls, and it is drawn on with the same gray-based foreground as the
    // rest of the palette. Adopt the host's `surfaceContainerHigh` only when that
    // foreground actually stays legible on it — a host whose Material theme diverges
    // from its own background (the very case this resolution exists for) would
    // otherwise pair a light container with white content, making dialog titles and
    // the date grid invisible. Opacity is required as well, because `luminance()`
    // ignores alpha: a host leaving this slot transparent — or never wiring it at all,
    // which leaves `Color.Unspecified`, alpha 0f — would produce see-through dialogs
    // and snackbars, the defect 1.13.1 fixed on `surfaceContainer`. That single alpha
    // check covers both cases, so the gate carries no separate `isSpecified` arm.
    // Rejecting a host value only costs the elevation tint; the palette's own
    // `onHover` is always legible.
    //
    // Both grays are checked, though only `gray700` can decide today: it backs
    // supporting text and the date picker's secondary labels, and with the current gray
    // tokens it is the binding constraint — on both palettes, every container that
    // clears 4.5:1 against it clears `gray900` as well. The `gray900` arm is kept on
    // purpose: the invariant is "all content drawn on this surface stays legible", and
    // nothing but those token values makes `gray700` the stricter of the two. It is
    // therefore unreachable-as-deciding rather than dead code — mutating it to `true`
    // fails no test — and its premise is pinned by the `gray700 is the binding
    // constraint on both palettes` test, which turns red the day a gray token makes
    // this arm decide.
    val hostElevated = surfaceContainerHigh
    return if (
        hostElevated.alpha == 1f &&
        hostElevated.keepsLegible(palette.gray900) &&
        hostElevated.keepsLegible(palette.gray700)
    ) {
        palette.copy(onHover = hostElevated)
    } else {
        palette
    }
}

/**
 * Projects [palette] onto this Material scheme, so Material3 components rendered inside
 * [OctopusTheme] read the Octopus colors instead of the host's.
 *
 * Only the slots the SDK genuinely owns are pinned. `inversePrimary` in particular is left to the
 * host on purpose: Material specifies it as legible *on* `inverseSurface`, and since the SDK does
 * not pin `inverseSurface`/`inverseOnSurface`, substituting an Octopus color there would break one
 * half of a pair whose other half stays the host's. `primaryHigh` is only read *out* of that slot
 * by [toOctopusColorScheme], as a heuristic for palette-less hosts — that is not a reason to write
 * it back in.
 */
internal fun ColorScheme.withOctopusPalette(palette: OctopusColorScheme): ColorScheme =
    copy(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        primaryContainer = palette.primaryLow,
        onPrimaryContainer = palette.primary,
        background = palette.background,
        surface = palette.background,
        // Menus and other popup surfaces read their background from
        // `surfaceContainer` (Material3 `MenuTokens.ContainerColor`), which renders in
        // a separate window and would otherwise fall back to the host app's (possibly
        // transparent) scheme. Pin it to our opaque background so dropdowns are opaque
        // regardless of the embedding app's theme.
        surfaceContainer = palette.background,
        surfaceContainerHigh = palette.onHover,
        // Content colors must be pinned alongside the surfaces above, or M3
        // components that derive their content color from the scheme (rather than
        // from an explicit Octopus color) draw the host app's `onSurface` over our
        // background. Most visibly: `DropdownMenuItem` labels, whose color comes
        // from `MenuDefaults.itemColors().textColor`, itself defaulting to
        // `ListTokens.ListItemLabelTextColor` = `onSurface` — in a light-themed
        // host embedding the SDK in dark mode, that renders dark text on our dark
        // menu background, unreadable, while the leading icons (tinted explicitly
        // to gray900) stay visible.
        onBackground = palette.gray900,
        onSurface = palette.gray900,
        // Same reasoning for the secondary content color: it backs
        // `ListTokens.ListItemLeadingIconColor` (menu leading icons) and
        // `RadioButtonTokens.UnselectedIconColor`, so unstyled M3 icons and
        // controls would otherwise be invisible too.
        onSurfaceVariant = palette.gray700,
    )

/**
 * Main theme for Octopus SDK components.
 * Provides theme context for SDK UI elements.
 *
 * @param colorScheme Color scheme configuration
 * @param typography Typography configuration
 * @param images Images resources configuration
 * @param topAppBar Top app bar configuration
 * @param content Composable content to be themed
 */
@Composable
fun OctopusTheme(
    colorScheme: OctopusColorScheme = OctopusTheme.colorScheme,
    typography: OctopusTypography = OctopusTheme.typography,
    images: OctopusImages = OctopusTheme.images,
    topAppBar: OctopusTopAppBar = OctopusTheme.topAppBar,
    content: @Composable () -> Unit,
) {
    OctopusLocaleWrapper {
        CompositionLocalProvider(
            LocalOctopusColorScheme provides colorScheme,
            LocalOctopusTypography provides typography,
            LocalOctopusImages provides images,
            LocalOctopusTopAppBar provides topAppBar,
        ) {
            ProvideTextStyle(value = typography.body1) {
                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.withOctopusPalette(colorScheme),
                ) {
                    // `MaterialTheme` does not set `LocalContentColor` — only a `Surface` does,
                    // from `contentColorFor(color)`. Between this theme root and the first
                    // surface below it (`OctopusScaffold`'s `Scaffold`, a bottom sheet, a
                    // dropdown), the ambient content color is therefore whatever the host left
                    // there: its own foreground, unchecked against our background, or — when the
                    // host wired a `MaterialTheme` and no `Surface` — Material's static
                    // `Color.Black` fallback, i.e. near-black content on a dark background.
                    //
                    // That gap is a documented entry point, not a degenerate one:
                    // [com.octopuscommunity.sdk.ui.home.OctopusHomeContent] renders through a bare
                    // `Box` and its KDoc shows it embedded under the *host's* `Scaffold`, and
                    // `octopusComposables(container = …)` lets a host replace the container
                    // outright. Provide the palette's own foreground here — the same value
                    // `contentColorFor(background)` yields on our surfaces — so anything drawn in
                    // that gap is legible without depending on the host. This is the
                    // `LocalContentColor` counterpart of the `onBackground`/`onSurface` pins in
                    // [withOctopusPalette].
                    CompositionLocalProvider(LocalContentColor provides colorScheme.gray900) {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * Whether this palette is a dark one, read from its own background like [toOctopusColorScheme]
 * does, rather than from `isSystemInDarkTheme()`: a host whose in-app dark/light setting is
 * decoupled from the OS would otherwise get the light/dark variant of the wrong theme.
 *
 * The background must be dark **and** the palette's text ([OctopusColorScheme.gray900]) lighter
 * than it, so a light palette on a mid-luminance background (an orange `0xFFFF6600`) is still
 * light: a background-only threshold would class it dark.
 */
internal val OctopusColorScheme.isDark: Boolean
    get() =
        background.isSpecified &&
            background.luminance() < 0.5f &&
            gray900.luminance() > background.luminance()

/** WCAG AA contrast floor for normal-size text. */
private const val MinContrastRatio = 4.5f

/**
 * Whether [foreground] stays legible when drawn on this color, per the WCAG relative-luminance
 * contrast ratio. Alpha is not considered — callers must reject non-opaque colors themselves,
 * since [luminance] ignores it.
 */
private fun Color.keepsLegible(foreground: Color): Boolean {
    val background = luminance() + 0.05f
    val content = foreground.luminance() + 0.05f
    val ratio = if (background > content) background / content else content / background
    return ratio >= MinContrastRatio
}
