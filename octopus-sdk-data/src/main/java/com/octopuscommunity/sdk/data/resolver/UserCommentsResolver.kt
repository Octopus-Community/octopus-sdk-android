package com.octopuscommunity.sdk.data.resolver

import com.octopuscommunity.GrpcModelProto.OctoObject
import com.octopuscommunity.commentOrNull
import com.octopuscommunity.replyOrNull

/**
 * A resolved entry of the profile "Comments" feed (internal ticket): a user's comment or reply together with
 * the parent context it hangs under, correlated from `GetFeedWithOctoObjectPageResponse.relatedObjects`.
 *
 * This is the raw (proto `OctoObject`-level) result of [UserCommentsResolver] — the repository layer
 * (`UserCommentRepositoryImpl`) maps it onto the domain models afterwards.
 */
internal sealed interface UserCommentContext {
    /** The feed item itself (the user's comment or reply). */
    val item: OctoObject

    /** A comment the user made on a post. */
    data class OfComment(
        override val item: OctoObject,
        val post: OctoObject,
    ) : UserCommentContext

    /**
     * A reply the user made to a comment.
     *
     * @property post The grandparent post. `null` when it is not shipped by the server (e.g. no longer
     * resolvable) — the reply is still shown under its parent comment.
     */
    data class OfReply(
        override val item: OctoObject,
        val parentComment: OctoObject,
        val post: OctoObject?,
    ) : UserCommentContext
}

/**
 * Correlates a `userComments` feed page with its parents via the `parentId` walk. Pure — no I/O — so the
 * tricky part (the walk + the orphan skipping the backend spec calls out) is unit-testable in isolation.
 *
 * Mirrors iOS's `UserCommentsResolver`
 * (`Sources/OctopusCore/Content/UserComments/UserCommentsResolver.swift`, internal ticket).
 */
internal object UserCommentsResolver {
    /**
     * Resolves each feed item to its parent context.
     *
     * - The parents live in a **separate, deduped** `relatedObjects` list; both the related objects and
     *   the page's own feed items are indexed (a reply's parent comment may be in either) — when the
     *   same id appears in both, the `relatedObjects` entry wins.
     * - **comment** → parent is a **post** (one hop). Missing parent => skip (don't render an orphan).
     * - **reply** → parent is a **comment** (one hop), whose own parent is a **post** (grandparent, may
     *   be `null`). Missing parent comment => skip.
     * - Anything that is neither a comment nor a reply is dropped (`userComments` only yields those).
     *
     * Feed order is preserved (the backend returns the merged comments+replies already sorted by the
     * item's own date, anti-chronologically).
     */
    fun resolve(
        feed: List<OctoObject>,
        relatedObjects: List<OctoObject>,
    ): List<UserCommentContext> {
        // Built with plain `put` (last write wins) rather than `putIfAbsent`, which is a Java 8
        // default method unavailable on API < 24 — this SDK ships `minSdk 23` with no core library
        // desugaring, so calling it would throw `NoSuchMethodError` on Android 6.0. Writing the
        // feed first and the related objects second keeps the documented precedence (`relatedObjects`
        // wins on an id present in both). Insertion order is irrelevant: this map is only ever
        // looked up by id, never iterated.
        val byId = HashMap<String, OctoObject>(feed.size + relatedObjects.size)
        for (octo in feed) {
            byId[octo.id] = octo
        }
        for (octo in relatedObjects) {
            byId[octo.id] = octo
        }

        return feed.mapNotNull { item ->
            when {
                item.content.commentOrNull != null -> {
                    val post = byId[item.parentId] ?: return@mapNotNull null // parent gone => skip
                    UserCommentContext.OfComment(item = item, post = post)
                }

                item.content.replyOrNull != null -> {
                    val comment = byId[item.parentId] ?: return@mapNotNull null // parent gone => skip
                    UserCommentContext.OfReply(
                        item = item,
                        parentComment = comment,
                        post = byId[comment.parentId],
                    )
                }

                else -> null // userComments only yields comments & replies
            }
        }
    }
}
