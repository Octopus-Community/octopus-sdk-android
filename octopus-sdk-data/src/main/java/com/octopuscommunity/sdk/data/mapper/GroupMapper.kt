package com.octopuscommunity.sdk.data.mapper

import androidx.core.net.toUri
import com.octopuscommunity.GrpcModelProto
import com.octopuscommunity.UserServiceProto
import com.octopuscommunity.UserServiceProto.FollowUnfollowTopicResponse.Error.DetailsCase.MISSINGPARENT
import com.octopuscommunity.UserServiceProto.FollowUnfollowTopicResponse.Error.DetailsCase.TOPICALREADYFOLLOWED
import com.octopuscommunity.UserServiceProto.FollowUnfollowTopicResponse.Error.DetailsCase.TOPICALREADYUNFOLLOWED
import com.octopuscommunity.UserServiceProto.FollowUnfollowTopicResponse.Error.DetailsCase.UNFOLLOWABLETOPIC
import com.octopuscommunity.UserServiceProto.SyncFollowTopicStatus.SYNC_FOLLOW_ALREADY_FOLLOWED
import com.octopuscommunity.UserServiceProto.SyncFollowTopicStatus.SYNC_FOLLOW_ALREADY_UNFOLLOWED
import com.octopuscommunity.UserServiceProto.SyncFollowTopicStatus.SYNC_FOLLOW_APPLIED
import com.octopuscommunity.UserServiceProto.SyncFollowTopicStatus.SYNC_FOLLOW_NOT_FOLLOWABLE
import com.octopuscommunity.UserServiceProto.SyncFollowTopicStatus.SYNC_FOLLOW_NOT_UNFOLLOWABLE
import com.octopuscommunity.UserServiceProto.SyncFollowTopicStatus.SYNC_FOLLOW_SKIPPED
import com.octopuscommunity.UserServiceProto.SyncFollowTopicStatus.SYNC_FOLLOW_TOPIC_NOT_FOUND
import com.octopuscommunity.ctaOrNull
import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.data.local.entity.FeedsEntity
import com.octopuscommunity.sdk.data.local.entity.PostEntity.CustomActionEntity
import com.octopuscommunity.sdk.data.local.entity.SectionEntity
import com.octopuscommunity.sdk.data.local.entity.TopicEntity
import com.octopuscommunity.sdk.data.local.entity.TranslatableTextEntity
import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.Post.CustomAction
import com.octopuscommunity.sdk.domain.model.SyncFollowGroupAction
import com.octopuscommunity.sdk.domain.model.SyncFollowGroupResult
import com.octopuscommunity.sdk.domain.model.SyncFollowGroupStatus
import com.octopuscommunity.sdk.domain.repository.GroupFollowUnfollowError
import com.octopuscommunity.syncFollowTopicAction
import com.octopuscommunity.topicOrNull
import java.util.Date

