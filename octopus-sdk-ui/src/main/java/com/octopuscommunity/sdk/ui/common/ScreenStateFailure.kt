package com.octopuscommunity.sdk.ui.common

import com.octopuscommunity.sdk.domain.network.OctopusResult

/**
 * A first load that failed, in the only two shapes the design distinguishes.
 *
 * Being offline gets its own illustration and wording because the member can act on it; everything
 * else — server down, timeout, permission, unknown — shares one neutral catch-all that says nothing
 * about the network. Iso iOS `ScreenStateFailure`.
 *
 * This is the *inline* half of [loadFailureChannel]: it only ever describes a failure with nothing on
 * screen behind it. A failure over visible content goes to the snackbar, which keeps the server's own
 * message.
 */
internal enum class ScreenStateFailure {
    NoNetwork,
    Other,
    ;

    companion object {
        fun from(failure: OctopusResult.Failure<*>): ScreenStateFailure =
            if (failure is OctopusResult.Failure.NoNetwork) NoNetwork else Other
    }
}
