package com.octopuscommunity.sdk.ui.common.pageindicator

import androidx.compose.runtime.Immutable

/**
 * What the page indicator of a carousel shows: at most five dots, a sliding window over the pages.
 *
 * When there are more pages than dots, the dots at the edge of the window that hide further pages
 * are drawn smaller, and the selected dot stays at a position that tells the user whether there is
 * more to see in the direction they are moving. Same rules as iOS.
 *
 * @property dots The dots, from left to right.
 * @property selectedPosition Position, in [dots], of the dot of the current page.
 * @property firstPage Index of the page the first dot stands for. The dot at a position stands for
 * page `firstPage + position`: it is how the indicator tells a window that moved by one page from
 * one that did not, since both can have the same dots and the same selected position.
 */
@Immutable
internal data class PageIndicatorState(
    val dots: List<Dot>,
    val selectedPosition: Int,
    val firstPage: Int = 0,
) {
    enum class Dot { Normal, Small }

    companion object {
        /** Number of dots displayed at most. */
        private const val MAX_DOTS = 5

        /** Minimum page count for the carousel to display a "current / total" counter. */
        private const val MIN_PAGES_WITH_COUNTER = 2

        /**
         * Builds the state of the indicator, or returns `null` when there is no need for one (fewer
         * than two pages).
         *
         * @param pageCount The number of pages of the carousel.
         * @param currentIndex The 0-based index of the current page.
         * @param lastMoveForward Whether the last move went toward the end (`true`) or toward the start.
         */
        fun make(
            pageCount: Int,
            currentIndex: Int,
            lastMoveForward: Boolean,
        ): PageIndicatorState? =
            window(pageCount, currentIndex, lastMoveForward)
                ?.let { it.copy(firstPage = currentIndex - it.selectedPosition) }

        private fun window(
            pageCount: Int,
            currentIndex: Int,
            lastMoveForward: Boolean,
        ): PageIndicatorState? {
            if (pageCount < 2) return null
            if (pageCount <= MAX_DOTS) {
                return PageIndicatorState(dots = List(pageCount) { Dot.Normal }, selectedPosition = currentIndex)
            }
            if (currentIndex == pageCount - 1) {
                return PageIndicatorState(dots = listOf(Dot.Small) + List(4) { Dot.Normal }, selectedPosition = 4)
            }
            val nearStart = if (lastMoveForward) currentIndex <= 3 else currentIndex <= 2
            if (nearStart) {
                return PageIndicatorState(dots = List(4) { Dot.Normal } + Dot.Small, selectedPosition = currentIndex)
            }
            // Going back from the end: while the last page is still in the window, the fifth dot
            // keeps its normal size and the selected one steps back from it.
            val pagesAfter = pageCount - 1 - currentIndex
            if (!lastMoveForward && pagesAfter <= 2) {
                return PageIndicatorState(dots = listOf(Dot.Small) + List(4) { Dot.Normal }, selectedPosition = 4 - pagesAfter)
            }
            return PageIndicatorState(
                dots = listOf(Dot.Small) + List(3) { Dot.Normal } + Dot.Small,
                selectedPosition = if (lastMoveForward) 3 else 2,
            )
        }

        /** Whether the carousel displays a "current / total" counter next to the dots. */
        fun showsCounter(pageCount: Int): Boolean = pageCount >= MIN_PAGES_WITH_COUNTER
    }
}
