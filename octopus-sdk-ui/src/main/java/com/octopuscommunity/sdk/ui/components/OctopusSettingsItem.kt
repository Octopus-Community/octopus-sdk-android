package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.medium

@Composable
internal fun OctopusSettingsItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    color: Color = OctopusTheme.colorScheme.gray900,
    disabledColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val resolvedDisabledColor =
        if (disabledColor.isSpecified) {
            disabledColor
        } else if (loading) {
            OctopusTheme.colorScheme.gray200
        } else {
            OctopusTheme.colorScheme.gray500
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable {
                    if (enabled) {
                        onClick()
                    }
                }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (loading) {
            OctopusProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = color,
            )
        } else {
            icon?.let {
                Icon(
                    painter = it,
                    contentDescription = null,
                    tint = if (enabled) color else resolvedDisabledColor,
                )
            }
        }
        Text(
            text = text,
            color = if (enabled) color else resolvedDisabledColor,
            style = OctopusTheme.typography.body2.medium,
        )
    }
}

@Preview
@Composable
private fun OctopusSettingsItemPreview() {
    OctopusTheme {
        OctopusSettingsItem(
            icon = OctopusTheme.icons.settings.info(),
            text = "Info",
            onClick = {},
        )
    }
}
