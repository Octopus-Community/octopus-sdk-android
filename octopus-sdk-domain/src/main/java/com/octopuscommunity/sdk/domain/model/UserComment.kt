package com.octopuscommunity.sdk.domain.model

import java.util.Date

/**
 * A single entry in the connected user's "Comments" profile feed: either a top-level [Comment]
 * the user authored, or a [Comment.Reply] the user authored.
 *
 * This is a pure read model built by walking proto `OctoObject` parent ids (see
 * `UserCommentsResolver` in `octopus-sdk-data`); it is not itself an [OctopusItem].
 *
 * @property id The id of the authored content (the comment's or reply's own id).
 * @property updateDate The authored content's own update date — the feed is ordered
 * anti-chronologically on this date (most recent first).
 * @property content The authored content itself, either a comment or a reply.
 * @property post The post this entry hangs under — always present. Entries whose anchor post was
 * not shipped by the server (e.g. filtered by ACL) carry no renderable context and are dropped by
 * the repository before they reach a caller, so consumers never have to defend against a missing
 * post.
 * @property parentComment The reply's parent comment, when [content] is
 * [Content.OfReply]. `null` when [content] is a top-level comment ([Content.OfComment]).
 *
 * The pairing of [parentComment] with [content]'s subtype is a documented invariant rather than a
 * structural one — `parentComment` could live inside [Content.OfReply], which would make it
 * unrepresentable to get wrong. It deliberately does not: this model is the iso-iOS translation of
 * `OctopusCore/Content/UserComments/UserComment.swift`, whose `Content` enum carries only the
 * authored content and which keeps `post` and `parentComment` as siblings of it. Splitting the shape
 * here would make the two SDKs' read models diverge for no behavioural gain, and the resolver that
 * builds these (`UserCommentsResolver`) is the only producer on either platform.
 */
data class UserComment(
    val id: String,
    val updateDate: Date,
    val content: Content,
    val post: Post,
    val parentComment: Comment?,
) {
    /**
     * The kind of content authored by the user for this entry (Kotlin idiom for iOS's
     * `enum Content { case comment, reply }`).
     */
    sealed interface Content {
        /**
         * A top-level comment the user authored, directly under [UserComment.post].
         *
         * @property comment The authored comment.
         */
        data class OfComment(
            val comment: Comment,
        ) : Content

        /**
         * A reply the user authored, under [UserComment.parentComment] — which is non-`null` for
         * exactly this subtype.
         *
         * @property reply The authored reply.
         */
        data class OfReply(
            val reply: Comment.Reply,
        ) : Content
    }
}

/**
 * A page of [UserComment]s returned by [com.octopuscommunity.sdk.domain.repository.UserCommentRepository].
 *
 * @property comments The comments/replies in this page, anti-chronologically ordered. Empty only
 * when there is no following page ([nextPageCursor] is `null`).
 * @property nextPageCursor Cursor to fetch the following page, or `null` when there is none.
 *
 * A returned page is **either non-empty, or carries no cursor**: the repository absorbs the server's
 * short/empty pages (its ACL filtering can drop every entry of an otherwise non-empty upstream page,
 * as can dropping entries whose anchor post was not shipped) by paging ahead itself until it has
 * something renderable or the cursor runs out. Callers therefore only need the plain "fetch while the
 * cursor is non-null" loop, with no empty-page special case.
 *
 * An empty page with no cursor is *usually* the end of the feed, but not necessarily: the paging-ahead
 * is bounded (`MAX_EMPTY_PAGE_LOOKAHEAD` in `octopus-sdk-data`), and on hitting that bound the
 * repository drops the live cursor rather than hand back the one shape this contract exists to forbid
 * — an empty page that still asks to be paged. A pathological run of fully-filtered pages therefore
 * ends the list early instead of spinning; recovering means reloading the feed, not following a cursor.
 */
data class UserCommentsPage(
    val comments: List<UserComment>,
    val nextPageCursor: String?,
)