class GroupMapper(
    private val preferencesDataStore: PreferencesDataStore,
    private val statusMapper: StatusMapper,
    private val sectionMapper: SectionMapper,
    private val translationMapper: TranslationMapper,
) : OctopusMapper {
    suspend fun GrpcModelProto.OctoObject.toEntity(requesterCtx: GrpcModelProto.RequesterCtx? = null): TopicEntity {
        val topic = content.topicOrNull
        return TopicEntity(
            id = id,
            parentId = parentId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            localeLanguage = preferencesDataStore.getLocaleIdentifier(),
            status = with(statusMapper) { status.toEntity() },
            name = topic?.name ?: "",
            description = topic?.description?.ifEmpty { null },
            followStatus =
                when (topic?.followStatus) {
                    GrpcModelProto.Topic.FollowStatus.TOPIC_FOLLOWED -> TopicEntity.FollowStatus.FOLLOWED
                    GrpcModelProto.Topic.FollowStatus.TOPIC_FORCE_FOLLOWED -> TopicEntity.FollowStatus.FORCE_FOLLOWED
                    GrpcModelProto.Topic.FollowStatus.TOPIC_FORCE_NOT_FOLLOWED -> TopicEntity.FollowStatus.NOT_FOLLOWABLE
                    else -> TopicEntity.FollowStatus.NOT_FOLLOWED
                },
            postsFeeds =
                FeedsEntity(
                    descendingId = descChildrenFeedId.ifEmpty { null },
                    ascendingId = ascChildrenFeedId.ifEmpty { null },
                ),
            sectionIds = topic?.sectionIdsList ?: emptyList(),
            canAccess = requesterCtx?.takeIf { it.hasCanAccess() }?.canAccess ?: true,
            canCreateChildren = requesterCtx?.takeIf { it.hasCanCreateChildren() }?.canCreateChildren ?: true,
            customAction = topic?.ctaOrNull?.toCustomActionEntity(),
        )
    }

    /**
     * Maps a proto Topic CTA to its persistence shape. Returns `null` when the operator-provided
     * payload cannot back a usable button — keeps parity with iOS (`GroupMapper.swift`) so both
     * platforms hide the button under the same conditions:
     *
     * - blank label (`text`),
     * - blank target URL (`targetLink`),
     * - target URL not parseable by [android.net.Uri].
     *
     * Topic protos do not carry `originalLanguage` / `translatedLanguage` (the BE swap happens
     * via the request header). The original side has no known language and stays unset. The
     * device locale is stamped on the translated side **only when** `translatedText` is non-empty;
     * if the proto carries only the original `text` (no translation requested or available), the
     * entire `translated` block of `TranslatableTextEntity` is dropped to `null` and the device
     * locale is not persisted at all.
     *
     * Note: the text/target presence gate is delegated to [isTopicCtaTextAndTargetPresent] so a
     * pure-JVM unit test can exercise the contract directly. The URI parsability gate uses
     * Android's `Uri.parse` and is therefore not unit-testable without Robolectric — it is
     * covered through QA on emulator.
     */
    private suspend fun GrpcModelProto.CTA.toCustomActionEntity(): CustomActionEntity? {
        if (!isCtaTextAndTargetPresent(text, targetLink)) return null
        if (targetLink.toUri().scheme == null) return null
        return CustomActionEntity(
            ctaText =
                TranslatableTextEntity(
                    originalText = text,
                    originalLanguage = "",
                    translatedText = translatedText.ifEmpty { null },
                    translatedLanguage = preferencesDataStore.getLocaleIdentifier(),
                ),
            targetUrl = targetLink,
        )
    }

    companion object {
        /**
         * Pure-Kotlin presence gate for the Topic / Post CTA: returns `false` when the
         * operator left either field blank. The caller still has to validate URI
         * parsability separately, but this piece of the contract is unit-testable in JVM
         * without `android.net.Uri` stubs.
         *
         * Shared between [GroupMapper] (Topic CTA) and `PostMapper` (Post CTA) so both
         * surfaces hide an unusable button under the same conditions — keeps parity with
         * iOS, which collapsed the same check into the failable `CustomAction.init?` on
         * internal ticket.
         *
         * Kept `internal` so tests in the same module can call it directly while
         * consumers can't.
         */
        internal fun isCtaTextAndTargetPresent(
            text: String,
            targetLink: String,
        ): Boolean = text.isNotBlank() && targetLink.isNotBlank()

        /**
         * Maps a public [SyncFollowGroupAction] to its protobuf request shape.
         *
         * Every source property is read through the explicit `this@toProto` receiver. This is
         * **not** stylistic: inside the `syncFollowTopicAction { }` DSL the closest implicit
         * receiver is the proto builder, which also exposes a `followed` property. Writing the
         * bare `followed = followed` would resolve the right-hand side to the builder's own
         * (default `false`) field instead of the action's value — a silent self-assignment that
         * shipped `followed = false` for every action regardless of intent (a re-follow was
         * silently sent as an unfollow). Mirrors the `this@toProto` convention used by every other mapper.
         *
         * Kept `internal` so [com.octopuscommunity.sdk.data.mapper.SyncFollowGroupActionToProtoTest]
         * can pin the behaviour in pure JVM (no Android deps needed to build the proto).
         */
        internal fun SyncFollowGroupAction.toProto(): UserServiceProto.SyncFollowTopicAction =
            syncFollowTopicAction {
                topicId = this@toProto.groupId
                followed = this@toProto.followed
                actionTimestamp = this@toProto.actionDate.time
            }
    }

    fun TopicEntity.toDomain() =
        OctopusGroup(
            id = id,
            parentId = parentId,
            creationDate = Date(createdAt),
            updateDate = Date(updatedAt),
            localLanguage = localeLanguage,
            status = with(statusMapper) { status.toDomain() },
            name = name,
            description = description,
            isFollowed =
                followStatus == TopicEntity.FollowStatus.FOLLOWED ||
                    followStatus == TopicEntity.FollowStatus.FORCE_FOLLOWED,
            canChangeFollowStatus =
                followStatus == TopicEntity.FollowStatus.FOLLOWED ||
                    followStatus == TopicEntity.FollowStatus.NOT_FOLLOWED,
            canAccess = canAccess,
            canCreateChildren = canCreateChildren,
            sections = emptyList(),
            postsFeeds =
                OctopusItem.Feeds(
                    descendingId = postsFeeds.descendingId,
                    ascendingId = postsFeeds.ascendingId,
                ),
            customAction =
                customAction?.let {
                    CustomAction(
                        ctaText = with(translationMapper) { it.ctaText.toDomain() },
                        targetUrl = it.targetUrl,
                    )
                },
        )

    fun List<TopicEntity>.toDomain(sectionEntities: List<SectionEntity>): List<OctopusGroup> {
        val sections = with(sectionMapper) { sectionEntities.map { it.toDomain() } }
        return map { groupEntity ->
            groupEntity.toDomain().copy(
                sections =
                    groupEntity.sectionIds.mapNotNull { sectionId ->
                        sections.firstOrNull { it.id == sectionId }
                    },
            )
        }
    }

    /**
     * Converts the public (isFollowed, canChangeFollowStatus) shape back into the internal
     * [TopicEntity.FollowStatus] used for Room storage. Used to restore the DB row when an
     * optimistic follow/unfollow fails.
     */
    fun OctopusGroup.toEntityFollowStatus(): TopicEntity.FollowStatus =
        when {
            !canChangeFollowStatus && isFollowed -> TopicEntity.FollowStatus.FORCE_FOLLOWED
            !canChangeFollowStatus && !isFollowed -> TopicEntity.FollowStatus.NOT_FOLLOWABLE
            isFollowed -> TopicEntity.FollowStatus.FOLLOWED
            else -> TopicEntity.FollowStatus.NOT_FOLLOWED
        }

    fun UserServiceProto.FollowUnfollowTopicResponse.Error.toGroupError() =
        when (detailsCase) {
            MISSINGPARENT -> GroupFollowUnfollowError.MissingGroup(message)
            UNFOLLOWABLETOPIC -> GroupFollowUnfollowError.UnfollowableGroup(message)
            TOPICALREADYFOLLOWED -> GroupFollowUnfollowError.GroupAlreadyFollowed(message)
            TOPICALREADYUNFOLLOWED -> GroupFollowUnfollowError.GroupAlreadyUnfollowed(message)
            else -> GroupFollowUnfollowError.Unknown(message)
        }

    fun UserServiceProto.SyncFollowTopicResult.toDomain(): SyncFollowGroupResult =
        SyncFollowGroupResult(
            groupId = topicId,
            status = status.toDomain(),
        )

    fun UserServiceProto.SyncFollowTopicStatus.toDomain(): SyncFollowGroupStatus =
        when (this) {
            SYNC_FOLLOW_APPLIED -> SyncFollowGroupStatus.Applied
            SYNC_FOLLOW_SKIPPED -> SyncFollowGroupStatus.Skipped
            SYNC_FOLLOW_TOPIC_NOT_FOUND -> SyncFollowGroupStatus.GroupNotFound
            SYNC_FOLLOW_NOT_FOLLOWABLE -> SyncFollowGroupStatus.NotFollowable
            SYNC_FOLLOW_NOT_UNFOLLOWABLE -> SyncFollowGroupStatus.NotUnfollowable
            SYNC_FOLLOW_ALREADY_FOLLOWED -> SyncFollowGroupStatus.AlreadyFollowed
            SYNC_FOLLOW_ALREADY_UNFOLLOWED -> SyncFollowGroupStatus.AlreadyUnfollowed
            else -> SyncFollowGroupStatus.UnknownError
        }
}
