package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.UserServiceProto.GetGuestJwtResponse
import com.octopuscommunity.UserServiceProto.GetJwtFromClientSignedTokenResponse
import com.octopuscommunity.UserServiceProto.GetJwtFromClientSignedTokenResponse.ErrorCode.UNKNOWN_ERROR
import com.octopuscommunity.UserServiceProto.GetJwtFromClientSignedTokenResponse.ErrorCode.UNRECOGNIZED
import com.octopuscommunity.UserServiceProto.GetJwtFromClientSignedTokenResponse.ErrorCode.USER_BANNED
import com.octopuscommunity.sdk.data.local.data.ClientUserData
import com.octopuscommunity.sdk.data.local.data.UserData
import com.octopuscommunity.sdk.domain.model.ClientUser
import com.octopuscommunity.sdk.domain.model.User
import com.octopuscommunity.sdk.domain.repository.ClientUserError.Other
import com.octopuscommunity.sdk.domain.repository.ClientUserError.UserBanned
import com.octopuscommunity.sdk.domain.repository.GuestError

open class UserMapper(
    private val mediaMapper: MediaMapper,
) : OctopusMapper {
    fun UserData.toDomain() =
        User(
            id = id,
            jwt = jwt,
        )

    fun User.toData() =
        UserData(
            id = id,
            jwt = jwt,
        )

    fun GetGuestJwtResponse.Success.toData() =
        UserData(
            id = userId,
            jwt = jwt,
        )

    fun GetJwtFromClientSignedTokenResponse.Success.toData() =
        UserData(
            id = userId,
            jwt = jwt,
        )

    fun ClientUserData.toDomain() =
        ClientUser(
            userId = id,
            profile =
                ClientUser.Profile(
                    nickname = profile.nickname,
                    bio = profile.bio,
                    picture = with(mediaMapper) { profile.avatar?.toDomain() },
                ),
        )

    fun ClientUser.toData() =
        ClientUserData(
            id = userId,
            profile =
                ClientUserData.Profile(
                    nickname = profile?.nickname,
                    bio = profile?.bio,
                    avatar = with(mediaMapper) { profile?.picture?.toData() },
                ),
        )

    fun GetGuestJwtResponse.Error.toDomain(): GuestError = guestErrorFrom(errorCode, message)

    fun GetJwtFromClientSignedTokenResponse.Error.toDomain() =
        when (errorCode) {
            USER_BANNED -> UserBanned(message)
            UNRECOGNIZED, UNKNOWN_ERROR, null -> Other(message)
        }
}

/**
 * Maps a `GetGuestJwt` failure onto its domain error.
 *
 * `USER_BANNED` is the reason the backend sends when it refuses a guest session to a banned user.
 * Before the gRPC contract carried it, that ban arrived as `UNKNOWN_ERROR` with the reason stranded
 * in the human-readable `message` — nothing a caller could branch on, and nothing that survives a
 * change of locale. Any other code, including one a newer backend introduces and this SDK version
 * does not know (`UNRECOGNIZED`), stays [GuestError.Unknown].
 *
 * Kept as a top-level function so it can be unit-tested without a [UserMapper], whose [MediaMapper]
 * needs a Coil `ImageLoader` and an Android `Context` — same reason as [pictureUpdateOrNull].
 */
internal fun guestErrorFrom(
    errorCode: GetGuestJwtResponse.ErrorCode,
    message: String,
): GuestError =
    when (errorCode) {
        GetGuestJwtResponse.ErrorCode.USER_BANNED -> GuestError.UserBanned(message)
        GetGuestJwtResponse.ErrorCode.UNRECOGNIZED,
        GetGuestJwtResponse.ErrorCode.UNKNOWN_ERROR,
        -> GuestError.Unknown(message)
    }
