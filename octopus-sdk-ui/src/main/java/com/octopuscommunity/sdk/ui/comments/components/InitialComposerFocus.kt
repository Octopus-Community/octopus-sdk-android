package com.octopuscommunity.sdk.ui.comments.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned

/** Consume route focus once, retaining that decision when returning from media/profile navigation. */
@Composable
internal fun Modifier.initialComposerFocus(
    enabled: Boolean,
    requestFocus: () -> Unit,
): Modifier {
    var requested by rememberSaveable { mutableStateOf(false) }
    return onGloballyPositioned {
        if (enabled && !requested) {
            requested = true
            requestFocus()
        }
    }
}
