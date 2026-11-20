package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.lineHeight125
import com.octopuscommunity.sdk.ui.medium
import kotlinx.coroutines.launch

@Composable
internal fun OctopusTabs(
    tabs: List<String>,
    modifier: Modifier = Modifier,
    initialTabIndex: Int = 0,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) = Column(modifier = modifier.fillMaxSize()) {
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(initialPage = initialTabIndex) { tabs.size }

    // A lone tab has nothing to select: no tab row at all (nor its divider), so its page sits directly
    // under whatever is above it, and nothing stays pinned when that content scrolls away (internal ticket).
    // If the tabs shrink to one while another page is selected, the pager clamps its current page to
    // the remaining one.
    if (showsTabRow(tabCount = tabs.size)) {
        val edgePadding = 16.dp
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Give each tab a minimum width of an equal share of the row so short labels fill the full
            // width (matching iOS's segmented control) instead of packing to the left; a tab is still
            // free to grow past its share — and the row to scroll — when its label is too long to fit
            // (e.g. long German labels).
            val tabMinWidth = ((maxWidth - edgePadding * 2) / tabs.size).coerceAtLeast(0.dp)

            // `SecondaryScrollableTabRow`, not `ScrollableTabRow`: the latter and the
            // `TabRowDefaults.tabIndicatorOffset(TabPosition)` it goes with are both deprecated. The
            // replacement passes the selected *index* to a scoped `tabIndicatorOffset` instead of a
            // measured `TabPosition`, which is the same full-tab-width indicator (`matchContentSize`
            // defaults to false) — the row is the M3 "secondary" variant either way.
            SecondaryScrollableTabRow(
                modifier =
                    Modifier
                        .fillMaxWidth(),
                selectedTabIndex = pagerState.currentPage,
                edgePadding = edgePadding,
                contentColor = OctopusTheme.colorScheme.gray700,
                containerColor = OctopusTheme.colorScheme.background,
                indicator = {
                    SecondaryIndicator(
                        modifier =
                            Modifier
                                .tabIndicatorOffset(pagerState.currentPage),
                        height = 3.dp,
                        color = OctopusTheme.colorScheme.primary,
                    )
                },
                divider = {
                    HorizontalDivider(
                        color = OctopusTheme.colorScheme.gray700,
                        thickness = 1.dp,
                    )
                },
            ) {
                tabs.forEachIndexed { page, tab ->
                    OctopusTab(
                        modifier = Modifier.widthIn(min = tabMinWidth),
                        text = tab,
                        selected = pagerState.currentPage == page,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        },
                    )
                }
            }
        }
    }

    HorizontalPager(
        modifier =
            Modifier
                .fillMaxSize(),
        state = pagerState,
    ) { page ->
        pageContent(page)
    }
}

/** Whether [OctopusTabs] draws its tab row: only when there are at least two tabs to choose from. */
internal fun showsTabRow(tabCount: Int): Boolean = tabCount >= 2

@Composable
internal fun OctopusTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String,
) {
    Tab(
        modifier = modifier,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        text = {
            Text(
                text = text,
                style = OctopusTheme.typography.caption1.medium.lineHeight125,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        selectedContentColor = if (enabled) OctopusTheme.colorScheme.primary else OctopusTheme.colorScheme.gray500,
        unselectedContentColor = if (enabled) OctopusTheme.colorScheme.gray700 else OctopusTheme.colorScheme.gray500,
    )
}

@Preview(showBackground = true)
@Composable
private fun OctopusTabsPreview() {
    OctopusTheme {
        OctopusTabs(
            tabs = listOf("Title1", "Title2"),
            pageContent = {},
        )
    }
}
