package com.octopuscommunity.sdk.ui.responses.blocks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource

/**
 * Trailing "⋯" icon button on a comment / reply header. Aligned with Figma `Bouton container`
 * (node 1953:11451) inside the Comment header symbol: `w-[44px] h-[37px] pb-[4px] pt-[10px]`.
 *
 * - 44 × 37dp hit-area (NOT 44 × 44 like a Material `IconButton`): the comment header in
 *   Figma is exactly 37dp tall (10 + 23 line + 4) and the more-menu container fills it
 *   exactly. A 44 × 44 button here would grow the surrounding `ResponseHeader` Row to 44dp
 *   (taking the max of its children's heights), leaving ~10dp of dead space above the body
 *   text. Matching the 37dp height keeps the geometry iso Figma.
 * - 20dp icon, gray500, top-end aligned. Padding `top=9 end=12 bottom=8` puts the icon's
 *   visual centre at y≈19 within the 37dp box — same position as Figma's 16px "⋯" glyph
 *   inside `pb-[4px] pt-[10px]`.
 * - Ripple is rendered on the **Icon** (not the outer Box) so the touch feedback is centred
 *   on the visible icon. Radius 16dp = 32dp diameter (proportional to 20dp).
 */
@Composable
internal fun ResponseMoreMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .width(44.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.TopEnd,
    ) {
        Icon(
            modifier =
                Modifier
                    .padding(top = 9.dp, end = 12.dp, bottom = 8.dp)
                    .indication(interactionSource, ripple(bounded = false, radius = 16.dp))
                    .size(20.dp),
            painter = OctopusTheme.icons.moreActions(),
            contentDescription = stringResource(R.string.accessibility_common_more),
            tint = OctopusTheme.colorScheme.gray500,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResponseMoreMenuButtonPreview() {
    OctopusTheme { ResponseMoreMenuButton(onClick = {}) }
}
