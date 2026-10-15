package com.octopuscommunity.sdk.sample.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.octopuscommunity.sdk.sample.data.model.SDKConfig.CommunityNavigation

/**
 * What the Community tab owes when the sample re-targets the SDK at another community.
 *
 * The tab hosts `OctopusHomeContent`, whose ViewModels are scoped to the tab's `NavBackStackEntry`
 * and were built from the Koin container `switchCommunity` has since replaced. Reaching the new
 * community therefore means giving that destination a **new entry**: nothing short of it clears the
 * `ViewModelStore`, and re-composing the same entry would hand the rebuilt subtree the same stale
 * ViewModels back.
 */
internal enum class CommunityTabReload {
    /** Nothing to do — the tab is already on the community session that is live. */
    NONE,

    /** The tab is on screen: replace its entry in place so the feed reloads without a restart. */
    RENAVIGATE,

    /**
     * The tab is off screen with its state saved (the tab bar pops with `saveState = true`, which
     * keeps the entry's `ViewModelStore` alive for `restoreState`). Drop that saved state so
     * re-entering the tab builds a fresh entry instead of restoring the previous community's one.
     */
    CLEAR_SAVED_STATE,
}

/**
 * Decides what to do about the Community tab, given the session it is showing
 * ([displayedSession]) and the one that is live ([currentSession]).
 *
 * Equal sessions mean the tab is current — which is also the first-composition case, and the
 * reason a fresh `MainScaffold` (Reset Configuration → Start SDK re-enters `MainRoute` with
 * `popUpTo(0)`) performs no navigation at all: its tab graph is already new. That matters beyond
 * tidiness — a navigation issued while the root graph is still settling is what internal ticket / internal change
 * crashed on.
 */
internal fun communityTabReload(
    displayedSession: Int,
    currentSession: Int,
    isCommunityTabOnScreen: Boolean,
    displayedMode: CommunityNavigation = CommunityNavigation.InTab,
    currentMode: CommunityNavigation = displayedMode,
): CommunityTabReload =
    when {
        displayedSession == currentSession && displayedMode == currentMode -> CommunityTabReload.NONE
        isCommunityTabOnScreen -> CommunityTabReload.RENAVIGATE
        else -> CommunityTabReload.CLEAR_SAVED_STATE
    }

/** MainRoute owns this process-local baseline while a root host destination covers its content. */
internal class CommunityTabSessionViewModel : ViewModel() {
    private var displayedSession: Int? = null

    fun reloadAction(
        currentSession: Int,
        isCommunityTabOnScreen: Boolean,
        displayedMode: CommunityNavigation,
        currentMode: CommunityNavigation,
    ): CommunityTabReload {
        val action =
            communityTabReload(
                // Zero is the tracker's asynchronous startup value, not a previous community.
                displayedSession?.takeUnless { it == 0 } ?: currentSession,
                currentSession,
                isCommunityTabOnScreen,
                displayedMode,
                currentMode,
            )
        displayedSession = currentSession
        return action
    }
}

/**
 * Applies [communityTabReload] to [navController] (the **tab** graph, never the root one) every
 * time [currentSession] moves.
 */
@Composable
internal fun ReloadCommunityTabOnSessionChange(
    navController: NavHostController,
    currentSession: Int,
    currentMode: CommunityNavigation,
    isCommunityTabOnScreen: Boolean,
    sessionState: CommunityTabSessionViewModel = viewModel(),
) {
    // Mode survives process death. The session counter does not: MainRoute's ViewModel preserves
    // it across host screens, but a fresh process re-seeds it from the live tracker instead of
    // comparing a saved counter with the tracker's reset value and discarding a restored stack.
    var displayedMode by rememberSaveable { mutableStateOf(currentMode) }
    val hasGraph = navController.currentBackStackEntry != null
    LaunchedEffect(currentSession, currentMode, hasGraph) {
        if (!hasGraph) return@LaunchedEffect
        val communityIsLive = runCatching { navController.getBackStackEntry<CommunityGraph>() }.isSuccess
        val action =
            sessionState.reloadAction(currentSession, isCommunityTabOnScreen || communityIsLive, displayedMode, currentMode)
        reloadCommunityStack(navController, action)
        displayedMode = currentMode
    }
}

/** Invalidate the graph's entire saved/live stack, rather than only its home entry. */
internal fun reloadCommunityStack(
    navController: NavHostController,
    action: CommunityTabReload,
) {
    if (action == CommunityTabReload.NONE) return
    navController.clearBackStack<CommunityGraph>()
    if (action == CommunityTabReload.RENAVIGATE) {
        navController.navigate(CommunityGraph) {
            popUpTo<CommunityGraph> { inclusive = true }
        }
    }
}
