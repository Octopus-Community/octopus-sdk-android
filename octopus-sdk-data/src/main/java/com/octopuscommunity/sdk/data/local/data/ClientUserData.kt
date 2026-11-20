package com.octopuscommunity.sdk.data.local.data

import kotlinx.serialization.Serializable

@Serializable
data class ClientUserData(
    val id: String,
    val profile: Profile
) {
    @Serializable
    data class Profile(
        val nickname: String?,
        val bio: String?,
        val avatar: ImageData?
    )
}