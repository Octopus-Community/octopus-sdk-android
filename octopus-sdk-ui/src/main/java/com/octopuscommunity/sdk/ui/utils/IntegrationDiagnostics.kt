package com.octopuscommunity.sdk.ui.utils

import android.util.Log

/** Integration diagnostics are emitted once per process, without any community or user data. */
internal object IntegrationDiagnostics {
    private val reported = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun reportOnce(
        key: String,
        message: String,
    ) {
        if (reported.add(
                key,
            )
        ) {
            Log.e("OctopusSDK", "$message See https://doc.octopuscommunity.com/SDK/quickstart/?platform=android (Android integration).")
        }
    }

    fun missingCallback(parameter: String) =
        reportOnce(
            parameter,
            "Missing $parameter: the requested host navigation cannot proceed. Provide this callback when integrating Octopus UI.",
        )
}
