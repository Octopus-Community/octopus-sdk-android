package com.octopuscommunity.sdk.sample.data.usecase

import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ClientUser
import com.octopuscommunity.sdk.domain.model.Resource
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.ClientUserError
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.sample.data.datastore.UserDataStore
import com.octopuscommunity.sdk.sample.data.describe
import com.octopuscommunity.sdk.sample.data.model.User
import com.octopuscommunity.sdk.sample.data.model.User.Avatar
import com.octopuscommunity.sdk.sample.data.utils.TokenProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CommunityConnectUseCase(
    private val userDataStore: UserDataStore,
    private val isSigningAvailable: () -> Boolean = TokenProvider::isSigningAvailable,
    private val connectUser: suspend (ClientUser, suspend () -> String) -> OctopusResult<ConnectionState, ClientUserError> =
        { user, tokenProvider -> OctopusSDK.connectUser(user, tokenProvider) },
) {
    private val _successfulSignIns = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Successful explicit connections, including reconnecting the same still-connected user. */
    internal val successfulSignIns = _successfulSignIns.asSharedFlow()

    private val _isApplying = MutableStateFlow(false)

    /** Apply runs in app scope, including while Config is closed or its ViewModel is recreated. */
    internal val isApplying = _isApplying.asStateFlow()

    internal fun tryBeginConfigApply() = _isApplying.compareAndSet(false, true)

    internal fun finishConfigApply() {
        _isApplying.value = false
    }

    internal class ConfigConnectionFailure(
        val message: String,
    )

    private val _configConnectionFailures = MutableStateFlow<List<ConfigConnectionFailure>>(emptyList())

    /** Pending Apply failures survive navigation and Activity recreation until dismissed. */
    internal val configConnectionFailures = _configConnectionFailures.asStateFlow()

    internal fun reportConfigConnectionFailure(failure: OctopusResult.Failure<*>) {
        reportConfigApplyMessage("Connection failed: ${failure.describe()}")
    }

    internal fun reportConfigApplyMessage(message: String) {
        _configConnectionFailures.update { it + ConfigConnectionFailure(message) }
    }

    internal fun consumeConfigConnectionFailure(failure: ConfigConnectionFailure) {
        // Overlapping Activity collectors must never acknowledge a different queued failure.
        _configConnectionFailures.update { failures -> failures.filterNot { it === failure } }
    }

    /** Config must check before storing a user that app-start reconnect will pick up. */
    internal fun signingFailure(): OctopusResult.Failure<ClientUserError>? =
        if (isSigningAvailable()) {
            null
        } else {
            OctopusResult.Failure.InvalidArguments(
                ClientUserError.MissingToken("SSO sign-in is unavailable in this build."),
            )
        }

    suspend operator fun invoke(user: User): OctopusResult<ConnectionState, ClientUserError> =
        run {
            // connectUser may log out the current member before requesting the next user's token.
            // Reject unavailable signing before touching either the SDK session or sign-out state.
            signingFailure()?.let { return@run it }
            userDataStore.updateUser(clearSignOut = true) { user }
            val result =
                connectUser(
                    ClientUser(
                        userId = user.id ?: "",
                        profile =
                            ClientUser.Profile(
                                nickname = user.nickname,
                                bio = user.bio,
                                picture =
                                    user.avatar?.let { avatar ->
                                        when (avatar.source) {
                                            Avatar.Source.LOCAL -> Resource.Local(avatar.data)
                                            Avatar.Source.REMOTE -> Resource.Remote(avatar.data)
                                        }
                                    },
                            ),
                    ),
                    {
                        // Re-read the entitlements at every token request so `refreshEntitlements()`
                        // picks up whatever the user toggled since the last connect. Mirrors iOS
                        // `AppUserManager.currentEntitlements` which is a mutable singleton field read
                        // by the tokenProvider closure on every call. Capturing `user.entitlements`
                        // directly would freeze the entitlements at connect time and silently break
                        // every subsequent `refreshEntitlements()`.
                        val current = userDataStore.getUser() ?: user
                        TokenProvider.getToken(
                            userId = current.id ?: user.id ?: "",
                            entitlements = current.entitlements,
                        )
                    },
                )
            result.onSuccess { _successfulSignIns.emit(Unit) }
            result
        }
}
