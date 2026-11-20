package com.octopuscommunity.sdk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.octopuscommunity.sdk.data.local.entity.CommentEntity
import com.octopuscommunity.sdk.data.local.entity.PostEntity
import com.octopuscommunity.sdk.data.local.entity.PostWithTopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE id IN (:ids)")
    @Transaction
    fun getPostsByIds(ids: List<String>): Flow<List<PostWithTopicEntity>>

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    @Transaction
    fun getAllPosts(): Flow<List<PostWithTopicEntity>>

    @Query("SELECT * FROM posts WHERE parentId = :topicId ORDER BY createdAt DESC")
    @Transaction
    fun getPostsByTopic(topicId: String): Flow<List<PostWithTopicEntity>>

    @Query("SELECT * FROM posts WHERE id = :id")
    @Transaction
    suspend fun getPost(id: String): PostWithTopicEntity?

    @Query("SELECT * FROM posts WHERE id = :id")
    @Transaction
    fun getPostFlow(id: String): Flow<PostWithTopicEntity?>

    @Query("SELECT * FROM posts WHERE client_object_id = :clientObjectId")
    @Transaction
    fun getPostByClientObject(clientObjectId: String): Flow<PostWithTopicEntity?>

    @Upsert
    suspend fun upsertPost(post: PostEntity)

    @Upsert
    suspend fun upsertPosts(posts: List<PostEntity>)

    @Upsert
    suspend fun upsertComments(comments: List<CommentEntity>)

    @Transaction
    suspend fun upsertPostsAndComments(
        posts: List<PostEntity>,
        comments: List<CommentEntity>,
    ) {
        upsertPosts(posts)
        upsertComments(comments)
    }

    @Update(PostEntity::class)
    suspend fun updatePostCounters(counters: PostEntity.CountersUpdate)

    @Update(PostEntity::class)
    suspend fun updatePostsCounters(counters: List<PostEntity.CountersUpdate>)

    @Update(CommentEntity::class)
    suspend fun updateCommentsCounters(counters: List<CommentEntity.CountersEntity.Update>)

    @Transaction
    suspend fun updatePostsAndCommentsCounters(
        counters: List<PostEntity.CountersUpdate>,
        commentsCounters: List<CommentEntity.CountersEntity.Update>,
    ) {
        updatePostsCounters(counters)
        updateCommentsCounters(commentsCounters)
    }

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deletePost(id: String)

    @Query("DELETE FROM posts")
    suspend fun deleteAllPosts()

    /** Deletes unreferenced posts atomically; an empty feed cache preserves all rows. */
    @Query(
        "DELETE FROM posts WHERE client_object_id IS NULL AND " +
            "EXISTS (SELECT 1 FROM feed_items) AND id NOT IN (SELECT itemId FROM feed_items)",
    )
    suspend fun deleteOrphanedPosts()

    @Transaction
    suspend fun replaceAllPosts(posts: List<PostEntity>) {
        deleteAllPosts()
        upsertPosts(posts)
    }

    @Query("UPDATE posts SET updatedAt = 0")
    suspend fun resetUpdatedAt()
}
