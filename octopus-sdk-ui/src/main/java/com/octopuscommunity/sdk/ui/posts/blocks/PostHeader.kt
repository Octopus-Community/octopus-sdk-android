package com.octopuscommunity.sdk.ui.posts.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.MinimalProfile
import com.octopuscommunity.sdk.domain.model.Profile
import com.octopuscommunity.sdk.test.mock.MockMinimalProfiles
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.components.OctopusAdminBadge
import com.octopuscommunity.sdk.ui.components.OctopusGamificationIcon
import com.octopuscommunity.sdk.ui.lineHeight150Trimmed
import com.octopuscommunity.sdk.ui.profile.components.ProfileAvatar
import com.octopuscommunity.sdk.ui.semiBold
import com.octopuscommunity.sdk.ui.utils.ElapsedTime

/**
 * Top row of a post card — aligned with Figma `Post header` (node 33:4245 with group, 2150:4800
 * without group):
 * - Outer Row: `paddingLeading:16`, `paddingBottom:4` (bottom baked into the header, not the
 *   next content block). `Modifier.height(IntrinsicSize.Min)` lets the Content column stretch
 *   to the avatar's height so the no-group variant can vertically center its single line.
 * - Avatar: 40dp, `paddingTop:16` baked inside iso Figma `pt-[16px]` on the Avatar container.
 * - Avatar ↔ Content: 8dp spacer iso Figma `gap-[8px]` on the Post header outer Row.
 * - Content column: `paddingTop:16` iso Figma `pt-[16px]`, `fillMaxHeight()` + `Arrangement.Center`
 *   iso Figma `self-stretch justify-center` on the Content column. With one row (no group), the
 *   single line sits at the avatar's vertical mid-line; with two rows, the column already fills
 *   the available height so `Center` is a no-op and the layout matches Figma's two-line case.
 *   VStack spacing 2pt between Profil+Date row and Group link row.
 * - Author padding (16dp above, half the gap below) stops before the group. Each label keeps
 *   its own Text button; automatic minimum-target expansion is disabled only for these labels.
 *   Opt-in group input fills the free header row, capped at the header bottom and the adjacent
 *   avatar/menu/translation touch bounds. It never covers post text or changes accessibility geometry.
 *   The invisible pointer area shares the Text's interaction source and indication.
 * - Profil+Date row: name `|spacing:0|` (gamif or admin badge with `paddingLeading:4`),
 *   then `|spacing:4|` dot `|spacing:4|` date — iso Figma `gap-[4px]`.
 * - More menu: flush to card's right edge (no trailing padding when shown), retaining the
 *   platform's 48dp minimum touch target around its 44dp layout.
 */
@Composable
internal fun PostHeader(
    profile: MinimalProfile?,
    authorName: String,
    relativeDate: ElapsedTime,
    gamificationLevel: CommunityConfig.Gamification.Level?,
    groupName: String?,
    onProfileClick: (() -> Unit)?,
    onGroupClick: (() -> Unit)?,
    onMoreActionsClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    extendGroupTouchTarget: Boolean = false,
    sharedGroupTouchTarget: PostHeaderGroupTouchTarget? = null,
) {
    val groupTouchTarget =
        sharedGroupTouchTarget ?: rememberPostHeaderGroupTouchTarget(
            onClick = onGroupClick,
            extendToHeaderBottom = extendGroupTouchTarget,
            groupName = groupName,
        )
    val groupArea = groupTouchTarget.area
    val extraInput =
        if (groupTouchTarget.enabled && sharedGroupTouchTarget == null) {
            Modifier.onGloballyPositioned { groupArea.input = it }.postHeaderGroupTouchArea(groupTouchTarget)
        } else {
            Modifier
        }
    Box(modifier = modifier.then(extraInput).onGloballyPositioned { groupArea.header = it }) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .padding(bottom = 4.dp)
                    .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top,
        ) {
            // Avatar: 40dp with 16dp top padding iso Figma `pt-[16px]` on the Avatar container.
            ProfileAvatar(
                modifier =
                    Modifier
                        .padding(top = 16.dp)
                        .size(40.dp)
                        .onGloballyPositioned { groupArea.avatar = it },
                profile = profile,
                onClick = onProfileClick,
            )
            // 8dp between avatar and content iso Figma `gap-[8px]` on the Post header outer Row.
            Spacer(modifier = Modifier.width(8.dp))
            // Content column: 16dp top iso Figma `pt-[16px]` + VStack spacing 2dp between rows.
            // `fillMaxHeight()` + `Arrangement.Center` iso Figma `self-stretch justify-center` —
            // when there is no group row, the single Profil+Date line vertically centers against
            // the avatar instead of sticking to the top.
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(top = 16.dp)
                        .onGloballyPositioned { groupArea.column = it },
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PostHeaderTouchTarget(
                        modifier = Modifier.weight(1f, fill = false),
                        onClick = onProfileClick,
                        aboveGroup = true,
                        enabled = groupName != null,
                    ) { labelModifier ->
                        Text(
                            modifier = labelModifier.onGloballyPositioned { groupArea.author = it },
                            text = authorName,
                            color = OctopusTheme.colorScheme.gray900,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            // Figma `Profil + Date` author: `body-2-semibold` 16sp · `leading-[24px]`.
                            // `lineHeight150Trimmed` (with Trim.Both via the extension) sets the line-box to
                            // exactly 24sp matching Figma's `leading-[24px]`.
                            style = OctopusTheme.typography.body2.semiBold.lineHeight150Trimmed,
                        )
                    }
                    // Admin XOR Gamification — iso iOS `AuthorAndDateHeaderView`:
                    // if admin tag, the admin pill replaces the gamification icon. iOS HStack
                    // spacing:0 inside authorView, so admin badge adds its own 4dp leading padding,
                    // while gamification icon touches the name directly.
                    if (profile?.tags?.contains(Profile.Tag.ADMIN) == true) {
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
                if (groupName != null) {
                    // VStack spacing: 2 between name row and group link — iso iOS.
                    Spacer(modifier = Modifier.height(2.dp))
                    PostHeaderTouchTarget(
                        onClick = onGroupClick,
                        aboveGroup = false,
                        interactionSource = groupTouchTarget.source,
                    ) { labelModifier ->
                        PostGroupLink(
                            groupName = groupName,
                            onTap = null,
                            modifier =
                                labelModifier.onGloballyPositioned {
                                    groupArea.label =
                                        it
                                },
                        )
                    }
                }
            }
            if (onMoreActionsClick != null) {
                PostMoreMenuButton(onClick = onMoreActionsClick, modifier = Modifier.onGloballyPositioned { groupArea.menu = it })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PostHeaderSummaryPreview() {
    OctopusTheme {
        PostHeader(
            profile = MockMinimalProfiles.default,
            authorName = "Antoine",
            relativeDate = ElapsedTime(text = "3min", accessibilityText = "il y a 3 minutes"),
            gamificationLevel = null,
            groupName = "Groupe",
            onProfileClick = {},
            onGroupClick = {},
            onMoreActionsClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PostHeaderNoGroupPreview() {
    OctopusTheme {
        PostHeader(
            profile = MockMinimalProfiles.default,
            authorName = "Antoine",
            relativeDate = ElapsedTime(text = "3min", accessibilityText = "il y a 3 minutes"),
            gamificationLevel = null,
            groupName = null,
            onProfileClick = {},
            onGroupClick = null,
            onMoreActionsClick = null,
        )
    }
}
