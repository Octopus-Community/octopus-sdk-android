package com.octopuscommunity.sdk.ui.responses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource

/** Stable, theme-derived placeholders; no shimmering motion or false empty state during first load. */
@Composable
internal fun ResponseListLoading(isReply: Boolean = false) {
    val color = OctopusTheme.colorScheme.gray300.copy(alpha = 0.55f)
    Column(
        modifier = Modifier.fillMaxWidth().progressSemantics().padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        repeat(3) {
            Row(
                modifier = Modifier.padding(start = if (isReply) 56.dp else 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(32.dp).background(color, CircleShape))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.width(96.dp).height(12.dp).background(color, RoundedCornerShape(6.dp)))
                    Box(Modifier.fillMaxWidth().height(12.dp).background(color, RoundedCornerShape(6.dp)))
                    Box(Modifier.fillMaxWidth(0.7f).height(12.dp).background(color, RoundedCornerShape(6.dp)))
                }
            }
        }
    }
}

/** Compact paging state, including an actionable failure in place of an endless loader. */
@Composable
internal fun ResponsePageLoading(
    failed: Boolean,
    onRetry: () -> Unit,
    showProgress: Boolean = true,
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!showProgress) {
            Box(Modifier.height(36.dp))
        } else if (failed) {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.common_retry), color = OctopusTheme.colorScheme.primary)
            }
        } else {
            CircularProgressIndicator(
                modifier = Modifier.padding(8.dp).size(20.dp),
                color = OctopusTheme.colorScheme.primary,
                trackColor = OctopusTheme.colorScheme.gray300,
                strokeWidth = 2.dp,
            )
        }
    }
}
