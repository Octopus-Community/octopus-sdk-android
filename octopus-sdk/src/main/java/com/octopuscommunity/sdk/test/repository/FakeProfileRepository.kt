package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.OtherUserProfile
import com.octopuscommunity.sdk.domain.model.Profile
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ProfileRepository
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateError
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateResult
import com.octopuscommunity.sdk.test.mock.MockCurrentUserProfiles
import com.octopuscommunity.sdk.test.mock.MockProfiles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [ProfileRepository]; the current user starts as [MockCurrentUserProfiles.default]. */
open class FakeProfileRepository :
    FakeUserRepository(),
    ProfileRepository {
    protected val _currentUserProfile =
        MutableStateFlow<CurrentUserProfile?>(
            MockCurrentUserProfiles.default,
        )

    private val _otherUserProfile = MutableStateFlow<OtherUserProfile?>(null)

    /** Replaces the current user's profile with [profile]. */
    fun setCurrentUserProfile(profile: CurrentUserProfile?) {
        _currentUserProfile.value = profile
    }

    override suspend fun getProfile() = _currentUserProfile.value

    override fun getProfileFlow(): Flow<CurrentUserProfile?> = _currentUserProfile.asStateFlow()

    override suspend fun fetchProfile(): OctopusResult<CurrentUserProfile?, Nothing> = OctopusResult.Success(_currentUserProfile.value)

    override suspend fun fetchNotificationsBadge(): OctopusResult<Int, Nothing> = OctopusResult.Success(0)

    override suspend fun createProfile(profile: CurrentUserProfile): OctopusResult<ProfileUpdateResult, ProfileUpdateError> =
        updateProfile(
            newProfile = profile,
            previousProfile = getProfile(),
        )

    override suspend fun updateProfile(
        newProfile: CurrentUserProfile,
        previousProfile: CurrentUserProfile?,
        findAvailableNickname: Boolean,
    ): OctopusResult<ProfileUpdateResult, ProfileUpdateError> {
        if (MockProfiles.all.any { it.nickname == newProfile.nickname && it.userId != newProfile.userId }) {
            return OctopusResult.Failure.InvalidArguments(
                listOf(ProfileUpdateError.NicknameError.AlreadyUsed("Username already taken")),
            )
        }

        _currentUserProfile.value = newProfile
        return OctopusResult.Success(
            ProfileUpdateResult(
                previousProfile = previousProfile,
                newProfile = newProfile,
                gamificationAction = null,
            ),
        )
    }

    /**
     * Same as the update without suggestions, except that a taken nickname comes back with two
     * suggestions when [wantNicknameSuggestions] is set.
     */
    override suspend fun updateProfile(
        newProfile: CurrentUserProfile,
        previousProfile: CurrentUserProfile?,
        findAvailableNickname: Boolean,
        wantNicknameSuggestions: Boolean,
    ): OctopusResult<ProfileUpdateResult, ProfileUpdateError> {
        val taken = MockProfiles.all.any { it.nickname == newProfile.nickname && it.userId != newProfile.userId }
        if (taken && wantNicknameSuggestions) {
            return OctopusResult.Failure.InvalidArguments(
                listOf(
                    ProfileUpdateError.NicknameError.AlreadyUsed(
                        errorMessage = "Username already taken",
                        suggestions = listOf("${newProfile.nickname}_7", "${newProfile.nickname}.b"),
                    ),
                ),
            )
        }
        return updateProfile(newProfile, previousProfile, findAvailableNickname)
    }

    override suspend fun deleteProfile() {
        _currentUserProfile.update { null }
    }

    override suspend fun blockProfile(userId: String): OctopusResult<Unit, Nothing> {
        _currentUserProfile.update { profile ->
            profile?.let {
                it.copy(
                    blockedProfiles = it.blockedProfiles + CurrentUserProfile.BlockedProfile(userId),
                )
            }
        }
        return OctopusResult.Success(Unit)
    }

    override suspend fun deleteUser() {
        _currentUserProfile.value = null
        user.value = null
    }

    override fun getProfileFlow(userId: String) =
        _otherUserProfile.map {
            MockProfiles.all.find { it.userId == userId }
        }

    override suspend fun fetchProfile(userId: String): OctopusResult<Profile?, Nothing> {
        val profile = MockProfiles.all.find { it.userId == userId }
        _otherUserProfile.value = profile
        return OctopusResult.Success(profile)
    }
}
