package com.octopuscommunity.sdk.sample.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The Material 3 roles, mapped from the DS colour roles of [SampleColors] so that a stock Material
 * component (switch, text field, dialog, menu) lands on the DS without a per-call override.
 *
 * - `primary` is the filled-button role (`button-bg`): navy in light, the deeper accent in dark.
 * - `primaryContainer` / `secondaryContainer` are the selected state (`accent-container`).
 * - `secondary` / `tertiary` are the accent ink (`accent-text`).
 * - The surface ladder is `bg` → `surface` → `surface-2` → `surface-3`.
 * - `error` is the DS `danger`, and `errorContainer` its tint.
 */
internal fun SampleColors.toMaterialColorScheme(dark: Boolean): ColorScheme = if (dark) darkScheme(this) else lightScheme(this)

private fun lightScheme(c: SampleColors): ColorScheme =
    lightColorScheme(
        primary = c.buttonBg,
        onPrimary = c.buttonText,
        primaryContainer = c.accentContainer,
        onPrimaryContainer = c.onAccentContainer,
        inversePrimary = SampleColors.Dark.accentText,
        secondary = c.accentText,
        onSecondary = Color.White,
        secondaryContainer = c.accentContainer,
        onSecondaryContainer = c.onAccentContainer,
        tertiary = c.accentText,
        onTertiary = Color.White,
        tertiaryContainer = c.accentTint,
        onTertiaryContainer = c.onAccentContainer,
        background = c.bg,
        onBackground = c.text,
        surface = c.surface,
        onSurface = c.text,
        surfaceVariant = c.surface3,
        onSurfaceVariant = c.textSecondary,
        surfaceTint = Color.Transparent,
        inverseSurface = c.navy,
        inverseOnSurface = c.onNavy,
        error = c.danger,
        onError = c.onDanger,
        errorContainer = c.dangerTint,
        onErrorContainer = c.danger,
        outline = c.outline,
        outlineVariant = c.hairline,
        scrim = Color.Black,
        surfaceBright = c.surface,
        surfaceDim = c.surface2,
        surfaceContainerLowest = c.surface,
        surfaceContainerLow = c.surface,
        surfaceContainer = c.surface2,
        surfaceContainerHigh = c.surface,
        surfaceContainerHighest = c.surface2,
    )

private fun darkScheme(c: SampleColors): ColorScheme =
    darkColorScheme(
        primary = c.buttonBg,
        onPrimary = c.buttonText,
        primaryContainer = c.accentContainer,
        onPrimaryContainer = c.onAccentContainer,
        inversePrimary = SampleColors.Light.accentText,
        secondary = c.accentText,
        onSecondary = c.bg,
        secondaryContainer = c.accentContainer,
        onSecondaryContainer = c.onAccentContainer,
        tertiary = c.accentText,
        onTertiary = c.bg,
        tertiaryContainer = c.accentTint,
        onTertiaryContainer = c.onAccentContainer,
        background = c.bg,
        onBackground = c.text,
        surface = c.surface,
        onSurface = c.text,
        surfaceVariant = c.surface3,
        onSurfaceVariant = c.textSecondary,
        surfaceTint = Color.Transparent,
        // The DS has no inverse-surface token; the snackbar's "light grey in dark" reads `text`.
        inverseSurface = c.text,
        inverseOnSurface = c.bg,
        error = c.danger,
        onError = c.onDanger,
        errorContainer = c.dangerTint,
        onErrorContainer = c.danger,
        outline = c.outline,
        outlineVariant = c.hairline,
        scrim = Color.Black,
        surfaceBright = c.surface3,
        surfaceDim = c.bg,
        surfaceContainerLowest = c.bg,
        surfaceContainerLow = c.surface,
        surfaceContainer = c.surface2,
        surfaceContainerHigh = c.surface,
        surfaceContainerHighest = c.surface2,
    )

private fun dsStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
) = TextStyle(
    fontFamily = SampleFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
)

/**
 * The DS type scale on Material 3's slots. Nothing is drawn below 12 sp.
 *
 * | DS            | Material       | size / line / weight |
 * |---------------|----------------|----------------------|
 * | title-1       | headlineSmall  | 26 / 34 / 600        |
 * | title-2       | titleLarge     | 22 / 28 / 600        |
 * | headline      | titleMedium    | 18 / 24 / 600        |
 * | body          | bodyLarge      | 16 / 22 / 400        |
 * | callout       | bodyMedium     | 14 / 20 / 400        |
 * | caption       | bodySmall      | 12 / 16 / 500        |
 * | label         | titleSmall, labelLarge | 14 / 20 / 600 |
 * | badge-label   | labelMedium, labelSmall | 12 / 16 / 600 |
 */
val SampleTypography: Typography =
    Typography(
        displayLarge = dsStyle(34, 40, FontWeight.SemiBold),
        displayMedium = dsStyle(30, 38, FontWeight.SemiBold),
        displaySmall = dsStyle(28, 36, FontWeight.SemiBold),
        headlineLarge = dsStyle(28, 36, FontWeight.SemiBold),
        headlineMedium = dsStyle(26, 34, FontWeight.SemiBold),
        headlineSmall = dsStyle(26, 34, FontWeight.SemiBold),
        titleLarge = dsStyle(22, 28, FontWeight.SemiBold),
        titleMedium = dsStyle(18, 24, FontWeight.SemiBold),
        titleSmall = dsStyle(14, 20, FontWeight.SemiBold),
        bodyLarge = dsStyle(16, 22, FontWeight.Normal),
        bodyMedium = dsStyle(14, 20, FontWeight.Normal),
        bodySmall = dsStyle(12, 16, FontWeight.Medium),
        labelLarge = dsStyle(14, 20, FontWeight.SemiBold),
        labelMedium = dsStyle(12, 16, FontWeight.SemiBold),
        labelSmall = dsStyle(12, 16, FontWeight.SemiBold),
    )

/** DS styles with no Material slot of their own. */
object SampleTextStyles {
    /** `body-strong`: 16 / 22 / 500. */
    val bodyStrong: TextStyle = dsStyle(16, 22, FontWeight.Medium)

    /** `button-label`: 16 / 22 / 600. */
    val buttonLabel: TextStyle = dsStyle(16, 22, FontWeight.SemiBold)

    /** `mono`: 13 / 18, identifiers, payloads and logs. */
    val mono: TextStyle =
        TextStyle(
            fontFamily = SampleMonoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )

    /** `mono` at the caption size, 12 / 16: API and type tags, log payloads. Never smaller. */
    val monoSmall: TextStyle = mono.copy(fontSize = 12.sp, lineHeight = 16.sp)
}

/**
 * Whether the sample's own chrome is drawn dark, honouring the Appearance the tester picked on
 * Config / Settings › Appearance rather than only the system setting.
 *
 * Read this instead of [isSystemInDarkTheme] anywhere the sample picks a colour by hand: with a
 * forced Light or Dark appearance the two disagree, and the screens that asked the system would
 * keep the other half of every colour pair.
 */
val LocalSampleDarkTheme = staticCompositionLocalOf { false }

@Composable
fun SampleAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) SampleColors.Dark else SampleColors.Light
    CompositionLocalProvider(
        LocalSampleDarkTheme provides darkTheme,
        LocalSampleColors provides colors,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(darkTheme),
            typography = SampleTypography,
            content = content,
        )
    }
}
