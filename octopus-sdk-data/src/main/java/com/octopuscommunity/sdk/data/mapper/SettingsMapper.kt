package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.NotificationServiceProto
import com.octopuscommunity.notificationSettings
import com.octopuscommunity.sdk.data.local.data.SettingsData
import com.octopuscommunity.sdk.domain.model.Settings

class SettingsMapper : OctopusMapper {
    fun SettingsData.toDomain() = Settings(
        notifications = notifications.toDomain()
    )

    fun SettingsData.Notifications.toDomain() = Settings.Notifications(
        pushEnabled = pushEnabled
    )

    fun NotificationServiceProto.NotificationSettings.toData() = SettingsData.Notifications(
        pushEnabled = pushNotificationEnabled
    )

    fun Settings.Notifications.toProto() = notificationSettings {
        this.pushNotificationEnabled = pushEnabled
    }
}