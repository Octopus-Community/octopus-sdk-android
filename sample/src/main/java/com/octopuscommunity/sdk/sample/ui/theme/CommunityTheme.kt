package com.octopuscommunity.sdk.sample.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.data.model.SDKConfig.Theme.Default
import com.octopuscommunity.sdk.ui.OctopusColorScheme
import com.octopuscommunity.sdk.ui.OctopusIconsDefaults
import com.octopuscommunity.sdk.ui.OctopusImagesDefaults
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.OctopusTypographyDefaults
import com.octopuscommunity.sdk.ui.octopusDarkColorScheme
import com.octopuscommunity.sdk.ui.octopusLightColorScheme

/**
 * Custom theming container for Octopus SDK screens within the sample app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityTheme(
    theme: SDKConfig.Theme,
    content: @Composable () -> Unit,
) {
    val colors = communityColorScheme(theme)
    when (theme) {
        // The sample's own brand theme ("Octopus navy"), and the one the SDK runs under by
        // default. Signal covers both the shell and the SDK in dark mode. Passing no theme at all
        // is a deliberate preset (`SdkDefault`), never the default — the SDK's built-in primary
        // is `internal change`, which reads as "broken", not as "unthemed".
        Default ->
            OctopusTheme(
                colorScheme = colors,
            ) { content() }

        // "SDK default": no theme handed over at all — the visual reference control, showing
        // what a host that passes nothing gets (built-in `internal change` primary included).
        SDKConfig.Theme.SdkDefault ->
            OctopusTheme(
                colorScheme = colors,
            ) { content() }

        is SDKConfig.Theme.Custom ->
            OctopusTheme(
                colorScheme = colors,
                typography =
                    if (theme.fonts) {
                        OctopusTypographyDefaults.typography(
                            title1 =
                                OctopusTypographyDefaults.typography().title1.copy(
                                    fontSize = 28.sp,
                                    fontFamily = FontFamily.SansSerif,
                                ),
                            title2 =
                                OctopusTypographyDefaults.typography().title2.copy(
                                    fontSize = 24.sp,
                                    fontFamily = FontFamily.Serif,
                                ),
                            body1 =
                                OctopusTypographyDefaults.typography().body1.copy(
                                    fontSize = 20.sp,
                                    fontFamily = FontFamily.Serif,
                                ),
                            body2 =
                                OctopusTypographyDefaults.typography().body2.copy(
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Serif,
                                ),
                            caption1 =
                                OctopusTypographyDefaults.typography().caption1.copy(
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Serif,
                                ),
                            caption2 =
                                OctopusTypographyDefaults.typography().caption2.copy(
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Serif,
                                ),
                        )
                    } else {
                        OctopusTypographyDefaults.typography()
                    },
                images =
                    OctopusImagesDefaults.images(
                        icons = if (theme.icons) customIcons() else OctopusIconsDefaults.icons(),
                    ),
            ) {
                content()
            }
    }
}

// The palette handed to the SDK, built from the sample's DS tokens so the community screens and
// the sample's own chrome read as one app. Only the slots the sample already configured are set:
// the SDK's own alert, success and moderation colours stay its defaults.
//
// Dark mapping (DS role → SDK slot):
//   bg → background, gray100 · surface → gray200 · hairline on surface (#2B364B) → gray300
//   text-secondary → gray500 · text → gray700, gray800, gray900 · surface-2 → onHover
//   accent-text → primary, link · accent-tint → primaryLow · accent → primaryHigh · bg → onPrimary

/** `hairline` (white at 8 %) composited on the dark `surface`: the SDK slot also drives `disabled`, so it is opaque. */
private val DarkHairlineOnSurface = Color(0xFF2B364B)

private fun sampleDarkColorScheme(): OctopusColorScheme {
    val c = SampleColors.Dark
    return octopusDarkColorScheme(
        primary = c.accentText,
        primaryLow = c.accentTint,
        primaryHigh = c.accent,
        onPrimary = c.bg,
        background = c.bg,
        gray100 = c.bg,
        gray200 = c.surface,
        gray300 = DarkHairlineOnSurface,
        gray500 = c.textSecondary,
        gray700 = c.text,
        gray800 = c.text,
        gray900 = c.text,
        onHover = c.surface2,
        link = c.accentText,
    )
}

private fun sampleLightColorScheme(): OctopusColorScheme {
    val c = SampleColors.Light
    return octopusLightColorScheme(
        primary = c.navy,
        primaryLow = c.accentTint,
        primaryHigh = c.accent,
        onPrimary = c.onNavy,
    )
}

/** The same resolved palette drives SDK content and the host's system-bar protection. */
@Composable
internal fun communityColorScheme(theme: SDKConfig.Theme): OctopusColorScheme =
    if (theme == SDKConfig.Theme.SdkDefault || (theme is SDKConfig.Theme.Custom && !theme.colors)) {
        if (LocalSampleDarkTheme.current) octopusDarkColorScheme() else octopusLightColorScheme()
    } else if (LocalSampleDarkTheme.current) {
        sampleDarkColorScheme()
    } else {
        sampleLightColorScheme()
    }
