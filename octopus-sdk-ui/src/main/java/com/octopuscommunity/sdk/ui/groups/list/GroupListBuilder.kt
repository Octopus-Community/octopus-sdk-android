package com.octopuscommunity.sdk.ui.groups.list

import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.groups.list.GroupsListViewModel.UiState.Group

/**
 * Builds the ordered block list rendered by the Topics nav.
 *
 * Ordering is admin-defined and follow-status independent:
 * 1. A single no-section block at the top, holding groups with no client section, in
 *    the order [groups] arrives.
 * 2. One block per client section, in the order [sections] arrives (the backend ships
 *    sections as an ordered array), each containing its topics in the order they appear
 *    in [groups].
 *
 * Empty blocks are omitted.
 */
internal fun buildGroupBlocks(
    groups: List<OctopusGroup>,
    sections: List<OctopusGroup.Section>,
): List<Group> =
    buildList {
        @Suppress("DEPRECATION")
        val noSectionGroups = groups.filter { it.sections.isEmpty() }
        if (noSectionGroups.isNotEmpty()) add(Group.NoSection(topics = noSectionGroups))

        sections.forEach { section ->
            @Suppress("DEPRECATION")
            val sectionTopics = groups.filter { section in it.sections }
            if (sectionTopics.isNotEmpty()) {
                add(
                    Group.Section(
                        id = section.id,
                        label = UiText.DynamicString(section.label),
                        topics = sectionTopics,
                    ),
                )
            }
        }
    }

/**
 * Counts how many distinct topics are followed across [blocks].
 *
 * Topics that belong to several [Group.Section] blocks are emitted once per block by
 * [buildGroupBlocks]. A raw `flatMap { topics }.count { isFollowed }` would count those
 * topics multiple times — for the "last followed group" guard in `setFollowed`, that means
 * a user whose only followed topic is shared across two sections could silently bypass the
 * guard and unfollow it. Deduplicating by topic id reflects the unique set the user
 * follows, not the rendered block sequence.
 */
internal fun countFollowedTopics(blocks: List<Group>): Int =
    blocks
        .flatMap { it.topics }
        .distinctBy { it.id }
        .count { it.isFollowed }
