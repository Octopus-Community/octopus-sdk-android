package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.ProfileFieldLockState
import com.octopuscommunity.sdk.sample.data.SDKConfigResolver
import com.octopuscommunity.sdk.sample.ui.components.ResultDisplay
import com.octopuscommunity.sdk.sample.ui.components.sampleBorderColor
import com.octopuscommunity.sdk.sample.ui.components.sampleTintedBackground

/**
 * Live read-out of what the SDK currently sees for the targeted community.
 *
 * Reads [OctopusSDK.communityConfigRepository], which is the backend-fetched [CommunityConfig] with
 * any local debug override applied on top — so it answers "what's actually enabled on this
 * community?" without guessing, and updates reactively after a `switchCommunity(...)`. Pairs with
 * the debug-override sub-sections below: tap an override and watch the matching line flip here.
 *
 * The four states (not initialised / fetching / loaded / failed) and the polling rationale live in
 * [communityConfigPanelStates]; this composable only renders them. Retry re-keys [produceState],
 * which restarts the cold flow — i.e. re-attempts the fetch the state machine parked after the
 * failure.
 */
@Composable
fun CommunityConfigPreview(apiKey: String) {
    var retryTick by remember { mutableIntStateOf(0) }
    val state by produceState<CommunityConfigPanelState>(
        initialValue = CommunityConfigPanelState.Loading,
        retryTick,
    ) {
        communityConfigPanelStates(OctopusCommunityConfigPanelSource).collect { value = it }
    }

    // Tinted sample-layer block, not `surfaceVariant`: this is sample chrome around SDK state, and
    // the design contract gives every such block the same tinted ground and 1dp border. The grey
    // surfaceVariant card it used to be was the one panel on the Scenarios tab that did not match.
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = sampleTintedBackground,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, sampleBorderColor),
    ) {
        Column(
            modifier =
                Modifier
                    .testTag("community-config-preview")
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Live community config",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text =
                    "What the SDK sees for the current community (backend config + any local " +
                        "override applied below). Updates after switchCommunity.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Line("API key", if (apiKey.isBlank()) "—" else SDKConfigResolver.maskApiKey(apiKey))

            when (val current = state) {
                CommunityConfigPanelState.NotInitialised ->
                    Line("Status", "SDK not initialised")

                CommunityConfigPanelState.Loading ->
                    Line("Status", "loading…")

                is CommunityConfigPanelState.Failed ->
                    Column(
                        modifier = Modifier.testTag("community-config-preview-error"),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Same error grammar as the other scenario sections: the failure verbatim,
                        // in the error-tinted block, plus the Retry affordance the Config screen
                        // uses for its own failed load.
                        ResultDisplay(result = current.message, isError = true)
                        TextButton(onClick = { retryTick++ }) { Text("Retry") }
                    }

                is CommunityConfigPanelState.Loaded -> ConfigLines(current.config)
            }
        }
    }
}

@Composable
private fun ConfigLines(config: CommunityConfig) {
    Line(
        "Post",
        "pictures ${tick(config.contentOptions.post.enablePictures)} · " +
            "polls ${tick(config.contentOptions.post.enablePolls)} · " +
            "text required ${tick(config.contentOptions.post.textRequired)}",
    )
    Line("Comment", "pictures ${tick(config.contentOptions.comment.enablePictures)}")
    Line("Reply", "pictures ${tick(config.contentOptions.reply.enablePictures)}")
    Line(
        "Profile lock",
        "nickname ${lock(config.profileFieldsLock.nickname)} · " +
            "avatar ${lock(config.profileFieldsLock.avatar)} · " +
            "bio ${lock(config.profileFieldsLock.bio)}",
    )
    Line("Unified Profile (exposeClientUserId)", tick(config.exposeClientUserId))
    Line("Terms acceptance", terms(config.termsAcceptanceMode))
    Line("Force nickname choice", tick(config.forceNicknameChoice))
    Line("Gamification", if (config.gamification != null) "on" else "off")
    Line("Force login (strong actions)", tick(config.forceLoginOnStrongActions))
    Line("Display account age", tick(config.displayAccountAge))
    Line("Powered by Octopus", poweredBy(config.poweredByOctopus))
}

private fun tick(enabled: Boolean): String = if (enabled) "✓ on" else "✗ off"

private fun lock(state: ProfileFieldLockState): String =
    when (state) {
        ProfileFieldLockState.EDITABLE -> "editable"
        ProfileFieldLockState.READ_ONLY -> "read-only"
        ProfileFieldLockState.DISABLED -> "hidden"
    }

private fun poweredBy(value: CommunityConfig.PoweredByOctopus): String =
    when (value) {
        is CommunityConfig.PoweredByOctopus.Default -> "default badge"
        is CommunityConfig.PoweredByOctopus.Custom -> "custom logo"
        is CommunityConfig.PoweredByOctopus.Hidden -> "hidden"
    }

private fun terms(mode: CommunityConfig.TermsAcceptanceMode): String =
    when (mode) {
        CommunityConfig.TermsAcceptanceMode.IMPLICIT -> "implicit (legal footer)"
        CommunityConfig.TermsAcceptanceMode.EXPLICIT_MULTI_CHECKBOX -> "explicit · multi checkbox"
        CommunityConfig.TermsAcceptanceMode.EXPLICIT_SINGLE_CHECKBOX -> "explicit · single checkbox"
    }

@Composable
private fun Line(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
