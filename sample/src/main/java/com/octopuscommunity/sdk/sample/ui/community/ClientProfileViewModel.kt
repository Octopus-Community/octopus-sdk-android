package com.octopuscommunity.sdk.sample.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.OctopusCommunityData
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.sample.data.describe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backs [ClientProfileScreen] — the sample's stand-in for a host app's own profile screen, opened
 * from the SDK's `onNavigateToProfile` (Unified Profile) with the tapped member's **clientUserId**
 * (the host's own id for them, never the Octopus id).
 *
 * It shows what a real integration would surface on that screen: the [clientUserId] it received, and
 * the read-only Octopus community stats resolved from that same id via
 * [OctopusSDK.fetchCommunityDataByClientUserIdResult] — so the host never handles an Octopus id.
 */
class ClientProfileViewModel : ViewModel() {
    data class State(
        val isLoading: Boolean = true,
        val communityData: OctopusCommunityData? = null,
        val communityDataError: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private var loaded = false

    fun load(clientUserId: String) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            // Resolve the member from the host's own client id (GetPublicProfile clientUserId lookup)
            // and surface their Octopus community stats. Works for any member, self included.
            when (val result = OctopusSDK.fetchCommunityDataByClientUserIdResult(clientUserId)) {
                is OctopusResult.Success ->
                    _state.update { it.copy(isLoading = false, communityData = result.data) }

                is OctopusResult.Failure ->
                    _state.update { it.copy(isLoading = false, communityDataError = result.describe()) }
            }
        }
    }
}
