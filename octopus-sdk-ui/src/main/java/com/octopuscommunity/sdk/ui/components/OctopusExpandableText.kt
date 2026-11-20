package com.octopuscommunity.sdk.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.utils.Ellipsis
import com.octopuscommunity.sdk.ui.utils.OctopusMotion
import com.octopuscommunity.sdk.ui.utils.TextTruncation
import com.octopuscommunity.sdk.ui.utils.buildExpandableText
import com.octopuscommunity.sdk.ui.utils.ellipsized

@Composable
internal fun OctopusExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = OctopusTheme.typography.body2,
    color: Color = OctopusTheme.colorScheme.gray900,
    linkify: Boolean = true,
    animateSize: Boolean = true,
) {
    val ellipsized = remember(text) { text.ellipsized(TextTruncation.Summary) }
    val isTruncated = ellipsized.isTruncated
    var isEllipsized by rememberSaveable(ellipsized.value) { mutableStateOf(isTruncated) }

    OctopusExpandableText(
        text = text,
        isEllipsized = isEllipsized,
        onToggleExpanded = { isEllipsized = !isEllipsized },
        modifier = modifier,
        style = style,
        color = color,
        linkify = linkify,
        animateSize = animateSize,
    )
}

/** Stateless rendering shared by the saved expansion state and deterministic previews. */
@Composable
internal fun OctopusExpandableText(
    text: String,
    isEllipsized: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = OctopusTheme.typography.body2,
    color: Color = OctopusTheme.colorScheme.gray900,
    linkify: Boolean = true,
    animateSize: Boolean = true,
) {
    val ellipsized = remember(text) { text.ellipsized(TextTruncation.Summary) }
    val isTruncated = ellipsized.isTruncated
    val clickableModifier =
        modifier.then(if (isTruncated) Modifier.clickable(onClick = onToggleExpanded) else Modifier)
    val displayedText =
        buildExpandableText(
            text = if (isEllipsized) ellipsized.value else text,
            linkify = linkify && !isEllipsized,
            ellipsis =
                if (isTruncated) {
                    if (isEllipsized) Ellipsis.More else Ellipsis.Less
                } else {
                    null
                },
        )

    if (animateSize) {
        Text(
            modifier = clickableModifier.animateContentSize(),
            text = displayedText,
            style = style,
            color = color,
        )
    } else {
        // Group descriptions live in a list header: its posts own the placement animation.
        AnimatedContent(
            targetState = displayedText,
            modifier = clickableModifier,
            transitionSpec = {
                fadeIn(tween(OctopusMotion.SHORT_DURATION_MS)) togetherWith
                    fadeOut(tween(OctopusMotion.SHORT_DURATION_MS)) using null
            },
            label = "ExpandableText",
        ) { shown ->
            Text(text = shown, style = style, color = color)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusExpandableTextPreview() {
    OctopusTheme {
        OctopusExpandableText(
            text =
                "This is a very long description that should be truncated after a certain " +
                    "number of characters or lines to demonstrate the expandable text behavior " +
                    "with a read more button at the end of the ellipsized content.",
        )
    }
}
