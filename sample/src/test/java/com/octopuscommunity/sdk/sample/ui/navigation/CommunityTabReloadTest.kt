package com.octopuscommunity.sdk.sample.ui.navigation

import com.octopuscommunity.sdk.sample.data.CommunitySessionTracker
import com.octopuscommunity.sdk.sample.data.model.SDKConfig.CommunityNavigation
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for the Community tab's reload decision (internal change) and for the counter that feeds it.
 *
 * The graph operations are covered by [CommunityNavigationTest]; this class covers the pure
 * three-way choice, and above all the "same session means
 * do nothing" case, which is what keeps a freshly composed `MainScaffold` from issuing a navigation
 * while the root graph is still settling (the internal ticket / internal change crash).
 */
class CommunityTabReloadTest {
    @Test
    fun `asynchronous startup seeds zero to one without clearing restored details`() {
        for (onScreen in listOf(false, true)) {
            val baseline = CommunityTabSessionViewModel()
            assertEquals(CommunityTabReload.NONE, baseline.reloadAction(0, onScreen, CommunityNavigation.InTab, CommunityNavigation.InTab))
            assertEquals(CommunityTabReload.NONE, baseline.reloadAction(1, onScreen, CommunityNavigation.InTab, CommunityNavigation.InTab))
            assertEquals(
                if (onScreen) CommunityTabReload.RENAVIGATE else CommunityTabReload.CLEAR_SAVED_STATE,
                baseline.reloadAction(2, onScreen, CommunityNavigation.InTab, CommunityNavigation.InTab),
            )
        }
    }

    @Test
    fun `a new process re-seeds the session baseline after the tracker resets`() {
        val previousProcess = CommunityTabSessionViewModel()
        previousProcess.reloadAction(4, false, CommunityNavigation.InTab, CommunityNavigation.InTab)
        val restoredProcess = CommunityTabSessionViewModel()
        val resetTracker = CommunitySessionTracker()
        for (onScreen in listOf(false, true)) {
            assertEquals(
                CommunityTabReload.NONE,
                restoredProcess.reloadAction(
                    resetTracker.generation.value,
                    onScreen,
                    CommunityNavigation.InTab,
                    CommunityNavigation.InTab,
                ),
            )
        }
    }

    @Test
    fun `a fresh process still invalidates details when the restored mode differs`() {
        assertEquals(
            CommunityTabReload.CLEAR_SAVED_STATE,
            CommunityTabSessionViewModel().reloadAction(0, false, CommunityNavigation.InTab, CommunityNavigation.FullScreen),
        )
    }

    @Test
    fun `changing only the navigation mode invalidates the whole Community stack`() {
        for (onScreen in listOf(false, true)) {
            for (mode in CommunityNavigation.entries) {
                assertEquals(
                    if (onScreen) CommunityTabReload.RENAVIGATE else CommunityTabReload.CLEAR_SAVED_STATE,
                    communityTabReload(3, 3, onScreen, mode, CommunityNavigation.entries.first { it != mode }),
                )
            }
        }
    }

    @Test
    fun `an unchanged session asks for nothing, wherever the tab is`() {
        for (onScreen in listOf(false, true)) {
            assertEquals(
                CommunityTabReload.NONE,
                communityTabReload(
                    displayedSession = 3,
                    currentSession = 3,
                    isCommunityTabOnScreen = onScreen,
                ),
            )
        }
    }

    @Test
    fun `a switch while the tab is on screen replaces its entry`() {
        assertEquals(
            CommunityTabReload.RENAVIGATE,
            communityTabReload(
                displayedSession = 0,
                currentSession = 1,
                isCommunityTabOnScreen = true,
            ),
        )
    }

    @Test
    fun `a switch while the tab is elsewhere drops its saved state`() {
        assertEquals(
            CommunityTabReload.CLEAR_SAVED_STATE,
            communityTabReload(
                displayedSession = 0,
                currentSession = 1,
                isCommunityTabOnScreen = false,
            ),
        )
    }

    @Test
    fun `several switches at once still only ask for one reload`() {
        // The counter can move by more than one before the effect runs (a config change that
        // re-targets the SDK, then a Switch community from the lifecycle scenario). The verdict is about
        // whether the tab is behind, not by how much.
        assertEquals(
            CommunityTabReload.RENAVIGATE,
            communityTabReload(
                displayedSession = 1,
                currentSession = 4,
                isCommunityTabOnScreen = true,
            ),
        )
    }

    @Test
    fun `the tracker never repeats a session number`() {
        val tracker = CommunitySessionTracker()
        val first = tracker.generation.value
        tracker.onCommunityTargeted()
        val second = tracker.generation.value
        tracker.onCommunityTargeted()
        val third = tracker.generation.value

        // Distinct values are the whole contract: the tab reacts to a *change*, so a tracker that
        // re-emitted the same number after a switch would leave the stale feed on screen — which
        // is exactly how `OctopusSDK.isInitialisedFlow` fails to signal a switch.
        assertEquals(listOf(first, second, third), listOf(first, second, third).distinct())
    }
}
