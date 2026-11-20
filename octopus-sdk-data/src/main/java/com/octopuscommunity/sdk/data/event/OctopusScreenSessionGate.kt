package com.octopuscommunity.sdk.data.event

/**
 * Decides whether an Octopus screen entering or leaving composition should start or stop the
 * Octopus session, and tracks whether any Octopus screen is currently on display.
 *
 * Extracted from the lifecycle observer so the decision is testable on the JVM.
 */
internal class OctopusScreenSessionGate {
    private var startedScreenCount = 0

    /** Whether the host app is currently in the background. */
    var isAppInBackground = false
        private set

    /** Whether any Octopus screen is currently on display, background transitions included. */
    val isOctopusStarted get() = startedScreenCount > 0

    fun onAppForegrounded() {
        isAppInBackground = false
    }

    fun onAppBackgrounded() {
        isAppInBackground = true
    }

    /**
     * Returns whether an Octopus session should be started.
     *
     * Never while the app is in the background: the foreground transition starts the session for
     * whatever screen is on display, and starting one here would have no live app session to
     * anchor it to.
     */
    fun onScreenStarted(): Boolean {
        startedScreenCount++
        return startedScreenCount == 1 && !isAppInBackground
    }

    /**
     * Returns whether the Octopus session should be stopped.
     *
     * Never while the app is in the background: the background transition already stopped it, and
     * a screen disposed afterwards must not ask for a second stop.
     */
    fun onScreenStopped(): Boolean {
        // Floored, for two reasons. An unmatched stop would otherwise drive the count negative,
        // and a negative count no longer lines up with what is on screen: onScreenStarted would
        // return false for the first screen of a visit, and true only on the accidental
        // co-composition of a second one, so sessions would arrive as near-empty bursts instead
        // of tracking the visit.
        // Flooring also changes what this function returns: from a count already at zero it now
        // returns true in the foreground, where a count going to -1 returned false. That asks for
        // a stop where none is needed, which StopOctopusSessionUseCase's idempotence absorbs --
        // nothing absorbed a negative count.
        startedScreenCount = (startedScreenCount - 1).coerceAtLeast(0)
        return startedScreenCount == 0 && !isAppInBackground
    }
}
