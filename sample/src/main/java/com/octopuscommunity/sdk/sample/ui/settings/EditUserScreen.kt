package com.octopuscommunity.sdk.sample.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.sample.ui.components.SampleTopAppBar
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton
import com.octopuscommunity.sdk.sample.ui.theme.samplePageColor
import com.octopuscommunity.sdk.sample.ui.theme.samplePageContentColor
import org.koin.androidx.compose.koinViewModel

/**
 * Stand-in for a host app's own profile *editor*. The mirror of [ClientProfileScreen] (the host's
 * profile *view*): the SDK hands the edit intent to the app via `onNavigateToProfileEdit` (Unified
 * Profile), and the host owns the whole edit flow. Kept visually consistent with that view screen —
 * a centered header, then the editable fields and host-owned entitlements.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserScreen(
    onBack: () -> Unit,
    viewModel: EditUserViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    Scaffold(
        containerColor = samplePageColor,
        contentColor = samplePageContentColor,
        topBar = {
            SampleTopAppBar(
                title = "Edit profile (host app)",
                showPlatformBadge = false,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Centered header — mirrors ClientProfileScreen's "Host-owned profile screen" block so
            // the host's view and edit surfaces read as one family.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Host-owned profile editor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text =
                        "The Octopus SDK handed this profile edit to the app via its " +
                            "onNavigateToProfileEdit callback (Unified Profile).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()

            OutlinedTextField(
                value = state.nickname,
                onValueChange = viewModel::setNickname,
                label = { Text("Nickname") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.bio,
                onValueChange = viewModel::setBio,
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            HorizontalDivider()

            Text(
                text = "Entitlements",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text =
                    "Toggle entitlements granted to this user. Use \"Save\" to reconnect the user " +
                        "with the new entitlements, or \"Refresh entitlements\" for a lighter SDK-side " +
                        "refresh that does not re-create the Octopus session.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            EditEntitlementsList(
                selection = state.entitlements,
                onToggle = viewModel::toggleEntitlement,
            )

            SecondaryPillButton(
                onClick = viewModel::refreshEntitlements,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && !state.isRefreshingEntitlements,
            ) {
                if (state.isRefreshingEntitlements) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text("Refresh entitlements")
            }
            if (state.entitlementsRefreshMessage != null) {
                Text(
                    text = state.entitlementsRefreshMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            HorizontalDivider()

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = viewModel::save,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                enabled = !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }
    }
}
