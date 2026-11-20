package com.octopuscommunity.sdk.test.mock

import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount

/** Mock reaction counts. */
object MockReactions {
    val default =
        ReactionCount(
            reactionKind = OctopusItem.Reaction.Kind.Heart,
            count = (0..100).random(),
        )

    val all =
        OctopusItem.Reaction.Kind.ALL.map {
            ReactionCount(
                reactionKind = it,
                count = (0..100).random(),
            )
        }
}
