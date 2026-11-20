package com.octopuscommunity.sdk.ui.access

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.octopuscommunity.sdk.ui.OctopusDestination

/**
 * What the route of every Octopus screen starts with: the serial name of the destinations, a constant written at
 * compile time, minus the name of one of them. Never the class name of [OctopusDestination], which R8 renames in a
 * minified host app while the routes keep their serial names.
 */
internal val octopusRoutePrefix =
    OctopusDestination.Home
        .serializer()
        .descriptor.serialName
        .substringBeforeLast('.') + "."

private const val COMMUNITY_ROOT_KEY = "com.octopuscommunity.sdk.ui.access.communityRoot"

/** Registers only a composed Home in this controller; a host route is a stopping point, never a pop target. */
@Composable
internal fun RegisterCommunityRoot(navController: NavHostController) {
    // A host may replace the ViewModelStoreOwner. Its navigation lifecycle still identifies the
    // actual entry. Never mark an outer controller's entry for content in a nested NavHost.
    val entry = LocalLifecycleOwner.current as? NavBackStackEntry ?: return
    if (navController.currentBackStack.value.none { it === entry }) return
    DisposableEffect(entry) {
        val savedState = entry.savedStateHandle
        savedState[COMMUNITY_ROOT_KEY] = true
        onDispose {
            // Keep inactive/saved community stacks registered, but not a live host destination
            // that switches away from its community content without navigating (state-driven tabs).
            if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                savedState.remove<Boolean>(COMMUNITY_ROOT_KEY)
            }
        }
    }
}

private val NavBackStackEntry.isOctopusRoute: Boolean
    get() = destination.route?.startsWith(octopusRoutePrefix) == true

private val NavBackStackEntry.isCommunityRoot: Boolean
    get() = savedStateHandle.get<Boolean>(COMMUNITY_ROOT_KEY) == true

/**
 * Pops the Octopus screens pushed over the first screen of the community, and stops there.
 *
 * The access denied screen stands in place of every Octopus screen: a member whose access is taken away on a pushed
 * screen (a post, a profile) would otherwise find it again behind each back press. The Octopus screens live in the
 * `NavHost` of the host app, so only an Octopus screen lying on another Octopus screen is popped: a screen of the host
 * app is never popped, nor an Octopus screen the host opened directly. A host-defined Home in the same controller
 * can also be the stopping point, once composed. Its registration survives inactive and saved back stacks.
 */
internal fun NavController.popPushedOctopusScreens() {
    while (currentBackStackEntry?.isOctopusRoute == true) {
        val previous = previousBackStackEntry ?: return
        if (!previous.isOctopusRoute && !previous.isCommunityRoot) return
        if (!popBackStack()) return
    }
}
