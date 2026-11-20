@file:OptIn(com.octopuscommunity.sdk.InternalOctopusApi::class)

package com.octopuscommunity.sdk.ui

import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavHostController
import com.octopuscommunity.sdk.OctopusLifecycle
import com.octopuscommunity.sdk.OctopusSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/** Maps only currently configured host links onto the graph's stable internal links. */
internal fun internalOctopusLink(
    uri: String,
    prefixes: List<String>,
): String? =
    prefixes.firstNotNullOfOrNull { prefix ->
        val base = prefix.trimEnd('/')
        when {
            uri == base -> OctopusSDK.INTERNAL_DEEP_LINK_BASE_PATH + "home"
            uri.startsWith("$base/") -> OctopusSDK.INTERNAL_DEEP_LINK_BASE_PATH + uri.removePrefix("$base/")
            else -> null
        }
    }

/**
 * Replays a configured SDK launch URL once initialization makes its prefix available, unless the
 * current destination and arguments already match that URL. Consumption is saved per Activity in
 * SavedStateRegistry; the host's Intent is never changed. Navigation saves/restores the resulting
 * back stack normally. Replay pushes onto the existing host back stack. Hosts remain responsible
 * for new Intents and fresh navigation controllers.
 * Waiting for initialization is cancelled when the Activity is destroyed.
 */
internal fun installOctopusPendingDeepLinks(controller: NavHostController) {
    val activity =
        generateSequence(controller.context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<ComponentActivity>()
            .firstOrNull() ?: return
    val uri = activity.intent?.dataString ?: return
    val state = PendingLaunchLinks.state(activity)
    state.controller = WeakReference(controller)
    if (state.handledUri == uri || state.waiting?.isActive == true) return
    state.waiting =
        activity.lifecycleScope.launch(Dispatchers.Main) {
            // Dispatch after NavHost installs its graph and restores or handles its launch link.
            OctopusLifecycle.container.first { it != null }
            val current = state.controller?.get() ?: return@launch
            if (state.handledUri == uri || current.currentDestination == null) return@launch
            val internal = internalOctopusLink(uri, OctopusSDK.appDeepLinkBasePaths) ?: return@launch
            val request = NavDeepLinkRequest.Builder.fromUri(internal.toUri()).build()
            val entry = current.currentBackStackEntry ?: return@launch
            val match = entry.destination.matchDeepLink(request)
            val alreadyRouted =
                match != null &&
                    (match.matchingArgs == null || match.hasMatchingArgs(entry.arguments))
            if (alreadyRouted) {
                state.handledUri = uri
            } else if (current.graph.hasDeepLink(request)) {
                current.navigate(request)
                state.handledUri = uri
            }
        }
}

/** No Activity or destination listeners are installed or retained for a navigation controller. */
private object PendingLaunchLinks {
    const val KEY = "com.octopuscommunity.sdk.launch_link_handled"
    private val states = java.util.WeakHashMap<ComponentActivity, LaunchState>()

    fun state(activity: ComponentActivity): LaunchState =
        states.getOrPut(activity) {
            val restored = activity.savedStateRegistry.consumeRestoredStateForKey(KEY)
            LaunchState(restored?.getString(KEY)).also { state ->
                activity.savedStateRegistry.registerSavedStateProvider(KEY) {
                    android.os.Bundle().apply { putString(KEY, state.handledUri) }
                }
            }
        }

    class LaunchState(
        var handledUri: String?,
    ) {
        var controller: WeakReference<NavHostController>? = null
        var waiting: kotlinx.coroutines.Job? = null
    }
}
