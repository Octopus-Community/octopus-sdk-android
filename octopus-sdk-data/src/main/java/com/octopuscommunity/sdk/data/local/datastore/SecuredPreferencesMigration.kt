package com.octopuscommunity.sdk.data.local.datastore

import android.content.SharedPreferences
import androidx.datastore.core.DataMigration
import androidx.datastore.migrations.SharedPreferencesMigration
import com.octopuscommunity.sdk.data.local.data.ClientUserData
import com.octopuscommunity.sdk.data.local.data.ImageData
import com.octopuscommunity.sdk.data.local.data.PushNotificationsData
import com.octopuscommunity.sdk.data.local.data.SecuredData
import com.octopuscommunity.sdk.data.local.data.UserData
import com.octopuscommunity.sdk.data.local.data.migrateLegacyLoginData
import kotlinx.coroutines.CancellationException

/** Never expose encrypted bytes as plain preferences or clean them up after a failed open. */
internal fun securedPreferencesMigration(
    readRawKeys: () -> Set<String>,
    openPreferences: () -> SharedPreferences,
): SharedPreferencesMigration<SecuredData> {
    val preferences by lazy(openPreferences)
    return SharedPreferencesMigration(
        produceSharedPreferences = { preferences },
        shouldRunMigration = {
            try {
                hasLegacySecuredPreferenceData(readRawKeys()) && preferences.all.isNotEmpty()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
        },
    ) { legacy, current ->
        val userId = legacy.getString("user_id")
        val userJwt = legacy.getString("user_jwt")
        val clientUserId = legacy.getString("client_user_id")
        val migrateIdentity = current.user == null && current.clientUser == null
        current.copy(
            user = if (migrateIdentity && userId != null && userJwt != null) UserData(userId, userJwt) else current.user,
            clientUser =
                if (migrateIdentity && clientUserId != null) {
                    ClientUserData(
                        id = clientUserId,
                        profile =
                            ClientUserData.Profile(
                                nickname = legacy.getString("client_user_nickname"),
                                bio = legacy.getString("client_user_bio"),
                                avatar =
                                    legacy.getString("client_user_avatar_type")?.let { type ->
                                        ImageData(
                                            type = ImageData.Type.valueOf(type),
                                            value = legacy.getString("client_user_avatar_value") ?: "",
                                        )
                                    },
                            ),
                    )
                } else {
                    current.clientUser
                },
            pushNotifications =
                current.pushNotifications ?: legacy.getString("push_notifications_token")?.let { token ->
                    PushNotificationsData(
                        token = token,
                        serverToken = legacy.getString("push_notifications_server_token"),
                        serverInstallId = legacy.getString("push_notifications_server_install_id"),
                        serverUserId = legacy.getString("push_notifications_server_user_id"),
                        serverLanguage = legacy.getString("push_notifications_server_language"),
                    )
                },
        )
        // SharedPreferencesMigration.cleanUp commits removal only after DataStore has saved
        // this merged value. Never clear the raw file from inside the migration transform.
    }
}

internal fun legacyLoginDataMigration(cleanUp: () -> Unit): DataMigration<SecuredData> =
    object : DataMigration<SecuredData> {
        override suspend fun shouldMigrate(currentData: SecuredData): Boolean = currentData.storageVersion < 1

        override suspend fun migrate(currentData: SecuredData): SecuredData = migrateLegacyLoginData(currentData)

        override suspend fun cleanUp() = cleanUp.invoke()
    }

/** A failed deletion must not invalidate the already committed migrated session. */
internal fun bestEffortMigrationCleanup(
    migration: DataMigration<SecuredData>,
    logFailure: (Throwable) -> Unit = ::logLegacyCleanupFailure,
): DataMigration<SecuredData> =
    object : DataMigration<SecuredData> by migration {
        override suspend fun cleanUp() {
            try {
                migration.cleanUp()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                logFailure(failure)
            }
        }
    }
