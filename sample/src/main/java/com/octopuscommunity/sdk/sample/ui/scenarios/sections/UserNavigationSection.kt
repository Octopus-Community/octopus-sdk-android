package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.sample.data.SampleFixtures
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton

/**
 * Unified Profile, on-demand Activity: opens a member's posts view via `OctopusSDK`'s
 * `navigateToOctopusActivity(userId)` (wired as [onOpenUserActivity], catalog
 * `qa-preset-initialScreen-9`) or `navigateToOctopusActivityByClientUserId(clientUserId)` (wired as
 * [onOpenUserActivityByClientUserId], catalog `qa-preset-initialScreen-8`). Paste any id — e.g.
 * copied from a `ScreenDisplayed.OtherUserProfile` event in the Events log — or rely on the seeds
 * below.
 *
 * Restored from the pre-internal change `ExplorerScreen` accordion's "User activity" section, now attached to
 * the `initialScreen` scenario screen: `scenario catalog` (shared) carries
 * these two behaviors as `initialScreen` presets 8 and 9 ("Open member activity (by clientUserId)" /
 * "(by profileId)"), not as anything under `communityData`.
 *
 * [SampleFixtures.userOther] seeds the profileId field, and [connectedClientUserId] (the host-side login id, from `UserDataStore`) seeds
 * the clientUserId field — each once, so a first tap resolves without requiring the tester to paste
 * an id manually, and neither ever overwrites a value the tester already typed.
 */
@Composable
fun UserActivitySection(
    connectedProfileUserId: String?,
    connectedClientUserId: String?,
    onOpenUserActivity: (userId: String) -> Unit,
    onOpenUserActivityByClientUserId: (clientUserId: String) -> Unit,
) {
    var activityUserId by rememberSaveable { mutableStateOf(SampleFixtures.userOther) }
    LaunchedEffect(connectedProfileUserId) {
        if (activityUserId.isBlank() && connectedProfileUserId != null) {
            activityUserId = connectedProfileUserId
        }
    }
    var activityClientUserId by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(connectedClientUserId) {
        if (activityClientUserId.isBlank() && connectedClientUserId != null) {
            activityClientUserId = connectedClientUserId
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "User activity",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "Open a member's posts (navigateToOctopusActivity). Title reads \"{author}'s Posts\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = activityUserId,
            onValueChange = { activityUserId = it },
            label = { Text("User ID") },
            modifier = Modifier.fillMaxWidth().testTag("user-activity-input"),
            singleLine = true,
        )
        SecondaryPillButton(
            onClick = { if (activityUserId.isNotBlank()) onOpenUserActivity(activityUserId.trim()) },
            enabled = activityUserId.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-initialScreen-9"),
        ) {
            Text("Open User Activity")
        }

        HorizontalDivider()

        Text(
            text = "navigateToOctopusActivityByClientUserId(clientUserId)",
            style = MaterialTheme.typography.labelLarge,
        )
        // An id that does not resolve ends on the SDK's error screen by contract (it never falls
        // back to your own activity), so say up front what the lookup needs.
        Text(
            text = CLIENT_USER_ID_LOOKUP_HINT,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("user-activity-by-clientuserid-hint"),
        )
        OutlinedTextField(
            value = activityClientUserId,
            onValueChange = { activityClientUserId = it },
            label = { Text("Client User ID") },
            modifier = Modifier.fillMaxWidth().testTag("user-activity-by-clientuserid-input"),
            singleLine = true,
        )
        SecondaryPillButton(
            onClick = {
                if (activityClientUserId.isNotBlank()) {
                    onOpenUserActivityByClientUserId(activityClientUserId.trim())
                }
            },
            enabled = activityClientUserId.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-initialScreen-8"),
        ) {
            Text("Open User Activity By Client User ID")
        }
    }
}

