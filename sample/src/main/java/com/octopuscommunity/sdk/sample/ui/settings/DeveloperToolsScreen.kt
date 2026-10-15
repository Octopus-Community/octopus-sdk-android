package com.octopuscommunity.sdk.sample.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.sample.BuildConfig
import com.octopuscommunity.sdk.sample.data.model.FeatureToggles
import com.octopuscommunity.sdk.sample.ui.components.SummaryRow

/**
 * Developer tools: the one place for what a developer needs and a tester can ignore.
 *
 * First row, Host app integration — the host callbacks and the URL policy, which used to sit on
 * the start Configuration screen in front of everyone who only wanted to open the community. It is
 * the single place those are set (Debug info reads them).
 * The other rows are read-only: Events log, Debug info and the debug console. Theme and language
 * are not set here, so the footer no longer points at them (the DS hint is the first sentence only).
 *
 * The debug console is the last row and opens as a sheet rather than as a destination — the app
 * bar's bug button opens the same sheet from anywhere. Its subtitle is the DS one: what the log
 * records, down to each feature toggle rebuilding the SDK config. Listing it
 * costs one line and answers the question the screen exists for: where do I read what the SDK did?
 */
@Composable
fun DeveloperToolsScreen(
    eventCount: Int,
    onNavigateToHostIntegration: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToDebugInfo: () -> Unit,
    onOpenDebugConsole: () -> Unit,
    featureToggles: FeatureToggles = FeatureToggles.Default,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag("developer-tools-screen"),
    ) {
        SummaryRow(
            icon = Icons.Default.Tune,
            title = "Host app integration",
            subtitle = "Profile, sign-in, links: what the app handles",
            onClick = onNavigateToHostIntegration,
            modifier = Modifier.testTag("devtools-host-integration-row"),
        )
        HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
        SummaryRow(
            icon = Icons.Default.Notifications,
            title = "Events log",
            subtitle = "$eventCount events · live",
            onClick = onNavigateToEvents,
            modifier = Modifier.testTag("devtools-events-row"),
        )
        HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
        SummaryRow(
            icon = Icons.Default.Info,
            title = "Debug info",
            // The toggles ride in the subtitle because this row is where someone lands when a
            // build behaves unexpectedly, and "which switches were on?" is the first question.
            subtitle =
                "SDK ${BuildConfig.OCTOPUS_SDK_VERSION} · build ${BuildConfig.VERSION_CODE} · " +
                    featureToggles.summaryLine,
            onClick = onNavigateToDebugInfo,
            modifier = Modifier.testTag("devtools-debug-info-row"),
        )
        HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
        SummaryRow(
            icon = Icons.Default.BugReport,
            title = "Debug console",
            subtitle = "Logs every feature toggle → SDK config rebuilt",
            onClick = onOpenDebugConsole,
            modifier = Modifier.testTag("devtools-debug-console-row"),
        )
        Text(
            text =
                "Host app integration sets what the app handles; the other three read what " +
                    "the SDK did.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
        )
    }
}
