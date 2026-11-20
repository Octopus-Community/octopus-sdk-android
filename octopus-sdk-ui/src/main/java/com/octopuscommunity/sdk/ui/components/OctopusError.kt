package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.medium

/**
 * What a content area shows when its load failed.
 *
 * @param onRetry when non-null, a Retry button under the message. Only the screens that can
 * actually re-run the failed load pass it; the ones whose host already offers a pull-to-refresh
 * leave it null and render exactly as they did before it existed.
 */
@Composable
internal fun OctopusError(
    text: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            imageVector = Icons.Outlined.ErrorOutline,
            tint = OctopusTheme.colorScheme.gray500,
            contentDescription = null,
        )
        Text(
            text = text,
            textAlign = TextAlign.Center,
            color = OctopusTheme.colorScheme.gray500,
            style = OctopusTheme.typography.body2.medium,
        )
        onRetry?.let { retry ->
            OctopusMediumOutlinedButton(
                text = stringResource(R.string.common_retry),
                onClick = retry,
            )
        }
    }
}

internal fun LazyListScope.octopusErrorItem(
    text: UiText?,
    onRetry: (() -> Unit)? = null,
) = item {
    OctopusError(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
        text = text?.asString() ?: stringResource(R.string.error_unknown_formal),
        onRetry = onRetry,
    )
}

@Preview
@Composable
private fun OctopusErrorPreview() {
    OctopusTheme {
        OctopusError(text = "Error")
    }
}

@Preview
@Composable
private fun OctopusErrorRetryPreview() {
    OctopusTheme {
        OctopusError(text = "Error", onRetry = {})
    }
}
