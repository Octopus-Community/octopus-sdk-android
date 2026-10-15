package com.octopuscommunity.sdk.sample.ui.scenarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ConnectionMode
import com.octopuscommunity.sdk.domain.model.OctopusCommunityData
import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.domain.model.SyncFollowGroupAction
import com.octopuscommunity.sdk.domain.model.TrackerEvent
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.sample.data.CommunitySessionTracker
import com.octopuscommunity.sdk.sample.data.SDKConfigResolver
import com.octopuscommunity.sdk.sample.data.SampleFixtures
import com.octopuscommunity.sdk.sample.data.datastore.SDKConfigDataStore
import com.octopuscommunity.sdk.sample.data.datastore.UserDataStore
import com.octopuscommunity.sdk.sample.data.describe
import com.octopuscommunity.sdk.sample.data.model.FeatureToggle
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.data.unifiedProfileActivation
import com.octopuscommunity.sdk.sample.data.usecase.CommunityConnectUseCase
import com.octopuscommunity.sdk.sample.data.usecase.DISCONNECT_FAILURE_MESSAGE
import com.octopuscommunity.sdk.sample.data.usecase.disconnectFromCommunity
import com.octopuscommunity.sdk.sample.ui.community.HostOverrides
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.OctopusCommunityConfigPanelSource
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.communityConfigPanelStates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

/**
 * The `customEvents` presets' values, in one place.
 *
 * Both the preset buttons and the scenario screen's Customize mode read them: a preset tap has to
 * restore *exactly* what the preset means, and a second copy of these strings in the UI layer is
 * how "Reset to preset" quietly stops resetting.
 */
internal const val DEFAULT_CUSTOM_EVENT_NAME = "sample_test_event"

/** Property value preset 2 sends; blank means "no property map at all" (see `trackCustomEvent`). */
internal const val DEFAULT_CUSTOM_EVENT_PROPERTY = "sample_value"

/** The single property key the sample event carries — the key itself is not customizable. */
internal const val DEFAULT_CUSTOM_EVENT_PROPERTY_KEY = "sample_prop"

