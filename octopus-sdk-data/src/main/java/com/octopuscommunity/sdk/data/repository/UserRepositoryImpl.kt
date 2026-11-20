package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.UserServiceProto.CanAccessCommunityResponse.NoAccessReasonCase
import com.octopuscommunity.sdk.data.local.dao.UserConfigDao
import com.octopuscommunity.sdk.data.local.data.PushNotificationsData
import com.octopuscommunity.sdk.data.local.data.UserData
import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.data.local.datastore.SecuredDataStore
import com.octopuscommunity.sdk.data.local.entity.UserConfigEntity
import com.octopuscommunity.sdk.data.mapper.UserConfigMapper
import com.octopuscommunity.sdk.data.mapper.UserMapper
import com.octopuscommunity.sdk.data.remote.service.NotificationService
import com.octopuscommunity.sdk.data.remote.service.UserService
import com.octopuscommunity.sdk.data.utils.call
import com.octopuscommunity.sdk.domain.model.UserConfig
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.CommunityClosureRepository
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository.OverrideCommunityAccessError
import com.octopuscommunity.sdk.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

open class UserRepositoryImpl(
    protected val securedDataStore: SecuredDataStore,
    protected val preferencesDataStore: PreferencesDataStore,
    protected val userConfigDao: UserConfigDao,
    protected val userService: UserService,
    protected val notificationService: NotificationService,
    protected val userMapper: UserMapper,
    protected val userConfigMapper: UserConfigMapper,
    protected val closureRepository: CommunityClosureRepository = OpenCommunityClosureRepository,
) : UserRepository {
    internal val rootFeedSession = securedDataStore.rootFeedSession

    override suspend fun getUser() =
        with(userMapper) {
            securedDataStore.getUser()?.toDomain()
        }

    override suspend fun updateJwt(jwt: String) {
        securedDataStore.updateUser {
            it?.copy(jwt = jwt)
        }
    }

    /** Replaces the stored JWT with [newJwt] only while it is still [sentJwt]. */
    internal suspend fun updateJwt(
        sentJwt: String,
        newJwt: String,
    ) {
        securedDataStore.updateUser { user ->
            if (user?.jwt == sentJwt) user.copy(jwt = newJwt) else user
        }
    }

    // While the community is closed, the access is the closure message, whatever the store holds: its row can be
    // missing, when the app was killed during the purge.
    override suspend fun getUserConfig() =
        closureRepository.closureFlow.value?.toUserConfig()
            ?: with(userConfigMapper) { (userConfigDao.getUserConfig() ?: heldAccess.value)?.toDomain() }

    override fun getUserConfigFlow() =
        with(userConfigMapper) {
            combine(userConfigDao.getUserConfigFlow(), closureRepository.closureFlow, heldAccess) { stored, closure, held ->
                closure?.toUserConfig() ?: (stored ?: held)?.toDomain()
            }
        }

    /**
     * The access shown while its row is purged and not yet replaced: the screen of a denied access must not vanish
     * while the session behind it is being replaced. Only this instance shows it.
     */
    internal val heldAccess = MutableStateFlow<UserConfigEntity?>(null)

    override suspend fun fetchCommunityAccess(): OctopusResult<UserConfig, Nothing> =
        readCommunityAccess() ?: OctopusResult.Failure.UserNotAuthenticated(reason = SESSION_ENDED)

    /**
     * [fetchCommunityAccess], or `null` when the server answered that it ended the session the access was read with.
     * That answer is no access to show: the stored one is kept until the session that replaces it reads its own.
     */
    internal suspend fun readCommunityAccess(): OctopusResult<UserConfig, Nothing>? {
        // The access of a closed community is the closure message: not asked until it reopens.
        closureRepository.closureFlow.value?.let { return OctopusResult.Success(it.toUserConfig()) }
        return call(userConfigMapper) {
            val user = securedDataStore.getUser() // In case of the user flow didn't finished updating
            val response = userService.canAccessCommunity()
            if (!response.canAccessCommunity && response.noAccessReasonCase == NoAccessReasonCase.SESSIONENDED) {
                // Told by its user id: the answer that ends the session also renewed its JWT.
                user?.id?.let { userService.notifySessionEnded(it) }
                return null
            }
            val access = response.toEntity()
            // Closed while the call was in flight: the answer would replace the closure message.
            closureRepository.closureFlow.value?.toUserConfig()
                ?: access.also { userConfigDao.upsertUserConfig(it) }.toDomain()
        }
    }

    override suspend fun overrideCommunityAccess(hasAccess: Boolean): OctopusResult<Unit, OverrideCommunityAccessError> =
        call(mapper = userConfigMapper) {
            val userId = getUser()?.id ?: return OctopusResult.Failure.UserNotAuthenticated()
            val response =
                userService.bypassABTesting(
                    userId = userId,
                    giveCommunityAccess = hasAccess,
                )
            when {
                response.hasSuccess() -> {
                    updateJwt(response.success.jwt)
                    fetchCommunityAccess()
                }

                else -> return OctopusResult.Failure.InvalidArguments(
                    errors = response.fail.errorsList.map { it.toDomain() },
                )
            }
        }

    override suspend fun getPushNotificationsToken() = securedDataStore.pushNotificationsFlow.first()?.token

    override suspend fun registerPushNotificationsToken(token: String?): OctopusResult<Unit, Nothing> =
        call {
            securedDataStore.updatePushNotifications { it?.copy(token = token) }
            val pushNotifications =
                PushNotificationsData(
                    token = token,
                    serverToken = token,
                    serverInstallId = preferencesDataStore.installId,
                    serverUserId = securedDataStore.getUser()?.id,
                    serverLanguage = preferencesDataStore.getLocaleIdentifier(),
                )
            if (securedDataStore.getPushNotifications() != pushNotifications) {
                notificationService.registerPushToken(token)
                securedDataStore.updatePushNotifications { pushNotifications }
            }
        }

    override suspend fun deleteUser() {
        saveUser(null)
    }

    suspend fun saveUser(user: UserData?) {
        securedDataStore.updateUser { user }
        onUserSaved()
    }

    /** Refreshes what depends on the stored user once it changed, and returns the access read for that user. */
    internal suspend fun onUserSaved(): OctopusResult<UserConfig, Nothing> {
        val access = fetchCommunityAccess()
        // Update notification token with user
        checkPushNotificationsToken()
        return access
    }

    internal companion object {
        /** The reason of the failure of an access read sent with a session the server ended. */
        const val SESSION_ENDED = "session_ended"
    }
}
