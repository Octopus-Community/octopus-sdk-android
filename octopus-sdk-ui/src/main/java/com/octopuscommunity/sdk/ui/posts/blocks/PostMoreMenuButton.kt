package com.octopuscommunity.sdk.ui.posts.blocks

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource

/**
 * Trailing "⋯" icon button that opens the post's moderation / deletion menu. The menu sheet
 * itself (`PostMenuSheet`) is rendered by the orchestrating `PostCard` — this block is just
 * the trigger. 24dp icon inside a 44dp touch target (iso iOS `PostMoreMenuButton`).
 */
@Composable
internal fun PostMoreMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        modifier = modifier.size(44.dp),
        onClick = onClick,
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor = OctopusTheme.colorScheme.gray500,
            ),
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = OctopusTheme.icons.moreActions(),
            contentDescription = stringResource(R.string.accessibility_common_more),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PostMoreMenuButtonPreview() {
    OctopusTheme { PostMoreMenuButton(onClick = {}) }
}
