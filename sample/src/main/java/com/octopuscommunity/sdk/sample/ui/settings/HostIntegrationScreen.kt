package com.octopuscommunity.sdk.sample.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.sample.data.SDKTestFakesLauncher
import com.octopuscommunity.sdk.sample.data.model.HostCallback
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.data.model.effectLine
import com.octopuscommunity.sdk.sample.data.model.label
import com.octopuscommunity.sdk.sample.data.model.tagId
import com.octopuscommunity.sdk.sample.ui.components.PrimaryPillButton
import com.octopuscommunity.sdk.sample.ui.components.SampleCard
import com.octopuscommunity.sdk.sample.ui.components.SampleListDivider
import com.octopuscommunity.sdk.sample.ui.components.SampleListGroup
import com.octopuscommunity.sdk.sample.ui.components.SampleSegmentedChoice
import com.octopuscommunity.sdk.sample.ui.components.SampleSwitchRow
import com.octopuscommunity.sdk.sample.ui.components.SampleTopAppBar
import com.octopuscommunity.sdk.sample.ui.components.SummaryGroupHeader
import com.octopuscommunity.sdk.sample.ui.components.sampleRadioColors
import com.octopuscommunity.sdk.sample.ui.theme.SampleRadius
import com.octopuscommunity.sdk.sample.ui.theme.SampleSpacing
import com.octopuscommunity.sdk.sample.ui.theme.SampleTheme
import com.octopuscommunity.sdk.sample.ui.theme.samplePageColor
import com.octopuscommunity.sdk.sample.ui.theme.samplePageContentColor
import org.koin.androidx.compose.koinViewModel

/**
 * Host app integration: which host callbacks the sample hands the SDK, and its URL policy.
 *
 * These are integration decisions a host app makes once, in code — not something a tester needs
 * before opening the community — so they live behind Settings › Developer tools rather than on the
 * start screen. Each row reads as the public doc names the use case, with the SDK symbol under it.
 *
 * The SDK receives the callbacks when the root navigation graph is built, so a change is stored at
 * once but only reaches the community on the next app start. The screen says so, and **Restart now**
 * recreates the Activity — the same thing as a cold start for the wiring, without leaving the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostIntegrationScreen(
    onBack: () -> Unit,
    viewModel: HostIntegrationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val testFakesRequest by SDKTestFakesLauncher.request.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    // What this visit started from, to confirm a change in place. It is not the live wiring (a change
    // made on an earlier visit and never restarted is not tracked), so Restart stays available.
    val openedWith = remember(state.isConfigured) { state.callbacks }
    val pendingRestart = state.callbacks != openedWith

    Scaffold(
        modifier = Modifier.testTag("host-integration-screen"),
        containerColor = samplePageColor,
        contentColor = samplePageContentColor,
        topBar = {
            SampleTopAppBar(
                title = "Host app integration",
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SampleSpacing.space4, vertical = SampleSpacing.space3),
            verticalArrangement = Arrangement.spacedBy(SampleSpacing.space2),
        ) {
            Text(
                text =
                    if (state.isConfigured) {
                        "What the sample app handles itself when the community asks. Everything is " +
                            "on by default, like a complete integration; switch one off to see what " +
                            "a host that skips it gets."
                    } else if (testFakesRequest != null) {
                        "SDK test fakes are running. Apply a configuration to save host integration " +
                            "settings; the synthetic session stays active."
                    } else {
                        "Start the SDK from the Configuration screen first — there is no " +
                            "integration to change yet."
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.isConfigured) {
                SampleCard(
                    title = "Community navigation",
                    subtitle = "Show tabs or use the whole screen.",
                ) {
                    SampleSegmentedChoice(
                        options = SDKConfig.CommunityNavigation.entries.map { it to it.label },
                        selected = state.communityNavigation,
                        onSelect = viewModel::setCommunityNavigation,
                        modifier = Modifier.testTag("community-navigation-select"),
                    )
                }
            }

            // The DS draws the wired count on the header: "4/5" as the tag, "4 of 5 on" read aloud.
            val wiredCount = HostCallback.entries.count { it.isWired(state.callbacks) }
            val callbackCount = HostCallback.entries.size
            SummaryGroupHeader(
                text = "Callbacks",
                countText = "$wiredCount/$callbackCount",
                countLabel = "$wiredCount of $callbackCount on",
                modifier = Modifier.testTag("host-integration-callbacks-count"),
            )
            SampleListGroup(modifier = Modifier.clip(SampleRadius.mdShape)) {
                HostCallback.entries.forEachIndexed { index, callback ->
                    if (index > 0) SampleListDivider()
                    val wired = callback.isWired(state.callbacks)
                    // A callback the SDK requires is locked on and says why, rather than offer a
                    // switch whose "off" crashes the community.
                    val requiredReason = state.requiredReasons[callback]
                    val scenarioOff = callback == HostCallback.Profile && !state.unifiedProfileEnabled
                    val effect =
                        if (scenarioOff) "Enable the Unified Profile scenario to wire this callback." else callback.effectLine(wired)
                    SampleSwitchRow(
                        title = callback.docName,
                        subtitle = "${callback.apiName}\n${requiredReason ?: effect}",
                        checked = wired,
                        onCheckedChange = { viewModel.setWired(callback, it) },
                        enabled = state.isConfigured && requiredReason == null && !scenarioOff,
                        modifier =
                            Modifier
                                .padding(horizontal = SampleSpacing.space4, vertical = SampleSpacing.space2)
                                .testTag("host-integration-${callback.id}-switch"),
                    )
                }
            }

            SummaryGroupHeader(HostCallback.URL_DOC_NAME)
            Text(
                text = HostCallback.URL_API_NAME,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SampleListGroup(modifier = Modifier.clip(SampleRadius.mdShape)) {
                SDKConfig.UrlHandling.entries.forEachIndexed { index, option ->
                    if (index > 0) SampleListDivider()
                    UrlOptionRow(
                        option = option,
                        selected = state.callbacks.urlHandling == option,
                        enabled = state.isConfigured,
                        onSelect = { viewModel.setUrlHandling(option) },
                    )
                }
            }

            Text(
                text =
                    if (pendingRestart) {
                        "Saved. The community picks up the change on the next app start."
                    } else {
                        "Callback and URL changes are saved at once and apply on the next app start."
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = if (pendingRestart) SampleTheme.colors.text else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = SampleSpacing.space4),
            )
            PrimaryPillButton(
                text = "Restart now",
                onClick = { activity?.recreate() },
                enabled = state.isConfigured && activity != null,
                modifier = Modifier.fillMaxWidth().testTag("host-integration-restart"),
            )
        }
    }
}

@Composable
private fun UrlOptionRow(
    option: SDKConfig.UrlHandling,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
                .padding(horizontal = SampleSpacing.space4, vertical = SampleSpacing.space2)
                .testTag("host-integration-url-${option.tagId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SampleSpacing.space3),
    ) {
        RadioButton(
            selected = selected,
            // The row owns the click, so the radio stays a pure indicator for accessibility.
            onClick = null,
            enabled = enabled,
            colors = sampleRadioColors(),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = option.label,
                style = MaterialTheme.typography.bodyLarge,
                color = SampleTheme.colors.text,
            )
            Text(
                text = option.effectLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
