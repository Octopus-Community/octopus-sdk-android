package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.sample.ui.community.HostOverrides
import com.octopuscommunity.sdk.sample.ui.components.ExpandableSection
import com.octopuscommunity.sdk.sample.ui.components.SampleSwitchRow

/** Dedicated Unified Profile controls, shown in the same scenario layout as its siblings. */
@Composable
fun UnifiedProfileSection(
    enabled: Boolean,
    active: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionIntro(
            "Unified Profile needs both the community's backend flag and a host profile callback. " +
                "Turn it on to force the flag and wire the sample's Client profile screen: " +
                "members with an app id open there, and members without one open Octopus Activity. " +
                "Turn it off to clear the override and remove the callback, restoring native profiles.",
        )
        SampleSwitchRow(
            title = "Unified profile",
            subtitle = unifiedProfileEffectLine(active),
            checked = enabled,
            onCheckedChange = onEnabledChange,
            modifier = Modifier.testTag("config-unified-profile"),
        )
        Text(
            text = "Restarts the SDK; with a demo key, selects the matching auth preset.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("unified-profile-restart-note"),
        )
        ExpandableSection(title = "Backend flag override", nested = true) {
            SectionIntro("Inspect either condition independently. Restarting the SDK restores the scenario switch's flag override.")
            ModeButton(
                label = "Use backend value",
                selected = HostOverrides.exposeClientUserIdOverride == null,
                testId = "expose-override-backend",
            ) { HostOverrides.applyExposeClientUserIdOverride(null) }
            ModeButton(
                label = "Force flag on",
                selected = HostOverrides.exposeClientUserIdOverride == true,
                testId = "expose-override-on",
            ) { HostOverrides.applyExposeClientUserIdOverride(true) }
            ModeButton(
                label = "Force flag off",
                selected = HostOverrides.exposeClientUserIdOverride == false,
                testId = "expose-override-off",
            ) { HostOverrides.applyExposeClientUserIdOverride(false) }
        }
    }
}

/** The switch stores a scenario choice; its subtitle describes the current effective behavior. */
internal fun unifiedProfileEffectLine(active: Boolean): String =
    if (active) "Profile taps open Client profile" else "Profile taps open native profiles"
