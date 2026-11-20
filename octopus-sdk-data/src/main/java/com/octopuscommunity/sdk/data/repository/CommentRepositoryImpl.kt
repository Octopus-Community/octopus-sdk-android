package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.OctoObjectServiceProto.GetResponse
import com.octopuscommunity.sdk.data.local.dao.CommentDao
import com.octopuscommunity.sdk.data.local.dao.FeedDao
import com.octopuscommunity.sdk.data.local.dao.ProfileDao
import com.octopuscommunity.sdk.data.local.dao.UserConfigDao
import com.octopuscommunity.sdk.data.local.dao.getCommentsByIdsChunked
import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.data.local.datastore.SecuredDataStore
import com.octopuscommunity.sdk.data.mapper.CommentMapper
import com.octopuscommunity.sdk.data.mapper.FeedMapper
import com.octopuscommunity.sdk.data.mapper.MediaEncodingException
import com.octopuscommunity.sdk.data.mapper.ProfileMapper
import com.octopuscommunity.sdk.data.mapper.ReactionMapper
import com.octopuscommunity.sdk.data.mapper.ReplyMapper
import com.octopuscommunity.sdk.data.mapper.UserConfigMapper
import com.octopuscommunity.sdk.data.mapper.UserMapper
import com.octopuscommunity.sdk.data.mapper.catchingMediaEncoding
import com.octopuscommunity.sdk.data.remote.service.FeedService
import com.octopuscommunity.sdk.data.remote.service.NotificationService
import com.octopuscommunity.sdk.data.remote.service.ObjectService
import com.octopuscommunity.sdk.data.remote.service.UserService
import com.octopuscommunity.sdk.data.utils.call
import com.octopuscommunity.sdk.data.utils.callWithValidation
import com.octopuscommunity.sdk.data.utils.joinLocalEntities
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.Feed
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.CommentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class CommentRepositoryImpl(
    securedDataStore: SecuredDataStore,
    preferencesDataStore: PreferencesDataStore,
    userConfigDao: UserConfigDao,
    profileDao: ProfileDao,
    feedDao: FeedDao,
    private val commentDao: CommentDao,
    userService: UserService,
    notificationService: NotificationService,
    feedService: FeedService,
    objectService: ObjectService,
    userMapper: UserMapper,
    userConfigMapper: UserConfigMapper,
    profileMapper: ProfileMapper,
    feedMapper: FeedMapper,
    private val reactionMapper: ReactionMapper,
    private val commentMapper: CommentMapper,
    private val replyMapper: ReplyMapper,
) : OctopusItemRepositoryImpl<Comment, Comment.Draft, Comment.CreationError>(
        securedDataStore,
        preferencesDataStore,
        userConfigDao,
        profileDao,
        feedDao,
        userService,
        notificationService,
        feedService,
        objectService,
        userMapper,
        userConfigMapper,
        profileMapper,
        feedMapper,
        reactionMapper,
    ),
    CommentRepository {
    override fun getLocalItemFlow(itemId: String): Flow<Comment?> =
        with(commentMapper) {
            combine(
                commentDao.getComment(itemId),
                getProfileFlow().map { it?.blockedProfiles },
            ) { commentEntity, blockedProfiles ->
                commentEntity?.toDomain()?.takeIf {
                    it.author?.isBlocked(blockedProfiles) != true
                }
            }
        }

    override suspend fun getLocalItems(feedItems: List<Feed.Item>) =
        with(commentMapper) {
            combine(
                commentDao.getCommentsByIdsChunked(feedItems.map { it.itemId }),
                getProfileFlow().map { it?.blockedProfiles },
            ) { commentEntities, blockedProfiles ->
                feedItems
                    .joinLocalEntities(
                        entities = commentEntities,
                        entityId = { it.comment.id },
                    ) { _, commentEntity -> commentEntity.toDomain() }
                    .filter {
                        it.author?.isBlocked(blockedProfiles) != true
                    }
            }.flowOn(Dispatchers.Default)
        }

    /**
     * Observes cached comments in an inclusive range of feed positions, in feed order.
     * Missing comments and blocked authors are omitted, as in the unbounded flow.
     * Room still re-runs this bounded query on table invalidation; unchanged rows are
     * suppressed before domain mapping. The existing unbounded API is unaffected.
     */
    internal fun getLocalItemsFlow(
        feedId: String,
        minPosition: Int,
        maxPosition: Int,
    ): Flow<List<Comment>> =
        with(commentMapper) {
            combine(
                commentDao.getFeedComments(feedId, minPosition, maxPosition).distinctUntilChanged(),
                getProfileFlow().map { it?.blockedProfiles }.distinctUntilChanged(),
            ) { commentEntities, blockedProfiles ->
                commentEntities.mapNotNull { entity ->
                    entity.toDomain().takeIf { it.author?.isBlocked(blockedProfiles) != true }
                }
            }.flowOn(Dispatchers.Default)
        }

    override suspend fun upsertLocalItem(item: GetResponse): Comment =
        with(commentMapper) {
            item.toEntity().also { commentDao.upsertComment(it) }.toDomain()
        }

    override suspend fun upsertLocalItems(
        items: List<GetResponse>,
        childItems: List<GetResponse>,
    ) = commentDao.upsertCommentsAndReplies(
        comments = with(commentMapper) { items.map { it.toEntity() } },
        replies = with(replyMapper) { childItems.map { it.toEntity() } },
    )

    override suspend fun updateLocalCounters(counters: GetResponse) =
        commentDao.updateCommentCounters(
            counters = with(commentMapper) { counters.toCounterUpdateEntity() },
        )

    override suspend fun updateLocalCounters(
        counters: List<GetResponse>,
        childCounters: List<GetResponse>,
    ) = commentDao.updateCommentsAndRepliesCounters(
        counters = with(commentMapper) { counters.map { it.toCounterUpdateEntity() } },
        repliesCounters = with(replyMapper) { childCounters.map { it.toCounterUpdateEntity() } },
    )

    override suspend fun updateLocalReaction(
        item: Comment,
        reaction: Reaction?,
        reactions: List<ReactionCount>,
    ) = with(reactionMapper) {
        commentDao.updateCommentReaction(
            id = item.id,
            reactions = reactions.map { it.toEntity() },
            reactionId = reaction?.id,
            reactionUnicode = reaction?.kind?.unicode,
        )
    }

    override suspend fun sendComment(
        draftComment: Comment.Draft,
        isParentTranslated: Boolean,
    ): OctopusResult<Comment, Comment.CreationError> =
        catchingMediaEncoding(MediaEncodingException::toCommentFileError) {
            callWithValidation(commentMapper) {
                val response =
                    objectService.putComment(
                        comment = draftComment.toProto(),
                        parentIsTranslated = isParentTranslated,
                    )
                when {
                    response.hasSuccess() -> {
                        val createdComment =
                            response.success.comment
                                .toEntity()
                                .also {
                                    commentDao.upsertComment(it)
                                }.toDomain()
                                .copy(
                                    post = draftComment.post,
                                )
                        // PutComment's response doesn't carry `RequesterCtx` — re-fetch immediately to
                        // hydrate the real permissions before observers wake up. iso iOS internal change.
                        //
                        // `runCatching` is intentional: fire-and-forget hydrate, see PostRepositoryImpl
                        // for the full rationale. Must remain the last statement of the success branch.
                        runCatching {
                            fetchCounters(
                                itemId = createdComment.id,
                                hasVideo = createdComment.medias?.video != null,
                                incrementViewCount = false,
                            )
                        }
                        createdComment
                    }

                    else -> return OctopusResult.Failure.InvalidArguments(
                        errors = response.fail.errorsList.map { it.toDomain() },
                    )
                }
            }
        }

    override suspend fun deleteComment(comment: Comment) =
        call(commentMapper) {
            objectService.deleteComment(comment.id)
            deleteLocalItem(comment.id)
        }

    override suspend fun deleteLocalItem(itemId: String) {
        commentDao.deleteComment(itemId)
    }

    override suspend fun deleteLocalOrphanedItems() {
        commentDao.deleteOrphanedComments()
    }

    override suspend fun resetCache() {
        commentDao.resetUpdatedAt()
    }
}
