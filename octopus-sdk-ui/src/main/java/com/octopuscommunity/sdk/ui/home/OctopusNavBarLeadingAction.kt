package com.octopuscommunity.sdk.ui.home

/**
 * Host-driven leading item on the home navigation bar, matching iOS.
 *
 * Tapping the item invokes [onTap]; the host is responsible for dismissing its container.
 * The home title moves to the center while this item occupies the leading slot.
 */
sealed interface OctopusNavBarLeadingAction {
    /** Invoked when the top-bar item is tapped; system back is not routed here. */
    val onTap: () -> Unit

    /**
     * Close affordance for a host container, such as a modal.
     * @property onTap Callback that dismisses the host container.
     */
    data class Close(
        override val onTap: () -> Unit,
    ) : OctopusNavBarLeadingAction

    /**
     * Back affordance for a host navigation route.
     * @property onTap Callback that returns to the host route.
     */
    data class Back(
        override val onTap: () -> Unit,
    ) : OctopusNavBarLeadingAction
}
