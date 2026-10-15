package com.octopuscommunity.sdk.sample.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Renders a result string with success/error styling.
 * Used by the scenario sections to display method execution results; the same DS code block as
 * [CodeResultBlock], so a result reads the same wherever the sample prints one.
 */
@Composable
fun ResultDisplay(
    result: String,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    CodeResultBlock(result = result, modifier = modifier, isError = isError)
}
