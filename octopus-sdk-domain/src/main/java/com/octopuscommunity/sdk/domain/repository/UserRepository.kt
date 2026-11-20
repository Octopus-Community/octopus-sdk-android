package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.User
import com.octopuscommunity.sdk.domain.model.UserConfig
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository.OverrideCommunityAccessError
import kotlinx.coroutines.flow.Flow

/** Repository for the authenticated user, its push token and its community access. */
interface UserRepository {
    /** Returns the stored user, or `null` when there is none. */
    suspend fun getUser(): User?

    /** Replaces the stored user's JWT with [jwt]. */
    suspend fun updateJwt(jwt: String)

    /** Deletes the stored user. */
    suspend fun deleteUser()

    /** Returns the stored push notifications token. */
    suspend fun getPushNotificationsToken(): String?

    /** Stores [token] and registers it with the backend when it changed. */
    suspend fun registerPushNotificationsToken(token: String?): OctopusResult<Unit, Nothing>

    /** Registers the stored push notifications token again, if there is one. */
    suspend fun checkPushNotificationsToken(): OctopusResult<Unit, Nothing> =
        getPushNotificationsToken()?.let { registerPushNotificationsToken(it) }
            ?: OctopusResult.Success(Unit)

    /** Returns the cached [UserConfig]. */
    suspend fun getUserConfig(): UserConfig?

    /** Observes the cached [UserConfig]. */
    fun getUserConfigFlow(): Flow<UserConfig?>

    /** Fetches whether the user can access the community and updates the cache. */
    suspend fun fetchCommunityAccess(): OctopusResult<UserConfig, Nothing>

    /** Overrides the user's community access for an A/B test, then refetches it. */
    suspend fun overrideCommunityAccess(hasAccess: Boolean): OctopusResult<Unit, OverrideCommunityAccessError>
}
