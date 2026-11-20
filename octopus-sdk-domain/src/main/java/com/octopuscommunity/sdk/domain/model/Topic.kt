package com.octopuscommunity.sdk.domain.model

import java.util.Date

/**
 * Represents a group in the Octopus community.
 *
 * Groups are content categories that organize posts within a community.
 * Users can follow/unfollow groups to customize their feed experience.
 *
 * @property isFollowed Whether the connected user currently follows this group.
 * @property canChangeFollowStatus Whether the connected user can change their follow status
 * on this group. `false` for essential/force-followed and force-not-followed groups that
 * community admins control.
 * @property canAccess Whether the connected user has access to this group. `true` for groups
 * without an access requirement, or when the user holds the required entitlement. `false` when
 * the group is visible to the user but they cannot open it (typically a premium/gated group).
 * When `false`, taps should route through the host app's `onGroupAccessDenied` navigation
 * callback instead of opening the group.
 * @property canCreateChildren Whether the connected user is allowed to create posts in this
 * group. `true` for unrestricted groups; `false` when the group is admin-only (internal ticket) and
 * the requester is not tagged admin, or when the group otherwise gates post creation. UI uses
 * this to filter the create-post topic picker and to hide the create-post button when no
 * topic in the relevant scope is creatable. Backward-compat default is `true`.
 */
data class OctopusGroup(
    val id: String,
    /** Parent community identifier. No replacement: iOS does not expose this property. */
    @Deprecated(message = "Not part of the iOS API; removed in 2.0.", level = DeprecationLevel.WARNING)
    val parentId: String,
    /** Creation date. No replacement: iOS does not expose this property. */
    @Deprecated(message = "Not part of the iOS API; removed in 2.0.", level = DeprecationLevel.WARNING)
    val creationDate: Date,
    /** Original content language. No replacement: iOS does not expose this property. */
    @Deprecated(message = "Not part of the iOS API; removed in 2.0.", level = DeprecationLevel.WARNING)
    val localLanguage: String,
    /** Last update date. No replacement: iOS does not expose this property. */
    @Deprecated(message = "Not part of the iOS API; removed in 2.0.", level = DeprecationLevel.WARNING)
    val updateDate: Date,
    /** Publication status. No replacement: iOS does not expose this property. */
    @Deprecated(message = "Not part of the iOS API; removed in 2.0.", level = DeprecationLevel.WARNING)
    val status: Status,
    val name: String,
    /** Group header description. No replacement: iOS does not expose this property. */
    @Deprecated(message = "Not part of the iOS API; removed in 2.0.", level = DeprecationLevel.WARNING)
    val description: String?,
    val isFollowed: Boolean = false,
    val canChangeFollowStatus: Boolean = true,
    val canAccess: Boolean = true,
    val canCreateChildren: Boolean = true,
    /** Sections containing this group. No replacement: iOS does not expose this property. */
    @Deprecated(message = "Not part of the iOS API; removed in 2.0.", level = DeprecationLevel.WARNING)
    val sections: List<Section> = emptyList(),
    /** Group post feed identifiers. No replacement: iOS does not expose this property. */
    @Deprecated(message = "Not part of the iOS API; removed in 2.0.", level = DeprecationLevel.WARNING)
    val postsFeeds: OctopusItem.Feeds = OctopusItem.Feeds(),
    /**
     * Operator-configured call-to-action button shown on the group detail header. `null` when
     * the group has no CTA, its text or target URL is missing, or the target URL is not parseable.
     * No replacement: iOS exposes no public group CTA API. The SDK UI still renders the button.
     */
    @Suppress("DEPRECATION")
    @Deprecated(message = "Not part of the iOS API; removed in 2.0.", level = DeprecationLevel.WARNING)
    val customAction: CustomAction? = null,
) {
    data class Section(
        val id: String,
        val label: String,
    )
}

/** Former name of [OctopusGroup]. */
@Deprecated("Use OctopusGroup", ReplaceWith("OctopusGroup"))
typealias Topic = OctopusGroup
