package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.Comment.Reply
import com.octopuscommunity.sdk.domain.network.OctopusResult

/** Repository for the replies to comments. Item reads and counters come from [OctopusItemRepository]. */
interface ReplyRepository : OctopusItemRepository<Reply, Reply.Draft, Reply.CreationError> {
    /**
     * Publishes [draftItem]. [isParentTranslated] tells whether the parent comment is displayed
     * translated.
     */
    suspend fun sendReply(
        draftItem: Reply.Draft,
        isParentTranslated: Boolean,
    ): OctopusResult<Reply, Reply.CreationError>

    /** Deletes [reply]. */
    suspend fun deleteReply(reply: Reply): OctopusResult<Unit, Nothing>
}
