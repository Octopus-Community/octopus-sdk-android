package com.octopuscommunity.sdk.sample.data.model

import android.net.Uri
import kotlinx.serialization.Serializable

/**
 * A user owned by the app.
 * If you don't handle one field, it will be passed as null to the SDK
 */
@Serializable
data class User(
    /** A unique, stable, identifier for this user */
    val id: String? = null,
    /** A nickname */
    val nickname: String? = null,
    /** The bio for this user */
    val bio: String? = null,
    /** A profile picture */
    val avatar: Avatar? = null,
    /**
     * Entitlements granted to this user, embedded as the `entitlements` claim of the
     * client-signed JWT issued by [com.octopuscommunity.sdk.sample.data.utils.TokenProvider].
     * Toggle them from the Edit User screen, then call
     * `OctopusSDK.refreshEntitlements()` to push the new claim set to the backend without
     * a full re-login.
     *
     * Example entitlement values: `customer:premium`, `customer:moderator`.
     */
    val entitlements: List<String> = emptyList(),
) {
    @Serializable
    data class Avatar(
        val source: Source,
        val data: String,
    ) {
        enum class Source { LOCAL, REMOTE }

        constructor(source: Source, data: Uri) : this(source, data.toString())
    }
}
