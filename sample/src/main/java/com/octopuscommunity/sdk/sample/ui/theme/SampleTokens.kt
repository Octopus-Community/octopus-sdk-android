package com.octopuscommunity.sdk.sample.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// The sample design system's tokens (`tokens.json`, DS v15), single-sourced here. Every colour,
// spacing, radius and size the sample draws with comes from this file; a hex written anywhere
// else in `sample/` is a drift from the five samples' shared system.
//
// Names follow the DS roles one to one (`accent-text` → `accentText`, `surface-2` → `surface2`),
// so a screen card and the Kotlin read the same.

/**
 * The single font hook of the sample.
 *
 * The DS sets General Sans, whose licence allows embedding it in a shipped app but not putting the
 * files in a repository or on a public server — and `sample/` is mirrored publicly. The sample
 * therefore keeps the platform font; swapping it is this one line, every style of
 * [SampleTypography] reads it.
 */
val SampleFontFamily: FontFamily = FontFamily.Default

/** The monospace family for identifiers, payloads and logs (`--font-mono`). */
val SampleMonoFontFamily: FontFamily = FontFamily.Monospace

/** The DS colour roles for one theme. */
@Immutable
data class SampleColors(
    val navy: Color,
    val accent: Color,
    val accentText: Color,
    val accentTint: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val text: Color,
    val textSecondary: Color,
    val hairline: Color,
    val border: Color,
    val outline: Color,
    val buttonBg: Color,
    val buttonText: Color,
    val onNavy: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerTint: Color,
    val success: Color,
    val onSuccess: Color,
    val successText: Color,
    val successTint: Color,
    val warning: Color,
    val onWarning: Color,
    val warningText: Color,
    val warningTint: Color,
    val codeBg: Color,
    val codeText: Color,
    val platformAndroid: Color,
    val onPlatformAndroid: Color,
) {
    /** The off switch, unselected radios and the N/A badge (`control-off` = `outline`). */
    val controlOff: Color get() = outline

    /** The focus ring (`focus` = `accent`). */
    val focus: Color get() = accent

    /** Guest marks (`guest` = `warning`); the word "Guest" itself takes [warningText]. */
    val guest: Color get() = warning

    companion object {
        val Light =
            SampleColors(
                navy = Color(0xFF0F1B2D),
                accent = Color(0xFF1D88FE),
                accentText = Color(0xFF095FBF),
                accentTint = Color(0xFFE8F2FF),
                accentContainer = Color(0xFFD4E6FF),
                onAccentContainer = Color(0xFF0A3D7A),
                bg = Color(0xFFF5F7FA),
                surface = Color(0xFFFFFFFF),
                surface2 = Color(0xFFE4E8EE),
                surface3 = Color(0xFFDAE0E8),
                text = Color(0xFF0F1B2D),
                textSecondary = Color(0xFF566170),
                hairline = Color(0xFFE4E7EC),
                border = Color(0xFFC2CAD5),
                outline = Color(0xFF747E8C),
                buttonBg = Color(0xFF0F1B2D),
                buttonText = Color(0xFFFFFFFF),
                onNavy = Color(0xFFFFFFFF),
                danger = Color(0xFFB03A4E),
                onDanger = Color(0xFFFFFFFF),
                dangerTint = Color(0xFFF8EDEF),
                success = Color(0xFF30B653),
                onSuccess = Color(0xFF0F1B2D),
                successText = Color(0xFF176830),
                successTint = Color(0xFFE3F5E8),
                warning = Color(0xFFD6A54C),
                onWarning = Color(0xFF0F1B2D),
                warningText = Color(0xFF7A5716),
                warningTint = Color(0xFFF9F2E6),
                codeBg = Color(0xFF0F1B2D),
                codeText = Color(0xFFD7E3F2),
                platformAndroid = Color(0xFF34A853),
                onPlatformAndroid = Color(0xFF0F1B2D),
            )

        val Dark =
            SampleColors(
                navy = Color(0xFF0F1B2D),
                accent = Color(0xFF1D88FE),
                accentText = Color(0xFF7FB8FF),
                accentTint = Color(0xFF1B3556),
                accentContainer = Color(0xFF2A5EA0),
                onAccentContainer = Color(0xFFE3EFFF),
                bg = Color(0xFF070D17),
                surface = Color(0xFF18253B),
                surface2 = Color(0xFF22324C),
                surface3 = Color(0xFF2E405C),
                text = Color(0xFFF2F5F9),
                textSecondary = Color(0xFFA3B0C2),
                // rgba(255,255,255,0.08): a separator, never a boundary.
                hairline = Color(0x14FFFFFF),
                border = Color(0xFF4A5F80),
                outline = Color(0xFF8093AB),
                buttonBg = Color(0xFF1570D9),
                buttonText = Color(0xFFFFFFFF),
                onNavy = Color(0xFFFFFFFF),
                danger = Color(0xFFF0A0A8),
                onDanger = Color(0xFF0A121E),
                dangerTint = Color(0xFF3F2E43),
                success = Color(0xFF30B653),
                onSuccess = Color(0xFF0B1421),
                successText = Color(0xFF8FE0A8),
                successTint = Color(0xFF14301F),
                warning = Color(0xFFD6A54C),
                onWarning = Color(0xFF0B1421),
                warningText = Color(0xFFE2BD78),
                warningTint = Color(0xFF3A3222),
                codeBg = Color(0xFF03060C),
                codeText = Color(0xFFD7E3F2),
                platformAndroid = Color(0xFF34A853),
                onPlatformAndroid = Color(0xFF0F1B2D),
            )
    }
}

