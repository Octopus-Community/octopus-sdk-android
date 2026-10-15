package com.octopuscommunity.sdk.sample.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.sample.data.SDKTestFakesLauncher
import com.octopuscommunity.sdk.sample.data.SDKTestFakesRequest
import com.octopuscommunity.sdk.sample.data.datastore.UserDataStore
import com.octopuscommunity.sdk.sample.data.describe
import com.octopuscommunity.sdk.sample.data.model.User
import com.octopuscommunity.sdk.sample.data.usecase.CommunityConnectUseCase
import com.octopuscommunity.sdk.sample.data.usecase.DISCONNECT_FAILURE_MESSAGE
import com.octopuscommunity.sdk.sample.data.usecase.disconnectFromCommunity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The one screen that owns "who is signed in".
 *
 * It replaces the pair of top-level Login / Edit user screens: the same identity was editable from
 * two places, each with its own copy of the entitlement set, and neither showed the other's. Here
 * the local record ([UserDataStore]) is what the *next* JWT will carry and `OctopusSDK.profile` is
 * what the backend actually granted — the screen shows both and never pretends they are one value.
 *
 * Vocabulary note: the UI says "entitlements", never "claim". A claim is the JWT-level mechanism;
 * an integrator reading this sample is deciding which entitlements their users get, and the
 * transport is an implementation detail of [CommunityConnectUseCase].
 */
class AccountViewModel(
    private val userDataStore: UserDataStore,
    private val communityConnectUseCase: CommunityConnectUseCase,
    private val sdkTestFakesRequests: StateFlow<SDKTestFakesRequest?> = SDKTestFakesLauncher.request,
) : ViewModel() {
    /** The entitlements provisioned on the demo backend, offered as ready-made switches. */
    enum class KnownEntitlement(
        val value: String,
        val label: String,
        val description: String,
    ) {
        PREMIUM("customer:premium", "Premium", "Unlocks the gated \"Premium\" topic"),
        MODERATOR("customer:moderator", "Moderator", "Grants moderation actions on posts"),
        VERIFIED("customer:verified", "Verified", "Marks the profile as verified"),
    }

    data class State(
        val userId: String = "",
        val nickname: String? = null,
        val avatarUrl: String? = null,
        /** What the *next* connect/refresh will ask for — the local record. */
        val entitlements: Set<String> = emptySet(),
        /** What the backend granted on the current session. Null while disconnected. */
        val grantedEntitlements: Set<String>? = null,
        val connectionState: ConnectionState = ConnectionState.NotConnected,
        val isReconnecting: Boolean = false,
        val isRefreshing: Boolean = false,
        val message: String? = null,
        val error: String? = null,
    ) {
        val isConnected: Boolean
            get() = connectionState is ConnectionState.Connected

        val isGuest: Boolean
            get() = (connectionState as? ConnectionState.Connected)?.isGuest == true

        /** A real SSO identity, as opposed to the guest session the SDK opens on its own. */
        val isSignedIn: Boolean
            get() = (connectionState as? ConnectionState.Connected)?.isGuest == false

        /** Initials for the avatar placeholder — at most two letters, never an emoji. */
        val initials: String
            get() {
                val source = nickname?.takeIf { it.isNotBlank() } ?: userId
                return source
                    .split(' ', '-', '_', '.')
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                    .ifEmpty { "?" }
            }

        /**
         * True when the local record asks for something the session does not carry. Drives the one
         * line that tells the tester *why* Reconnect is the button to press.
         */
        val hasPendingEntitlementChange: Boolean
            get() = grantedEntitlements != null && grantedEntitlements != entitlements
    }

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            userDataStore.userFlow.collect { user ->
                _state.update {
                    it.copy(
                        // The field is edited locally, so a store emission must not stomp on what
                        // the tester is typing once an id is present.
                        userId = it.userId.ifBlank { user?.id.orEmpty() },
                        nickname = it.nickname ?: user?.nickname,
                        avatarUrl = user?.avatar?.data,
                        entitlements =
                            if (it.entitlements.isEmpty()) {
                                user?.entitlements?.toSet().orEmpty()
                            } else {
                                it.entitlements
                            },
                    )
                }
            }
        }
        viewModelScope.launch {
            OctopusSDK.isInitialisedFlow.collect { initialized ->
                if (!initialized) return@collect
                launch {
                    OctopusSDK.connectionState.collect { connection ->
                        _state.update { it.copy(connectionState = connection) }
                    }
                }
                launch {
                    OctopusSDK.profile.collect { profile ->
                        _state.update { it.copy(grantedEntitlements = profile?.entitlements) }
                    }
                }
            }
        }
    }

    fun setUserId(value: String) = _state.update { it.copy(userId = value, error = null) }

    fun setNickname(value: String) = _state.update { it.copy(nickname = value, error = null) }

    fun toggleEntitlement(
        value: String,
        granted: Boolean,
    ) = _state.update {
        it.copy(
            entitlements = if (granted) it.entitlements + value else it.entitlements - value,
            message = null,
        )
    }

    fun dismissMessage() = _state.update { it.copy(message = null, error = null) }

    /**
     * Forges a local JWT with the current values and calls `connectUser` — the full path, which is
     * what an entitlement *added* to the set needs (the backend only reads them at mint time).
     */
    fun reconnect() {
        val current = _state.value
        if (current.userId.isBlank()) {
            _state.update { it.copy(error = "User ID is required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isReconnecting = true, error = null, message = null) }
            val user =
                (userDataStore.getUser() ?: User()).copy(
                    id = current.userId,
                    nickname = current.nickname?.ifBlank { null },
                    entitlements = current.entitlements.toList(),
                )
            userDataStore.updateUser(clearSignOut = true) { user }
            if (sdkTestFakesRequests.value != null) {
                _state.update {
                    it.copy(
                        isReconnecting = false,
                        message = "Saved. SDK test fakes stay active; the host user applies once the app restarts.",
                    )
                }
                return@launch
            }
            _state.update {
                when (val result = communityConnectUseCase(user)) {
                    is OctopusResult.Success ->
                        it.copy(isReconnecting = false, message = "Connected as ${user.id}.")

                    is OctopusResult.Failure ->
                        it.copy(isReconnecting = false, error = "Connection failed: ${result.describe()}")
                }
            }
        }
    }

    /** The lighter path: same session, the SDK re-reads the entitlements. */
    fun refreshEntitlements() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null, message = null) }
            userDataStore.updateUser(clearSignOut = false) { it?.copy(entitlements = _state.value.entitlements.toList()) }
            _state.update {
                when (val result = OctopusSDK.refreshEntitlements()) {
                    is OctopusResult.Success ->
                        it.copy(isRefreshing = false, message = "Entitlements refreshed.")

                    is OctopusResult.Failure ->
                        it.copy(isRefreshing = false, error = "Refresh failed: ${result.describe()}")
                }
            }
        }
    }

    /**
     * Ends the Octopus session. The local record is deliberately kept: the tester almost always
     * wants to reconnect the same identity, and wiping it is what "Reset Configuration" is for.
     */
    fun disconnect() {
        viewModelScope.launch {
            _state.update { it.copy(message = null, error = null) }
            userDataStore
                .disconnectFromCommunity()
                .onSuccess { _state.update { it.copy(message = "Disconnected.", grantedEntitlements = null) } }
                .onFailure { _state.update { it.copy(error = DISCONNECT_FAILURE_MESSAGE) } }
        }
    }
}
