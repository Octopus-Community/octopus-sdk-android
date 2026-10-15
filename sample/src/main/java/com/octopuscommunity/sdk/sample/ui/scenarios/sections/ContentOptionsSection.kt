package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.InternalOctopusApi
import com.octopuscommunity.sdk.domain.model.CommunityConfig.ContentOptions
import com.octopuscommunity.sdk.sample.ui.community.HostOverrides

/**
 * QA tooling for the per-content-type pictures/polls gating (internal ticket).
 *
 * Applies a DEBUG-only local override of the community [ContentOptions] — without a backend-driven
 * config — so the creation gating can be exercised in the sample independently of which community
 * is seeded. Each preset turns one affordance off, or lets a post with an image go without text;
 * open the post composer (or the comment / reply input) after applying one to observe the matching
 * picture / poll button disappear, or to publish a post with an image and no text.
 *
 * State is routed through [HostOverrides.contentOptionsOverride] rather than a local
 * `rememberSaveable` label (as before the override-UX pass): the currently-applied preset renders
 * selected ([ModeButton]), an [OverrideActiveBadge] shows only while an override is active, and the
 * "Clear override" button — `qa-preset-contentOptions-clear`, a first-class catalog preset, not
 * sample-added sugar — stays present but disabled while there is nothing to clear, so its id always
 * resolves at runtime.
 *
 * Test ids (`qa-preset-contentOptions-N` / `contentOptions-result`) mirror the sibling
 * [ProfileLockSection] and the shared scenario catalog, so the QA pipeline drives both
 * platforms with the same ids. `testTagsAsResourceId` surfaces them as resource-ids in the
 * uiautomator / `android layout` tree.
 */
@OptIn(InternalOctopusApi::class)
@Composable
fun ContentOptionsSection() {
    val applied = HostOverrides.contentOptionsOverride
    val hasOverride = applied != null

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionIntro(
            "Debug-only: override the community content options locally, then open the post " +
                "composer (or a comment / reply input) to see the matching picture / poll " +
                "affordance disappear, or publish a post with an image and no text.",
        )

        if (hasOverride) {
            OverrideActiveBadge(testId = "contentOptions-override-badge")
        }

        Preset(
            label = "All enabled (default / no-op)",
            testId = "qa-preset-contentOptions-1",
            options = ContentOptions(),
            applied = applied,
        )
        Preset(
            label = "Post: pictures off",
            testId = "qa-preset-contentOptions-2",
            options = ContentOptions(post = ContentOptions.PostOptions(enablePictures = false)),
            applied = applied,
        )
        Preset(
            label = "Post: polls off",
            testId = "qa-preset-contentOptions-3",
            options = ContentOptions(post = ContentOptions.PostOptions(enablePolls = false)),
            applied = applied,
        )
        Preset(
            label = "Post: pictures + polls off",
            testId = "qa-preset-contentOptions-4",
            options =
                ContentOptions(
                    post = ContentOptions.PostOptions(enablePictures = false, enablePolls = false),
                ),
            applied = applied,
        )
        Preset(
            label = "Comment: pictures off",
            testId = "qa-preset-contentOptions-5",
            options = ContentOptions(comment = ContentOptions.CommentOptions(enablePictures = false)),
            applied = applied,
        )
        Preset(
            label = "Reply: pictures off",
            testId = "qa-preset-contentOptions-6",
            options = ContentOptions(reply = ContentOptions.ReplyOptions(enablePictures = false)),
            applied = applied,
        )
        Preset(
            label = "Post: text optional with an image",
            testId = "qa-preset-contentOptions-7",
            options = ContentOptions(post = ContentOptions.PostOptions(textRequired = false)),
            applied = applied,
        )

        // qa-preset-contentOptions-clear is a first-class catalog preset, not sample-added UX
        // sugar — it must stay in the composition (enabled = hasOverride) rather than being removed
        // when there is nothing to clear, or a QA teardown / up-front resolve of this id from a
        // clean state finds no node.
        ModeButton(
            label = "Clear override (backend default)",
            selected = false,
            enabled = hasOverride,
            testId = "qa-preset-contentOptions-clear",
            onClick = { HostOverrides.applyContentOptionsOverride(null) },
        )

        Text(
            modifier = Modifier.testTag("contentOptions-result"),
            text = "Applied: ${applied?.let(::contentOptionsLabel) ?: "none (backend default)"}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(InternalOctopusApi::class)
@Composable
private fun Preset(
    label: String,
    testId: String,
    options: ContentOptions,
    applied: ContentOptions?,
) {
    ModeButton(
        label = label,
        selected = applied == options,
        testId = testId,
        onClick = { HostOverrides.applyContentOptionsOverride(options) },
    )
}

/**
 * Renders the preset's own label rather than [ContentOptions]'s raw `toString()`
 * (`ContentOptions(post=PostOptions(enablePictures=false, …), …)`), mirroring
 * [ProfileLockSection]'s `lockLabel()`. `contentOptions-result` is a catalog `result_test_id`, so
 * changing its rendered text shape is worth keeping deliberate and readable.
 */
private fun contentOptionsLabel(options: ContentOptions): String =
    when (options) {
        ContentOptions() -> "All enabled (default / no-op)"
        ContentOptions(post = ContentOptions.PostOptions(enablePictures = false)) -> "Post: pictures off"
        ContentOptions(post = ContentOptions.PostOptions(enablePolls = false)) -> "Post: polls off"
        ContentOptions(
            post = ContentOptions.PostOptions(enablePictures = false, enablePolls = false),
        ),
        -> "Post: pictures + polls off"
        ContentOptions(comment = ContentOptions.CommentOptions(enablePictures = false)) -> "Comment: pictures off"
        ContentOptions(reply = ContentOptions.ReplyOptions(enablePictures = false)) -> "Reply: pictures off"
        ContentOptions(post = ContentOptions.PostOptions(textRequired = false)) -> "Post: text optional with an image"
        else -> options.toString()
    }
