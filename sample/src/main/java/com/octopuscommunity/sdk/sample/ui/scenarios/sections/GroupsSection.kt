package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.sample.ui.components.ApiMethodCard
import com.octopuscommunity.sdk.sample.ui.components.LiveFlowDisplay
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenariosViewModel
import com.octopuscommunity.sdk.sample.ui.theme.SampleTheme

/**
 * Groups section: live groups list with inline follow/unfollow buttons, fetchGroups,
 * and a `syncFollowGroups` card with a 3-choice offset selector (-5 min / now / +5 min).
 */
@Composable
fun GroupsSection(
    groups: List<OctopusGroup>,
    filteredGroups: List<OctopusGroup>,
    groupSearchQuery: String,
    groupIdInput: String,
    loadingGroupIds: Set<String>,
    syncOffset: ScenariosViewModel.SyncOffset,
    getMethodState: (String) -> ScenariosViewModel.MethodState,
    onFetchGroups: () -> Unit,
    onGroupSearchQueryChanged: (String) -> Unit,
    onGroupIdChanged: (String) -> Unit,
    onFollowGroup: (String) -> Unit,
    onUnfollowGroup: (String) -> Unit,
    onSyncOffsetChanged: (ScenariosViewModel.SyncOffset) -> Unit,
    onSyncFollowGroupsInvertAll: () -> Unit,
    onSyncFollowGroupsForceFollowAll: () -> Unit,
    onSyncFollowGroupsForceUnfollowAll: () -> Unit,
    // Catalog QA hooks (shared scenario catalog) — null outside the Scenarios screen, so
    // every other call site of this composable is unaffected.
    fetchGroupsTestTag: String? = null,
    syncInvertAllTestTag: String? = null,
    syncForceFollowAllTestTag: String? = null,
    syncForceUnfollowAllTestTag: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LiveFlowDisplay(
            label = "groups",
            // Show filtered count when a search query is active so the user knows how
            // many groups the filter matched, falling back to the full count otherwise.
            value =
                if (groupSearchQuery.isBlank()) {
                    "${groups.size} group(s) cached"
                } else {
                    "${filteredGroups.size} of ${groups.size} group(s) match"
                },
        )

        // Search / filter the inline groups list (case-insensitive substring on `group.name`).
        // Helps both the QA Tester (find e.g. "📢 Announcements" without scrolling 83+ entries)
        // and human devs exploring the API. The full `groups` list still feeds the sync-batch
        // triggers below (canChangeFollowStatus filter is a separate concern).
        if (groups.isNotEmpty()) {
            OutlinedTextField(
                value = groupSearchQuery,
                onValueChange = onGroupSearchQueryChanged,
                label = { Text("Filter groups") },
                placeholder = { Text("e.g. Announcements, News, Premium…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (groupSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { onGroupSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search",
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        // Group list with inline actions. Tapping a row copies its id into the Group ID field.
        if (filteredGroups.isNotEmpty()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filteredGroups.forEach { group ->
                    GroupRow(
                        group = group,
                        isLoading = group.id in loadingGroupIds,
                        onRowClick = { onGroupIdChanged(group.id) },
                        onFollow = { onFollowGroup(group.id) },
                        onUnfollow = { onUnfollowGroup(group.id) },
                    )
                }
            }
        } else if (groups.isNotEmpty() && groupSearchQuery.isNotBlank()) {
            // Search query produced no match — give the user a clear empty state so they
            // don't think the list disappeared.
            Text(
                text = "No group matches '$groupSearchQuery'",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        ApiMethodCard(
            name = "fetchGroups()",
            description = "Fetch fresh groups from server",
            isLoading = getMethodState("fetchGroups").isLoading,
            onExecute = onFetchGroups,
            cardTestTag = fetchGroupsTestTag,
        )

        // Manual group ID input (tap a group row above to auto-fill).
        OutlinedTextField(
            value = groupIdInput,
            onValueChange = onGroupIdChanged,
            label = { Text("Group ID") },
            placeholder = { Text("Tap a group above or paste an ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ApiMethodCard(
                name = "followGroup(groupId)",
                description = "Follow by ID",
                isLoading = getMethodState("followGroup").isLoading,
                onExecute = { onFollowGroup(groupIdInput.trim()) },
                modifier = Modifier.weight(1f),
            )

            ApiMethodCard(
                name = "unfollowGroup(groupId)",
                description = "Unfollow by ID",
                isLoading = getMethodState("unfollowGroup").isLoading,
                onExecute = { onUnfollowGroup(groupIdInput.trim()) },
                modifier = Modifier.weight(1f),
            )
        }

        // syncFollowGroups(actions) — group three trigger flavours of the same SDK call:
        //   - invert all (toggle current state of every controllable group)
        //   - FOLLOW all (exercises AlreadyFollowed for already-followed groups)
        //   - UNFOLLOW all (exercises AlreadyUnfollowed for already-unfollowed groups)
        // The offset selector sits inside the same container so it's visually clear that
        // `actionDate` applies to all three triggers, not to the individual follow/unfollow
        // cards above. All three triggers share one method state, and the screen's single
        // Result block shows whichever of them fired last.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "syncFollowGroups(actions)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = "actionDate offset",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ScenariosViewModel.SyncOffset.entries.forEachIndexed { index, offset ->
                    SegmentedButton(
                        // DS Segmented: selected on `accent-container`, dividers in `outline`.
                        colors =
                            SegmentedButtonDefaults.colors(
                                activeContainerColor = SampleTheme.colors.accentContainer,
                                activeContentColor = SampleTheme.colors.onAccentContainer,
                                activeBorderColor = SampleTheme.colors.outline,
                                inactiveContainerColor = Color.Transparent,
                                inactiveContentColor = SampleTheme.colors.text,
                                inactiveBorderColor = SampleTheme.colors.outline,
                            ),
                        selected = syncOffset == offset,
                        onClick = { onSyncOffsetChanged(offset) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ScenariosViewModel.SyncOffset.entries.size,
                            ),
                    ) {
                        Text(
                            text =
                                when (offset) {
                                    ScenariosViewModel.SyncOffset.Past5Min -> "now − 5 min"
                                    ScenariosViewModel.SyncOffset.Now -> "now"
                                    ScenariosViewModel.SyncOffset.Future5Min -> "now + 5 min"
                                },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            ApiMethodCard(
                name = "invert all",
                description =
                    "Invert follow state for every controllable group. " +
                        "Exercises Applied (both directions) + Skipped (with past offset).",
                isLoading = getMethodState("syncFollowGroups").isLoading,
                onExecute = onSyncFollowGroupsInvertAll,
                cardTestTag = syncInvertAllTestTag,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ApiMethodCard(
                    name = "FOLLOW all",
                    description =
                        "Send `followed = true` for every controllable group. " +
                            "Exercises AlreadyFollowed + Applied.",
                    isLoading = getMethodState("syncFollowGroups").isLoading,
                    onExecute = onSyncFollowGroupsForceFollowAll,
                    modifier = Modifier.weight(1f),
                    cardTestTag = syncForceFollowAllTestTag,
                )

                ApiMethodCard(
                    name = "UNFOLLOW all",
                    description =
                        "Send `followed = false` for every controllable group. " +
                            "Exercises AlreadyUnfollowed + Applied.",
                    isLoading = getMethodState("syncFollowGroups").isLoading,
                    onExecute = onSyncFollowGroupsForceUnfollowAll,
                    modifier = Modifier.weight(1f),
                    cardTestTag = syncForceUnfollowAllTestTag,
                )
            }
        }
    }
}

@Composable
private fun GroupRow(
    group: OctopusGroup,
    isLoading: Boolean,
    onRowClick: () -> Unit,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
) {
    val statusColor by animateColorAsState(
        targetValue =
            if (group.isFollowed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        label = "statusColor",
    )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onRowClick)
                .defaultMinSize(minHeight = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text =
                    when {
                        !group.canChangeFollowStatus -> "Managed by server"
                        group.isFollowed -> "Followed"
                        else -> "Not followed"
                    },
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
            )
        }

        Box(
            modifier = Modifier.defaultMinSize(minWidth = 88.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else if (group.canChangeFollowStatus) {
                if (group.isFollowed) {
                    SecondaryPillButton(
                        onClick = onUnfollow,
                        minHeight = 0,
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                    ) {
                        Text("Unfollow", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    FilledTonalButton(
                        onClick = onFollow,
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                    ) {
                        Text("Follow", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
