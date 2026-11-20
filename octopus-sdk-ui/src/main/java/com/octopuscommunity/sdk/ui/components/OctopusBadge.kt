package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.test.mock.MockGamificationLevels
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.isDark
import com.octopuscommunity.sdk.ui.medium
import com.octopuscommunity.sdk.ui.semiBold
import com.octopuscommunity.sdk.ui.utils.toColor

data class OctopusBadgeColors(
    val containerColor: Color,
    val contentColor: Color,
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
) {
    constructor(containerColor: Color, contentColor: Color) : this(
        containerColor = containerColor,
        contentColor = contentColor,
        selectedContainerColor = containerColor,
        selectedContentColor = contentColor,
    )
}

object OctopusBadgeDefaults {
    @Composable
    fun badgeColors(
        containerColor: Color = OctopusTheme.colorScheme.primaryLow,
        contentColor: Color = OctopusTheme.colorScheme.primary,
        selectedContainerColor: Color = OctopusTheme.colorScheme.primary,
        selectedContentColor: Color = OctopusTheme.colorScheme.onPrimary,
    ) = OctopusBadgeColors(
        containerColor = containerColor,
        contentColor = contentColor,
        selectedContainerColor = selectedContainerColor,
        selectedContentColor = selectedContentColor,
    )

    @Composable
    fun badgeColorsAlert(
        containerColor: Color = OctopusTheme.colorScheme.alertLow,
        contentColor: Color = OctopusTheme.colorScheme.alert,
    ) = OctopusBadgeColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )
}

@Composable
fun OctopusBadge(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    colors: OctopusBadgeColors = OctopusBadgeDefaults.badgeColors(),
    textStyle: TextStyle = OctopusTheme.typography.body2.semiBold,
    padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
    onClick: (() -> Unit)? = null,
) = Box(
    modifier =
        modifier
            .then(
                if (onClick != null) {
                    Modifier.minimumInteractiveComponentSize()
                } else {
                    Modifier
                },
            ).clip(RoundedCornerShape(80.dp))
            .background(
                when (selected) {
                    true -> colors.selectedContainerColor
                    false -> colors.containerColor
                },
            ).then(
                if (onClick != null) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ).padding(padding),
) {
    Text(
        text = text,
        color =
            when (selected) {
                true -> colors.selectedContentColor
                false -> colors.contentColor
            },
        style = textStyle,
    )
}

@Composable
fun OctopusSmallBadge(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    colors: OctopusBadgeColors = OctopusBadgeDefaults.badgeColors(),
    onClick: (() -> Unit)? = null,
) = OctopusBadge(
    modifier = modifier,
    text = text,
    selected = selected,
    colors = colors,
    textStyle = OctopusTheme.typography.caption1.medium,
    padding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
    onClick = onClick,
)

@Composable
internal fun OctopusAdminBadge(modifier: Modifier = Modifier) =
    OctopusBadge(
        modifier =
            modifier.border(
                width = 1.dp,
                color = OctopusTheme.colorScheme.gray300,
                shape = RoundedCornerShape(80.dp),
            ),
        text = stringResource(R.string.profile_tag_admin),
        colors =
            OctopusBadgeDefaults.badgeColors(
                containerColor = Color.Transparent,
                contentColor = OctopusTheme.colorScheme.gray900,
            ),
        textStyle = OctopusTheme.typography.caption2.medium,
        padding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
    )

@Composable
fun OctopusGamificationBadge(
    level: CommunityConfig.Gamification.Level,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) = with(level) {
    val (backgroundColor, textColor) =
        colors
            ?.let {
                if (OctopusTheme.colorScheme.isDark) it.dark else it.light
            }?.let { it.background.toColor() to it.text.toColor() } ?: return@with

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(80.dp))
                .background(backgroundColor)
                .then(
                    if (onClick != null) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    },
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        name?.let { name ->
            Text(
                modifier =
                    Modifier
                        .padding(
                            start = 8.dp,
                            top = 2.dp,
                            end = 0.dp,
                            bottom = 2.dp,
                        ),
                text = name,
                color = textColor,
                style = OctopusTheme.typography.caption1.medium,
            )
        }
        Icon(
            modifier = Modifier.size(19.dp),
            painter = OctopusTheme.icons.gamification.badge(),
            contentDescription = null,
            tint = textColor,
        )
    }
}

@Composable
fun OctopusGamificationIcon(
    level: CommunityConfig.Gamification.Level,
    modifier: Modifier = Modifier,
) = with(level) {
    val backgroundColor =
        colors
            ?.let {
                if (OctopusTheme.colorScheme.isDark) it.dark else it.light
            }?.background
            ?.toColor() ?: return@with

    Icon(
        modifier = modifier,
        painter = OctopusTheme.icons.gamification.badge(),
        contentDescription = "Gamification Badge",
        tint = backgroundColor,
    )
}

@Preview(showBackground = true)
@Composable
private fun OctopusBadgePreview() {
    OctopusTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OctopusBadge(
                text = "Badge",
                selected = false,
                onClick = {},
            )
            OctopusBadge(
                text = "Badge",
                selected = true,
                onClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusSmallBadgePreview() {
    OctopusTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OctopusSmallBadge(
                text = "Badge",
                selected = false,
                onClick = {},
            )
            OctopusSmallBadge(
                text = "Badge",
                selected = true,
                onClick = {},
            )
            OctopusSmallBadge(
                text = "Alert",
                colors = OctopusBadgeDefaults.badgeColorsAlert(),
                onClick = {},
            )
            OctopusAdminBadge()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusGamificationPreview() {
    OctopusTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MockGamificationLevels.all.forEach { level ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OctopusGamificationBadge(
                        level = level,
                        onClick = {},
                    )
                    OctopusGamificationIcon(level = level)
                }
            }
        }
    }
}
