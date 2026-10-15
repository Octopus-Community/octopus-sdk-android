package com.octopuscommunity.sdk.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.OctopusEvent
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.sample.data.AppUpdateChecker
import com.octopuscommunity.sdk.sample.data.SDKTestFakesLauncher
import com.octopuscommunity.sdk.sample.data.SDKTestFakesRequest
import com.octopuscommunity.sdk.sample.data.datastore.SDKConfigDataStore
import com.octopuscommunity.sdk.sample.data.model.DebugEntry
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    configDataStore: SDKConfigDataStore,
    private val appUpdateChecker: AppUpdateChecker,
    sdkTestFakesRequests: StateFlow<SDKTestFakesRequest?> = SDKTestFakesLauncher.request,
    // Seams for MainViewModelTest: the stored configuration and the SDK's readiness, which a JVM
    // test cannot drive through a real DataStore read and a real initialize() in virtual time.
    configFlow: Flow<SDKConfig?> = configDataStore.configFlow,
    isSdkInitializedFlow: Flow<Boolean> = OctopusSDK.isInitialisedFlow,
) : ViewModel() {
    companion object {
        /**
         * Hard cap on the persistent typed-event buffer backing the Events Log
         * screen (FIFO trim, oldest dropped). Demo-only — keeps the list (and
         * the copy-on-write state updates) bounded on long sessions. Mirrors
         * the Flutter sample's `RealDebugLog.maxTypedEvents` and the iOS
         * sample's persistent `EventsViewModel`.
         */
        const val MAX_SDK_EVENTS = 50

        /**
         * Hard cap on the merged event/state/API debug feed backing the Debug
         * Console sheet (FIFO trim, oldest dropped). Bounds memory and the
         * console's rendering cost on long sessions.
         */
        const val MAX_DEBUG_ENTRIES = 200

        /**
         * Upper bound on how long a cold start waits for the stored configuration to bring the SDK
         * up before it shows the first screen anyway (an initialization that never reports ready
         * must not leave the app on its splash screen).
         */
        const val START_RESOLUTION_TIMEOUT_MS = 2_000L
    }

    data class State(
        val sdkConfig: SDKConfig? = null,
        val isSdkInitialized: Boolean = false,
        val connectionState: ConnectionState = ConnectionState.NotConnected,
        val sdkEvents: List<OctopusEvent> = emptyList(),
        val debugLog: List<DebugEntry> = emptyList(),
        val appUpdateStatus: AppUpdateChecker.Status = AppUpdateChecker.Status.Unknown,
        val lastNotifiedUpdateVersionCode: Int? = null,
        val isConfigLoaded: Boolean = false,
        val isStartResolved: Boolean = false,
    ) {
        val theme: SDKConfig.Theme get() = sdkConfig?.theme ?: SDKConfig.Theme.Default
        val appearance: SDKConfig.Appearance
            get() = sdkConfig?.appearance ?: SDKConfig.Appearance.System
        val serverEnvironment: SDKConfig.ServerEnvironment
            get() = sdkConfig?.serverEnvironment ?: SDKConfig.ServerEnvironment.Default
        val hostCallbacks: SDKConfig.HostCallbacks
            get() = sdkConfig?.effectiveHostCallbacks ?: SDKConfig.HostCallbacks()
        val isAppUpdateAvailable: Boolean
            get() = appUpdateStatus is AppUpdateChecker.Status.UpdateAvailable
    }

    private val _state = MutableStateFlow(State())

    /**
     * Latches [State.isStartResolved] once the first screen is known: Config for a fresh install
     * (no stored configuration), Main once a stored configuration has brought the SDK up. Never
     * cleared afterwards, so a later reset goes back to Config through the usual navigation.
     */
    private fun State.resolvingStart(): State =
        if (!isStartResolved && isConfigLoaded && (sdkConfig == null || isSdkInitialized)) {
            copy(isStartResolved = true)
        } else {
            this
        }

    val state = _state.asStateFlow()

    private var eventsJob: Job? = null
    private var connectionJob: Job? = null

    init {
        viewModelScope.launch {
            delay(START_RESOLUTION_TIMEOUT_MS)
            _state.update { it.copy(isStartResolved = true) }
        }
        viewModelScope.launch {
            combine(configFlow, sdkTestFakesRequests) { config, fakes ->
                // A fresh install can enter fake mode without creating a persisted backend selection.
                config ?: fakes?.let {
                    SDKConfig(authKind = SDKConfig.AuthKind.SSO(forceLoginOnStrongActions = true))
                }
            }.collectLatest { config ->
                _state.update { it.copy(sdkConfig = config, isConfigLoaded = true).resolvingStart() }
            }
        }
        viewModelScope.launch {
            isSdkInitializedFlow.collectLatest { initialized ->
                _state.update { it.copy(isSdkInitialized = initialized).resolvingStart() }
                if (initialized) {
                    addDebugEntry(
                        DebugEntry.Category.STATE,
                        "SDK initialized",
                    )
                    startCollecting()
                } else {
                    addDebugEntry(
                        DebugEntry.Category.STATE,
                        "SDK not initialized",
                    )
                    stopCollecting()
                }
            }
        }
    }

    private fun startCollecting() {
        if (eventsJob != null) return

        eventsJob =
            viewModelScope.launch {
                OctopusSDK.events.collect { event ->
                    _state.update {
                        it.copy(sdkEvents = (it.sdkEvents + event).takeLast(MAX_SDK_EVENTS))
                    }
                    addDebugEntry(
                        category = DebugEntry.Category.EVENT,
                        summary = event::class.simpleName ?: "Unknown Event",
                        details = event.toString(),
                    )
                }
            }
        connectionJob =
            viewModelScope.launch {
                OctopusSDK.connectionState.collect { connState ->
                    val previousState = _state.value.connectionState
                    _state.update { it.copy(connectionState = connState) }
                    if (previousState != connState) {
                        addDebugEntry(
                            category = DebugEntry.Category.STATE,
                            summary = "Connection state changed",
                            details =
                                when (connState) {
                                    is ConnectionState.Connected ->
                                        if (connState.isGuest) {
                                            "Connected (Guest)"
                                        } else {
                                            "Connected (User: ${connState.profile.nickname ?: connState.profile.userId})"
                                        }
                                    ConnectionState.NotConnected -> "Not Connected"
                                },
                        )
                    }
                }
            }
    }

    private fun stopCollecting() {
        eventsJob?.cancel()
        eventsJob = null
        connectionJob?.cancel()
        connectionJob = null
        _state.update {
            it.copy(
                connectionState = ConnectionState.NotConnected,
                sdkEvents = emptyList(),
            )
        }
    }

    fun addDebugEntry(
        category: DebugEntry.Category,
        summary: String,
        details: String? = null,
    ) {
        _state.update { current ->
            current.copy(
                debugLog =
                    (
                        current.debugLog +
                            DebugEntry(
                                category = category,
                                summary = summary,
                                details = details,
                            )
                    ).takeLast(MAX_DEBUG_ENTRIES),
            )
        }
    }

    fun clearEvents() {
        _state.update { it.copy(sdkEvents = emptyList()) }
    }

    fun clearDebugLog() {
        _state.update { it.copy(debugLog = emptyList()) }
    }

    fun checkForAppUpdate() {
        viewModelScope.launch {
            val status = appUpdateChecker.check()
            _state.update { it.copy(appUpdateStatus = status) }
        }
    }

    /**
     * Returns the available versionCode if a NEW update needs to be notified,
     * i.e. the snackbar was not already shown for this exact version. Also records it.
     */
    fun acknowledgeUpdateNotificationIfNew(): Int? {
        val status =
            _state.value.appUpdateStatus as? AppUpdateChecker.Status.UpdateAvailable
                ?: return null
        val availableVersion = status.info.availableVersionCode()
        if (_state.value.lastNotifiedUpdateVersionCode == availableVersion) return null
        _state.update { it.copy(lastNotifiedUpdateVersionCode = availableVersion) }
        return availableVersion
    }

    val appUpdateManager get() = appUpdateChecker.manager
}
