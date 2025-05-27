package com.octopuscommunity.sample.sso.hybrid.data.model

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
    /** A profile picture */
    val avatar: Avatar? = null,
    /** Age information about this user */
    val ageInformation: AgeInformation? = null
) {
    @Serializable
    data class Avatar(val source: Source, val data: String) {
        enum class Source { LOCAL, REMOTE }

        constructor(source: Source, data: Uri) : this(source, data.toString())
    }

    @Serializable
    enum class AgeInformation { LEGAL_AGE_REACHED, UNDERAGE }
}