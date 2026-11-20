package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme

/**
 * Max width of the scrollable content column on large screens (tablet / landscape) so line lengths
 * stay readable instead of stretching edge to edge (internal ticket).
 *
 * - Phone: 570dp
 * - Tablet (smallest width >= 600dp): 1106dp
 *
 * Mirrors the iOS values (570pt / 1106pt, by device idiom).
 */
@Composable
internal fun octopusMaxContentWidth(): Dp = if (LocalConfiguration.current.smallestScreenWidthDp >= 600) 1106.dp else 570.dp

/**
 * Caps [content] to [octopusMaxContentWidth] and centers it horizontally.
 *
 * When the available width exceeds the cap (tablet, landscape) the surrounding space becomes side
 * margins painted with the default separator color (`gray300`); otherwise the standard background is
 * used so nothing changes on narrow screens (e.g. phone portrait). The content column itself keeps
 * the standard background so it reads as a centered card (internal ticket).
 *
 * Apply this around a screen's scrollable content (the `LazyColumn` / vertical scroll). Full-width
 * bars (top app bar, etc.) must stay outside of it.
 */
@Composable
internal fun ConstrainedContentColumn(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val maxContentWidth = octopusMaxContentWidth()
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth > maxContentWidth
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        if (isWide) {
                            OctopusTheme.colorScheme.gray300
                        } else {
                            OctopusTheme.colorScheme.background
                        },
                    ),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .widthIn(max = maxContentWidth)
                        .fillMaxWidth()
                        .background(OctopusTheme.colorScheme.background),
                content = content,
            )
        }
    }
}
