package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.domain.model.SyncFollowGroupAction
import com.octopuscommunity.sdk.domain.model.SyncFollowGroupResult
import com.octopuscommunity.sdk.domain.model.SyncFollowGroupStatus
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.GroupFollowUnfollowError
import com.octopuscommunity.sdk.domain.repository.GroupRepository
import com.octopuscommunity.sdk.test.mock.MockTopics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [GroupRepository] serving [MockTopics]. */
class FakeTopicRepository : GroupRepository {
    private val _groups = MutableStateFlow(MockTopics.all)
    private val _sections = MutableStateFlow(MockTopics.sections)

    override fun getGroupsFlow(): Flow<List<OctopusGroup>> = _groups.asStateFlow()

    override fun getGroupFlow(id: String): Flow<OctopusGroup?> =
        _groups.map { groups ->
            groups.firstOrNull { it.id == id }
        }

    override fun getSectionsFlow(): Flow<List<OctopusGroup.Section>> = _sections.asStateFlow()

    override suspend fun fetchGroups(): OctopusResult<List<OctopusGroup>, Nothing> = OctopusResult.Success(_groups.value)

    override suspend fun fetchGroup(id: String): OctopusResult<OctopusGroup?, Nothing> =
        OctopusResult.Success(_groups.value.firstOrNull { it.id == id })

    override suspend fun setFollowed(
        group: OctopusGroup,
        followed: Boolean,
    ): OctopusResult<Unit, GroupFollowUnfollowError> {
        _groups.value =
            _groups.value.map {
                if (it.id == group.id) it.copy(isFollowed = followed) else it
            }
        return OctopusResult.Success(Unit)
    }

    override suspend fun syncFollowGroups(actions: List<SyncFollowGroupAction>): OctopusResult<List<SyncFollowGroupResult>, Nothing> {
        if (actions.isEmpty()) return OctopusResult.Success(emptyList())
        val results =
            actions.map { action ->
                val existing = _groups.value.firstOrNull { it.id == action.groupId }
                when {
                    existing == null ->
                        SyncFollowGroupResult(action.groupId, SyncFollowGroupStatus.GroupNotFound)
                    !existing.canChangeFollowStatus && existing.isFollowed && !action.followed ->
                        SyncFollowGroupResult(action.groupId, SyncFollowGroupStatus.NotUnfollowable)
                    !existing.canChangeFollowStatus && !existing.isFollowed && action.followed ->
                        SyncFollowGroupResult(action.groupId, SyncFollowGroupStatus.NotFollowable)
                    existing.isFollowed && action.followed ->
                        SyncFollowGroupResult(action.groupId, SyncFollowGroupStatus.AlreadyFollowed)
                    !existing.isFollowed && !action.followed ->
                        SyncFollowGroupResult(action.groupId, SyncFollowGroupStatus.AlreadyUnfollowed)
                    else -> SyncFollowGroupResult(action.groupId, SyncFollowGroupStatus.Applied)
                }
            }
        _groups.value =
            _groups.value.map { group ->
                val result = results.firstOrNull { it.groupId == group.id }
                if (result?.status == SyncFollowGroupStatus.Applied) {
                    val action = actions.first { it.groupId == group.id }
                    group.copy(isFollowed = action.followed)
                } else {
                    group
                }
            }
        return OctopusResult.Success(results)
    }
}