/**
 * Unified Profile, on-demand Profile: opens a member's public, read-only profile via
 * `navigateToOctopusProfile(userId)` / `navigateToOctopusProfileByClientUserId(clientUserId)`, or
 * the connected user's own (editable) profile via [onOpenMyProfile]. Self mode reuses the existing
 * self-profile destination directly — `navigateToOctopusProfile` has no no-arg self overload.
 *
 * Restored from the pre-internal change `ExplorerScreen` accordion's "User profile" section, now attached to
 * the `initialScreen` scenario screen (`scenario catalog`, shared):
 * [onOpenProfileByClientUserId] is preset 10 ("Open member profile (OctopusProfileScreen)" — the
 * catalog label is generic but the reference Flutter scenario resolves it to the clientUserId
 * overload specifically) and [onOpenMyProfile] is preset 11 ("Open my own profile (no id)").
 * [onOpenProfileByUserId] (`navigateToOctopusProfile(userId)`, the plain-SDK-userId overload) has no
 * corresponding preset among 8-11 — it is kept as a plain debug affordance, same as pre-internal change, and
 * deliberately left off the `qa-preset-*` contract rather than given an invented id.
 *
 * [SampleFixtures.userOther] seeds the "by user id" field and [connectedClientUserId] (the host-side
 * login id, from `UserDataStore`) seeds the "by client user id" field, both once and without
 * overwriting a value the tester already typed.
 */
@Composable
fun UserProfileSection(
    connectedProfileUserId: String?,
    connectedClientUserId: String?,
    onOpenMyProfile: () -> Unit,
    onOpenProfileByUserId: (userId: String) -> Unit,
    onOpenProfileByClientUserId: (clientUserId: String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "User profile",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "Open a member's public profile (read-only) or your own editable profile.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SecondaryPillButton(
            onClick = onOpenMyProfile,
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-initialScreen-11"),
        ) {
            Text("Open My Profile")
        }

        HorizontalDivider()

        var profileUserId by rememberSaveable { mutableStateOf(SampleFixtures.userOther) }
        LaunchedEffect(connectedProfileUserId) {
            if (profileUserId.isBlank() && connectedProfileUserId != null) {
                profileUserId = connectedProfileUserId
            }
        }
        Text(text = "navigateToOctopusProfile(userId)", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = profileUserId,
            onValueChange = { profileUserId = it },
            label = { Text("User ID") },
            modifier = Modifier.fillMaxWidth().testTag("user-profile-by-userid-input"),
            singleLine = true,
        )
        SecondaryPillButton(
            onClick = { if (profileUserId.isNotBlank()) onOpenProfileByUserId(profileUserId.trim()) },
            enabled = profileUserId.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("user-profile-by-userid-open"),
        ) {
            Text("Open Profile By User ID")
        }

        HorizontalDivider()

        var profileClientUserId by rememberSaveable { mutableStateOf("") }
        LaunchedEffect(connectedClientUserId) {
            if (profileClientUserId.isBlank() && connectedClientUserId != null) {
                profileClientUserId = connectedClientUserId
            }
        }
        Text(
            text = "navigateToOctopusProfileByClientUserId(clientUserId)",
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = CLIENT_USER_ID_LOOKUP_HINT,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = profileClientUserId,
            onValueChange = { profileClientUserId = it },
            label = { Text("Client User ID") },
            modifier = Modifier.fillMaxWidth().testTag("user-profile-by-clientuserid-input"),
            singleLine = true,
        )
        SecondaryPillButton(
            onClick = {
                if (profileClientUserId.isNotBlank()) {
                    onOpenProfileByClientUserId(profileClientUserId.trim())
                }
            },
            enabled = profileClientUserId.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-initialScreen-10"),
        ) {
            Text("Open Profile By Client User ID")
        }
    }
}

/**
 * Why a client-user-id lookup can end on the SDK's "Oops… Retry" screen: the backend resolves the
 * id only when the community exposes client user ids (Unified Profile on, from Config or the
 * backend) and only for a host id that has already connected to that community. The field is
 * seeded with the id you are connected with in the sample, which is not enough on a community
 * where Unified Profile is off.
 */
private const val CLIENT_USER_ID_LOOKUP_HINT =
    "Resolves only when Unified Profile is on for this community and the id has connected to it. " +
        "Otherwise the SDK shows its error screen, by design."
