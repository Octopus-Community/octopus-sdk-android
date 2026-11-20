package com.octopuscommunity.sdk.data.local.data

import kotlinx.serialization.Serializable

@Serializable
data class SettingsData(
    val notifications: Notifications = Notifications()
) {
    @Serializable
    data class Notifications(val pushEnabled: Boolean = true)
}