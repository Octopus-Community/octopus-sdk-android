package com.octopuscommunity.sdk.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey override val id: String,
    override val parentId: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val localeLanguage: String,
    @Embedded(prefix = "status_") override val status: StatusEntity,
    @Embedded(prefix = "author_") val author: MinimalProfileEntity?,
    @Embedded(prefix = "text_") val text: TranslatableTextEntity,
    @Embedded(prefix = "medias_") val medias: MediasEntity?,
    @Embedded(prefix = "poll_") val poll: PollEntity?,
    @Embedded(prefix = "comments_feeds_") val commentsFeeds: FeedsEntity,
    @Embedded(prefix = "counters_") val counters: CountersEntity = CountersEntity(),
    @Embedded(prefix = "reaction_") val userReaction: ReactionEntity? = null,
    val userVoteId: String? = null,
    @Embedded(prefix = "client_object_") val clientObject: ClientObjectEntity?,
    @Embedded(prefix = "custom_action_") val customAction: CustomActionEntity?,
    val canAccess: Boolean = true,
    val canCreateChildren: Boolean = true,
) : OctopusEntity {
    data class PollEntity(
        val answers: List<AnswerEntity>,
    ) {
        @Serializable
        data class AnswerEntity(
            val id: String,
            @Embedded(prefix = "text_") val text: TranslatableTextEntity,
        )

        @Serializable
        data class ResultEntity(
            val answerId: String,
            val voteCount: Int,
        )
    }

    data class CountersEntity(
        val viewCount: Int = 0,
        val commentCount: Int = 0,
        val reactions: List<ReactionCountEntity> = listOf(),
        val pollResults: List<PollEntity.ResultEntity>? = null,
    )

    data class ClientObjectEntity(
        val id: String,
        @Embedded(prefix = "catchPhrase_") val catchPhrase: TranslatableTextEntity? = null,
        @Embedded(prefix = "ctaText_") val ctaText: TranslatableTextEntity? = null,
    )

    data class CustomActionEntity(
        @Embedded(prefix = "ctaText_") val ctaText: TranslatableTextEntity,
        val targetUrl: String?,
    )

    data class CountersUpdate(
        @PrimaryKey val id: String,
        @Embedded(prefix = "counters_") val counters: CountersEntity,
        @Embedded(prefix = "reaction_") val userReaction: ReactionEntity?,
        val userVoteId: String?,
        // canAccess / canCreateChildren are persisted via the additional-data fetch (the
        // GetBatch response is the only place we receive an authoritative `RequesterCtx` —
        // PutPost doesn't carry one, so a fresh post lands in the DB with default-open
        // permissions until the post-create getCounters hydrates them. See internal ticket hotfix
        // / iOS internal change.
        val canAccess: Boolean,
        val canCreateChildren: Boolean,
    )
}

data class PostWithTopicEntity(
    @Embedded val post: PostEntity,
    @Relation(
        parentColumn = "parentId",
        entityColumn = "id",
    )
    val topic: TopicEntity?,
)
