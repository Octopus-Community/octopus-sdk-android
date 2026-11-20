package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.profileOrNull
import com.octopuscommunity.sdk.data.local.dao.ProfileDao
import com.octopuscommunity.sdk.data.local.dao.UserConfigDao
import com.octopuscommunity.sdk.data.local.dao.updatePublicProfileGamification
import com.octopuscommunity.sdk.data.local.dao.upsertPublicProfile
import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.data.local.datastore.SecuredDataStore
import com.octopuscommunity.sdk.data.mapper.MediaEncodingException
import com.octopuscommunity.sdk.data.mapper.ProfileMapper
import com.octopuscommunity.sdk.data.mapper.UserConfigMapper
import com.octopuscommunity.sdk.data.mapper.UserMapper
import com.octopuscommunity.sdk.data.mapper.catchingMediaEncoding
import com.octopuscommunity.sdk.data.mapper.toOctopusCommunityData
import com.octopuscommunity.sdk.data.remote.service.NotificationService
import com.octopuscommunity.sdk.data.remote.service.UserService
import com.octopuscommunity.sdk.data.utils.call
import com.octopuscommunity.sdk.data.utils.callWithValidation
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile.BlockedProfile
import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.OctopusCommunityData
import com.octopuscommunity.sdk.domain.model.Profile
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.OctopusResult.Failure.ContentUnavailable
import com.octopuscommunity.sdk.domain.network.OctopusResult.Failure.InvalidArguments
import com.octopuscommunity.sdk.domain.network.OctopusResult.Failure.PermissionDenied
import com.octopuscommunity.sdk.domain.network.OctopusResult.Failure.UserNotAuthenticated
import com.octopuscommunity.sdk.domain.network.mapSuccess
import com.octopuscommunity.sdk.domain.repository.CommunityClosureRepository
import com.octopuscommunity.sdk.domain.repository.ProfileRepository
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateError
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

