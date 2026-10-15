package com.octopuscommunity.sdk.sample.ui.scenarios

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.domain.model.CreatePostScreenInfo
import com.octopuscommunity.sdk.domain.model.OctopusEvent
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.sample.data.model.FeatureToggles
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.ui.components.ApiMethodCard
import com.octopuscommunity.sdk.sample.ui.components.ApiPill
import com.octopuscommunity.sdk.sample.ui.components.CodeResultBlock
import com.octopuscommunity.sdk.sample.ui.components.PrimaryPillButton
import com.octopuscommunity.sdk.sample.ui.components.SampleBanner
import com.octopuscommunity.sdk.sample.ui.components.SampleBannerTone
import com.octopuscommunity.sdk.sample.ui.components.SampleScenarioBlock
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.BridgeSection
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.BridgeShareSection
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.CommunityDataSection
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.ConnectionSection
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.ConsentTextsSection
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.ContentOptionsSection
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.ForceNicknameChoiceSection
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.GroupsSection
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.LifecycleSection
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.NotificationsSection
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.ProfileLockSection
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.ReactionsSection
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.SwitchCommunitySection
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.TermsAcceptanceSection
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.UnifiedProfileSection
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.UserActivitySection
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.UserProfileSection
import com.octopuscommunity.sdk.sample.ui.theme.SampleSpacing
import com.octopuscommunity.sdk.sample.ui.theme.SampleTheme

