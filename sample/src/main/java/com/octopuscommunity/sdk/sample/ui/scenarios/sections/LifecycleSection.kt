package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.sample.ui.components.ApiMethodCard
import com.octopuscommunity.sdk.sample.ui.components.LiveFlowDisplay
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenariosViewModel

/**
 * Lifecycle section: reset (with confirmation) and stop.
 *
 * Switch-Community lives under the "Community config" group ([SwitchCommunitySection]) because it
 * changes which community config the SDK loads, not the SDK lifecycle.
 */
@Composable
fun LifecycleSection(
    isInitialized: Boolean,
    getMethodState: (String) -> ScenariosViewModel.MethodState,
    onReset: () -> Unit,
    onStop: () -> Unit,
    // Catalog QA hooks (shared scenario catalog `lifecycle` scenario) — null outside the
    // Scenarios screen. Presets 2/3 only: preset 1 ("switchCommunity (alt key)") needs a second
    // real community API key, which this sample never bakes in — see ScenarioDetailScreen.
    resetTestTag: String? = null,
    stopTestTag: String? = null,
) {
    var showResetConfirmation by remember { mutableStateOf(false) }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Confirm Reset") },
            text = {
                Text("This will disconnect the user and clear all locally cached data. Are you sure?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmation = false
                        onReset()
                    },
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Live state
        LiveFlowDisplay(
            label = "isInitialised",
            value = isInitialized.toString(),
        )

        // API methods
        ApiMethodCard(
            name = "reset()",
            description = "Disconnect user and clear all locally cached data (with confirmation)",
            isLoading = getMethodState("reset").isLoading,
            onExecute = { showResetConfirmation = true },
            cardTestTag = resetTestTag,
        )

        ApiMethodCard(
            name = "stop()",
            description = "Stop the SDK",
            isLoading = getMethodState("stop").isLoading,
            onExecute = onStop,
            cardTestTag = stopTestTag,
        )
    }
}
