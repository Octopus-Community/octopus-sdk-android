package com.octopuscommunity.sdk.domain.model

/**
 * One entry of the "who reacted" list: a member, and the reaction they left.
 *
 * Distinct from [OctopusItem.Reaction], which is the *current* user's reaction on a content and carries
 * the id needed to delete it. This one describes someone else's reaction and is read-only.
 *
 * @param author the member who reacted, or `null` when they have been deleted or banned. The reaction
 * still exists and still counts, so the entry is kept and rendered anonymously rather than dropped.
 * @param kind the reaction they left.
 */
data class ProfileReaction(
    val author: Author?,
    val kind: OctopusItem.Reaction.Kind,
)

/**
 * A page of the "who reacted" list.
 *
 * @param reactions most recent reaction first.
 * @param nextCursor cursor to pass back to fetch the following page, `null` when the list is exhausted.
 */
data class ReactionsPage(
    val reactions: List<ProfileReaction>,
    val nextCursor: String?,
)
