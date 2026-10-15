package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.OctopusSDK
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
 * QA tooling for the nickname choice shown before a first contribution.
 *
 * Applies a DEBUG-only local override of the community `forceNicknameChoice`, so the empty-field
 * (on) and prefilled (off) variants can be exercised whatever the community's back-office setting.
 * The screen only appears for a member whose nickname is not confirmed yet: the read-out below says
 * whether the current one is, and a connected member can switch to a fresh guest to see it again.
 *
 * Test ids mirror the iOS sample and the shared scenario catalog.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ForceNicknameChoiceSection() {
    val scope = rememberCoroutineScope()
    val userDataStore: UserDataStore = koinInject()
    var disconnectError by rememberCommunityDisconnectError()

    // Polled for the same switchCommunity-survival reason as TermsAcceptanceSection.
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
    val effective by produceState<Boolean?>(initialValue = null) {
        while (true) {
            value =
                try {
                    OctopusSDK.communityConfigRepository.getCommunityConfig()?.forceNicknameChoice
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (e: Exception) {
                    null
                }
            delay(1_000)
        }
    }
    val hasOverride = HostOverrides.forceNicknameChoiceOverride != null

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        disconnectError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("forceNicknameChoice-disconnectError"))
        }
        SectionIntro(
            "Debug-only: override the community's forceNicknameChoice locally (applied live — switch " +
                "to the Community tab to see it). A member who has not confirmed a nickname yet gets " +
                "the nickname choice screen at their first post, comment or reply: empty field when on, " +
                "prefilled with the generated nickname when off.",
        )
        Text(
            modifier = Modifier.testTag("forceNicknameChoice-hasConfirmedNickname"),
            text = "Current user hasConfirmedNickname: ${profile?.hasConfirmedNickname?.toString() ?: "…"}",
            style = MaterialTheme.typography.bodySmall,
        )
        if (hasOverride) {
            OverrideActiveBadge(testId = "forceNicknameChoice-override-badge")
        }
        ModeButton(
            label = "On — the member types a nickname",
            selected = HostOverrides.forceNicknameChoiceOverride == true,
            testId = "qa-preset-forceNicknameChoice-1",
        ) { HostOverrides.applyForceNicknameChoiceOverride(true) }
        ModeButton(
            label = "Off — the generated nickname is prefilled",
            selected = HostOverrides.forceNicknameChoiceOverride == false,
            testId = "qa-preset-forceNicknameChoice-2",
        ) { HostOverrides.applyForceNicknameChoiceOverride(false) }
        // A first-class catalog preset: kept in the composition (enabled = hasOverride), see
        // TermsAcceptanceSection.
        ModeButton(
            label = "Clear override (backend default)",
            selected = false,
            enabled = hasOverride,
            testId = "qa-preset-forceNicknameChoice-clear",
        ) { HostOverrides.applyForceNicknameChoiceOverride(null) }
        Text(
            modifier = Modifier.testTag("forceNicknameChoice-result"),
            text =
                "Effective value: ${effective?.let { if (it) "✓ on" else "✗ off" } ?: "…"}" +
                    "  (${if (hasOverride) "local override" else "backend value"})",
            style = MaterialTheme.typography.bodySmall,
        )
        // A confirmed guest cannot be reset in-app (the guest identity is bound to the install id).
        if (profile?.hasConfirmedNickname == true && profile?.isGuest == true) {
            Text(
                modifier = Modifier.testTag("forceNicknameChoice-confirmedGuest"),
                text =
                    "This guest has already confirmed a nickname, so the screen won't show again. " +
                        "Clear the app's data (Android Settings › Apps › Storage) to get a fresh guest.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        // Same reset as TermsAcceptanceSection: only a connected member can go back to a fresh guest.
        if (profile?.hasConfirmedNickname == true && profile?.isGuest == false) {
            Button(
                modifier = Modifier.fillMaxWidth().testTag("forceNicknameChoice-freshGuest"),
                onClick = {
                    val enabled = HostOverrides.forceNicknameChoiceOverride
                    scope.launch {
                        disconnectError = null
                        userDataStore
                            .disconnectFromCommunity()
                            .onSuccess { HostOverrides.applyForceNicknameChoiceOverride(enabled) }
                            .onFailure { disconnectError = DISCONNECT_FAILURE_MESSAGE }
                    }
                },
            ) { Text("Sign out to a guest to see the screen again") }
        }
    }
}
