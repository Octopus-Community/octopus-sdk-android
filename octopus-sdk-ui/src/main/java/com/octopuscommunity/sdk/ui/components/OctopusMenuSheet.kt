package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.medium

data class MenuItem(
    val title: String,
    val onClick: () -> Unit,
    val icon: Painter? = null,
    val isDestructive: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OctopusMenuSheet(
    onDismissRequest: () -> Unit,
    items: List<MenuItem>,
    modifier: Modifier = Modifier,
) = OctopusModalBottomSheet(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
    ) {
        items.forEach { item ->
            OctopusMenuSheetItem(
                icon = item.icon,
                title = item.title,
                onClick = item.onClick,
                isDestructive = item.isDestructive,
            )
        }
    }
}

@Composable
internal fun OctopusMenuSheetItem(
    icon: Painter? = null,
    title: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon?.let {
            Icon(
                painter = it,
                contentDescription = null,
                tint =
                    if (isDestructive) {
                        OctopusTheme.colorScheme.alert
                    } else {
                        OctopusTheme.colorScheme.gray900
                    },
            )
        }
        Text(
            text = title,
            color =
                if (isDestructive) {
                    OctopusTheme.colorScheme.alert
                } else {
                    OctopusTheme.colorScheme.gray900
                },
            style = OctopusTheme.typography.body2.medium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusMenuSheetPreview() {
    OctopusTheme {
        OctopusMenuSheet(
            modifier = Modifier.height(250.dp),
            onDismissRequest = {},
            items =
                listOf(
                    MenuItem(
                        title = stringResource(R.string.settings_terms_of_use),
                        onClick = {},
                        icon = OctopusTheme.icons.settings.info(),
                    ),
                    MenuItem(
                        title = stringResource(R.string.settings_report_content),
                        onClick = {},
                    ),
                ),
        )
    }
}
