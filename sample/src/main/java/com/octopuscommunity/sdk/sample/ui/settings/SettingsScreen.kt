package com.octopuscommunity.sdk.sample.ui.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.sample.BuildConfig
import com.octopuscommunity.sdk.sample.data.SampleConfigSummary
import com.octopuscommunity.sdk.sample.data.SampleSdkStatus
import com.octopuscommunity.sdk.sample.extensions.LocalExtensions
import com.octopuscommunity.sdk.sample.ui.components.SampleCard
import com.octopuscommunity.sdk.sample.ui.components.SampleInfoRow
import com.octopuscommunity.sdk.sample.ui.components.StatusDot
import com.octopuscommunity.sdk.sample.ui.components.SummaryGroupHeader
import com.octopuscommunity.sdk.sample.ui.components.SummaryRow
import com.octopuscommunity.sdk.sample.ui.components.dotColor
import com.octopuscommunity.sdk.sample.ui.components.rememberIsOnline
import com.octopuscommunity.sdk.sample.ui.components.sampleDangerColor
import com.octopuscommunity.sdk.sample.ui.theme.SampleMotion
import com.octopuscommunity.sdk.sample.ui.theme.SampleSpacing
import org.koin.androidx.compose.koinViewModel

/**
 * Settings is a pure summary: one row = one navigation, a chevron on every row, and a subtitle
 * that reads the current value rather than describing what the row is for.
 *
 * It used to carry controls of its own (a Disconnect action, three read-only debug panels), which
 * is how a "settings" screen ends up being the place where a destructive action hides one tap from
 * the language picker. Everything that acts now lives on the screen that owns the state.
 *
 * Reset Configuration is the one exception, and it is placed rather than tolerated: its own group,
 * red, last on screen, behind a confirmation. It is where the design cards put it across the
 * samples — a tester looking for "start over" looks in Settings, and sending them to About to find
 * it cost more than the one destructive row on this screen does. Its value line answers the
 * question a red row provokes before the dialog does.
 *
 */
