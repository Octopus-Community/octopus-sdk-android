package com.octopuscommunity.sdk.ui.common.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.data.repository.windowFeeds
import com.octopuscommunity.sdk.domain.model.Feed
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ProfileModified
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.OctopusItem.Feeds
import com.octopuscommunity.sdk.domain.model.Status
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.OctopusItemRepository
import com.octopuscommunity.sdk.domain.repository.TranslationRepository
import com.octopuscommunity.sdk.domain.usecase.SetReactionUseCase
import com.octopuscommunity.sdk.domain.usecase.ToggleTranslationUseCase
import com.octopuscommunity.sdk.ui.common.ErrorChannel
import com.octopuscommunity.sdk.ui.common.ScreenStateFailure
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.errorText
import com.octopuscommunity.sdk.ui.common.loadFailureChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

sealed interface ScrollTo {
    data class Item(
        val id: String,
    ) : ScrollTo

    data object Latest : ScrollTo
}

@OptIn(FlowPreview::class)
abstract class OctopusItemsListViewModel<T : OctopusItem>(
    commonParameters: CommonParameters,
    private val itemRepository: OctopusItemRepository<T, *, *>,
    translationRepository: TranslationRepository,
    toggleTranslation: ToggleTranslationUseCase,
    setReaction: SetReactionUseCase,
    val feeds: Feeds,
    val scrollToLatest: Boolean = false,
    val scrollToId: String? = null,
    private val pagesSize: Int?,
    /**
     * Whether a list opened on [scrollToId] asks the server for the page around it (internal ticket)
     * rather than walking the ascending feed up to it. Only for a feed the server serves anchors on.
     */
    private val opensOnItem: Boolean = true,
    /**
     * Where the window of a list opened on an item is deleted from the cache once the list is
     * cleared, which `viewModelScope` no longer runs by then.
     */
    private val windowCleanupScope: CoroutineScope = commonParameters.containerScope ?: defaultWindowCleanupScope,
) : OctopusItemViewModel<T>(
        commonParameters,
        translationRepository,
        toggleTranslation,
        setReaction,
    ) {
    private val windowFeedRepository = itemRepository.windowFeeds()

    @Immutable
    data class UiState<T : OctopusItem>(
        val ascendingItems: List<T> = emptyList(),
        val descendingItems: List<T> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val hasMoreItems: Boolean = true,
        val scrollTo: ScrollTo? = null,
        @Deprecated(
            "Dead since the screen states landed: never read, and no longer written. " +
                "Use hasLoadFailure / loadFailureIsNoNetwork for a failed first load, and the error " +
                "snackbar for a failure over content already on screen. Removed in the next major.",
            level = DeprecationLevel.WARNING,
        )
        val error: UiText? = null,
        /**
         * Set when the first load failed with nothing on screen behind it, which is what the inline
         * screen state renders. A failure over a list already displayed goes to the snackbar.
         */
        val hasLoadFailure: Boolean = false,
        val loadFailureIsNoNetwork: Boolean = false,
        /**
         * Set when the list opened on its newest items and older pages are still on the server: the
         * screen shows a loader above the items and loads them as the user scrolls up.
         */
        val hasOlderItems: Boolean = false,
        /**
         * Set when the list opened on an item away from its latest (internal ticket) and newer pages are
         * still on the server: the screen shows a loader below the items and loads them as the user
         * scrolls down.
         */
        val hasNewerItems: Boolean = false,
        /**
         * Set while the list shows a window of the descending feed (internal ticket) rather than walking the
         * ascending one: a refresh then replaces the window with a single page of the server.
         */
        val isNewestFirst: Boolean = false,
        /** A failed page keeps its cursor and an inline retry instead of an endless spinner. */
        val olderPageFailed: Boolean = false,
        /** A failed newer page keeps its cursor and shows an inline retry. */
        val newerPageFailed: Boolean = false,
        /** A failed ascending page or page walk keeps its cursor and shows an inline retry. */
        val morePageFailed: Boolean = false,
    ) {
        /**
         * The newest items come after the ascending ones only once the ascending feed is exhausted:
         * before that, they would sit right after its first pages, past a gap of comments never
         * loaded.
         */
        val items =
            (ascendingItems + if (hasMoreItems) emptyList() else descendingItems.reversed())
                .distinctBy { it.id }
                .filter { it.status !is Status.Moderated }

        /**
         * Whether pulling the top of the list refreshes it: only when a refresh gives back the list
         * on screen. A newest-first refresh replaces the window with one server page, around the
         * item the list opened on while newer pages separate it from the latest item, or else the
         * newest page, capped at [MAX_PAGE_SIZE] items. So the pull waits for the window to reach
         * the start of the thread (the top of the list is the loader of older items until then)
         * and, opened on an item, its latest item too, and stays off for a window grown past one
         * server page: either refresh would drop the older items the user scrolled up to.
         */
        val canPullToRefresh: Boolean
            get() =
                !hasOlderItems &&
                    !hasNewerItems &&
                    !(isNewestFirst && descendingItems.size > MAX_PAGE_SIZE)

        val scrollToIndex: Int? =
            when (scrollTo) {
                is ScrollTo.Item -> items.indexOfFirst { it.id == scrollTo.id }.takeIf { it >= 0 }
                is ScrollTo.Latest -> items.lastIndex.takeIf { it >= 0 }
                else -> null
            }
    }

    /**
     * Newest-first mode (internal ticket): the list reads the descending feed, a page at a time from its
     * newest end, so opening on the latest item never walks the whole thread. It starts there when
     * the list opens on its latest item, and switches to it when a new item must be shown while the
     * ascending feed still has unloaded pages. It also opens on an item (internal ticket): the window is
     * then the page around that item, and grows toward the latest one as the user scrolls down.
     */
    private var isNewestFirst = (scrollToLatest || (scrollToId != null && opensOnItem)) && feeds.descendingId != null
    private var olderPageCursor: String? = null
    private var isLoadingOlder = false
    private var isPolling = false

    /**
     * The item the window opened on, while newer pages still separate the window from the latest
     * item: the window is then not the newest page, so it is neither polled nor refreshed as one.
     */
    private var anchorItemId: String? = scrollToId?.takeIf { isNewestFirst && !scrollToLatest }

    /**
     * Where the list caches the descending feed. Opened on an item, it keeps its window under an id
     * of its own for its whole life, newest pages and polls included (see [WindowFeeds]); otherwise
     * it shares the feed's cache with the other lists of the feed.
     */
    private val descendingStorageId: String? =
        feeds.descendingId?.let { feedId -> if (anchorItemId != null) WindowFeeds.newId(feedId) else feedId }
    private var newerPageCursor: String? = null
    private var isLoadingNewer = false

    /**
     * The older and newer page loads in flight. Replacing the window cancels them first: a page
     * landing after it would be stored next to a window it no longer borders.
     */
    private var olderPageJob: Job? = null
    private var newerPageJob: Job? = null

    /**
     * The list asked for the next older or newer page while one was still loading. The cached feed
     * can deliver a page's items before its load completes: the list sees them, asks for the next
     * page at once and is turned down, then stays on the loader with nothing left to ask again. The
     * request is kept and served once the load in flight is over, or once a window being replaced
     * meanwhile is shown.
     */
    private var isOlderPageRequested = false
    private var isNewerPageRequested = false

    private val _state = MutableStateFlow(UiState<T>(hasMoreItems = !isNewestFirst))
    val state = _state.asStateFlow()

    /**
     * Counts the pages loaded on the list's request. A page whose items are all filtered out
     * (moderated, or already shown) changes nothing else on screen: the list re-checks its position
     * on this count, or it would wait for a scroll before asking for the next page.
     */
    private val _pageLoads = MutableStateFlow(0)
    internal val pageLoads: StateFlow<Int> = _pageLoads.asStateFlow()

    private var nextPageCursor: String? = null
    private var hasMoreItems: Boolean? = null
    private var autoFetchLatest = false
    private var autoFetchLatestJob: Job? = null
    private val itemsToFetch = MutableStateFlow<Set<String>>(setOf())

    private var ascendingCollectionJob: Job? = null
    private var descendingCollectionJob: Job? = null

    // Initial load, refresh, posting and ascending pagination must not replace each other's window.
    private val windowFetchMutex = Mutex()
    private var isFetching = false

    // After logout, cached emissions may contain pages whose window was purged. Only a
    // successful replacement establishes their membership and paging boundaries again.
    private var isWindowInvalidated = false
    private var isLoadingMore = false
    private var isMorePageRequested = false
    private var isScrolledToBottom = false
    private var composerWantsLatest = false

    init {
        viewModelScope.launch {
            itemsToFetch
                .debounce(0.5.seconds)
                .collect { itemIds ->
                    if (itemIds.isNotEmpty()) {
                        itemRepository.fetchCounters(itemIds = itemIds.toList())
                        itemsToFetch.update { it - itemIds }
                    }
                }
        }
        viewModelScope.launch {
            eventRepository.getEventsFlow().collect { event ->
                when (event) {
                    is ProfileModified -> {
                        itemRepository.fetchItems(itemIds = _state.value.items.map { it.id })
                    }

                    else -> {}
                }
            }
        }
        viewModelScope.launch {
            var memberId: String? = null
            var reconnectPending = false
            currentUserProfile.collect { profile ->
                // Unlike userContentCleared, observe null too: logout must invalidate paging
                // before a visibility callback can load into the purged window.
                val memberLeft = memberId != null && profile?.userId != memberId
                if (memberLeft) reconnectPending = true
                val reconnected = profile != null && reconnectPending
                if (memberLeft || reconnected) {
                    // Reset again on reconnect: a replacement may have finished in the gap,
                    // or a fast logout/reconnect may have conflated the null profile.
                    windowFetchMutex.withLock { invalidateWindow() }
                }
                memberId = profile?.takeUnless { it.isGuest }?.userId
                if (reconnected) {
                    reconnectPending = false
                    refresh()
                }
            }
        }
        viewModelScope.launch {
            // The closure deleted the cached items and their feeds, with the user unchanged: the
            // window is purged as after a logout, and loads again once the community reopened.
            communityReopened.collect {
                windowFetchMutex.withLock { invalidateWindow() }
                refresh()
            }
        }

        viewModelScope.launch {
            windowFetchMutex.withLock {
                val anchorId = anchorItemId
                if (anchorId != null) {
                    fetchAnchoredItems(anchorId = anchorId, pageSize = pagesSize, scrollToAnchor = true)
                } else if (isNewestFirst) {
                    fetchNewestItems(pageSize = pagesSize, scrollTo = ScrollTo.Latest)
                } else if (scrollToLatest) {
                    // No descending feed to open from: walk the ascending one to its end.
                    fetchAllItems(scrollTo = ScrollTo.Latest)
                } else if (scrollToId != null) {
                    fetchAllItems(scrollTo = ScrollTo.Item(id = scrollToId))
                } else {
                    fetchPaginatedItems(pageCursor = null)
                }

                _state.update { it.copy(isLoading = false) }
                if (composerWantsLatest) {
                    composerWantsLatest = false
                    onComposerOpened()
                }
            }
        }

        startCollectingItems(feeds)
    }

    fun refresh() {
        if (_state.value.isRefreshing || _state.value.isLoading) return
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            windowFetchMutex.withLock {
                val anchorId = anchorItemId
                if (anchorId != null) {
                    fetchAnchoredItems(
                        anchorId = anchorId,
                        pageSize = windowPageSize(),
                        // A retry after a failed first load still has to land on the item.
                        scrollToAnchor = _state.value.items.isEmpty(),
                    )
                } else if (isNewestFirst) {
                    fetchNewestItems(
                        pageSize = windowPageSize(),
                        // A retry after a failed first load still has to land on the latest item.
                        scrollTo = if (_state.value.items.isEmpty()) ScrollTo.Latest else null,
                    )
                } else {
                    fetchPaginatedItems(pageCursor = null)
                }
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /** Composing follows the latest item, fetching only if the displayed window stops short of it. */
    internal fun onComposerOpened() {
        if (_state.value.isLoading) {
            composerWantsLatest = true
            return
        }
        if (_state.value.scrollTo == ScrollTo.Latest) return
        if (!_state.value.isLoading && !_state.value.hasMoreItems && !_state.value.hasNewerItems) {
            _state.update { it.copy(scrollTo = ScrollTo.Latest) }
        } else if (!_state.value.isLoading) {
            scrollToLatestItem()
        }
    }

    fun scrollToLatestItem() {
        viewModelScope.launch {
            windowFetchMutex.withLock {
                when {
                    isNewestFirst -> fetchNewestItems(pageSize = windowPageSize(), scrollTo = ScrollTo.Latest)

                    // The latest item is past ascending pages never loaded: rather than walking them all,
                    // reopen the list on its newest page.
                    hasMoreItems == true && feeds.descendingId != null -> {
                        isNewestFirst = true
                        ascendingCollectionJob?.cancel()
                        ascendingCollectionJob = null
                        fetchNewestItems(pageSize = pagesSize, scrollTo = ScrollTo.Latest)
                    }

                    else ->
                        fetchAllItems(
                            pageCursor = nextPageCursor,
                            pagesSize = if (hasMoreItems == true) MAX_PAGE_SIZE else 5,
                            scrollTo = ScrollTo.Latest,
                        )
                }
            }
        }
    }

    fun resetScrollTo() {
        _state.update { it.copy(scrollTo = null) }
    }

    fun onResume() {
        if (autoFetchLatest) {
            startAutoFetchLatest()
        }
    }

    fun onPause() {
        stopAutoFetchLatest()
    }

    override fun onItemAppeared(item: T) {
        super.onItemAppeared(item)
        itemsToFetch.update { it + item.id }
    }

    override fun onItemDisappeared(item: T) {
        super.onItemDisappeared(item)
        itemsToFetch.update { it - item.id }
    }

    fun onLoadMoreVisible() {
        // Claim the load before launching: claiming it inside the mutex queues every callback
        // behind the current fetch, turning one burst of visibility events into a full feed walk.
        if (isLoadingMore) {
            isMorePageRequested = true
            return
        }
        isLoadingMore = true
        viewModelScope.launch {
            try {
                windowFetchMutex.withLock {
                    if (hasMoreItems == true && !isFetching) {
                        _state.update { it.copy(morePageFailed = false) }
                        fetchPaginatedItems(pageCursor = nextPageCursor)
                        _pageLoads.update { it + 1 }
                    }
                }
            } finally {
                isLoadingMore = false
            }
            val loadPendingPage = isMorePageRequested && !_state.value.morePageFailed
            isMorePageRequested = false
            if (loadPendingPage) onLoadMoreVisible()
        }
    }

    /**
     * Loads the page older than the oldest item on screen, in newest-first mode. Ignored while the
     * list is still loading or has a scroll pending: the loader above the items is on screen before
     * the list has scrolled down to the latest item, and must not load a page then.
     */
    fun onLoadOlderVisible() {
        val feedId = feeds.descendingId ?: return
        val storageFeedId = descendingStorageId ?: return
        val cursor = olderPageCursor ?: return
        if (!isNewestFirst) return
        if (isLoadingOlder || isPolling || isFetching) {
            isOlderPageRequested = true
            return
        }
        if (_state.value.isLoading || _state.value.scrollTo != null) return
        isLoadingOlder = true
        _state.update { it.copy(olderPageFailed = false) }
        isOlderPageRequested = false
        olderPageJob =
            viewModelScope.launch {
                try {
                    loadOlderPage(feedId = feedId, storageFeedId = storageFeedId, cursor = cursor)
                } finally {
                    isLoadingOlder = false
                }
                servePendingOlderPage()
                // A page the list asked for meanwhile is now loading, and counts once it lands.
                if (!isLoadingOlder) _pageLoads.update { it + 1 }
            }
    }

    private suspend fun loadOlderPage(
        feedId: String,
        storageFeedId: String,
        cursor: String,
    ) {
        val response =
            fetchDescendingPage(
                feedId = feedId,
                storageFeedId = storageFeedId,
                pageSize = pagesSize,
                pageCursor = cursor,
            )
        // Cancelled by a window replacement: the result belongs to the window that was replaced.
        currentCoroutineContext().ensureActive()
        when (response) {
            is OctopusResult.Success -> {
                olderPageCursor = response.data.nextPageCursor
                _state.update {
                    it.copy(
                        descendingItems = response.data.items.first(),
                        hasOlderItems = olderPageCursor != null,
                        hasLoadFailure = false,
                    )
                }
            }

            // Keep the cursor and the window; the inline retry asks for this page again.
            is OctopusResult.Failure -> {
                isOlderPageRequested = false
                _state.update { it.copy(olderPageFailed = true) }
            }
        }
    }

    /**
     * Loads the page newer than the newest item on screen, when the list opened on an item away
     * from its latest (internal ticket). Ignored while the list is still loading or has a scroll pending,
     * like [onLoadOlderVisible].
     */
    fun onLoadNewerVisible() {
        val feedId = feeds.descendingId ?: return
        val storageFeedId = descendingStorageId ?: return
        val cursor = newerPageCursor ?: return
        if (!isNewestFirst) return
        if (isLoadingNewer || isPolling || isFetching) {
            isNewerPageRequested = true
            return
        }
        if (_state.value.isLoading || _state.value.scrollTo != null) return
        isLoadingNewer = true
        _state.update { it.copy(newerPageFailed = false) }
        isNewerPageRequested = false
        newerPageJob =
            viewModelScope.launch {
                try {
                    loadNewerPage(feedId = feedId, storageFeedId = storageFeedId, cursor = cursor)
                } finally {
                    isLoadingNewer = false
                }
                servePendingNewerPage()
                if (!isLoadingNewer) _pageLoads.update { it + 1 }
            }
    }

    /** Loads the older page the list asked for while a load was in flight, see [isOlderPageRequested]. */
    private fun servePendingOlderPage() {
        if (!isOlderPageRequested) return
        isOlderPageRequested = false
        onLoadOlderVisible()
    }

    /** Loads the newer page the list asked for while a load was in flight, see [isNewerPageRequested]. */
    private fun servePendingNewerPage() {
        if (!isNewerPageRequested) return
        isNewerPageRequested = false
        onLoadNewerVisible()
    }

    private suspend fun loadNewerPage(
        feedId: String,
        storageFeedId: String,
        cursor: String,
    ) {
        val response =
            windowFeedRepository.fetchPreviousItems(
                feedId = feedId,
                pageCursor = cursor,
                pageSize = pagesSize,
                storageFeedId = storageFeedId,
            )
        // Cancelled by a window replacement: the result belongs to the window that was replaced.
        currentCoroutineContext().ensureActive()
        when (response) {
            is OctopusResult.Success -> {
                newerPageCursor = response.data.nextPageCursor
                // The window now reaches the latest item: it is the newest page again.
                if (newerPageCursor == null) anchorItemId = null
                _state.update {
                    it.copy(
                        descendingItems = response.data.items.first(),
                        hasNewerItems = newerPageCursor != null,
                        hasLoadFailure = false,
                    )
                }
            }

            // Keep the cursor and the anchored window; an explicit retry resumes this page.
            is OctopusResult.Failure -> {
                isNewerPageRequested = false
                _state.update { it.copy(newerPageFailed = true) }
            }
        }
    }

    /**
     * Cancels the older and newer page loads, and waits for them to end, before a window
     * replacement. A page the list was loading stays asked for, and is loaded from the new window
     * once shown: a refresh replaces the window with one of the same size, and the list, still where
     * it was, has no reason to ask again.
     */
    private suspend fun cancelWindowPaging() {
        if (olderPageJob?.isActive == true) isOlderPageRequested = true
        if (newerPageJob?.isActive == true) isNewerPageRequested = true
        olderPageJob?.cancelAndJoin()
        newerPageJob?.cancelAndJoin()
        olderPageJob = null
        newerPageJob = null
    }

    /** Invalidates both paging directions and their cached presentation after a session purge. */
    private suspend fun invalidateWindow() {
        cancelWindowPaging()
        dropPendingPages()
        olderPageCursor = null
        newerPageCursor = null
        nextPageCursor = null
        hasMoreItems = false
        isMorePageRequested = false
        isWindowInvalidated = true
        _state.update {
            it.copy(
                ascendingItems = emptyList(),
                descendingItems = emptyList(),
                hasMoreItems = false,
                hasOlderItems = false,
                hasNewerItems = false,
                olderPageFailed = false,
                newerPageFailed = false,
                morePageFailed = false,
                scrollTo = null,
            )
        }
    }

    /**
     * Fetches a page of the descending feed [feedId] into the cache [storageFeedId]. A list sharing
     * the feed's cache asks for it the way it did before windows were stored apart, which a
     * repository that predates [storageFeedId] (or delegates to one that does) still serves.
     */
    private suspend fun fetchDescendingPage(
        feedId: String,
        storageFeedId: String,
        pageSize: Int?,
        pageCursor: String?,
    ): OctopusResult<Feed<T>, Nothing> =
        when (storageFeedId) {
            feedId -> itemRepository.fetchItems(feedId = feedId, pageSize = pageSize, pageCursor = pageCursor)
            else ->
                windowFeedRepository.fetchItems(
                    feedId = feedId,
                    storageFeedId = storageFeedId,
                    pageSize = pageSize,
                    pageCursor = pageCursor,
                )
        }

    /** Drops the page requests kept for a window that will not be shown. */
    private fun dropPendingPages() {
        isOlderPageRequested = false
        isNewerPageRequested = false
    }

    fun onScrolledToBottomChanged(isScrolledToBottom: Boolean) {
        this.isScrolledToBottom = isScrolledToBottom
        viewModelScope.launch {
            if (isScrolledToBottom) {
                if (!autoFetchLatest) {
                    autoFetchLatest = true
                    startAutoFetchLatest()
                }
            } else {
                autoFetchLatest = false
                stopAutoFetchLatest()
            }
        }
    }

    private suspend fun fetchAllItems(
        pageCursor: String? = null,
        pagesSize: Int? = 100,
        scrollTo: ScrollTo?,
    ) {
        val feedId = feeds.ascendingId ?: return
        val previousPageCursor = nextPageCursor
        val previousHasMoreItems = hasMoreItems
        val previouslyHadMoreItems = _state.value.hasMoreItems
        isFetching = true

        // Only a ScrollTo.Item gives the loop something to look for; ScrollTo.Latest is by
        // definition on the last page, so it keeps walking the whole feed.
        val targetItemId = (scrollTo as? ScrollTo.Item)?.id
        var currentPageCursor = pageCursor
        var latestSuccessfulResponse: OctopusResult.Success<Feed<T>>?
        var hasReachedTarget = false

        do {
            when (
                val response =
                    itemRepository.fetchItems(
                        feedId = feedId,
                        pageSize = pagesSize,
                        pageCursor = currentPageCursor,
                    )
            ) {
                is OctopusResult.Success -> {
                    latestSuccessfulResponse = response
                    currentPageCursor = response.data.nextPageCursor
                    if (currentPageCursor?.isNotEmpty() == true) {
                        nextPageCursor = currentPageCursor
                    }
                    // Stop as soon as the item we are scrolling to is loaded: the pages beyond it
                    // are not needed to place the scroll, and onLoadMoreVisible() fetches them if
                    // the user keeps scrolling. nextPageCursor and hasMoreItems are left as the
                    // last page reported them, so paging resumes exactly where this loop stopped.
                    hasReachedTarget =
                        targetItemId != null &&
                        response.data.items
                            .first()
                            .any { item -> item.id == targetItemId }
                }

                is OctopusResult.Failure -> {
                    if (_state.value.items.isNotEmpty()) {
                        // A partial walk has not published its full window: retry from where it began.
                        nextPageCursor = previousPageCursor
                        hasMoreItems = previousHasMoreItems
                        _state.update { it.copy(hasMoreItems = previouslyHadMoreItems, morePageFailed = true) }
                        isFetching = false
                    } else {
                        handleFetchFailure(response, scrollTo)
                    }
                    return
                }
            }
        } while (currentPageCursor != null && !hasReachedTarget)

        handleFetchSuccess(
            feed = latestSuccessfulResponse.data,
            scrollTo = scrollTo,
        )
    }

    /**
     * Replaces the newest-first window with the newest page of the descending feed. The cached
     * ascending items are dropped: the list now reads the descending feed only.
     */
    private suspend fun fetchNewestItems(
        pageSize: Int?,
        scrollTo: ScrollTo?,
    ) {
        val feedId = feeds.descendingId ?: return
        val storageFeedId = descendingStorageId ?: return
        isFetching = true
        cancelWindowPaging()
        when (
            val response =
                fetchDescendingPage(
                    feedId = feedId,
                    storageFeedId = storageFeedId,
                    pageSize = pageSize,
                    pageCursor = null,
                )
        ) {
            is OctopusResult.Success ->
                showWindow(
                    items = response.data.items.first(),
                    olderCursor = response.data.nextPageCursor,
                    newerCursor = null,
                    scrollTo = scrollTo,
                )

            is OctopusResult.Failure -> handleWindowFailure(response, scrollTo)
        }
    }

    /**
     * Replaces the newest-first window with the page of the descending feed around [anchorId]
     * (internal ticket), [scrollToAnchor] placing the list on it. A server that cannot open the feed on
     * that item (deleted, or a server that predates anchors) answers with the newest page instead,
     * and one refusing anchors on the feed answers with an error: a list opening on the item then
     * walks the ascending feed up to it, as before anchors. Without network, the error stays.
     */
    private suspend fun fetchAnchoredItems(
        anchorId: String,
        pageSize: Int?,
        scrollToAnchor: Boolean,
    ) {
        val feedId = feeds.descendingId ?: return
        val storageFeedId = descendingStorageId ?: return
        isFetching = true
        cancelWindowPaging()
        when (
            val response =
                windowFeedRepository.fetchItemsAround(
                    feedId = feedId,
                    anchorItemId = anchorId,
                    pageSize = pageSize,
                    storageFeedId = storageFeedId,
                )
        ) {
            is OctopusResult.Success -> {
                val anchored = response.data
                if (!anchored.isAnchored && scrollToAnchor && feeds.ascendingId != null) {
                    walkAscendingFeedTo(itemId = anchorId)
                    return
                }
                showWindow(
                    items = anchored.feed.items.first(),
                    olderCursor = anchored.feed.nextPageCursor,
                    newerCursor = anchored.previousPageCursor,
                    scrollTo =
                        when {
                            !scrollToAnchor -> null
                            anchored.isAnchored -> ScrollTo.Item(id = anchorId)
                            else -> ScrollTo.Latest
                        },
                )
            }

            is OctopusResult.Failure ->
                if (response != OctopusResult.Failure.NoNetwork && scrollToAnchor && feeds.ascendingId != null) {
                    // A server refusing the anchor on this feed must not leave the list on an error.
                    walkAscendingFeedTo(itemId = anchorId)
                } else {
                    handleWindowFailure(
                        response,
                        scrollTo = if (scrollToAnchor) ScrollTo.Item(id = anchorId) else null,
                    )
                }
        }
    }

    /**
     * Leaves newest-first mode for the ascending feed, walked a page at a time up to [itemId]: the
     * way the list opened on an item before anchors, kept for a server that cannot serve one.
     */
    private suspend fun walkAscendingFeedTo(itemId: String) {
        isNewestFirst = false
        anchorItemId = null
        olderPageCursor = null
        newerPageCursor = null
        dropPendingPages()
        _state.update {
            it.copy(
                descendingItems = emptyList(),
                hasMoreItems = true,
                hasOlderItems = false,
                hasNewerItems = false,
                isNewestFirst = false,
            )
        }
        startCollectingAscendingItems()
        fetchAllItems(scrollTo = ScrollTo.Item(id = itemId))
    }

    /**
     * Shows [items], a page of the descending feed, as the whole newest-first window, with the
     * cursors to the pages older and newer than it. Without a newer cursor, the window reaches the
     * latest item: it is the newest page, and no longer anchored.
     */
    private fun showWindow(
        items: List<T>,
        olderCursor: String?,
        newerCursor: String?,
        scrollTo: ScrollTo?,
    ) {
        isWindowInvalidated = false
        hasMoreItems = false
        olderPageCursor = olderCursor
        newerPageCursor = newerCursor
        if (newerCursor == null) anchorItemId = null
        _state.update {
            it.copy(
                ascendingItems = emptyList(),
                descendingItems = items,
                hasMoreItems = false,
                hasOlderItems = olderCursor != null,
                hasNewerItems = newerCursor != null,
                isNewestFirst = true,
                scrollTo = shownScrollTarget(items, scrollTo),
                hasLoadFailure = false,
                olderPageFailed = false,
                newerPageFailed = false,
                morePageFailed = false,
            )
        }
        isFetching = false
        // The pages the list asked for while the window was being replaced, now from the new one.
        servePendingOlderPage()
        servePendingNewerPage()
    }

    /**
     * [scrollTo], unless it targets an item of [items] the list does not show (moderated since the
     * link to it was shared): the shown item closest to it then. A target the list does not show is
     * never scrolled to, and the pending scroll would block paging in both directions.
     */
    private fun shownScrollTarget(
        items: List<T>,
        scrollTo: ScrollTo?,
    ): ScrollTo? {
        if (scrollTo !is ScrollTo.Item) return scrollTo
        val index = items.indexOfFirst { it.id == scrollTo.id }
        if (index < 0) return ScrollTo.Latest
        if (items[index].status !is Status.Moderated) return scrollTo
        return items.indices
            .filter { items[it].status !is Status.Moderated }
            .minByOrNull { abs(it - index) }
            ?.let { ScrollTo.Item(id = items[it].id) }
    }

    private fun handleWindowFailure(
        result: OctopusResult.Failure<*>,
        scrollTo: ScrollTo?,
    ) {
        // A failed replacement leaves the cached membership intact. Its cursors still border it,
        // even when every item is filtered out or deleted locally. A failed first load already
        // has null cursors: an empty items flow is not evidence that no window was loaded.
        dropPendingPages()
        handleFetchFailure(result, scrollTo)
    }

    /**
     * Size of a newest page that keeps the whole newest-first window: fetching the newest page
     * replaces the window, so a smaller one would drop the older items the user scrolled up to.
     * Capped by the server's page limit. This bounds the replacement request, not the accumulated
     * pages: scrolling can grow the cached window beyond that limit.
     */
    private fun windowPageSize(): Int =
        _state.value.descendingItems.size
            .coerceIn(pagesSize ?: POLL_PAGE_SIZE, MAX_PAGE_SIZE)

    private suspend fun fetchPaginatedItems(pageCursor: String?) {
        val feedId = feeds.ascendingId ?: return
        isFetching = true
        when (
            val response =
                itemRepository.fetchItems(
                    feedId = feedId,
                    pageSize = pagesSize,
                    pageCursor = pageCursor,
                )
        ) {
            is OctopusResult.Success -> handleFetchSuccess(response.data)
            is OctopusResult.Failure -> {
                if (pageCursor != null && _state.value.items.isNotEmpty()) {
                    // This page failed, not the feed: keep both the cursor and the unloaded gap.
                    _state.update { it.copy(morePageFailed = true) }
                    isFetching = false
                } else {
                    handleFetchFailure(response)
                }
            }
        }
    }

    private suspend fun handleFetchSuccess(
        feed: Feed<T>,
        scrollTo: ScrollTo? = null,
    ) {
        // An ascending page that lands after the list switched to newest-first is stale.
        if (isNewestFirst) {
            isFetching = false
            return
        }
        hasMoreItems = feed.hasMoreItems
        if (feed.nextPageCursor?.isNotEmpty() == true) {
            nextPageCursor = feed.nextPageCursor
        }

        val items = feed.items.first()
        isWindowInvalidated = false
        _state.update {
            it.copy(
                ascendingItems = items,
                hasMoreItems = feed.hasMoreItems,
                scrollTo = shownScrollTarget(items, scrollTo),
                hasLoadFailure = false,
                olderPageFailed = false,
                newerPageFailed = false,
                morePageFailed = false,
            )
        }
        isFetching = false
    }

    /** @param onRetry the snackbar's retry: the whole list again, unless a single page failed. */
    private fun handleFetchFailure(
        result: OctopusResult.Failure<*>,
        scrollTo: ScrollTo? = null,
        onRetry: () -> Unit = { refresh() },
    ) {
        val hasVisibleContent = _state.value.items.isNotEmpty()
        // Ascending cursors cannot recover a failed switch to a newest-first window.
        val keepAscendingPage = hasVisibleContent && !isNewestFirst
        if (!keepAscendingPage) {
            hasMoreItems = false
            nextPageCursor = null
        }

        // One error, one channel (see loadFailureChannel): the inline error and the snackbar/alert
        // used to fire together, so the user dismissed one to read the same sentence in the other.
        val channel = loadFailureChannel(hasVisibleContent = hasVisibleContent)

        _state.update { state ->
            state.copy(
                hasMoreItems = if (keepAscendingPage) state.hasMoreItems else false,
                scrollTo = scrollTo,
                hasLoadFailure = channel == ErrorChannel.Inline,
                loadFailureIsNoNetwork = ScreenStateFailure.from(result) == ScreenStateFailure.NoNetwork,
            )
        }
        isFetching = false

        // Snackbar, not showError: a load failure never escalates to the modal alert — that one is
        // reserved for actions the user explicitly triggered.
        if (channel == ErrorChannel.Transient) {
            showErrorSnackbar(
                message = result.errorText,
                isNoNetwork = ScreenStateFailure.from(result) == ScreenStateFailure.NoNetwork,
                onRetry = onRetry,
            )
        }
    }

    private fun startAutoFetchLatest() {
        val feedId = feeds.descendingId ?: return
        val storageFeedId = descendingStorageId ?: return
        if (autoFetchLatestJob != null) return

        autoFetchLatestJob =
            viewModelScope.launch {
                // Follow the conversation only when a poll finds a latest item the previous poll
                // had not seen. Requesting the scroll on every poll re-ran it every 5 s while the
                // user sat at the bottom (the list clears the request once it has scrolled):
                // harmless for a short last item, but a last item taller than the viewport (often
                // the case with the keyboard open) has its top above the screen there, so the list
                // jumped back up to it and kept auto-scrolling and twitching. The comparison is
                // against the previous poll, not the current state, because the local items flow
                // may already have delivered the new item to the state before the poll returns.
                var latestSeenId =
                    _state.value.items
                        .lastOrNull()
                        ?.id
                while (isActive) {
                    delay(5.seconds)
                    if (isWindowInvalidated || isFetching || windowFetchMutex.isLocked || isLoadingOlder || hasMoreItems == true) continue
                    // A window opened on an item away from the latest is not the newest page: the
                    // poll would replace it with that page, the items on screen with it.
                    if (anchorItemId != null || isLoadingNewer) continue
                    // A window grown past one page by scrolling up would be cut back to its newest
                    // page, under the user's eyes: it is polled again once they are back down.
                    if (isNewestFirst &&
                        !isScrolledToBottom &&
                        _state.value.descendingItems.size > MAX_PAGE_SIZE
                    ) {
                        continue
                    }

                    windowFetchMutex.withLock {
                        // Newest-first, the poll replaces the whole window, so it asks for all of it.
                        val pageSize = if (isNewestFirst) windowPageSize() else POLL_PAGE_SIZE
                        isPolling = true
                        val result =
                            try {
                                fetchDescendingPage(
                                    feedId = feedId,
                                    storageFeedId = storageFeedId,
                                    pageSize = pageSize,
                                    pageCursor = null,
                                )
                            } finally {
                                isPolling = false
                            }
                        when (result) {
                            is OctopusResult.Success -> {
                                val items = result.data.items.first()
                                val previousLatestId = latestSeenId
                                if (isNewestFirst) olderPageCursor = result.data.nextPageCursor
                                _state.update { state ->
                                    val updated =
                                        state.copy(
                                            descendingItems = items,
                                            hasOlderItems = if (isNewestFirst) olderPageCursor != null else state.hasOlderItems,
                                        )
                                    val hasNewLatest = updated.items.lastOrNull()?.id != previousLatestId
                                    updated.copy(
                                        // With nothing new, a pending scroll is left as is rather than
                                        // cleared, which also used to cancel it mid-flight.
                                        scrollTo =
                                            if (isScrolledToBottom && hasNewLatest) {
                                                ScrollTo.Latest
                                            } else {
                                                state.scrollTo
                                            },
                                    )
                                }
                                latestSeenId =
                                    _state.value.items
                                        .lastOrNull()
                                        ?.id
                            }

                            is OctopusResult.Failure -> {}
                        }
                    }
                    servePendingOlderPage()
                }
            }
    }

    private fun stopAutoFetchLatest() {
        autoFetchLatestJob?.cancel()
        autoFetchLatestJob = null
    }

    private fun startCollectingItems(feeds: Feeds) {
        startCollectingAscendingItems()
        descendingCollectionJob =
            descendingStorageId?.let { feedId ->
                viewModelScope.launch {
                    itemRepository.getLocalItemsFlow(feedId).collect { items ->
                        if (!isFetching && !isWindowInvalidated) {
                            _state.update { it.copy(descendingItems = items) }
                        }
                    }
                }
            }
    }

    private fun startCollectingAscendingItems() {
        // Newest-first, the cached ascending feed is not read: it would sit before the window.
        ascendingCollectionJob?.cancel()
        ascendingCollectionJob =
            feeds.ascendingId?.takeUnless { isNewestFirst }?.let { feedId ->
                viewModelScope.launch {
                    itemRepository.getLocalItemsFlow(feedId).collect { items ->
                        if (!isFetching && !isWindowInvalidated) {
                            _state.update { it.copy(ascendingItems = items) }
                        }
                    }
                }
            }
    }

    private fun stopCollectingItems() {
        ascendingCollectionJob?.cancel()
        ascendingCollectionJob = null
        descendingCollectionJob?.cancel()
        descendingCollectionJob = null
    }

    override fun onCleared() {
        stopCollectingItems()
        stopAutoFetchLatest()
        descendingStorageId?.takeIf { it != feeds.descendingId }?.let { windowId ->
            // The database may already be closed (SDK stopped, community switched): a failed
            // purge must not reach the host app, the next home opening purges leftovers anyway.
            windowCleanupScope.launch { runCatching { windowFeedRepository.deleteLocalFeed(windowId) } }
        }
        super.onCleared()
    }

    private companion object {
        /** Outlives the lists, so that a cleared list still deletes its window. */
        val defaultWindowCleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        /** Page size of the latest-items poll outside newest-first mode. */
        const val POLL_PAGE_SIZE = 10

        /** The server rejects a feed page larger than this. */
        const val MAX_PAGE_SIZE = 100
    }
}
