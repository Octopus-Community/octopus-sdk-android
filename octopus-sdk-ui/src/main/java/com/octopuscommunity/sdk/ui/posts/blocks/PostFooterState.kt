package com.octopuscommunity.sdk.ui.posts.blocks

import androidx.compose.runtime.Immutable
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction

/**
 * What the one-line post footer shows (internal ticket): the like button, the comment button and the
 * stack of the most used reactions. Built by [postFooterState], rendered by [PostFooter].
 */
@Immutable
internal data class PostFooterState(
    /** The member's own reaction, shown in place of the empty heart. `null` when they have not reacted. */
    val userReaction: Reaction.Kind?,
    /** Total of every reaction on the post, shown next to the like button when > 0. */
    val reactionCount: Int,
    /** Shown next to the comment bubble when > 0. */
    val commentCount: Int,
    /** `false` when comments are closed on the post: the comment button is hidden. */
    val showCommentButton: Boolean,
    /** Up to [MAX_TOP_REACTIONS] kinds, most used first. Empty: nothing on the right. */
    val topReactions: List<Reaction.Kind>,
) {
    /**
     * Kind sent on a tap on the like button: the member's reaction, whatever it is, which removes
     * it, or a heart when they have not reacted yet.
     */
    val tapReaction: Reaction.Kind get() = userReaction ?: Reaction.Kind.Heart

    companion object {
        const val MAX_TOP_REACTIONS = 3
    }
}

/**
 * Builds the footer state from the post's reaction counters. The top reactions are sorted by
 * count, most used first; ties keep the order the server sent. Kinds with no reaction are ignored.
 */
internal fun postFooterState(
    reactions: List<OctopusItem.ReactionCount>,
    userReaction: Reaction?,
    commentCount: Int,
    canCreateChildren: Boolean,
): PostFooterState {
    val usedReactions = reactions.filter { it.count > 0 }
    return PostFooterState(
        userReaction = userReaction?.kind,
        reactionCount = usedReactions.sumOf { it.count },
        commentCount = commentCount.coerceAtLeast(0),
        showCommentButton = canCreateChildren,
        topReactions =
            usedReactions
                .sortedByDescending { it.count }
                .take(PostFooterState.MAX_TOP_REACTIONS)
                .map { it.reactionKind },
    )
}
