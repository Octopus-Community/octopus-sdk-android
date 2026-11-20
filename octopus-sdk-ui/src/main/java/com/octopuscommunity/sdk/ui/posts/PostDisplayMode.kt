package com.octopuscommunity.sdk.ui.posts

/**
 * Describes where a [PostUiModel] is rendered by the shared `PostCard` composable. Drives text
 * truncation, tap behaviour and whether the group link is shown in the header.
 *
 * Equivalent to iOS `PostViewContext`.
 */
sealed interface PostDisplayMode {
    /**
     * Rendered inside a feed (main feed, group feed, profile feed, …).
     *
     * @property onCardTap called when the user taps the card surface.
     * @property onChildrenTap called when the user taps the comment / view count in the
     * aggregated info row — navigates to detail without opening the composer. The action bar's
     * comment button surfaces the composer separately (see `PostCard.onCommentButtonTap`).
     * @property displayGroupName whether the group link row is displayed in the header.
     */
    data class Summary(
        val onCardTap: () -> Unit,
        val onChildrenTap: () -> Unit,
        val displayGroupName: Boolean,
    ) : PostDisplayMode

    /** Rendered as the post header of the detail screen. */
    data object Detail : PostDisplayMode
}
