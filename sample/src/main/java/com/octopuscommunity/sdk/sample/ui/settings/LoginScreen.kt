package com.octopuscommunity.sdk.sample.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.sample.data.pickerUserIds
import com.octopuscommunity.sdk.sample.ui.components.SampleTopAppBar
import com.octopuscommunity.sdk.sample.ui.components.sampleSuggestionChipBorder
import com.octopuscommunity.sdk.sample.ui.components.sampleSuggestionChipColors
import com.octopuscommunity.sdk.sample.ui.theme.samplePageColor
import com.octopuscommunity.sdk.sample.ui.theme.samplePageContentColor
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isConnected) {
        if (state.isConnected) onBack()
    }

    Scaffold(
        containerColor = samplePageColor,
        contentColor = samplePageContentColor,
        topBar = {
            SampleTopAppBar(
                title = "SSO Login",
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Connect SSO User",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text =
                    if (state.managedFields.isNotEmpty()) {
                        "Enter user details to connect via SSO. Fields marked with * are managed by your app."
                    } else {
                        "Enter user details to connect via SSO"
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.forceLogin) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Force login is enabled — users will be prompted to sign in for strong actions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.userId,
                onValueChange = viewModel::setUserId,
                label = { Text("User ID *") },
                supportingText = { Text("Required — unique identifier from your system") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            // Quick-pick chips: tap to fill the User ID field. Lets QA flip
            // identities between two sample instances (e.g. Android emulator +
            // iOS sim) without rebuilding — required for end-to-end push QA.
            // Hidden when no ids are predefined / seeded.
            if (pickerUserIds.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    pickerUserIds.forEach { id ->
                        SuggestionChip(
                            colors = sampleSuggestionChipColors(),
                            border = sampleSuggestionChipBorder(),
                            onClick = { viewModel.setUserId(id) },
                            label = { Text(id) },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = state.nickname,
                onValueChange = viewModel::setNickname,
                label = {
                    Text(if (state.isNicknameManaged) "Nickname * (app-managed)" else "Nickname")
                },
                supportingText =
                    if (state.isNicknameManaged) {
                        { Text("Managed by your app — Octopus won't modify it") }
                    } else {
                        null
                    },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.bio,
                onValueChange = viewModel::setBio,
                label = {
                    Text(if (state.isBioManaged) "Bio * (app-managed)" else "Bio")
                },
                supportingText =
                    if (state.isBioManaged) {
                        { Text("Managed by your app — Octopus won't modify it") }
                    } else {
                        null
                    },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Entitlements",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Toggle entitlements granted to this user. They are embedded in the JWT minted at connect time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EditEntitlementsList(
                selection = state.entitlements,
                onToggle = viewModel::toggleEntitlement,
            )

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::connect,
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
                Text("Connect", fontWeight = FontWeight.Bold)
            }
        }
    }
}
