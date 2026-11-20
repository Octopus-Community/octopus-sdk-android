package com.octopuscommunity.sdk.ui.responses.blocks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.TranslatableText
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.components.BodySelectionContainer
import com.octopuscommunity.sdk.ui.lineHeight150Trimmed
import com.octopuscommunity.sdk.ui.utils.OctopusMotion
import com.octopuscommunity.sdk.ui.utils.TextTruncation
import com.octopuscommunity.sdk.ui.utils.rememberBody

/**
 * User-generated response text block. Rendered identically in summary and detail contexts, the
 * summary differing only in that it is cut to [TextTruncation.Content] and gains a "… Read more"
 * suffix (internal ticket, iso iOS internal change). Renders nothing when the resolved text is empty.
 *
 * Iso iOS `ResponseTextContentView` — padding matches Figma (leading 12, trailing 8, bottom 2).
 */
@Composable
internal fun ResponseTextContent(
    text: TranslatableText,
    translated: Boolean,
    ellipsize: Boolean,
    modifier: Modifier = Modifier,
) {
    val raw = text.getText(translated = translated).ifEmpty { return }
    val body =
        rememberBody(
            text = raw,
            truncation = if (ellipsize) TextTruncation.Content else null,
        )
    // Iso iOS: the translate toggle swaps the text with a 200 ms crossfade. Fade only, no size
    // animation: a size tween clips the text at rest and fights the list's `animateItem()`
    // placement, which already moves the neighbours.
    AnimatedContent(
        targetState = body,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = {
            fadeIn(tween(OctopusMotion.SHORT_DURATION_MS)) togetherWith
                fadeOut(tween(OctopusMotion.SHORT_DURATION_MS)) using null
        },
        label = "ResponseTextContent",
    ) { shown ->
        BodySelectionContainer(isTruncated = shown.isTruncated) {
            Text(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 8.dp, bottom = 2.dp),
                text = shown.value,
                color = OctopusTheme.colorScheme.gray900,
                // Figma `Content Text` inside Comment Card: `body-2-regular` 16sp · lineHeight 24sp
                // (~1.5× ratio).
                style = OctopusTheme.typography.body2.lineHeight150Trimmed,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResponseTextContentPreview() {
    OctopusTheme {
        ResponseTextContent(
            text =
                TranslatableText(
                    originalText = "A comment with some text content.",
                    originalLanguage = "en",
                ),
            translated = false,
            ellipsize = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResponseTextContentTallInkPreview() {
    OctopusTheme {
        ResponseTextContent(
            text =
                TranslatableText(
                    originalText = "Ÿ É Å 😀 Ở Ẫ Ǻ\nSecond line",
                    originalLanguage = "fr",
                ),
            translated = false,
            ellipsize = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResponseTextContentEllipsizedWithLinkPreview() {
    OctopusTheme {
        ResponseTextContent(
            text =
                TranslatableText(
                    originalText =
                        "Details are at https://octopuscommunity.com/faq and in the " +
                            "[handbook](https://octopuscommunity.com/handbook). This comment is " +
                            "deliberately long enough that the feed cuts it, so the link must " +
                            "still be tappable after the cut, and this last sentence is what " +
                            "the truncation drops.",
                    originalLanguage = "en",
                ),
            translated = false,
            ellipsize = true,
        )
    }
}
