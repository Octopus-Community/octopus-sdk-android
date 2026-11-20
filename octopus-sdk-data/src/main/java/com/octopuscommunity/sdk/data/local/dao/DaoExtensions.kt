package com.octopuscommunity.sdk.data.local.dao

import com.octopuscommunity.sdk.data.local.entity.CommentWithPostEntity
import com.octopuscommunity.sdk.data.local.entity.PostWithTopicEntity
import com.octopuscommunity.sdk.data.local.entity.ReplyWithCommentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * SQLite has a limit of 999 bind variables per query (SQLITE_MAX_VARIABLE_NUMBER).
 * These extensions chunk large ID lists to avoid "too many SQL variables" crashes.
 */
private const val SQLITE_CHUNK_SIZE = 900

fun PostDao.getPostsByIdsChunked(ids: List<String>): Flow<List<PostWithTopicEntity>> {
    if (ids.isEmpty()) return flowOf(emptyList())
    if (ids.size <= SQLITE_CHUNK_SIZE) return getPostsByIds(ids)
    return combine(ids.chunked(SQLITE_CHUNK_SIZE).map { getPostsByIds(it) }) { arrays ->
        arrays.flatMap { it }
    }
}

fun CommentDao.getCommentsByIdsChunked(ids: List<String>): Flow<List<CommentWithPostEntity>> {
    if (ids.isEmpty()) return flowOf(emptyList())
    if (ids.size <= SQLITE_CHUNK_SIZE) return getCommentsByIds(ids)
    return combine(ids.chunked(SQLITE_CHUNK_SIZE).map { getCommentsByIds(it) }) { arrays ->
        arrays.flatMap { it }
    }
}

fun ReplyDao.getRepliesByIdsChunked(ids: List<String>): Flow<List<ReplyWithCommentEntity>> {
    if (ids.isEmpty()) return flowOf(emptyList())
    if (ids.size <= SQLITE_CHUNK_SIZE) return getRepliesByIds(ids)
    return combine(ids.chunked(SQLITE_CHUNK_SIZE).map { getRepliesByIds(it) }) { arrays ->
        arrays.flatMap { it }
    }
}

suspend fun NotificationDao.updateNotificationsReadStatusChunked(
    ids: List<String>,
    isRead: Boolean
): Int {
    if (ids.isEmpty() || ids.size <= SQLITE_CHUNK_SIZE) return updateNotificationsReadStatus(ids, isRead)
    var updated = 0
    for (chunk in ids.chunked(SQLITE_CHUNK_SIZE)) {
        updated += updateNotificationsReadStatus(chunk, isRead)
    }
    return updated
}
