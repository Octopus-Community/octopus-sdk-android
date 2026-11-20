package com.octopuscommunity.sdk.ui.responses

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemsListViewModel
import com.octopuscommunity.sdk.ui.common.viewmodel.ScrollTo
import com.octopuscommunity.sdk.ui.components.OctopusPullToRefresh
import com.octopuscommunity.sdk.ui.components.octopusScreenStateFailureItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

/** Key of the header item. The header always sits at index 0; the key only names it. */
private const val HEADER_ITEM_KEY = "responses_list_header"

/** Key of the loader above the items, shown while older items are still on the server. */
private const val OLDER_LOADER_KEY = "responses_list_older_loader"

/** Key of the loader below the items, shown while newer items are still on the server. */
private const val NEWER_LOADER_KEY = "responses_list_newer_loader"

/** How close to the top of the list the first item on screen must be to load older items. */
private const val LOAD_OLDER_THRESHOLD = 3

/** How close to the bottom of the list the last item on screen must be to load newer items. */
private const val LOAD_NEWER_THRESHOLD = 3

/**
 * Pull-to-refresh list of responses shared by `CommentsList` and `RepliesList`: the header, then
 * the loading, failure or empty state, or the items followed by a load-more loader. It also wires
 * the lifecycle, scroll-to and scrolled-to-bottom effects.
 *
 * What differs between the two lists stays with the caller: the ViewModel, the loader and failure
 * paddings, and [itemContent], which renders one item with its own modifier and callbacks.
 */
