package com.octopuscommunity.sdk.ui.components

import com.octopuscommunity.sdk.MissingOnNavigateToLoginException
import com.octopuscommunity.sdk.MissingOnNavigateToProfileEditException
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.ui.utils.IntegrationDiagnostics

/** Preserve the documented exceptions at action time, never while composing a screen. */
internal object HostNavigationCallbacks {
    fun login(callback: (() -> Unit)?) {
        if (callback != null) {
            callback()
        } else {
            IntegrationDiagnostics.missingCallback("onNavigateToLogin")
            throw MissingOnNavigateToLoginException()
        }
    }

    fun edit(
        callback: ((ProfileField?) -> Unit)?,
        field: ProfileField?,
    ) {
        if (callback != null) {
            callback(field)
        } else {
            IntegrationDiagnostics.missingCallback("onNavigateToProfileEdit")
            throw MissingOnNavigateToProfileEditException()
        }
    }
}
