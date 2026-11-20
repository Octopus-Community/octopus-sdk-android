package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.OtherUserProfile
import com.octopuscommunity.sdk.domain.model.Profile
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.ServerError
import kotlinx.coroutines.flow.Flow

/** Repository for the current user's profile and other users' profiles. */
interface ProfileRepository : UserRepository {
    /** Returns the cached profile of the current user, or `null` when there is none. */
    suspend fun getProfile(): CurrentUserProfile?

    /** Observes the cached profile of the current user. */
    fun getProfileFlow(): Flow<CurrentUserProfile?>

    /** Observes the cached profile of the user [userId]. */
    fun getProfileFlow(userId: String): Flow<OtherUserProfile?>

    /** Fetches the public profile of the user [userId]. */
    suspend fun fetchProfile(userId: String): OctopusResult<Profile?, Nothing>

    /**
     * Fetches a member's public profile by the host's own client user id (Unified Profile), resolved
     * through the `GetPublicProfile` client-user-id lookup. Backs
     * [com.octopuscommunity.sdk.OctopusSDK.fetchCommunityDataByClientUserId]. Defaults to empty
     * success (fakes need no override); the data-layer implementation performs the real lookup.
     */
    suspend fun fetchProfileByClientUserId(clientUserId: String): OctopusResult<Profile?, Nothing> = OctopusResult.Success(null)

    /** Fetches the profile of the current user. */
    suspend fun fetchProfile(): OctopusResult<CurrentUserProfile?, Nothing>

    /** Fetches the number of notifications the current user has not seen. */
    suspend fun fetchNotificationsBadge(): OctopusResult<Int, Nothing>

    /** Creates [profile] on the backend. */
    suspend fun createProfile(profile: CurrentUserProfile): OctopusResult<ProfileUpdateResult, ProfileUpdateError>

    /**
     * Updates the profile from [previousProfile] to [newProfile]. With [findAvailableNickname], the
     * backend generates an available nickname when the requested one is missing or taken.
     */
    suspend fun updateProfile(
        newProfile: CurrentUserProfile,
        previousProfile: CurrentUserProfile?,
        findAvailableNickname: Boolean = false,
    ): OctopusResult<ProfileUpdateResult, ProfileUpdateError>

    /**
     * Updates the profile like [updateProfile], and with [wantNicknameSuggestions] asks the backend,
     * when the requested nickname is already taken, for available nicknames derived from it
     * ([ProfileUpdateError.NicknameError.AlreadyUsed.suggestions]). Defaults to the update without
     * suggestions, so implementations that do not support them need no override.
     */
    suspend fun updateProfile(
        newProfile: CurrentUserProfile,
        previousProfile: CurrentUserProfile?,
        findAvailableNickname: Boolean,
        wantNicknameSuggestions: Boolean,
    ): OctopusResult<ProfileUpdateResult, ProfileUpdateError> = updateProfile(newProfile, previousProfile, findAvailableNickname)

    /** Blocks the user [userId]. Admin-tagged profiles cannot be blocked. */
    suspend fun blockProfile(userId: String): OctopusResult<Unit, Nothing>

    /** Deletes the cached profile of the current user. */
    suspend fun deleteProfile()
}

/** Outcome of a profile creation or update. */
data class ProfileUpdateResult(
    /** The cached profile before the creation or update, if any. */
    val previousProfile: CurrentUserProfile?,
    /** Profile after the update. */
    val newProfile: CurrentUserProfile,
    /** Gamification action the update completed, if any. */
    val gamificationAction: Gamification.Action?,
)

/** Errors returned by a profile creation or update, grouped by profile field. */
sealed interface ProfileUpdateError : ServerError {
    /** Errors on the nickname. */
    sealed class NicknameError : ProfileUpdateError {
        /**
         * The nickname is used by another member. [suggestions] are available nicknames derived from
         * it, sent only when the update asked for them (`wantNicknameSuggestions`); empty otherwise.
         */
        data class AlreadyUsed(
            override val errorMessage: String,
            val suggestions: List<String>,
        ) : NicknameError() {
            /** The refusal without suggestions, as before suggestions existed. */
            constructor(errorMessage: String) : this(errorMessage, emptyList())

            /** Keeps the single-field copy linking for already compiled callers. */
            @Deprecated("Use the copy that takes the suggestions.", level = DeprecationLevel.HIDDEN)
            fun copy(errorMessage: String = this.errorMessage): AlreadyUsed = AlreadyUsed(errorMessage, suggestions)
        }

        data class BannedWords(
            override val errorMessage: String,
            val bannedWords: List<String>,
        ) : NicknameError()

        data class BadFormat(
            override val errorMessage: String,
        ) : NicknameError()

        /** The nickname exceeds its character limit. */
        data class CharLimitReached(
            /** Server-provided message describing the nickname length refusal. */
            override val errorMessage: String,
        ) : NicknameError()

        data class Unknown(
            override val errorMessage: String,
        ) : NicknameError()
    }

    /** Errors on the email. */
    sealed class EmailError : ProfileUpdateError {
        data class AlreadyTaken(
            override val errorMessage: String,
        ) : EmailError()

        data class BadFormat(
            override val errorMessage: String,
        ) : EmailError()

        data class Unknown(
            override val errorMessage: String,
        ) : EmailError()
    }

    /** Errors on the picture. */
    sealed class PictureError : ProfileUpdateError {
        data class Unknown(
            override val errorMessage: String,
        ) : PictureError()
    }

    /** Errors on the bio. */
    sealed class BioError : ProfileUpdateError {
        data class BannedWords(
            override val errorMessage: String,
            val bannedWords: List<String>,
        ) : BioError()

        data class BadFormat(
            override val errorMessage: String,
        ) : BioError()

        data class CharLimitReached(
            override val errorMessage: String,
        ) : BioError()

        data class Unknown(
            override val errorMessage: String,
        ) : BioError()
    }

    /** Any other error. */
    data class OtherError(
        override val errorMessage: String,
    ) : ProfileUpdateError
}
