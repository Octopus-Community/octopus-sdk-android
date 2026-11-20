package com.octopuscommunity.sdk.ui.groups.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.ConstrainedContentColumn
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusProgressIndicator
import com.octopuscommunity.sdk.ui.components.OctopusPullToRefresh
import com.octopuscommunity.sdk.ui.components.OctopusScreenState
import com.octopuscommunity.sdk.ui.components.OctopusScreenStateDefaults
import com.octopuscommunity.sdk.ui.groups.components.FollowButton
import com.octopuscommunity.sdk.ui.medium
import com.octopuscommunity.sdk.ui.semiBold

@Composable
fun GroupsList(
    modifier: Modifier = Modifier,
    pullToRefresh: Boolean = true,
    selectedGroupId: String? = null,
    footer: (@Composable () -> Unit)? = null,
    selectable: Boolean = false,
    onGroupClick: ((OctopusGroup) -> Unit)? = null,
) = OctopusContent {
    val viewModel = octopusViewModel<GroupsListViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    // internal ticket: when used as the create-post topic picker (`selectable = true`), exclude
    // topics that the current user cannot post in. The feed list (`selectable = false`)
    // keeps all groups so locked ones can route through the host app's access-denied
    // callback. Mirrors iOS GroupListViewModel.filtered(_:) (internal change).
    val displayedSections =
        remember(state.groups, selectable) {
            fun List<OctopusGroup>.filteredForContext(): List<OctopusGroup> = if (selectable) filter { it.canCreateChildren } else this
            state.groups
                .map { group ->
                    when (group) {
                        is GroupsListViewModel.UiState.Group.NoSection ->
                            group.copy(topics = group.topics.filteredForContext())
                        is GroupsListViewModel.UiState.Group.Section ->
                            group.copy(topics = group.topics.filteredForContext())
                    }
                }.filter { it.topics.isNotEmpty() }
        }
    OctopusPullToRefresh(
        modifier = modifier,
        enabled = pullToRefresh,
        refreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
    ) {
        // Cap & center the list column on large screens so the follow CTA stays close to the group
        // name instead of being pushed to the far edge in landscape (internal ticket).
        ConstrainedContentColumn {
            when {
                state.isLoading -> {
                    // At the design's offset rather than centered, so it lands where the first row
                    // will be.
                    OctopusProgressIndicator(
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = OctopusScreenStateDefaults.ExploreLoaderOffset),
                    )
                }

                state.hasLoadFailure -> {
                    OctopusScreenState(
                        modifier = Modifier.align(Alignment.TopCenter),
                        illustration =
                            if (state.loadFailureIsNoNetwork) {
                                OctopusTheme.icons.screenStates.networkError()
                            } else {
                                OctopusTheme.icons.screenStates.error()
                            },
                        text =
                            stringResource(
                                if (state.loadFailureIsNoNetwork) {
                                    R.string.error_no_network_formal
                                } else {
                                    R.string.error_unknown_formal
                                },
                            ),
                        verticalPadding = OctopusScreenStateDefaults.ExplorePadding,
                        action = stringResource(R.string.common_retry) to viewModel::retryFirstLoad,
                    )
                }

                footer == null -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        displayedSections.forEachIndexed { index, group ->
                            if (group is GroupsListViewModel.UiState.Group.Section) {
                                item(key = "section_${group.id}") {
                                    GroupSectionHeader(
                                        label = group.label.asString(),
                                        showSeparator = index > 0,
                                    )
                                }
                            }
                            items(group.topics, key = { "${group.id}_${it.id}" }) { topic ->
                                GroupItem(
                                    topic = topic,
                                    selectable = selectable,
                                    isSelected = topic.id == selectedGroupId,
                                    onFollowChanged = { viewModel.setFollowed(topic, it) },
                                    onClick = {
                                        if (!topic.canAccess) {
                                            viewModel.notifyGroupAccessDenied(topic.id)
                                        } else {
                                            onGroupClick?.invoke(topic)
                                        }
                                    },
                                )
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }
                    }
                }

                else -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        check(constraints.hasBoundedHeight) {
                            "GroupsList with a non-null footer requires a parent with bounded height"
                        }
                        StretchToBottomScrollableColumn(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                            minHeightPx = constraints.maxHeight,
                            stickyFooter = footer,
                            content = {
                                displayedSections.forEachIndexed { index, group ->
                                    if (group is GroupsListViewModel.UiState.Group.Section) {
                                        GroupSectionHeader(
                                            label = group.label.asString(),
                                            showSeparator = index > 0,
                                        )
                                    }
                                    group.topics.forEach { topic ->
                                        GroupItem(
                                            topic = topic,
                                            selectable = selectable,
                                            isSelected = topic.id == selectedGroupId,
                                            onFollowChanged = { viewModel.setFollowed(topic, it) },
                                            onClick = {
                                                if (!topic.canAccess) {
                                                    viewModel.notifyGroupAccessDenied(topic.id)
                                                } else {
                                                    onGroupClick?.invoke(topic)
                                                }
                                            },
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

// `internal` (not `private`) so the host-side screenshot test can lock the separator + 20/8 padding
// directly (see GroupSectionHeaderScreenshotTest). Not part of the public API.
@Composable
internal fun GroupSectionHeader(
    label: String,
    showSeparator: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // A full-width 1px separator is drawn above every section block except the first rendered
        // one. The 8dp of space above the line is provided by the inter-block spacer emitted after
        // the previous section (see the `Spacer(8.dp)` in the list loops). Matches iOS internal change
        // (GroupListView.SectionView).
        if (showSeparator) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OctopusTheme.colorScheme.gray300),
            )
        }
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 8.dp),
            text = label,
            style = OctopusTheme.typography.body2.semiBold,
            color = OctopusTheme.colorScheme.gray500,
        )
    }
}

/**
 * Lays out [content] from the top, [stickyFooter] at the bottom, and stretches its overall height
 * to at least [minHeightPx] so the footer pins to the viewport bottom on short lists and follows
 * the last item on long lists. Mirrors the iOS `VStack(minHeight: geometry.size.height) + Spacer`
 * approach inside a vertical scroll.
 */
@Composable
private fun StretchToBottomScrollableColumn(
    minHeightPx: Int,
    stickyFooter: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        contents = listOf(content, stickyFooter),
    ) { (contentMeasurables, footerMeasurables), constraints ->
        val childConstraints = constraints.copy(minHeight = 0)
        val contentPlaceables = contentMeasurables.map { it.measure(childConstraints) }
        val footerPlaceables = footerMeasurables.map { it.measure(childConstraints) }

        val contentHeight = contentPlaceables.sumOf { it.height }
        val footerHeight = footerPlaceables.sumOf { it.height }
        val laidOutHeight =
            maxOf(contentHeight + footerHeight, minHeightPx)
                .coerceAtLeast(constraints.minHeight)
        val laidOutWidth =
            (contentPlaceables + footerPlaceables)
                .maxOfOrNull { it.width }
                ?.coerceAtLeast(constraints.minWidth)
                ?: constraints.minWidth

        layout(laidOutWidth, laidOutHeight) {
            var y = 0
            contentPlaceables.forEach {
                it.placeRelative(0, y)
                y += it.height
            }
            var fy = laidOutHeight - footerHeight
            footerPlaceables.forEach {
                it.placeRelative(0, fy)
                fy += it.height
            }
        }
    }
}

@Composable
internal fun GroupItem(
    topic: OctopusGroup,
    onFollowChanged: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectable: Boolean = false,
    isSelected: Boolean = false,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .heightIn(min = 44.dp)
                    .padding(start = 16.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = topic.name,
                style = OctopusTheme.typography.body1.medium,
                color = OctopusTheme.colorScheme.gray900,
            )

            if (selectable) {
                if (isSelected) {
                    Icon(
                        modifier =
                            Modifier
                                .padding(end = 10.dp)
                                .size(24.dp),
                        painter = OctopusTheme.icons.groups.selected(),
                        contentDescription = null,
                        tint = OctopusTheme.colorScheme.primary,
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(end = 7.dp),
                ) {
                    if (topic.canChangeFollowStatus) {
                        FollowButton(
                            modifier = Modifier.heightIn(max = 32.dp),
                            isFollowed = topic.isFollowed,
                            onFollowChanged = onFollowChanged,
                        )
                    }

                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = OctopusTheme.icons.cellNavIndicator(),
                        contentDescription = null,
                        tint = OctopusTheme.colorScheme.gray300,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupsListPreview() {
    OctopusTheme {
        GroupsList()
    }
}
