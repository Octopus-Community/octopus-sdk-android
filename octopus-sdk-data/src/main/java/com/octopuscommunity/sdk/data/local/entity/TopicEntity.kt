package com.octopuscommunity.sdk.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.octopuscommunity.sdk.data.local.entity.PostEntity.CustomActionEntity

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey override val id: String,
    override val parentId: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val localeLanguage: String,
    @Embedded(prefix = "status_") override val status: StatusEntity,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "follow_status") val followStatus: FollowStatus,
    @Embedded(prefix = "post_feeds_") val postsFeeds: FeedsEntity,
    val sectionIds: List<String>,
    val canAccess: Boolean = true,
    val canCreateChildren: Boolean = true,
    @Embedded(prefix = "custom_action_") val customAction: CustomActionEntity? = null
) : OctopusEntity {
    enum class FollowStatus { NOT_FOLLOWED, FOLLOWED, FORCE_FOLLOWED, NOT_FOLLOWABLE }
}