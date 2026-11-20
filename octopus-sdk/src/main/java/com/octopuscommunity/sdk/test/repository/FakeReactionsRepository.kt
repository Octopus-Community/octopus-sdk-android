package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.ProfileReaction
import com.octopuscommunity.sdk.domain.model.ReactionsPage
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ReactionsRepository
import com.octopuscommunity.sdk.test.mock.MockMinimalProfiles

/**
 * Deterministic "who reacted" list: every mock profile reacted once with each kind, so a preview or
 * screenshot test sees the same members and the same pagination regardless of which content or tab it
 * renders. Stateless, like the real [ReactionsRepository] — nothing here depends on [contentId].
 */
class FakeReactionsRepository : ReactionsRepository {
    private val allReactions: List<ProfileReaction> =
        MockMinimalProfiles.all.flatMap { author ->
            KINDS.map { kind -> ProfileReaction(author = author, kind = kind) }
        }

    override suspend fun getReactions(
        contentId: String,
        kind: Reaction.Kind?,
        cursor: String?,
        pageSize: Int,
    ): OctopusResult<ReactionsPage, Nothing> {
        val filtered = if (kind == null) allReactions else allReactions.filter { it.kind == kind }
        // The cursor is the fake's own offset into `filtered`, encoded as a string — meaningless
        // outside this class, exactly like a real backend cursor is meaningless outside it.
        val offset = cursor?.toIntOrNull() ?: 0
        val page = filtered.drop(offset).take(pageSize)
        val nextOffset = offset + page.size
        val nextCursor = nextOffset.takeIf { it < filtered.size }?.toString()
        return OctopusResult.Success(ReactionsPage(reactions = page, nextCursor = nextCursor))
    }

    private companion object {
        val KINDS =
            listOf(
                Reaction.Kind.Heart,
                Reaction.Kind.Joy,
                Reaction.Kind.MouthOpen,
                Reaction.Kind.Clap,
                Reaction.Kind.Cry,
                Reaction.Kind.Rage,
            )
    }
}
