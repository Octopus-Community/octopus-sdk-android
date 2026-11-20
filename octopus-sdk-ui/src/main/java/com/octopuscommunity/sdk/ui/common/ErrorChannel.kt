package com.octopuscommunity.sdk.ui.common

/**
 * The single surface a failure is reported on.
 *
 * One error, one channel. Surfacing the same failure on two channels at once makes the user
 * dismiss a dialog (or a snackbar) only to read the very same sentence underneath it — observed
 * during QA on 2026-08-20, where an offline community feed rendered its error inline *and* raised
 * the transient channel with the identical text.
 */
internal enum class ErrorChannel {
    /** Rendered where the content would have been, and stays until the load succeeds. */
    Inline,

    /** The snackbar: dismissible, leaves the content it floats over readable. */
    Transient,
}

/**
 * Picks the channel for a *load* failure (fetching a feed, a list, a page).
 *
 * The inline slot is only free when there is nothing to show: with content already on screen it is
 * taken, and replacing a visible feed with an error message would drop what the user is reading, so
 * the failure goes to the transient channel instead. Either way it is reported exactly once.
 *
 * Failures from an action the user explicitly triggered (sending a post, reacting, deleting) never
 * go through here — they have no inline surface and go through
 * [com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel.showError], the only path that can
 * escalate to the modal alert dialog.
 *
 * The rule this establishes — a load failure never goes modal — is applied on the feed and item-list
 * paths. Several load paths still call `showError` directly and can therefore still raise the modal
 * (post and comment details, activity, and both profile summaries — the other user's and the
 * connected user's); migrating them is follow-up work, not a regression introduced here.
 *
 * @param hasVisibleContent whether the surface already displays content (a non-empty list).
 */
internal fun loadFailureChannel(hasVisibleContent: Boolean): ErrorChannel =
    if (hasVisibleContent) ErrorChannel.Transient else ErrorChannel.Inline
