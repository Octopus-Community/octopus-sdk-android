package com.octopuscommunity.sdk.sample.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.ui.components.SampleCard
import com.octopuscommunity.sdk.sample.ui.components.SampleSegmentedChoice
import org.koin.androidx.compose.koinViewModel

/**
 * Appearance: the light/dark choice and the theme preset handed to the SDK.
 *
 * The same two controls the Config screen carries in its Theme section — one persisted value,
 * two places to change it, no second source of truth.
 */
@Composable
fun AppearanceScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config = state.sdkConfig

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("appearance-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SampleCard(
            title = "Appearance",
            subtitle = "Applies to the sample and to the SDK surface it hosts.",
        ) {
            SampleSegmentedChoice(
                options =
                    listOf(
                        SDKConfig.Appearance.System to "System",
                        SDKConfig.Appearance.Light to "Light",
                        SDKConfig.Appearance.Dark to "Dark",
                    ),
                selected = config?.appearance ?: SDKConfig.Appearance.System,
                onSelect = viewModel::setAppearance,
                modifier = Modifier.testTag("appearance-mode-select"),
            )
        }

        SampleCard(
            title = "Theme preset",
            subtitle = "\"SDK default\" hands the SDK no theme — what a fresh integration looks like.",
        ) {
            SampleSegmentedChoice(
                options =
                    listOf(
                        true to "Octopus navy",
                        false to "SDK default",
                    ),
                selected = config?.theme !is SDKConfig.Theme.SdkDefault,
                onSelect = viewModel::setThemePreset,
                modifier = Modifier.testTag("appearance-preset-select"),
            )
        }

        Text(
            text = "Changing the preset re-themes the community tab immediately — no restart.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
