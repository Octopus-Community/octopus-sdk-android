package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.FeedServiceProto.GetFeedWithOctoObjectPageResponse
import com.octopuscommunity.GrpcModelProto.Aggregate
import com.octopuscommunity.GrpcModelProto.OctoObject
import com.octopuscommunity.commentOrNull
import com.octopuscommunity.postOrNull
import com.octopuscommunity.replyOrNull
import com.octopuscommunity.sdk.data.local.dao.CommentDao
import com.octopuscommunity.sdk.data.local.dao.PostDao
import com.octopuscommunity.sdk.data.local.dao.ReplyDao
import com.octopuscommunity.sdk.data.local.entity.CommentEntity
import com.octopuscommunity.sdk.data.local.entity.PostEntity
import com.octopuscommunity.sdk.data.local.entity.ReplyEntity
import com.octopuscommunity.sdk.data.mapper.CommentMapper
import com.octopuscommunity.sdk.data.mapper.PostMapper
import com.octopuscommunity.sdk.data.mapper.ReplyMapper
import com.octopuscommunity.sdk.data.remote.service.FeedService
import com.octopuscommunity.sdk.data.resolver.UserCommentContext
import com.octopuscommunity.sdk.data.resolver.UserCommentsResolver
import com.octopuscommunity.sdk.data.utils.call
import com.octopuscommunity.sdk.domain.model.UserComment
import com.octopuscommunity.sdk.domain.model.UserCommentsPage
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.UserCommentRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first

/**
 * Fetches the connected user's authored comments & replies (the profile "Comments" tab, internal ticket) via
 * the `WithOctoObject` feed, mapping each item onto a [UserComment] with its resolved parent context.
 *
 * Pagination is cursor-driven, and this layer owns the empty-page problem: a page whose entries are
 * all dropped (server-side ACL filtering, or an entry whose anchor post was not shipped) is not the
 * end of the feed, so [firstPage]/[nextPage] page ahead themselves until they have something
 * renderable or the cursor runs out. Callers get the plain contract "a returned page is either
 * non-empty or the end" and need no empty-page special case - keeping that rule out of the public
 * API, where both first-party UIs had already got it wrong in different ways.
 *
 * The feed itself is not cached as a feed (each page is fetched fresh, mirroring iOS's
 * `UserCommentsRepository`), but every octo object it ships - the authored comments/replies and their
 * parent posts/comments - is upserted into the shared `posts`/`comments`/`replies` tables so the tab's
 * cells observe live measures (view/comment/reaction counts) the same way the main feed does.
 *
 * The upsert never overwrites an existing user reaction: the `WithOctoObject` response carries no
 * `RequesterCtx` (so we have no authoritative reaction info for it), so each entity is merged with
 * whatever is already cached for that id before being written back - mirroring iOS's
 * `UserCommentsRepository.persist` (`OctoObjectEntity.fill` only writes user interactions when
 * non-`nil`) and this codebase's own `PostRepositoryImpl.saveBridgePost` idiom
 * (`GetOrCreateBridgePostResponse.Success.toEntity(oldPost)` carries over `userReaction`/`userVoteId`
 * from the previously cached row rather than the fresh, less-informed one).
 */
