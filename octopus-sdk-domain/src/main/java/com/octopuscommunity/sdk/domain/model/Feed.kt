package com.octopuscommunity.sdk.domain.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.Date

/** A page of the feed [id]. [nextPageCursor] is `null` on the last page. */
data class Feed<T : OctopusItem>(
    val id: String,
    val items: Flow<List<T>> = emptyFlow(),
    val nextPageCursor: String? = null,
) {
    /** Reference from the feed [feedId] to the item [itemId], as of [updateDate]. */
    data class Item(
        val feedId: String,
        val itemId: String,
        val updateDate: Date,
        val featuredChildId: String? = null,
    ) {
        /** Whether [item] is older than this reference, so a newer version exists. */
        fun hasUpdate(item: OctopusItem): Boolean = item.updateDate.before(updateDate)
    }

    /** Whether a next page exists. */
    val hasMoreItems = nextPageCursor != null
}
