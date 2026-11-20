package com.octopuscommunity.sdk.data.local.data

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val id: String,
    val jwt: String
)