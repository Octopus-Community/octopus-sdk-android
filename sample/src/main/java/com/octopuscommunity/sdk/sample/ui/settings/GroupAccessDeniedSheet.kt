package com.octopuscommunity.sdk.sample.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.extensions.LocalExtensions
import com.octopuscommunity.sdk.sample.ui.theme.SampleModalBottomSheet
import org.koin.androidx.compose.koinViewModel

/**
 * Modal bottom sheet shown when the SDK invokes the `onGroupAccessDenied` callback. Mirrors
 * the iOS `GroupAccessDeniedScreen` (internal change): displays the locked group's name, lets the
 * tester toggle entitlements, and triggers `OctopusSDK.refreshEntitlements()` on
 * "Save & retry" — closing automatically once the refresh succeeds.
 *
 * Stays focused on the two known toggles. Custom claims editing is intentionally not exposed
 * here to keep the unlock flow short; the [onNavigateToEditProfile] callback opens the full
 * editor instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupAccessDeniedSheet(
    groupId: String,
    onDismiss: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    serverEnvironment: SDKConfig.ServerEnvironment,
    viewModel: EditUserViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var groupName by remember(groupId) { mutableStateOf(groupId) }
    LaunchedEffect(groupId) {
        // Lookup the group's display name from the SDK's cache; fall back to the id.
        when (val result = OctopusSDK.fetchGroups()) {
            is OctopusResult.Success ->
                groupName = result.data.firstOrNull { it.id == groupId }?.name ?: groupId

            is OctopusResult.Failure -> {
                // Keep the id as a fallback.
            }
        }
    }

    // Capture the refresh count at open time; only dismiss when it increments. Using a counter
    // (rather than reacting to `entitlementsRefreshMessage`) avoids the stale-state race where
    // the VM, scoped to the Activity, retains the last success message and would auto-dismiss
    // the sheet immediately on a subsequent open.
    val initialRefreshCount = remember(groupId) { state.entitlementsRefreshCount }
    LaunchedEffect(state.entitlementsRefreshCount) {
        if (state.entitlementsRefreshCount > initialRefreshCount) {
            onDismiss()
        }
    }

    SampleModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            // A ModalBottomSheet renders into its own Window / SemanticsOwner, outside the
            // Activity-root flag hoisted in MainActivity.kt — so this composition root needs
            // its own testTagsAsResourceId, ancestor of every tagged node below.
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { testTagsAsResourceId = true },
        ) {
            LocalExtensions.WarningBanner(serverEnvironment = serverEnvironment)
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text =
                        "This group is not accessible to your current entitlements. " +
                            "Adjust them below to gain access.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()

                EditEntitlementsList(
                    selection = state.entitlements,
                    onToggle = viewModel::toggleEntitlement,
                    allowCustomClaims = false,
                )

                TextButton(
                    onClick = {
                        onDismiss()
                        onNavigateToEditProfile()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Need a custom entitlement? Edit profile")
                }

                Button(
                    onClick = viewModel::refreshEntitlements,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isRefreshingEntitlements,
                ) {
                    if (state.isRefreshingEntitlements) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier
                                    .padding(end = 8.dp)
                                    .size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(if (state.isRefreshingEntitlements) "Saving…" else "Save & retry")
                }

                if (state.error != null) {
                    Text(
                        text = state.error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
