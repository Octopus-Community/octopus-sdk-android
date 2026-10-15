package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.sample.ui.components.ApiMethodCard
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenariosViewModel

/**
 * The `communityData` QA scenario — the Unified Profile read surface a host uses to render a
 * member's public Octopus stats on its **own** profile screen, instead of pushing the user into the
 * SDK's native profile.
 *
 * Covers the whole read API with single-tap presets:
 *  - [com.octopuscommunity.sdk.OctopusSDK.fetchCommunityDataByClientUserIdResult] — the id kind a host has,
 *  - [com.octopuscommunity.sdk.OctopusSDK.fetchCommunityDataResult] — replayed with the Octopus id the
 *    first lookup returned, so both paths are covered without pasting an opaque id,
 *  - [com.octopuscommunity.sdk.OctopusSDK.communityDataFlowByClientUserId] — the reactive
 *    counterpart; start/stop presets make the observation lifecycle observable,
 *  - the exactly-one-id contract (compile-time on Android — see
 *    [ScenariosViewModel.reportCommunityDataIdContract]),
 *  - the host-rendered profile screen itself (`ClientProfileScreen`).
 *
 * Test ids `qa-preset-communityData-1..6` and `communityData-result` are the cross-platform contract
 * from the shared scenario catalog, applied verbatim so the QA pipeline drives Android,
 * iOS and Flutter with the same ids. `communityData-liveState` is an Android addition with no
 * catalog slot yet (the catalog has no live-state field) — treat it as convenience, not contract.
 * `testTagsAsResourceId` surfaces them all as resource-ids in the uiautomator / `android layout`
 * tree.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CommunityDataSection(
    scenario: ScenariosViewModel.CommunityDataScenarioState,
    lookupClientUserId: String?,
    profileClientUserId: String?,
    userIdInput: String,
    rawFetchState: ScenariosViewModel.MethodState,
    onUserIdInputChanged: (String) -> Unit,
    onRawFetch: () -> Unit,
    onFetchByClientUserId: () -> Unit,
    onFetchByUserId: () -> Unit,
    onStartObserving: () -> Unit,
    onStopObserving: () -> Unit,
    onCheckIdContract: () -> Unit,
    onOpenClientProfile: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionIntro(
            "Reads a member's public Octopus stats with fetchCommunityDataResult / " +
                "fetchCommunityDataByClientUserIdResult (one shot) and their Flow counterparts " +
                "(reactive), by Octopus userId or by your own clientUserId. gamification.score is " +
                "always null through this API. Read gamification.level from the result panel below " +
                "(presets 1 and 2, straight from the network) or from preset 6's profile — never " +
                "from the live-state panel, which can be showing a store row of any age (see " +
                "LiveState).",
        )

        // Every preset stays in the tree unconditionally — the catalog lists Android as a platform
        // that HAS all six, and a contract id missing from the accessibility tree reads as a
        // failure. The async ones are gated only while a lookup is in flight, so a Tester tapping
        // in sequence can't interleave two coroutines and read whichever finished last.
        Preset(
            label = "Preset 1 · Fetch by clientUserId",
            testId = "qa-preset-communityData-1",
            enabled = !scenario.isLoading,
            onClick = onFetchByClientUserId,
        )
        Preset(
            label = "Preset 2 · Fetch by profileId (from the last lookup)",
            testId = "qa-preset-communityData-2",
            enabled = !scenario.isLoading,
            onClick = onFetchByUserId,
        )
        Preset(
            label = "Preset 3 · Observe by clientUserId (start)",
            testId = "qa-preset-communityData-3",
            enabled = !scenario.isLoading,
            onClick = onStartObserving,
        )
        // Not gated: tearing an observation down must work even mid-fetch.
        Preset(
            label = "Preset 4 · Stop observing",
            testId = "qa-preset-communityData-4",
            onClick = onStopObserving,
        )
        Preset(
            label = "Preset 5 · Contract: both / neither id throws",
            testId = "qa-preset-communityData-5",
            onClick = onCheckIdContract,
        )
        Preset(
            label = "Preset 6 · Open the host-rendered profile page",
            testId = "qa-preset-communityData-6",
            onClick = onOpenClientProfile,
        )

        ResultPanel(scenario)
        LiveState(
            scenario = scenario,
            lookupClientUserId = lookupClientUserId,
            profileClientUserId = profileClientUserId,
        )

        HorizontalDivider()

        // Raw one-shot lookup on an arbitrary Octopus user id — carried over from the former API Explorer,
        // which the presets deliberately don't cover (they never ask for a hand-typed id).
        Text(
            text = "fetchCommunityDataResult(userId) — any id",
            style = MaterialTheme.typography.labelLarge,
        )
        OutlinedTextField(
            value = userIdInput,
            onValueChange = onUserIdInputChanged,
            label = { Text("User ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        ApiMethodCard(
            name = "fetchCommunityDataResult(userId)",
            description = "Fetch a user's message count + gamification (read-only).",
            isLoading = rawFetchState.isLoading,
            onExecute = onRawFetch,
        )
    }
}

@Composable
private fun Preset(
    label: String,
    testId: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    SecondaryPillButton(
        modifier = Modifier.fillMaxWidth().testTag(testId),
        enabled = enabled,
        onClick = onClick,
    ) {
        Text(label)
    }
}

@Composable
private fun ResultPanel(scenario: ScenariosViewModel.CommunityDataScenarioState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.testTag("communityData-result").padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Result",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            // `result` is always rendered, never replaced by the in-flight state: presets 4/5/6 are
            // un-gated precisely so they work during a fetch, and swapping the panel for "loading…"
            // would swallow the very confirmation they exist to give. The in-flight state is an
            // extra line instead, so no write is ever invisible.
            Text(
                text = scenario.result ?: "none yet — tap a preset.",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color =
                    if (scenario.isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            if (scenario.isLoading) {
                Text(
                    text = "a lookup is in flight…",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Everything the scenario currently knows, so a tester can read state without tapping.
 *
 * Deliberately shows **no gamification row**, and that omission is the point.
 *
 * The `last value from` row above names the call that produced the value, but not how old that value
 * is — and age is what matters here. `communityDataFlowByClientUserId` resolves the member over the
 * network once, then emits from Room (`profileDao.getPublicProfile`), so every later emission
 * carries whatever last wrote that row. The panel then keeps the value indefinitely:
 * `ScenariosViewModel.stopCollecting` clears the observation flags but not `data`, so a value stays
 * on screen after the collector that produced it is dead. A gamification level read
 * here is therefore not evidence of what the backend serves now, and a QA step asserting on it can
 * PASS on a store row. The cost is already paid, if not provably through this panel: a
 * `gamification.level=2` was reported as evidence in the test plan of the PR that added this
 * scenario — which does not say what surface it was read off — while a later read-only
 * `GetPublicProfile` probe of the same member, with `fetchGamification` set, came back with no
 * gamification block at all. The two reads differ by more than timing: the probe ran at guest level
 * (the client-user-id lookup resolves without a user JWT), while the sample read from a connected
 * SSO session. Which of them reflects what a host actually sees was never established. A value that
 * cannot be traced back to a single read is not an assertion target.
 *
 * `score` is worse than ambiguous: it can never be non-null here. The public read path maps
 * `PublicProfile` onto `OtherUserProfile`, which does not override `Profile.gamificationScore`
 * (default `null`), and the proto's `PublicGamificationScore.score` is a BO-only field the mapper
 * never reads. The row could only ever print "null".
 *
 * Where gamification stays observable: presets 1 and 2 print the level into `communityData-result`
 * (`ScenariosViewModel.describeCommunityData`) from the gRPC response itself — `fetchProfile*` maps
 * the response and only upserts as a side effect — and preset 6's host-rendered profile is
 * `fetch`-backed too. Residual gap, accepted: the flow path (presets 3/4) now has no gamification
 * surface at all, since its result panel only ever carries lifecycle text. On a community with
 * gamification on that is real signal lost, and it is worth less than a row no tester can trust.
 *
 * `userId` and `messageCount` come off the same object and carry the same age caveat, but they stay:
 * both are observable on every community, and any preset re-reads them from the network, so a wrong
 * value is one tap from being corrected instead of being permanently unfalsifiable.
 */
