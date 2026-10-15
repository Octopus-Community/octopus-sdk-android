package com.octopuscommunity.sdk.sample.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.octopuscommunity.sdk.sample.data.SampleConfigSummary
import com.octopuscommunity.sdk.sample.data.pickerUserIds
import com.octopuscommunity.sdk.sample.ui.components.DangerPillButton
import com.octopuscommunity.sdk.sample.ui.components.PrimaryPillButton
import com.octopuscommunity.sdk.sample.ui.components.SampleCard
import com.octopuscommunity.sdk.sample.ui.components.SampleSwitchRow
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton
import com.octopuscommunity.sdk.sample.ui.components.StatusDot
import com.octopuscommunity.sdk.sample.ui.components.sampleAmberColor
import com.octopuscommunity.sdk.sample.ui.components.sampleDangerColor
import com.octopuscommunity.sdk.sample.ui.components.sampleInputChipBorder
import com.octopuscommunity.sdk.sample.ui.components.sampleInputChipColors
import com.octopuscommunity.sdk.sample.ui.components.sampleSuccessColor
import com.octopuscommunity.sdk.sample.ui.components.sampleSuggestionChipBorder
import com.octopuscommunity.sdk.sample.ui.components.sampleSuggestionChipColors
import com.octopuscommunity.sdk.sample.ui.theme.SampleTheme
import org.koin.androidx.compose.koinViewModel

/**
 * Account: the identity the SDK is connected with, its entitlements, and the three session actions.
 *
 * One screen instead of the old Login + Edit user pair. Every action carries exactly one line
 * saying what it does to the session — the difference between `connectUser` and
 * `refreshEntitlements` is the single thing integrators get wrong, and it is not discoverable from
 * two button labels alone.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onEditProfile: () -> Unit,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp)
                .testTag("account-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IdentityCard(state = state, onEditProfile = onEditProfile)

        SampleCard(
            title = "SSO login",
            subtitle = "The identity the sample signs its local JWT with.",
        ) {
            OutlinedTextField(
                value = state.userId,
                onValueChange = viewModel::setUserId,
                label = { Text("User ID") },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("account-user-id-field"),
            )
            if (pickerUserIds.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                value = state.nickname.orEmpty(),
                onValueChange = viewModel::setNickname,
                label = { Text("Nickname") },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("account-nickname-field"),
            )
        }

        EntitlementsCard(state = state, onToggle = viewModel::toggleEntitlement)

        SampleCard(title = "Session") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (state.isSignedIn) {
                    // Two ways to push an entitlement change, and they are not interchangeable:
                    // an entitlement the backend has never seen only reaches it through a new
                    // token, which is why Reconnect is the primary and not the scarier-looking
                    // of the two.
                    ActionBlock(help = "Re-creates the Octopus session with the updated entitlements.") {
                        PrimaryPillButton(
                            text = "Reconnect",
                            onClick = viewModel::reconnect,
                            isLoading = state.isReconnecting,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .testTag("account-reconnect-button"),
                        )
                    }
                    ActionBlock(help = "Lighter SDK-side refresh, keeps the session.") {
                        SecondaryPillButton(
                            text = "Refresh entitlements",
                            onClick = viewModel::refreshEntitlements,
                            isLoading = state.isRefreshing,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .testTag("account-refresh-entitlements-button"),
                        )
                    }
                    DangerPillButton(
                        text = "Disconnect",
                        onClick = viewModel::disconnect,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag("account-disconnect-button"),
                    )
                } else {
                    ActionBlock(
                        help =
                            "Forges a local JWT with these values and calls connectUser. " +
                                "No real identity server is contacted.",
                    ) {
                        PrimaryPillButton(
                            text = "Connect SSO User",
                            onClick = viewModel::reconnect,
                            isLoading = state.isReconnecting,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .testTag("account-connect-button"),
                        )
                    }
                }
            }
        }

        state.error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = sampleDangerColor,
                modifier = Modifier.testTag("account-error"),
            )
        }
        state.message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = sampleSuccessColor,
                modifier = Modifier.testTag("account-message"),
            )
        }
    }
}

/** Avatar + name + the connection pill, above the controls that change them. */
@Composable
private fun IdentityCard(
    state: AccountViewModel.State,
    onEditProfile: () -> Unit,
) {
    SampleCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Avatar(url = state.avatarUrl, initials = state.initials)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.nickname ?: state.userId.ifBlank { "No user" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = state.userId.ifBlank { SampleConfigSummary.UNKNOWN },
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                state.isGuest -> StatusDot(sampleAmberColor, "Guest")
                state.isConnected -> StatusDot(sampleSuccessColor, "Connected")
                else -> StatusDot(sampleDangerColor, "Off")
            }
        }
        if (state.isSignedIn) {
            SecondaryPillButton(
                text = "Edit profile",
                onClick = onEditProfile,
                minHeight = 40,
                modifier = Modifier.testTag("account-edit-profile-button"),
            )
        }
    }
}

@Composable
private fun Avatar(
    url: String?,
    initials: String,
) {
    Box(
        modifier =
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                // DS Avatar: initials on `accent-container`, in `on-accent-container`.
                .background(SampleTheme.colors.accentContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Initials rather than a generic person glyph: on a screen whose whole subject is
            // *which* identity is connected, a silhouette that never changes says nothing.
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SampleTheme.colors.onAccentContainer,
            )
        }
    }
}

/**
 * The entitlements block: the ready-made switches, the free-form additions as removable chips, and
 * one line reconciling what the local record asks for with what the backend granted.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EntitlementsCard(
    state: AccountViewModel.State,
    onToggle: (String, Boolean) -> Unit,
) {
    val known = AccountViewModel.KnownEntitlement.entries
    val knownValues = known.map { it.value }.toSet()
    val custom = (state.entitlements - knownValues).sorted()
    var input by remember { mutableStateOf("") }

    fun commit() {
        val trimmed = input.trim()
        if (trimmed.isNotEmpty()) onToggle(trimmed, true)
        input = ""
    }

    SampleCard(
        title = "Entitlements",
        subtitle = "What this user is allowed to see and do in the community.",
        modifier = Modifier.testTag("account-entitlements-card"),
    ) {
        known.forEach { entitlement ->
            SampleSwitchRow(
                title = entitlement.label,
                subtitle = entitlement.description,
                checked = entitlement.value in state.entitlements,
                onCheckedChange = { onToggle(entitlement.value, it) },
                modifier = Modifier.testTag("account-entitlement-${entitlement.name.lowercase()}"),
            )
        }

        if (custom.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                custom.forEach { value ->
                    InputChip(
                        colors = sampleInputChipColors(),
                        border = sampleInputChipBorder(),
                        selected = true,
                        onClick = {},
                        label = { Text(value) },
                        trailingIcon = {
                            IconButton(onClick = { onToggle(value, false) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove $value")
                            }
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Custom entitlement") },
            placeholder = { Text("e.g. customer:vip") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            trailingIcon = {
                if (input.isNotBlank()) {
                    IconButton(onClick = ::commit) {
                        Icon(Icons.Default.Add, contentDescription = "Add entitlement")
                    }
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("account-custom-entitlement-field"),
        )

        if (state.hasPendingEntitlementChange) {
            Text(
                text = "Pending — the current session still carries the previous set. Reconnect to apply.",
                style = MaterialTheme.typography.bodySmall,
                color = sampleAmberColor,
                modifier = Modifier.testTag("account-entitlements-pending"),
            )
        }
    }
}

/** A single action: the button, then the one line saying what it does to the session. */
@Composable
private fun ActionBlock(
    help: String,
    button: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        button()
        Text(
            text = help,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
