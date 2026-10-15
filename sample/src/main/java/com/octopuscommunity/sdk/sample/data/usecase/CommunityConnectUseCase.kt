package com.octopuscommunity.sdk.sample.data.usecase

import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ClientUser
import com.octopuscommunity.sdk.domain.model.Resource
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.sample.data.datastore.UserDataStore
import com.octopuscommunity.sdk.sample.data.model.User
import com.octopuscommunity.sdk.sample.data.model.User.Avatar
import com.octopuscommunity.sdk.sample.data.utils.TokenProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CommunityConnectUseCase(
    private val userDataStore: UserDataStore,
) {
    private val _successfulSignIns = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Successful explicit connections, including reconnecting the same still-connected user. */
    internal val successfulSignIns = _successfulSignIns.asSharedFlow()

    suspend operator fun invoke(user: User) =
        run {
            userDataStore.setSignedOut(false)
            val result =
                OctopusSDK.connectUser(
                    user =
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
                    tokenProvider = {
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
