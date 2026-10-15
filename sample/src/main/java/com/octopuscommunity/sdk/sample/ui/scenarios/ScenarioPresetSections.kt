package com.octopuscommunity.sdk.sample.ui.scenarios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.OctopusEvent
import com.octopuscommunity.sdk.model.OctopusNotification
import com.octopuscommunity.sdk.sample.data.SampleFixtures
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.ui.community.HostOverrides
import com.octopuscommunity.sdk.sample.ui.components.LiveFlowDisplay
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton
import com.octopuscommunity.sdk.sample.ui.components.sampleDefaultSwitchColors
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.ModeButton
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.OverrideActiveBadge
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.SectionIntro
import com.octopuscommunity.sdk.sample.ui.settings.EditUserViewModel
import org.koin.androidx.compose.koinViewModel

/*
 * New scenario preset screens for Lot 2 (Explorer -> Scenarios), built directly against catalog
 * preset ids that had no UI before this refactor: communityAccess, customEvents, locale,
 * refreshEntitlements.
 *
 * These follow the same look already established by the pre-existing preset screens
 * (com.octopuscommunity.sdk.sample.ui.scenarios.sections.ProfileLockSection,
 * ContentOptionsSection, TermsAcceptanceSection): one full-width SecondaryPillButton per catalog
 * preset, labeled with the preset's own name, plus a single result line carrying the scenario's
 * result_test_id. connection, groups, syncFollowGroups, lifecycle and
 * notSeenNotifications reuse their existing section composables instead (see
 * ScenarioDetailScreen) — only the genuinely new presets get new composables here.
 */

/** Shared result line: one `Text` node carrying the scenario's single `result_test_id`. */
@Composable
private fun ScenarioResult(
    text: String?,
    isError: Boolean,
    resultTestTag: String,
) {
    // Nothing to report until a preset has run: a lone "—" read as a glitch, not as "idle". The
    // node carrying the result id appears with the first result, which is when QA reads it.
    if (text == null) return
    Text(
        modifier = Modifier.fillMaxWidth().testTag(resultTestTag),
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color =
            if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
    )
}

/**
 * `communityAccess` scenario: overrideCommunityAccess(hasAccess) (enforced by the SDK) and
 * trackAccessToCommunity(hasAccess) (analytics-only) — both already existed as a Switch + 2
 * generic buttons in the legacy accordion; here each combination gets its own catalog-labeled
 * preset button instead.
 */
@Composable
fun CommunityAccessPresets(
    hasAccessToCommunity: Boolean,
    onOverrideGrant: () -> Unit,
    onOverrideDeny: () -> Unit,
    onTrackHasAccess: () -> Unit,
) {
    // Override-UX pass: HostOverrides.communityAccessOverride is tracking-only here (the actual
    // suspend overrideCommunityAccess() call and its result already go through ScenariosViewModel /
    // MethodState below) — it exists purely so the Grant/Deny buttons can render selected and the
    // badge can appear. overrideCommunityAccess has no reset call in the public API (unlike the
    // debugOverride* hooks), so — unlike the other override scenarios — there is deliberately no
    // "clear" action here: re-tapping the other button is the only reversal available.
    //
    // The tracking call itself lives in ScenariosViewModel.executeOverrideCommunityAccess()'s
    // success branch, not here in onClick: setting it eagerly, before the suspend SDK call
    // resolves, used to show "Override active · granted" and a selected button while the result
    // line simultaneously showed Error — badge and outcome could disagree on a failed call.
    val communityOverride = HostOverrides.communityAccessOverride

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionIntro(
            "Community-access signals your app supplies to the SDK. overrideCommunityAccess is " +
                "enforced by the SDK; trackAccessToCommunity is analytics-only. There is no reset " +
                "call for the override — re-apply the other value to reverse it.",
        )

        if (communityOverride != null) {
            OverrideActiveBadge(
                testId = "communityAccess-override-badge",
                label = if (communityOverride) "Override active · granted" else "Override active · denied",
            )
        }

        ModeButton(
            label = "Override · grant access",
            selected = communityOverride == true,
            testId = "qa-preset-communityAccess-1",
            onClick = onOverrideGrant,
        )

        ModeButton(
            label = "Override · deny access",
            selected = communityOverride == false,
            testId = "qa-preset-communityAccess-2",
            onClick = onOverrideDeny,
        )

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-communityAccess-3"),
            onClick = onTrackHasAccess,
        ) { Text("Track · has access") }

        LiveFlowDisplay(label = "hasAccessToCommunity", value = hasAccessToCommunity.toString())
    }
}

