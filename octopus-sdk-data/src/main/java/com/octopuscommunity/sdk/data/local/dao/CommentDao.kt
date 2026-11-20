package com.octopuscommunity.sdk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.octopuscommunity.sdk.data.local.entity.CommentEntity
import com.octopuscommunity.sdk.data.local.entity.CommentEntity.CountersEntity
import com.octopuscommunity.sdk.data.local.entity.CommentWithPostEntity
import com.octopuscommunity.sdk.data.local.entity.ReactionCountEntity
import com.octopuscommunity.sdk.data.local.entity.ReplyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE id IN (:ids)")
    @Transaction
    fun getCommentsByIds(ids: List<String>): Flow<List<CommentWithPostEntity>>

    @Query(
        "SELECT comments.* FROM feed_items JOIN comments ON comments.id = feed_items.itemId " +
            "WHERE feed_items.feedId = :feedId AND feed_items.position BETWEEN :minPosition AND :maxPosition " +
            "ORDER BY feed_items.position ASC",
    )
    @Transaction
    fun getFeedComments(
        feedId: String,
        minPosition: Int,
        maxPosition: Int,
    ): Flow<List<CommentWithPostEntity>>

    @Query("SELECT * FROM comments WHERE id = :id")
    @Transaction
    fun getComment(id: String): Flow<CommentWithPostEntity?>

    @Upsert
    suspend fun upsertComment(comment: CommentEntity)

    @Upsert
    suspend fun upsertComments(comments: List<CommentEntity>)

    @Upsert
    suspend fun upsertReplies(replies: List<ReplyEntity>)

    @Transaction
    suspend fun upsertCommentsAndReplies(
        comments: List<CommentEntity>,
        replies: List<ReplyEntity>,
    ) {
        upsertComments(comments)
        upsertReplies(replies)
    }

    @Query(
        "UPDATE comments SET counters_reactions = :reactions, " +
            "user_reaction_id = :reactionId, user_reaction_unicode = :reactionUnicode WHERE id = :id",
    )
    suspend fun updateCommentReaction(
        id: String,
        reactions: List<ReactionCountEntity>,
        reactionId: String?,
        reactionUnicode: String?,
    )

    @Update(CommentEntity::class)
    suspend fun updateCommentCounters(counters: CountersEntity.Update)

    @Update(CommentEntity::class)
    suspend fun updateCommentsCounters(counters: List<CountersEntity.Update>)

    @Update(ReplyEntity::class)
    suspend fun updateRepliesCounters(counters: List<ReplyEntity.CountersEntity.Update>)

    @Transaction
    suspend fun updateCommentsAndRepliesCounters(
        counters: List<CountersEntity.Update>,
        repliesCounters: List<ReplyEntity.CountersEntity.Update>,
    ) {
        updateCommentsCounters(counters)
        updateRepliesCounters(repliesCounters)
    }

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun deleteComment(id: String)

    /** Deletes unreferenced comments atomically; an empty feed cache preserves all rows. */
    @Query(
        "DELETE FROM comments WHERE " +
            "EXISTS (SELECT 1 FROM feed_items) AND id NOT IN (SELECT itemId FROM feed_items)",
    )
    suspend fun deleteOrphanedComments()

    @Query("UPDATE comments SET updatedAt = 0")
    suspend fun resetUpdatedAt()
}
