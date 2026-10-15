package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.CommunityConfig.TermsAcceptanceMode
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.sample.data.datastore.UserDataStore
import com.octopuscommunity.sdk.sample.data.usecase.DISCONNECT_FAILURE_MESSAGE
import com.octopuscommunity.sdk.sample.data.usecase.disconnectFromCommunity
import com.octopuscommunity.sdk.sample.ui.community.HostOverrides
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * QA tooling for the explicit terms-acceptance (consent) modes.
 *
 * Applies a DEBUG-only local override of the community [TermsAcceptanceMode] — without a
 * backend-driven config — so the consent sheet can be exercised in the sample independently of which
 * community is seeded and before the backend serves the mode. The override is applied live: pick a
 * mode here, then switch to the Community tab and make a first contribution (post / comment / reply)
 * with a user that has not yet accepted the CGU to see the consent sheet.
 *
 * Test ids (`qa-preset-termsAcceptance-N` / `termsAcceptance-result`) mirror the sibling
 * [ContentOptionsSection] and the shared scenario catalog, so the QA pipeline drives both
 * platforms with the same ids.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TermsAcceptanceSection() {
    val scope = rememberCoroutineScope()
    val userDataStore: UserDataStore = koinInject()
    var disconnectError by rememberCommunityDisconnectError()

    // Poll the CURRENT user profile every second rather than collecting the flow once:
    // switchCommunity rebuilds the Koin container (fresh profileRepository + DB), so a one-shot
    // getProfileFlow() collector stays bound to the old, cleared DB and freezes. Re-reading
    // through the suspend getter resolves profileRepository fresh on every tick. Both
    // hasAcceptedCgu and isGuest come from this one read, so they always agree.
    val profile by produceState<CurrentUserProfile?>(initialValue = null) {
        while (true) {
            value =
                try {
                    OctopusSDK.profileRepository.getProfile()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (e: Exception) {
                    null
                }
            delay(1_000)
        }
    }
    val hasAcceptedCgu = profile?.hasAcceptedCgu
    val isGuest = profile?.isGuest == true

    // Effective mode = the backend value with any local override already applied, read straight from
    // the SDK config (same source as the "Live community config" panel). Polled for the same
    // switchCommunity-survival reason as above. This is what actually drives the consent sheet — so
    // the read-out below always names a real mode, never "none".
    val effectiveMode by produceState<TermsAcceptanceMode?>(initialValue = null) {
        while (true) {
            value =
                try {
                    OctopusSDK.communityConfigRepository.getCommunityConfig()?.termsAcceptanceMode
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (e: Exception) {
                    null
                }
            delay(1_000)
        }
    }

    val switchToFreshGuest: () -> Unit = {
        // Capture the current override before disconnecting, then re-apply it. disconnectUser()
        // returns a CONNECTED user (SSO) to a fresh guest (hasAcceptedCgu = false); it
        // does not rebuild Koin and does not clear the override, so re-applying it afterwards is
        // race-free and idempotent. This is ONLY wired for a connected non-guest user (see the button
        // below): for a guest, logout() is a no-op — the guest identity is bound to the install id,
        // so there is no in-app reset (only clearing the app's data yields a fresh, unaccepted guest).
        val mode = HostOverrides.termsAcceptanceModeOverride
        scope.launch {
            disconnectError = null
            userDataStore
                .disconnectFromCommunity()
                .onSuccess { HostOverrides.applyTermsAcceptanceModeOverride(mode) }
                .onFailure { disconnectError = DISCONNECT_FAILURE_MESSAGE }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        disconnectError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("termsAcceptance-disconnectError"))
        }
        SectionIntro(
            "Debug-only: override the community terms-acceptance mode locally (applied live — " +
                "switch to the Community tab to see it). In an explicit mode a consent sheet is " +
                "shown at the first contribution, for a user who has not accepted the CGU yet.",
        )

        Text(
            modifier = Modifier.testTag("termsAcceptance-hasAcceptedCgu"),
            text = "Current user hasAcceptedCgu: ${hasAcceptedCgu?.toString() ?: "…"}",
            style = MaterialTheme.typography.bodySmall,
        )

        // Override-UX pass: a badge makes the override visible even after navigating away and
        // back, and "Clear override" (qa-preset-termsAcceptance-clear, a first-class catalog
        // preset) stays present but disabled while there is nothing to clear — never removed, so
        // its id always resolves at runtime.
        val hasOverride = HostOverrides.termsAcceptanceModeOverride != null
        if (hasOverride) {
            OverrideActiveBadge(testId = "termsAcceptance-override-badge")
        }

        ModeButton(
            label = "Implicit (default / no-op)",
            selected = HostOverrides.termsAcceptanceModeOverride == TermsAcceptanceMode.IMPLICIT,
            testId = "qa-preset-termsAcceptance-1",
        ) { HostOverrides.applyTermsAcceptanceModeOverride(TermsAcceptanceMode.IMPLICIT) }
        ModeButton(
            label = "Explicit — multi checkbox",
            selected =
                HostOverrides.termsAcceptanceModeOverride ==
                    TermsAcceptanceMode.EXPLICIT_MULTI_CHECKBOX,
            testId = "qa-preset-termsAcceptance-2",
        ) { HostOverrides.applyTermsAcceptanceModeOverride(TermsAcceptanceMode.EXPLICIT_MULTI_CHECKBOX) }
        ModeButton(
            label = "Explicit — single checkbox",
            selected =
                HostOverrides.termsAcceptanceModeOverride ==
                    TermsAcceptanceMode.EXPLICIT_SINGLE_CHECKBOX,
            testId = "qa-preset-termsAcceptance-3",
        ) { HostOverrides.applyTermsAcceptanceModeOverride(TermsAcceptanceMode.EXPLICIT_SINGLE_CHECKBOX) }
        // qa-preset-termsAcceptance-clear is a first-class catalog preset, not sample-added UX
        // sugar — it must stay in the composition (enabled = hasOverride) rather than being removed
        // when there is nothing to clear, or a QA teardown / up-front resolve of this id from a
        // clean state finds no node.
        ModeButton(
            label = "Clear override (backend default)",
            selected = false,
            enabled = hasOverride,
            testId = "qa-preset-termsAcceptance-clear",
        ) { HostOverrides.applyTermsAcceptanceModeOverride(null) }

        // Effective mode, resolved by the SDK (override applied over the backend value). Always a
        // real mode + its source — the local override or the backend — never "none".
        Text(
            modifier = Modifier.testTag("termsAcceptance-result"),
            text =
                "Effective mode: ${effectiveMode?.let(::termsLabel) ?: "…"}" +
                    "  (${if (hasOverride) "local override" else "backend value"})",
            style = MaterialTheme.typography.bodySmall,
        )

        // Persistent warning while the current user has already accepted: switching the mode won't
        // re-prompt them (consent is per-user). To re-test the consent sheet a fresh, unaccepted
        // user is needed. Warning-styled so it stands out.
        //   - SSO user: an explicit "fresh guest" button actually works (logout deletes the user and
        //     reconnects a fresh guest with hasAcceptedCgu = false).
        //   - Guest: no in-app reset exists (the acceptance is bound to the install id), so no button
        //     is offered — only an honest note pointing at the real fix (clear the app's data).
        if (hasAcceptedCgu == true) {
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("termsAcceptance-alreadyAccepted"),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            text =
                                "This user has already accepted the CGU — changing the mode won't " +
                                    "re-prompt them. Use a fresh, unaccepted user to re-test the " +
                                    "consent sheet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    if (isGuest) {
                        Text(
                            text =
                                "You are a guest: an accepted guest can't be reset in-app (the " +
                                    "acceptance is bound to this install). Clear the app's data " +
                                    "(Android Settings › Apps › Storage) to get a fresh, unaccepted " +
                                    "guest.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    } else {
                        Button(
                            modifier =
                                Modifier.fillMaxWidth().testTag("termsAcceptance-freshGuest"),
                            onClick = switchToFreshGuest,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                        ) {
                            Text("Disconnect user → fresh guest")
                        }
                    }
                }
            }
        }
    }
}

private fun termsLabel(mode: TermsAcceptanceMode): String =
    when (mode) {
        TermsAcceptanceMode.IMPLICIT -> "implicit"
        TermsAcceptanceMode.EXPLICIT_MULTI_CHECKBOX -> "explicit · multi checkbox"
        TermsAcceptanceMode.EXPLICIT_SINGLE_CHECKBOX -> "explicit · single checkbox"
    }
