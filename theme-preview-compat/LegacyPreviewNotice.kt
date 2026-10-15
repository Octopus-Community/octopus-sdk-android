package com.octopuscommunity.tools.compat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Explains a retired SDK screen in the mirror-owned theme configurator. */
@Composable
internal fun LegacyPreviewNotice(screen: String) {
    Column(Modifier.padding(24.dp)) {
        Text(screen, style = MaterialTheme.typography.titleLarge)
        Text(
            "This screen is no longer provided by the SDK. Your host app supplies authentication and account settings.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
