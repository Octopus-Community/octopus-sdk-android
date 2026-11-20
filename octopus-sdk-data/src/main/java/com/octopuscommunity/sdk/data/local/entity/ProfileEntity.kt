package com.octopuscommunity.sdk.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "current_user_profiles")
data class CurrentUserProfileEntity(
    @PrimaryKey val id: Int = 0,
    val userId: String,
    val nickname: String?,
    val originalNickname: String?,
    val email: String? = null,
    val picture: MediaEntity.ResourceEntity? = null,
    val bio: String?,
    @Embedded(prefix = "posts_feeds_") val postsFeeds: FeedsEntity,
    @Embedded(prefix = "comments_feeds_") val commentsFeeds: FeedsEntity = FeedsEntity(null, null),
    val hasSeenOnboarding: Boolean?,
    val hasAcceptedCgu: Boolean?,
    val hasConfirmedNickname: Boolean?,
    val hasConfirmedBio: Boolean?,
    val hasConfirmedPicture: Boolean?,
    val isGuest: Boolean,
    val needMigration: Boolean,
    val createdAt: Long?,
    val tags: List<ProfileTagEntity>,
    val blockedProfiles: List<BlockedProfileEntity> = emptyList(),
    val notSeenNotificationsCount: Int,
    val messageCount: Int?,
    val gamificationLevelIndex: Int?,
    val gamificationScore: Int?,
    val entitlements: List<String> = emptyList(),
    // The community's default avatar: raw server strings, validated before being written, parsed on read
    // (see DefaultAvatarMapping.kt). Either both colors or the image URL.
    val defaultAvatarBackgroundHex: String? = null,
    val defaultAvatarInitialsHex: String? = null,
    val defaultAvatarImageUrl: String? = null,
    // What the default avatar would be if the picture were removed, same format. Never validated against the
    // picture: it is only ever set while there is one.
    val defaultAvatarIfPictureRemovedBackgroundHex: String? = null,
    val defaultAvatarIfPictureRemovedInitialsHex: String? = null,
    val defaultAvatarIfPictureRemovedImageUrl: String? = null,
) {
    @Serializable
    data class BlockedProfileEntity(
        val userId: String,
    )

    data class NotSeenNotificationsCountUpdate(
        val id: Int = 0,
        val notSeenNotificationsCount: Int,
    )
}

enum class ProfileTagEntity { ADMIN }

@Entity(tableName = "public_profiles")
data class PublicProfileEntity(
    @PrimaryKey val userId: String,
    val nickname: String,
    val pictureUrl: String? = null,
    val bio: String?,
    @Embedded(prefix = "posts_feeds_") val postsFeeds: FeedsEntity,
    @Embedded(prefix = "comments_feeds_") val commentsFeeds: FeedsEntity = FeedsEntity(null, null),
    val createdAt: Long?,
    val tags: List<ProfileTagEntity>,
    val messageCount: Int?,
    val gamificationLevelIndex: Int?,
    val clientUserId: String? = null,
    // The community's default avatar, as on CurrentUserProfileEntity.
    val defaultAvatarBackgroundHex: String? = null,
    val defaultAvatarInitialsHex: String? = null,
    val defaultAvatarImageUrl: String? = null,
    // The member's total points: only ever read by OctopusSDK's community-data API, never by the Octopus
    // screens. OtherUserProfile does not carry this total.
    val gamificationScore: Int? = null,
)

/** The gamification standing of a public profile, written from the connected member's own profile. */
data class PublicProfileGamificationUpdate(
    val userId: String,
    val gamificationLevelIndex: Int?,
    val gamificationScore: Int?,
)

@Serializable
data class MinimalProfileEntity(
    val userId: String,
    val nickname: String,
    val pictureUrl: String?,
    val tags: List<ProfileTagEntity>,
    val gamificationLevelIndex: Int?,
    val clientUserId: String? = null,
    // The community's default avatar, as on CurrentUserProfileEntity. Embedded with the `author_` prefix in
    // posts, comments and replies; serialized into the JSON authors of notifications (null when absent).
    val defaultAvatarBackgroundHex: String? = null,
    val defaultAvatarInitialsHex: String? = null,
    val defaultAvatarImageUrl: String? = null,
)
