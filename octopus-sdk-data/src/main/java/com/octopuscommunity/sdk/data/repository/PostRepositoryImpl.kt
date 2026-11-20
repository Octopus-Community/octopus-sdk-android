package com.octopuscommunity.sdk.data.repository

import android.util.Log
import com.octopuscommunity.OctoObjectServiceProto.GetOrCreateBridgePostResponse
import com.octopuscommunity.OctoObjectServiceProto.GetResponse
import com.octopuscommunity.sdk.data.local.dao.CommentDao
import com.octopuscommunity.sdk.data.local.dao.FeedDao
import com.octopuscommunity.sdk.data.local.dao.PostDao
import com.octopuscommunity.sdk.data.local.dao.ProfileDao
import com.octopuscommunity.sdk.data.local.dao.UserConfigDao
import com.octopuscommunity.sdk.data.local.dao.getCommentsByIdsChunked
import com.octopuscommunity.sdk.data.local.dao.getPostsByIdsChunked
import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.data.local.datastore.SecuredDataStore
import com.octopuscommunity.sdk.data.mapper.CommentMapper
import com.octopuscommunity.sdk.data.mapper.EncodedImage
import com.octopuscommunity.sdk.data.mapper.FeedMapper
import com.octopuscommunity.sdk.data.mapper.MediaEncodingException
import com.octopuscommunity.sdk.data.mapper.PostMapper
import com.octopuscommunity.sdk.data.mapper.ProfileMapper
import com.octopuscommunity.sdk.data.mapper.ReactionMapper
import com.octopuscommunity.sdk.data.mapper.UserConfigMapper
import com.octopuscommunity.sdk.data.mapper.UserMapper
import com.octopuscommunity.sdk.data.mapper.catchingMediaEncoding
import com.octopuscommunity.sdk.data.mapper.toClientPostError
import com.octopuscommunity.sdk.data.mapper.toPostFileError
import com.octopuscommunity.sdk.data.mapper.toRwOctoObject
import com.octopuscommunity.sdk.data.remote.service.FeedService
import com.octopuscommunity.sdk.data.remote.service.NotificationService
import com.octopuscommunity.sdk.data.remote.service.ObjectService
import com.octopuscommunity.sdk.data.remote.service.UserService
import com.octopuscommunity.sdk.data.remote.upload.HttpImageUploadClient
import com.octopuscommunity.sdk.data.utils.call
import com.octopuscommunity.sdk.data.utils.callWithValidation
import com.octopuscommunity.sdk.data.utils.joinLocalEntities
import com.octopuscommunity.sdk.domain.BridgeShareTokenProviderHolder
import com.octopuscommunity.sdk.domain.model.ClientPost
import com.octopuscommunity.sdk.domain.model.CreationSource
import com.octopuscommunity.sdk.domain.model.Feed
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount
import com.octopuscommunity.sdk.domain.model.OctopusReactionKind
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.OctopusResult.Failure
import com.octopuscommunity.sdk.domain.network.mapError
import com.octopuscommunity.sdk.domain.repository.ClientPostError
import com.octopuscommunity.sdk.domain.repository.ClientPostError.ClientObjectError
import com.octopuscommunity.sdk.domain.repository.PollVoteError
import com.octopuscommunity.sdk.domain.repository.PostCreationError
import com.octopuscommunity.sdk.domain.repository.PostRepository
import com.octopuscommunity.sdk.domain.repository.SetReactionError
import io.grpc.Status
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class PostRepositoryImpl(
    securedDataStore: SecuredDataStore,
    preferencesDataStore: PreferencesDataStore,
    userConfigDao: UserConfigDao,
    profileDao: ProfileDao,
    feedDao: FeedDao,
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    userService: UserService,
    notificationService: NotificationService,
    feedService: FeedService,
    objectService: ObjectService,
    userMapper: UserMapper,
    userConfigMapper: UserConfigMapper,
    profileMapper: ProfileMapper,
    feedMapper: FeedMapper,
    reactionMapper: ReactionMapper,
    private val postMapper: PostMapper,
    private val commentMapper: CommentMapper,
    private val bridgeShareTokenProviderHolder: BridgeShareTokenProviderHolder,
) : OctopusItemRepositoryImpl<Post, Post.Draft, PostCreationError>(
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
    PostRepository {
    /** Uploads the photos of a post to storage before the post is created, see [PostImageUploader]. */
    private val imageUploader =
        PostImageUploader(
            createUploads = { objectService.createImageUploads(it) },
            client = HttpImageUploadClient(),
        )

    override fun getLocalItemFlow(itemId: String): Flow<Post?> =
        with(postMapper) {
            combine(
                postDao.getPostFlow(itemId),
                getProfileFlow().map { it?.blockedProfiles },
            ) { postEntity, blockedProfiles ->
                postEntity?.toDomain()?.takeIf {
                    it.author?.isBlocked(blockedProfiles) != true
                }
            }
        }

    override suspend fun getLocalItems(feedItems: List<Feed.Item>) =
        with(postMapper) {
            combine(
                postDao.getPostsByIdsChunked(feedItems.map { it.itemId }),
                commentDao.getCommentsByIdsChunked(feedItems.mapNotNull { it.featuredChildId }),
                getProfileFlow().map { it?.blockedProfiles },
            ) { postEntities, commentEntities, blockedProfiles ->
                feedItems
                    .joinLocalEntities(
                        entities = postEntities,
                        entityId = { it.post.id },
                        children = commentEntities,
                        childId = { it.comment.id },
                    ) { _, postEntity, commentEntity ->
                        postEntity.toDomain().copy(
                            featuredComment =
                                with(commentMapper) {
                                    commentEntity?.toDomain()?.takeIf {
                                        it.author?.isBlocked(blockedProfiles) != true
                                    }
                                },
                        )
                    }.filter {
                        it.author?.isBlocked(blockedProfiles) != true
                    }
            }.flowOn(Dispatchers.Default)
        }

    override suspend fun upsertLocalItem(item: GetResponse): Post =
        with(postMapper) {
            item.toEntity().also { postDao.upsertPost(it) }.toDomain()
        }

    override suspend fun upsertLocalItems(
        items: List<GetResponse>,
        childItems: List<GetResponse>,
    ) = postDao.upsertPostsAndComments(
        posts = with(postMapper) { items.map { it.toEntity() } },
        comments = with(commentMapper) { childItems.map { it.toEntity() } },
    )

    override suspend fun updateLocalCounters(counters: GetResponse) =
        postDao.updatePostCounters(
            counters = with(postMapper) { counters.toCountersUpdateEntity() },
        )

    override suspend fun updateLocalCounters(
        counters: List<GetResponse>,
        childCounters: List<GetResponse>,
    ) = postDao.updatePostsAndCommentsCounters(
        counters = with(postMapper) { counters.map { it.toCountersUpdateEntity() } },
        commentsCounters = with(commentMapper) { childCounters.map { it.toCounterUpdateEntity() } },
    )

    override suspend fun updateLocalReaction(
        item: Post,
        reaction: Reaction?,
        reactions: List<ReactionCount>,
    ) = with(postMapper) {
        postDao.upsertPost(
            post =
                item
                    .copy(
                        userReaction = reaction,
                        reactions = reactions,
                    ).toEntity(),
        )
    }

    override suspend fun sendPost(
        draftItem: Post.Draft,
        creationSource: CreationSource,
    ): OctopusResult<Post, PostCreationError> {
        draftImageCountError(draftItem)?.let { error -> return Failure.InvalidArguments(errors = listOf(error)) }
        return catchingMediaEncoding(MediaEncodingException::toPostFileError) {
            callWithValidation(mapper = postMapper) {
                // Bridge / Share-in-game: when the host registered a signature provider and this is a
                // prefilled share carrying an image, sign the final content so the server accepts the
                // image in a community that forbids member pictures. A null token (or no provider)
                // sends the post unsigned and the server decides: it reads the token only when the
                // community forbids pictures. Throwing sends nothing and the editor shows an error.
                val clientToken =
                    if (creationSource == CreationSource.PREFILLED_FROM_CLIENT && draftItem.images.isNotEmpty()) {
                        bridgeShareTokenProviderHolder.tokenProvider?.let { provider ->
                            val fingerprint = with(postMapper) { draftItem.toBridgeShareFingerprint() }
                            try {
                                provider.invoke(fingerprint)
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                Log.e("OctopusRepository", "Prefilled share signing failed", exception)
                                return Failure.StatusError(
                                    code = Status.Code.UNKNOWN.value(),
                                    description = exception.message?.ifEmpty { null },
                                    exception = exception,
                                )
                            }
                        }
                    } else {
                        null
                    }
                // Resolved before the photos are compressed and uploaded, so a draft without a group
                // fails before them (the prefilled-share signature above, as before, comes first).
                val parentId = draftItem.group!!.id
                val pressedEnterAt = System.currentTimeMillis()
                // Every photo goes to storage first, then the post references the uploads in order.
                val images =
                    when (val upload = imageUploader.upload(draftItem.encodeImages())) {
                        is PostImageUploader.Result.Uploaded -> upload.images
                        is PostImageUploader.Result.Refused ->
                            return Failure.InvalidArguments(errors = upload.errors.map { it.toPostFileError() })
                    }
                val response =
                    objectService.putPost(
                        octopusObject =
                            draftItem.toRwOctoObject(
                                parentId = parentId,
                                pressedEnterAt = pressedEnterAt,
                                encodedImages = images,
                            ),
                        creationSource = creationSource.toProto(),
                        clientToken = clientToken,
                    )
                when {
                    response.hasSuccess() -> {
                        val createdPost =
                            response.success.post
                                .toEntity()
                                .also {
                                    postDao.upsertPost(it)
                                }.toDomain()
                                .copy(
                                    group = draftItem.group,
                                )
                        // PutPost's response doesn't carry `RequesterCtx`, so the post lands in
                        // the DB with default-open permissions (canAccess = canCreateChildren =
                        // true). Re-fetch immediately to persist the real entitlements before the
                        // observers wake up — otherwise the feed would flash the Comment button
                        // for a post created in a no-comments group until the cell's onAppear
                        // triggers its own counter fetch. iso iOS internal change.
                        //
                        // `runCatching` is intentional and must remain the last statement of the
                        // success branch: this is a fire-and-forget best-effort hydrate. Any
                        // failure (transient network, auth refresh, even CancellationException
                        // if the caller is cancelled mid-flight) is swallowed because the send
                        // itself already succeeded server-side and the cell's onAppear will
                        // retry the fetch later. Don't move logic *after* this block — the
                        // swallowed CancellationException would mask coroutine cancellations.
                        runCatching {
                            fetchCounters(
                                itemId = createdPost.id,
                                hasVideo = createdPost.medias?.video != null,
                                incrementViewCount = false,
                            )
                        }
                        createdPost
                    }

                    else -> return Failure.InvalidArguments(
                        errors = response.fail.errorsList.map { it.toDomain() },
                    )
                }
            }
        }
    }

    override fun getClientObjectPostFlow(clientObjectId: String): Flow<Post?> =
        with(postMapper) {
            postDao.getPostByClientObject(clientObjectId = clientObjectId).map { it?.toDomain() }
        }

    override suspend fun fetchOrCreateClientObjectPost(
        clientPost: ClientPost,
        tokenProvider: (suspend (String) -> String?)?,
    ): OctopusResult<Post, ClientPostError> {
        clientPostImageCountError(clientPost)?.let { error -> return Failure.InvalidArguments(errors = listOf(error)) }
        return with(postMapper) {
            // First try to get the post if it already exists
            fetchBridgePost(clientObjectId = clientPost.objectId)
                .mapError { _: ClientObjectError.PostNotFound ->
                    // Post not found: read the local images once — the fingerprint hashes the very
                    // bytes the upload sends — ask for the token, then create. An image that cannot
                    // be read refuses the post before the token is asked for.
                    withLocalImagesInMemory {
                        val localImages =
                            when (val read = clientPost.encodeLocalImages()) {
                                is OctopusResult.Success -> read.data
                                is OctopusResult.Failure -> return@withLocalImagesInMemory read
                            }
                        createBridgePost(
                            clientPost = clientPost,
                            localImages = localImages,
                            clientToken = tokenProvider?.invoke(clientPost.toFingerprint(localImages.toMap())),
                        )
                    }.mapError { _: ClientObjectError.PostAlreadyExists ->
                        // Race condition: another caller created the post between our get and
                        // create. Retry the fetch
                        fetchBridgePost(clientObjectId = clientPost.objectId)
                    }
                }
        }
    }

    private suspend fun fetchBridgePost(clientObjectId: String): OctopusResult<Post, ClientPostError> =
        call(postMapper) {
            val response = objectService.getBridgePost(clientObjectId)
            when {
                response.hasSuccess() -> saveBridgePost(response.success)
                else -> return Failure.InvalidArguments(
                    errors = response.fail.errorsList.map { it.toDomain() },
                )
            }
        }

    /**
     * Creates the bridge post of [clientPost]: only a post that is really created uploads its
     * [localImages] (read by [PostMapper.encodeLocalImages]); remote images stay URLs.
     */
    private suspend fun createBridgePost(
        clientPost: ClientPost,
        localImages: List<Pair<Int, EncodedImage>>,
        clientToken: String?,
    ): OctopusResult<Post, ClientPostError> =
        call(postMapper) {
            val response =
                when (
                    val creation =
                        createBridgePostWithUploads(
                            clientPost = clientPost,
                            localImages = localImages,
                            clientToken = clientToken,
                            uploader = imageUploader,
                            create = { post, token ->
                                objectService.createBridgePost(postBridge = post, groupId = clientPost.groupId, clientToken = token)
                            },
                        )
                ) {
                    is BridgePostCreation.Answered -> creation.response
                    is BridgePostCreation.Refused ->
                        return Failure.InvalidArguments(errors = creation.errors.map { it.toClientPostError() })
                }
            when {
                response.hasSuccess() -> saveBridgePost(response.success)
                else -> return Failure.InvalidArguments(
                    errors = response.fail.errorsList.map { it.toDomain() },
                )
            }
        }

    private suspend fun saveBridgePost(success: GetOrCreateBridgePostResponse.Success): Post =
        with(postMapper) {
            val postId = success.postBridge.id
            success
                .toEntity(postDao.getPost(postId)?.post)
                .also { postDao.upsertPost(it) }
                .toDomain()
        }

    override suspend fun deletePost(post: Post): OctopusResult<Unit, Nothing> =
        call(postMapper) {
            objectService.deletePost(post.id)
            deleteLocalItem(post.id)
        }

    override suspend fun deleteLocalItem(itemId: String) {
        postDao.deletePost(itemId)
    }

    override suspend fun deleteLocalOrphanedItems() {
        postDao.deleteOrphanedPosts()
    }

    override suspend fun resetCache() {
        postDao.resetUpdatedAt()
    }

    override suspend fun setReaction(
        reactionKind: OctopusReactionKind?,
        postId: String,
    ): OctopusResult<Reaction?, SetReactionError> {
        // Reject unknown reactions
        if (reactionKind is Reaction.Kind.Unknown) {
            return Failure.InvalidArguments(SetReactionError.UnknownReaction)
        }

        // Try to get the post locally first
        val localPost = with(postMapper) { postDao.getPost(id = postId)?.toDomain() }

        // On local miss, fetch from remote. Propagate Connection failures rather than collapsing
        // them to "post not found" — mirrors iOS internal change's `PostsRepository.set(reaction:postId:)`
        // refinement that distinguishes the underlying cause.
        //
        // Special case: a server NOT_FOUND surfaces on Android as `Failure.ContentUnavailable`
        // (see `RepositoryUtils.kt`) — map it to `SetReactionError.PostNotFound` so the
        // typed-error contract matches iOS (`.postNotFound`). Other Connection failures
        // (NoNetwork, UserNotAuthenticated, StatusError, PermissionDenied) stay orthogonal in
        // `OctopusResult.Failure.Connection.*`, per Android SDK convention.
        val post: Post =
            localPost ?: run {
                val fetchResult =
                    fetchItem(
                        itemId = postId,
                        hasVideo = false,
                        incrementViewCount = false,
                    )
                when (fetchResult) {
                    is OctopusResult.Success -> fetchResult.data
                    is OctopusResult.Failure.ContentUnavailable ->
                        return Failure.InvalidArguments(SetReactionError.PostNotFound)
                    is OctopusResult.Failure.Connection -> return fetchResult
                    is OctopusResult.Failure.InvalidArguments -> error("fetchItem<Nothing> cannot have InvalidArguments")
                }
            } ?: return Failure.InvalidArguments(SetReactionError.PostNotFound)

        // No bridge precondition — works on any post (bridge or community).
        // isContentTranslated stays null — matches today's behavior, mirrors iOS internal change.
        return setReaction(
            item = post,
            reactionKind = reactionKind as? Reaction.Kind,
            isContentTranslated = null,
        )
    }

    override suspend fun pollVote(
        post: Post,
        answer: Post.Poll.Answer,
        isPostTranslated: Boolean,
    ): OctopusResult<Post, PollVoteError> =
        call(postMapper) {
            val optimisticPost =
                post.copy(
                    pollVoteId = answer.id,
                    poll =
                        post.poll?.let { poll ->
                            poll.copy(
                                results =
                                    poll.results.map { result ->
                                        if (result.answerId == answer.id) {
                                            // Increment the vote count for the selected answer
                                            result.copy(voteCount = result.voteCount + 1)
                                        } else {
                                            result
                                        }
                                    },
                            )
                        },
                )
            postDao.upsertPost(optimisticPost.toEntity())

            try {
                val response =
                    objectService.pollVote(
                        parentId = post.id,
                        answerId = answer.id,
                        isParentTranslated = isPostTranslated,
                    )
                when {
                    response.hasSuccess() ->
                        optimisticPost
                            .copy(
                                pollVoteId = response.success.pollVote.content.vote.pollAnswerId,
                            ).also {
                                postDao.upsertPost(it.toEntity())
                            }

                    else -> {
                        postDao.upsertPost(post.toEntity())
                        return Failure.InvalidArguments(
                            errors = response.fail.errorsList.map { it.toDomain() },
                        )
                    }
                }
            } catch (e: Exception) {
                // If there's an error, rollback
                postDao.upsertPost(post.toEntity())
                throw e
            }
        }
}
