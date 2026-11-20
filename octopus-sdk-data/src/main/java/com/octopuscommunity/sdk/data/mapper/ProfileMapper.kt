package com.octopuscommunity.sdk.data.mapper

import com.google.protobuf.ByteString
import com.octopuscommunity.GrpcModelProto
import com.octopuscommunity.UserServiceProto
import com.octopuscommunity.UserServiceProto.UpdateProfileResponse.Error.DetailsCase.ALREADYTAKEN
import com.octopuscommunity.UserServiceProto.UpdateProfileResponse.Error.DetailsCase.BADFORMAT
import com.octopuscommunity.UserServiceProto.UpdateProfileResponse.Error.DetailsCase.BANNEDWORDUSED
import com.octopuscommunity.UserServiceProto.UpdateProfileResponse.Error.DetailsCase.CHARLIMITREACHED
import com.octopuscommunity.UserServiceProto.UpdateProfileResponse.Error.Field
import com.octopuscommunity.gamificationScoreOrNull
import com.octopuscommunity.pictureDeleteRequest
import com.octopuscommunity.pictureUpdateRequest
import com.octopuscommunity.pictureUploadRequest
import com.octopuscommunity.profileUpdate
import com.octopuscommunity.sdk.data.local.entity.CurrentUserProfileEntity
import com.octopuscommunity.sdk.data.local.entity.CurrentUserProfileEntity.BlockedProfileEntity
import com.octopuscommunity.sdk.data.local.entity.FeedsEntity
import com.octopuscommunity.sdk.data.local.entity.MediaEntity
import com.octopuscommunity.sdk.data.local.entity.MinimalProfileEntity
import com.octopuscommunity.sdk.data.local.entity.ProfileTagEntity
import com.octopuscommunity.sdk.data.local.entity.PublicProfileEntity
import com.octopuscommunity.sdk.data.local.entity.PublicProfileGamificationUpdate
import com.octopuscommunity.sdk.domain.model.Author
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile.BlockedProfile
import com.octopuscommunity.sdk.domain.model.OctopusCommunityData
import com.octopuscommunity.sdk.domain.model.OctopusGamification
import com.octopuscommunity.sdk.domain.model.OtherUserProfile
import com.octopuscommunity.sdk.domain.model.Profile
import com.octopuscommunity.sdk.domain.model.Resource
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateError.BioError
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateError.EmailError
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateError.NicknameError
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateError.OtherError
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateError.PictureError
import java.util.Date