@Composable
internal fun <T : OctopusItem> ResponsesList(
    viewModel: OctopusItemsListViewModel<T>,
    header: @Composable LazyItemScope.() -> Unit,
    placeHolder: @Composable LazyItemScope.() -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    failureVerticalPadding: Dp,
    modifier: Modifier,
    bottomPadding: Dp,
    composerRequest: Int = 0,
    isReply: Boolean = false,
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var retainedTarget by rememberSaveable(stateSaver = responseScrollTargetSaver) { mutableStateOf<ScrollTo?>(null) }
    var positioning by remember { mutableStateOf(false) }
    var requestVersion by remember { mutableIntStateOf(0) }
    val alignedPosition = remember { ResponseListPosition() }
    val pageLoads by viewModel.pageLoads.collectAsStateWithLifecycle()
    // Read in the effect that follows them, never here: the IME inset changes on every frame of the
    // keyboard animation, and reading it here would recompose the whole list on each one.
    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    val userScrollConnection =
        remember(viewModel) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source == NestedScrollSource.UserInput && available.y != 0f) {
                        retainedTarget = null
                        positioning = false
                        viewModel.resetScrollTo()
                    }
                    return Offset.Zero
                }
            }
        }
    LaunchedEffect(composerRequest) {
        if (composerRequest > 0) viewModel.onComposerOpened()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onPause() }

    OctopusPullToRefresh(
        modifier = modifier,
        refreshing = (isRefreshing || state.isRefreshing) && state.items.isNotEmpty(),
        // Off until the list reaches the start of the thread: above that, the loader of older
        // items acts, and a refresh would drop the older items the user scrolled up to.
        enabled = state.canPullToRefresh && !state.isLoading,
        onRefresh = {
            onRefresh()
            viewModel.refresh()
        },
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .nestedScroll(userScrollConnection),
            state = listState,
            contentPadding = PaddingValues(bottom = bottomPadding + 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = HEADER_ITEM_KEY, content = header)

            if (state.isLoading || (state.isRefreshing && state.items.isEmpty())) {
                item(key = "responses_initial_loading") { ResponseListLoading(isReply) }
            } else if (state.hasLoadFailure) {
                octopusScreenStateFailureItem(
                    isNoNetwork = state.loadFailureIsNoNetwork,
                    verticalPadding = failureVerticalPadding,
                    onRetry = { viewModel.refresh() },
                )
            } else if (state.items.isEmpty()) {
                item(content = placeHolder)
            } else {
                if (state.hasOlderItems) {
                    item(key = OLDER_LOADER_KEY) {
                        ResponsePageLoading(
                            state.olderPageFailed,
                            viewModel::onLoadOlderVisible,
                            showProgress =
                                !isRefreshing && !state.isRefreshing,
                        )
                    }
                }
                items(
                    items = state.items,
                    key = { it.id },
                    itemContent = itemContent,
                )
                if (state.hasNewerItems) {
                    item(key = NEWER_LOADER_KEY) {
                        ResponsePageLoading(
                            state.newerPageFailed,
                            viewModel::onLoadNewerVisible,
                            showProgress =
                                !isRefreshing && !state.isRefreshing,
                        )
                    }
                }
                if (state.hasMoreItems) {
                    item(key = "responses_list_more_loader") {
                        ResponsePageLoading(
                            state.morePageFailed,
                            viewModel::onLoadMoreVisible,
                            showProgress =
                                !isRefreshing && !state.isRefreshing,
                        )
                    }
                }
            }
        }
    }

    // Older items are inserted above the ones on screen. The list keeps its first visible item in
    // place on its own, by key, unless that item is the header or the loader above the items: the
    // view then stays at the top and the items on screen are pushed down. Here the previous first
    // item is put back where it was, during the same composition, so nothing jumps.
    val firstItemId = state.items.firstOrNull()?.id
    val previousFirstItemId = remember { arrayOfNulls<String>(1) }
    val previousId = previousFirstItemId[0]
    if (firstItemId != previousId) {
        previousFirstItemId[0] = firstItemId
        val newIndex = previousId?.let { id -> state.items.indexOfFirst { it.id == id } } ?: -1
        if (newIndex > 0 && listState.firstVisibleItemIndex <= 1) {
            val offset =
                listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.key == previousId }
                    ?.offset ?: 0
            listState.requestScrollToItem(
                index = itemsStartIndex(state.hasOlderItems) + newIndex,
                scrollOffset = -offset,
            )
        }
    }

    val itemIds = remember(state.items) { state.items.map { it.id } }
    val targetIndex = responseScrollIndex(retainedTarget, itemIds, state.hasOlderItems)
    val targetId =
        when (val target = retainedTarget) {
            is ScrollTo.Item -> target.id
            ScrollTo.Latest -> itemIds.lastOrNull()
            null -> null
        }
    // A scroll still to land holds paging back, but only one whose target is in the list: a target
    // the list has not loaded (a latest item behind a page all moderated, a reply the first load
    // failed to reach) only comes with the pages that it would otherwise hold back forever.
    val scrollPending = positioning && targetIndex != null

    // Load older items as the user scrolls up near the top. Not while a scroll is pending: the
    // list opens at the top, before it scrolls down to the latest item.
    val canLoadOlder =
        state.hasOlderItems &&
            !state.isLoading &&
            !state.isRefreshing &&
            !state.olderPageFailed &&
            state.scrollTo == null &&
            !scrollPending
    // Keyed on the page count too: a page whose items are all filtered out leaves the size as it is.
    LaunchedEffect(listState, canLoadOlder, state.items.size, pageLoads) {
        if (!canLoadOlder) return@LaunchedEffect
        // Read the position once the list has laid out the new items, and the anchor above has
        // moved it down: read before, it is still at the top and would load the next page at once.
        withFrameNanos { }
        snapshotFlow { listState.firstVisibleItemIndex <= LOAD_OLDER_THRESHOLD }
            .distinctUntilChanged()
            .collect { isNearTop -> if (isNearTop) viewModel.onLoadOlderVisible() }
    }

    // Load newer items as the user scrolls down near the bottom, when the list opened on an item
    // away from the latest. They are added below the items on screen, which stay in place.
    val canLoadNewer =
        (state.hasNewerItems || state.hasMoreItems) &&
            !state.isLoading &&
            !state.isRefreshing &&
            !state.newerPageFailed &&
            !state.morePageFailed &&
            state.scrollTo == null &&
            !scrollPending
    LaunchedEffect(listState, canLoadNewer, state.items.size, pageLoads) {
        if (!canLoadNewer) return@LaunchedEffect
        withFrameNanos { }
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
            lastVisibleIndex >= layoutInfo.totalItemsCount - 1 - LOAD_NEWER_THRESHOLD
        }.distinctUntilChanged()
            .collect { isNearBottom ->
                if (isNearBottom) {
                    if (state.hasNewerItems) viewModel.onLoadNewerVisible() else viewModel.onLoadMoreVisible()
                }
            }
    }

    LaunchedEffect(state.scrollTo, state.isLoading) {
        if (!state.isLoading && state.scrollTo != null) {
            retainedTarget = state.scrollTo
            positioning = true
            requestVersion++
            viewModel.resetScrollTo()
        }
    }

    // An item the list loaded without (deleted, or moderated since) is no longer a target: kept, it
    // would pull the list back the day it shows up again, wherever the user is. Not while a new
    // request is pending: a window replaced without the target can ask for the latest item in the
    // same update, and that request, retained by the effect above, must survive this one. Nor
    // after a failed load, which loaded nothing: a retry that issues no new request lands on it.
    val goneTarget =
        (retainedTarget as? ScrollTo.Item)?.takeIf {
            targetIndex == null && !state.isLoading && state.scrollTo == null && !state.hasLoadFailure
        }
    LaunchedEffect(goneTarget) {
        if (goneTarget != null && retainedTarget == goneTarget) retainedTarget = null
    }
    val currentItemIds by rememberUpdatedState(itemIds)
    // Follow the target index and layout/inset changes, including the final IME frame and composer
    // expansion. Appends after an Item target leave its position alone. A drag releases the retained
    // target, as does an accessibility scroll off screen. Removing the recorded first visible
    // response invalidates the position comparison and needs realignment instead.
    LaunchedEffect(targetIndex, targetId, requestVersion, bottomPadding) {
        // No reset of `positioning` here: this run can be the stale one of a window replaced in the
        // same update as a new request, and would mark that request's first scroll as already done.
        if (targetIndex == null) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            Triple(
                layoutInfo.viewportSize.height,
                imeInsets.getBottom(density),
                layoutInfo.visibleItemsInfo.firstOrNull { it.key == targetId }?.size ?: 0,
            )
        }.distinctUntilChanged()
            .collectLatest {
                withFrameNanos { }
                // A scroll that is not the user's drag (an accessibility action) and took the target
                // off screen releases it too, rather than having the next layout change snap it back.
                if (!positioning && alignedPosition.hasMoved(listState, currentItemIds) && !listState.isVisible(targetId)) {
                    retainedTarget = null
                    return@collectLatest
                }
                try {
                    if (!listState.scrollResponseIntoView(targetIndex, animate = positioning)) retainedTarget = null
                    positioning = false
                } finally {
                    alignedPosition.record(listState, currentItemIds)
                }
            }
    }

    // Leaving the screen cancels an animated scroll before it clears its request: clear it here,
    // or it replays over the restored position on return. A configuration change keeps it, so the
    // recreated list resumes the scroll. Not in a `finally`, which would also run when a
    // new target supersedes the running scroll, and drop that new target.
    val activity = LocalActivity.current
    DisposableEffect(viewModel) {
        onDispose { if (activity?.isChangingConfigurations != true) viewModel.resetScrollTo() }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.canScrollForward }
            .distinctUntilChanged()
            .collect { canScrollForward ->
                viewModel.onScrolledToBottomChanged(!canScrollForward)
            }
    }
}