/** `customEvents` scenario: track(Custom), with and without extra properties. */
@Composable
fun CustomEventsPresets(
    onTrackNoProperties: () -> Unit,
    onTrackWithProperties: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionIntro("track(Custom) — send a sample custom tracking event.")

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-customEvents-1"),
            onClick = onTrackNoProperties,
        ) { Text("Track sample event (no props)") }

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-customEvents-2"),
            onClick = onTrackWithProperties,
        ) { Text("Track sample event (with props)") }
    }
}

/** `locale` scenario: overrideDefaultLocale(locale) — force fr, force en, reset to system. */
@Composable
fun LocalePresets(
    onForceFr: () -> Unit,
    onForceEn: () -> Unit,
    onResetToSystem: () -> Unit,
) {
    // Override-UX pass: HostOverrides.localeOverride is tracking-only (overrideDefaultLocale is a
    // suspend call already routed through ScenariosViewModel/MethodState below) — the buttons read
    // it here to render selected and the badge/clear to appear, without duplicating the SDK call.
    // It is SET inside ScenariosViewModel.executeOverrideLocale()'s success branch, not eagerly in
    // onClick below: tracking it before the suspend call resolves used to show the badge/selection
    // as if the override had already taken, even on a call that then failed.
    val applied = HostOverrides.localeOverride
    val hasOverride = applied != null

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionIntro("overrideDefaultLocale(locale) — force a locale, or reset to the system default.")

        if (hasOverride) {
            OverrideActiveBadge(testId = "locale-override-badge", label = "Override active · $applied")
        }

        ModeButton(
            label = "Force fr",
            selected = applied == "fr",
            testId = "qa-preset-locale-1",
            onClick = onForceFr,
        )

        ModeButton(
            label = "Force en",
            selected = applied == "en",
            testId = "qa-preset-locale-2",
            onClick = onForceEn,
        )

        // "Reset to system" IS catalog preset 3 here — one of exactly 3 qa-preset-locale-N ids —
        // so it must stay reachable regardless of override state, per Gate B. It renders selected
        // once no override is applied, reflecting the current (default) state like the other two
        // buttons. The profileFieldsLock/contentOptions/termsAcceptance "Clear override" buttons
        // are ALSO first-class catalog presets (qa-preset-*-clear, not sample-added sugar) — they
        // used to be wrongly gated with `if (hasOverride)`, which pulled the node out of the
        // composition entirely; fixed to stay present with `enabled = hasOverride` instead, the
        // same shape "Reset to system" already had here.
        ModeButton(
            label = "Reset to system",
            selected = !hasOverride,
            testId = "qa-preset-locale-3",
            onClick = onResetToSystem,
        )
    }
}

/**
 * `refreshEntitlements` scenario: refreshEntitlements() re-issues the JWT after a tester edits
 * entitlements on the Edit profile screen (Settings), without a full connectUser() round-trip.
 * Owned by [EditUserViewModel], not [ScenariosViewModel] — resolved here with its own
 * `koinViewModel()` default. [EditUserViewModel] is registered as a Koin `viewModel` factory
 * (scoped per `NavBackStackEntry`), so this is its OWN instance, distinct from the one
 * `GroupAccessDeniedSheet` or the Edit-profile screen resolve on their own destinations — there is
 * no shared state between them, only the same class.
 */
@Composable
fun RefreshEntitlementsPresets(viewModel: EditUserViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionIntro(
            "refreshEntitlements() re-issues the JWT after entitlements were edited on the Edit " +
                "profile screen (Settings > Account > Edit profile), without a full connectUser() round-trip.",
        )

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-refreshEntitlements-1"),
            enabled = !state.isRefreshingEntitlements,
            onClick = viewModel::refreshEntitlements,
        ) {
            Text(if (state.isRefreshingEntitlements) "Refreshing…" else "Refresh entitlements")
        }

        ScenarioResult(
            text = state.entitlementsRefreshMessage ?: state.error,
            isError = state.error != null,
            resultTestTag = "refreshEntitlements-result",
        )
    }
}

