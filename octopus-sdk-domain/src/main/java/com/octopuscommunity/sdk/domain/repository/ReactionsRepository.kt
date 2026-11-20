package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.ReactionsPage
import com.octopuscommunity.sdk.domain.network.OctopusResult

/**
 * Lists who reacted on a content.
 *
 * Stateless on purpose: nothing is persisted. The list is volatile — someone can react or unreact at any
 * moment — and is only ever shown in a transient sheet, so a cache would age badly for no benefit. Paging
 * state belongs to the caller, which holds one cursor per reaction tab.
 */
interface ReactionsRepository {
    /**
     * Fetches one page of the members who reacted on [contentId], most recent reaction first.
     *
     * The ordering, and the filtering by reaction kind, are the backend's: it owns the sort so that every
     * page is consistent with the previous one.
     *
     * @param kind restricts the list to a single reaction kind. `null` lists every kind, for the "All" tab.
     * @param cursor `null` for the first page, otherwise the previous page's [ReactionsPage.nextCursor].
     */
    suspend fun getReactions(
        contentId: String,
        kind: OctopusItem.Reaction.Kind?,
        cursor: String?,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): OctopusResult<ReactionsPage, Nothing>

    companion object {
        /**
         * Rows fetched per [getReactions] call when the caller does not ask for a specific page size.
         * 20, as the comments and replies lists request, and as iOS's own reactions list does.
         */
        const val DEFAULT_PAGE_SIZE = 20
    }
}
