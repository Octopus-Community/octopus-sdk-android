package com.octopuscommunity.sdk.domain.model

import com.octopuscommunity.sdk.domain.model.CurrentUserProfile.BlockedProfile
import com.octopuscommunity.sdk.domain.model.OctopusItem.Feeds
import java.util.Date

/** Editable fields of a profile. */
interface OctopusProfile {
    val nickname: String
    val picture: Resource?
    val bio: String?
}

/** Fields shared by every profile, including the authors of items. */
interface MinimalProfile {
    val userId: String
    val nickname: String
    val picture: Resource?
    val gamificationLevelIndex: Int?
    val tags: List<Profile.Tag>

    /**
     * The user's id in the host app's own system (Unified Profile). For other members it is non-null
     * only when the community exposes client user ids ([CommunityConfig.exposeClientUserId]) and the
     * member has one — absent for Octopus-auth users, guests and BO/admin-created profiles. For the
     * connected user ([CurrentUserProfile]) in SSO mode it carries the id passed to `connectUser`
     * (held locally, independent of the backend flag). The host receives it through
     * `onNavigateToProfile` so it can open its own profile screen for the tapped member. Defaults to
     * `null`.
     */
    val clientUserId: String? get() = null

    /**
     * What to draw for this member instead of a [picture] they do not have: the default avatar their
     * community chose. `null` when the member has a picture, or when the community leaves it to the SDK
     * (initials on a color computed from the nickname). Defaults to `null`.
     */
    val defaultAvatar: DefaultAvatar? get() = null

    /** Whether this profile is in [blockedProfiles]. */
    fun isBlocked(blockedProfiles: List<BlockedProfile>?) = blockedProfiles?.any { it.userId == userId } ?: false
}

/** Author of an item. */
data class Author(
    override val userId: String,
    override val nickname: String,
    override val picture: Resource.Remote?,
    override val gamificationLevelIndex: Int?,
    override val tags: List<Profile.Tag>,
    override val clientUserId: String? = null,
    /** See [MinimalProfile.defaultAvatar]. Appended last so positional calls keep their meaning. */
    override val defaultAvatar: DefaultAvatar? = null,
) : MinimalProfile {
    /** The constructor as it was before [defaultAvatar], kept so code compiled against it keeps linking. */
    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    constructor(
        userId: String,
        nickname: String,
        picture: Resource.Remote?,
        gamificationLevelIndex: Int?,
        tags: List<Profile.Tag>,
        clientUserId: String? = null,
    ) : this(
        userId = userId,
        nickname = nickname,
        picture = picture,
        gamificationLevelIndex = gamificationLevelIndex,
        tags = tags,
        clientUserId = clientUserId,
        defaultAvatar = null,
    )

    /** `copy` as it was before [defaultAvatar], which it keeps; kept so code compiled against it keeps linking. */
    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun copy(
        userId: String = this.userId,
        nickname: String = this.nickname,
        picture: Resource.Remote? = this.picture,
        gamificationLevelIndex: Int? = this.gamificationLevelIndex,
        tags: List<Profile.Tag> = this.tags,
        clientUserId: String? = this.clientUserId,
    ): Author =
        Author(
            userId = userId,
            nickname = nickname,
            picture = picture,
            gamificationLevelIndex = gamificationLevelIndex,
            tags = tags,
            clientUserId = clientUserId,
            defaultAvatar = defaultAvatar,
        )
}

