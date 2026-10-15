package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Switch-Community: re-target the SDK at a different community by pasting its API key and
 * re-initializing. Deliberately just a key field + a single apply button (no preset picker): a
 * community switch re-inits the SDK, and the embedded Community tab does not reliably reload the new
 * community's feed after that, so the simplest surface is the safe one.
 */
@Composable
fun SwitchCommunitySection(
    switchApiKeyInput: String,
    switchingApiKey: String?,
    onSwitchApiKeyChanged: (String) -> Unit,
    onSwitchCommunity: () -> Unit,
) {
    val isSwitching = switchingApiKey != null

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Paste a community API key to re-target the SDK and re-initialize.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = switchApiKeyInput,
            onValueChange = onSwitchApiKeyChanged,
            label = { Text("API Key for new community") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Button(
            onClick = onSwitchCommunity,
            enabled = !isSwitching && switchApiKeyInput.isNotBlank(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp),
        ) {
            Text(
                text = if (isSwitching) "Switching…" else "Switch community",
                fontWeight = FontWeight.Bold,
            )
        }
        // No result text here: `switchCommunity` is one of the `lifecycle` method-state slots, and
        // the screen's single Result block prints it. Printing it twice was the duplicate this
        // section used to carry.
    }
}
