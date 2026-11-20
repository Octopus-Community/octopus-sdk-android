package com.octopuscommunity.sdk.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

internal val LocalOctopusTypography = staticCompositionLocalOf<OctopusTypography?> { null }

/**
 * Typography configuration for Octopus SDK UI components.
 *
 * @property title1 Large title style (26sp)
 * @property title2 Medium title style (22sp)
 * @property body1 Primary body text style (18sp)
 * @property body2 Secondary body text style (16sp)
 * @property caption1 Primary caption style (14sp)
 * @property caption2 Secondary caption style (12sp)
 */
data class OctopusTypography(
    val title1: TextStyle,
    val title2: TextStyle,
    val body1: TextStyle,
    val body2: TextStyle,
    val caption1: TextStyle,
    val caption2: TextStyle,
)

/**
 * Default typography configurations for Octopus SDK UI components.
 */
object OctopusTypographyDefaults {
    /**
     * Creates typography configuration for Octopus SDK UI components.
     *
     * @param title1 Large title style (26sp)
     * @param title2 Medium title style (22sp)
     * @param body1 Primary body text style (18sp)
     * @param body2 Secondary body text style (16sp)
     * @param caption1 Primary caption style (14sp)
     * @param caption2 Secondary caption style (12sp)
     */
    fun typography(
        title1: TextStyle = TextStyle(fontSize = 26.sp, lineHeight = 36.sp),
        title2: TextStyle = TextStyle(fontSize = 22.sp),
        body1: TextStyle = TextStyle(fontSize = 18.sp),
        body2: TextStyle = TextStyle(fontSize = 16.sp),
        caption1: TextStyle = TextStyle(fontSize = 14.sp),
        caption2: TextStyle = TextStyle(fontSize = 12.sp),
    ) = OctopusTypography(
        title1 = title1,
        title2 = title2,
        body1 = body1,
        body2 = body2,
        caption1 = caption1,
        caption2 = caption2,
    )
}

/**
 * Typography configuration for Octopus SDK UI components.
 *
 * This function will provide an OctopusTypography with variant font sizes.
 *
 * @param title1 Large title style (26sp)
 * @param title2 Medium title style (22sp)
 * @param body1 Primary body text style (18sp)
 * @param body2 Secondary body text style (16sp)
 * @param caption1 Primary caption style (14sp)
 * @param caption2 Secondary caption style (12sp)
 * @deprecated Use [OctopusTypographyDefaults.typography] instead.
 */
@Deprecated(
    message = "Use OctopusTypographyDefaults.typography() instead.",
    replaceWith =
        ReplaceWith(
            "OctopusTypographyDefaults.typography(title1, title2, body1, body2, caption1, caption2)",
            "com.octopuscommunity.sdk.ui.OctopusTypographyDefaults",
        ),
)
fun octopusTypography(
    title1: TextStyle = TextStyle(fontSize = 26.sp, lineHeight = 36.sp),
    title2: TextStyle = TextStyle(fontSize = 22.sp),
    body1: TextStyle = TextStyle(fontSize = 18.sp),
    body2: TextStyle = TextStyle(fontSize = 16.sp),
    caption1: TextStyle = TextStyle(fontSize = 14.sp),
    caption2: TextStyle = TextStyle(fontSize = 12.sp),
) = OctopusTypographyDefaults.typography(title1, title2, body1, body2, caption1, caption2)

val TextStyle.medium get() = copy(fontWeight = FontWeight.Medium) // W500
val TextStyle.semiBold get() = copy(fontWeight = FontWeight.SemiBold) // W600
val TextStyle.bold get() = copy(fontWeight = FontWeight.Bold) // W700

internal val TextStyle.lineHeight150 get() = copy(lineHeight = (fontSize * 1.5))

/**
 * Body text lineHeight matching the Figma Design System (`leading-[24px]` for body-2 16sp,
 * a 1.5 ratio). Includes `LineHeightStyle.Trim.Both` so the first and last lines don't carry
 * the extra half-leading that Compose's default `Trim.None` adds — this matters when the text
 * sits right below another block (e.g. `ResponseHeader` above `ResponseTextContent`): without
 * the trim the body's first glyph drifts below the line-box top and visibly opens the gap with
 * the preceding block. With `Trim.Both` the first glyph lands at its natural cap height from the
 * block boundary, matching the SwiftUI / Figma rendering iOS produces (iOS SDK internal change renders
 * the same content at a 24px line height).
 *
 * Distinct from [lineHeight150] (same 1.5 ratio, no trim) used for standalone paragraphs.
 */
internal val TextStyle.lineHeight150Trimmed
    get() =
        copy(
            lineHeight = (fontSize * 1.5),
            lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Proportional,
                    trim = LineHeightStyle.Trim.Both,
                ),
        )

internal val TextStyle.lineHeight125 get() = copy(lineHeight = (fontSize * 1.25))
