package com.octopuscommunity.sdk.sample.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.sample.data.model.DebugEntry
import com.octopuscommunity.sdk.sample.ui.components.SampleStateView
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton
import com.octopuscommunity.sdk.sample.ui.components.sampleBorderColor
import com.octopuscommunity.sdk.sample.ui.theme.SampleRadius
import com.octopuscommunity.sdk.sample.ui.theme.SampleTextStyles
import com.octopuscommunity.sdk.sample.ui.theme.SampleTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The Events log — Settings → Developer tools → Events log.
 *
 * Two things an integrator needs from a log and never got from the old one: *when* it happened, and
 * *who* caused it. Every row therefore carries an origin pill — **SDK** for something the SDK
 * emitted or decided, **HOST** for a call the sample made into it — plus the wall-clock time and the
 * raw detail in monospace. The origin split is the whole point of the screen: a missing event is a
 * different bug depending on which side of the boundary it was supposed to cross.
 *
 * It renders the merged debug feed rather than the typed `OctopusEvent` list: the typed events are
 * already in that feed (recorded with their timestamp when collected), and the feed is the only
 * source that also holds the host-side calls.
 *
 * No `Scaffold` and no back arrow — this is a destination of the Settings tab graph, and the shared
 * app bar belongs to `MainScaffold`.
 */
@Composable
fun EventsScreen(
    log: List<DebugEntry>,
    onClearEvents: () -> Unit,
) {
    val context = LocalContext.current
    // Newest first: on a screen you open *after* doing the thing you want to inspect, the
    // interesting row is the last one recorded.
    val rows = log.asReversed()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag("events-screen"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${rows.size} events",
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            SecondaryPillButton(
                text = "Copy log",
                onClick = { copyToClipboard(context, asPlainText(rows)) },
                enabled = rows.isNotEmpty(),
                minHeight = 36,
                icon = Icons.Default.ContentCopy,
                modifier = Modifier.testTag("events-copy-log-button"),
            )
            SecondaryPillButton(
                text = "Clear",
                onClick = onClearEvents,
                enabled = rows.isNotEmpty(),
                minHeight = 36,
                icon = Icons.Default.DeleteSweep,
                modifier = Modifier.testTag("events-clear-button"),
            )
        }

        if (rows.isEmpty()) {
            // DS screen 09, state C (empty): the StateView — an icon disc, the fact, the way out.
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                SampleStateView(
                    icon = Icons.Default.Timeline,
                    title = "No events yet",
                    message = "Events will appear here as you interact with the SDK",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows, key = { it.id }) { entry -> EventCard(entry) }
            }
        }
    }
}

@Composable
private fun EventCard(entry: DebugEntry) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, sampleBorderColor, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
                .testTag("event-row"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OriginPill(entry.origin)
            Text(
                text = entry.summary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = TIME_FORMAT.format(Date(entry.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        entry.details?.takeIf { it.isNotBlank() }?.let { details ->
            // DS LogRow payload: `code-bg` / `code-text`, mono 12/16.
            Text(
                text = details,
                style = SampleTextStyles.monoSmall,
                color = SampleTheme.colors.codeText,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(SampleRadius.smShape)
                        .background(SampleTheme.colors.codeBg)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

/**
 * DS LogRow tag, 22 high, `radius-sm`: SDK is the info level (`accent-tint` / `accent-text`), HOST
 * the neutral tag (`surface-3`, 1px `outline`, `text`). The word carries the meaning, not the hue.
 */
@Composable
private fun OriginPill(origin: Origin) {
    val c = SampleTheme.colors
    val sdk = origin == Origin.SDK
    Text(
        text = origin.name,
        style = SampleTextStyles.monoSmall,
        fontWeight = FontWeight.SemiBold,
        color = if (sdk) c.accentText else c.text,
        modifier =
            Modifier
                .heightIn(min = 22.dp)
                .clip(SampleRadius.smShape)
                .background(if (sdk) c.accentTint else c.surface3)
                .border(1.dp, if (sdk) c.accentText.copy(alpha = 0.35f) else c.outline, SampleRadius.smShape)
                .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

/** Which side of the SDK boundary produced the entry. */
private enum class Origin {
    SDK,
    HOST,
}

/**
 * A typed event or a state change is the SDK telling the host something; an API call is the host
 * telling the SDK something. That is the only distinction the pill claims to make.
 */
private val DebugEntry.origin: Origin
    get() =
        when (category) {
            DebugEntry.Category.EVENT, DebugEntry.Category.STATE -> Origin.SDK
            DebugEntry.Category.API_CALL -> Origin.HOST
        }

private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.US)

private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

/**
 * Plain text rather than JSON: what this export is for is a ticket, a Slack message or a `grep` —
 * places where a reader wants the same columns they just read on screen, in the same order. One
 * line per entry, details indented under it, so a copied log survives being pasted anywhere.
 */
private fun asPlainText(entries: List<DebugEntry>): String =
    entries.joinToString(separator = "\n") { entry ->
        buildString {
            append(ISO_FORMAT.format(Date(entry.timestamp)))
            append("  ")
            append(entry.origin.name.padEnd(4))
            append("  ")
            append(entry.summary)
            entry.details?.takeIf { it.isNotBlank() }?.let { details ->
                details.lineSequence().forEach { line ->
                    append("\n        ")
                    append(line)
                }
            }
        }
    }

private fun copyToClipboard(
    context: Context,
    text: String,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Octopus events", text))
}
