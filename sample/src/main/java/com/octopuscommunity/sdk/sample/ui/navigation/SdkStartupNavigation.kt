package com.octopuscommunity.sdk.sample.ui.navigation

import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController

/** Complete onboarding without dismissing a Configuration revisit restored over the shell. */
internal fun NavHostController.finishSdkOnboarding(hasPendingConfigLink: Boolean) {
    // Activity recreation reruns the SDK-ready effect after Navigation restores the back stack.
    // Main below Config means this is a revisit, and its entry owns the unsaved form's ViewModel.
    if (runCatching { getBackStackEntry<MainRoute>() }.isSuccess) return
    val current = currentDestination
    if (current?.hasRoute<ConfigRoute>() == true ||
        (hasPendingConfigLink && current?.hasRoute<HostIntegrationRoute>() == true)
    ) {
        navigate(MainRoute) {
            popUpTo(0) { inclusive = true }
        }
    }
}
