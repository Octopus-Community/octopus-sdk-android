package com.octopuscommunity.sdk.ui.posts.blocks

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
 * User-generated text block. Rendered identically in summary (feed) and detail contexts —
 * markdown, markdown links and bare URLs in both — the feed differing only in that it is cut to
 * [TextTruncation.Content] and gains a "… Read more" suffix (internal ticket, iso iOS internal change).
 *
 * Renders nothing when the resolved text is empty.
 */
@Composable
internal fun PostTextContent(
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
    // Iso iOS: the translate toggle (and the inline "Read more" of a commentless post) swaps the
    // text with a 200 ms crossfade. Fade only, no size animation: a size tween clips the text at
    // rest and fights the list's `animateItem()` placement, which already moves the neighbours.
    AnimatedContent(
        targetState = body,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = {
            fadeIn(tween(OctopusMotion.SHORT_DURATION_MS)) togetherWith
                fadeOut(tween(OctopusMotion.SHORT_DURATION_MS)) using null
        },
        label = "PostTextContent",
    ) { shown ->
        BodySelectionContainer(isTruncated = shown.isTruncated) {
            Text(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                text = shown.value,
                color = OctopusTheme.colorScheme.gray900,
                // Figma `Content Text` (node 1870:15551): `body-2-regular` 16sp · lineHeight 24sp
                // (~1.5× ratio).
                style = OctopusTheme.typography.body2.lineHeight150Trimmed,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PostTextContentFullPreview() {
    OctopusTheme {
        PostTextContent(
            text =
                TranslatableText(
                    originalText = "A longer multi-line text with some\nline breaks",
                    originalLanguage = "en",
                ),
            translated = false,
            ellipsize = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PostTextContentTallInkPreview() {
    OctopusTheme {
        PostTextContent(
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
private fun PostTextContentEllipsizedPreview() {
    OctopusTheme {
        PostTextContent(
            text =
                TranslatableText(
                    originalText = "Un texte assez long qui sera tronqué par la logique d'ellipsisation ".repeat(5),
                    originalLanguage = "fr",
                ),
            translated = false,
            ellipsize = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PostTextContentEllipsizedWithLinkPreview() {
    OctopusTheme {
        PostTextContent(
            text =
                TranslatableText(
                    originalText =
                        "Read the rules at https://octopuscommunity.com/rules before posting, " +
                            "and see the **highlights** or the [full guide](https://octopuscommunity.com/guide). " +
                            "This body is long enough to be cut by the feed so the link has to survive " +
                            "the truncation and still open its complete destination.",
                    originalLanguage = "en",
                ),
            translated = false,
            ellipsize = true,
        )
    }
}
