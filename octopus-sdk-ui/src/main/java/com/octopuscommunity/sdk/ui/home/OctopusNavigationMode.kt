package com.octopuscommunity.sdk.ui.home

/**
 * Navigation container requested by a host of [OctopusHomeScreen], matching iOS.
 *
 * Android uses the supplied Navigation Compose controller in both modes, including in modal
 * containers. Neither mode replaces the controller or resets its back stack.
 */
enum class OctopusNavigationMode {
    /** Use the platform's default navigation container. */
    Automatic,

    /** Request stack navigation; Android already uses the supplied controller's stack. */
    NavigationStack,
}
