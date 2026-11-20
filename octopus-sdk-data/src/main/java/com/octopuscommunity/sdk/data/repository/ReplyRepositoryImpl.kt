package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.OctoObjectServiceProto.GetResponse
import com.octopuscommunity.sdk.data.local.dao.FeedDao
import com.octopuscommunity.sdk.data.local.dao.ProfileDao
import com.octopuscommunity.sdk.data.local.dao.ReplyDao
import com.octopuscommunity.sdk.data.local.dao.UserConfigDao
import com.octopuscommunity.sdk.data.local.dao.getRepliesByIdsChunked
import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.data.local.datastore.SecuredDataStore
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
import com.octopuscommunity.sdk.domain.model.Comment.Reply
import com.octopuscommunity.sdk.domain.model.Feed
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ReplyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ReplyRepositoryImpl(
    securedDataStore: SecuredDataStore,
    preferencesDataStore: PreferencesDataStore,
    userConfigDao: UserConfigDao,
    profileDao: ProfileDao,
    feedDao: FeedDao,
    private val replyDao: ReplyDao,
    userService: UserService,
    notificationService: NotificationService,
    feedService: FeedService,
    objectService: ObjectService,
    userMapper: UserMapper,
    userConfigMapper: UserConfigMapper,
    profileMapper: ProfileMapper,
    feedMapper: FeedMapper,
    reactionMapper: ReactionMapper,
    private val replyMapper: ReplyMapper,
) : OctopusItemRepositoryImpl<Reply, Reply.Draft, Reply.CreationError>(
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
    ReplyRepository {
    override fun getLocalItemFlow(itemId: String): Flow<Reply?> =
        with(replyMapper) {
            combine(
                replyDao.getReply(itemId),
                getProfileFlow().map { it?.blockedProfiles },
            ) { replyEntity, blockedProfiles ->
                replyEntity?.toDomain()?.takeIf {
                    it.author?.isBlocked(blockedProfiles) != true
                }
            }
        }

    override suspend fun getLocalItems(feedItems: List<Feed.Item>) =
        with(replyMapper) {
            combine(
                replyDao.getRepliesByIdsChunked(feedItems.map { it.itemId }),
                getProfileFlow().map { it?.blockedProfiles },
            ) { replyEntities, blockedProfiles ->
                feedItems
                    .joinLocalEntities(
                        entities = replyEntities,
                        entityId = { it.reply.id },
                    ) { _, replyEntity -> replyEntity.toDomain() }
                    .filter {
                        it.author?.isBlocked(blockedProfiles) != true
                    }
            }.flowOn(Dispatchers.Default)
        }

    override suspend fun upsertLocalItem(item: GetResponse): Reply =
        with(replyMapper) {
            item.toEntity().also { replyDao.upsertReply(it) }.toDomain()
        }

    override suspend fun upsertLocalItems(
        items: List<GetResponse>,
        childItems: List<GetResponse>,
    ) = replyDao.upsertReplies(
        replies = with(replyMapper) { items.map { it.toEntity() } },
    )

    override suspend fun updateLocalCounters(counters: GetResponse) =
        replyDao.updateReplyCounters(
            counters = with(replyMapper) { counters.toCounterUpdateEntity() },
        )

    override suspend fun updateLocalCounters(
        counters: List<GetResponse>,
        childCounters: List<GetResponse>,
    ) = replyDao.updateRepliesCounters(
        counters = with(replyMapper) { counters.map { it.toCounterUpdateEntity() } },
    )

    override suspend fun updateLocalReaction(
        item: Reply,
        reaction: Reaction?,
        reactions: List<ReactionCount>,
    ) = with(replyMapper) {
        replyDao.upsertReply(
            reply =
                item
                    .copy(
                        userReaction = reaction,
                        reactions = reactions,
                    ).toEntity(),
        )
    }

    override suspend fun sendReply(
        draftItem: Reply.Draft,
        isParentTranslated: Boolean,
    ): OctopusResult<Reply, Reply.CreationError> =
        catchingMediaEncoding(MediaEncodingException::toReplyFileError) {
            callWithValidation(replyMapper) {
                val response =
                    objectService.putReply(
                        reply = draftItem.toProto(),
                        isParentTranslated = isParentTranslated,
                    )
                when {
                    response.hasSuccess() -> {
                        val createdReply =
                            response.success.reply
                                .toEntity()
                                .also {
                                    replyDao.upsertReply(it)
                                }.toDomain()
                                .copy(
                                    comment = draftItem.comment,
                                )
                        // PutReply's response doesn't carry `RequesterCtx` — re-fetch immediately
                        // to hydrate the real permissions before observers wake up. iso iOS internal change.
                        //
                        // `runCatching` is intentional: fire-and-forget hydrate, see
                        // PostRepositoryImpl for the full rationale. Must remain the last
                        // statement of the success branch.
                        runCatching {
                            fetchCounters(
                                itemId = createdReply.id,
                                hasVideo = createdReply.medias?.video != null,
                                incrementViewCount = false,
                            )
                        }
                        createdReply
                    }

                    else -> return OctopusResult.Failure.InvalidArguments(
                        errors = response.fail.errorsList.map { it.toDomain() },
                    )
                }
            }
        }

    override suspend fun deleteReply(reply: Reply): OctopusResult<Unit, Nothing> =
        call(replyMapper) {
            objectService.deleteReply(reply.id)
            deleteLocalItem(reply.id)
        }

    override suspend fun deleteLocalItem(itemId: String) {
        replyDao.deleteReply(itemId)
    }

    override suspend fun deleteLocalOrphanedItems() {
        replyDao.deleteOrphanedReplies()
    }

    override suspend fun resetCache() {
        replyDao.resetUpdatedAt()
    }
}
