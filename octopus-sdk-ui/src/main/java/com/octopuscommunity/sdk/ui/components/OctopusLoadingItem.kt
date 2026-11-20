package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.utils.onVisibilityChanged

/**
 * @param topPadding how far down the content area the spinner sits. The default suits the
 * end-of-list pagination spinner; a first load with nothing on screen yet uses the per-screen offset
 * the design gives it (see [OctopusScreenStateDefaults]).
 * @param key the lazy item key. Needed when items are inserted before the spinner, so the list keeps
 * the spinner's identity instead of tying it to its index.
 */
internal fun LazyListScope.octopusLoadingItem(
    onVisibilityChanged: ((Boolean) -> Unit) = {},
    topPadding: Dp = 32.dp,
    key: Any? = null,
) = item(key = key) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .onVisibilityChanged(onVisibilityChanged)
                .padding(bottom = 32.dp, start = 32.dp, end = 32.dp)
                .padding(top = topPadding),
        contentAlignment = Alignment.Center,
    ) {
        OctopusProgressIndicator(
            color = OctopusTheme.colorScheme.primary,
        )
    }
}
