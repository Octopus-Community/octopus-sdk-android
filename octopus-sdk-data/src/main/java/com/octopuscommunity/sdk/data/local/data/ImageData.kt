package com.octopuscommunity.sdk.data.local.data

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
data class ImageData(val type: Type, val value: String) {
    @Serializable
    enum class Type { LOCAL, REMOTE }

    constructor(url: String) : this(Type.REMOTE, url)
    constructor(localUri: Uri) : this(Type.LOCAL, localUri.toString())
}