/** A full profile, of the current user or of another user. */
interface Profile :
    MinimalProfile,
    OctopusProfile {
    /** Tags of a profile. */
    enum class Tag { ADMIN, }

    override val bio: String?

    /** Whether the profile has a picture. */
    val hasPicture get() = picture != null

    /** Feeds of the posts written by this profile. */
    val postsFeeds: Feeds

    /**
     * Feed of comments/replies authored by this profile ("Comments" profile tab).
     *
     * Defaulted so adding it stays source-compatible for any external implementer of this interface.
     */
    val commentsFeeds: Feeds get() = Feeds()

    /** Number of messages written by this profile, when known. */
    val messageCount: Int?

    /** Creation date of the profile, when known. */
    val creationDate: Date?

    /** Gamification score of the profile, when known. */
    val gamificationScore: Int? get() = null

    /** Whether [currentUserProfile] can report this profile. */
    fun canBeReported(currentUserProfile: CurrentUserProfile?) = currentUserProfile != null

    /** Whether [currentUserProfile] can block this profile: not already blocked and not an admin. */
    fun canBeBlocked(currentUserProfile: CurrentUserProfile?) =
        currentUserProfile != null &&
            !isBlocked(currentUserProfile.blockedProfiles) &&
            Tag.ADMIN !in tags
}

