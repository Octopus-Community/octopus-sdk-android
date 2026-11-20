package com.octopuscommunity.sdk.ui.posts.blocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.components.OctopusMediumButton

/**
 * CTA button block (bridge "open client object" or openURL custom action). The orchestrator
 * pre-resolves the text and wires the click to the appropriate navigation handler.
 */
@Composable
internal fun PostCTAContent(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Figma `Content CTA` (node 1870:19137): `px-[16px] py-[8px]` on the CTA container.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        OctopusMediumButton(text = text, onClick = onClick)
    }
}

@Preview(showBackground = true)
@Composable
private fun PostCTAContentPreview() {
    OctopusTheme {
        PostCTAContent(text = "Open on partner site", onClick = {})
    }
}