class UserCommentRepositoryImpl(
    private val feedService: FeedService,
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val replyDao: ReplyDao,
    private val postMapper: PostMapper,
    private val commentMapper: CommentMapper,
    private val replyMapper: ReplyMapper,
) : UserCommentRepository {
    override suspend fun firstPage(
        feedId: String,
        pageSize: Int,
    ): OctopusResult<UserCommentsPage, Nothing> =
        call {
            renderableUserCommentsPage(
                first = page(feedService.initializeFeedWithOctoObject(feedId = feedId, pageSize = pageSize)),
                nextPage = { cursor -> page(fetchPage(pageCursor = cursor, pageSize = pageSize)) },
            )
        }

    override suspend fun nextPage(
        pageCursor: String,
        pageSize: Int,
    ): OctopusResult<UserCommentsPage, Nothing> =
        call {
            renderableUserCommentsPage(
                first = page(fetchPage(pageCursor = pageCursor, pageSize = pageSize)),
                nextPage = { cursor -> page(fetchPage(pageCursor = cursor, pageSize = pageSize)) },
            )
        }

    private suspend fun fetchPage(
        pageCursor: String,
        pageSize: Int,
    ) = feedService.getFeedWithOctoObjectPage(
        pageCursor = pageCursor,
        pageSize = pageSize,
        fetchAggregates = true,
    )

    private suspend fun page(response: GetFeedWithOctoObjectPageResponse): UserCommentsPage {
        val aggregatesById =
            (response.aggregatesList + response.relatedAggregatesList)
                .associate { it.itemId to it.aggregate }

        // Entries with no anchor post (a reply whose grandparent post wasn't shipped) have no context
        // to render, so they are dropped here rather than published as a nullable `post` every caller
        // has to defend against. Dropped before `persist` too, so the cache only holds what the tab
        // can actually show.
        val renderable =
            UserCommentsResolver
                .resolve(
                    feed = response.feedList,
                    relatedObjects = response.relatedObjectsList,
                ).withAnchorPost()

        persist(contexts = renderable.map { (context, _) -> context }, aggregatesById = aggregatesById)

        return UserCommentsPage(
            comments = renderable.map { (context, anchorPost) -> userComment(context, anchorPost, aggregatesById) },
            nextPageCursor = response.nextPageCursor.ifEmpty { null },
        )
    }

    private suspend fun userComment(
        context: UserCommentContext,
        anchorPost: OctoObject,
        aggregatesById: Map<String, Aggregate>,
    ): UserComment =
        when (context) {
            is UserCommentContext.OfComment -> {
                val comment = comment(context.item, aggregatesById)
                UserComment(
                    id = comment.id,
                    updateDate = comment.updateDate,
                    content = UserComment.Content.OfComment(comment),
                    post = post(anchorPost, aggregatesById),
                    parentComment = null,
                )
            }

            is UserCommentContext.OfReply -> {
                val reply = reply(context.item, aggregatesById)
                UserComment(
                    id = reply.id,
                    updateDate = reply.updateDate,
                    content = UserComment.Content.OfReply(reply),
                    post = post(anchorPost, aggregatesById),
                    parentComment = comment(context.parentComment, aggregatesById),
                )
            }
        }

    private suspend fun post(
        octo: OctoObject,
        aggregatesById: Map<String, Aggregate>,
    ) = with(postMapper) {
        octo
            .toEntity()
            .copy(counters = aggregatesById[octo.id]?.toEntity() ?: PostEntity.CountersEntity())
            .toDomain()
    }

    private suspend fun comment(
        octo: OctoObject,
        aggregatesById: Map<String, Aggregate>,
    ) = with(commentMapper) {
        octo
            .toEntity()
            .copy(counters = aggregatesById[octo.id]?.toEntity() ?: CommentEntity.CountersEntity())
            .toDomain()
    }

    private suspend fun reply(
        octo: OctoObject,
        aggregatesById: Map<String, Aggregate>,
    ) = with(replyMapper) {
        octo
            .toEntity()
            .copy(counters = aggregatesById[octo.id]?.toEntity() ?: ReplyEntity.CountersEntity())
            .toDomain()
    }

    /**
     * Upserts every octo object carried by the page (authored comments/replies + their parent
     * posts/comments) into the shared content tables, so the UI can observe their live measures.
     * Classified by content oneof - a given id is written to exactly one of the three tables.
     */
    private suspend fun persist(
        contexts: List<UserCommentContext>,
        aggregatesById: Map<String, Aggregate>,
    ) {
        val posts = LinkedHashMap<String, OctoObject>()
        val comments = LinkedHashMap<String, OctoObject>()
        val replies = LinkedHashMap<String, OctoObject>()

        fun classify(octo: OctoObject) {
            when {
                octo.content.commentOrNull != null -> comments[octo.id] = octo
                octo.content.replyOrNull != null -> replies[octo.id] = octo
                octo.content.postOrNull != null -> posts[octo.id] = octo
            }
        }

        for (context in contexts) {
            classify(context.item)
            when (context) {
                is UserCommentContext.OfComment -> classify(context.post)
                is UserCommentContext.OfReply -> {
                    classify(context.parentComment)
                    context.post?.let(::classify)
                }
            }
        }

        // The cached rows are read in one query per table, not one per id. A page carries up to three
        // objects per entry (the authored comment/reply, its parent comment, its anchor post), and
        // [renderableUserCommentsPage] can chain up to `MAX_EMPTY_PAGE_LOOKAHEAD + 1` pages inside a
        // single call — so per-id point reads meant up to 21 x (3 x pageSize) sequential Room round
        // trips behind one scroll tick, all of them on the caller's thread.
        val existingPosts =
            if (posts.isEmpty()) {
                emptyMap()
            } else {
                postDao
                    .getPostsByIds(posts.keys.toList())
                    .first()
                    .associate { it.post.id to it.post }
            }
        val existingComments =
            if (comments.isEmpty()) {
                emptyMap()
            } else {
                commentDao
                    .getCommentsByIds(comments.keys.toList())
                    .first()
                    .associate { it.comment.id to it.comment }
            }
        val existingReplies =
            if (replies.isEmpty()) {
                emptyMap()
            } else {
                replyDao
                    .getRepliesByIds(replies.keys.toList())
                    .first()
                    .associate { it.reply.id to it.reply }
            }

        val postEntities =
            posts.values.map { octo ->
                with(postMapper) {
                    octo
                        .toEntity()
                        .copy(counters = aggregatesById[octo.id]?.toEntity() ?: PostEntity.CountersEntity())
                }.preservingReactionOf(existingPosts[octo.id])
            }
        val commentEntities =
            comments.values.map { octo ->
                with(commentMapper) {
                    octo
                        .toEntity()
                        .copy(counters = aggregatesById[octo.id]?.toEntity() ?: CommentEntity.CountersEntity())
                }.preservingReactionOf(existingComments[octo.id])
            }
        val replyEntities =
            replies.values.map { octo ->
                with(replyMapper) {
                    octo
                        .toEntity()
                        .copy(counters = aggregatesById[octo.id]?.toEntity() ?: ReplyEntity.CountersEntity())
                }.preservingReactionOf(existingReplies[octo.id])
            }

        if (postEntities.isNotEmpty()) postDao.upsertPosts(postEntities)
        if (commentEntities.isNotEmpty()) commentDao.upsertComments(commentEntities)
        if (replyEntities.isNotEmpty()) replyDao.upsertReplies(replyEntities)
    }
}

