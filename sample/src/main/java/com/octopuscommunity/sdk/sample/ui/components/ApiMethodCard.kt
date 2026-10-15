package com.octopuscommunity.sdk.sample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Card for a single SDK API method: what it is, what it does, and the button that runs it.
 *
 * It deliberately does **not** render the call's result. The scenario detail screen owns a single
 * Result block for the whole screen (zone 4, the raw `OctopusResult` on the dark ground with its
 * Success/Failure status and duration), and this card used to print the same string again in a
 * grey inline box right above it — the same answer twice, in two different visual languages.
 *
 * [cardTestTag] is an optional QA hook (shared scenario catalog,
 * `scenario catalog`) — null by default. The matching `result_test_id` now
 * lives on the detail screen's Result block, not here.
 */
@Composable
fun ApiMethodCard(
    name: String,
    description: String,
    isLoading: Boolean,
    enabled: Boolean = true,
    onExecute: () -> Unit,
    modifier: Modifier = Modifier,
    extraContent: @Composable (() -> Unit)? = null,
    cardTestTag: String? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (extraContent != null) {
            extraContent()
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                SecondaryPillButton(onClick = {}, enabled = false) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                    Text("Running...")
                }
            } else {
                Button(
                    onClick = onExecute,
                    enabled = enabled,
                    modifier = if (cardTestTag != null) Modifier.testTag(cardTestTag) else Modifier,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Text("Execute")
                }
            }
        }
    }
}
