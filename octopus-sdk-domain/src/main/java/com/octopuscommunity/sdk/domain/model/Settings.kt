package com.octopuscommunity.sdk.domain.model

/** Settings of the current user. */
data class Settings(
    val notifications: Notifications,
) {
    /** Notification settings; [pushEnabled] tells whether push notifications are enabled. */
    data class Notifications(
        val pushEnabled: Boolean,
    )
}
