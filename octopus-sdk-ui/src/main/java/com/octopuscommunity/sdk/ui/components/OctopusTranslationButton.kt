package com.octopuscommunity.sdk.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OctopusTranslationButton(
    translated: Boolean,
    originalLanguage: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OctopusActionButton(
        modifier = modifier,
        text =
            if (translated) {
                if (originalLanguage.isNotEmpty()) {
                    stringResource(R.string.common_view_original_language, originalLanguage)
                } else {
                    stringResource(R.string.common_view_original)
                }
            } else {
                stringResource(R.string.common_view_translation)
            },
        onClick = onClick,
    )
}

@Preview(showBackground = true)
@Composable
private fun OctopusTranslationButtonPreview() {
    OctopusTheme {
        OctopusTranslationButton(
            translated = false,
            originalLanguage = "fr",
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusTranslationButtonTranslatedPreview() {
    OctopusTheme {
        OctopusTranslationButton(
            translated = true,
            originalLanguage = "en",
            onClick = {},
        )
    }
}
