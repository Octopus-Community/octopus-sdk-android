package com.octopuscommunity.sdk.ui.components.reactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount
import com.octopuscommunity.sdk.domain.model.ProfileReaction
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusAvatar
import com.octopuscommunity.sdk.ui.components.OctopusModalBottomSheet
import com.octopuscommunity.sdk.ui.components.ReactionImage
import com.octopuscommunity.sdk.ui.components.getAccessibilityLabel
import com.octopuscommunity.sdk.ui.components.reactions.ReactionsListViewModel.Tab
import com.octopuscommunity.sdk.ui.components.reactions.ReactionsListViewModel.TabContent
import com.octopuscommunity.sdk.ui.medium
import com.octopuscommunity.sdk.ui.utils.emojiOverlapBorder
import org.koin.core.parameter.parametersOf

/**
 * The "who reacted" bottom sheet: one tab per reaction kind present on the content, plus an All tab when
 * there are several, each listing the members who reacted, most recent first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OctopusReactionsListSheet(
    contentId: String,
    reactions: List<ReactionCount>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The sheet owns its own ViewModelStore instead of borrowing the screen's. Two problems are solved
    // at once, and neither could be by a key alone:
    //  - state per open. The default owner is the screen's, which outlives this composable's presence
    //    on screen — dismissing the sheet drops it out of composition (see the `if (showReactionsSheet)`
    //    at the call site) but leaves the cached instance in place, so reopening the *same* post's sheet
    //    served stale counts, tabs and pages from before the member reacted again. A store created here
    //    is new on every (re-)entry into composition, i.e. on every open, so the instance is always
    //    seeded with the current `reactions` snapshot — while staying stable across the recompositions
    //    the sheet goes through while it remains open (pagination progress therefore survives
    //    `reactions` changing live, e.g. another member reacting concurrently).
    //  - release on dismiss. Keying a fresh instance into the *screen's* store — the previous fix — left
    //    every past instance in it, holding its loaded pages until the screen left the back stack: one
    //    leaked view model per open. Clearing our own store on dispose runs `onCleared`, which cancels
    //    `viewModelScope` and drops the pages with it.
    val viewModelStoreOwner =
        remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
    DisposableEffect(viewModelStoreOwner) {
        onDispose { viewModelStoreOwner.viewModelStore.clear() }
    }
    val viewModel =
        octopusViewModel<ReactionsListViewModel>(viewModelStoreOwner = viewModelStoreOwner) {
            parametersOf(contentId, reactions)
        }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { state.tabs.size })

    // Two-way binding between the pager and the tab bar: swiping moves the selection, tapping a tab
    // animates the pager. Only *settled* pages count — currentPage flips at half a swipe, which would
    // fire animateScrollToPage while the gesture is still running and leave the pager stuck mid-page.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect(viewModel::selectTab)
    }
    LaunchedEffect(state.selectedTabIndex) {
        if (pagerState.currentPage != state.selectedTabIndex) {
            pagerState.animateScrollToPage(state.selectedTabIndex)
        }
    }

    OctopusModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        // Fills the sheet, which is what makes Material open it half-height and let it be dragged to
        // full screen — the equivalent of iOS's [.medium, .large] detents. It also keeps the height
        // stable between tabs: sized to its content, the sheet would resize from one list to another.
        Column(modifier = Modifier.fillMaxHeight()) {
            // No top padding, where iOS puts 16dp: its drag indicator is drawn *over* the sheet's top
            // edge, while BottomSheetDefaults.DragHandle is a real child taking 48dp above us. Adding
            // the padding would push the tab bar to 64dp from the top against iOS's 16.
            ReactionTabsBar(
                tabs = state.tabs,
                counts = state.counts,
                selectedIndex = state.selectedTabIndex,
                onTabClick = viewModel::selectTab,
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val tab = state.tabs[page]
                ReactionsList(
                    content = state.content(tab),
                    onProfileClick = { reaction ->
                        // The profile opens beneath the sheet, so dismiss it before navigation.
                        onDismissRequest()
                        viewModel.onProfileClick(reaction)
                    },
                    onLoadMore = { viewModel.loadMore(tab) },
                    onRetry = { viewModel.retry(tab) },
                )
            }
        }
    }
}

/**
 * Horizontal tab strip, in the design system's tab style: 44dp tall, 2dp underline on the selected one.
 * Scrolls horizontally because the number of tabs follows the reaction kinds actually used, which can
 * overflow the width.
 */