/**
 * `theme` scenario: no SDK method call — the catalog's own `api_symbols.android` list is empty
 * ("the theme is a composable/host parameter, not an OctopusSDK member"). Swaps the persisted
 * [SDKConfig.Theme] that [onSetTheme] writes — the same field the Config screen's own theme picker
 * writes — which `MainViewModel` turns live into the `CommunityTheme` wrapper, no app restart.
 */
@Composable
fun ThemePresets(
    theme: SDKConfig.Theme,
    onSetTheme: (SDKConfig.Theme) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionIntro(
            "Swaps the SDKConfig.Theme handed to the SDK's CommunityTheme wrapper — the same " +
                "field the Config screen's own theme picker writes. Applied live, no restart.",
        )

        ModeButton(
            label = "Default theme",
            selected = theme is SDKConfig.Theme.Default,
            testId = "qa-preset-theme-1",
            onClick = { onSetTheme(SDKConfig.Theme.Default) },
        )

        ModeButton(
            label = "Custom theme (brand colors + logo)",
            selected = theme is SDKConfig.Theme.Custom,
            testId = "qa-preset-theme-2",
            onClick = {
                // Re-apply the existing Custom flags (colors/fonts/icons) rather than always
                // handing over Theme.Custom()'s all-true defaults: if a Custom theme with a
                // hand-picked flag subset is already persisted, this button already renders
                // selected, and tapping it must not silently overwrite it back to all-true.
                onSetTheme((theme as? SDKConfig.Theme.Custom) ?: SDKConfig.Theme.Custom())
            },
        )

        ScenarioResult(text = "Current theme: ${themeLabel(theme)}", isError = false, resultTestTag = "theme-result")
    }
}

/**
 * Renders the preset's own label rather than [SDKConfig.Theme]'s raw `toString()`, mirroring
 * [ContentOptionsSection]'s `contentOptionsLabel()` and [ProfileLockSection]'s `lockLabel()`.
 * `theme-result` is a catalog `result_test_id`, so its rendered text shape is worth keeping
 * deliberate and readable rather than incidental to a data class's generated `toString()`.
 */
private fun themeLabel(theme: SDKConfig.Theme): String =
    when (theme) {
        SDKConfig.Theme.Default -> "Default theme"
        SDKConfig.Theme.SdkDefault -> "SDK default (no theme)"
        is SDKConfig.Theme.Custom ->
            "Custom theme (colors=${theme.colors}, fonts=${theme.fonts}, icons=${theme.icons})"
    }

/**
 * `events` scenario: observes `OctopusSDK.events` — NOT `customEvents`'s emitting side, the
 * OBSERVING side. [events] is the same capped list `MainViewModel` already collects from
 * `OctopusSDK.events` for the sample's own Events screen; reused here rather than re-collecting the
 * same flow a second time. Per the catalog note, the single preset only clears this local log —
 * what QA actually asserts is what the stream emits while OTHER scenarios run, not this screen.
 */
@Composable
fun EventsPresets(
    events: List<OctopusEvent>,
    onClearEvents: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionIntro(
            "OctopusSDK.events — every event this session emitted, most recent last. The single " +
                "preset only clears this local log; QA asserts what the stream emits while other " +
                "scenarios run, not this screen.",
        )

        LiveFlowDisplay(label = "events.size", value = events.size.toString())

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-events-1"),
            onClick = onClearEvents,
        ) { Text("Clear log") }

        ScenarioResult(
            text = if (events.isEmpty()) "Log empty" else "Last: ${events.last()}",
            isError = false,
            resultTestTag = "events-result",
        )
    }
}

/**
 * `trackABTests` scenario: trackAccessToCommunity(hasAccess) — the host informing the SDK of its
 * OWN cohort decision (analytics-only). Shares the symbol with `communityAccess` preset 3, per the
 * catalog's documented split (Flutter splits the same capability the same way). Per the catalog
 * note the cohort flag is a live-state switch (`qa-trackABTests-can-access-switch`), not a preset.
 *
 * [state] is a real [ScenariosViewModel.MethodState] from its own `"trackABTestsAccess"` slot —
 * distinct from `communityAccess`'s `"trackAccess"` slot even though both call the same SDK
 * method, so tapping one card's button can no longer pre-fill the other card's result line (see
 * [ScenariosViewModel.executeTrackAccessToCommunity]'s KDoc) — the same own-slot treatment
 * [ForceOctopusABTestsPresets] gives its `"overrideAccessABTests"` slot, EXCEPT for one composed
 * case: the catalog's own "Note for QA" for this scenario reads "opens the community, or reports
 * an error when the switch is OFF", matching how the Flutter sample expresses the iOS sample's
 * disabled button. A prior fix forwarded [state] verbatim and lost that OFF-switch error
 * entirely (a real `trackAccessToCommunity` call still succeeds regardless of the switch, since
 * the switch is a sample-only gate on whether Home opens, not a parameter of the SDK call) —
 * this composes both: a genuine failure from the SDK call itself still wins and is shown as-is,
 * but a *successful* call made with the switch off has its result line replaced with the
 * catalog's error, so a QA agent asserting "error when OFF" doesn't read a pass as a failure.
 * Home is only opened when the switch is on; when it is off the tracking call is still made,
 * just without navigating. The swap is keyed off the switch position captured at tap time
 * (`lastTappedCanAccess` below), not the switch's live value, so flipping it afterward without a
 * new tap cannot retroactively re-label a result that was already produced.
 */
