package com.octopuscommunity.sdk.sample.ui.console

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.sample.data.model.DebugEntry
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.extensions.LocalExtensions
import com.octopuscommunity.sdk.sample.ui.components.SampleStateView
import com.octopuscommunity.sdk.sample.ui.components.sampleFilterChipBorder
import com.octopuscommunity.sdk.sample.ui.components.sampleFilterChipColors
import com.octopuscommunity.sdk.sample.ui.theme.SampleModalBottomSheet
import com.octopuscommunity.sdk.sample.ui.theme.SampleRadius
import com.octopuscommunity.sdk.sample.ui.theme.SampleTextStyles
import com.octopuscommunity.sdk.sample.ui.theme.SampleTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug console shown as a modal bottom sheet.
 * Displays a chronological log of SDK events, state changes, and API call results.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DebugConsoleSheet(
    entries: List<DebugEntry>,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    serverEnvironment: SDKConfig.ServerEnvironment,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var activeFilters by rememberSaveable { mutableStateOf(DebugEntry.Category.entries.toSet()) }

    val filteredEntries =
        remember(entries, searchQuery, activeFilters) {
            entries
                .filter { entry ->
                    entry.category in activeFilters &&
                        (
                            searchQuery.isBlank() ||
                                entry.summary.contains(searchQuery, ignoreCase = true) ||
                                entry.details?.contains(searchQuery, ignoreCase = true) == true
                        )
                }.reversed() // Newest first
        }

    val context = LocalContext.current

    SampleModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            // A ModalBottomSheet renders into its own Window / SemanticsOwner, outside the
            // Activity-root flag hoisted in MainActivity.kt — so this composition root needs
            // its own testTagsAsResourceId, ancestor of every tagged node below (debug-* ids).
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .semantics { testTagsAsResourceId = true },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LocalExtensions.WarningBanner(serverEnvironment = serverEnvironment)

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Debug Console",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row {
                    IconButton(
                        onClick = {
                            val allText =
                                entries.joinToString("\n") { entry ->
                                    val time = formatTimestamp(entry.timestamp)
                                    "[$time] [${entry.category.label}] ${entry.summary}" +
                                        (entry.details?.let { "\n  $it" } ?: "")
                                }
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("Debug Log", allText),
                            )
                        },
                        modifier = Modifier.testTag("debug-copy-button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy All",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = onClear, modifier = Modifier.testTag("debug-clear-button")) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Log",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("debug-close-button")) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search logs...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
            )

            // Filter chips
            FlowRow(
                modifier = Modifier.testTag("debug-filter-select"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DebugEntry.Category.entries.forEach { category ->
                    FilterChip(
                        leadingIcon =
                            if (category in activeFilters) {
                                { Icon(Icons.Default.Check, contentDescription = "Selected") }
                            } else {
                                null
                            },
                        colors = sampleFilterChipColors(),
                        border = sampleFilterChipBorder(selected = category in activeFilters),
                        selected = category in activeFilters,
                        onClick = {
                            activeFilters =
                                if (category in activeFilters) {
                                    activeFilters - category
                                } else {
                                    activeFilters + category
                                }
                        },
                        label = {
                            Text(
                                text = "${category.label} (${entries.count { it.category == category }})",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }

            // Entry count
            Text(
                text = "${filteredEntries.size} of ${entries.size} entries",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Entries list
            if (filteredEntries.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    // DS screen 09, state C (empty).
                    SampleStateView(
                        icon = Icons.Default.Terminal,
                        title = if (entries.isEmpty()) "No entries yet" else "No matching entries",
                        message =
                            if (entries.isEmpty()) {
                                "SDK events, state changes and API calls land here as they happen."
                            } else {
                                "Nothing matches the current filters."
                            },
                    )
                }
            } else {
                val listState = rememberLazyListState()
                // Auto-scroll to top (newest) when new entries arrive
                LaunchedEffect(filteredEntries.size) {
                    if (filteredEntries.isNotEmpty()) {
                        listState.animateScrollToItem(0)
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("debug-log-list"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filteredEntries, key = { it.id }) { entry ->
                        DebugEntryRow(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugEntryRow(entry: DebugEntry) {
    var expanded by remember { mutableStateOf(false) }
    val hasDetails = entry.details != null

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .then(
                    if (hasDetails) {
                        Modifier.clickable { expanded = !expanded }
                    } else {
                        Modifier
                    },
                ).padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Timestamp
            Text(
                text = formatTimestamp(entry.timestamp),
                style = SampleTextStyles.monoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Category tag
            // DS LogRow tag (`log-cat-*`): each category borrows a tonal pair (info, success,
            // warning) and the label word is what tells them apart. DS v17 gives the tag back a
            // 1px inset edge in its own ink at 45 %: the fill alone melted into the dark row.
            val ds = SampleTheme.colors
            val (tagFill, tagInk) =
                when (entry.category) {
                    DebugEntry.Category.EVENT -> ds.accentTint to ds.accentText
                    DebugEntry.Category.STATE -> ds.successTint to ds.successText
                    DebugEntry.Category.API_CALL -> ds.warningTint to ds.warningText
                }
            Text(
                text = entry.category.label,
                style = MaterialTheme.typography.labelSmall,
                color = tagInk,
                modifier =
                    Modifier
                        .clip(SampleRadius.smShape)
                        .background(tagFill)
                        .border(1.dp, tagInk.copy(alpha = 0.45f), SampleRadius.smShape)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
            )

            // Summary
            Text(
                text = entry.summary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = if (expanded) Int.MAX_VALUE else 1,
            )

            // Expand indicator
            if (hasDetails) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    // Decoration (expand/collapse caret), exempt from the content floor.
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                )
            }
        }

        // Expandable details
        AnimatedVisibility(
            visible = expanded && hasDetails,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Text(
                text = entry.details ?: "",
                style = SampleTextStyles.monoSmall,
                color = SampleTheme.colors.codeText,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SampleTheme.colors.codeBg)
                        .padding(6.dp),
            )
        }
    }
}

private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

private fun formatTimestamp(timestamp: Long): String = timeFormat.format(Date(timestamp))
