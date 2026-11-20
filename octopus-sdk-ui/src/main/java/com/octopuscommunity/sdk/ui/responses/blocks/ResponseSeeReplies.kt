package com.octopuscommunity.sdk.ui.responses.blocks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.semiBold
import com.octopuscommunity.sdk.ui.utils.currentLocale
import com.octopuscommunity.sdk.ui.utils.toCompactString

/**
 * "See N replies" row displayed under a comment when it has replies. Iso iOS
 * `ResponseSeeRepliesView`. The horizontal alignment with the comment text block is owned by
 * the orchestrator (not by this block) — this block fills its parent horizontally.
 */
@Composable
internal fun ResponseSeeReplies(
    replyCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.currentLocale
    val label =
        pluralStringResource(
            R.plurals.reply_see_count,
            replyCount,
            replyCount.toCompactString(locale),
        )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .defaultMinSize(
                    minWidth = ButtonDefaults.MinWidth,
                    minHeight = ButtonDefaults.MinHeight,
                ).padding(bottom = 12.dp)
                .semantics(mergeDescendants = true) { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            contentDescription = null,
            painter =
                OctopusTheme.icons.content.comment
                    .seeReplies(),
            tint = OctopusTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = OctopusTheme.colorScheme.primary,
            style = OctopusTheme.typography.caption1.semiBold,
        )
    }
}

/**
 * "N other replies" row shown under a reply's own bubble in the profile Comments tab (internal ticket):
 * the parent comment's *other* replies (its `childCount` minus the member's own reply), so the
 * member isn't shown "1 reply" to their own comment. Visually identical to [ResponseSeeReplies]
 * (same icon/typography) but with the "other" plural wording — iso iOS
 * `SeeRepliesRow(otherWording: true)`. Hidden when [otherReplyCount] is `0` or less, mirroring iOS.
 */
@Composable
internal fun ResponseSeeOtherReplies(
    otherReplyCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (otherReplyCount <= 0) return

    val locale = LocalConfiguration.current.currentLocale
    val label =
        pluralStringResource(
            R.plurals.profile_comments_see_other_replies,
            otherReplyCount,
            otherReplyCount.toCompactString(locale),
        )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .defaultMinSize(
                    minWidth = ButtonDefaults.MinWidth,
                    minHeight = ButtonDefaults.MinHeight,
                ).padding(bottom = 12.dp)
                .semantics(mergeDescendants = true) { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            contentDescription = null,
            painter =
                OctopusTheme.icons.content.comment
                    .seeReplies(),
            tint = OctopusTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = OctopusTheme.colorScheme.primary,
            style = OctopusTheme.typography.caption1.semiBold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResponseSeeRepliesPreview() {
    OctopusTheme { ResponseSeeReplies(replyCount = 18, onClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun ResponseSeeOtherRepliesPreview() {
    OctopusTheme { ResponseSeeOtherReplies(otherReplyCount = 3, onClick = {}) }
}
