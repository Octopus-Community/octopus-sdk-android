package com.octopuscommunity.sdk.sample.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.octopuscommunity.sdk.sample.ui.components.dangerPillEdge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The DS colour tokens, light and dark, against the WCAG floors the DS states: 4.5:1 for text
 * (nothing in the sample is drawn below 12 sp, so no large-text exemption applies) and 3:1 for the
 * edge of an unfilled control.
 */
class SampleColorsContrastTest {
    private val themes = listOf("light" to SampleColors.Light, "dark" to SampleColors.Dark)

    private val textPairs: List<Triple<String, (SampleColors) -> Color, (SampleColors) -> Color>> =
        listOf(
            Triple("text on bg", { it.text }, { it.bg }),
            Triple("text on surface", { it.text }, { it.surface }),
            Triple("text on surface-2", { it.text }, { it.surface2 }),
            Triple("text on surface-3", { it.text }, { it.surface3 }),
            Triple("text-secondary on bg", { it.textSecondary }, { it.bg }),
            Triple("text-secondary on surface", { it.textSecondary }, { it.surface }),
            Triple("text-secondary on surface-2", { it.textSecondary }, { it.surface2 }),
            Triple("text-secondary on surface-3", { it.textSecondary }, { it.surface3 }),
            Triple("accent-text on bg", { it.accentText }, { it.bg }),
            Triple("accent-text on surface", { it.accentText }, { it.surface }),
            Triple("accent-text on accent-tint", { it.accentText }, { it.accentTint }),
            Triple("on-accent-container on accent-container", { it.onAccentContainer }, { it.accentContainer }),
            Triple("danger on surface", { it.danger }, { it.surface }),
            Triple("danger on danger-tint", { it.danger }, { it.dangerTint }),
            Triple("success-text on surface", { it.successText }, { it.surface }),
            Triple("success-text on success-tint", { it.successText }, { it.successTint }),
            Triple("warning-text on surface", { it.warningText }, { it.surface }),
            Triple("warning-text on warning-tint", { it.warningText }, { it.warningTint }),
            Triple("button-text on button-bg", { it.buttonText }, { it.buttonBg }),
            Triple("code-text on code-bg", { it.codeText }, { it.codeBg }),
        )

    @Test
    fun `every text pairing clears 4_5 to 1 in both themes`() {
        for ((theme, colors) in themes) {
            for ((role, fg, bg) in textPairs) {
                val ratio = contrastRatio(fg(colors), bg(colors))
                assertTrue(ratio >= 4.5, "$theme $role: $ratio:1")
            }
        }
    }

    @Test
    fun `control outline clears 3 to 1 on the page and on a card in both themes`() {
        for ((theme, colors) in themes) {
            assertTrue(contrastRatio(colors.outline, colors.bg) >= 3.0, "$theme outline on bg")
            assertTrue(contrastRatio(colors.outline, colors.surface) >= 3.0, "$theme outline on surface")
        }
    }

    @Test
    fun `danger button edge clears 3 to 1 on the page, the card and its own tint in both themes`() {
        // DangerPillButton draws a solid `danger` edge on a `danger-tint` fill; the edge is the
        // control boundary, so it owes the non-text floor against every backdrop it can sit on.
        for ((theme, colors) in themes) {
            val backdrops = listOf("bg" to colors.bg, "surface" to colors.surface, "danger-tint" to colors.dangerTint)
            for ((backdrop, bg) in backdrops) {
                assertTrue(contrastRatio(dangerPillEdge(colors), bg) >= 3.0, "$theme danger edge on $backdrop")
            }
        }
    }

    @Test
    fun `dark button blue is the one pairing that sits close to the floor`() {
        // White on #1570d9 is the DS dark button; pinned so a nudge of the blue is a visible change.
        assertEquals(4.83, contrastRatio(SampleColors.Dark.buttonText, SampleColors.Dark.buttonBg), absoluteTolerance = 0.02)
    }

    private fun contrastRatio(
        foreground: Color,
        background: Color,
    ): Double {
        // These tokens are opaque; translucency would require compositing before measuring.
        assertEquals(1f, foreground.alpha)
        assertEquals(1f, background.alpha)
        val foregroundLuminance = foreground.luminance().toDouble()
        val backgroundLuminance = background.luminance().toDouble()
        return (maxOf(foregroundLuminance, backgroundLuminance) + 0.05) /
            (minOf(foregroundLuminance, backgroundLuminance) + 0.05)
    }
}
