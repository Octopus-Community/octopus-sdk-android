package com.octopuscommunity.sdk.sample.ui.settings

import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState

/** The visible connection detail and the raw ID available to copy, if any. */
internal data class DebugConnectionInfo(
    val label: String,
    val userId: String?,
)

internal fun debugConnectionInfo(connectionState: ConnectionState): DebugConnectionInfo {
    val connected = connectionState as? ConnectionState.Connected ?: return DebugConnectionInfo("Not connected", null)
    val userId = connected.profile.userId.takeIf { it.isNotBlank() }
    val label =
        if (connected.isGuest) {
            userId?.let { "Guest · read-only · $it" } ?: "Guest · read-only"
        } else {
            listOfNotNull("Connected", connected.profile.nickname.takeIf { it.isNotBlank() }, userId).joinToString(" · ")
        }
    return DebugConnectionInfo(label, userId)
}
