package com.octopuscommunity.sdk.data.local.data

import kotlinx.serialization.Serializable

@Serializable
data class PushNotificationsData(
    val token: String?,
    val serverToken: String? = null,
    val serverInstallId: String? = null,
    val serverUserId: String? = null,
    val serverLanguage: String? = null
)