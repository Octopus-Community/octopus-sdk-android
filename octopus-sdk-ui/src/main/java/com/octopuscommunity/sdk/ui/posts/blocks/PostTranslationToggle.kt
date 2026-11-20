package com.octopuscommunity.sdk.ui.posts.blocks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.components.TranslationToggle

/**
 * "See original" / "See translation" toggle of a translated post, right under the post header.
 *
 * Its icon box starts 13dp from the card edge, 3dp before the 16dp text gutter: the glyph has
 * whitespace inside its box, so this lines it up with the post text. The 16dp after the label
 * belongs to the touch area. Layout and touch area: see [TranslationToggle].
 */
@Composable
internal fun PostTranslationToggle(
    translated: Boolean,
    originalLanguage: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    touchTargetModifier: Modifier = Modifier,
) {
    TranslationToggle(
        modifier = modifier,
        touchTargetModifier = touchTargetModifier,
        translated = translated,
        originalLanguage = originalLanguage,
        onClick = onClick,
        startPadding = 13.dp,
        endPadding = 16.dp,
    )
}

@Preview(showBackground = true)
@Composable
private fun PostTranslationTogglePreview() {
    OctopusTheme {
        PostTranslationToggle(
            translated = false,
            originalLanguage = "fr",
            onClick = {},
        )
    }
}
