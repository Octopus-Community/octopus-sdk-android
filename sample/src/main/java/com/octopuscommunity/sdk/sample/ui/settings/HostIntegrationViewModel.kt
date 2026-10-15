package com.octopuscommunity.sdk.sample.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.sample.data.datastore.SDKConfigDataStore
import com.octopuscommunity.sdk.sample.data.model.HostCallback
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Reads and writes the persisted host wiring (`SDKConfig.hostCallbacks`), one switch at a time.
 *
 * Writes go straight to the stored config — the same keys the Config screen used to write on
 * Apply — so nothing else in the sample has to learn a new source. With no stored config yet (the
 * SDK was never started) there is nothing to amend, and [State.isConfigured] says so.
 */
class HostIntegrationViewModel(
    private val configDataStore: SDKConfigDataStore,
) : ViewModel() {
    data class State(
        val isConfigured: Boolean = false,
        val unifiedProfileEnabled: Boolean = false,
        /** The wiring the community runs with ([SDKConfig.effectiveHostCallbacks]). */
        val callbacks: SDKConfig.HostCallbacks = SDKConfig.HostCallbacks(),
        val communityNavigation: SDKConfig.CommunityNavigation = SDKConfig.CommunityNavigation.InTab,
        /** Callbacks the SDK requires under the current config, locked on, with the reason shown. */
        val requiredReasons: Map<HostCallback, String> = emptyMap(),
    )

    val state =
        configDataStore.configFlow
            .map { config ->
                State(
                    isConfigured = config != null,
                    unifiedProfileEnabled = config?.unifiedProfileEnabled == true,
                    callbacks = config?.effectiveHostCallbacks ?: SDKConfig.HostCallbacks(),
                    communityNavigation = config?.communityNavigation ?: SDKConfig.CommunityNavigation.InTab,
                    requiredReasons =
                        if (config == null) {
                            emptyMap()
                        } else {
                            HostCallback.entries
                                .mapNotNull { callback -> callback.requiredReason(config)?.let { callback to it } }
                                .toMap()
                        },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())

    fun setWired(
        callback: HostCallback,
        wired: Boolean,
    ) = update { callback.withWired(it, wired) }

    fun setUrlHandling(value: SDKConfig.UrlHandling) = update { it.copy(urlHandling = value) }

    fun setCommunityNavigation(value: SDKConfig.CommunityNavigation) {
        viewModelScope.launch {
            configDataStore.updateConfig { it?.copy(communityNavigation = value) }
        }
    }

    private fun update(transform: (SDKConfig.HostCallbacks) -> SDKConfig.HostCallbacks) {
        viewModelScope.launch {
            configDataStore.updateConfig { persisted ->
                persisted?.copy(hostCallbacks = transform(persisted.hostCallbacks))
            }
        }
    }
}