/**
 * Where the list was left by its last alignment on the target: the item IDs, its first visible item
 * and that item's offset. LazyList keeps surviving responses in place by key when the list changes.
 * Only removing the recorded first visible response invalidates the position comparison; header and
 * loader keys never count as removed responses.
 * Not a state: written and read in the effect only.
 */
private class ResponseListPosition {
    private var itemIds: List<String> = emptyList()
    private var firstKey: Any? = null
    private var firstOffset = 0

    fun record(
        listState: LazyListState,
        itemIds: List<String>,
    ) {
        this.itemIds = itemIds
        val first = listState.layoutInfo.visibleItemsInfo.firstOrNull()
        firstKey = first?.key
        firstOffset = first?.offset ?: 0
    }

    /** Whether a scroll moved the list since [record]; never before a first alignment. */
    fun hasMoved(
        listState: LazyListState,
        itemIds: List<String>,
    ): Boolean {
        val key = firstKey ?: return false
        if (key in this.itemIds && key !in itemIds) return false
        val first = listState.layoutInfo.visibleItemsInfo.firstOrNull()
        return first?.key != key || first.offset != firstOffset
    }
}

private fun LazyListState.isVisible(key: Any?): Boolean = layoutInfo.visibleItemsInfo.any { it.key == key }

/** Index of the first item in the list: after the header, and the loader above the items if shown. */
private fun itemsStartIndex(hasOlderItems: Boolean): Int = if (hasOlderItems) 2 else 1

private val responseScrollTargetSaver =
    Saver<ScrollTo?, String>(
        save = { target ->
            when (target) {
                ScrollTo.Latest -> "latest"
                is ScrollTo.Item -> "item:${target.id}"
                null -> null
            }
        },
        restore = { if (it == "latest") ScrollTo.Latest else ScrollTo.Item(it.removePrefix("item:")) },
    )
