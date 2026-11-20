package com.octopuscommunity.sdk.data.model

import com.octopuscommunity.sdk.domain.model.Feed
import com.octopuscommunity.sdk.domain.model.OctopusItem

/**
 * A page of a feed opened at one of its items, as
 * [com.octopuscommunity.sdk.data.repository.WindowFeedRepository.fetchItemsAround] returns it.
 *
 * @property feed the page. Its [Feed.nextPageCursor] continues after the page, as for any page.
 * @property previousPageCursor continues before the page, toward the start of the feed, through
 * [com.octopuscommunity.sdk.data.repository.WindowFeedRepository.fetchPreviousItems]. `null`
 * when nothing comes before the page.
 * @property isAnchored whether the page holds the requested item. When it does not, the feed could
 * not be opened there (the item is gone, hidden, or belongs to another feed, or the server does not
 * open feeds at an item) and [feed] is its first page.
 */
data class AnchoredFeed<T : OctopusItem>(
    val feed: Feed<T>,
    val previousPageCursor: String? = null,
    val isAnchored: Boolean = false,
)