/** Profile of the current user, which may be a guest. */
data class CurrentUserProfile(
    override val userId: String = "",
    override val nickname: String = "",
    override val picture: Resource? = null,
    override val bio: String? = null,
    override val postsFeeds: Feeds = Feeds(),
    override val gamificationLevelIndex: Int? = null,
    override val messageCount: Int? = null,
    override val creationDate: Date? = null,
    override val tags: List<Profile.Tag> = emptyList(),
    override val gamificationScore: Int? = null,
    val originalNickname: String? = null,
    val email: String? = null,
    /** Number of notifications the user has not seen. */
    val notSeenNotificationsCount: Int = 0,
    /** Profiles the user blocked. */
    val blockedProfiles: List<BlockedProfile> = emptyList(),
    val hasSeenOnboarding: Boolean = true,
    val hasAcceptedCgu: Boolean = false,
    val hasConfirmedNickname: Boolean = false,
    val hasConfirmedBio: Boolean = false,
    val hasConfirmedPicture: Boolean = false,
    /** Whether the user is a guest. */
    val isGuest: Boolean = true,
    val needMigration: Boolean = false,
    /**
     * Held entitlement identifiers (opaque tokens defined by the host app).
     *
     * Display only — the SDK never intersects this set against per-group requirements.
     * Group access decisions are pre-resolved by the backend and surfaced via
     * [OctopusGroup.canAccess].
     */
    val entitlements: Set<String> = emptySet(),
    /**
     * The connected user's id in the host app's own system (Unified Profile), from `connectUser`.
     * Populated in SSO mode for a non-guest user; `null` otherwise. See [MinimalProfile.clientUserId].
     */
    override val clientUserId: String? = null,
    /**
     * Appended last on purpose (internal ticket): inserting it next to [postsFeeds] would renumber every
     * `componentN` after it, silently changing the meaning of positional construction and
     * destructuring of this public model across a minor version.
     */
    override val commentsFeeds: Feeds = Feeds(),
    /**
     * See [MinimalProfile.defaultAvatar]. Shown on the user's own profile, where it does not count as a
     * picture: [hasPicture] stays `false`, so adding a picture is offered and removing one is not. Appended
     * last for the reason given on [commentsFeeds].
     */
    override val defaultAvatar: DefaultAvatar? = null,
    /**
     * The default avatar the user would get if they removed their picture now, which the profile editor
     * previews once they remove it, before they save. Only set while the user has a picture and the
     * community has its own default avatar, and never together with [defaultAvatar]; the profile saved
     * afterwards then carries the actual default in [defaultAvatar]. Appended last for the reason given
     * on [commentsFeeds].
     */
    val defaultAvatarIfPictureRemoved: DefaultAvatar? = null,
) : Profile {
    /** The constructor as it was before [defaultAvatar], kept so code compiled against it keeps linking. */
    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    constructor(
        userId: String = "",
        nickname: String = "",
        picture: Resource? = null,
        bio: String? = null,
        postsFeeds: Feeds = Feeds(),
        gamificationLevelIndex: Int? = null,
        messageCount: Int? = null,
        creationDate: Date? = null,
        tags: List<Profile.Tag> = emptyList(),
        gamificationScore: Int? = null,
        originalNickname: String? = null,
        email: String? = null,
        notSeenNotificationsCount: Int = 0,
        blockedProfiles: List<BlockedProfile> = emptyList(),
        hasSeenOnboarding: Boolean = true,
        hasAcceptedCgu: Boolean = false,
        hasConfirmedNickname: Boolean = false,
        hasConfirmedBio: Boolean = false,
        hasConfirmedPicture: Boolean = false,
        isGuest: Boolean = true,
        needMigration: Boolean = false,
        entitlements: Set<String> = emptySet(),
        clientUserId: String? = null,
        commentsFeeds: Feeds = Feeds(),
    ) : this(
        userId = userId,
        nickname = nickname,
        picture = picture,
        bio = bio,
        postsFeeds = postsFeeds,
        gamificationLevelIndex = gamificationLevelIndex,
        messageCount = messageCount,
        creationDate = creationDate,
        tags = tags,
        gamificationScore = gamificationScore,
        originalNickname = originalNickname,
        email = email,
        notSeenNotificationsCount = notSeenNotificationsCount,
        blockedProfiles = blockedProfiles,
        hasSeenOnboarding = hasSeenOnboarding,
        hasAcceptedCgu = hasAcceptedCgu,
        hasConfirmedNickname = hasConfirmedNickname,
        hasConfirmedBio = hasConfirmedBio,
        hasConfirmedPicture = hasConfirmedPicture,
        isGuest = isGuest,
        needMigration = needMigration,
        entitlements = entitlements,
        clientUserId = clientUserId,
        commentsFeeds = commentsFeeds,
        defaultAvatar = null,
        defaultAvatarIfPictureRemoved = null,
    )

    /**
     * `copy` as it was before [defaultAvatar], which it keeps along with [defaultAvatarIfPictureRemoved];
     * kept so code compiled against it keeps linking.
     */
    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun copy(
        userId: String = this.userId,
        nickname: String = this.nickname,
        picture: Resource? = this.picture,
        bio: String? = this.bio,
        postsFeeds: Feeds = this.postsFeeds,
        gamificationLevelIndex: Int? = this.gamificationLevelIndex,
        messageCount: Int? = this.messageCount,
        creationDate: Date? = this.creationDate,
        tags: List<Profile.Tag> = this.tags,
        gamificationScore: Int? = this.gamificationScore,
        originalNickname: String? = this.originalNickname,
        email: String? = this.email,
        notSeenNotificationsCount: Int = this.notSeenNotificationsCount,
        blockedProfiles: List<BlockedProfile> = this.blockedProfiles,
        hasSeenOnboarding: Boolean = this.hasSeenOnboarding,
        hasAcceptedCgu: Boolean = this.hasAcceptedCgu,
        hasConfirmedNickname: Boolean = this.hasConfirmedNickname,
        hasConfirmedBio: Boolean = this.hasConfirmedBio,
        hasConfirmedPicture: Boolean = this.hasConfirmedPicture,
        isGuest: Boolean = this.isGuest,
        needMigration: Boolean = this.needMigration,
        entitlements: Set<String> = this.entitlements,
        clientUserId: String? = this.clientUserId,
        commentsFeeds: Feeds = this.commentsFeeds,
    ): CurrentUserProfile =
        CurrentUserProfile(
            userId = userId,
            nickname = nickname,
            picture = picture,
            bio = bio,
            postsFeeds = postsFeeds,
            gamificationLevelIndex = gamificationLevelIndex,
            messageCount = messageCount,
            creationDate = creationDate,
            tags = tags,
            gamificationScore = gamificationScore,
            originalNickname = originalNickname,
            email = email,
            notSeenNotificationsCount = notSeenNotificationsCount,
            blockedProfiles = blockedProfiles,
            hasSeenOnboarding = hasSeenOnboarding,
            hasAcceptedCgu = hasAcceptedCgu,
            hasConfirmedNickname = hasConfirmedNickname,
            hasConfirmedBio = hasConfirmedBio,
            hasConfirmedPicture = hasConfirmedPicture,
            isGuest = isGuest,
            needMigration = needMigration,
            entitlements = entitlements,
            clientUserId = clientUserId,
            commentsFeeds = commentsFeeds,
            defaultAvatar = defaultAvatar,
            defaultAvatarIfPictureRemoved = defaultAvatarIfPictureRemoved,
        )

    /** A profile blocked by the current user. */
    data class BlockedProfile(
        val userId: String,
    )

    companion object {
        /** Allowed nickname length. */
        val NICKNAME_LENGTH = 3..20

        /** Allowed bio length. */
        val BIO_LENGTH = 0..200
        const val IMAGE_UPLOAD_MAX_SIZE = Media.Image.UPLOAD_MAX_DIMENSION
        const val IMAGE_UPLOAD_QUALITY = Media.Image.UPLOAD_QUALITY
    }
}

