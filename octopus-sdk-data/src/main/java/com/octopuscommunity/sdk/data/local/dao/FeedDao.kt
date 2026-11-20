package com.octopuscommunity.sdk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.octopuscommunity.sdk.data.local.entity.CommentEntity
import com.octopuscommunity.sdk.data.local.entity.FeedItemEntity
import com.octopuscommunity.sdk.data.local.entity.OctopusEntity
import com.octopuscommunity.sdk.data.local.entity.PostEntity
import com.octopuscommunity.sdk.data.local.entity.ReplyEntity
import com.octopuscommunity.sdk.data.local.entity.RootFeedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    /** Reads post rows without their related topic, for refresh conflict detection. */
    @Query("SELECT * FROM posts WHERE id IN (:ids)")
    suspend fun getRefreshPosts(ids: List<String>): List<PostEntity>

    /** Reads comment rows without their related post, for refresh conflict detection. */
    @Query("SELECT * FROM comments WHERE id IN (:ids)")
    suspend fun getRefreshComments(ids: List<String>): List<CommentEntity>

    /** Reads reply rows without their related comment, for refresh conflict detection. */
    @Query("SELECT * FROM replies WHERE id IN (:ids)")
    suspend fun getRefreshReplies(ids: List<String>): List<ReplyEntity>

    /** Captures full local state, including absence, keyed by globally unique object id. */
    @Transaction
    suspend fun getItemSnapshots(ids: List<String>): Map<String, OctopusEntity?> {
        val rows = mutableMapOf<String, OctopusEntity>()
        for (chunk in ids.distinct().chunked(900)) {
            for (row in getRefreshPosts(chunk) + getRefreshComments(chunk) + getRefreshReplies(chunk)) {
                rows[row.id] = row
            }
        }
        return ids.associateWith { rows[it] }
    }

    /**
     * Checks snapshots and stores only unchanged ids within one Room transaction. All item DAOs
     * share this database, so a local write cannot slip between the comparison and [store].
     * No database transaction is held while the network batches are fetched.
     */
    @Transaction
    suspend fun storeUnchangedItems(
        snapshots: Map<String, OctopusEntity?>,
        store: suspend (Set<String>) -> Unit,
    ) {
        val current = getItemSnapshots(snapshots.keys.toList())
        store(snapshots.filter { (id, previous) -> current[id] == previous }.keys)
    }

    @Query("SELECT * FROM root_feeds ORDER BY position ASC")
    fun getAllRootFeeds(): Flow<List<RootFeedEntity>>

    @Upsert
    suspend fun upsertRootFeeds(rootFeeds: List<RootFeedEntity>)

    @Query("DELETE FROM root_feeds")
    suspend fun deleteAllRootFeeds()

    @Transaction
    suspend fun replaceAllRootFeeds(rootFeeds: List<RootFeedEntity>) {
        deleteAllRootFeeds()
        upsertRootFeeds(rootFeeds)
    }

    @Query("SELECT * FROM feed_items WHERE feedId = :feedId ORDER BY position ASC")
    fun getFeedItems(feedId: String): Flow<List<FeedItemEntity>>

    @Query("SELECT MAX(position) FROM feed_items WHERE feedId = :feedId")
    suspend fun getMaxPosition(feedId: String): Int?

    @Query("SELECT MIN(position) FROM feed_items WHERE feedId = :feedId")
    suspend fun getMinPosition(feedId: String): Int?

    @Upsert
    suspend fun upsertFeedItems(items: List<FeedItemEntity>)

    @Query("DELETE FROM feed_items WHERE feedId = :feedId")
    suspend fun deleteFeedItems(feedId: String)

    /**
     * Deletes the items of the feeds whose id contains [idMarker] but not [keptIdMarker]. `instr`
     * rather than `LIKE`: it matches the markers literally, case included.
     */
    @Query("DELETE FROM feed_items WHERE instr(feedId, :idMarker) > 0 AND instr(feedId, :keptIdMarker) = 0")
    suspend fun deleteFeedItemsMarked(
        idMarker: String,
        keptIdMarker: String,
    )

    @Transaction
    suspend fun replaceFeedItems(
        feedId: String,
        items: List<FeedItemEntity>,
    ) {
        deleteFeedItems(feedId)
        upsertFeedItems(items)
    }
}
