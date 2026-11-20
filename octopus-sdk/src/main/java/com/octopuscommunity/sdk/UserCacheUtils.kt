package com.octopuscommunity.sdk

import coil3.ImageLoader
import com.octopuscommunity.sdk.data.local.data.PushNotificationsData
import com.octopuscommunity.sdk.data.local.database.OctopusDatabase
import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.data.local.datastore.SecuredDataStore
import com.octopuscommunity.sdk.data.local.datastore.SessionsDataStore
import com.octopuscommunity.sdk.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.Koin

/**
 * Empties what the SDK cached once the community is closed, so that nothing of it is shown or published to the host
 * app (groups, related posts, community data, unseen notifications) while it is, nor sent when it reopens.
 *
 * Unlike [clearUserCaches], it keeps the session, the client user and the push token: the identity of a guest is only
 * its session, which must still be theirs when the community reopens. It forgets that the token was registered: the
 * server may not have it any more when the community reopens, which then registers it again. It reloads nothing: no
 * call may leave while the community is closed.
 */
internal suspend fun Koin.purgeClosedCommunity() {
    withContext(Dispatchers.IO) {
        get<OctopusDatabase>().clearClosedCommunityTables()
        // Session ends journaled before the closure would be replayed, and sent, at reopening.
        get<SessionsDataStore>().update { it.copy(pendingAppSessionEnds = emptyList()) }
        get<SecuredDataStore>().update { current ->
            current.copy(pushNotifications = current.pushNotifications?.token?.let { PushNotificationsData(token = it) })
        }
        val images = get<ImageLoader>()
        images.memoryCache?.clear()
        images.diskCache?.clear()
    }
}

/** Shared by logout, SSO user switches and the full SDK reset. */
internal class UserCacheClearer(
    koin: Koin,
) {
    private val secured = koin.get<SecuredDataStore>()
    private val database = koin.get<OctopusDatabase>()
    private val sessions = koin.get<SessionsDataStore>()
    private val settings = koin.get<SettingsDataStore>()
    private val preferences = koin.get<PreferencesDataStore>()
    private val images = koin.get<ImageLoader>()

    suspend fun clear(clearInstallationData: Boolean = false) =
        withContext(Dispatchers.IO) {
            if (clearInstallationData) {
                // Full resets also discard retained encrypted storage and community-scoped data.
                secured.clear()
                database.clearAllTables()
            } else {
                val keepGuest = database.profileDao().getCurrentUserProfile()?.isGuest == true
                // The SSO logout already forgot retained storage through updateClientUser(null).
                // Keep the device token in the same atomic update that discards user credentials.
                secured.update { current ->
                    current.copy(
                        user = current.user.takeIf { keepGuest },
                        clientUser = null,
                        pushNotifications = current.pushNotifications?.token?.let { PushNotificationsData(token = it) },
                    )
                }
                database.clearUserTables(keepGuestProfile = keepGuest)
            }
            if (clearInstallationData) sessions.clear() else sessions.clearUserData()
            settings.clearUserData()
            if (clearInstallationData) preferences.clear() else preferences.clearUserData()
            images.memoryCache?.clear()
            images.diskCache?.clear()
            Unit
        }
}

/** Resolves the prebuilt cache clearer while admitted by the SDK lifecycle lock. */
internal suspend fun Koin.clearUserCaches(clearInstallationData: Boolean = false) =
    (getOrNull<UserCacheClearer>() ?: UserCacheClearer(this)).clear(clearInstallationData)
