package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.network.OctopusResult

/** Repository for the comments of posts. Item reads and counters come from [OctopusItemRepository]. */
interface CommentRepository : OctopusItemRepository<Comment, Comment.Draft, Comment.CreationError> {
    /**
     * Publishes [draftComment]. [isParentTranslated] tells whether the parent post is displayed
     * translated.
     */
    suspend fun sendComment(
        draftComment: Comment.Draft,
        isParentTranslated: Boolean,
    ): OctopusResult<Comment, Comment.CreationError>

    /** Deletes [comment]. */
    suspend fun deleteComment(comment: Comment): OctopusResult<Unit, Nothing>
}
