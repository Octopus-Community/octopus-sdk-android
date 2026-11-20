package com.octopuscommunity.sdk.domain.model

import android.net.Uri
import androidx.core.net.toUri

/** A file, either [Local] on the device or [Remote]. */
sealed class Resource(
    val data: Any,
) {
    /** A local file at [uri]. */
    data class Local(
        val uri: Uri,
    ) : Resource(uri) {
        constructor(uri: String) : this(uri.toUri())
    }

    /** A remote file at [url]. */
    data class Remote(
        val url: String,
    ) : Resource(url)
}
