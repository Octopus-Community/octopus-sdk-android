package com.octopuscommunity.sample.sso.client.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.octopuscommunity.sdk.domain.model.ClientUser
import com.octopuscommunity.sdk.domain.model.Image

private const val ID_KEY = "app_user_id"
private const val NICKNAME_KEY = "app_user_nickname"
private const val BIO_KEY = "app_user_bio"
private const val AVATAR_TYPE_KEY = "app_user_avatar_type"
private const val AVATAR_VALUE_KEY = "app_user_avatar_value"
private const val AGE_INFORMATION_KEY = "app_user_age_information"

/**
 * A user owned by the app.
 * If you don't handle one field, it will be passed as null to the SDK
 */
data class AppUser(
    /** A unique, stable, identifier for this user */
    val userId: String? = null,
    /** A nickname */
    val nickname: String? = null,
    /** The bio for this user */
    val bio: String? = null,
    /** A profile picture */
    val avatar: Image? = null,
    /** Age information about this user */
    val ageInformation: ClientUser.Profile.AgeInformation? = null
)

private fun Context.getUserPreferences() =
    getSharedPreferences("app_user_store", Context.MODE_PRIVATE)

/**
 * User store
 * Only used to keep the logged in app user across app restart.
 * You probably do not need this class as you already have your own way to store your users.
 */
fun Context.getStoredUser(): AppUser? {
    val preferences = getUserPreferences()
    val id = preferences.getString(ID_KEY, null) ?: return null

    return AppUser(
        userId = id,
        nickname = preferences.getString(NICKNAME_KEY, null),
        bio = preferences.getString(BIO_KEY, null),
        avatar = when (preferences.getString(AVATAR_TYPE_KEY, null)) {
            "local" -> Image.Local(Uri.parse(preferences.getString(AVATAR_VALUE_KEY, "")))
            "remote" -> Image.Remote(preferences.getString(AVATAR_VALUE_KEY, "") ?: "")
            else -> null
        },
        ageInformation = when (preferences.getString(AGE_INFORMATION_KEY, null)) {
            "legal_age_reached" -> ClientUser.Profile.AgeInformation.LegalAgeReached
            "underage" -> ClientUser.Profile.AgeInformation.Underage
            else -> null
        }
    )
}

fun Context.setStoredUser(user: AppUser?) {
    getUserPreferences().edit {
        if (user != null) {
            putString(ID_KEY, user.userId)
            putString(NICKNAME_KEY, user.nickname)
            putString(BIO_KEY, user.bio)
            putString(
                AVATAR_TYPE_KEY, when (user.avatar) {
                    is Image.Local -> "local"
                    is Image.Remote -> "remote"
                    else -> null
                }
            )
            putString(AVATAR_VALUE_KEY, user.avatar?.data.toString())
            putString(
                AGE_INFORMATION_KEY, when (user.ageInformation) {
                    ClientUser.Profile.AgeInformation.LegalAgeReached -> "legal_age_reached"
                    ClientUser.Profile.AgeInformation.Underage -> "underage"
                    else -> null
                }
            )

        } else {
            clear()
        }
    }
}

fun Context.clearStoredUser() = setStoredUser(null)

fun AppUser.toOctopusUser() = ClientUser(
    id = userId ?: "",
    profile = ClientUser.Profile(
        nickname = nickname,
        bio = bio,
        avatar = avatar,
        ageInformation = ageInformation
    )
)