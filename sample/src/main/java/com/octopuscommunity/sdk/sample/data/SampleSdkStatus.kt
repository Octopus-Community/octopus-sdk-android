package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState

/**
 * The one-word answer to "is the SDK usable right now?", shared by Settings (the SDK status line of
 * the Community block), Developer tools › Debug info and the Scenarios banners, so the three never
 * disagree.
 *
 * Derived, never stored: it is a read of [com.octopuscommunity.sdk.OctopusSDK.isInitialisedFlow],
 * `OctopusSDK.connectionState` and the device's network. "Unreachable" means the device has no
 * network at all, or only a captive portal still waiting for its sign-in page; a network the system
 * could not validate (a proxy or a region blocking its probe) still counts as online, since the SDK
 * works there. A backend that refuses a reachable device shows as [NoSession] (the SDK opened no
 * session, not even a guest one) and its own error state in the Community tab.
 */
enum class SampleSdkStatus(
    /** What the status line reads. */
    val label: String,
    /** The sentence under it: what the state means for a tester. */
    val detail: String,
) {
    NotStarted("Not started", "Start the SDK from Configuration."),
    Unreachable("Unreachable", "The device is offline: SDK calls fail until it reconnects."),
    NoSession("Started, no session", "The SDK has not opened a session yet, not even a guest one."),
    Guest("Running as guest", "Read-only: connect an SSO user to post and react."),
    SignedIn("Running, signed in", "Every scenario is available."),
    ;

    /** Whether the state is one a tester should fix before running a scenario. */
    val needsAttention: Boolean get() = this != SignedIn

    companion object {
        /** Precedence: not started, then offline, then the connection itself. */
        fun of(
            isInitialized: Boolean,
            connectionState: ConnectionState,
            isOnline: Boolean,
        ): SampleSdkStatus =
            when {
                !isInitialized -> NotStarted
                !isOnline -> Unreachable
                connectionState !is ConnectionState.Connected -> NoSession
                connectionState.isGuest -> Guest
                else -> SignedIn
            }
    }
}
