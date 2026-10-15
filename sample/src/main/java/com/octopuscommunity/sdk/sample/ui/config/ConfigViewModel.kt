package com.octopuscommunity.sdk.sample.ui.config

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.sample.data.AuthPreset
import com.octopuscommunity.sdk.sample.data.SDKTestFakesLauncher
import com.octopuscommunity.sdk.sample.data.SDKTestFakesRequest
import com.octopuscommunity.sdk.sample.data.SampleConfigSummary
import com.octopuscommunity.sdk.sample.data.datastore.SDKConfigDataStore
import com.octopuscommunity.sdk.sample.data.datastore.UserDataStore
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.data.model.User
import com.octopuscommunity.sdk.sample.data.resolveAuthPreset
import com.octopuscommunity.sdk.sample.data.unifiedProfileActivation
import com.octopuscommunity.sdk.sample.data.usecase.CommunityConnectUseCase
import com.octopuscommunity.sdk.sample.extensions.LocalExtensions
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.OctopusCommunityConfigPanelSource
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.communityConfigPanelStates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class ConfigViewModel(
    private val configDataStore: SDKConfigDataStore,
    private val userDataStore: UserDataStore,
    private val communityConnectUseCase: CommunityConnectUseCase,
    /**
     * Application-lifetime scope for Apply: Apply navigates away at once, which clears this
     * ViewModel and would cancel a connect still on the network in [viewModelScope].
     */
    private val appScope: CoroutineScope,
    private val sdkTestFakesRequests: StateFlow<SDKTestFakesRequest?> = SDKTestFakesLauncher.request,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
    // In-memory storage flows let draft merge tests run entirely on the test scheduler.
    configFlow: Flow<SDKConfig?> = configDataStore.configFlow,
    userFlow: Flow<User?> = userDataStore.userFlow,
) : ViewModel() {
    @Serializable
    data class State(
        val useCustomCommunity: Boolean = false,
        val customApiKey: String = "",
        val forceLogin: Boolean = false,
        val managedFields: Set<ProfileField> = emptySet(),
        /** Theme preset handed to the SDK on "Start SDK" — Octopus navy by default. */
        val theme: SDKConfig.Theme = SDKConfig.Theme.Default,
        /** Light/dark choice, the Segmented control the DS draws above the theme preset. */
        val appearance: SDKConfig.Appearance = SDKConfig.Appearance.System,
        /**
         * SSO user picked from the DS chips (`pickerUserIds`), applied on Apply. Null until the
         * stored user is read, and stays null when none is stored and no chip was tapped — Apply
         * then leaves the user alone, as before.
         */
        val ssoUserId: String? = null,
        val serverEnvKind: ServerEnvKind = ServerEnvKind.DEFAULT,
        val customServerHost: String = "",
        val customServerPort: String = "443",
        val localConfiguration: JsonObject? = null,
    )

    enum class ServerEnvKind { DEFAULT, PROD, CUSTOM, LOCAL }

    val localOptions = LocalExtensions.configOptions(viewModelScope)

    private val savedDraft = savedStateHandle.get<Bundle>("configDraft")
    private val restoredDraft = savedDraft?.getString("state")?.let { Json.decodeFromString<State>(it) }
    private var previousStoredState = savedDraft?.getString("baseline")?.let { Json.decodeFromString<State>(it) }
    private val _state = MutableStateFlow(restoredDraft ?: State())
    private var hasHydrated = restoredDraft != null
    val state = _state.asStateFlow()

    /**
     * The stored config, kept warm so [saveAndStart] can say what Apply is about to do the moment it
     * is tapped — the write itself stays asynchronous. Null until the first read, and when nothing
     * is stored yet (onboarding).
     */
    private val persistedConfig: StateFlow<SDKConfig?> =
        configFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Effective activation, including the backend flag override and the current host wiring. */
    val unifiedProfileActive: StateFlow<Boolean> =
        unifiedProfileActivation(communityConfigPanelStates(OctopusCommunityConfigPanelSource), persistedConfig)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** The stored SSO user id read at hydration, to tell a user switch apart on Apply. */
    private var storedUserId: String? = null

    init {
        // Only Activity saved state: editing never writes DataStore or changes the running SDK.
        // Capture synchronously at save time, including the last keystroke before destruction.
        savedStateHandle.setSavedStateProvider("configDraft") {
            // An untouched placeholder saved before the first read is not a user draft.
            if (!hasHydrated && _state.value == State()) {
                Bundle()
            } else {
                val baseline = previousStoredState ?: if (restoredDraft == null) State() else null
                bundleOf(
                    "state" to Json.encodeToString(State.serializer(), _state.value),
                    "baseline" to baseline?.let { Json.encodeToString(State.serializer(), it) },
                )
            }
        }
        viewModelScope.launch {
            storedUserId = userFlow.first()?.id
            if (restoredDraft?.localConfiguration != null) localOptions.reload()
            configFlow.collect { config ->
                val next = config?.toEditableState() ?: State()
                // Keep the saved storage baseline: the first emission may already contain changes
                // to untouched fields. Older saved drafts have no baseline; preserve their behavior.
                val baseline = previousStoredState ?: if (restoredDraft != null) next else State()
                _state.update { current ->
                    // A field still matching the previous snapshot follows live storage. A draft
                    // wins until Apply, including the auth shape selected by a named-key preset.
                    current.copy(
                        useCustomCommunity =
                            if (current.useCustomCommunity ==
                                baseline.useCustomCommunity
                            ) {
                                next.useCustomCommunity
                            } else {
                                current.useCustomCommunity
                            },
                        customApiKey = if (current.customApiKey == baseline.customApiKey) next.customApiKey else current.customApiKey,
                        forceLogin = if (current.forceLogin == baseline.forceLogin) next.forceLogin else current.forceLogin,
                        managedFields = if (current.managedFields == baseline.managedFields) next.managedFields else current.managedFields,
                        theme = if (current.theme == baseline.theme) next.theme else current.theme,
                        appearance = if (current.appearance == baseline.appearance) next.appearance else current.appearance,
                        ssoUserId = current.ssoUserId ?: storedUserId,
                        serverEnvKind = if (current.serverEnvKind == baseline.serverEnvKind) next.serverEnvKind else current.serverEnvKind,
                        localConfiguration =
                            if (current.localConfiguration ==
                                baseline.localConfiguration
                            ) {
                                next.localConfiguration
                            } else {
                                current.localConfiguration
                            },
                        customServerHost =
                            if (current.customServerHost ==
                                baseline.customServerHost
                            ) {
                                next.customServerHost
                            } else {
                                current.customServerHost
                            },
                        customServerPort =
                            if (current.customServerPort ==
                                baseline.customServerPort
                            ) {
                                next.customServerPort
                            } else {
                                current.customServerPort
                            },
                    )
                }
                if (next.localConfiguration != null && next.localConfiguration != baseline.localConfiguration) localOptions.reload()
                previousStoredState = next
                hasHydrated = true
            }
        }
    }

    fun setUseCustomCommunity(value: Boolean) {
        _state.update { it.copy(useCustomCommunity = value) }
    }

    /**
     * Applies a named-key preset from the Config screen picker: switches off
     * the custom key and applies the preset's auth shape, from which
     * `SDKConfigResolver` derives the key.
     *
     * The hand-tuned managed-fields subset is kept when it already resolves
     * to this preset's slot (e.g. re-selecting "some managed fields" must not
     * clobber a QA-chosen subset); otherwise the preset's representative
     * shape is applied. Saving preserves the scenario's activation; choosing managed fields
     * does not implicitly wire the profile callback.
     */
    fun applyAuthPreset(preset: AuthPreset) {
        _state.update { current ->
            val managedFields =
                if (resolveAuthPreset(preset.forceLogin, current.managedFields).id == preset.id) {
                    current.managedFields
                } else {
                    preset.managedFields
                }
            current.copy(
                useCustomCommunity = false,
                forceLogin = preset.forceLogin,
                managedFields = managedFields,
            )
        }
    }

    fun setCustomApiKey(value: String) {
        _state.update { it.copy(customApiKey = value) }
    }

    fun toggleManagedField(field: ProfileField) {
        _state.update { current ->
            val newFields = current.managedFields.toMutableSet()
            if (newFields.contains(field)) newFields.remove(field) else newFields.add(field)
            current.copy(managedFields = newFields)
        }
    }

    /**
     * Theme preset. `navy` keeps a persisted `Custom` demo theme (it already renders the navy
     * palette) rather than silently dropping its fonts/icons flags.
     */
    fun setThemePreset(navy: Boolean) {
        _state.update {
            it.copy(
                theme =
                    when {
                        !navy -> SDKConfig.Theme.SdkDefault
                        it.theme is SDKConfig.Theme.SdkDefault -> SDKConfig.Theme.Default
                        else -> it.theme
                    },
            )
        }
    }

    fun setSsoUserId(id: String) {
        _state.update { it.copy(ssoUserId = id) }
    }

    fun setAppearance(appearance: SDKConfig.Appearance) {
        _state.update { it.copy(appearance = appearance) }
    }

    fun selectLocalConfiguration(selection: JsonObject?) {
        _state.update { it.copy(localConfiguration = selection) }
    }

    fun setServerEnvKind(value: ServerEnvKind) {
        _state.update { it.copy(serverEnvKind = value) }
    }

    fun setCustomServerHost(value: String) {
        _state.update { it.copy(customServerHost = value) }
    }

    fun setCustomServerPort(value: String) {
        _state.update { it.copy(customServerPort = value) }
    }

    private fun SDKConfig.toEditableState(): State {
        val sso = authKind as? SDKConfig.AuthKind.SSO
        val customEnv = serverEnvironment as? SDKConfig.ServerEnvironment.Custom
        return State(
            useCustomCommunity = customCommunity,
            customApiKey = customApiKey ?: "",
            forceLogin = sso?.forceLoginOnStrongActions == true,
            managedFields = sso?.appManagedFields?.toSet() ?: emptySet(),
            theme = theme,
            appearance = appearance,
            serverEnvKind =
                when {
                    localConfiguration != null -> ServerEnvKind.LOCAL
                    serverEnvironment == SDKConfig.ServerEnvironment.Prod -> ServerEnvKind.PROD
                    customEnv != null -> ServerEnvKind.CUSTOM
                    else -> ServerEnvKind.DEFAULT
                },
            localConfiguration = localConfiguration,
            customServerHost = customEnv?.host ?: "",
            customServerPort = customEnv?.port?.toString() ?: "443",
        )
    }

    /**
     * Persists the edited config (the SDK then re-inits from `SampleApplication` when its init key
     * changed) and returns the one-line outcome the screen shows as feedback — "restarting on …",
     * "no restart needed" or "nothing changed" — so Apply never looks like it did nothing.
     */
    fun saveAndStart(): String {
        val current = _state.value
        val authKind =
            SDKConfig.AuthKind.SSO(
                appManagedFields = current.managedFields.toList(),
                forceLoginOnStrongActions = current.forceLogin,
            )
        // In LOCAL mode the env carries the endpoint, so the server setting stays Default:
        // two sources for one endpoint would only diverge.
        val serverEnv =
            when (current.serverEnvKind) {
                ServerEnvKind.LOCAL -> SDKConfig.ServerEnvironment.Default
                ServerEnvKind.DEFAULT -> SDKConfig.ServerEnvironment.Default
                ServerEnvKind.PROD -> SDKConfig.ServerEnvironment.Prod
                ServerEnvKind.CUSTOM ->
                    SDKConfig.ServerEnvironment.Custom(
                        host = current.customServerHost.trim(),
                        port = current.customServerPort.trim().toIntOrNull() ?: 443,
                    )
            }

        fun nextConfig(persisted: SDKConfig?): SDKConfig =
            // Amend the stored config rather than rebuild it: this screen owns the fields below and
            // nothing else. Host app integration and push registration are set on their own
            // screens. Auth presets preserve Unified Profile activation; Apply otherwise keeps their
            // choices. Appearance is set here AND on Settings → Appearance; last write wins.
            (persisted ?: SDKConfig(authKind = authKind)).withAuthKind(authKind).copy(
                customCommunity = current.useCustomCommunity,
                customApiKey = current.customApiKey.ifBlank { null },
                theme = current.theme,
                appearance = current.appearance,
                serverEnvironment = serverEnv,
                // Only kept in LOCAL mode: leaving that mode means the developer chose
                // another endpoint, and keeping the env would silently override it.
                localConfiguration =
                    if (current.serverEnvKind == ServerEnvKind.LOCAL) {
                        current.localConfiguration ?: persisted?.localConfiguration
                    } else {
                        null
                    },
            )
        val persistedNow = persistedConfig.value
        val userChanged = current.ssoUserId != null && current.ssoUserId != storedUserId
        val outcome =
            applyOutcomeMessage(
                persisted = persistedNow,
                next = nextConfig(persistedNow),
                userChanged = userChanged,
                testFakesActive = sdkTestFakesRequests.value != null,
            )
        appScope.launch {
            // A different SSO id is a different person: start from a fresh User rather than carry
            // the previous one's nickname, avatar and entitlements over to it. Persisted BEFORE the
            // config, so a re-init triggered by the config write reconnects the new user.
            val picked = current.ssoUserId
            val newUser = if (picked != null && picked != userDataStore.getUser()?.id) User(id = picked) else null
            newUser?.let { user -> userDataStore.updateUser(clearSignOut = true) { user } }
            var reinitialises = true
            configDataStore.updateConfig { persisted ->
                val next = nextConfig(persisted)
                reinitialises = persisted?.sdkInitKey != next.sdkInitKey
                next
            }
            // A user-only change leaves the re-init key as it was, so SampleApplication will not
            // reconnect anyone: connect the picked user here, as Settings → Account does.
            if (newUser != null && !reinitialises && sdkTestFakesRequests.value == null && OctopusSDK.isInitialised) {
                communityConnectUseCase(newUser)
            }
        }
        return outcome
    }
}

