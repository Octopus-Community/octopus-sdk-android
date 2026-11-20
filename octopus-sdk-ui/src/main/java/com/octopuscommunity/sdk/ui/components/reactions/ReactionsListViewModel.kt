package com.octopuscommunity.sdk.ui.components.reactions

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount
import com.octopuscommunity.sdk.domain.model.ProfileReaction
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ReactionsRepository
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.errorText
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backs the "who reacted" sheet ([OctopusReactionsListSheet]): one tab per reaction kind plus an "All"
 * tab, each paging independently through [ReactionsRepository.getReactions].
 */
class ReactionsListViewModel internal constructor(
    commonParameters: CommonParameters,
    private val reactionsRepository: ReactionsRepository,
    private val contentId: String,
    reactionCounts: List<ReactionCount>,
) : OctopusViewModel(commonParameters) {
    /** One tab of the sheet: every reaction, or a single kind. */
    sealed interface Tab {
        data object All : Tab

        data class OfKind(
            val kind: Reaction.Kind,
        ) : Tab

        /** What the backend expects as a filter: `null` for the All tab. */
        val reactionKind: Reaction.Kind?
            get() = (this as? OfKind)?.kind
    }

    /**
     * Paging state of a single tab. Each tab pages independently, so switching back and forth does not
     * restart a list the member already scrolled.
     */
    @Immutable
    data class TabContent(
        val reactions: List<ProfileReaction> = emptyList(),
        /**
         * Cursor of the *next* page. `null` means either "not loaded yet" or "list exhausted" —
         * [hasLoadedOnce] tells the two apart.
         */
        val nextCursor: String? = null,
        /**
         * Cursor consumed by the most recent successful fetch (the `cursor` argument [load] was called
         * with — `null` for a first page). Compared against [nextCursor] to guard a backend that ever
         * repeats the same cursor: the auto-load-more effect in the sheet is keyed on `nextCursor`, but
         * it lives in a `LazyColumn` item that leaves and re-enters composition as [isLoadingMore]
         * toggles, so a fresh mount re-fires it even with an unchanged key. Without this guard, a
         * repeated cursor would re-fetch the same page forever.
         */
        val consumedCursor: String? = null,
        val hasLoadedOnce: Boolean = false,
        val isLoadingFirstPage: Boolean = false,
        val isLoadingMore: Boolean = false,
        val error: UiText? = null,
    ) {
        val canLoadMore get() = hasLoadedOnce && nextCursor != null && nextCursor != consumedCursor && error == null
    }

    @Immutable
    data class UiState(
        val tabs: List<Tab>,
        /**
         * Count per reaction kind, from the counters the content already carries, so the tab bar shows
         * its numbers before any page is fetched. The All tab has no entry: the design shows it without
         * a count.
         */
        val counts: Map<Tab, Int>,
        val selectedTabIndex: Int = 0,
        val contents: Map<Tab, TabContent> = emptyMap(),
    ) {
        val selectedTab: Tab get() = tabs[selectedTabIndex.coerceIn(0, tabs.lastIndex)]

        fun content(tab: Tab): TabContent = contents[tab] ?: TabContent()
    }

    private val _uiState = MutableStateFlow(buildInitialState(reactionCounts))
    val uiState = _uiState.asStateFlow()

    init {
        loadFirstPageIfNeeded(_uiState.value.selectedTab)
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
        loadFirstPageIfNeeded(_uiState.value.selectedTab)
    }

    fun onProfileClick(reaction: ProfileReaction) {
        reaction.author?.let { navigateToProfile(it) }
    }

    /** Loads the first page of a tab, once. */
    fun loadFirstPageIfNeeded(tab: Tab) {
        val content = _uiState.value.content(tab)
        if (content.hasLoadedOnce || content.isLoadingFirstPage) return
        updateContent(tab) { it.copy(isLoadingFirstPage = true, error = null) }
        load(tab = tab, cursor = null)
    }

    fun loadMore(tab: Tab) {
        val content = _uiState.value.content(tab)
        val cursor = content.nextCursor
        if (cursor == null ||
            !content.hasLoadedOnce ||
            content.isLoadingMore ||
            content.isLoadingFirstPage ||
            // Already fetched with this exact cursor — see the KDoc on TabContent.consumedCursor.
            cursor == content.consumedCursor
        ) {
            return
        }
        updateContent(tab) { it.copy(isLoadingMore = true, error = null) }
        load(tab = tab, cursor = cursor)
    }

    fun retry(tab: Tab) {
        val content = _uiState.value.content(tab)
        // `hasLoadedOnce` — not "reactions is empty" — is what tells an unresolved first page apart from
        // a page that has already succeeded at least once: a first page can legitimately come back with
        // zero rows and a cursor (e.g. every reaction on it was since removed), and a failure while
        // paging past that point must still resume from the cursor instead of restarting from scratch,
        // which discarding reactions.isEmpty() as the signal would otherwise do.
        if (content.hasLoadedOnce) {
            loadMore(tab)
        } else {
            updateContent(tab) { it.copy(error = null, hasLoadedOnce = false) }
            loadFirstPageIfNeeded(tab)
        }
    }

    private fun load(
        tab: Tab,
        cursor: String?,
    ) {
        viewModelScope.launch {
            when (
                val result =
                    reactionsRepository.getReactions(
                        contentId = contentId,
                        kind = tab.reactionKind,
                        cursor = cursor,
                    )
            ) {
                is OctopusResult.Success ->
                    updateContent(tab) {
                        it.copy(
                            reactions = it.reactions + result.data.reactions,
                            nextCursor = result.data.nextCursor,
                            consumedCursor = cursor,
                            hasLoadedOnce = true,
                            isLoadingFirstPage = false,
                            isLoadingMore = false,
                        )
                    }

                is OctopusResult.Failure ->
                    updateContent(tab) {
                        it.copy(
                            isLoadingFirstPage = false,
                            isLoadingMore = false,
                            // The shared mapper, not a local one: it is what turns a deleted content
                            // into "content unavailable" and surfaces the server's own reason on a
                            // refused or unauthenticated call, exactly as the rest of the SDK does.
                            error = result.errorText,
                        )
                    }
            }
        }
    }

    private fun updateContent(
        tab: Tab,
        transform: (TabContent) -> TabContent,
    ) {
        _uiState.update { state ->
            state.copy(contents = state.contents + (tab to transform(state.content(tab))))
        }
    }

    internal companion object {
        internal fun buildInitialState(reactionCounts: List<ReactionCount>): UiState {
            // distinctBy guards a backend sending the same kind twice: `tabs` and `counts` are both keyed
            // by `Tab.OfKind`, equal for the same kind, so a duplicate wouldn't merge there — it would
            // show as a second, identically-behaving tab with the same label. Keeps the first occurrence.
            val nonEmpty = reactionCounts.filter { it.count > 0 }.distinctBy { it.reactionKind }
            // Ordered by volume, most reacted first. Ties keep the backend's own order: the spec breaks
            // them by most recent reaction and ReactionCount carries no date to sort on, so only the
            // backend can express that — through the order it sends the counters in. sortedByDescending
            // is documented as stable, which is what preserves that tie-break.
            val ordered = nonEmpty.sortedByDescending { it.count }
            val kindTabs = ordered.map { Tab.OfKind(it.reactionKind) }
            val tabs: List<Tab> =
                when (kindTabs.size) {
                    // Unreachable from the UI — the summary only opens the sheet when something was reacted
                    // — but selectedTab indexes into this, so it must never be empty.
                    0 -> listOf(Tab.All)
                    // A single kind needs no All tab: it would list exactly the same people.
                    1 -> kindTabs
                    else -> listOf(Tab.All) + kindTabs
                }
            return UiState(
                tabs = tabs,
                counts = ordered.associate { Tab.OfKind(it.reactionKind) to it.count },
            )
        }
    }
}
