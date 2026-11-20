package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.domain.model.SyncFollowGroupAction
import com.octopuscommunity.sdk.domain.model.SyncFollowGroupResult
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.ServerError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Repository for the groups (formerly topics) of the community. */
interface GroupRepository {
    /** Observes the cached groups. */
    fun getGroupsFlow(): Flow<List<OctopusGroup>>

    /** Observes the cached group [id]. */
    fun getGroupFlow(id: String): Flow<OctopusGroup?>

    /** Observes the cached group sections. */
    fun getSectionsFlow(): Flow<List<OctopusGroup.Section>>

    /** Fetches the groups from the backend and updates the cache. */
    suspend fun fetchGroups(): OctopusResult<List<OctopusGroup>, Nothing>

    /**
     * Fetches the group [id] alone from the backend, including the groups that [fetchGroups]
     * leaves out because the current user may not see them in the list. A group's visibility only
     * decides whether it is listed; whether its content can be read is [OctopusGroup.canAccess].
     *
     * The result is not written to the cache: the group stays out of [getGroupsFlow] and
     * [getGroupFlow]. Returns `null` when no group has this [id]. The default implementation
     * fetches nothing and returns `null`.
     */
    suspend fun fetchGroup(id: String): OctopusResult<OctopusGroup?, Nothing> = OctopusResult.Success(null)

    /** Follows [group] when [followed] is `true`, unfollows it otherwise. */
    suspend fun setFollowed(
        group: OctopusGroup,
        followed: Boolean,
    ): OctopusResult<Unit, GroupFollowUnfollowError>

    /**
     * Applies the follow and unfollow [actions] in one call, then refreshes the groups. Returns one
     * result per action. The default implementation applies nothing and returns an empty list.
     */
    suspend fun syncFollowGroups(actions: List<SyncFollowGroupAction>): OctopusResult<List<SyncFollowGroupResult>, Nothing> =
        OctopusResult.Success(emptyList())
}

/**
 * `true` iff at least one group in the repository is both accessible AND allows post creation
 * by the current user. Used by cross-group "create post" entry points (home feed, current user
 * profile, group detail) to hide the create-post button when no eligible topic exists.
 *
 * Cold-start carve-out: an empty group list resolves to `true` to match the per-field default —
 * we have no evidence yet that post creation is forbidden, so the FAB stays visible until the
 * groups actually load. Without this guard the FAB would flicker out on every fresh install
 * before the first sync.
 *
 * Mirrors iOS `TopicsRepository.canCreateAnyPost` (internal change).
 */
fun GroupRepository.canCreateAnyPostFlow(): Flow<Boolean> =
    getGroupsFlow()
        .map { groups ->
            if (groups.isEmpty()) {
                true
            } else {
                groups.any { it.canAccess && it.canCreateChildren }
            }
        }.distinctUntilChanged()

/** Errors returned by [GroupRepository.setFollowed]. */
sealed interface GroupFollowUnfollowError : ServerError {
    /** The group does not exist. */
    data class MissingGroup(
        override val errorMessage: String,
    ) : GroupFollowUnfollowError

    /** The group cannot be unfollowed. */
    data class UnfollowableGroup(
        override val errorMessage: String,
    ) : GroupFollowUnfollowError

    /** The group is already followed. */
    data class GroupAlreadyFollowed(
        override val errorMessage: String,
    ) : GroupFollowUnfollowError

    /** The group is already unfollowed. */
    data class GroupAlreadyUnfollowed(
        override val errorMessage: String,
    ) : GroupFollowUnfollowError

    /**
     * Returned by [com.octopuscommunity.sdk.OctopusSDK.unfollowGroup] when the user attempts to
     * unfollow their last followed group. A user must always have at least one followed group
     * (counting both voluntarily-followed and force-followed groups). This guard matches the
     * behavior of the built-in Octopus SDK UI.
     */
    data class LastFollowedGroup(
        override val errorMessage: String,
    ) : GroupFollowUnfollowError

    /** The backend returned a generic error. */
    data class Unknown(
        override val errorMessage: String,
    ) : GroupFollowUnfollowError
}

// region Deprecated backward-compatible aliases

/** @suppress */
@Deprecated("Use GroupRepository", ReplaceWith("GroupRepository"))
typealias TopicRepository = GroupRepository

/** @suppress */
@Deprecated("Use GroupFollowUnfollowError", ReplaceWith("GroupFollowUnfollowError"))
typealias TopicFollowUnfollowError = GroupFollowUnfollowError

// endregion