/**
 * One scenario's control panel, dispatched by [scenarioId] — the destination of every card on
 * [ScenariosListScreen]. Reuses the same section composables the legacy `ExplorerScreen` accordion
 * used (that accordion is deleted as dead code by this same chantier, see MainScaffold), plus the
 * new Tier-2 preset composables in `ScenarioPresetSections.kt` for the catalog scenarios that had
 * no UI before Lot 2.
 *
 * [viewModel] is the SAME [ScenariosViewModel] instance for every scenario (hoisted once in
 * `MainScaffold`, not per-destination) — Koin's `koinViewModel()` scopes to the enclosing
 * `NavBackStackEntry`, so re-resolving it per scenario would fragment state (e.g. a fetched groups
 * list forgotten when navigating from `groups` to `syncFollowGroups` and back).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioDetailScreen(
    scenarioId: String,
    viewModel: ScenariosViewModel,
    onOpenCreatePost: ((CreatePostScreenInfo) -> Unit)? = null,
    onOpenClientProfile: (clientUserId: String) -> Unit,
    onOpenPost: (postId: String) -> Unit,
    onOpenComment: (commentId: String) -> Unit,
    onOpenBridgeTopic: (groupId: String) -> Unit,
    onOpenHome: () -> Unit,
    // The three presentation shapes of the shared catalog. They take a host container rather than
    // an SDK call, so unlike every other action on this screen they are hoisted all the way to the
    // Activity (`sheet`, `modal`) or pushed on the host graph (`fullscreen`) — see
    // `PresentationPresetSections.kt`.
    onOpenCommunityFullScreen: (postId: String?, withBottomBar: Boolean) -> Unit = { _, _ -> },
    onOpenCommunitySheet: (postId: String?) -> Unit = {},
    onOpenCommunityModal: (postId: String?) -> Unit = {},
    onOpenUserActivity: (userId: String) -> Unit,
    onOpenUserActivityByClientUserId: (clientUserId: String) -> Unit,
    onOpenMyProfile: () -> Unit,
    onOpenProfileByUserId: (userId: String) -> Unit,
    onOpenProfileByClientUserId: (clientUserId: String) -> Unit,
    sdkEvents: List<OctopusEvent>,
    onClearEvents: () -> Unit,
    theme: SDKConfig.Theme,
    onSetTheme: (SDKConfig.Theme) -> Unit,
    onVerifyInCommunity: () -> Unit,
    // Read here, changed on the Scenarios list — the section header owns the switch and this
    // screen owns nothing. Two places to flip the same boolean is how a tester ends up reporting
    // a state they never set. The link passes the section, so the list opens on its header.
    featureToggles: FeatureToggles = FeatureToggles.Default,
    onOpenScenarios: (ScenarioSection) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val unifiedProfileActive by viewModel.unifiedProfileActive.collectAsStateWithLifecycle()
    val spec = ScenarioCatalog.find(scenarioId)
    val context = LocalContext.current
    // The connected, non-guest user's own Octopus profile id — seeds the "activity" / "profile by
    // user id" fields below so a first tap resolves without pasting an id manually.
    val connectedProfileUserId =
        (state.connectionState as? ConnectionState.Connected)
            ?.takeUnless { it.profile.isGuest }
            ?.profile
            ?.userId

    // The most recent call across this scenario's method slots. Hoisted above the layout because
    // two zones read it: the Result zone renders it, and Customize compares its rank against the
    // last edit to tell a fresh result from one that answers values nobody is sending any more.
    val lastCall =
        spec
            ?.methodKeys
            ?.mapNotNull { key -> state.methods[key] }
            ?.filter { it.rank > 0L }
            ?.maxByOrNull { it.rank }

    // Zone 2's opt-in editor, and the owner of "which preset are we on". Null for every scenario
    // that deliberately has no open value domain — `scenarioCustomizeSpec` carries that list and
    // the reason for each exclusion.
    val customize =
        rememberScenarioCustomizeState(
            scenarioId = scenarioId,
            spec = scenarioCustomizeSpec(scenarioId, state, viewModel),
            lastCallRank = lastCall?.rank ?: 0L,
        )

    // A call's result lands in zone 4, below the panel whose button made it — on a phone usually
    // under the fold, so a tap "did nothing" until the tester thought to scroll. Each call started
    // from this screen (a rank above the one found on entry) scrolls the Result zone into view, once
    // when it starts and once when its result lands and the block grows. Entering the screen on an
    // older result does not move anything.
    val resultBringIntoView = remember { BringIntoViewRequester() }
    val entryRank = remember(scenarioId) { lastCall?.rank ?: 0L }
    val lastCallRank = lastCall?.rank ?: 0L
    val lastCallLanded = lastCall?.result != null
    LaunchedEffect(lastCallRank, lastCallLanded) {
        if (lastCallRank > entryRank) resultBringIntoView.bringIntoView()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("scenario-detail-screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Zone 1 — what this calls, and what it does not do. Two lines, no more: a tester who has
        // to read a paragraph before pressing a button will press the button first.
        spec?.let { ScenarioHeader(it) }

        // Still zone 1, when the section this scenario sits under owns a switch: the position of
        // that switch, read-only, and the way back to the one place it can be moved.
        spec?.section?.let { section ->
            section.featureToggle?.let { toggle ->
                FeatureStateLine(
                    text = toggle.stateLine(featureToggles[toggle]),
                    onOpenScenarios = { onOpenScenarios(section) },
                )
            }
        }

        // Zones 2 to 5 sit in one tinted "Try it" block (DS ScenarioBlock, screen 08): parameters,
        // the promise, the controls and the result read as one bench, apart from the header above
        // and the verify step below.
        SampleScenarioBlock(title = "Try it", tryTone = true) {
            // Zone 2 — what the call takes. Declared once in the catalog so the pill, the parameters
            // and the panel below can never disagree about which API this scenario is; where the
            // parameter has an open domain, the block also carries the Customize affordance that lets a
            // human retype the value the presets pre-fill.
            spec?.parameters?.takeIf { it.isNotEmpty() }?.let {
                ScenarioParametersBlock(params = it, customize = customize)
            }

            // The promise, in the words the PO checks it against — outside the parameters block so a
            // scenario that takes no argument still makes one. A run whose expected outcome is only
            // in the test plan is a run nobody can judge on the device.
            spec?.let {
                Text(
                    text = "You will see: ${it.youWillSee}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("scenario-you-will-see"),
                )
            }

            // Zone 3 — the controls. Each panel renders its own, because each panel's inputs are
            // genuinely different and its Run buttons carry the `qa-preset-<id>-<n>` ids the shared
            // QA runner drives: several catalog scenarios legitimately expose three or seven actions,
            // which no single anchored button could stand for.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (scenarioId) {
                    "unifiedProfile" ->
                        UnifiedProfileSection(
                            enabled = state.sdkConfig?.unifiedProfileEnabled == true,
                            active = unifiedProfileActive,
                            onEnabledChange = viewModel::executeSetUnifiedProfile,
                        )

                    "connection" ->
                        ConnectionSection(
                            connectionState = state.connectionState,
                            isUserConnected = state.isUserConnected,
                            getMethodState = { key -> state.methods[key] ?: ScenariosViewModel.MethodState() },
                            onConnectUser = viewModel::executeConnectUser,
                            onDisconnectUser = viewModel::executeDisconnectUser,
                            connectTestTag = "qa-preset-connection-1",
                            disconnectTestTag = "qa-preset-connection-5",
                        )

                    "groups" ->
                        GroupsSection(
                            groups = state.groups,
                            filteredGroups = state.filteredGroups,
                            groupSearchQuery = state.groupSearchQuery,
                            groupIdInput = state.groupIdInput,
                            loadingGroupIds = state.loadingGroupIds,
                            syncOffset = state.syncOffset,
                            getMethodState = { key -> state.methods[key] ?: ScenariosViewModel.MethodState() },
                            onFetchGroups = viewModel::executeFetchGroups,
                            onGroupSearchQueryChanged = viewModel::setGroupSearchQuery,
                            onGroupIdChanged = viewModel::setGroupIdInput,
                            onFollowGroup = viewModel::executeFollowGroup,
                            onUnfollowGroup = viewModel::executeUnfollowGroup,
                            onSyncOffsetChanged = viewModel::setSyncOffset,
                            onSyncFollowGroupsInvertAll = viewModel::executeSyncFollowGroupsInvertAll,
                            onSyncFollowGroupsForceFollowAll = viewModel::executeSyncFollowGroupsForceFollowAll,
                            onSyncFollowGroupsForceUnfollowAll = viewModel::executeSyncFollowGroupsForceUnfollowAll,
                            fetchGroupsTestTag = "qa-preset-groups-1",
                        )

                    "syncFollowGroups" ->
                        GroupsSection(
                            groups = state.groups,
                            filteredGroups = state.filteredGroups,
                            groupSearchQuery = state.groupSearchQuery,
                            groupIdInput = state.groupIdInput,
                            loadingGroupIds = state.loadingGroupIds,
                            syncOffset = state.syncOffset,
                            getMethodState = { key -> state.methods[key] ?: ScenariosViewModel.MethodState() },
                            onFetchGroups = viewModel::executeFetchGroups,
                            onGroupSearchQueryChanged = viewModel::setGroupSearchQuery,
                            onGroupIdChanged = viewModel::setGroupIdInput,
                            onFollowGroup = viewModel::executeFollowGroup,
                            onUnfollowGroup = viewModel::executeUnfollowGroup,
                            onSyncOffsetChanged = viewModel::setSyncOffset,
                            onSyncFollowGroupsInvertAll = viewModel::executeSyncFollowGroupsInvertAll,
                            onSyncFollowGroupsForceFollowAll = viewModel::executeSyncFollowGroupsForceFollowAll,
                            onSyncFollowGroupsForceUnfollowAll = viewModel::executeSyncFollowGroupsForceUnfollowAll,
                            syncInvertAllTestTag = "qa-preset-syncFollowGroups-1",
                            syncForceFollowAllTestTag = "qa-preset-syncFollowGroups-2",
                            syncForceUnfollowAllTestTag = "qa-preset-syncFollowGroups-3",
                        )

                    "notSeenNotifications" ->
                        NotificationsSection(
                            notSeenCount = state.notSeenCount,
                            getMethodState = { key -> state.methods[key] ?: ScenariosViewModel.MethodState() },
                            onUpdateNotSeenCount = viewModel::executeUpdateNotSeenCount,
                            onRegisterToken = viewModel::executeRegisterToken,
                            showRegisterToken = false,
                            updateNotSeenTestTag = "qa-preset-notSeenNotifications-2",
                            onOpenHome = onOpenHome,
                            openHomeTestTag = "qa-preset-notSeenNotifications-1",
                        )

                    "pushNotifications" ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PushNotificationsPresets(onOpenPost = onOpenPost)
                            // Rehomed from the API Explorer (DS v1): the token registration call
                            // has no other entry point once the Explorer is gone. Its raw result
                            // lands in the untagged Result block below — the preset above owns
                            // `pushNotifications-result`.
                            ApiMethodCard(
                                name = "registerNotificationsToken()",
                                description = "Fetch the FCM token and register it with the SDK",
                                isLoading = state.methods["registerToken"]?.isLoading == true,
                                onExecute = viewModel::executeRegisterToken,
                                cardTestTag = "pushNotifications-register-token",
                            )
                        }

                    "communityAccess" ->
                        CommunityAccessPresets(
                            hasAccessToCommunity = state.hasAccessToCommunity,
                            onOverrideGrant = {
                                viewModel.setCommunityAccessInput(true)
                                viewModel.executeOverrideCommunityAccess()
                            },
                            onOverrideDeny = {
                                viewModel.setCommunityAccessInput(false)
                                viewModel.executeOverrideCommunityAccess()
                            },
                            onTrackHasAccess = {
                                viewModel.setCommunityAccessInput(true)
                                viewModel.executeTrackAccessToCommunity()
                            },
                        )

                    // These two panels' buttons stay exactly what they were — one tap, same
                    // `qa-preset-*` ids — but they now write their values through the Customize spec
                    // instead of inlining them. That is what lets the block above answer "which preset
                    // is this?" at all: the preset's values exist in one place, so a button and the
                    // `Custom` chip cannot drift apart. Tapping one also closes Customize, which is the
                    // design's "picking a preset again exits Customize".
                    "customEvents" ->
                        CustomEventsPresets(
                            onTrackNoProperties = { customize.runPreset(0) },
                            onTrackWithProperties = { customize.runPreset(1) },
                        )

                    "locale" ->
                        LocalePresets(
                            onForceFr = { customize.runPreset(0) },
                            onForceEn = { customize.runPreset(1) },
                            onResetToSystem = { customize.runPreset(2) },
                        )

                    "lifecycle" -> {
                        LifecycleSection(
                            isInitialized = state.isInitialized,
                            getMethodState = { key -> state.methods[key] ?: ScenariosViewModel.MethodState() },
                            onReset = viewModel::executeReset,
                            onStop = viewModel::executeStop,
                            resetTestTag = "qa-preset-lifecycle-2",
                            stopTestTag = "qa-preset-lifecycle-3",
                        )

                        HorizontalDivider()

                        // No qa-preset id here: the catalog's preset 1 ("switchCommunity, alt key")
                        // needs a second real community API key this sample never bakes in, so it
                        // legitimately stays non-built (see ScenarioCatalog) — this is the same manual
                        // key-paste UI the legacy accordion offered, without a wired preset button.
                        Text(
                            text = "Switch community",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        SwitchCommunitySection(
                            switchApiKeyInput = state.switchApiKeyInput,
                            switchingApiKey = state.switchingApiKey,
                            onSwitchApiKeyChanged = viewModel::setSwitchApiKeyInput,
                            onSwitchCommunity = { viewModel.executeSwitchCommunity(context) },
                        )
                    }

                    "createPost" ->
                        if (onOpenCreatePost != null) {
                            BridgeShareSection(
                                groups = state.groups,
                                onOpenCreatePost = onOpenCreatePost,
                            )
                        } else {
                            PendingPlaceholder("Bridge Share isn't wired on this navigation host.")
                        }

                    "reactions" -> ReactionsSection()

                    "communityData" -> {
                        CommunityDataSection(
                            scenario = state.communityDataScenario,
                            lookupClientUserId = state.communityDataLookupClientUserId,
                            // Same blank-safety as the lookup getter, so a blank id renders "—" rather
                            // than an empty Text with no node for the accessibility tree to report.
                            profileClientUserId =
                                state.connectionState.profile
                                    ?.clientUserId
                                    ?.takeUnless { it.isBlank() },
                            userIdInput = state.communityDataUserIdInput,
                            rawFetchState = state.methods["communityData"] ?: ScenariosViewModel.MethodState(),
                            onUserIdInputChanged = viewModel::setCommunityDataUserId,
                            onRawFetch = viewModel::executeFetchCommunityData,
                            onFetchByClientUserId = viewModel::executeCommunityDataFetchByClientUserId,
                            onFetchByUserId = viewModel::executeCommunityDataFetchByUserId,
                            onStartObserving = viewModel::startCommunityDataObservation,
                            onStopObserving = viewModel::stopCommunityDataObservation,
                            onCheckIdContract = viewModel::reportCommunityDataIdContract,
                            // The ViewModel owns the precondition: it posts either the hand-off result
                            // or a stated reason, and only returns an id when there is one to navigate
                            // with.
                            onOpenClientProfile = {
                                viewModel.onCommunityDataProfileOpened()?.let { onOpenClientProfile(it) }
                            },
                        )
                    }

                    "initialScreen" -> {
                        // Restored from the pre-internal change legacy accordion's "User activity" / "User profile"
                        // sections. Attached here, not to `communityData`: the shared catalog
                        // (`scenario catalog`) carries these four behaviors as
                        // `initialScreen` presets 8-11, so this is the scenario screen the catalog
                        // expects them under.
                        UserActivitySection(
                            connectedProfileUserId = connectedProfileUserId,
                            connectedClientUserId = state.connectedClientUserId,
                            onOpenUserActivity = onOpenUserActivity,
                            onOpenUserActivityByClientUserId = onOpenUserActivityByClientUserId,
                        )

                        HorizontalDivider()
                        UserProfileSection(
                            connectedProfileUserId = connectedProfileUserId,
                            connectedClientUserId = state.connectedClientUserId,
                            onOpenMyProfile = onOpenMyProfile,
                            onOpenProfileByUserId = onOpenProfileByUserId,
                            onOpenProfileByClientUserId = onOpenProfileByClientUserId,
                        )
                    }

                    "profileFieldsLock" -> ProfileLockSection()

                    "contentOptions" -> ContentOptionsSection()

                    "termsAcceptance" -> TermsAcceptanceSection()

                    "consentTexts" -> ConsentTextsSection()

                    "forceNicknameChoice" -> ForceNicknameChoiceSection()

                    "refreshEntitlements" -> RefreshEntitlementsPresets()

                    "bridge" ->
                        BridgeSection(
                            groups = state.groups,
                            onOpenPost = onOpenPost,
                            onOpenBridgeTopic = onOpenBridgeTopic,
                        )

                    "theme" ->
                        ThemePresets(
                            theme = theme,
                            onSetTheme = onSetTheme,
                        )

                    "events" ->
                        EventsPresets(
                            events = sdkEvents,
                            onClearEvents = onClearEvents,
                        )

                    "trackABTests" ->
                        TrackABTestsPresets(
                            // Own key, distinct from communityAccess's "trackAccess" slot — see
                            // ScenariosViewModel.executeTrackAccessToCommunity's KDoc — so this card
                            // never shows a result the communityAccess card produced.
                            state = state.methods["trackABTestsAccess"] ?: ScenariosViewModel.MethodState(),
                            onTrackAccess = { canAccess ->
                                viewModel.setCommunityAccessInput(canAccess)
                                viewModel.executeTrackAccessToCommunity(methodKey = "trackABTestsAccess")
                            },
                            onOpenHome = onOpenHome,
                        )

                    "forceOctopusABTests" ->
                        ForceOctopusABTestsPresets(
                            hasAccessToCommunity = state.hasAccessToCommunity,
                            onGrant = {
                                viewModel.setCommunityAccessInput(true)
                                viewModel.executeOverrideCommunityAccess(methodKey = "overrideAccessABTests")
                            },
                            onDeny = {
                                viewModel.setCommunityAccessInput(false)
                                viewModel.executeOverrideCommunityAccess(methodKey = "overrideAccessABTests")
                            },
                        )

                    "groupAccessDenied" -> GroupAccessDeniedPresets()

                    "fullscreen" ->
                        FullscreenPresets(
                            onOpenCommunityFullScreen = onOpenCommunityFullScreen,
                            onOpenComment = onOpenComment,
                        )

                    "modal" -> ModalPresets(onOpenCommunityModal = onOpenCommunityModal)

                    "sheet" -> SheetPresets(onOpenCommunitySheet = onOpenCommunitySheet)

                    else ->
                        PendingPlaceholder(
                            spec?.subtitle
                                ?: "Unknown scenario id \"$scenarioId\" — not in ScenarioCatalog.",
                        )
                }
            }

            // Zone 4 — the raw result of the last call this scenario made, whichever of its method
            // slots produced it. Absent until something has run: an empty terminal block on entry
            // reads as a failed call.
            //
            // It normally carries the catalog's `result_test_id` (shared
            // `scenario catalog`). The panels above used to print the same string a
            // second time in a grey inline box, with per-card "which button did you tap last"
            // bookkeeping purely to decide where the catalog id belonged; hoisting the block here
            // deletes that bookkeeping and the duplicate. Where a panel legitimately owns the id
            // already — `communityData`, whose ResultPanel shows the preset scenario state this block
            // cannot see — the catalog opts out and this block stays untagged, so uiautomator never
            // finds two nodes with the same resource-id showing different states.
            val resultTestId = ScenarioCatalog.resultTestId(scenarioId)
            //
            // Three states, and all three are visible — "tapping Run and seeing nothing happen" was
            // the first complaint on every platform. Idle says so in one muted line rather than an
            // empty terminal block (which reads as a failed call); Running shows the skeleton, which
            // is the one and only loading affordance the design allows here — no toast, no snackbar,
            // because a snackbar covers the very thing the tester is looking at.
            //
            // Guarded on `methodKeys`: a scenario whose outcome is a screen opening (the three
            // presentation modes) writes no method slot, so a Result zone there would sit on "Not run
            // yet" forever — a permanent lie is worse than an absent zone. The design's answer for
            // those, the event trail, is listed as a gap rather than faked here.
            if (spec != null && spec.methodKeys.isNotEmpty()) {
                Column(
                    modifier = Modifier.bringIntoViewRequester(resultBringIntoView),
                    verticalArrangement = Arrangement.spacedBy(SampleSpacing.space3),
                ) {
                    Text(
                        text = "Result",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    when {
                        // Customize moved the values after the last call landed. Same muted line, same
                        // `scenario-result-idle` tag — deliberately not a new id, because this is the same
                        // "there is nothing here to read" state the tag already means, and Customize is
                        // required to add no test id at all. Clears the moment Run is pressed: the values
                        // are snapshotted at the *start* of a call, not on its result.
                        customize.resultStale ->
                            Text(
                                text = "Not run yet — values changed",
                                style = MaterialTheme.typography.bodySmall,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("scenario-result-idle"),
                            )

                        lastCall == null || (lastCall.result == null && !lastCall.isLoading) ->
                            Text(
                                text = "Not run yet",
                                style = MaterialTheme.typography.bodySmall,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("scenario-result-idle"),
                            )

                        lastCall.result == null -> ResultSkeleton()

                        else ->
                            CodeResultBlock(
                                result = lastCall.result.orEmpty(),
                                isError = lastCall.isError,
                                durationMs = lastCall.durationMs,
                                modifier = resultTestId?.let { Modifier.testTag(it) } ?: Modifier,
                            )
                    }
                }
            }

            // Zone 5 — the anchored primary action, for the scenarios that really do have exactly one.
            // Wired only where that action lives on this shared ViewModel; the other single-action
            // scenarios (refreshEntitlements, pushNotifications) own their action inside a
            // self-contained panel with its own ViewModel, and duplicating it here would mean two
            // buttons firing the same call from one screen.
            val primaryRun: Pair<String, () -> Unit>? =
                when (scenarioId) {
                    "groups" -> "Run fetchGroups()" to viewModel::executeFetchGroups
                    "trackABTests" ->
                        "Run trackAccessToCommunity(true)" to {
                            viewModel.setCommunityAccessInput(true)
                            viewModel.executeTrackAccessToCommunity(methodKey = "trackABTestsAccess")
                        }

                    else -> null
                }
            primaryRun?.let { (label, action) ->
                PrimaryPillButton(
                    text = label,
                    onClick = action,
                    isLoading = lastCall?.isLoading == true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("scenario-run"),
                )
            }

            // Customize's own Run, shown only while the fields are unlocked. It exists because the
            // preset buttons above would overwrite what the tester just typed — with the fields open,
            // there has to be a way to send *these* values.
            //
            // Reuses `scenario-run` rather than minting an id: the `primaryRun` above covers `groups`
            // and `trackABTests`, neither of which has a Customize spec, so the two are disjoint by
            // construction and the guard below keeps them that way if that ever changes. QA never opens
            // Customize, so this node never exists during a pipeline run.
            if (customize.spec != null && customize.customizing && primaryRun == null) {
                PrimaryPillButton(
                    text = if (lastCall?.result != null) "Run again" else "Run",
                    onClick = customize::run,
                    isLoading = lastCall?.isLoading == true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("scenario-run"),
                )
            }
        }

        // Zone 6 — the observable half of the loop. Only for scenarios whose effect actually shows
        // up in the feed; offering to "verify" an analytics-only call would teach the wrong thing.
        if (spec?.isVerifiableInCommunity == true) {
            SecondaryPillButton(
                text = "Verify in Community",
                onClick = onVerifyInCommunity,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("scenario-verify-in-community"),
            )
        }

        // Last, and deliberately so: the engineering caveat answers "why can I not do X here?",
        // a question you only have after trying. On the list card it was a paragraph between the
        // tester and the tap; here it costs nothing and is the only written trace of the gap.
        spec?.note?.takeIf { it.isNotBlank() }?.let { note ->
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            Text(
                text = "Note",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("scenario-note"),
            )
        }
    }
}

/**
 * The Running state of zone 4: three bars where the result will land.
 *
 * A skeleton rather than a spinner because it says *where* the answer will appear as well as that
 * one is coming, and it holds the layout still so the screen does not jump under the thumb when
 * the call returns. The pulse is opacity only — no horizontal sweep — so it stays legible with
 * "Remove animations" on.
 */