@Composable
fun SettingsScreen(
    onNavigateToAccount: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToServerAndCommunity: () -> Unit,
    onNavigateToDeveloperTools: () -> Unit,
    onSendFeedback: () -> Unit,
    onNavigateToAbout: () -> Unit,
    isUpdateAvailable: Boolean = false,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isOnline by rememberIsOnline()
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag("settings-screen"),
    ) {
        SettingsCommunityBlock(
            community = SampleConfigSummary.community(state.sdkConfig),
            server = SampleConfigSummary.serverEnvironment(state.sdkConfig),
            status = SampleSdkStatus.of(state.isInitialized, state.connectionState, isOnline),
            modifier =
                Modifier.padding(
                    start = SampleSpacing.gutter,
                    end = SampleSpacing.gutter,
                    top = SampleSpacing.space4,
                    bottom = SampleSpacing.space2,
                ),
        )

        // Account sits alone above the first group header: it is the one row whose value changes
        // what every other screen can do.
        SummaryRow(
            icon = Icons.Default.Person,
            title = "Account",
            subtitle = state.accountSubtitle,
            onClick = onNavigateToAccount,
            modifier = Modifier.testTag("settings-account-row"),
        )

        SummaryGroupHeader("SDK")
        SummaryRow(
            icon = Icons.Default.DarkMode,
            title = "Appearance",
            subtitle = state.appearanceSubtitle,
            onClick = onNavigateToAppearance,
            modifier = Modifier.testTag("settings-appearance-row"),
        )
        HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
        SummaryRow(
            icon = Icons.Default.Language,
            title = "Language",
            subtitle = state.languageSubtitle,
            onClick = onNavigateToLanguage,
            modifier = Modifier.testTag("settings-language-row"),
        )
        HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
        SummaryRow(
            icon = Icons.Default.Cloud,
            // Named for what it does, not for the two of its many fields that used to fit in a
            // label: this row is the way back into the one screen that owns the SDK setup.
            title = "Configuration",
            // What the Config screen owns, as the DS Settings screen lists it — not the current
            // values, which Developer tools › Debug info's "Configuration" card shows.
            subtitle = "Community, server, SSO user, theme",
            onClick = onNavigateToServerAndCommunity,
            // The QA runner keys on this id to walk back into Config — it named the old
            // "Back to Config" row and must survive the restructure.
            modifier = Modifier.testTag("settings-reconfigure-button"),
        )

        SummaryGroupHeader("Support")
        SummaryRow(
            icon = Icons.Default.Terminal,
            title = "Developer tools",
            subtitle = "Host app integration · Events log · Debug info · Debug console",
            onClick = onNavigateToDeveloperTools,
            modifier = Modifier.testTag("settings-devtools-row"),
        )
        LocalExtensions.FeedbackRow(onSendFeedback)
        HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
        SummaryRow(
            icon = Icons.Default.Info,
            title = "About",
            // The Play update card lives behind this row since Home was dropped (DS v1), so an
            // available update is named here first — the Settings tab carries the same badge.
            subtitle =
                (if (isUpdateAvailable) "Update available · " else "") +
                    "Sample ${BuildConfig.VERSION_NAME} · SDK ${BuildConfig.OCTOPUS_SDK_VERSION}" +
                    if (LocalExtensions.designReferenceUrl.isNotBlank()) {
                        " · Design reference"
                    } else {
                        ""
                    },
            onClick = onNavigateToAbout,
            modifier = Modifier.testTag("settings-about-row"),
        )

        // Its own group, red, last on screen. The header is deliberately blank-titled by absence:
        // a destructive action that shares a group with About is a destructive action one mis-tap
        // away from the row above it.
        SummaryGroupHeader("Reset")
        SummaryRow(
            icon = Icons.Default.DeleteForever,
            title = "Reset Configuration",
            // Card 05 settles this subtitle. It answers the question a destructive row provokes
            // — "what am I about to lose?" — before the dialog does, and the answer is the
            // reassuring half: the blast radius stops at this device.
            subtitle = "Local demo setup only — nothing is deleted on the server",
            onClick = { confirmReset = true },
            destructive = true,
            // The QA runner keys on this id — it has survived every restructure of this action.
            modifier = Modifier.testTag("settings-reset-button"),
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text =
                "Sample ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · " +
                    "SDK ${BuildConfig.OCTOPUS_SDK_VERSION}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("settings-version-label"),
        )
        Spacer(Modifier.height(24.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset Configuration?") },
            text = {
                Text(
                    "Clears the sample's saved setup — server, community, SSO user, theme — and " +
                        "restarts on the Config screen. Nothing is deleted on the server.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        viewModel.resetConfig()
                    },
                    // An AlertDialog renders into its own Window / SemanticsOwner, so the flag
                    // hoisted at the Activity's composition root (MainActivity.kt) never reaches
                    // this node — `testTagsAsResourceId` resolves by walking UP the semantics
                    // ancestors, and the walk stops at this window's own root. Without it the id
                    // exists in the Compose tree and is invisible to uiautomator, which is what
                    // the QA runner drives. Same reason as GroupAccessDeniedSheet and
                    // DebugConsole; declared here rather than on a wrapper because an
                    // AlertDialog's slots have no shared content root to hang it on.
                    modifier =
                        Modifier
                            .semantics { testTagsAsResourceId = true }
                            .testTag("settings-reset-confirm"),
                ) {
                    Text("Reset", color = sampleDangerColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            },
        )
    }
}

/**
 * The DS Settings "Community" block: what the SDK is pointed at and whether it works, read-only.
 * It carries no "Change" of its own: the Configuration row below is the one way into Config, which
 * owns every one of these values — two entries side by side to the same screen read as two
 * different settings (QA feedback of 2026-10-02).
 */
@Composable
private fun SettingsCommunityBlock(
    community: String,
    server: String,
    status: SampleSdkStatus,
    modifier: Modifier = Modifier,
) {
    SampleCard(
        modifier = modifier.testTag("settings-community-block"),
        title = "Community",
    ) {
        // "Name", not "Community": the card title already says Community (ISO QA of 2026-10-06).
        SampleInfoRow(label = "Name", value = community)
        SampleInfoRow(label = "Server", value = server)
        // Fades from one state to the next (guest → signed in, online → offline) rather than
        // swapping in a frame, so the change itself is visible.
        Crossfade(targetState = status, animationSpec = SampleMotion.defaultEffects(), label = "sdk-status") { status ->
            Column {
                StatusDot(
                    color = status.dotColor,
                    label = "SDK · ${status.label}",
                    modifier = Modifier.testTag("settings-sdk-status"),
                )
                Text(
                    text = status.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 14.dp, top = 2.dp),
                )
            }
        }
    }
}