@Composable
private fun LiveState(
    scenario: ScenariosViewModel.CommunityDataScenarioState,
    lookupClientUserId: String?,
    profileClientUserId: String?,
) {
    val data = scenario.data
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.testTag("communityData-liveState").padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Live state",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Line("profile.clientUserId", profileClientUserId ?: "—")
            Line("lookup clientUserId", lookupClientUserId ?: "—")
            // "no" alone would hide the case that matters: the flow ended by itself because the
            // lookup resolved no member. Naming the reason is what stops a dead collector from
            // reading like a live one.
            Line(
                "observing",
                when {
                    scenario.observing -> "yes"
                    scenario.observationEndedReason != null ->
                        "no — ${scenario.observationEndedReason}"
                    else -> "no"
                },
            )
            Line("last value from", scenario.source)
            // Android names the Octopus id `userId`; the catalog label keeps Flutter's `profileId`.
            Line("userId", data?.userId ?: "—")
            Line("messageCount", data?.messageCount?.toString() ?: "null")
            // Rare by design: the SDK turns a failed clientUserId lookup into a `null` emission and
            // completes, so that case shows up on the `observing` row above, not here. This row only
            // fires on a genuine throw from the flow (e.g. the local store failing mid-collect).
            scenario.flowError?.let { Line("flow error", it) }
        }
    }
}

@Composable
private fun Line(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(0.45f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}