@Composable
fun TrackABTestsPresets(
    state: ScenariosViewModel.MethodState,
    onTrackAccess: (canAccess: Boolean) -> Unit,
    onOpenHome: () -> Unit,
) {
    var canAccess by rememberSaveable { mutableStateOf(true) }

    // Which switch position the tap was actually made with, so flipping the switch afterwards
    // (without a new tap) can't retroactively re-label a result that was already produced — the
    // same capture-at-tap pattern CommunityAccessPresets uses for lastTapped.
    var lastTappedCanAccess by rememberSaveable { mutableStateOf<Boolean?>(null) }

    // See KDoc above: only a successful call made with the switch off gets its result line
    // swapped for the catalog's OFF-switch error — a real failure from the SDK call always wins,
    // and nothing is shown before the preset has ever been tapped.
    val (resultText, resultIsError) =
        when {
            lastTappedCanAccess != false || state.isError || state.result == null ->
                state.result to state.isError
            else -> "Error: cohort denied - community not opened" to true
        }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionIntro(
            "trackAccessToCommunity(hasAccess) — analytics-only signal of a host-decided cohort. " +
                "Flip the switch, then open the community: Home only opens while the switch is on, " +
                "and the result line reports an error while it's off.",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Cohort can access", style = MaterialTheme.typography.bodyMedium)
            Switch(
                colors = sampleDefaultSwitchColors(),
                checked = canAccess,
                onCheckedChange = { canAccess = it },
                modifier = Modifier.testTag("qa-trackABTests-can-access-switch"),
            )
        }

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-trackABTests-1"),
            onClick = {
                lastTappedCanAccess = canAccess
                onTrackAccess(canAccess)
                if (canAccess) onOpenHome()
            },
        ) { Text("Open Octopus Home Screen") }

        if (!canAccess) {
            Text(
                text =
                    "Switch is off — tracking still fires, but Home is not opened and the " +
                        "result below reports an error.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // The one panel that keeps its own result node: what it shows is a TRANSFORM of the
        // method state (a successful call made with the switch off is relabelled as the catalog's
        // OFF-switch error), not a copy of it, so the screen's raw Result block cannot stand in
        // for it. `trackABTests` is therefore absent from ScenarioCatalog.METHOD_KEYS.
        ScenarioResult(text = resultText, isError = resultIsError, resultTestTag = "trackABTests-result")
    }
}

/**
 * `forceOctopusABTests` scenario: overrideCommunityAccess(hasAccess) — the SDK-side override that
 * takes full precedence over any cohort trackAccessToCommunity reported. Shares the symbol with
 * `communityAccess` presets 1-2, per the catalog's documented split (Flutter splits the same
 * capability the same way) — reuses the same [HostOverrides.communityAccessOverride] tracking
 * state so the badge stays consistent between both cards, set from the same
 * `ScenariosViewModel.executeOverrideCommunityAccess()` success branch [CommunityAccessPresets]
 * uses (see its KDoc for why it is not set eagerly in onClick). Preset 3 "re-applies" by resending
 * the last value forced here — overrideCommunityAccess has no getter to read the SDK's own current
 * value back.
 *
 * The screen's Result block reads this scenario's own `"overrideAccessABTests"` slot — distinct
 * from `communityAccess`'s `"overrideAccess"` slot even though both call the same SDK method
 * through the same [ScenariosViewModel.executeOverrideCommunityAccess], so tapping one card's
 * button can no longer pre-fill the other card's result. The badge above stays shared on purpose
 * (it mirrors one process-wide SDK override both cards should agree on); only the result is
 * split, mirroring [TrackABTestsPresets]'s own `"trackABTestsAccess"` split.
 */
