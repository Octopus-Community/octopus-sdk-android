package com.octopuscommunity.sdk.sample.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.OctopusProfile
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.sample.data.datastore.UserDataStore
import com.octopuscommunity.sdk.sample.data.describe
import com.octopuscommunity.sdk.sample.data.usecase.CommunityConnectUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditUserViewModel(
    private val userDataStore: UserDataStore,
    private val communityConnectUseCase: CommunityConnectUseCase,
    private val profileFlow: Flow<OctopusProfile?> = OctopusSDK.profile,
) : ViewModel() {
    /**
     * Example entitlements for the demo community (internal ticket). Toggling them in the
     * sample UI rewrites the JWT claim and re-issues an Octopus JWT via the SDK; the
     * gating decisions on the gated topics (`Premium`, `Teaser`, `Announcements`) flip
     * accordingly.
     */
    enum class KnownEntitlement(
        val claim: String,
        val label: String,
    ) {
        PREMIUM("customer:premium", "customer:premium"),
        MODERATOR("customer:moderator", "customer:moderator"),
    }

    data class State(
        val nickname: String = "",
        val bio: String = "",
        val entitlements: Set<String> = emptySet(),
        val isLoading: Boolean = false,
        val isRefreshingEntitlements: Boolean = false,
        val error: String? = null,
        val isSaved: Boolean = false,
        val entitlementsRefreshMessage: String? = null,
        /**
         * Incremented every time a refresh round-trip succeeds. Drives one-shot side effects
         * (e.g. dismiss a sheet) without relying on a string flag that would re-trigger on
         * stale state when the VM outlives the consuming composable.
         */
        val entitlementsRefreshCount: Int = 0,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        // Seed nickname/bio from the local user store.
        viewModelScope.launch {
            userDataStore.getUser()?.let { user ->
                _state.update {
                    it.copy(
                        nickname = user.nickname ?: "",
                        bio = user.bio ?: "",
                    )
                }
            }
        }
        // Mirror backend grants only when the emitted host identity matches the stored user.
        // Keep the whole profile: equal grants can belong to different identities.
        viewModelScope.launch {
            profileFlow
                .distinctUntilChanged()
                .collect { profile -> userDataStore.mirrorGrantedEntitlements(profile) }
        }
        // The stored user also changes without a new SDK profile (edits, sign-in, or reset).
        // Observe just the grants so an unrelated user write does not discard draft toggles.
        viewModelScope.launch {
            userDataStore.userFlow
                .map { it?.entitlements?.toSet().orEmpty() }
                .distinctUntilChanged()
                .collect { entitlements ->
                    _state.update { it.copy(entitlements = entitlements) }
                }
        }
    }

    fun setNickname(value: String) = _state.update { it.copy(nickname = value) }

    fun setBio(value: String) = _state.update { it.copy(bio = value) }

    fun toggleEntitlement(
        claim: String,
        granted: Boolean,
    ) {
        _state.update {
            it.copy(
                entitlements = if (granted) it.entitlements + claim else it.entitlements - claim,
                entitlementsRefreshMessage = null,
            )
        }
    }

    fun save() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val updatedUser =
                userDataStore.getUser()?.copy(
                    nickname = _state.value.nickname.ifBlank { null },
                    bio = _state.value.bio.ifBlank { null },
                    entitlements = _state.value.entitlements.toList(),
                )

            if (updatedUser != null) {
                userDataStore.updateUser(clearSignOut = true) { updatedUser }
                val result = communityConnectUseCase(updatedUser)
                when (result) {
                    is OctopusResult.Success -> {
                        // The `OctopusSDK.profile` collector in `init` will mirror back the
                        // BE-granted entitlements into both `state` and `userDataStore`.
                        _state.update { it.copy(isLoading = false, isSaved = true) }
                    }
                    is OctopusResult.Failure -> {
                        _state.update { it.copy(isLoading = false, error = "Update failed: ${result.describe()}") }
                    }
                }
            } else {
                _state.update { it.copy(isLoading = false, error = "No user found. Login first.") }
            }
        }
    }

    /**
     * Persists the new entitlements set to the local user store and calls
     * `OctopusSDK.refreshEntitlements()` so the backend re-issues a JWT with the updated
     * `entitlements` claim and the SDK refetches the affected views (groups list, group
     * detail) — without going through the full `connectUser` flow.
     */
    fun refreshEntitlements() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isRefreshingEntitlements = true,
                    error = null,
                    entitlementsRefreshMessage = null,
                )
            }

            // Save the toggled entitlements first so the next tokenProvider call picks them up.
            userDataStore.updateUser(clearSignOut = false) { current ->
                current?.copy(entitlements = _state.value.entitlements.toList())
            }

            val result = OctopusSDK.refreshEntitlements()
            _state.update {
                when (result) {
                    // The `OctopusSDK.profile` collector in `init` will mirror back the
                    // BE-granted entitlements into both `state` and `userDataStore`.
                    is OctopusResult.Success ->
                        it.copy(
                            isRefreshingEntitlements = false,
                            entitlementsRefreshMessage = "Entitlements refreshed.",
                            entitlementsRefreshCount = it.entitlementsRefreshCount + 1,
                        )

                    is OctopusResult.Failure ->
                        it.copy(
                            isRefreshingEntitlements = false,
                            error = "Refresh failed: ${result.describe()}",
                        )
                }
            }
        }
    }
}
