package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.sample.ui.components.ApiMethodCard
import com.octopuscommunity.sdk.sample.ui.components.LiveFlowDisplay
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenariosViewModel

/**
 * Notifications section: live count, manual refresh, register token display.
 */
@Composable
fun NotificationsSection(
    notSeenCount: Int,
    getMethodState: (String) -> ScenariosViewModel.MethodState,
    onUpdateNotSeenCount: () -> Unit,
    onRegisterToken: () -> Unit,
    // `registerToken()` belongs to the catalog's separate `pushNotifications` scenario (now built —
    // see PushNotificationsPresets in ScenarioPresetSections.kt), not to `notSeenNotifications`. The
    // Scenarios screen's notSeenNotifications detail hides it so the two scenarios don't visually
    // bleed into each other; the default keeps showing it for any other future call site of this
    // composable.
    showRegisterToken: Boolean = true,
    // Catalog QA hooks (shared scenario catalog `notSeenNotifications` scenario) — null
    // outside the Scenarios screen.
    updateNotSeenTestTag: String? = null,
    // Preset 1 ("Open Octopus, full-page route") — a navigation action, not a method call, so it
    // is a plain callback rather than something routed through getMethodState. Null outside the
    // Scenarios screen (no button rendered), matching updateNotSeenTestTag above.
    onOpenHome: (() -> Unit)? = null,
    openHomeTestTag: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Live notification count
        LiveFlowDisplay(
            label = "notSeenNotificationsCount",
            value = notSeenCount.toString(),
        )

        if (onOpenHome != null) {
            SecondaryPillButton(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .let { if (openHomeTestTag != null) it.testTag(openHomeTestTag) else it },
                onClick = onOpenHome,
            ) { Text("Open Octopus (full-page route)") }
        }

        // API methods
        ApiMethodCard(
            name = "updateNotSeenNotificationsCount()",
            description = "Fetch latest unseen notification count from server",
            isLoading = getMethodState("updateNotSeen").isLoading,
            onExecute = onUpdateNotSeenCount,
            cardTestTag = updateNotSeenTestTag,
        )

        if (showRegisterToken) {
            ApiMethodCard(
                name = "registerNotificationsToken()",
                description = "Display current FCM token registration info",
                isLoading = getMethodState("registerToken").isLoading,
                onExecute = onRegisterToken,
            )
        }
    }
}
