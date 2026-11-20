package com.octopuscommunity.sdk.ui.responses.blocks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.components.TranslationToggle

/**
 * "See original" / "See translation" toggle inside a translated comment or reply bubble, right
 * under the bubble header.
 *
 * Its icon box starts 9dp from the bubble edge, 3dp before the 12dp inset of the bubble text: the
 * glyph has whitespace inside its box, so this lines it up with the text. The 8dp after the label
 * matches the bubble's trailing inset. Layout and touch area: see [TranslationToggle].
 */
@Composable
internal fun ResponseTranslationToggle(
    translated: Boolean,
    originalLanguage: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TranslationToggle(
        modifier = modifier,
        translated = translated,
        originalLanguage = originalLanguage,
        onClick = onClick,
        startPadding = 9.dp,
        endPadding = 8.dp,
    )
}

@Preview(showBackground = true)
@Composable
private fun ResponseTranslationTogglePreview() {
    OctopusTheme {
        ResponseTranslationToggle(
            translated = false,
            originalLanguage = "fr",
            onClick = {},
        )
    }
}
