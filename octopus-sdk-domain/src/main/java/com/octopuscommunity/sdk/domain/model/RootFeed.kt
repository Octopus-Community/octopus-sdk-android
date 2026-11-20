package com.octopuscommunity.sdk.domain.model

/** A top-level feed of the community home, displayed with [label]. */
data class RootFeed(
    val id: String,
    val label: String,
    /**
     * The id of the group that is related to this feed.
     * Null if the feed is not representing a group or is multi-group (for example, the feed "For You").
     */
    val relatedGroupId: String? = null,
)

/** @suppress */
@Deprecated("Use relatedGroupId", ReplaceWith("relatedGroupId"))
val RootFeed.relatedTopicId: String? get() = relatedGroupId
