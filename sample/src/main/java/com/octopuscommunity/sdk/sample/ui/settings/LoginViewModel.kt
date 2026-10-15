package com.octopuscommunity.sdk.sample.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.sample.data.datastore.SDKConfigDataStore
import com.octopuscommunity.sdk.sample.data.datastore.UserDataStore
import com.octopuscommunity.sdk.sample.data.describe
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.data.model.User
import com.octopuscommunity.sdk.sample.data.pickerUserIds
import com.octopuscommunity.sdk.sample.data.usecase.CommunityConnectUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userDataStore: UserDataStore,
    private val communityConnectUseCase: CommunityConnectUseCase,
    private val configDataStore: SDKConfigDataStore,
) : ViewModel() {
    data class State(
        val userId: String = "",
        val nickname: String = "",
        val bio: String = "",
        val entitlements: Set<String> = emptySet(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val isConnected: Boolean = false,
        val managedFields: Set<ProfileField> = emptySet(),
        val forceLogin: Boolean = false,
    ) {
        val isNicknameManaged: Boolean get() = ProfileField.NICKNAME in managedFields
        val isBioManaged: Boolean get() = ProfileField.BIO in managedFields
    }

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Load managed fields from config
            val config = configDataStore.configFlow.first()
            if (config?.authKind is SDKConfig.AuthKind.SSO) {
                val sso = config.authKind as SDKConfig.AuthKind.SSO
                _state.update {
                    it.copy(
                        managedFields = sso.appManagedFields.toSet(),
                        forceLogin = sso.forceLoginOnStrongActions,
                    )
                }
            }
            // Load saved user
            userDataStore.getUser()?.let { user ->
                _state.update {
                    it.copy(
                        userId = user.id ?: "",
                        nickname = user.nickname ?: "",
                        bio = user.bio ?: "",
                        entitlements = user.entitlements.toSet(),
                    )
                }
            }
            // Seed the userId field on a fresh install (no saved user) with
            // the first quick-pick id so the chip row's default identity is
            // already in the field and the user can hit Connect.
            if (_state.value.userId.isBlank()) {
                pickerUserIds.firstOrNull()?.let { seed ->
                    _state.update { it.copy(userId = seed) }
                }
            }
        }
    }

    fun setUserId(value: String) = _state.update { it.copy(userId = value) }

    fun setNickname(value: String) = _state.update { it.copy(nickname = value) }

    fun setBio(value: String) = _state.update { it.copy(bio = value) }

    /**
     * Toggle an entitlement claim before connecting. Mirrors iOS `AppLoginScreen`
     * `EditEntitlementsView` — selected entitlements are persisted on the local user record
     * at connect time so the `tokenProvider` lambda can embed them in the next client-signed
     * JWT.
     */
    fun toggleEntitlement(
        claim: String,
        granted: Boolean,
    ) {
        _state.update {
            it.copy(
                entitlements = if (granted) it.entitlements + claim else it.entitlements - claim,
            )
        }
    }

    fun connect() {
        val current = _state.value
        if (current.userId.isBlank()) {
            _state.update { it.copy(error = "User ID is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val user =
                User(
                    id = current.userId,
                    nickname = current.nickname.ifBlank { null },
                    bio = current.bio.ifBlank { null },
                    entitlements = current.entitlements.toList(),
                )

            // Save user locally
            userDataStore.updateUser(clearSignOut = true) { user }

            // Connect to Octopus
            val result = communityConnectUseCase(user)
            when (result) {
                is OctopusResult.Success -> {
                    _state.update { it.copy(isLoading = false, isConnected = true) }
                }
                is OctopusResult.Failure -> {
                    _state.update { it.copy(isLoading = false, error = "Connection failed: ${result.describe()}") }
                }
            }
        }
    }
}
