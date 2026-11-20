package com.octopuscommunity.sdk.ui.posts.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.posts.PostTag
import com.octopuscommunity.sdk.ui.semiBold

/**
 * Row of status tags for a post — shown between the header and the content (catch phrase /
 * text). Iso iOS `PostTagsView`. Renders nothing when [tags] is empty.
 *
 * Currently the only tag surfaced on Android is [PostTag.Moderated] (alert-colored "Modéré"
 * pill with a shield-with-slash glyph).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PostTags(
    tags: Set<PostTag>,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty()) return
    FlowRow(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tags.forEach { tag ->
            when (tag) {
                PostTag.Moderated ->
                    ContentTagBadge(
                        icon =
                            OctopusTheme.icons.content.post
                                .moderated(),
                        text = stringResource(R.string.post_status_moderated),
                        contentColor = OctopusTheme.colorScheme.alert,
                        containerColor = OctopusTheme.colorScheme.alertLow,
                    )
            }
        }
    }
}

/**
 * Internal pill-shaped status badge with a leading icon, used by [PostTags] and
 * `ResponseTags`. Iso iOS `PostTagBadge` (private in `PostTagsView.swift`).
 *
 * Specs (Figma SDK Design System, node 2007:5203):
 * - Container: pill, padding `start=4 end=8 top=2 bottom=2`, gap 2dp
 * - Icon: 22dp container — the underlying drawable carries a 24-unit viewport
 *   with the shield path centered inside (~12×15 effective), so the visible
 *   glyph spans ~11×14dp at 22dp, matching Figma `Tags view / moderation`
 *   instance. iOS uses 16pt which renders smaller than Figma — Android picks
 *   the Design System reference; cross-platform note in the PR description.
 * - Text: caption2 (12sp) Semibold, [contentColor]
 */
@Composable
internal fun ContentTagBadge(
    icon: Painter,
    text: String,
    contentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(80.dp))
                .background(containerColor)
                .padding(start = 4.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = icon,
            contentDescription = null,
            tint = contentColor,
        )
        Text(
            text = text,
            color = contentColor,
            style = OctopusTheme.typography.caption2.semiBold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PostTagsModeratedPreview() {
    OctopusTheme { PostTags(tags = setOf(PostTag.Moderated)) }
}
