package com.octopuscommunity.sdk.data.local.data

import com.octopuscommunity.sdk.data.BuildConfig
import kotlinx.serialization.Serializable

@Serializable
data class SecuredData(
    val user: UserData? = null,
    val clientUser: ClientUserData? = null,
    val pushNotifications: PushNotificationsData? = null,
    val sdkVersionCode: Int = BuildConfig.SDK_VERSION_CODE,
    /** Missing in older payloads; advancing this forces a rewrite without obsolete login data. */
    val storageVersion: Int = 0,
)

/** Forces a storage rewrite without obsolete login data, preserving the session. */
internal fun migrateLegacyLoginData(data: SecuredData): SecuredData = data.copy(storageVersion = 1)
