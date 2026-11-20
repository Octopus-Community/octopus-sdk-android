package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.UserCommentsPage
import com.octopuscommunity.sdk.domain.network.OctopusResult

/**
 * Repository for a member's "Comments" profile feed: the comments and replies that member has
 * authored, most recent first.
 *
 * Serves both the connected user's own tabs and another member's profile (where the tab is gated by
 * [com.octopuscommunity.sdk.domain.model.CommunityConfig.showCommentsOnOtherProfiles]); the feed id
 * alone selects whose comments are returned, so nothing here is own-profile specific.
 *
 * Every page returned is **either non-empty, or the end of the feed** — see [UserCommentsPage].
 */
interface UserCommentRepository {
    /**
     * Fetches the first page of the feed identified by [feedId].
     *
     * @param feedId The feed id to fetch (see [com.octopuscommunity.sdk.domain.model.Profile.commentsFeeds]).
     * @param pageSize The maximum number of entries to return in this page. The server may return
     * fewer, and this repository may consume several upstream pages of this size to honour the
     * non-empty-page contract.
     * @return [OctopusResult.Success] with the first [UserCommentsPage], or a connection failure.
     */
    suspend fun firstPage(
        feedId: String,
        pageSize: Int,
    ): OctopusResult<UserCommentsPage, Nothing>

    /**
     * Fetches the page following [pageCursor].
     *
     * @param pageCursor Cursor returned by a previous [firstPage]/[nextPage] call
     * ([UserCommentsPage.nextPageCursor]).
     * @param pageSize The maximum number of entries to return in this page. See [firstPage].
     * @return [OctopusResult.Success] with the next [UserCommentsPage], or a connection failure.
     */
    suspend fun nextPage(
        pageCursor: String,
        pageSize: Int,
    ): OctopusResult<UserCommentsPage, Nothing>
}
