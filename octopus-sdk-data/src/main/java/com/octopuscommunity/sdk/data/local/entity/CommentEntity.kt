package com.octopuscommunity.sdk.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey override val id: String,
    override val parentId: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val localeLanguage: String,
    @Embedded(prefix = "status_") override val status: StatusEntity,
    @Embedded(prefix = "author_") val author: MinimalProfileEntity?,
    @Embedded(prefix = "text_") val text: TranslatableTextEntity,
    @Embedded(prefix = "medias_") val medias: MediasEntity?,
    @Embedded(prefix = "replies_feeds_") val repliesFeeds: FeedsEntity,
    @Embedded(prefix = "counters_") val counters: CountersEntity = CountersEntity(),
    @Embedded(prefix = "user_reaction_") val userReaction: ReactionEntity? = null,
    val canAccess: Boolean = true,
    val canCreateChildren: Boolean = true,
) : OctopusEntity {
    data class CountersEntity(
        val replyCount: Int = 0,
        val reactions: List<ReactionCountEntity> = listOf(),
    ) {
        data class Update(
            @PrimaryKey val id: String,
            @Embedded(prefix = "counters_") val counters: CountersEntity,
            @Embedded(prefix = "user_reaction_") val userReaction: ReactionEntity?,
            // See internal ticket hotfix / iOS internal change: PutComment response carries no RequesterCtx,
            // so permissions are only authoritative via the additional-data fetch.
            val canAccess: Boolean,
            val canCreateChildren: Boolean,
        )
    }
}

data class CommentWithPostEntity(
    @Embedded val comment: CommentEntity,
    @Relation(
        parentColumn = "parentId",
        entityColumn = "id",
    )
    val post: PostEntity?,
)
