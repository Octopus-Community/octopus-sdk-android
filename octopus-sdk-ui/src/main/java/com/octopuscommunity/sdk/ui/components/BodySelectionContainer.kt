package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable

/** Selection follows actual truncation, including when nested in a host selection container. */
@Composable
internal fun BodySelectionContainer(
    isTruncated: Boolean,
    content: @Composable () -> Unit,
) {
    if (isTruncated) {
        DisableSelection(content = content)
    } else {
        SelectionContainer(content = content)
    }
}
