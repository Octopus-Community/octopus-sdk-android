package com.octopuscommunity.sdk.domain.model

/** The authenticated user [id] and its [jwt]. */
data class User(
    val id: String,
    val jwt: String,
)

/**
 * A user owned by the client (i.e. you).
 *
 * Pass your user through `OctopusSDK.connectUser` so
 * OctopusSDK can "transform" it into an Octopus Community user.
 * This must be done using `OctopusSDK.connectUser()` function
 *
 * @param userId A unique identifier for this user
 * @param profile The User's profile
 */
data class ClientUser(
    val userId: String,
    val profile: Profile? = null,
) {
    /**
     * The User's profile
     *
     * @param nickname nickname/username of your user
     * @param picture Url or local Uri of your user's avatar image
     * @param bio Description of your user
     */
    data class Profile(
        val nickname: String? = null,
        val bio: String? = null,
        val picture: Resource? = null,
    )
}
