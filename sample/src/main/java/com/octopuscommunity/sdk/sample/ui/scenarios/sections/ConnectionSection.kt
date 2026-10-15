package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.sample.ui.components.ApiMethodCard
import com.octopuscommunity.sdk.sample.ui.components.LiveFlowDisplay
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenariosViewModel

/**
 * Connection section: connectUser, disconnectUser, connectionState, isUserConnected.
 */
@Composable
fun ConnectionSection(
    connectionState: ConnectionState,
    isUserConnected: Boolean,
    getMethodState: (String) -> ScenariosViewModel.MethodState,
    onConnectUser: () -> Unit,
    onDisconnectUser: () -> Unit,
    // Catalog QA hooks (shared scenario catalog `connection` scenario) — null outside the
    // Scenarios screen. Only presets 1 ("connect, no entitlements") and 5 ("disconnect") map to
    // this sample: connectUser() has no entitlement-injection parameter today, so presets 2-4
    // (Premium / Moderator / both) are not built — see ScenarioDetailScreen.
    connectTestTag: String? = null,
    disconnectTestTag: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Live flow displays
        LiveFlowDisplay(
            label = "connectionState",
            value =
                when (connectionState) {
                    is ConnectionState.Connected -> {
                        if (connectionState.isGuest) {
                            "Connected (Guest)"
                        } else {
                            "Connected (${connectionState.profile.nickname ?: "User"})"
                        }
                    }
                    ConnectionState.NotConnected -> "NotConnected"
                },
        )

        LiveFlowDisplay(
            label = "isUserConnected",
            value = isUserConnected.toString(),
        )

        // API method cards
        ApiMethodCard(
            name = "connectUser()",
            description = "Connect the stored SSO user to Octopus",
            isLoading = getMethodState("connectUser").isLoading,
            onExecute = onConnectUser,
            cardTestTag = connectTestTag,
        )

        ApiMethodCard(
            name = "disconnectUser()",
            description = "Disconnect the current user",
            isLoading = getMethodState("disconnectUser").isLoading,
            onExecute = onDisconnectUser,
            cardTestTag = disconnectTestTag,
        )
    }
}
