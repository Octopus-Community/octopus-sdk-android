package com.octopuscommunity.sdk.ui.responses.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.Profile
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.components.OctopusAdminBadge
import com.octopuscommunity.sdk.ui.components.OctopusGamificationIcon
import com.octopuscommunity.sdk.ui.lineHeight150Trimmed
import com.octopuscommunity.sdk.ui.semiBold
import com.octopuscommunity.sdk.ui.utils.ElapsedTime

/**
 * Header row inside a response card: author name + admin badge + separator dot + date · optional
 * trailing "⋯". Iso iOS `ResponseHeaderView`.
 *
 * Strings are pre-resolved by the orchestrator. Admin tag surfacing depends on the author's tags.
 */
@Composable
internal fun ResponseHeader(
    authorName: String,
    authorTags: List<Profile.Tag>,
    gamificationLevel: CommunityConfig.Gamification.Level?,
    relativeDate: ElapsedTime,
    onProfileClick: () -> Unit,
    onMoreActionsClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        // Aligned with Figma `Comment header` (node 1953:11470): outer container has only
        // `pl-[12px]`; the more-actions button is positioned via spacing on the parent Row
        // (8dp gap iso the Comment Card outer `gap-[8px]`).
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Inner row for Profil+Date — Figma `Profil + Date` (node I1953:11470;2209:14032):
        // `pt-[10px] pb-[4px]` and `h-[37px]` for the entire Comment header. Pinning the row
        // height to 37dp guarantees the outer Row matches the Figma header height even though
        // the row's natural content (body-2-semibold 16sp ≈ 19sp tall + 14dp padding = 33dp) is
        // shorter than the more-menu (44 × 37dp). Without an explicit height, the more-menu
        // would dictate the outer Row height (37dp) while the inner row stayed at 33dp,
        // leaving ~4dp of dead space between the header text and the body text below.
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .height(37.dp)
                    .padding(top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .clickable(role = Role.Button, onClick = onProfileClick),
                text = authorName,
                color = OctopusTheme.colorScheme.gray900,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                // Figma author: `body-2-semibold` 16sp · `leading-[24px]`. `lineHeight150Trimmed`
                // gives the line-box exactly 24sp so it matches the Figma comp height inside
                // the 37dp Comment header row and the glyph lands at the same Y as Figma's.
                style = OctopusTheme.typography.body2.semiBold.lineHeight150Trimmed,
            )
            // Admin XOR Gamification — iso iOS `AuthorAndDateHeaderView`: iOS inner HStack
            // spacing:0 for name+badge; admin adds its own 4dp leading pad, gamif touches name.
            if (authorTags.contains(Profile.Tag.ADMIN)) {
                Spacer(modifier = Modifier.width(4.dp))
                OctopusAdminBadge()
            } else if (gamificationLevel != null) {
                OctopusGamificationIcon(
                    modifier = Modifier.size(19.dp),
                    level = gamificationLevel,
                )
            }
            // 4dp between authorView and dot (iOS HStack spacing: 4).
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier =
                    Modifier
                        .size(2.dp)
                        .background(
                            color = OctopusTheme.colorScheme.gray900,
                            shape = CircleShape,
                        ),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = relativeDate.text,
                modifier = Modifier.semantics { contentDescription = relativeDate.accessibilityText },
                color = OctopusTheme.colorScheme.gray500,
                style = OctopusTheme.typography.caption1,
            )
        }
        if (onMoreActionsClick != null) {
            ResponseMoreMenuButton(onClick = onMoreActionsClick)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResponseHeaderPreview() {
    OctopusTheme {
        ResponseHeader(
            authorName = "Antoine",
            authorTags = emptyList(),
            gamificationLevel = null,
            relativeDate = ElapsedTime(text = "3min", accessibilityText = "il y a 3 minutes"),
            onProfileClick = {},
            onMoreActionsClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResponseHeaderAdminPreview() {
    OctopusTheme {
        ResponseHeader(
            authorName = "Marie Dupont",
            authorTags = listOf(Profile.Tag.ADMIN),
            gamificationLevel = null,
            relativeDate = ElapsedTime(text = "1h", accessibilityText = "il y a 1 heure"),
            onProfileClick = {},
            onMoreActionsClick = null,
        )
    }
}
