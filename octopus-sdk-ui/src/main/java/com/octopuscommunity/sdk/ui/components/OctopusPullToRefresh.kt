package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.octopuscommunity.sdk.ui.OctopusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OctopusPullToRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indicatorPadding: PaddingValues = PaddingValues(),
    content: @Composable BoxScope.() -> Unit,
) {
    // One layout whether enabled or not: a list that turns the gesture on or off while shown keeps
    // its content, and its scroll position, instead of being rebuilt under another parent.
    val state = rememberPullToRefreshState()
    Box(
        modifier =
            modifier.pullToRefresh(
                state = state,
                isRefreshing = refreshing,
                enabled = enabled,
                onRefresh = onRefresh,
            ),
    ) {
        content()
        if (enabled) {
            Indicator(
                modifier =
                    Modifier
                        .padding(indicatorPadding)
                        .align(Alignment.TopCenter),
                state = state,
                isRefreshing = refreshing,
                containerColor = OctopusTheme.colorScheme.background,
                color = OctopusTheme.colorScheme.primary,
            )
        }
    }
}