/** Upstream pages consumed at most, per call, looking for one that yields something renderable. */
internal const val MAX_EMPTY_PAGE_LOOKAHEAD = 20

/**
 * Follows the cursor while pages yield nothing renderable, so the page handed back honours the
 * "either non-empty, or the end of the feed" contract ([UserCommentsPage]).
 *
 * Kept a pure function of [first] and [nextPage] (rather than a method reaching for the feed service)
 * so the rule the whole contract rests on is unit-testable without the gRPC/Room graph behind it.
 *
 * Bounded by [maxLookahead] so a pathological feed (an unbounded run of fully-filtered pages) can't
 * spin forever. On hitting that bound the cursor is dropped rather than handed back live: an empty page
 * *with* a cursor is exactly the state this contract exists to forbid, and it would strand the caller
 * on an empty list with no trigger left to resume paging.
 */
internal suspend fun renderableUserCommentsPage(
    first: UserCommentsPage,
    maxLookahead: Int = MAX_EMPTY_PAGE_LOOKAHEAD,
    nextPage: suspend (cursor: String) -> UserCommentsPage,
): UserCommentsPage {
    var page = first
    var lookahead = 0
    while (page.comments.isEmpty()) {
        // Each iteration is a network round trip plus a page-worth of Room upserts, so a scope that
        // died mid-lookahead (the tab left, the view model cleared) must stop here rather than run the
        // remaining budget out. `nextPage` itself may well be cancellable, but that is the caller's
        // choice, not this loop's guarantee.
        currentCoroutineContext().ensureActive()
        val cursor = page.nextPageCursor ?: return page // genuine end of feed
        if (lookahead++ >= maxLookahead) return page.copy(nextPageCursor = null)
        page = nextPage(cursor)
    }
    return page
}

/**
 * Pairs each entry with the post it hangs under, dropping the entries that have none — a reply whose
 * grandparent post was not shipped by the server has no context to render, so it never reaches a
 * caller (which is what lets [UserComment.post] be non-null).
 */
internal fun List<UserCommentContext>.withAnchorPost(): List<Pair<UserCommentContext, OctoObject>> =
    mapNotNull { context -> context.anchorPost?.let { context to it } }

private val UserCommentContext.anchorPost: OctoObject?
    get() =
        when (this) {
            is UserCommentContext.OfComment -> post
            is UserCommentContext.OfReply -> post
        }

/**
 * Carries over the requester-scoped reaction fields, and the requester-scoped `canAccess` /
 * `canCreateChildren` permission flags, from [existing] (the row already cached, if any) onto this
 * freshly-mapped entity, instead of the `null`/default-open values [PostMapper.toEntity] fills in for
 * them (the `WithOctoObject` feed ships no `RequesterCtx`, so a fresh mapping alone can't know the real
 * per-user reaction or permissions - see internal ticket hotfix / iOS internal change for the same bug class). No-op
 * (keeps the fresh, permission-open defaults) for a never-before-seen id.
 */
internal fun PostEntity.preservingReactionOf(existing: PostEntity?): PostEntity =
    copy(
        userReaction = existing?.userReaction,
        userVoteId = existing?.userVoteId,
        canAccess = existing?.canAccess ?: canAccess,
        canCreateChildren = existing?.canCreateChildren ?: canCreateChildren,
    )

/** @see PostEntity.preservingReactionOf */
internal fun CommentEntity.preservingReactionOf(existing: CommentEntity?): CommentEntity =
    copy(
        userReaction = existing?.userReaction,
        canAccess = existing?.canAccess ?: canAccess,
        canCreateChildren = existing?.canCreateChildren ?: canCreateChildren,
    )

/** @see PostEntity.preservingReactionOf */
internal fun ReplyEntity.preservingReactionOf(existing: ReplyEntity?): ReplyEntity =
    copy(
        userReaction = existing?.userReaction,
        canAccess = existing?.canAccess ?: canAccess,
        canCreateChildren = existing?.canCreateChildren ?: canCreateChildren,
    )
