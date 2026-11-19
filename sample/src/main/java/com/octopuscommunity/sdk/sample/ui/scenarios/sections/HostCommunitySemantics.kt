package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import android.view.View
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

internal const val HOST_COMMUNITY_CONTENT_TAG = "hostLifecycles-community-content"
internal const val HOST_POST_CONTENT_TAG = "hostLifecycles-post-content"

/** The sample's test bench uses Compose's public testing interface, never SDK internals. */
internal class HostCommunitySemantics(
    private val view: View,
    /** The SDK's "open post" click label, in both tones. */
    private val openPostLabels: Set<String>,
) {
    private fun nodes(tag: String): Sequence<SemanticsNode> {
        val root = (view as? ViewRootForTest)?.semanticsOwner?.unmergedRootSemanticsNode ?: return emptySequence()
        val scoped = root.descendants().firstOrNull { it.config.getOrNull(SemanticsProperties.TestTag) == tag }
        return scoped?.descendants() ?: emptySequence()
    }

    fun clickFirstPost(): Boolean {
        val action =
            nodes(HOST_COMMUNITY_CONTENT_TAG)
                .filter { !it.boundsInWindow.isEmpty }
                .mapNotNull { it.config.getOrNull(SemanticsActions.OnClick) }
                .firstOrNull { it.label in openPostLabels }
        // This is the SDK card's real semantics click: it invokes its retained ViewModel handler.
        return action?.action?.invoke() == true
    }

    fun postContentDisplayed(): Boolean =
        nodes(HOST_POST_CONTENT_TAG).any {
            !it.boundsInWindow.isEmpty && it.config.getOrNull(SemanticsActions.OnClick)?.label in openPostLabels
        }
}

private fun SemanticsNode.descendants(): Sequence<SemanticsNode> =
    sequence {
        yield(this@descendants)
        children.forEach { yieldAll(it.descendants()) }
    }

/** Keep Running through loading/mapping and a continuous three-second dwell on rendered content. */
internal suspend fun awaitHostPostContent(isDisplayed: () -> Boolean) {
    withTimeout(30_000) {
        var displayedMillis = 0
        while (displayedMillis < 3_000) {
            if (!isDisplayed()) {
                displayedMillis = 0
                delay(100)
            } else {
                delay(100)
                displayedMillis = if (isDisplayed()) displayedMillis + 100 else 0
            }
        }
    }
}
