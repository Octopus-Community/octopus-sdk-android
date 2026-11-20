package com.octopuscommunity.sdk.ui.responses

/**
 * Describes where a [ResponseUiModel] is rendered by the shared `ResponseCard` composable. Drives
 * card tap behaviour and ellipsization of the body text; other differences (reply button
 * visibility, see-replies row) are driven by the model's [ResponseKind], not by this mode.
 *
 * Equivalent to iOS `ResponseViewContext` + `DisplayableFeedResponse.ellipsizeText`.
 */
sealed interface ResponseDisplayMode {
    /**
     * In a list — post summary's featured comment, post detail's comment list, or comment
     * detail's reply list.
     *
     * @property onCardTap optional: pass `null` to skip the "tap anywhere on the card" shortcut
     *   (used for comments in `PostDetailsScreen` where only avatar, "See N replies" row and
     *   action-bar buttons should be tappable). Pass a lambda to enable it (used for the featured
     *   comment in the post summary, where tapping the card opens the parent post).
     * @property ellipsizeText `true` only for the featured comment shown inside a post summary
     *   (matches iOS `DisplayablePost.featuredComment` with `ellipsizeText: true`). All other
     *   `Summary` usages — comments inside a post detail, replies inside a comment detail —
     *   pass `false` so the body is rendered in full, which is what iOS does by default
     *   (`DisplayableFeedResponse.init(from comment:)` defaults `ellipsizeText` to `false`).
     */
    data class Summary(
        val onCardTap: (() -> Unit)?,
        val ellipsizeText: Boolean = false,
    ) : ResponseDisplayMode

    /** As the header of the comment detail screen. */
    data object Detail : ResponseDisplayMode
}
