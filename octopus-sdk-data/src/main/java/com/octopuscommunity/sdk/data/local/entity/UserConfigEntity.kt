package com.octopuscommunity.sdk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_configs")
data class UserConfigEntity(
    @PrimaryKey val id: Int = 0,
    val canAccessCommunity: Boolean,
    val accessDeniedMessage: String?,
    /**
     * Why the access is denied, as one of the `REASON_*` texts of `UserConfigMapper`; `null` while it is granted
     * and on a row written before the reason existed. Free text rather than an enum, so that a value written by a
     * newer SDK reads as unknown instead of failing the read.
     */
    val accessDeniedReason: String? = null,
    val banNickname: String? = null,
    val banPolicyName: String? = null,
    val banPolicyDescription: String? = null,
    val banContactEmail: String? = null,
)
