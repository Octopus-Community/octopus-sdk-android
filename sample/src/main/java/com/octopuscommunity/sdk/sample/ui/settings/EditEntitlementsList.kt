package com.octopuscommunity.sdk.sample.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.sample.ui.components.sampleDefaultSwitchColors
import com.octopuscommunity.sdk.sample.ui.components.sampleInputChipBorder
import com.octopuscommunity.sdk.sample.ui.components.sampleInputChipColors
import kotlinx.coroutines.delay

/**
 * Stateless reusable entitlements editor. Mirrors iOS
 * the shared entitlement editor:
 * the parent owns the `Set<String>` selection and is notified of toggles via [onToggle].
 *
 * Exposes the [EditUserViewModel.KnownEntitlement] entries as ready-to-toggle switches. When
 * [allowCustomClaims] is true (the default), an extra free-text input and removable chips let
 * testers grant arbitrary claims (e.g. `customer:vip`, `customer:beta`) without rebuilding the
 * sample. Contexts that should stay focused on quick toggling (such as the
 * `GroupAccessDenied` sheet) can pass `allowCustomClaims = false` to hide both the input and
 * the chips.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditEntitlementsList(
    selection: Set<String>,
    onToggle: (claim: String, granted: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    allowCustomClaims: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EditUserViewModel.KnownEntitlement.entries.forEach { ent ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ent.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    colors = sampleDefaultSwitchColors(),
                    checked = ent.claim in selection,
                    onCheckedChange = { granted -> onToggle(ent.claim, granted) },
                )
            }
        }

        if (allowCustomClaims) {
            CustomClaimsEditor(selection = selection, onToggle = onToggle)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CustomClaimsEditor(
    selection: Set<String>,
    onToggle: (claim: String, granted: Boolean) -> Unit,
) {
    val knownClaims =
        EditUserViewModel.KnownEntitlement.entries
            .map { it.claim }
            .toSet()
    val customClaims = (selection - knownClaims).sorted()
    var customClaimInput by remember { mutableStateOf("") }
    var inlineHint by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    if (inlineHint != null) {
        LaunchedEffect(inlineHint) {
            delay(2_500)
            inlineHint = null
        }
    }

    fun commitCustomClaim() {
        val trimmed = customClaimInput.trim()
        if (trimmed.isEmpty()) {
            customClaimInput = ""
            return
        }
        if (trimmed in selection) {
            inlineHint =
                if (trimmed in knownClaims) {
                    "\"$trimmed\" is already granted via the toggle above."
                } else {
                    "\"$trimmed\" is already in the list."
                }
            customClaimInput = ""
            return
        }
        onToggle(trimmed, true)
        customClaimInput = ""
        inlineHint = null
        keyboardController?.hide()
    }

    if (customClaims.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            customClaims.forEach { claim ->
                InputChip(
                    colors = sampleInputChipColors(),
                    border = sampleInputChipBorder(),
                    selected = true,
                    onClick = {},
                    label = { Text(claim) },
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Custom entitlement $claim, granted"
                        },
                    trailingIcon = {
                        IconButton(
                            onClick = { onToggle(claim, false) },
                            modifier = Modifier.size(InputChipDefaults.IconSize * 1.5f),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove $claim",
                                modifier = Modifier.size(InputChipDefaults.IconSize),
                            )
                        }
                    },
                )
            }
        }
    }

    OutlinedTextField(
        value = customClaimInput,
        onValueChange = {
            customClaimInput = it
            if (inlineHint != null) inlineHint = null
        },
        label = { Text("Custom entitlement") },
        placeholder = { Text("e.g. customer:vip") },
        supportingText =
            if (inlineHint != null) {
                { Text(inlineHint!!, color = MaterialTheme.colorScheme.tertiary) }
            } else {
                null
            },
        modifier =
            Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && customClaimInput.trim().isNotEmpty()) {
                        commitCustomClaim()
                    }
                },
        singleLine = true,
        trailingIcon = {
            if (customClaimInput.isNotBlank()) {
                IconButton(onClick = ::commitCustomClaim) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add custom entitlement",
                    )
                }
            }
        },
        keyboardActions = KeyboardActions(onDone = { commitCustomClaim() }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    )
}