@Composable
internal fun ReactionTabsBar(
    tabs: List<Tab>,
    counts: Map<Tab, Int>,
    selectedIndex: Int,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ruleColor = OctopusTheme.colorScheme.gray300
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                // drawBehind rather than a sibling Box: the selected tab's 2dp underline shares this
                // baseline and has to sit on top of the rule, which child order alone did not grant.
                .drawBehind {
                    val inset = 16.dp.toPx()
                    val y = size.height - 1.dp.toPx() / 2
                    drawLine(
                        color = ruleColor,
                        start = Offset(inset, y),
                        end = Offset(size.width - inset, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                },
    ) {
        Row(
            modifier =
                Modifier
                    .horizontalScroll(rememberScrollState())
                    // Announces the strip as a set of mutually exclusive tabs rather than as unrelated
                    // buttons.
                    .selectableGroup()
                    // 8 here + 8 inside each tab gives the first label the theme's 16dp margin, while the
                    // tap areas stay adjacent.
                    .padding(horizontal = 8.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                ReactionTab(
                    tab = tab,
                    count = counts[tab],
                    isSelected = index == selectedIndex,
                    onClick = { onTabClick(index) },
                )
            }
        }
    }
}

@Composable
private fun ReactionTab(
    tab: Tab,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val color =
        if (isSelected) {
            OctopusTheme.colorScheme.primary
        } else {
            OctopusTheme.colorScheme.gray700
        }
    // Merging the children would announce a bare "54": a kind tab holds an image with no label of its
    // own plus the count, so the kind has to be spelled out here.
    val contentDescription =
        when (tab) {
            is Tab.All -> stringResource(R.string.reaction_list_tabs_all)
            is Tab.OfKind ->
                pluralStringResource(
                    R.plurals.accessibility_reaction_count_reaction_count,
                    count ?: 0,
                    tab.kind.getAccessibilityLabel(),
                    count ?: 0,
                )
        }
    val stateDescription =
        stringResource(
            if (isSelected) {
                R.string.accessibility_common_selected
            } else {
                R.string.accessibility_common_not_selected
            },
        )
    Column(
        modifier =
            Modifier
                // selectable, not clickable: it carries the selected state the tab strip needs to
                // announce, and still supplies the same ripple.
                .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
                .semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                    this.stateDescription = stateDescription
                }.padding(horizontal = 8.dp)
                // Inside a horizontal scroll the width is unbounded, so the underline's fillMaxWidth
                // would resolve to zero and never show. This gives the column the label's own width.
                .width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            when (tab) {
                // No count here, per the design: the total is implied by the per-kind tabs.
                is Tab.All ->
                    Text(
                        text = stringResource(R.string.reaction_list_tabs_all),
                        style = OctopusTheme.typography.body2.medium,
                        color = color,
                    )

                is Tab.OfKind -> {
                    ReactionImage(kind = tab.kind, modifier = Modifier.size(20.dp))
                    Text(
                        text = (count ?: 0).toString(),
                        style = OctopusTheme.typography.body2.medium,
                        color = color,
                    )
                }
            }
        }
        // Spans the label only, not the tap area: the design underlines the text.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(if (isSelected) OctopusTheme.colorScheme.primary else Color.Transparent),
        )
    }
}

@Composable
internal fun ReactionsList(
    content: TabContent,
    onProfileClick: (ProfileReaction) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(content.reactions) { reaction ->
            ReactionProfileRow(reaction = reaction, onClick = { onProfileClick(reaction) })
        }
        item {
            when {
                // Also covers a page that has not started loading: the pager brings the next tab into
                // view before `settledPage` selects it and the fetch begins, and a spinner reads better
                // there than the blank list the member would otherwise swipe onto.
                content.error == null && (!content.hasLoadedOnce || content.isLoadingMore) ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = OctopusTheme.colorScheme.primary)
                    }

                content.error != null ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = content.error.asString(),
                            style = OctopusTheme.typography.body2,
                            color = OctopusTheme.colorScheme.gray700,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            modifier = Modifier.clickable(role = Role.Button, onClick = onRetry),
                            text = stringResource(R.string.common_retry),
                            style = OctopusTheme.typography.body2.medium,
                            color = OctopusTheme.colorScheme.primary,
                        )
                    }

                // Loaded, exhausted (or the cursor-repeat guard stopped it) and still zero rows — without
                // this branch the sheet would otherwise render as a blank list.
                content.hasLoadedOnce && content.reactions.isEmpty() && !content.canLoadMore ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.reaction_list_empty),
                            style = OctopusTheme.typography.body2,
                            color = OctopusTheme.colorScheme.gray700,
                            textAlign = TextAlign.Center,
                        )
                    }

                // Paging is triggered by reaching the end of the list rather than by a button.
                content.canLoadMore -> LaunchedEffect(content.nextCursor) { onLoadMore() }
            }
        }
    }
}

/** One row of the list: the member who reacted, with their reaction badged on their avatar. */
@Composable
internal fun ReactionProfileRow(
    reaction: ProfileReaction,
    onClick: () -> Unit,
) {
    val author = reaction.author
    val nickname = author?.nickname ?: stringResource(R.string.author_deleted)
    // The badge is a bare image: without this the row would announce the name alone and the reaction —
    // the whole point of the list — would be lost.
    val reactionLabel = reaction.kind.getAccessibilityLabel()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                // A deleted or banned member has no profile to open, so the row stays inert.
                .then(if (author != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .semantics(mergeDescendants = true) {
                    this.contentDescription = nickname
                    this.stateDescription = reactionLabel
                }
                // 8 + the 40dp avatar gives the design's 56dp row.
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box {
            OctopusAvatar(
                nickname = author?.nickname,
                avatar = author?.picture,
                modifier = Modifier.size(40.dp),
                defaultAvatar = author?.defaultAvatar,
            )
            // Badged on the avatar rather than trailing the row, as the design has it: it reads as "this
            // member reacted with this" instead of as a separate column.
            ReactionImage(
                kind = reaction.kind,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(16.dp)
                        .emojiOverlapBorder(OctopusTheme.colorScheme.background),
            )
        }
        Text(
            text = nickname,
            style = OctopusTheme.typography.body2.medium,
            color = OctopusTheme.colorScheme.gray900,
            maxLines = 1,
        )
    }
}