class ScenariosViewModel(
    private val userDataStore: UserDataStore,
    private val communityConnectUseCase: CommunityConnectUseCase,
    private val configDataStore: SDKConfigDataStore,
    private val communitySessionTracker: CommunitySessionTracker,
) : ViewModel() {
    data class MethodState(
        val isLoading: Boolean = false,
        val result: String? = null,
        val isError: Boolean = false,
        /**
         * Wall-clock duration of the call that produced [result], in milliseconds.
         *
         * Measured centrally in [setMethodState] rather than at each of the ~30 call sites: it is
         * the interval between the `loading = true` transition and the terminal one, which is
         * exactly what an integrator watching a scenario wants to read, and no execute* method
         * has to remember to time itself.
         */
        val durationMs: Long? = null,
        /**
         * Monotonic rank of the write that produced this state, 0 for "never executed".
         *
         * A scenario screen shows the result of the *last* action the tester ran, and several
         * scenarios drive three or seven different method keys. A wall-clock timestamp would tie on
         * two calls landing in the same millisecond, so this is a plain counter: the highest rank
         * among a scenario's keys is the call to display.
         */
        val rank: Long = 0L,
    )

    /**
     * State of the `communityData` QA scenario (Unified Profile read API).
     *
     * Mirrors the Flutter example's Community Data scenario page: the last value seen from either
     * lookup or from the observation, where it came from, and the id learned from the last
     * successful lookup so the by-user-id preset needs no hand-typed id.
     */
    data class CommunityDataScenarioState(
        val data: OctopusCommunityData? = null,
        /** Where [data] came from — makes it obvious whether the value was pushed or pulled. */
        val source: String = "—",
        /** Octopus user id learned from the last successful lookup (preset 2 replays it). */
        val lastUserId: String? = null,
        val observing: Boolean = false,
        /**
         * Why the observation ended on its own, when it did — `null` after an explicit stop.
         *
         * `communityDataFlowByClientUserId` is not infinite: it emits `null` and **completes** when
         * the client-user-id lookup resolves no member. Without this, a dead collector would still
         * read `observing: yes` and preset 4 would claim to tear down something already over.
         */
        val observationEndedReason: String? = null,
        val flowError: String? = null,
        val isLoading: Boolean = false,
        val result: String? = null,
        val isError: Boolean = false,
    )

    data class State(
        val methods: Map<String, MethodState> = emptyMap(),
        // Live flow states
        val connectionState: ConnectionState = ConnectionState.NotConnected,
        val isUserConnected: Boolean = false,
        val groups: List<OctopusGroup> = emptyList(),
        val notSeenCount: Int = 0,
        val hasAccessToCommunity: Boolean = false,
        // Input fields
        val communityAccessInput: Boolean = true,
        val localeInput: String = "",
        // The `customEvents` scenario's two editable values. They live here rather than in the
        // composable so Customize mode survives a rotation the same way every other input on this
        // screen does — a value a tester typed and lost to a screen rotation reads as a bug in the
        // SDK, not in the sample.
        val customEventNameInput: String = DEFAULT_CUSTOM_EVENT_NAME,
        val customEventPropertyInput: String = "",
        val communityDataUserIdInput: String = SampleFixtures.userOther,
        val groupIdInput: String = SampleFixtures.topicDefault,
        val groupSearchQuery: String = "",
        val loadingGroupIds: Set<String> = emptySet(),
        val switchApiKeyInput: String = "",
        val currentApiKey: String = "",
        /**
         * The persisted sample config, so the scenario screens read the same source Debug
         * info's "Configuration" block and Settings read. Held here
         * rather than injected into the composable: three screens showing three snapshots of the
         * same config is exactly what the design-first pass set out to remove.
         */
        val sdkConfig: SDKConfig? = null,
        // API key currently being switched to (preset slot key or the trimmed custom input), or
        // null when no switch is in flight. Drives per-button loading: only the clicked
        // switch-community button shows the spinner, the others are disabled meanwhile.
        val switchingApiKey: String? = null,
        val isInitialized: Boolean = false,
        val syncOffset: SyncOffset = SyncOffset.Now,
        // Host-side login id (User.id from UserDataStore) for the currently stored user, if any.
        // Lets the Explorer prefill navigateToOctopusProfileByClientUserId with an id guaranteed
        // to resolve, without adding a dedicated ViewModel.
        val connectedClientUserId: String? = null,
        val communityDataScenario: CommunityDataScenarioState = CommunityDataScenarioState(),
    ) {
        /**
         * The host-side id to look a member up by: the id the SDK reports for the connected user
         * when available, else the locally stored host login id. `null` when neither is known
         * (guest / not connected) — the presets then report that instead of calling the API.
         */
        val communityDataLookupClientUserId: String?
            get() =
                connectionState.profile?.clientUserId?.takeUnless { it.isBlank() }
                    ?: connectedClientUserId?.takeUnless { it.isBlank() }

        /**
         * `groups` filtered by [groupSearchQuery] (case-insensitive substring match on the
         * group's `name`). Returns the full list when the query is blank. The inline list
         * + force-batch sync filters (canChangeFollowStatus) still apply against the full
         * `groups` list — search only narrows what's rendered to the human.
         */
        val filteredGroups: List<OctopusGroup>
            get() =
                if (groupSearchQuery.isBlank()) {
                    groups
                } else {
                    groups.filter { it.name.contains(groupSearchQuery, ignoreCase = true) }
                }
    }

    enum class SyncOffset(
        val offsetMillis: Long,
    ) {
        Past5Min(-5 * 60 * 1000L),
        Now(0L),
        Future5Min(+5 * 60 * 1000L),
    }

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    val unifiedProfileActive =
        unifiedProfileActivation(
            communityConfigPanelStates(OctopusCommunityConfigPanelSource),
            configDataStore.configFlow,
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var collectJob: Job? = null

    // Held here rather than in the composable: the scenario's observation must survive the section
    // being collapsed (AnimatedVisibility drops the content out of composition, which would kill a
    // section-scoped collector and silently stop observing behind the tester's back).
    //
    // Volatile because the identity guard in `startCommunityDataObservation` reads it from
    // `invokeOnCompletion`, which runs on whichever thread finalises the job. Every write happens on
    // the main thread today, so this only removes an unstated assumption.
    @Volatile
    private var communityDataObservationJob: Job? = null

    init {
        viewModelScope.launch {
            OctopusSDK.isInitialisedFlow.collect { initialized ->
                _state.update { it.copy(isInitialized = initialized) }
                if (initialized) {
                    startCollecting()
                } else {
                    stopCollecting()
                }
            }
        }
        // Surface the targeted community's key in the preview. Initial / Config-screen value comes
        // from the stored config; switchCommunity updates it (see executeSwitchCommunity) since the
        // runtime switch does not write back to configDataStore.
        viewModelScope.launch {
            configDataStore.configFlow.collect { config ->
                _state.update {
                    it.copy(
                        sdkConfig = config,
                        currentApiKey = config?.let(SDKConfigResolver::resolveApiKey).orEmpty(),
                    )
                }
            }
        }
        // Track the locally stored host user (Settings/Login) so the Explorer can prefill
        // navigateToOctopusProfileByClientUserId with an id guaranteed to resolve.
        viewModelScope.launch {
            userDataStore.userFlow.collect { user ->
                _state.update { it.copy(connectedClientUserId = user?.id) }
            }
        }
    }

    private fun startCollecting() {
        if (collectJob != null) return
        collectJob =
            viewModelScope.launch {
                launch {
                    OctopusSDK.connectionState.collect { state ->
                        _state.update { it.copy(connectionState = state) }
                    }
                }
                launch {
                    OctopusSDK.isUserConnected.collect { connected ->
                        _state.update { it.copy(isUserConnected = connected) }
                    }
                }
                launch {
                    OctopusSDK.groups.collect { groups ->
                        _state.update { it.copy(groups = groups) }
                    }
                }
                launch {
                    OctopusSDK.notSeenNotificationsCount.collect { count ->
                        _state.update { it.copy(notSeenCount = count) }
                    }
                }
                launch {
                    OctopusSDK.hasAccessToCommunity.collect { hasAccess ->
                        _state.update { it.copy(hasAccessToCommunity = hasAccess) }
                    }
                }
            }
    }

    private fun stopCollecting() {
        collectJob?.cancel()
        collectJob = null
        // The community-data observation is bound to the repository of the community that just went
        // away (stop / reset / switchCommunity), so it can never emit again — tear it down too and
        // report it as not observing instead of leaving a stale "yes" in the scenario panel.
        val hadLiveObservation = communityDataObservationJob?.isActive == true
        communityDataObservationJob?.cancel()
        communityDataObservationJob = null
        _state.update {
            it.copy(
                connectionState = ConnectionState.NotConnected,
                isUserConnected = false,
                groups = emptyList(),
                notSeenCount = 0,
                hasAccessToCommunity = false,
                communityDataScenario =
                    it.communityDataScenario.copy(observing = false, observationEndedReason = null),
            )
        }
        // The result panel is the assertion target, so it must not keep claiming an observation the
        // SDK lifecycle just killed from under the scenario.
        if (hadLiveObservation) {
            setCommunityDataScenarioResult(
                "The observation was torn down by the SDK lifecycle " +
                    "(stop / reset / switchCommunity).",
                isError = true,
            )
        }
    }

    // --- Input field setters ---

    fun setCommunityAccessInput(value: Boolean) {
        _state.update { it.copy(communityAccessInput = value) }
    }

    fun setLocaleInput(value: String) {
        _state.update { it.copy(localeInput = value) }
    }

    fun setCustomEventNameInput(value: String) {
        _state.update { it.copy(customEventNameInput = value) }
    }

    fun setCustomEventPropertyInput(value: String) {
        _state.update { it.copy(customEventPropertyInput = value) }
    }

    fun setCommunityDataUserId(value: String) {
        _state.update { it.copy(communityDataUserIdInput = value) }
    }

    // Read-only community-data API (Unified Profile P3): fetch a user's aggregate message count +
    // gamification standing, for a host to surface inside its own profile UI.
    fun executeFetchCommunityData() {
        viewModelScope.launch {
            val userId = _state.value.communityDataUserIdInput.trim()
            if (userId.isBlank()) {
                setMethodState("communityData", result = "Error: enter a user id", isError = true)
                return@launch
            }
            setMethodState("communityData", loading = true)
            try {
                when (val result = OctopusSDK.fetchCommunityDataResult(userId)) {
                    is OctopusResult.Success -> {
                        val data = result.data
                        setMethodState(
                            "communityData",
                            result =
                                if (data == null) {
                                    "null (no data for $userId)"
                                } else {
                                    describeCommunityData(data)
                                },
                        )
                    }

                    is OctopusResult.Failure ->
                        setMethodState("communityData", result = "Failure: ${result.describe()}", isError = true)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                setMethodState("communityData", result = "Error: ${e.message}", isError = true)
            }
        }
    }

    // --- `communityData` QA scenario (Unified Profile read API) ---
    // Single-tap presets over the whole read surface, so the QA pipeline drives it with the shared
    // ids from the shared scenario catalog. The free-text `executeFetchCommunityData` above stays
    // for exploring an arbitrary id, which no preset covers.

    private fun updateCommunityDataScenario(transform: (CommunityDataScenarioState) -> CommunityDataScenarioState) {
        _state.update { it.copy(communityDataScenario = transform(it.communityDataScenario)) }
    }

    // Deliberately does NOT touch `isLoading`: that flag gates the async presets, and only the
    // coroutine that raised it may clear it. Letting every result writer clear it let the
    // synchronous presets (4/5/6) re-enable the gated ones mid-fetch, whose result then landed on
    // top of theirs.
    private fun setCommunityDataScenarioResult(
        result: String,
        isError: Boolean = false,
    ) {
        updateCommunityDataScenario { it.copy(result = result, isError = isError) }
    }

    private fun recordCommunityData(
        data: OctopusCommunityData?,
        source: String,
    ) {
        updateCommunityDataScenario {
            it.copy(
                data = data,
                source = source,
                // Deliberately RETAINED when the new value is null: preset 2 replays this id, and a
                // null emission means "no member resolved", not "that id was wrong". Iso with the
                // Flutter reference, which keeps `_lastProfileId` across null values too.
                lastUserId = data?.userId ?: it.lastUserId,
            )
        }
    }

    private fun describeCommunityData(data: OctopusCommunityData): String {
        val gamification =
            data.gamification
                ?.let { "level=${it.level}, score=${it.score ?: "null"}" }
                ?: "null (gamification off)"
        return "userId=${data.userId}, messageCount=${data.messageCount ?: "null"}, " +
            "gamification=[$gamification]"
    }

    /** Preset 1 — one-shot lookup by the host's own id (the id kind a host actually has). */
    fun executeCommunityDataFetchByClientUserId() {
        val clientUserId = _state.value.communityDataLookupClientUserId
        if (clientUserId == null) {
            setCommunityDataScenarioResult(
                "No clientUserId known — connect an SSO user first (Settings → Account).",
                isError = true,
            )
            return
        }
        viewModelScope.launch {
            updateCommunityDataScenario { it.copy(isLoading = true) }
            try {
                when (val result = OctopusSDK.fetchCommunityDataByClientUserIdResult(clientUserId)) {
                    is OctopusResult.Success -> {
                        val data = result.data
                        recordCommunityData(data, "fetch(clientUserId)")
                        setCommunityDataScenarioResult(
                            if (data == null) {
                                "Unknown member for clientUserId \"$clientUserId\" (successful null)."
                            } else {
                                "Fetched: ${describeCommunityData(data)}"
                            },
                        )
                    }

                    is OctopusResult.Failure ->
                        setCommunityDataScenarioResult(
                            "fetchCommunityDataByClientUserIdResult failed: ${result.describe()}",
                            isError = true,
                        )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                setCommunityDataScenarioResult(
                    "fetchCommunityDataByClientUserIdResult failed: ${e.message ?: e}",
                    isError = true,
                )
            } finally {
                // In `finally` so a cancellation cannot leave the gated presets locked out for good
                // with the panel stuck on "loading…".
                updateCommunityDataScenario { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Preset 2 — one-shot lookup by the Octopus id, replayed from the last successful lookup so the
     * tester never has to paste an opaque id.
     */
    fun executeCommunityDataFetchByUserId() {
        val userId = _state.value.communityDataScenario.lastUserId
        if (userId == null) {
            setCommunityDataScenarioResult(
                "No userId known yet — run Preset 1 or 3 first.",
                isError = true,
            )
            return
        }
        viewModelScope.launch {
            updateCommunityDataScenario { it.copy(isLoading = true) }
            try {
                when (val result = OctopusSDK.fetchCommunityDataResult(userId)) {
                    is OctopusResult.Success -> {
                        val data = result.data
                        recordCommunityData(data, "fetch(userId)")
                        setCommunityDataScenarioResult(
                            if (data == null) {
                                "Unknown member for userId \"$userId\" (successful null)."
                            } else {
                                "Fetched: ${describeCommunityData(data)}"
                            },
                        )
                    }

                    is OctopusResult.Failure ->
                        setCommunityDataScenarioResult(
                            "fetchCommunityDataResult failed: ${result.describe()}",
                            isError = true,
                        )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                setCommunityDataScenarioResult(
                    "fetchCommunityDataResult failed: ${e.message ?: e}",
                    isError = true,
                )
            } finally {
                updateCommunityDataScenario { it.copy(isLoading = false) }
            }
        }
    }

    /** Preset 3 — start observing reactively, which makes the observation lifecycle observable. */
    fun startCommunityDataObservation() {
        val clientUserId = _state.value.communityDataLookupClientUserId
        if (clientUserId == null) {
            setCommunityDataScenarioResult(
                "No clientUserId known — connect an SSO user first (Settings → Account).",
                isError = true,
            )
            return
        }
        val previous = communityDataObservationJob
        updateCommunityDataScenario {
            it.copy(observing = true, observationEndedReason = null, flowError = null)
        }
        // Started LAZY so the job is registered and its completion handler attached BEFORE it can
        // run: viewModelScope dispatches on Main.immediate, so an eagerly-started body would race
        // the assignment below and a fast completion could fire against a null field.
        val job =
            viewModelScope.launch(start = CoroutineStart.LAZY) {
                // Await the previous collector's teardown before resubscribing, so a restart never
                // leaves two collectors briefly sharing the same underlying Room flow.
                previous?.cancelAndJoin()
                try {
                    OctopusSDK.communityDataFlowByClientUserId(clientUserId).collect { data ->
                        recordCommunityData(data, "flow(clientUserId)")
                    }
                    // Reached only when the flow ENDS by itself — the SDK swallows a failed
                    // client-user-id lookup into a single `null` emission and completes.
                    updateCommunityDataScenario {
                        it.copy(
                            observationEndedReason =
                                "flow completed — the clientUserId lookup resolved no member " +
                                    "(the community may not expose client user ids)",
                        )
                    }
                    // Also overwrite the RESULT panel, not just the live-state row: the catalog
                    // names `communityData-result` as the assertion target, so leaving the
                    // "Observing…" sentence there would keep grading preset 3 a pass over a
                    // collector that is already dead.
                    setCommunityDataScenarioResult(
                        "Observation ended on its own — the clientUserId lookup resolved no " +
                            "member (the community may not expose client user ids). Nothing is " +
                            "being observed any more.",
                        isError = true,
                    )
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (e: Exception) {
                    val message = e.message ?: e.toString()
                    updateCommunityDataScenario { it.copy(flowError = message) }
                    setCommunityDataScenarioResult(
                        "Observation failed: $message. Nothing is being observed any more.",
                        isError = true,
                    )
                }
            }
        communityDataObservationJob = job
        // `observing` follows the job's REAL lifetime, never a manual flag: that is what keeps
        // presets 3 and 4 honest when the flow ends on its own. The identity guard stops a stale
        // job's completion from clobbering a newer observation.
        job.invokeOnCompletion {
            if (communityDataObservationJob === job) {
                communityDataObservationJob = null
                updateCommunityDataScenario { it.copy(observing = false) }
            }
        }
        // Posted BEFORE start(): on Main.immediate a LAZY coroutine runs inline until its first real
        // suspension, so a repository that resolved without suspending would run the whole flow —
        // including the honest "observation ended" result — inside start(), and this line would then
        // overwrite it with a false success. Writing it first makes the ordering safe here instead of
        // resting on a suspension detail in another module.
        setCommunityDataScenarioResult(
            "Observing community data for clientUserId \"$clientUserId\". Run Preset 1 or act in " +
                "the community to see an emission.",
        )
        job.start()
    }

    /** Preset 4 — tear the observation down, so QA can see the collector actually stop. */
    fun stopCommunityDataObservation() {
        val job = communityDataObservationJob
        // A completed-but-not-yet-cleared job is NOT an active observation: reporting a teardown
        // here would be the false PASS this preset exists to rule out.
        val wasObserving = job != null && job.isActive
        val endedReason = _state.value.communityDataScenario.observationEndedReason
        job?.cancel()
        communityDataObservationJob = null
        // Reason cleared here too: kept, it would go on describing an observation two taps back.
        updateCommunityDataScenario { it.copy(observing = false, observationEndedReason = null) }
        setCommunityDataScenarioResult(
            when {
                wasObserving -> "Cancelled the collector — the observation is torn down."
                endedReason != null -> "Nothing to stop — the observation had already ended: $endedReason."
                else -> "Nothing was being observed."
            },
        )
    }

    /**
     * Preset 5 — the exactly-one-id contract.
     *
     * On Android there is nothing to exercise at runtime: the two lookups are four distinct
     * functions (`fetchCommunityDataResult(userId)` / `fetchCommunityDataByClientUserIdResult(clientUserId)`
     * and their Flow counterparts), so "both ids" / "neither id" cannot be expressed at all. The
     * Flutter wrapper collapses them into one function with optional named parameters, which is why
     * it enforces the contract at runtime with an `ArgumentError`. The preset reports that
     * difference rather than silently going missing — QA gets a deterministic answer either way.
     *
     * The message is deliberately plain prose with **no machine marker**. The catalog routes an
     * unavailable capability to the Cancelled bucket through a Tester-emitted `Not implemented: `
     * reason, but borrowing that string here would assert something false: the community-data
     * capability *is* implemented on Android — only Flutter's runtime both/neither check has no
     * equivalent. Whether the app should emit a marker at all is a catalog-owner call, raised as an
     * open question on the PR rather than guessed at here.
     */
    fun reportCommunityDataIdContract() {
        setCommunityDataScenarioResult(
            "Not applicable on Android: the exactly-one-id contract is a compile-time guarantee. " +
                "fetchCommunityDataResult(userId) and fetchCommunityDataByClientUserIdResult(clientUserId) are " +
                "distinct functions (same for the Flow pair), so passing both ids or neither does " +
                "not compile. The runtime ArgumentError exists only on the Flutter wrapper, whose " +
                "single function takes both ids as optional named parameters.",
        )
    }

    /**
     * Preset 6 — the navigation itself belongs to the host's nav graph, so the caller performs it
     * and this only records the hand-off. Returns whether there was an id to hand over: a missing
     * one must produce a stated reason, not a silent no-op reusing the previous preset's result.
     */
    fun onCommunityDataProfileOpened(): String? {
        val clientUserId = _state.value.communityDataLookupClientUserId
        if (clientUserId == null) {
            setCommunityDataScenarioResult(
                "No clientUserId known — connect an SSO user first (Settings → Account).",
                isError = true,
            )
            return null
        }
        setCommunityDataScenarioResult(
            "Pushed the host profile screen for \"$clientUserId\" — it fetches and renders the " +
                "community data in the host's own UI.",
        )
        return clientUserId
    }

    fun setGroupIdInput(value: String) {
        _state.update { it.copy(groupIdInput = value) }
    }

    fun setGroupSearchQuery(value: String) {
        _state.update { it.copy(groupSearchQuery = value) }
    }

    fun setSwitchApiKeyInput(value: String) {
        _state.update { it.copy(switchApiKeyInput = value) }
    }

    // --- Method state helpers ---

    /** Start instants of the in-flight calls, keyed the same way as [State.methods]. */
    private val methodStartedAt = mutableMapOf<String, Long>()

    /** Feeds [MethodState.rank]; never reset, so ordering holds for the process' lifetime. */
    private var methodRank = 0L

    private fun setMethodState(
        key: String,
        loading: Boolean = false,
        result: String? = null,
        isError: Boolean = false,
    ) {
        val now = System.currentTimeMillis()
        val duration =
            if (loading) {
                methodStartedAt[key] = now
                null
            } else {
                methodStartedAt.remove(key)?.let { now - it }
            }
        val rank = ++methodRank
        _state.update { current ->
            current.copy(
                methods = current.methods + (key to MethodState(loading, result, isError, duration, rank)),
            )
        }
    }

    fun getMethodState(key: String): MethodState = _state.value.methods[key] ?: MethodState()

    // --- Theme (`theme` scenario) ---

    // No SDK method call — the catalog's own api_symbols.android list is empty for this scenario
    // ("the theme is a composable/host parameter, not an OctopusSDK member"). Persists the same
    // SDKConfig.Theme field the Config screen's own theme picker writes; MainViewModel turns it
    // live into the CommunityTheme wrapper, no app restart needed.

    /**
     * Flip one [FeatureToggle] — the Scenarios section headers are the only place this is called
     * from, per the shared design contract ("a feature toggle lives in the section header, and
     * nowhere else"). It rewrites the stored config, so it takes effect the way a host would ship
     * the change rather than pretending to be a live switch.
     *
     * Enabling [FeatureToggle.PushRegistration] also registers the device token straight away, so
     * the flip has something to show: the toggle would otherwise only change what happens on the
     * *next* token refresh, which can be days away, and a switch whose effect is invisible is the
     * failure mode the contract calls a bug.
     */
    fun executeSetFeatureToggle(
        toggle: FeatureToggle,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            configDataStore.updateConfig { config ->
                when (toggle) {
                    FeatureToggle.ForceLogin ->
                        config?.copy(
                            authKind =
                                (config.authKind as? SDKConfig.AuthKind.SSO)
                                    ?.copy(forceLoginOnStrongActions = enabled)
                                    ?: config.authKind,
                        )

                    FeatureToggle.PushRegistration -> config?.copy(pushRegistrationEnabled = enabled)
                }
            }
            if (toggle == FeatureToggle.PushRegistration && enabled) {
                executeRegisterToken()
            }
        }
    }

    /** The scenario switch sets the flag override and host callback through one persisted config. */
    fun executeSetUnifiedProfile(enabled: Boolean) {
        viewModelScope.launch {
            configDataStore.updateConfig { it?.withUnifiedProfile(enabled) }
        }
    }

    fun executeSetTheme(theme: SDKConfig.Theme) {
        viewModelScope.launch {
            configDataStore.updateConfig { it?.copy(theme = theme) }
        }
    }

    // --- Connection methods ---

    fun executeConnectUser() {
        viewModelScope.launch {
            setMethodState("connectUser", loading = true)
            try {
                val user = userDataStore.getUser()
                if (user != null) {
                    val result = communityConnectUseCase(user)
                    when (result) {
                        is OctopusResult.Success ->
                            setMethodState(
                                "connectUser",
                                result = "Success: ${result.data}",
                            )

                        is OctopusResult.Failure ->
                            setMethodState(
                                "connectUser",
                                result = "Failure: ${result.describe()}",
                                isError = true,
                            )
                    }
                } else {
                    setMethodState(
                        "connectUser",
                        result = "No user stored. Login first via Settings.",
                        isError = true,
                    )
                }
            } catch (e: Exception) {
                setMethodState("connectUser", result = "Error: ${e.message}", isError = true)
            }
        }
    }

    fun executeDisconnectUser() {
        viewModelScope.launch {
            setMethodState("disconnectUser", loading = true)
            try {
                userDataStore
                    .disconnectFromCommunity()
                    .onSuccess { setMethodState("disconnectUser", result = "Success: User disconnected") }
                    .onFailure {
                        setMethodState(
                            "disconnectUser",
                            result = "Error: $DISCONNECT_FAILURE_MESSAGE ${it.message.orEmpty()}",
                            isError = true,
                        )
                    }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                setMethodState("disconnectUser", result = "Error: ${e.message}", isError = true)
            }
        }
    }

    // --- Group methods ---

    fun executeFetchGroups() {
        viewModelScope.launch {
            setMethodState("fetchGroups", loading = true)
            try {
                val result = OctopusSDK.fetchGroups()
                when (result) {
                    is OctopusResult.Success -> {
                        val groupNames =
                            result.data.joinToString("\n") {
                                val flag =
                                    when {
                                        !it.canChangeFollowStatus && it.isFollowed -> "ForceFollowed"
                                        !it.canChangeFollowStatus && !it.isFollowed -> "ForceNotFollowed"
                                        it.isFollowed -> "Followed"
                                        else -> "NotFollowed"
                                    }
                                "- ${it.name} (${it.id}) [$flag]"
                            }
                        setMethodState(
                            "fetchGroups",
                            result = "Found ${result.data.size} groups:\n$groupNames",
                        )
                    }

                    is OctopusResult.Failure ->
                        setMethodState(
                            "fetchGroups",
                            result = "Failure: ${result.describe()}",
                            isError = true,
                        )
                }
            } catch (e: Exception) {
                setMethodState("fetchGroups", result = "Error: ${e.message}", isError = true)
            }
        }
    }

    fun executeFollowGroup(groupId: String) {
        viewModelScope.launch {
            _state.update { it.copy(loadingGroupIds = it.loadingGroupIds + groupId) }
            try {
                val result = OctopusSDK.followGroup(groupId)
                when (result) {
                    is OctopusResult.Success ->
                        setMethodState(
                            "followGroup",
                            result = "Followed: $groupId",
                        )

                    is OctopusResult.Failure ->
                        setMethodState(
                            "followGroup",
                            result = "Failure: ${result.describe()}",
                            isError = true,
                        )
                }
            } catch (e: Exception) {
                setMethodState("followGroup", result = "Error: ${e.message}", isError = true)
            } finally {
                _state.update { it.copy(loadingGroupIds = it.loadingGroupIds - groupId) }
            }
        }
    }

    fun executeUnfollowGroup(groupId: String) {
        viewModelScope.launch {
            _state.update { it.copy(loadingGroupIds = it.loadingGroupIds + groupId) }
            try {
                val result = OctopusSDK.unfollowGroup(groupId)
                when (result) {
                    is OctopusResult.Success ->
                        setMethodState(
                            "unfollowGroup",
                            result = "Unfollowed: $groupId",
                        )

                    is OctopusResult.Failure ->
                        setMethodState(
                            "unfollowGroup",
                            result = "Failure: ${result.describe()}",
                            isError = true,
                        )
                }
            } catch (e: Exception) {
                setMethodState("unfollowGroup", result = "Error: ${e.message}", isError = true)
            } finally {
                _state.update { it.copy(loadingGroupIds = it.loadingGroupIds - groupId) }
            }
        }
    }

    fun setSyncOffset(offset: SyncOffset) {
        _state.update { it.copy(syncOffset = offset) }
    }

    /**
     * Builds a sync batch that inverts the follow state of every group the user currently
     * has control over (canChangeFollowStatus == true) and pushes it to the backend using
     * the currently selected [State.syncOffset] to shift the per-action `actionDate`.
     *
     * Exercises [SyncFollowGroupStatus.Applied] for both directions (FOLLOW + UNFOLLOW) and
     * [SyncFollowGroupStatus.Skipped] when the offset is in the past.
     */
    fun executeSyncFollowGroupsInvertAll() {
        runSyncFollowGroupsOn(_state.value.groups.filter { it.canChangeFollowStatus }) {
            !it.isFollowed
        }
    }

    /**
     * Builds a sync batch that sends `followed = true` for every group the user controls,
     * regardless of its current state. Lets QA exercise the
     * [SyncFollowGroupStatus.AlreadyFollowed] codepath, which the invert-all batch
     * never reaches because it only sends UNFOLLOW for already-followed groups (the
     * inverse-of-current rule never emits a FOLLOW for an already-followed group).
     */
    fun executeSyncFollowGroupsForceFollowAll() {
        runSyncFollowGroupsOn(_state.value.groups.filter { it.canChangeFollowStatus }) { true }
    }

    /**
     * Symmetric counterpart to [executeSyncFollowGroupsForceFollowAll]: sends
     * `followed = false` for every group the user controls, exercising
     * [SyncFollowGroupStatus.AlreadyUnfollowed] on groups that were not followed.
     */
    fun executeSyncFollowGroupsForceUnfollowAll() {
        runSyncFollowGroupsOn(_state.value.groups.filter { it.canChangeFollowStatus }) { false }
    }

    private fun runSyncFollowGroupsOn(
        groups: List<OctopusGroup>,
        followedFor: (OctopusGroup) -> Boolean,
    ) {
        if (groups.isEmpty()) {
            setMethodState(
                "syncFollowGroups",
                result = "No group with canChangeFollowStatus=true — nothing to sync.",
            )
            return
        }
        val actionDate = Date(System.currentTimeMillis() + _state.value.syncOffset.offsetMillis)
        val actions =
            groups.map {
                SyncFollowGroupAction(
                    groupId = it.id,
                    followed = followedFor(it),
                    actionDate = actionDate,
                )
            }
        runSyncFollowGroups(actions)
    }

    private fun runSyncFollowGroups(actions: List<SyncFollowGroupAction>) {
        viewModelScope.launch {
            setMethodState("syncFollowGroups", loading = true)
            try {
                when (val result = OctopusSDK.syncFollowGroups(actions)) {
                    is OctopusResult.Success -> {
                        val summary =
                            result.data.joinToString("\n") { r ->
                                "- ${r.groupId}: ${r.status::class.simpleName}"
                            }
                        setMethodState(
                            "syncFollowGroups",
                            result = "Sent ${actions.size} action(s):\n$summary",
                        )
                    }

                    is OctopusResult.Failure ->
                        setMethodState(
                            "syncFollowGroups",
                            result = "Failure: ${result.describe()}",
                            isError = true,
                        )
                }
            } catch (e: Exception) {
                setMethodState(
                    "syncFollowGroups",
                    result = "Error: ${e.message}",
                    isError = true,
                )
            }
        }
    }

    // --- Notification methods ---

    fun executeUpdateNotSeenCount() {
        viewModelScope.launch {
            setMethodState("updateNotSeen", loading = true)
            try {
                val result = OctopusSDK.updateNotSeenNotificationsCount()
                when (result) {
                    is OctopusResult.Success ->
                        setMethodState(
                            "updateNotSeen",
                            result = "Unseen count: ${result.data}",
                        )

                    is OctopusResult.Failure ->
                        setMethodState(
                            "updateNotSeen",
                            result = "Failure: ${result.describe()}",
                            isError = true,
                        )
                }
            } catch (e: Exception) {
                setMethodState("updateNotSeen", result = "Error: ${e.message}", isError = true)
            }
        }
    }

    /**
     * Hand the device's current FCM token to the SDK, if the `PushRegistration` feature toggle
     * allows it.
     *
     * Used to print a paragraph about `MessagingService` instead of calling anything, which made
     * the Notifications panel the one place in the former API Explorer where "run" ran nothing. It now
     * performs the same registration the messaging service performs on a token refresh, which is
     * what makes the toggle observable: flipping it on registers now, flipping it off leaves the
     * next refresh unregistered.
     *
     * Failure is reported, never thrown: no Firebase project on a build (or a device with no Play
     * services) is a normal state for a sample, and it must read as a result rather than a crash.
     */
    fun executeRegisterToken() {
        viewModelScope.launch {
            setMethodState("registerToken", loading = true)
            if (configDataStore.configFlow.first()?.pushRegistrationEnabled == false) {
                setMethodState(
                    "registerToken",
                    result =
                        "Skipped: push registration is off.\n" +
                            "Turn it on from the Notifications section on the Scenarios tab.",
                )
                return@launch
            }
            try {
                FirebaseMessaging
                    .getInstance()
                    .token
                    .addOnCompleteListener { task ->
                        val token = task.result
                        when {
                            !task.isSuccessful ->
                                setMethodState(
                                    "registerToken",
                                    result = "Failure: ${task.exception?.message ?: "no FCM token"}",
                                    isError = true,
                                )

                            token.isNullOrBlank() ->
                                setMethodState(
                                    "registerToken",
                                    result = "Failure: FCM returned an empty token",
                                    isError = true,
                                )

                            else -> {
                                OctopusSDK.registerNotificationsToken(token)
                                setMethodState(
                                    "registerToken",
                                    // Truncated on purpose: a device token is a credential-shaped
                                    // value, and the sample's own Debug console is copyable.
                                    result = "Success: registered token ${token.take(8)}…",
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                setMethodState("registerToken", result = "Error: ${e.message}", isError = true)
            }
        }
    }

    // --- Community Access ---

    /**
     * [methodKey] defaults to the `communityAccess` scenario's own slot ("overrideAccess"), but
     * `forceOctopusABTests` calls this with its own distinct key ("overrideAccessABTests"): both
     * scenarios call the same SDK method, but sharing one [MethodState] slot let a tap on either
     * card's button pre-fill the OTHER card's result line the next time it was opened. A distinct
     * key per caller keeps each card's result gated to results it actually produced — the same
     * split [executeTrackAccessToCommunity] already has. The shared badge
     * ([HostOverrides.communityAccessOverride]) stays shared on purpose: it mirrors a genuinely
     * process-wide SDK override, so both cards should agree on it. Only the result line needs its
     * own slot per card.
     */
    fun executeOverrideCommunityAccess(methodKey: String = "overrideAccess") {
        viewModelScope.launch {
            setMethodState(methodKey, loading = true)
            try {
                val hasAccess = _state.value.communityAccessInput
                val result = OctopusSDK.overrideCommunityAccess(hasAccess)
                when (result) {
                    is OctopusResult.Success -> {
                        // Tracked here, on confirmed success, rather than eagerly in the
                        // composable's onClick: the "override active" badge and the selected
                        // button must never claim a state the SDK call has not actually reached.
                        HostOverrides.trackCommunityAccessOverride(hasAccess)
                        setMethodState(
                            methodKey,
                            result = "Success: access=$hasAccess",
                        )
                    }

                    is OctopusResult.Failure ->
                        setMethodState(
                            methodKey,
                            result = "Failure: ${result.describe()}",
                            isError = true,
                        )
                }
            } catch (e: Exception) {
                setMethodState(methodKey, result = "Error: ${e.message}", isError = true)
            }
        }
    }

    /**
     * [methodKey] defaults to the `communityAccess` scenario's own slot ("trackAccess"), but
     * `trackABTests` calls this with its own distinct key ("trackABTestsAccess"): both scenarios
     * call the same SDK method, but sharing one [MethodState] slot let a tap on either card's
     * button pre-fill the OTHER card's result line the next time it was opened. A distinct key per
     * caller keeps each card's result gated to results it actually produced, matching how the two
     * scenarios were already given their own `result_test_id`.
     */
    fun executeTrackAccessToCommunity(methodKey: String = "trackAccess") {
        try {
            val hasAccess = _state.value.communityAccessInput
            OctopusSDK.trackAccessToCommunity(hasAccess)
            setMethodState(methodKey, result = "Success: tracked access=$hasAccess")
        } catch (e: Exception) {
            setMethodState(methodKey, result = "Error: ${e.message}", isError = true)
        }
    }

    // --- Tracking ---

    /**
     * The `customEvents` scenario's Run: the same call, built from
     * [State.customEventNameInput] / [State.customEventPropertyInput].
     *
     * Its preset buttons write those two inputs before calling this, so a preset tap sends exactly
     * what it did before Customize existed; only a human editing a field makes them differ.
     */
    fun executeTrackCustomEventFromInputs() {
        trackCustomEvent(
            name = _state.value.customEventNameInput,
            propertyValue = _state.value.customEventPropertyInput,
        )
    }

    /**
     * A blank [propertyValue] is the "no properties" shape, not an empty property: an event whose
     * only property carries an empty string is a different (and misleading) payload from one with
     * no property map at all, and the sample is where an integrator learns which is which.
     */
    private fun trackCustomEvent(
        name: String,
        propertyValue: String,
    ) {
        viewModelScope.launch {
            setMethodState("trackCustom", loading = true)
            try {
                val eventName = name.ifBlank { DEFAULT_CUSTOM_EVENT_NAME }
                val withProperties = propertyValue.isNotBlank()
                val event =
                    if (withProperties) {
                        TrackerEvent.Custom(
                            name = eventName,
                            properties =
                                mapOf(
                                    DEFAULT_CUSTOM_EVENT_PROPERTY_KEY to
                                        TrackerEvent.Custom.Property(value = propertyValue),
                                ),
                        )
                    } else {
                        TrackerEvent.Custom(name = eventName)
                    }
                OctopusSDK.track(event)
                setMethodState(
                    "trackCustom",
                    result =
                        if (withProperties) {
                            "Success: Custom event \"$eventName\" tracked (with properties)"
                        } else {
                            "Success: Custom event \"$eventName\" tracked"
                        },
                )
            } catch (e: Exception) {
                setMethodState("trackCustom", result = "Error: ${e.message}", isError = true)
            }
        }
    }

    // --- Locale ---

    fun executeOverrideLocale() {
        viewModelScope.launch {
            setMethodState("overrideLocale", loading = true)
            try {
                val localeStr = _state.value.localeInput
                val locale = if (localeStr.isBlank()) null else java.util.Locale(localeStr)
                OctopusSDK.overrideDefaultLocale(locale)
                // Tracked here, after the suspend call actually returns, rather than eagerly in
                // the composable's onClick — see executeOverrideCommunityAccess() above for why.
                HostOverrides.trackLocaleOverride(localeStr.ifBlank { null })
                setMethodState(
                    "overrideLocale",
                    result = "Success: locale=${locale ?: "system default"}",
                )
            } catch (e: Exception) {
                setMethodState("overrideLocale", result = "Error: ${e.message}", isError = true)
            }
        }
    }

    // --- Lifecycle ---

    fun executeReset() {
        viewModelScope.launch {
            setMethodState("reset", loading = true)
            try {
                OctopusSDK.reset()
                setMethodState("reset", result = "Success: SDK reset complete")
            } catch (e: Exception) {
                setMethodState("reset", result = "Error: ${e.message}", isError = true)
            }
        }
    }

    fun executeStop() {
        viewModelScope.launch {
            setMethodState("stop", loading = true)
            try {
                OctopusSDK.stop()
                setMethodState("stop", result = "Success: SDK stopped")
            } catch (e: Exception) {
                setMethodState("stop", result = "Error: ${e.message}", isError = true)
            }
        }
    }

    // --- Switch Community ---

    /**
     * Re-targets the SDK at a different community, using the free-text API-key field
     * (`switchApiKeyInput`).
     */
    fun executeSwitchCommunity(context: android.content.Context) {
        viewModelScope.launch {
            val apiKey = _state.value.switchApiKeyInput.trim()
            if (apiKey.isBlank()) {
                setMethodState(
                    "switchCommunity",
                    result = "Error: API key cannot be empty",
                    isError = true,
                )
                return@launch
            }
            setMethodState("switchCommunity", loading = true)
            _state.update { it.copy(switchingApiKey = apiKey) }
            try {
                // Use the current config's connection mode, fallback to SSO
                val config = configDataStore.configFlow.first()
                val connectionMode =
                    when (val authKind = config?.authKind) {
                        is com.octopuscommunity.sdk.sample.data.model.SDKConfig.AuthKind.SSO ->
                            ConnectionMode.SSO(appManagedFields = authKind.appManagedFields.toSet())

                        null -> ConnectionMode.SSO()
                    }
                OctopusSDK.switchCommunity(
                    context = context,
                    apiKey = apiKey,
                    connectionMode = connectionMode,
                )
                _state.update { it.copy(currentApiKey = apiKey) }
                // After the call, never before: the host surfaces that rebuild themselves on this
                // signal — the Community tab and its embedded feed (internal change) — have to land on the
                // container `switchCommunity` has just published, not on the one it tore down.
                communitySessionTracker.onCommunityTargeted()
                setMethodState(
                    "switchCommunity",
                    // Never echo any part of the API key back into a UI-displayed string — this
                    // result can end up in a screenshot or a QA report.
                    result = "Success: Switched community",
                )
            } catch (e: Exception) {
                setMethodState(
                    "switchCommunity",
                    result = "Error: ${e.message}",
                    isError = true,
                )
            } finally {
                _state.update { it.copy(switchingApiKey = null) }
            }
        }
    }
}
