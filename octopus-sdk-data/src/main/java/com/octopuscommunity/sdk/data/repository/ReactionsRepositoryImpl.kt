package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.sdk.data.mapper.ProfileMapper
import com.octopuscommunity.sdk.data.mapper.ReactionMapper
import com.octopuscommunity.sdk.data.remote.service.ObjectService
import com.octopuscommunity.sdk.data.utils.call
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.ReactionsPage
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ReactionsRepository

class ReactionsRepositoryImpl(
    private val objectService: ObjectService,
    private val reactionMapper: ReactionMapper,
    private val profileMapper: ProfileMapper,
) : ReactionsRepository {
    override suspend fun getReactions(
        contentId: String,
        kind: OctopusItem.Reaction.Kind?,
        cursor: String?,
        pageSize: Int,
    ): OctopusResult<ReactionsPage, Nothing> =
        call(reactionMapper) {
            val response =
                objectService.getReactionsPage(
                    parentId = contentId,
                    unicode = kind?.unicode,
                    pageCursor = cursor,
                    pageSize = pageSize,
                )
            // No `with(reactionMapper)` here: `call(mapper) { … }` already runs the block with the
            // mapper as receiver.
            response.toDomain { with(profileMapper) { it.toEntity().toDomain() } }
        }
}
