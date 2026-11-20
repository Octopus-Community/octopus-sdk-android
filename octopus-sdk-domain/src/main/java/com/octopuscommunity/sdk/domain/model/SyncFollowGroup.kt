package com.octopuscommunity.sdk.domain.model

import java.util.Date

/**
 * A single follow/unfollow action targeting one group, tagged with a date used by the
 * backend for conflict resolution in [com.octopuscommunity.sdk.OctopusSDK.syncFollowGroups].
 *
 * @property groupId id of the group to follow or unfollow.
 * @property followed `true` to follow, `false` to unfollow.
 * @property actionDate moment at which the host app decided to apply this action. If the
 * backend has stored a more recent action for the same group and user, this action is
 * skipped (see [SyncFollowGroupStatus.Skipped]).
 */
data class SyncFollowGroupAction(
    val groupId: String,
    val followed: Boolean,
    val actionDate: Date
)

/**
 * Per-action result returned by [com.octopuscommunity.sdk.OctopusSDK.syncFollowGroups].
 *
 * @property groupId id of the group targeted by the original action.
 * @property status outcome of the action (see [SyncFollowGroupStatus]).
 */
data class SyncFollowGroupResult(
    val groupId: String,
    val status: SyncFollowGroupStatus
)

/**
 * Outcome of a single [SyncFollowGroupAction] after the backend has processed it.
 *
 * Note: these values describe per-action outcomes, not transport errors. Transport-level errors
 * (network, unauthenticated, etc.) surface via [com.octopuscommunity.sdk.domain.network.OctopusResult.Failure].
 */
sealed interface SyncFollowGroupStatus {
    /** The follow/unfollow was applied to the backend. */
    data object Applied : SyncFollowGroupStatus

    /**
     * The stored action timestamp for this group is more recent than the one provided; the
     * backend ignored this action intentionally. Not an error.
     */
    data object Skipped : SyncFollowGroupStatus

    /** No group with the given [SyncFollowGroupAction.groupId] exists on the backend. */
    data object GroupNotFound : SyncFollowGroupStatus

    /** The group is configured as force-hidden and cannot be followed. */
    data object NotFollowable : SyncFollowGroupStatus

    /** The group is configured as essential / force-followed and cannot be unfollowed. */
    data object NotUnfollowable : SyncFollowGroupStatus

    /** The user was already following this group; no change was needed. */
    data object AlreadyFollowed : SyncFollowGroupStatus

    /** The user was already not following this group; no change was needed. */
    data object AlreadyUnfollowed : SyncFollowGroupStatus

    /**
     * The backend returned an unspecified or unexpected error for this action, or a status the
     * SDK does not know how to map yet. Treat as a non-fatal per-action failure.
     */
    data object UnknownError : SyncFollowGroupStatus
}