open class ProfileRepositoryImpl(
    securedDataStore: SecuredDataStore,
    preferencesDataStore: PreferencesDataStore,
    userConfigDao: UserConfigDao,
    protected val profileDao: ProfileDao,
    userService: UserService,
    notificationService: NotificationService,
    userMapper: UserMapper,
    userConfigMapper: UserConfigMapper,
    protected val profileMapper: ProfileMapper,
    closureRepository: CommunityClosureRepository = OpenCommunityClosureRepository,
) : UserRepositoryImpl(
        securedDataStore,
        preferencesDataStore,
        userConfigDao,
        userService,
        notificationService,
        userMapper,
        userConfigMapper,
        closureRepository,
    ),
    ProfileRepository {
    override suspend fun getProfile() =
        with(profileMapper) {
            getUser()?.let { user ->
                profileDao.getCurrentUserProfile()?.toDomain()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getProfileFlow() =
        with(profileMapper) {
            profileDao.getCurrentUserProfileFlow().map { it?.toDomain() }
        }

    override suspend fun fetchProfile(): OctopusResult<CurrentUserProfile?, Nothing> =
        call(profileMapper) {
            val userId = getUser()?.id ?: return@call null

            val gamificationTicket = ProfileGamificationOrder.nextTicket()
            val response =
                userService.getPrivateProfile(
                    userId = userId,
                    fetchUserBlockList = true,
                    fetchNotificationsBadge = true,
                    fetchGamification = true,
                    fetchTotalMessages = true,
                )
            if (response.hasProfile()) {
                response.profile
                    .toEntity()
                    .also {
                        profileDao.upsertCurrentUserProfile(it)
                        // Keeps the member's own community data (level and points) current without a
                        // separate public-profile fetch. The DAO only updates a cached, gamified public
                        // profile: private profiles carry points even with gamification disabled.
                        val update = it.toPublicProfileGamificationUpdate()
                        profileDao.updatePublicProfileGamification(
                            userId = update.userId,
                            gamificationLevelIndex = update.gamificationLevelIndex,
                            gamificationScore = update.gamificationScore,
                            gamificationTicket = gamificationTicket,
                        )
                    }.toDomain()
            } else {
                profileDao.deleteCurrentUserProfile()
                null
            }
        }.let { result ->
            when (result) {
                is ContentUnavailable ->
                    OctopusResult.Success(null).also {
                        profileDao.deleteCurrentUserProfile()
                    }

                is PermissionDenied ->
                    result.also {
                        fetchCommunityAccess()
                    }

                else -> result
            }
        }

    override suspend fun fetchNotificationsBadge(): OctopusResult<Int, Nothing> =
        call(profileMapper) {
            userService
                .getPrivateProfile(
                    userId = getUser()?.id ?: return@call 0,
                    fetchNotificationsBadge = true,
                ).profileOrNull
                ?.toNotSeenNotificationsCountUpdateEntity()
                ?.also {
                    profileDao.updateCurrentUserProfileNotifications(it)
                }?.toDomain() ?: 0
        }

    override suspend fun createProfile(profile: CurrentUserProfile): OctopusResult<ProfileUpdateResult, ProfileUpdateError> =
        catchingMediaEncoding(MediaEncodingException::toProfilePictureError) {
            callWithValidation(profileMapper) {
                val savedProfile = getProfile()

                val response =
                    userService.createProfile(
                        userId = profile.userId,
                        profileUpdate =
                            profile.toProto(
                                previousProfile = savedProfile,
                                failOnPictureEncodingError = true,
                            ),
                    )
                when {
                    response.hasSuccess() -> {
                        response.success.let { result ->
                            ProfileUpdateResult(
                                previousProfile = savedProfile,
                                newProfile =
                                    result.profile
                                        .toEntity()
                                        .also { profileEntity ->
                                            profileDao.upsertCurrentUserProfile(profileEntity)
                                        }.toDomain(),
                                gamificationAction =
                                    if (result.shouldDisplayProfileCompletedGamificationToast) {
                                        Gamification.Action.PROFILE_COMPLETED
                                    } else {
                                        null
                                    },
                            )
                        }
                    }

                    else -> return InvalidArguments(
                        errors = response.fail.errorsList.map { it.toDomain() },
                    )
                }
            }
        }

    override suspend fun updateProfile(
        newProfile: CurrentUserProfile,
        previousProfile: CurrentUserProfile?,
        findAvailableNickname: Boolean,
    ): OctopusResult<ProfileUpdateResult, ProfileUpdateError> =
        updateProfile(newProfile, previousProfile, findAvailableNickname, wantNicknameSuggestions = false)

    override suspend fun updateProfile(
        newProfile: CurrentUserProfile,
        previousProfile: CurrentUserProfile?,
        findAvailableNickname: Boolean,
        wantNicknameSuggestions: Boolean,
    ): OctopusResult<ProfileUpdateResult, ProfileUpdateError> =
        catchingMediaEncoding(MediaEncodingException::toProfilePictureError) {
            callWithValidation(profileMapper) {
                val response =
                    userService.updateProfile(
                        userId = newProfile.userId,
                        profileUpdate =
                            newProfile.toProto(
                                previousProfile = previousProfile,
                                findAvailableNickname = findAvailableNickname,
                                failOnPictureEncodingError = true,
                            ),
                        wantNicknameSuggestions = wantNicknameSuggestions,
                    )
                when {
                    response.hasSuccess() ->
                        response.success.let { result ->
                            ProfileUpdateResult(
                                previousProfile = previousProfile,
                                newProfile =
                                    result.profile
                                        .toEntity()
                                        .also { profileEntity ->
                                            profileDao.upsertCurrentUserProfile(profileEntity)
                                        }.toDomain(),
                                gamificationAction =
                                    if (result.shouldDisplayProfileCompletedGamificationToast) {
                                        Gamification.Action.PROFILE_COMPLETED
                                    } else {
                                        null
                                    },
                            )
                        }

                    else -> return InvalidArguments(
                        errors = response.fail.errorsList.map { it.toDomain() },
                    )
                }
            }
        }

    override suspend fun blockProfile(userId: String): OctopusResult<Unit, Nothing> =
        call(profileMapper) {
            val profile = getProfile() ?: return UserNotAuthenticated()
            // Defense in depth: UI hides Block on admin-tagged profiles, reject here too.
            val target = getProfileFlow(userId).firstOrNull()
            if (target != null && Profile.Tag.ADMIN in target.tags) {
                return PermissionDenied(reason = "Cannot block an admin-tagged profile")
            }
            userService.blockUser(userId = userId)
            (profile.blockedProfiles + BlockedProfile(userId)).also { blockedProfiles ->
                profileDao.upsertCurrentUserProfile(
                    profile
                        .copy(
                            blockedProfiles = blockedProfiles,
                        ).toEntity(),
                )
            }
        }

    override suspend fun deleteProfile() {
        profileDao.deleteCurrentUserProfile()
    }

    override fun getProfileFlow(userId: String) =
        with(profileMapper) {
            profileDao.getPublicProfile(userId).map { it?.toDomain() }
        }

    override suspend fun fetchProfile(userId: String) = fetchPublicProfile(userId).mapSuccess { with(profileMapper) { it?.toDomain() } }

    /** Observes cached community data, including totals that are not part of OtherUserProfile. */
    fun getCommunityDataFlow(userId: String): Flow<OctopusCommunityData?> =
        profileDao.getPublicProfile(userId).map { it?.toOctopusCommunityData() }

    /** Refreshes the public profile and maps its total directly to host community data. */
    suspend fun fetchCommunityData(userId: String): OctopusResult<OctopusCommunityData?, Nothing> =
        fetchPublicProfile(userId).mapSuccess { it?.toOctopusCommunityData() }

    /** Resolves a host user id and maps the refreshed public profile directly to community data. */
    suspend fun fetchCommunityDataByClientUserId(clientUserId: String): OctopusResult<OctopusCommunityData?, Nothing> =
        fetchPublicProfileByClientUserId(clientUserId).mapSuccess { it?.toOctopusCommunityData() }

    private suspend fun fetchPublicProfile(userId: String) =
        call(profileMapper) {
            val gamificationTicket = ProfileGamificationOrder.nextTicket()
            userService
                .getPublicProfile(userId)
                .profileOrNull
                ?.toEntity()
                .also { profile ->
                    if (profile != null) {
                        profileDao.upsertPublicProfile(profile, gamificationTicket)
                    } else {
                        profileDao.deletePublicProfile(userId)
                    }
                }
        }

    // Resolve by the host's own client user id via the GetPublicProfile oneof (gRPC contract).
    override suspend fun fetchProfileByClientUserId(clientUserId: String) =
        fetchPublicProfileByClientUserId(clientUserId).mapSuccess { with(profileMapper) { it?.toDomain() } }

    private suspend fun fetchPublicProfileByClientUserId(clientUserId: String) =
        call(profileMapper) {
            val gamificationTicket = ProfileGamificationOrder.nextTicket()
            userService
                .getPublicProfileByClientUserId(clientUserId)
                .profileOrNull
                ?.toEntity()
                .also { profile ->
                    if (profile != null) {
                        profileDao.upsertPublicProfile(profile, gamificationTicket)
                    }
                }
        }
}