@Composable
fun ForceOctopusABTestsPresets(
    hasAccessToCommunity: Boolean,
    onGrant: () -> Unit,
    onDeny: () -> Unit,
) {
    val communityOverride = HostOverrides.communityAccessOverride

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionIntro(
            "overrideCommunityAccess(hasAccess) forces the cohort the SDK itself reports, taking " +
                "full precedence over trackAccessToCommunity. Preset 3 re-applies the last value " +
                "forced here — there is no getter to read the SDK's own current override.",
        )

        if (communityOverride != null) {
            OverrideActiveBadge(
                testId = "forceOctopusABTests-override-badge",
                label = if (communityOverride) "Forced · granted" else "Forced · denied",
            )
        }

        ModeButton(
            label = "Force cohort · grant community access",
            selected = communityOverride == true,
            testId = "qa-preset-forceOctopusABTests-1",
            onClick = onGrant,
        )

        ModeButton(
            label = "Force cohort · deny community access",
            selected = communityOverride == false,
            testId = "qa-preset-forceOctopusABTests-2",
            onClick = onDeny,
        )

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-forceOctopusABTests-3"),
            enabled = communityOverride != null,
            onClick = {
                when (communityOverride) {
                    true -> onGrant()
                    false -> onDeny()
                    null -> Unit
                }
            },
        ) { Text("Re-apply current SDK value") }

        LiveFlowDisplay(label = "hasAccessToCommunity", value = hasAccessToCommunity.toString())
    }
}

/**
 * `groupAccessDenied` scenario: setGroupAccessDeniedCallback + its property getter
 * (`groupAccessDeniedCallback`, compiled to `getGroupAccessDeniedCallback` — both catalog
 * `api_symbols`) — the SDK-level fallback the host hears from when a member touches a locked
 * group, but only when the per-screen `onGroupAccessDenied` passed to `octopusComposables(...)`
 * is null.
 *
 * This IS reachable from the real flow, not merely hypothetically: `MainActivity` passes null at
 * BOTH of its `octopusComposables(...)` call sites whenever the
 * `hostCallbacks.groupAccessDeniedWired` toggle (Developer tools › Host app integration) is
 * off, and `OctopusNavigation.kt`'s fallback to
 * `OctopusSDK.groupAccessDeniedCallback` then picks up exactly what this scenario writes. (An
 * earlier version of this KDoc claimed the per-screen callback was always non-null and therefore
 * always took precedence — that was wrong; the precedence rule is real, the "always non-null"
 * premise under it was not.)
 *
 * Today's blast radius is nil regardless: `OctopusScreenContent` invokes the resolved callback
 * null-safely (`?.invoke`), and nothing else in the sample ever sets the SDK-level callback. But
 * "nil because nothing else touches it" is not the same as "safe by design" — so rather than lean
 * on that, "Register" installs a lambda that is inert but OBSERVABLE: it only increments
 * [HostOverrides.groupAccessDeniedFireCount] (shown below), it never navigates. Whichever way
 * `groupAccessDeniedWired` is set, tapping a locked group with this preset registered can only move
 * a number on this screen.
 *
 * The setter's callback parameter is non-nullable — there is no way to pass null, and no separate
 * unregister method. "Unregister" therefore installs a real no-op, deliberately distinct from the
 * counting lambda above rather than a second copy of it, so the two presets are told apart at
 * runtime and not just by their labels; `registered` tracks which of the two this scenario itself
 * last installed, since the getter alone can't distinguish "a no-op callback" from "no callback"
 * once both have been set at least once.
 *
 * `registered` and the fire count both live on [HostOverrides] rather than as `rememberSaveable`
 * here: the callback fires from the Octopus screen, a different NavHost destination, which disposes
 * this composition before the fallback ever runs. A `rememberSaveable` var would count into an
 * orphaned `MutableState` this screen never reads again — see [HostOverrides]'s KDoc for the full
 * story.
 */
