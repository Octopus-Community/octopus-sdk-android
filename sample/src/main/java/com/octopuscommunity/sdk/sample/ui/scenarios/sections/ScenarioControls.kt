package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton
import com.octopuscommunity.sdk.sample.ui.theme.SampleTheme

// Small shared controls for the scenario sections, so the server-config and Unified-Profile
// override sub-sections render with one consistent look instead of each re-declaring the same widgets.

/** Muted helper paragraph shown at the top of a (sub-)section. */
@Composable
internal fun SectionIntro(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * A full-width choice button that renders filled when [selected], outlined otherwise.
 *
 * [enabled] defaults to `true` and only affects clickability/dimming — the node stays present in
 * the composition either way. That distinction matters for a `qa-preset-*` id: a QA pipeline
 * resolves these ids at runtime, so a button that should be temporarily unavailable (e.g. "Clear
 * override" with nothing to clear) must stay disabled rather than removed, or a teardown /
 * up-front resolve of its id finds nothing from a clean state.
 */
@Composable
internal fun ModeButton(
    label: String,
    selected: Boolean,
    testId: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val modifier = Modifier.fillMaxWidth().testTag(testId)
    if (selected) {
        Button(onClick = onClick, enabled = enabled, modifier = modifier) { Text(label) }
    } else {
        SecondaryPillButton(onClick = onClick, enabled = enabled, modifier = modifier) { Text(label) }
    }
}

/**
 * Shared "an override is currently applied" indicator for every debug-override scenario
 * (`profileFieldsLock`, `contentOptions`, `termsAcceptance`, `locale`, `communityAccess`) — one
 * consistent pill instead of a per-screen design, per the Scenarios fast-follow override-UX pass.
 * Rendered only while an override is active; callers gate visibility with `if (hasOverride)`.
 */
@Composable
internal fun OverrideActiveBadge(
    testId: String,
    label: String = "Override active",
) {
    Surface(
        modifier = Modifier.testTag(testId),
        color = SampleTheme.colors.accentTint,
        border = BorderStroke(1.dp, SampleTheme.colors.accentText.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "● $label",
                style = MaterialTheme.typography.labelMedium,
                color = SampleTheme.colors.accentText,
            )
        }
    }
}
