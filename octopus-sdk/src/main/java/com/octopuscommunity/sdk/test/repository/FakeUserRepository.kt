package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.model.User
import com.octopuscommunity.sdk.domain.model.UserConfig
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.UserRepository
import com.octopuscommunity.sdk.test.mock.MockCurrentUserProfiles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [UserRepository] whose user has the id of [MockCurrentUserProfiles.default], with community access. */
open class FakeUserRepository :
    FakeNotificationRepository(),
    UserRepository {
    protected val user =
        MutableStateFlow<User?>(
            User(id = MockCurrentUserProfiles.default.userId, jwt = ""),
        )

    private val _userConfig =
        MutableStateFlow<UserConfig?>(
            UserConfig(
                canAccessCommunity = true,
                accessDeniedMessage = "The community will be available soon. Please check back shortly",
            ),
        )

    override suspend fun getUser(): User? = user.value

    override suspend fun updateJwt(jwt: String) {
        user.update { it?.copy(jwt = jwt) }
    }

    override suspend fun deleteUser() {
        user.update { null }
    }

    override suspend fun getUserConfig(): UserConfig? = _userConfig.value

    override fun getUserConfigFlow(): Flow<UserConfig?> = _userConfig

    /** Replaces the community access the fake reports; `null` is an access not known yet. */
    fun setUserConfig(userConfig: UserConfig?) {
        _userConfig.value = userConfig
    }

    override suspend fun fetchCommunityAccess(): OctopusResult<UserConfig, Nothing> =
        OctopusResult.Success(_userConfig.value ?: UserConfig(canAccessCommunity = true))

    override suspend fun overrideCommunityAccess(
        hasAccess: Boolean,
    ): OctopusResult<Unit, CommunityConfigRepository.OverrideCommunityAccessError> = OctopusResult.Success(Unit)

    override suspend fun getPushNotificationsToken(): String? = ""

    override suspend fun registerPushNotificationsToken(token: String?): OctopusResult<Unit, Nothing> = OctopusResult.Success(Unit)
}
