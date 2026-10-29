package com.octopuscommunity.sdk.sample.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.octopuscommunity.sdk.ui.OctopusDestination
import kotlinx.serialization.ExperimentalSerializationApi

/** Retained by the presentation's graph entry, so a switch while it is covered or rotating is observed. */
internal class CommunityPresentationSessionViewModel : ViewModel() {
    private var displayedSession: Int? = null

    fun hasChanged(currentSession: Int): Boolean {
        val changed = displayedSession?.let { it != 0 && it != currentSession } ?: false
        displayedSession = currentSession
        return changed
    }
}

/**
 * Calls [reload] once per session change seen by [navController]'s graph.
 *
 * The baseline lives in a ViewModel of the graph entry, not in the composition: a caller that is
 * disposed and recreated while the switch happens (covered, rotating) still sees the old session.
 * A replacement graph entry records the current session as its own baseline before later switches.
 */
@Composable
internal fun OnGraphSessionChange(
    navController: NavHostController,
    currentSession: Int,
    reload: (NavHostController) -> Unit,
) {
    val backStack by navController.currentBackStack.collectAsState()
    // The caller composes before its NavHost: there is no graph to own a baseline yet.
    val graphEntry = backStack.firstOrNull()
    LaunchedEffect(navController, currentSession, graphEntry?.id) {
        if (graphEntry == null) return@LaunchedEffect
        val session = ViewModelProvider(graphEntry)[CommunityPresentationSessionViewModel::class.java]
        if (session.hasChanged(currentSession)) reload(navController)
    }
}

/** Replace SDK-backed entries of a sheet/modal graph rather than reusing the previous Koin container. */
@Composable
internal inline fun <reified T : Any> ReloadCommunityPresentationOnSessionChange(
    navController: NavHostController,
    currentSession: Int,
    homeRoute: T,
) {
    OnGraphSessionChange(navController, currentSession) { reloadCommunityPresentation(it, homeRoute) }
}

/** Inclusive pop also removes SDK details or host routes pushed above the presentation. */
internal inline fun <reified T : Any> reloadCommunityPresentation(
    navController: NavHostController,
    homeRoute: T,
) {
    navController.navigate(homeRoute) {
        popUpTo<T> { inclusive = true }
    }
}

/**
 * The root graph's counterpart of [ReloadCommunityPresentationOnSessionChange]: the fullscreen
 * preset and the SDK destinations the scenario demos push on the root graph.
 */
@Composable
internal fun ReloadRootCommunityEntriesOnSessionChange(
    navController: NavHostController,
    currentSession: Int,
) {
    OnGraphSessionChange(navController, currentSession, ::reloadRootCommunityEntries)
}

// Serial names retain their prefix even when runtime class names are minified.
@OptIn(ExperimentalSerializationApi::class)
private val octopusDestinationPrefix =
    OctopusDestination.Home
        .serializer()
        .descriptor.serialName
        .substringBeforeLast('.') + "."

/** Every SDK destination is a subtype of [OctopusDestination], whose serial name prefixes the route. */
internal fun NavDestination.isOctopusDestination(): Boolean = route?.startsWith(octopusDestinationPrefix) == true

/** Root entries whose ViewModels resolve SDK DI from the container current when they were built. */
internal fun NavDestination.isCommunityBacked(): Boolean = hasRoute<CommunityFullScreenRoute>() || isOctopusDestination()

/**
 * Removes the lowest community-backed root entry and everything above it.
 *
 * The fullscreen preset is rebuilt without its post, and the SDK home is rebuilt. Other SDK roots
 * and details are dropped so the host screen below them is shown again.
 */
internal fun reloadRootCommunityEntries(navController: NavHostController) {
    val stale = navController.currentBackStack.value.firstOrNull { it.destination.isCommunityBacked() } ?: return
    val rebuilt: Any? =
        when {
            stale.destination.hasRoute<CommunityFullScreenRoute>() -> stale.toRoute<CommunityFullScreenRoute>().copy(postId = null)
            stale.destination.hasRoute<OctopusDestination.Home>() -> OctopusDestination.Home
            else -> null
        }

    fun isStaleLive() = navController.currentBackStack.value.any { it.id == stale.id }
    // Entry by entry: popUpTo(id) stops at the most recent entry of a route pushed more than once.
    while (isStaleLive() && navController.previousBackStackEntry != null) {
        navController.popBackStack()
    }
    if (!isStaleLive()) {
        rebuilt?.let { navController.navigate(it) }
    } else {
        // The stale entry is the only destination left: replace it rather than empty the graph.
        navController.navigate(rebuilt ?: MainRoute) {
            popUpTo(navController.graph.id)
        }
    }
}
