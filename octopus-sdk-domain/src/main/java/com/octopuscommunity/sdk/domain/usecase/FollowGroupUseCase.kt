package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.OctopusEvent.GroupFollowingChanged
import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.GroupRepository

/** Follows or unfollows a group, and emits [GroupFollowingChanged] on success. */
class FollowGroupUseCase(
    private val groupRepository: GroupRepository,
    private val eventRepository: EventRepository,
) {
    /** Follows [group] when [follow] is `true`, unfollows it otherwise. */
    suspend operator fun invoke(
        group: OctopusGroup,
        follow: Boolean,
    ) = groupRepository.setFollowed(group, follow).onSuccess {
        eventRepository.emitEvent(
            GroupFollowingChanged(groupId = group.id, followed = follow),
        )
    }
}
