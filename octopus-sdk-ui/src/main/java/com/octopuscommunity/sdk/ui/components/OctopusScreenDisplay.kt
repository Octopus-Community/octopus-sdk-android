package com.octopuscommunity.sdk.ui.components

import androidx.lifecycle.Lifecycle

/**
 * Whether an Octopus screen counts as displayed after the lifecycle hosting its view moved to
 * [hostState].
 *
 * [hostState] is the view-tree lifecycle that hosts the screen's navigation: the Activity or
 * Fragment natively, the `<OctopusUIView>` owner in React Native. A host at least STARTED is
 * displayed. Below STARTED the screen is hidden only while its Activity is still started: a React
 * Native view hidden or detached inside a visible Activity. When the Activity itself is stopped
 * (the app going to the background, another Activity on top) the screen keeps its [displayed]
 * value and the app lifecycle owns the session, as before. An unknown Activity is treated as
 * started.
 */
internal fun nextScreenDisplayed(
    displayed: Boolean,
    hostState: Lifecycle.State,
    activityState: Lifecycle.State?,
): Boolean =
    when {
        hostState.isAtLeast(Lifecycle.State.STARTED) -> true
        activityState != null && !activityState.isAtLeast(Lifecycle.State.STARTED) -> displayed
        else -> false
    }