/**
 * What Apply / Start SDK is about to do, in one line for the snackbar: whether the SDK restarts
 * (the re-init key changed, see [SDKConfig.sdkInitKey]), only takes the new settings (theme,
 * appearance, user), or has nothing to take. [persisted] is null on onboarding. While SDK test
 * fakes run ([testFakesActive]) a re-init key change is only saved: `SampleApplication` keeps the
 * synthetic session and does not re-init, so no restart is announced.
 */
internal fun applyOutcomeMessage(
    persisted: SDKConfig?,
    next: SDKConfig,
    userChanged: Boolean,
    testFakesActive: Boolean = false,
): String {
    val target = SampleConfigSummary.apiKeySource(next)
    return when {
        persisted == null -> "Starting the SDK \u00b7 $target"
        persisted.sdkInitKey != next.sdkInitKey && testFakesActive ->
            "Saved \u00b7 SDK test fakes stay active, $target applies once the app restarts"
        persisted.sdkInitKey != next.sdkInitKey -> "Applied \u00b7 restarting the SDK on $target"
        userChanged && testFakesActive -> "Saved \u00b7 SDK test fakes stay active; the host user applies once the app restarts"
        userChanged -> "Applied \u00b7 connecting the new user, no SDK restart needed"
        persisted != next -> "Applied \u00b7 no SDK restart needed"
        else -> "Nothing to apply \u00b7 the configuration is unchanged"
    }
}