@Composable
fun GroupAccessDeniedPresets() {
    val registered = HostOverrides.groupAccessDeniedRegistered
    val fireCount = HostOverrides.groupAccessDeniedFireCount

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionIntro(
            "setGroupAccessDeniedCallback registers the SDK-level fallback used only when the " +
                "per-screen onGroupAccessDenied is null (Developer tools > Host app integration > onGroupAccessDenied off). " +
                "\"Register\" here only counts fires below — it never navigates — so it's safe to " +
                "leave on regardless of that toggle. \"Unregister\" installs a real no-op — the " +
                "setter's parameter can't be null.",
        )

        if (registered != null) {
            OverrideActiveBadge(
                testId = "groupAccessDenied-badge",
                label = if (registered == true) "Registered" else "Unregistered (no-op)",
            )
        }

        ModeButton(
            label = "Register callback",
            selected = registered == true,
            testId = "qa-preset-groupAccessDenied-1",
            onClick = HostOverrides::registerGroupAccessDeniedCallback,
        )

        ModeButton(
            label = "Unregister callback",
            selected = registered == false,
            testId = "qa-preset-groupAccessDenied-2",
            onClick = HostOverrides::unregisterGroupAccessDeniedCallback,
        )

        if (registered == true) {
            LiveFlowDisplay(label = "fired", value = fireCount.toString())
        }

        ScenarioResult(
            text =
                when (registered) {
                    true -> "Success: callback registered (fired $fireCount time(s))"
                    false -> "Success: callback set to no-op (unregister substitute)"
                    null -> null
                },
            isError = false,
            resultTestTag = "groupAccessDenied-result",
        )
    }
}

/**
 * `pushNotifications` scenario: simulates a Firebase `RemoteMessage.data` map for an Octopus post
 * notification — use the QA post or paste a postId, tap the preset, and the sample parses it with
 * `isOctopusNotification`/`getOctopusNotification` before opening the post via [onOpenPost], the
 * same way a real `FirebaseMessagingService.onMessageReceived` handler would.
 *
 * Does NOT call `registerNotificationsToken`, even though it's listed under this scenario's
 * `api_symbols` in the shared catalog: the catalog's single preset here
 * (`qa-preset-pushNotifications-1`, "Open sample notification (deep link)") is about the deep-link
 * parse only, and there is no dedicated preset for token registration. `registerNotificationsToken`
 * is fire-and-forget (`OctopusSDK.kt` launches it on `sdkScope`, result discarded — "Success" would
 * prove nothing) and, unlike this scenario's other side effects, is a real backend write: a fake
 * token here would displace the genuine one `MessagingService` registers on this device, breaking
 * push for that account. Left to the real integration instead.
 */
@Composable
fun PushNotificationsPresets(onOpenPost: (postId: String) -> Unit) {
    var postIdInput by rememberSaveable { mutableStateOf(SampleFixtures.postText) }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    var isError by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionIntro(
            "Simulates a Firebase RemoteMessage.data map for an Octopus post notification: use the QA post or paste " +
                "a postId, tap the preset, and the sample parses it with isOctopusNotification() / " +
                "getOctopusNotification() before opening the post, the same way a real FCM handler " +
                "would.",
        )

        OutlinedTextField(
            value = postIdInput,
            onValueChange = { postIdInput = it },
            label = { Text("Post ID") },
            modifier = Modifier.fillMaxWidth().testTag("pushNotifications-postid-input"),
            singleLine = true,
        )

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-pushNotifications-1"),
            onClick = {
                val data =
                    mapOf(
                        OctopusNotification.IS_OCTOPUS_NOTIFICATION_KEY to "true",
                        OctopusNotification.TITLE_KEY to "New reply on your post",
                        OctopusNotification.BODY_KEY to "Someone replied to you on Octopus",
                        OctopusNotification.LINK_PATH_KEY to "/posts/$postIdInput",
                        OctopusNotification.POST_ID_KEY to postIdInput,
                    )
                val notification =
                    if (OctopusSDK.isOctopusNotification(data)) OctopusSDK.getOctopusNotification(data) else null
                val postId = notification?.postId?.takeUnless { it.isBlank() }
                when {
                    notification == null -> {
                        isError = true
                        result = "Not recognized as an Octopus notification"
                    }

                    postId == null -> {
                        isError = true
                        result = "Parsed, but no postId to open — paste a Post ID first."
                    }

                    else -> {
                        isError = false
                        result = "Success: opening postId=$postId"
                        onOpenPost(postId)
                    }
                }
            },
        ) { Text("Open sample notification (deep link)") }

        ScenarioResult(text = result, isError = isError, resultTestTag = "pushNotifications-result")
    }
}
