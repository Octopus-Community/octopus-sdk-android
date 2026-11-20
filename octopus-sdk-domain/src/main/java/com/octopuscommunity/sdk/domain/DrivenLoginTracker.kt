package com.octopuscommunity.sdk.domain

import com.octopuscommunity.sdk.domain.model.OctopusDrivenLoginAction

/**
 * Process-wide holder for the action that triggered an Octopus login request, so the
 * `octopus_driven_login` event (emitted by `StartCollectingDrivenLoginUseCase` when the user then
 * becomes a real, non-guest logged-in user) can carry it.
 *
 * State is in-memory only: a fresh Koin container is built on every SDK init (cold launch), so the
 * pending action naturally resets per app run. It is deliberately not tied to background/foreground,
 * so an external SSO login flow (which backgrounds the app) is still attributed to Octopus.
 */
class DrivenLoginTracker {
    @Volatile
    var pendingAction: OctopusDrivenLoginAction? = null

    /** Records that the login screen was presented because the user tried to perform [action]. */
    fun notifyLoginRequested(action: OctopusDrivenLoginAction) {
        pendingAction = action
    }

    /** Returns and clears the pending action, so a login is attributed at most once per request. */
    fun consumePendingAction(): OctopusDrivenLoginAction? {
        val action = pendingAction
        pendingAction = null
        return action
    }
}