class ProfileMapper(
    private val mediaMapper: MediaMapper,
    private val feedMapper: FeedMapper,
) : UserMapper(mediaMapper) {
    fun UserServiceProto.PrivateProfile.toEntity(): CurrentUserProfileEntity {
        val defaultAvatarColumns = protoDefaultAvatarColumns(hasDefaultAvatar(), defaultAvatar)
        // Not normalized against the picture: it only comes with one, which it does not replace.
        val ifPictureRemovedColumns =
            protoDefaultAvatarColumns(hasDefaultAvatarIfPictureRemoved(), defaultAvatarIfPictureRemoved)
        return CurrentUserProfileEntity(
            userId = id,
            nickname = nickname?.ifEmpty { null },
            originalNickname = originalNickname?.ifEmpty { null },
            email = email,
            picture = normalizedPictureUrl(pictureUrl, defaultAvatarColumns)?.let { MediaEntity.ResourceEntity(url = it) },
            bio = bio?.ifEmpty { null },
            postsFeeds =
                FeedsEntity(
                    descendingId = descPostFeedId,
                    ascendingId = ascPostFeedId,
                ),
            commentsFeeds =
                FeedsEntity(
                    descendingId = descCommentFeedId,
                    ascendingId = ascCommentFeedId,
                ),
            notSeenNotificationsCount = notSeenNotificationsCount,
            blockedProfiles = usersBlockListList.map { BlockedProfileEntity(it) },
            hasSeenOnboarding = hasSeenOnboarding.takeIf { hasHasSeenOnboarding() },
            hasAcceptedCgu = hasAcceptedCgu.takeIf { hasHasAcceptedCgu() },
            hasConfirmedNickname = hasConfirmedNickname.takeIf { hasHasConfirmedNickname() },
            hasConfirmedBio = hasConfirmedBio.takeIf { hasHasConfirmedBio() },
            hasConfirmedPicture = hasConfirmedPicture.takeIf { hasHasConfirmedPicture() },
            isGuest = isGuest,
            needMigration = !hasHasConfirmedNickname(),
            createdAt = accountCreatedAt,
            tags = tagsList.mapNotNull { it.toEntity() },
            messageCount = totalMessages,
            gamificationLevelIndex = gamificationScoreOrNull?.level,
            gamificationScore = gamificationScoreOrNull?.score,
            entitlements = entitlementsList,
            defaultAvatarBackgroundHex = defaultAvatarColumns?.backgroundHex,
            defaultAvatarInitialsHex = defaultAvatarColumns?.initialsHex,
            defaultAvatarImageUrl = defaultAvatarColumns?.imageUrl,
            defaultAvatarIfPictureRemovedBackgroundHex = ifPictureRemovedColumns?.backgroundHex,
            defaultAvatarIfPictureRemovedInitialsHex = ifPictureRemovedColumns?.initialsHex,
            defaultAvatarIfPictureRemovedImageUrl = ifPictureRemovedColumns?.imageUrl,
        )
    }

    fun CurrentUserProfileEntity.toDomain() =
        CurrentUserProfile(
            userId = userId,
            nickname = nickname ?: "",
            originalNickname = originalNickname,
            email = email,
            picture = with(mediaMapper) { picture?.toDomain() },
            bio = bio?.trim()?.ifEmpty { null },
            postsFeeds = with(feedMapper) { postsFeeds.toDomain() },
            commentsFeeds = with(feedMapper) { commentsFeeds.toDomain() },
            gamificationLevelIndex = gamificationLevelIndex,
            notSeenNotificationsCount = notSeenNotificationsCount,
            blockedProfiles = blockedProfiles.map { it.toDomain() },
            hasSeenOnboarding = hasSeenOnboarding ?: false,
            hasAcceptedCgu = hasAcceptedCgu ?: false,
            hasConfirmedNickname = hasConfirmedNickname ?: false,
            hasConfirmedBio = hasConfirmedBio ?: false,
            hasConfirmedPicture = hasConfirmedPicture ?: false,
            isGuest = isGuest,
            needMigration = needMigration,
            messageCount = messageCount,
            creationDate = createdAt?.let { Date(it) },
            tags = tags.map { it.toDomain() },
            gamificationScore = gamificationScore,
            entitlements = entitlements.toSet(),
            defaultAvatar = defaultAvatarOf(defaultAvatarBackgroundHex, defaultAvatarInitialsHex, defaultAvatarImageUrl),
            defaultAvatarIfPictureRemoved =
                defaultAvatarOf(
                    defaultAvatarIfPictureRemovedBackgroundHex,
                    defaultAvatarIfPictureRemovedInitialsHex,
                    defaultAvatarIfPictureRemovedImageUrl,
                ),
        )

    fun CurrentUserProfile.toEntity(): CurrentUserProfileEntity {
        val defaultAvatarColumns = defaultAvatar.toColumns()
        val ifPictureRemovedColumns = defaultAvatarIfPictureRemoved.toColumns()
        return CurrentUserProfileEntity(
            userId = userId,
            nickname = nickname,
            originalNickname = originalNickname,
            email = email,
            picture = with(mediaMapper) { picture?.toEntity() },
            bio = bio,
            postsFeeds = with(feedMapper) { postsFeeds.toEntity() },
            commentsFeeds = with(feedMapper) { commentsFeeds.toEntity() },
            notSeenNotificationsCount = notSeenNotificationsCount,
            blockedProfiles = blockedProfiles.map { it.toEntity() },
            hasSeenOnboarding = hasSeenOnboarding,
            hasAcceptedCgu = hasAcceptedCgu,
            hasConfirmedNickname = hasConfirmedNickname,
            hasConfirmedBio = hasConfirmedBio,
            hasConfirmedPicture = hasConfirmedPicture,
            isGuest = isGuest,
            needMigration = needMigration,
            createdAt = creationDate?.time,
            tags = tags.map { it.toEntity() },
            messageCount = messageCount,
            gamificationLevelIndex = gamificationLevelIndex,
            gamificationScore = gamificationScore,
            entitlements = entitlements.toList(),
            defaultAvatarBackgroundHex = defaultAvatarColumns.backgroundHex,
            defaultAvatarInitialsHex = defaultAvatarColumns.initialsHex,
            defaultAvatarImageUrl = defaultAvatarColumns.imageUrl,
            defaultAvatarIfPictureRemovedBackgroundHex = ifPictureRemovedColumns.backgroundHex,
            defaultAvatarIfPictureRemovedInitialsHex = ifPictureRemovedColumns.initialsHex,
            defaultAvatarIfPictureRemovedImageUrl = ifPictureRemovedColumns.imageUrl,
        )
    }

    /**
     * The connected member's own level and total points, to refresh the cached public profile that
     * `OctopusSDK.communityDataFlow` reads for the same member: the private profile is the one the SDK
     * fetches again after the member earns points. The DAO must only apply this to a public profile
     * already marked as gamified: a private score block is also sent when gamification is disabled.
     * An admin-tagged profile carries no gamification on its public profile, so none is written for it.
     */
    fun CurrentUserProfileEntity.toPublicProfileGamificationUpdate(): PublicProfileGamificationUpdate {
        val isAdmin = ProfileTagEntity.ADMIN in tags
        return PublicProfileGamificationUpdate(
            userId = userId,
            gamificationLevelIndex = gamificationLevelIndex.takeUnless { isAdmin },
            gamificationScore = gamificationScore.takeUnless { isAdmin },
        )
    }

    fun UserServiceProto.PrivateProfile.toNotSeenNotificationsCountUpdateEntity() =
        CurrentUserProfileEntity.NotSeenNotificationsCountUpdate(
            notSeenNotificationsCount = notSeenNotificationsCount,
        )

    fun CurrentUserProfileEntity.NotSeenNotificationsCountUpdate.toDomain() = notSeenNotificationsCount

    fun BlockedProfile.toEntity() =
        BlockedProfileEntity(
            userId = userId,
        )

    fun BlockedProfileEntity.toDomain() =
        BlockedProfile(
            userId = userId,
        )

    /**
     * @param failOnPictureEncodingError When `true`, a profile picture that cannot be encoded raises
     *   [MediaEncodingException] rather than letting the update through without it. Only the two
     *   paths where the member picked the picture themselves (`createProfile`, `updateProfile`)
     *   pass `true`; the SSO profile sync passes `false`, since its picture comes from the host's
     *   `ClientUser` and failing a login over an avatar the member never chose would be the worse
     *   regression. It carries **no default**: tolerance is the behaviour that hid internal change, so every
     *   call site says which one it wants. Either way the failure is logged, and either way the
     *   `picture` field is left out entirely rather than sent empty — an upload request with no
     *   `file` is what the backend reads as "this member has no avatar", which is the silent reset
     *   of internal change.
     *
     * @throws MediaEncodingException if [failOnPictureEncodingError] is `true` and the picture
     *   could not be encoded.
     */
    suspend fun CurrentUserProfile.toProto(
        previousProfile: CurrentUserProfile?,
        findAvailableNickname: Boolean = false,
        failOnPictureEncodingError: Boolean,
    ) = profileUpdate {
        this@toProto.email.takeIf { it != previousProfile?.email }?.let {
            email = it
        }
        if (this@toProto.nickname != previousProfile?.nickname) {
            nickname = this@toProto.nickname
        }
        val picture = this@toProto.picture
        if (picture != previousProfile?.picture) {
            // The bytes are computed BEFORE the field is built, because on the tolerant path there
            // may be none: emitting `picture` anyway would send an upload request with no `file`,
            // which the backend reads as an avatar reset the member never asked for (internal change).
            val encoded =
                picture?.let {
                    with(mediaMapper) {
                        if (failOnPictureEncodingError) {
                            it.encodeToByteString(
                                maxSize = CurrentUserProfile.IMAGE_UPLOAD_MAX_SIZE,
                                quality = CurrentUserProfile.IMAGE_UPLOAD_QUALITY,
                            )
                        } else {
                            it.toByteString(
                                maxSize = CurrentUserProfile.IMAGE_UPLOAD_MAX_SIZE,
                                quality = CurrentUserProfile.IMAGE_UPLOAD_QUALITY,
                            )
                        }
                    }
                }
            pictureUpdateOrNull(hasPicture = picture != null, encoded = encoded)?.let {
                this.picture = it
            }
        }
        if (this@toProto.bio != previousProfile?.bio) {
            bio = this@toProto.bio ?: ""
        }
        if (this@toProto.hasSeenOnboarding != previousProfile?.hasSeenOnboarding || needMigration) {
            hasSeenOnboarding = this@toProto.hasSeenOnboarding
        }
        if (this@toProto.hasAcceptedCgu != previousProfile?.hasAcceptedCgu || needMigration) {
            hasAcceptedCgu = this@toProto.hasAcceptedCgu
        }
        if (this@toProto.hasConfirmedNickname != previousProfile?.hasConfirmedNickname || needMigration) {
            hasConfirmedNickname = this@toProto.hasConfirmedNickname
        }
        if (this@toProto.hasConfirmedBio != previousProfile?.hasConfirmedBio || needMigration) {
            hasConfirmedBio = this@toProto.hasConfirmedBio
        }
        if (this@toProto.hasConfirmedPicture != previousProfile?.hasConfirmedPicture || needMigration) {
            hasConfirmedPicture = this@toProto.hasConfirmedPicture
        }
        optFindAvailableNickname = findAvailableNickname
    }

    fun UserServiceProto.UpdateProfileResponse.Error.toDomain() =
        when (field) {
            Field.NICKNAME ->
                when (detailsCase) {
                    ALREADYTAKEN -> NicknameError.AlreadyUsed(message, alreadyTaken.suggestionsList)
                    BADFORMAT -> NicknameError.BadFormat(message)
                    BANNEDWORDUSED ->
                        NicknameError.BannedWords(
                            errorMessage = message,
                            bannedWords = bannedWordUsed.wordsList,
                        )

                    CHARLIMITREACHED -> NicknameError.CharLimitReached(message)

                    else -> NicknameError.Unknown(message)
                }

            Field.EMAIL ->
                when (detailsCase) {
                    ALREADYTAKEN -> EmailError.AlreadyTaken(message)
                    BADFORMAT -> EmailError.BadFormat(message)
                    else -> EmailError.Unknown(message)
                }

            Field.PROFILE_PICTURE -> PictureError.Unknown(message)
            Field.BIO ->
                when (detailsCase) {
                    BANNEDWORDUSED ->
                        BioError.BannedWords(
                            errorMessage = message,
                            bannedWords = bannedWordUsed.wordsList,
                        )

                    BADFORMAT -> BioError.BadFormat(message)
                    CHARLIMITREACHED -> BioError.CharLimitReached(message)

                    else -> BioError.Unknown(message)
                }

            else -> OtherError(message)
        }

    fun MinimalProfileEntity.toDomain() =
        Author(
            userId = userId,
            nickname = nickname,
            picture = pictureUrl?.ifEmpty { null }?.let { Resource.Remote(it) },
            tags = tags.map { it.toDomain() },
            gamificationLevelIndex = gamificationLevelIndex,
            clientUserId = clientUserId,
            defaultAvatar = defaultAvatarOf(defaultAvatarBackgroundHex, defaultAvatarInitialsHex, defaultAvatarImageUrl),
        )

    fun Author.toEntity(): MinimalProfileEntity {
        val defaultAvatarColumns = defaultAvatar.toColumns()
        return MinimalProfileEntity(
            userId = userId,
            nickname = nickname,
            pictureUrl = picture?.url,
            tags = tags.map { it.toEntity() },
            gamificationLevelIndex = gamificationLevelIndex,
            clientUserId = clientUserId,
            defaultAvatarBackgroundHex = defaultAvatarColumns.backgroundHex,
            defaultAvatarInitialsHex = defaultAvatarColumns.initialsHex,
            defaultAvatarImageUrl = defaultAvatarColumns.imageUrl,
        )
    }

    fun GrpcModelProto.MinimalProfile.toEntity(): MinimalProfileEntity {
        val defaultAvatarColumns = protoDefaultAvatarColumns(hasDefaultAvatar(), defaultAvatar)
        return MinimalProfileEntity(
            userId = profileId,
            nickname = nickname,
            pictureUrl = normalizedPictureUrl(avatarUrl, defaultAvatarColumns),
            tags = tagsList.mapNotNull { it.toEntity() },
            gamificationLevelIndex = gamificationLevel,
            // MinimalProfile.clientUserId = 6 (gRPC contract)
            clientUserId = protoClientUserId(hasClientUserId(), clientUserId),
            defaultAvatarBackgroundHex = defaultAvatarColumns?.backgroundHex,
            defaultAvatarInitialsHex = defaultAvatarColumns?.initialsHex,
            defaultAvatarImageUrl = defaultAvatarColumns?.imageUrl,
        )
    }

    fun PublicProfileEntity.toDomain() =
        OtherUserProfile(
            userId = userId,
            nickname = nickname,
            picture = pictureUrl?.ifEmpty { null }?.let { Resource.Remote(url = it) },
            bio = bio?.trim(),
            postsFeeds = with(feedMapper) { postsFeeds.toDomain() },
            commentsFeeds = with(feedMapper) { commentsFeeds.toDomain() },
            messageCount = messageCount,
            creationDate = createdAt?.let { Date(it) },
            tags = tags.map { it.toDomain() },
            gamificationLevelIndex = gamificationLevelIndex,
            clientUserId = clientUserId,
            defaultAvatar = defaultAvatarOf(defaultAvatarBackgroundHex, defaultAvatarInitialsHex, defaultAvatarImageUrl),
        )

    fun UserServiceProto.PublicProfile.toEntity(): PublicProfileEntity {
        val defaultAvatarColumns = protoDefaultAvatarColumns(hasDefaultAvatar(), defaultAvatar)
        return PublicProfileEntity(
            userId = id,
            nickname = nickname,
            pictureUrl = normalizedPictureUrl(pictureUrl, defaultAvatarColumns),
            bio = bio?.ifEmpty { null },
            postsFeeds =
                FeedsEntity(
                    descendingId = descPostFeedId,
                    ascendingId = ascPostFeedId,
                ),
            commentsFeeds =
                FeedsEntity(
                    descendingId = descCommentFeedId,
                    ascendingId = ascCommentFeedId,
                ),
            createdAt = if (hasAccountCreatedAt()) accountCreatedAt else null,
            tags = tagsList.mapNotNull { it.toEntity() },
            messageCount = if (hasTotalMessages()) totalMessages else null,
            gamificationLevelIndex = gamificationScoreOrNull?.level,
            // Null when the server does not send the total; absent when gamification is off or the profile is
            // tagged admin. Read directly from Room by the community-data API, never by profile UI.
            gamificationScore = gamificationScoreOrNull?.takeIf { it.hasScore() }?.score,
            // PublicProfile.clientUserId = 50 (gRPC contract)
            clientUserId = protoClientUserId(hasClientUserId(), clientUserId),
            defaultAvatarBackgroundHex = defaultAvatarColumns?.backgroundHex,
            defaultAvatarInitialsHex = defaultAvatarColumns?.initialsHex,
            defaultAvatarImageUrl = defaultAvatarColumns?.imageUrl,
        )
    }

    fun ProfileTagEntity.toDomain() =
        when (this) {
            ProfileTagEntity.ADMIN -> Profile.Tag.ADMIN
        }

    fun Profile.Tag.toEntity() =
        when (this) {
            Profile.Tag.ADMIN -> ProfileTagEntity.ADMIN
        }

    fun GrpcModelProto.ProfileTag.toEntity() =
        when (this) {
            GrpcModelProto.ProfileTag.ADMIN -> ProfileTagEntity.ADMIN
            GrpcModelProto.ProfileTag.UNDEFINED,
            GrpcModelProto.ProfileTag.UNRECOGNIZED,
            -> null
        }
}

