package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.GrpcModelProto
import com.octopuscommunity.GrpcModelProto.OctoObject
import com.octopuscommunity.ReactionServiceProto
import com.octopuscommunity.reactionOrNull
import com.octopuscommunity.sdk.data.local.entity.ReactionCountEntity
import com.octopuscommunity.sdk.data.local.entity.ReactionEntity
import com.octopuscommunity.sdk.domain.model.Author
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount
import com.octopuscommunity.sdk.domain.model.ProfileReaction
import com.octopuscommunity.sdk.domain.model.ReactionsPage
import com.octopuscommunity.sdk.domain.repository.SetReactionError

class ReactionMapper : OctopusMapper {
    fun Reaction.toEntity() =
        ReactionEntity(
            unicode = kind.unicode,
            id = id,
        )

    fun ReactionEntity.toDomain() =
        Reaction(
            kind = unicode.toDomain(),
            id = id,
        )

    fun GrpcModelProto.ReactionCtx.toEntity() =
        ReactionEntity(
            unicode = unicode,
            id = reactionId,
        )

    fun OctoObject.toDomain() =
        content.reactionOrNull?.let { reaction ->
            Reaction(
                id = id,
                kind = reaction.unicode.toDomain(),
            )
        }

    fun String.toDomain(): Reaction.Kind =
        Reaction.Kind.ALL.firstOrNull { it.unicode == this }
            ?: Reaction.Kind.Unknown(this)

    fun ReactionCount.toEntity() =
        ReactionCountEntity(
            unicode = reactionKind.unicode,
            count = count,
        )

    fun ReactionCountEntity.toDomain() =
        ReactionCount(
            reactionKind = unicode.toDomain(),
            count = count,
        )

    fun GrpcModelProto.ReactionData.toEntity() =
        ReactionCountEntity(
            unicode = unicode,
            count = count,
        )

    fun ReactionServiceProto.PutReactionResponse.Error.toDomain(): SetReactionError =
        when (field) {
            ReactionServiceProto.PutReactionResponse.Error.Field.CONTENT_PARENT ->
                when (detailsCase) {
                    ReactionServiceProto.PutReactionResponse.Error.DetailsCase.MISSINGPARENT ->
                        SetReactionError.PostNotFound

                    else -> SetReactionError.ReactionError(message)
                }

            else -> SetReactionError.ReactionError(message)
        }

    /**
     * Maps a page of the "who reacted" list.
     *
     * [toAuthor] converts a proto profile, so this stays independent of [ProfileMapper] and of its
     * dependencies — the two normalizations below are what actually need covering.
     */
    fun ReactionServiceProto.GetReactionsPageResponse.toDomain(toAuthor: (GrpcModelProto.MinimalProfile) -> Author) =
        ReactionsPage(
            reactions =
                reactionsList.map { userReaction ->
                    ProfileReaction(
                        // The backend documents the profile as "empty when the user is deleted or banned",
                        // which can mean an absent message or a present-but-blank one: both are "no profile".
                        author =
                            userReaction.profile
                                .takeIf { userReaction.hasProfile() && it.profileId.isNotEmpty() }
                                ?.let(toAuthor),
                        kind = userReaction.unicode.toDomain(),
                    )
                },
            // An empty cursor means "no more pages" just as much as an absent one does: sending it back
            // would restart the list from the top.
            nextCursor = nextPageCursor.takeIf { hasNextPageCursor() && it.isNotEmpty() },
        )
}