/** The DS colours of the theme in force, provided by [SampleAppTheme]. */
val LocalSampleColors = staticCompositionLocalOf { SampleColors.Light }

/** Shorthand for the sample's own design-system values. */
object SampleTheme {
    val colors: SampleColors
        @Composable @ReadOnlyComposable
        get() = LocalSampleColors.current
}

/** The 4-point grid (`space-1` … `space-10`). */
object SampleSpacing {
    val space1: Dp = 4.dp
    val space2: Dp = 8.dp
    val space3: Dp = 12.dp
    val space4: Dp = 16.dp
    val space5: Dp = 20.dp
    val space6: Dp = 24.dp
    val space8: Dp = 32.dp
    val space10: Dp = 40.dp

    /** Side gutter of every screen (`space-4`). */
    val gutter: Dp = space4

    /** Gap between two groups of a screen (`space-6`). */
    val groupGap: Dp = space6
}

/** Corner radii. Buttons are pills; cards, list groups, fields and bubbles `md`; banners `lg`; sheets `xl`. */
object SampleRadius {
    val sm: Dp = 6.dp
    val md: Dp = 8.dp
    val lg: Dp = 12.dp
    val xl: Dp = 16.dp
    val fab: Dp = 32.dp
    val pill: Dp = 999.dp

    val smShape: Shape = RoundedCornerShape(sm)
    val mdShape: Shape = RoundedCornerShape(md)
    val lgShape: Shape = RoundedCornerShape(lg)
    val xlShape: Shape = RoundedCornerShape(xl)
    val pillShape: Shape = RoundedCornerShape(50)
}

/** Touch targets, control heights and icon sizes. */
object SampleSize {
    val touchMin: Dp = 48.dp
    val buttonHeight: Dp = 48.dp
    val buttonHeightSm: Dp = 36.dp
    val rowMin: Dp = 56.dp
    val scenarioRowMin: Dp = 64.dp
    val badgeHeight: Dp = 28.dp
    val chipHeight: Dp = 40.dp
    val icon: Dp = 24.dp
    val iconSm: Dp = 20.dp
    val iconXs: Dp = 16.dp
    val avatarSm: Dp = 32.dp
    val avatarLg: Dp = 56.dp
    val stateDisc: Dp = 64.dp
}

/**
 * `shadow-card`: every level-1 card is drawn with a 1px `border` plus this shadow. Compose has no
 * CSS box-shadow, so the two-layer light shadow (1/3 at 4 %, 8/20 at 5 % of the navy) and the dark
 * one (1/2 black at 40 %) are approximated by a low elevation with the DS shadow colour.
 */
@Composable
@ReadOnlyComposable
private fun cardShadowColor(): Color = if (LocalSampleDarkTheme.current) Color(0x66000000) else Color(0x1A0F1B2D)

/** A level-1 surface: `surface` fill is left to the caller; this draws `shadow-card` and the 1px `border`. */
fun Modifier.sampleCardEdge(shape: Shape = SampleRadius.mdShape): Modifier =
    composed {
        val shadowColor = cardShadowColor()
        this
            .shadow(elevation = 2.dp, shape = shape, ambientColor = shadowColor, spotColor = shadowColor)
            .border(1.dp, SampleTheme.colors.border, shape)
    }

/** A nested surface (`--oc-nested-edge`): the 1px `border`, no shadow. */
fun Modifier.sampleNestedEdge(shape: Shape = SampleRadius.mdShape): Modifier =
    composed { this.border(1.dp, SampleTheme.colors.border, shape) }

/**
 * `focus-ring` (`:focus-visible { outline: 2px solid focus; outline-offset: 2px }`): a solid 2px
 * `focus` ring separated from the element by a 2px gap, drawn only while the element holds
 * keyboard or D-pad focus. No glow, and touch never shows it (a tap does not focus a clickable).
 *
 * The ring is drawn outside the element's bounds, so the gap shows whatever the element sits on.
 * [gap] moves it: a negative value draws it inside, for a row whose list group clips its children
 * (`-4.dp` puts the ring 2dp inside the row's edge).
 */
fun Modifier.sampleFocusRing(
    interactionSource: InteractionSource,
    shape: Shape = SampleRadius.pillShape,
    gap: Dp = 2.dp,
): Modifier =
    composed {
        val focused by interactionSource.collectIsFocusedAsState()
        val ring = SampleTheme.colors.focus
        drawWithContent {
            drawContent()
            if (focused) {
                val stroke = 2.dp.toPx()
                // The stroke is centred on its path: put the path half a stroke past the gap.
                val inset = gap.toPx() + stroke / 2f
                val ringSize = Size(size.width + 2 * inset, size.height + 2 * inset)
                if (ringSize.width > 0f && ringSize.height > 0f) {
                    translate(-inset, -inset) {
                        drawOutline(
                            outline = shape.createOutline(ringSize, layoutDirection, this),
                            color = ring,
                            style = Stroke(width = stroke),
                        )
                    }
                }
            }
        }
    }