/**
 * Normalizes a proto `client_user_id` (Unified Profile) for storage: absent field or empty string →
 * `null`. An empty string would read as "this member has a host profile" to the Unified Profile
 * profile-tap gate (see `resolveProfileTapTarget`) — a false positive the backend can produce, so it
 * is dropped defensively at the mapping boundary. Mirrors iOS's `.nilIfEmpty` normalization
 * (`MinimalProfile` / `StorableProfile`).
 */
internal fun protoClientUserId(
    hasClientUserId: Boolean,
    clientUserId: String,
): String? = if (hasClientUserId) clientUserId.ifEmpty { null } else null

/**
 * Builds the `picture` field of a profile update, or returns `null` when the field must not be sent
 * at all.
 *
 * Three cases, and the third is the point:
 * - the member removed their picture → an explicit delete request;
 * - the picture was encoded → an upload request carrying the bytes;
 * - the picture could not be encoded on the tolerant path (SSO sync) → **nothing**. Before internal change an
 *   upload request was emitted anyway with no `file` in it, which the backend applies as "this
 *   member has no avatar": one unreadable `ClientUser` picture silently wiped the avatar the member
 *   had. Leaving the field out keeps the stored picture exactly as it was, and the encoder has
 *   already logged why nothing was sent.
 *
 * @param hasPicture Whether the profile carries a picture at all — `false` means the member removed
 *   it, which is a deliberate deletion and not an encoding failure.
 * @param encoded The encoded bytes and whether they are a lossy encoding, or `null` when encoding
 *   failed and the caller chose to tolerate it.
 */
internal fun pictureUpdateOrNull(
    hasPicture: Boolean,
    encoded: Pair<ByteString, Boolean>?,
) = when {
    !hasPicture -> pictureUpdateRequest { delete = pictureDeleteRequest {} }

    encoded == null -> null

    else ->
        pictureUpdateRequest {
            new =
                pictureUploadRequest {
                    file = encoded.first
                    isOptimized = encoded.second
                }
        }
}

/** Maps cached totals directly to host community data, without exposing them on OtherUserProfile. */
internal fun PublicProfileEntity.toOctopusCommunityData(): OctopusCommunityData =
    OctopusCommunityData(
        userId = userId,
        messageCount = messageCount,
        gamification =
            gamificationLevelIndex
                ?.takeUnless { ProfileTagEntity.ADMIN in tags }
                ?.let { OctopusGamification(level = it, score = gamificationScore) },
    )
