package com.octopuscommunity.sdk.sample.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.sample.BuildConfig
import com.octopuscommunity.sdk.sample.data.SDKConfigResolver
import com.octopuscommunity.sdk.sample.data.SampleConfigSummary
import com.octopuscommunity.sdk.sample.data.SampleSdkStatus
import com.octopuscommunity.sdk.sample.data.model.FeatureToggle
import com.octopuscommunity.sdk.sample.data.model.FeatureToggles
import com.octopuscommunity.sdk.sample.data.model.HostCallback
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.data.model.label
import com.octopuscommunity.sdk.sample.ui.components.CurrentConfigurationBlock
import com.octopuscommunity.sdk.sample.ui.components.SampleCard
import com.octopuscommunity.sdk.sample.ui.components.SampleInfoRow
import com.octopuscommunity.sdk.sample.ui.components.dotColor
import com.octopuscommunity.sdk.sample.ui.components.rememberIsOnline
import com.octopuscommunity.sdk.sample.ui.components.sampleDangerColor
import com.octopuscommunity.sdk.sample.ui.components.sampleSuccessColor
import org.koin.androidx.compose.koinViewModel

/**
 * Debug info: the read-only panels that used to be stacked at the bottom of Settings, plus what the
 * dropped Home and API Explorer tabs used to show (DS v1): the connection detail, the unread count,
 * the community-access answer and the full 8-line configuration.
 *
 * Read-only on purpose: Host app integration and the Unified Profile scenario own the live
 * callback switches. The Configuration card has no "Change" action; Settings › Configuration
 * is the entry point for those settings.
 */
@Composable
fun DebugInfoScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isOnline by rememberIsOnline()
    val status = SampleSdkStatus.of(state.isInitialized, state.connectionState, isOnline)
    val config = state.sdkConfig
    val connection = debugConnectionInfo(state.connectionState)
    val context = LocalContext.current

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("debug-info-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SampleCard(title = "SDK") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SampleInfoRow("SDK version", BuildConfig.OCTOPUS_SDK_VERSION)
                // The same verdict as the Settings Community block, so a bug report pasted from
                // here and a screenshot of Settings never disagree on the state the SDK was in.
                SampleInfoRow(
                    "Status",
                    status.label,
                    valueColor = status.dotColor,
                    modifier = Modifier.testTag("debug-info-sdk-status"),
                )
                SampleInfoRow("Network", if (isOnline) "Online" else "Offline")
                SampleInfoRow("Initialized", state.isInitialized.toString())
                SampleInfoRow("Auth mode", config?.authKind?.shortLabel ?: SampleConfigSummary.UNKNOWN)
                SampleInfoRow(
                    "Connection",
                    connection.label,
                    modifier =
                        Modifier.testTag("debug-info-connection").then(
                            connection.userId?.let { userId ->
                                Modifier
                                    .sizeIn(minHeight = 48.dp)
                                    .clickable(onClickLabel = "Copy user ID", role = Role.Button) {
                                        copyDebugUserId(context, userId)
                                    }.padding(vertical = 8.dp)
                            } ?: Modifier,
                        ),
                )
                SampleInfoRow("Unread notifications", state.notSeenNotifications.toString())
                SampleInfoRow("Community access", if (state.hasAccessToCommunity) "Granted" else "Denied")
            }
        }

        SampleCard(
            title = "Feature toggles",
            subtitle = "Set on Scenarios. A bug report that omits these is a bug report twice.",
        ) {
            val toggles = config?.featureToggles ?: FeatureToggles.Default
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FeatureToggle.entries.forEach { toggle ->
                    SampleInfoRow(
                        label = toggle.label,
                        value = toggle.readOnlyValueLine(toggles[toggle]),
                    )
                }
            }
        }

        CurrentConfigurationBlock(
            lines =
                state.configurationLines +
                    ("API key" to SDKConfigResolver.maskApiKey(state.effectiveApiKey)),
            modifier = Modifier.testTag("debug-info-configuration"),
        )

        SampleCard(
            title = "Host app integration",
            subtitle = "What the app handles itself — set in Developer tools › Host app integration.",
        ) {
            val hc = config?.effectiveHostCallbacks ?: SDKConfig.HostCallbacks()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HostCallback.entries.forEach { callback -> WiringRow(callback.docName, callback.isWired(hc)) }
                SampleInfoRow(HostCallback.URL_DOC_NAME, hc.urlHandling.label)
            }
        }

        SampleCard(title = "Build") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SampleInfoRow("Sample version", BuildConfig.VERSION_NAME)
                SampleInfoRow("Build", BuildConfig.VERSION_CODE.toString())
                SampleInfoRow("Flavor", if (BuildConfig.IS_PROD_FLAVOR) "prod" else "demo")
            }
        }
    }
}

@Composable
private fun WiringRow(
    label: String,
    wired: Boolean,
) {
    SampleInfoRow(
        label = label,
        value = if (wired) "Wired" else "Not wired",
        valueColor = if (wired) sampleSuccessColor else sampleDangerColor,
    )
}
