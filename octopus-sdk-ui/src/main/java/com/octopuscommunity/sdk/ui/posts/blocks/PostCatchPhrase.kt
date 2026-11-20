package com.octopuscommunity.sdk.ui.posts.blocks

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.TranslatableText
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.lineHeight150Trimmed
import com.octopuscommunity.sdk.ui.semiBold

/**
 * Semibold catch-phrase block (e.g. a bridge post's catch-phrase).
 *
 * Renders nothing when the resolved text is empty.
 */
@Composable
internal fun PostCatchPhrase(
    text: TranslatableText,
    translated: Boolean,
    modifier: Modifier = Modifier,
) {
    val content = text.getText(translated = translated).ifEmpty { return }
    SelectionContainer {
        Text(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            text = content,
            color = OctopusTheme.colorScheme.gray900,
            // Figma `Content Catch phrase` (node 1870:15620): `body-2-semibold` 16sp · lineHeight 24sp.
            style = OctopusTheme.typography.body2.semiBold.lineHeight150Trimmed,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PostCatchPhrasePreview() {
    OctopusTheme {
        PostCatchPhrase(
            text =
                TranslatableText(
                    originalText = "Catch phrase",
                    originalLanguage = "en",
                ),
            translated = false,
        )
    }
}
