package com.octopuscommunity.sdk.ui.posts.blocks

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.medium

/**
 * Group name rendered as a primary-colored caption chip. Tappable when `onTap` is provided
 * (navigates to the group page).
 */
@Composable
internal fun PostGroupLink(
    groupName: String,
    onTap: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.then(
            if (onTap != null) {
                Modifier.clickable(role = Role.Button, onClick = onTap)
            } else Modifier
        ),
        text = groupName,
        color = OctopusTheme.colorScheme.primary,
        style = OctopusTheme.typography.caption1.medium
    )
}

@Preview(showBackground = true)
@Composable
private fun PostGroupLinkTappablePreview() {
    OctopusTheme { PostGroupLink(groupName = "Groupe", onTap = {}) }
}

@Preview(showBackground = true)
@Composable
private fun PostGroupLinkPlainPreview() {
    OctopusTheme { PostGroupLink(groupName = "Groupe", onTap = null) }
}
