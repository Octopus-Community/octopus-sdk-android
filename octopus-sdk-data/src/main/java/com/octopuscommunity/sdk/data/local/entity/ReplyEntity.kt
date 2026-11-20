package com.octopuscommunity.sdk.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "replies")
data class ReplyEntity(
    @PrimaryKey override val id: String,
    override val parentId: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val localeLanguage: String,
    @Embedded(prefix = "status_") override val status: StatusEntity,
    @Embedded(prefix = "author_") val author: MinimalProfileEntity?,
    @Embedded(prefix = "text_") val text: TranslatableTextEntity,
    @Embedded(prefix = "medias_") val medias: MediasEntity?,
    @Embedded(prefix = "counters_") val counters: CountersEntity = CountersEntity(),
    @Embedded(prefix = "reaction_") val userReaction: ReactionEntity? = null,
    val canAccess: Boolean = true,
    val canCreateChildren: Boolean = true,
) : OctopusEntity {
    data class CountersEntity(
        val reactions: List<ReactionCountEntity> = listOf(),
    ) {
        data class Update(
            @PrimaryKey val id: String,
            @Embedded(prefix = "counters_") val counters: CountersEntity,
            @Embedded(prefix = "reaction_") val userReaction: ReactionEntity?,
            // See internal ticket hotfix / iOS internal change: PutReply response carries no RequesterCtx,
            // so permissions are only authoritative via the additional-data fetch.
            val canAccess: Boolean,
            val canCreateChildren: Boolean,
        )
    }
}

data class ReplyWithCommentEntity(
    @Embedded val reply: ReplyEntity,
    @Relation(
        parentColumn = "parentId",
        entityColumn = "id",
    )
    val comment: CommentEntity?,
)
