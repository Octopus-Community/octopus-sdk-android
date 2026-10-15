package com.octopuscommunity.sdk.sample.ui.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.sample.data.SDKConfigResolver
import com.octopuscommunity.sdk.sample.data.SampleConfigSummary
import com.octopuscommunity.sdk.sample.data.datastore.SDKConfigDataStore
import com.octopuscommunity.sdk.sample.data.datastore.UserDataStore
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val configDataStore: SDKConfigDataStore,
    private val userDataStore: UserDataStore,
    private val preferences: SharedPreferences,
) : ViewModel() {
    data class State(
        val sdkConfig: SDKConfig? = null,
        val effectiveApiKey: String = "",
        val connectionState: ConnectionState = ConnectionState.NotConnected,
        val isInitialized: Boolean = false,
        val user: User? = null,
        val localeOverride: String? = null,
        /** `OctopusSDK.notSeenNotificationsCount`, shown by Debug info and the Community tab badge. */
        val notSeenNotifications: Int = 0,
        /** `OctopusSDK.hasAccessToCommunity`, the live answer of the community-access gate. */
        val hasAccessToCommunity: Boolean = false,
    ) {
        val isConnected: Boolean
            get() = (connectionState as? ConnectionState.Connected)?.isGuest == false

        // The Settings summary shows one current value per row and no control at all, so every
        // subtitle is a read of the same state the screens behind them own — never a description
        // of what the row does.
        val accountSubtitle: String get() = SampleConfigSummary.accountSubtitle(user, isConnected)

        val appearanceSubtitle: String get() = SampleConfigSummary.theme(sdkConfig)

        val languageSubtitle: String get() = SampleConfigSummary.language(localeOverride)

        /** Debug info's Configuration card: the 8 lines Home used to frame (DS v1 dropped Home). */
        val configurationLines: List<Pair<String, String>>
            get() = SampleConfigSummary.configurationLines(sdkConfig, user, localeOverride)
    }

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val localeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == SampleConfigSummary.LOCALE_OVERRIDE_KEY) {
                _state.update { it.copy(localeOverride = prefs.getString(key, null)) }
            }
        }

    init {
        _state.update {
            it.copy(localeOverride = preferences.getString(SampleConfigSummary.LOCALE_OVERRIDE_KEY, null))
        }
        preferences.registerOnSharedPreferenceChangeListener(localeListener)
        viewModelScope.launch {
            userDataStore.userFlow.collect { user ->
                _state.update { it.copy(user = user) }
            }
        }
        viewModelScope.launch {
            configDataStore.configFlow.collect { config ->
                _state.update {
                    it.copy(
                        sdkConfig = config,
                        effectiveApiKey = config?.let(SDKConfigResolver::resolveApiKey).orEmpty(),
                    )
                }
            }
        }
        viewModelScope.launch {
            OctopusSDK.isInitialisedFlow.collect { initialized ->
                _state.update { it.copy(isInitialized = initialized) }
                if (initialized) {
                    launch {
                        OctopusSDK.connectionState.collect { state ->
                            _state.update { it.copy(connectionState = state) }
                        }
                    }
                    launch {
                        OctopusSDK.notSeenNotificationsCount.collect { count ->
                            _state.update { it.copy(notSeenNotifications = count) }
                        }
                    }
                    launch {
                        OctopusSDK.hasAccessToCommunity.collect { hasAccess ->
                            _state.update { it.copy(hasAccessToCommunity = hasAccess) }
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        preferences.unregisterOnSharedPreferenceChangeListener(localeListener)
        super.onCleared()
    }

    /**
     * Local reset: clears the sample's own persisted setup and nothing else. No account is deleted
     * and no backend is called — the next launch simply lands on the Config screen again.
     */
    fun resetConfig() {
        viewModelScope.launch {
            configDataStore.clear()
            userDataStore.clear()
        }
    }

    /**
     * Light/dark choice. Persisted in [SDKConfig] rather than in SharedPreferences because it is
     * part of "what the SDK is configured with" — Debug info reads it from the same place.
     */
    fun setAppearance(appearance: SDKConfig.Appearance) {
        viewModelScope.launch {
            configDataStore.updateConfig { it?.copy(appearance = appearance) }
        }
    }

    /**
     * Theme preset. `Default` is the sample's Octopus-navy `OctopusTheme`; `SdkDefault` hands the
     * SDK no theme at all, which is what a fresh integration looks like. A `Custom` demo theme
     * set from the QA scenario screen already renders the navy palette, so re-selecting "Octopus
     * navy" keeps it rather than silently dropping its fonts/icons flags.
     */
    fun setThemePreset(navy: Boolean) {
        viewModelScope.launch {
            configDataStore.updateConfig { config ->
                config?.copy(
                    theme =
                        when {
                            !navy -> SDKConfig.Theme.SdkDefault
                            config.theme is SDKConfig.Theme.SdkDefault -> SDKConfig.Theme.Default
                            else -> config.theme
                        },
                )
            }
        }
    }
}
