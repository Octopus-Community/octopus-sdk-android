package com.octopuscommunity.sdk.sample.data

/** One automatic notification prompt per Activity session, including recreation and restore. */
internal class NotificationPermissionPrompt(
    var hasRequested: Boolean = false,
) {
    fun shouldLaunch(
        apiLevel: Int,
        permissionGranted: Boolean,
    ): Boolean {
        if (apiLevel < 33 || permissionGranted || hasRequested) return false
        hasRequested = true
        return true
    }
}
