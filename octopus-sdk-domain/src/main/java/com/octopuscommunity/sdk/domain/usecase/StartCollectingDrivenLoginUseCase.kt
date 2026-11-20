package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.DrivenLoginTracker
import com.octopuscommunity.sdk.domain.model.TrackerEvent
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState.Connected
import com.octopuscommunity.sdk.domain.repository.TrackerRepository

/**
 * Emits the `octopus_driven_login` analytics event when Octopus drives a login: within one app run
 * the user triggered an action requiring login (the login screen was presented — recorded via
 * [DrivenLoginTracker.notifyLoginRequested]) and then becomes a real (non-guest) logged-in user,
 * including via the host app's SSO flow outside the Octopus UI. The event carries the triggering
 * action type.
 *
 * Long-lived collector: launched once on SDK init in the SDK scope; collects until the scope is
 * cancelled. Emitted via the dedicated `TrackRequest.octopusDrivenLogin` gRPC field (not a custom
 * event), per the SDK convention for Octopus-tracked events.
 */
class StartCollectingDrivenLoginUseCase(
    private val connectionRepository: ConnectionRepository,
    private val trackerRepository: TrackerRepository,
    private val drivenLoginTracker: DrivenLoginTracker,
) {
    suspend operator fun invoke() {
        // Seed from the current state so a user already logged in when collection starts doesn't
        // produce a false edge on the flow's initial replay.
        var wasLoggedIn = connectionRepository.getConnectionState().isRealUser()
        connectionRepository.getConnectionStateFlow().collect { state ->
            val isLoggedIn = state.isRealUser()
            // Only the logged-out/guest -> real-user edge counts, and only if a login was requested.
            if (!wasLoggedIn && isLoggedIn) {
                drivenLoginTracker.consumePendingAction()?.let { action ->
                    trackerRepository.trackEvent(TrackerEvent.OctopusDrivenLogin(action = action))
                }
            }
            wasLoggedIn = isLoggedIn
        }
    }

    private fun ConnectionState.isRealUser(): Boolean = this is Connected && !isGuest
}