@Composable
private fun ResultSkeleton() {
    val transition = rememberInfiniteTransition(label = "result-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 700),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "result-skeleton-alpha",
    )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SampleTheme.colors.codeBg)
                .padding(12.dp)
                .testTag("scenario-result-skeleton"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(0.45f, 0.8f, 0.6f).forEach { widthFraction ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(widthFraction)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = alpha)),
            )
        }
    }
}

/**
 * The read-only feature-state line of zone 1: the position of the section's switch, and a way back
 * to the section header that owns it — the list opens with that section expanded and scrolled to.
 *
 * Read-only is the whole point, and it is the shared design contract's rule rather than a local
 * choice: *"a feature toggle is read here, changed in the list"*. A switch offered on both screens
 * would be two truths about one boolean, and the sample exists to be believed.
 *
 * Drawn as the DS info banner: the state is the title, "Change" is the action, and the action
 * keeps the 48 dp touch target so it survives a large system font and a thumb.
 */
@Composable
private fun FeatureStateLine(
    text: String,
    onOpenScenarios: () -> Unit,
) {
    SampleBanner(
        tone = SampleBannerTone.Info,
        title = text,
        actionLabel = "Change",
        onAction = onOpenScenarios,
        modifier = Modifier.testTag("scenario-feature-state"),
    )
}

/**
 * Zone 1 of the scenario skeleton: the API pill and the two lines under it.
 *
 * The pill is the scenario's identity — the same string on the list card, here, and in the Explorer
 * — so a tester can move between the three surfaces without re-deriving what is being called.
 */
@Composable
private fun ScenarioHeader(spec: ScenarioSpec) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ApiPill(spec.api)
        Text(
            text = spec.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Fallback panel for a scenario id with nothing to show: either `onOpenCreatePost` isn't wired on
 * this navigation host (the "createPost" case above), or [scenarioId] doesn't resolve in
 * [ScenarioCatalog] at all (stale deep link, typo). [ScenarioStatus.PENDING] scenarios are no
 * longer reachable through the list — [ScenariosListScreen] filters them out — so this is a
 * defensive catch-all, not the normal path into a not-yet-built scenario.
 */
@Composable
private fun PendingPlaceholder(reason: String) {
    Text(
        text = "Not built on Android yet",
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = reason,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
