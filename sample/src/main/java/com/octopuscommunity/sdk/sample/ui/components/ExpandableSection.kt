package com.octopuscommunity.sdk.sample.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.sample.ui.theme.SampleMotion

/**
 * Collapsible accordion section — divider, tappable title row with an expand/collapse chevron,
 * animated content. Shared by [com.octopuscommunity.sdk.sample.ui.config.ConfigScreen] and
 * [com.octopuscommunity.sdk.sample.ui.scenarios.ScenariosListScreen], the two surviving call sites
 * of the pre-internal change `ExplorerScreen` accordion this pattern was extracted from.
 *
 * [initiallyExpanded] defaults to `false` (collapsed) — same default the legacy accordion used for
 * every section except its own top "Connection" one. [nested] switches to the sub-section styling
 * the legacy accordion used for a section nested inside another (indented, smaller/unaccented
 * title) — e.g. "Unified Profile (exposeClientUserId)" inside "Community config". [modifier] is
 * applied to the whole section (header and body), which is what a bring-into-view anchor needs.
 * [summary], when set, is a one-line recap of the section's current choice under its title, so a
 * collapsed section still says what it holds without being opened.
 */
@Composable
fun ExpandableSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    nested: Boolean = false,
    summary: String? = null,
    content: @Composable () -> Unit,
) {
    // Keyed on [initiallyExpanded] so a section that becomes the anchor of a reused entry opens;
    // an unchanged key (rotation, back from a pushed screen) restores the tester's own choice.
    var expanded by rememberSaveable(initiallyExpanded) { mutableStateOf(initiallyExpanded) }

    Column(modifier = if (nested) modifier.padding(start = 12.dp) else modifier) {
        HorizontalDivider()
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    // Nested sections drop the accent color so they read as a sub-level of the
                    // section they're inside, without a second, smaller text style to maintain.
                    color = if (nested) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                )
                if (summary != null) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        AnimatedVisibility(visible = expanded, enter = SampleMotion.reveal, exit = SampleMotion.conceal) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        }
    }
}
