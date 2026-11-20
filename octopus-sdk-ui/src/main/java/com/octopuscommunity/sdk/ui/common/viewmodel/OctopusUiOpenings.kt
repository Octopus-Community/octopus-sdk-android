package com.octopuscommunity.sdk.ui.common.viewmodel

/**
 * Tracks openings of the Octopus UI, the counterpart of the root view's `onAppear` on iOS.
 * Callers register a unique token only while their screen intersects the clipped host viewport,
 * not merely while composed: a retained off-screen pager page must not keep an opening alive.
 * Home appearances are additionally gated by the screen's resumed lifecycle.
 *
 * Overlapping screens and swaps within one main-thread turn belong to the same opening.
 * Registrations survive lifecycle pauses (internal navigation or app backgrounding); disposal
 * during a configuration change also preserves the opening. Leaving the viewport or disposing
 * the last screen otherwise ends it. Repeated starts/stops for the same token are idempotent.
 *
 * Main thread only, like the composition effects that drive it.
 */
internal class OctopusUiOpenings(
    private val postToMainThread: (() -> Unit) -> Unit,
) {
    private val screensOnDisplay = mutableSetOf<Any>()
    private var registrationVersion = 0
    private var homeAppearanceHandled = false

    fun onScreenStarted(registration: Any) {
        if (screensOnDisplay.add(registration)) registrationVersion++
    }

    fun onScreenStopped(
        registration: Any,
        changingConfigurations: Boolean,
    ) {
        if (!screensOnDisplay.remove(registration)) return
        if (screensOnDisplay.isNotEmpty() || changingConfigurations) return
        // Decided one turn later, so a screen swap that leaves before it enters stays one opening.
        // A subsequent start invalidates this callback, even if that screen then rotates away.
        val stoppedVersion = registrationVersion
        postToMainThread {
            if (screensOnDisplay.isEmpty() && registrationVersion == stoppedVersion) homeAppearanceHandled = false
        }
    }

    /** Returns whether this is the first visible Home appearance of the current opening. */
    fun handleHomeAppearance(): Boolean {
        if (screensOnDisplay.isEmpty() || homeAppearanceHandled) return false
        homeAppearanceHandled = true
        return true
    }
}