/** Profile of another user. */
data class OtherUserProfile(
    override val userId: String = "",
    override val nickname: String = "",
    override val picture: Resource.Remote? = null,
    override val bio: String? = null,
    override val postsFeeds: Feeds,
    override val messageCount: Int?,
    override val creationDate: Date?,
    override val tags: List<Profile.Tag>,
    override val gamificationLevelIndex: Int?,
    override val clientUserId: String? = null,
    /** Appended last on purpose — see [CurrentUserProfile.commentsFeeds]. */
    override val commentsFeeds: Feeds = Feeds(),
    /** See [MinimalProfile.defaultAvatar]. Appended last — see [CurrentUserProfile.commentsFeeds]. */
    override val defaultAvatar: DefaultAvatar? = null,
) : Profile {
    /** The constructor as it was before [defaultAvatar], kept so code compiled against it keeps linking. */
    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    constructor(
        userId: String = "",
        nickname: String = "",
        picture: Resource.Remote? = null,
        bio: String? = null,
        postsFeeds: Feeds,
        messageCount: Int?,
        creationDate: Date?,
        tags: List<Profile.Tag>,
        gamificationLevelIndex: Int?,
        clientUserId: String? = null,
        commentsFeeds: Feeds = Feeds(),
    ) : this(
        userId = userId,
        nickname = nickname,
        picture = picture,
        bio = bio,
        postsFeeds = postsFeeds,
        messageCount = messageCount,
        creationDate = creationDate,
        tags = tags,
        gamificationLevelIndex = gamificationLevelIndex,
        clientUserId = clientUserId,
        commentsFeeds = commentsFeeds,
        defaultAvatar = null,
    )

    /** `copy` as it was before [defaultAvatar], which it keeps; kept so code compiled against it keeps linking. */
    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun copy(
        userId: String = this.userId,
        nickname: String = this.nickname,
        picture: Resource.Remote? = this.picture,
        bio: String? = this.bio,
        postsFeeds: Feeds = this.postsFeeds,
        messageCount: Int? = this.messageCount,
        creationDate: Date? = this.creationDate,
        tags: List<Profile.Tag> = this.tags,
        gamificationLevelIndex: Int? = this.gamificationLevelIndex,
        clientUserId: String? = this.clientUserId,
        commentsFeeds: Feeds = this.commentsFeeds,
    ): OtherUserProfile =
        OtherUserProfile(
            userId = userId,
            nickname = nickname,
            picture = picture,
            bio = bio,
            postsFeeds = postsFeeds,
            messageCount = messageCount,
            creationDate = creationDate,
            tags = tags,
            gamificationLevelIndex = gamificationLevelIndex,
            clientUserId = clientUserId,
            commentsFeeds = commentsFeeds,
            defaultAvatar = defaultAvatar,
        )
}

/**
 * The field of a Profile
 */
enum class ProfileField {
    /** Username */
    NICKNAME,

    /** Bio */
    BIO,

    /** Profile picture */
    PICTURE,

    ;

    companion object {
        val ALL = entries.toSet()
    }
}
