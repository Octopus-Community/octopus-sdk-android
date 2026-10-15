package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.OctopusReactionKind
import com.octopuscommunity.sdk.domain.network.onFailure
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.sample.data.SampleFixtures
import com.octopuscommunity.sdk.sample.data.describe
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton
import kotlinx.coroutines.launch

/**
 * QA tooling for [OctopusSDK.setReaction] (native since 1.12.0): set, change, or clear the
 * current user's reaction on an EXISTING post.
 *
 * The post is identified by an id the tester pastes in — never auto-fetched — because the demo
 * community is shared and this section must never guess at or depend on "the latest post". Every
 * preset below only calls `setReaction` on the pasted id; nothing here ever creates a post (see
 * [BridgeShareSection] / [BridgeSection] to obtain a real post id first).
 *
 * Test ids (`qa-preset-reactions-N` / `reactions-result`) are applied verbatim from the shared
 * shared scenario catalog. `testTagsAsResourceId` surfaces them as resource-ids in the
 * uiautomator / `android layout` tree.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ReactionsSection() {
    var postId by rememberSaveable { mutableStateOf(SampleFixtures.postReactionStack) }
    var resultText by rememberSaveable { mutableStateOf<String?>(null) }
    var isError by rememberSaveable { mutableStateOf(false) }
    // Not rememberSaveable: this section sits under an AnimatedVisibility, and persisting the
    // flag would restore it as `true` after process death (or a config change) even though no
    // coroutine survived to ever set it back to `false`, leaving the spinner stuck forever.
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun react(
        label: String,
        kind: OctopusReactionKind?,
    ) {
        val id = postId.trim()
        if (id.isBlank()) return
        scope.launch {
            isLoading = true
            resultText = null
            isError = false
            try {
                OctopusSDK
                    .setReaction(reaction = kind, postId = id)
                    .onSuccess { resultText = "$label -> OK" }
                    .onFailure { failure ->
                        isError = true
                        resultText = "$label -> Error: ${failure.describe()}"
                    }
            } finally {
                // In `finally` so a cancellation cannot leave the presets locked out for good.
                isLoading = false
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionIntro(
            "setReaction(reaction, postId) — set, change or clear the current user's reaction on " +
                "an EXISTING post. Paste a post id below (e.g. one opened from the Bridge section, " +
                "or copied from the community feed); no post is ever created here.",
        )

        OutlinedTextField(
            value = postId,
            onValueChange = { postId = it },
            label = { Text("Post ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        val enabled = postId.isNotBlank() && !isLoading

        Preset(
            label = "Preset 1 · React heart",
            testId = "qa-preset-reactions-1",
            enabled = enabled,
        ) { react("Preset 1 · React heart", OctopusReactionKind.Heart) }
        Preset(
            label = "Preset 2 · Change reaction to joy",
            testId = "qa-preset-reactions-2",
            enabled = enabled,
        ) { react("Preset 2 · Change reaction to joy", OctopusReactionKind.Joy) }
        Preset(
            label = "Preset 3 · Unreact (null)",
            testId = "qa-preset-reactions-3",
            enabled = enabled,
        ) { react("Preset 3 · Unreact", null) }
        Preset(
            label = "Preset 4 · React mouthOpen",
            testId = "qa-preset-reactions-4",
            enabled = enabled,
        ) { react("Preset 4 · React mouthOpen", OctopusReactionKind.MouthOpen) }
        Preset(
            label = "Preset 5 · React clap",
            testId = "qa-preset-reactions-5",
            enabled = enabled,
        ) { react("Preset 5 · React clap", OctopusReactionKind.Clap) }
        Preset(
            label = "Preset 6 · React cry",
            testId = "qa-preset-reactions-6",
            enabled = enabled,
        ) { react("Preset 6 · React cry", OctopusReactionKind.Cry) }
        Preset(
            label = "Preset 7 · React rage",
            testId = "qa-preset-reactions-7",
            enabled = enabled,
        ) { react("Preset 7 · React rage", OctopusReactionKind.Rage) }

        Text(
            modifier = Modifier.testTag("reactions-result"),
            text =
                when {
                    isLoading -> "…"
                    else -> resultText ?: "none yet — paste a post id and tap a preset."
                },
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Preset(
    label: String,
    testId: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    SecondaryPillButton(
        modifier = Modifier.fillMaxWidth().testTag(testId),
        enabled = enabled,
        onClick = onClick,
    ) {
        Text(label)
    }
}
