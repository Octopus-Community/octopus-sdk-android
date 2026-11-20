package com.octopuscommunity.sdk.ui.consent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.medium

/** Whole lines of the implicit notice that are always fully visible before it scrolls. */
internal const val IMPLICIT_CONSENT_NOTICE_MAX_LINES = 5

/** Share of the next line that peeks under the last whole line of a scrolling notice. */
internal const val IMPLICIT_CONSENT_NOTICE_PEEK_FRACTION = 0.5f

/**
 * Largest share of the window height the visible part of the implicit notice may take, whatever the
 * font scale: at the largest text sizes the five and a half lines would otherwise fill most of the
 * composer once the keyboard is up.
 */
internal const val IMPLICIT_CONSENT_NOTICE_MAX_WINDOW_FRACTION = 0.2f

/**
 * The legal notice under the post / comment / reply editor in the implicit terms-acceptance mode.
 * A notice of up to [IMPLICIT_CONSENT_NOTICE_MAX_LINES] lines keeps its natural height. A longer one
 * is cut to those lines plus half of the next one, so the half line peeking under the fifth tells
 * that there is more, and scrolls inside that zone; it is never truncated. That zone never exceeds
 * [IMPLICIT_CONSENT_NOTICE_MAX_WINDOW_FRACTION] of the window height either, so at a very large font scale
 * the notice scrolls in fewer lines, and a notice below that cap keeps its natural height. The SDK wording's links
 * open the three legal documents; a community wording's links hand their URL to [onNavigateToUrl].
 */
@Composable
internal fun ImplicitConsentNotice(
    wording: ConsentWording,
    onNavigateToTermsOfUse: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToCommunityGuidelines: () -> Unit,
    onNavigateToUrl: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = OctopusTheme.typography.caption2.medium
    val linesHeight =
        rememberTextLinesHeight(
            style = style,
            lines = IMPLICIT_CONSENT_NOTICE_MAX_LINES,
            extraLineFraction = IMPLICIT_CONSENT_NOTICE_PEEK_FRACTION,
        )
    val maxTextHeight = implicitConsentNoticeMaxHeight(linesHeight, rememberWindowHeight())
    ConsentWordingText(
        modifier =
            modifier
                .fillMaxWidth()
                .background(OctopusTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .heightIn(max = maxTextHeight)
                .verticalScroll(rememberScrollState()),
        wording = wording,
        sdkLinkActions =
            listOf(
                onNavigateToTermsOfUse,
                onNavigateToPrivacyPolicy,
                onNavigateToCommunityGuidelines,
            ),
        onNavigateToUrl = onNavigateToUrl,
        color = OctopusTheme.colorScheme.gray900,
        textAlign = TextAlign.Center,
        style = style,
    )
}

/**
 * The cap of the implicit notice's visible height: the measured [linesHeight] (five and a half lines),
 * lowered to [IMPLICIT_CONSENT_NOTICE_MAX_WINDOW_FRACTION] of the [windowHeight] when that is smaller.
 * An unknown window ([windowHeight] not positive, e.g. before the first measure) applies no relative cap.
 */
internal fun implicitConsentNoticeMaxHeight(
    linesHeight: Dp,
    windowHeight: Dp,
): Dp =
    if (windowHeight > 0.dp) {
        minOf(linesHeight, windowHeight * IMPLICIT_CONSENT_NOTICE_MAX_WINDOW_FRACTION)
    } else {
        linesHeight
    }

/**
 * Height of the window the notice is shown in. Read from [LocalWindowInfo]'s `containerSize`, the size
 * in pixels of the window that hosts the composition: in split-screen, freeform or a pop-up it is the
 * window's own height, not the display's, and it does not wait for an activity configuration change as
 * `LocalConfiguration.screenHeightDp` can. The soft keyboard is not subtracted, so the cap does not
 * move when it shows or hides.
 */
@Composable
private fun rememberWindowHeight(): Dp {
    val heightPx = LocalWindowInfo.current.containerSize.height
    val density = LocalDensity.current
    return remember(heightPx, density) { with(density) { heightPx.toDp() } }
}

/**
 * Height of [lines] lines of [style] plus [extraLineFraction] of one more, measured with the current
 * density and font scale, so the cap follows the host's typography and the user's text size. The
 * extra part is taken from the measured height of one more line, not from a guessed line height.
 * Measured with the theme's primary font ("Ag"): a taller fallback font (emoji, some scripts) makes the peek
 * approximate.
 */
@Composable
internal fun rememberTextLinesHeight(
    style: TextStyle,
    lines: Int,
    extraLineFraction: Float = 0f,
): Dp {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(textMeasurer, style, lines, extraLineFraction, density) {
        fun heightPx(count: Int) =
            textMeasurer
                .measure(text = List(count) { "Ag" }.joinToString(separator = "\n"), style = style)
                .size.height
        val whole = heightPx(lines)
        val extra = if (extraLineFraction > 0f) (heightPx(lines + 1) - whole) * extraLineFraction else 0f
        with(density) { (whole + extra).toDp() }
    }
}
