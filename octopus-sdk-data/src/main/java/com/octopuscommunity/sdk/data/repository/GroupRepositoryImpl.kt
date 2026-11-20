package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.getRequest
import com.octopuscommunity.octoObjectOrNull
import com.octopuscommunity.requesterCtxOrNull
import com.octopuscommunity.sdk.data.local.dao.SectionDao
import com.octopuscommunity.sdk.data.local.dao.TopicDao
import com.octopuscommunity.sdk.data.local.entity.TopicEntity
import com.octopuscommunity.sdk.data.mapper.GroupMapper
import com.octopuscommunity.sdk.data.mapper.SectionMapper
import com.octopuscommunity.sdk.data.remote.service.ObjectService
import com.octopuscommunity.sdk.data.remote.service.UserService
import com.octopuscommunity.sdk.data.utils.call
import com.octopuscommunity.sdk.data.utils.callWithValidation
import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.domain.model.SyncFollowGroupAction
import com.octopuscommunity.sdk.domain.model.SyncFollowGroupResult
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.GroupFollowUnfollowError
import com.octopuscommunity.sdk.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class GroupRepositoryImpl(
    private val topicDao: TopicDao,
    private val sectionDao: SectionDao,
    private val objectService: ObjectService,
    private val userService: UserService,
    private val groupMapper: GroupMapper,
    private val sectionMapper: SectionMapper,
) : GroupRepository {
    override fun getGroupsFlow(): Flow<List<OctopusGroup>> =
        combine(
            topicDao.getAllTopics(),
            sectionDao.getAllSections(),
        ) { groupEntities, sectionEntities ->
            with(groupMapper) { groupEntities.toDomain(sectionEntities) }
        }

    override fun getGroupFlow(id: String): Flow<OctopusGroup?> =
        getGroupsFlow().map { groups ->
            groups.firstOrNull { it.id == id }
        }

    override fun getSectionsFlow(): Flow<List<OctopusGroup.Section>> =
        with(sectionMapper) {
            sectionDao.getAllSections().map { entities -> entities.map { it.toDomain() } }
        }

    override suspend fun fetchGroups(): OctopusResult<List<OctopusGroup>, Nothing> =
        call(groupMapper) {
            val response = objectService.getGroups()
            val sectionEntities = with(sectionMapper) { response.sectionsList.map { it.toEntity() } }
            sectionDao.replaceAllSections(sectionEntities)
            val requesterCtxs = response.topicsRequesterCtxsList
            val groupEntities =
                with(groupMapper) {
                    response.topicsList.mapIndexed { index, topic ->
                        topic.toEntity(requesterCtx = requesterCtxs.getOrNull(index))
                    }
                }
            topicDao.replaceAllTopics(groupEntities)
            return@call groupEntities.toDomain(sectionEntities)
        }

    override suspend fun fetchGroup(id: String): OctopusResult<OctopusGroup?, Nothing> =
        when (
            val result =
                call(groupMapper) {
                    val response =
                        objectService.get(
                            getRequest {
                                octoObjectId = id
                                fetchObject = true
                                fetchRequesterCtx = true
                            },
                        )
                    val octoObject = response.octoObjectOrNull?.takeIf { it.content.hasTopic() } ?: return@call null
                    // Never written to the cache: a group left out of the list must stay out of it.
                    val groupEntity = octoObject.toEntity(requesterCtx = response.requesterCtxOrNull)
                    listOf(groupEntity).toDomain(sectionDao.getAllSections().first()).first()
                }
        ) {
            is OctopusResult.Failure.ContentUnavailable -> OctopusResult.Success(null)
            else -> result
        }

    override suspend fun setFollowed(
        group: OctopusGroup,
        followed: Boolean,
    ): OctopusResult<Unit, GroupFollowUnfollowError> {
        val optimisticStatus =
            if (followed) TopicEntity.FollowStatus.FOLLOWED else TopicEntity.FollowStatus.NOT_FOLLOWED
        topicDao.updateFollowStatus(group.id, optimisticStatus)
        return callWithValidation<Unit, GroupFollowUnfollowError> {
            val response =
                if (followed) {
                    userService.followGroup(group.id)
                } else {
                    userService.unfollowGroup(group.id)
                }
            if (!response.hasSuccess()) {
                return OctopusResult.Failure.InvalidArguments(
                    errors = with(groupMapper) { response.fail.errorsList.map { it.toGroupError() } },
                )
            }
        }.also { result ->
            if (result is OctopusResult.Failure) {
                topicDao.updateFollowStatus(
                    topicId = group.id,
                    followStatus = with(groupMapper) { group.toEntityFollowStatus() },
                )
            }
        }
    }

    override suspend fun syncFollowGroups(actions: List<SyncFollowGroupAction>): OctopusResult<List<SyncFollowGroupResult>, Nothing> {
        if (actions.isEmpty()) return OctopusResult.Success(emptyList())
        return call(groupMapper) {
            val protoActions = with(GroupMapper) { actions.map { it.toProto() } }
            val response = userService.syncFollowTopics(protoActions)
            // Refresh local cache so Groups list / feed reflect the new follow states.
            fetchGroups()
            with(groupMapper) { response.resultsList.map { it.toDomain() } }
        }
    }
}
