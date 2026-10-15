package com.octopuscommunity.sdk.sample.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.octopuscommunity.sdk.sample.data.model.SDKConfig.CommunityNavigation
import com.octopuscommunity.sdk.sample.ui.config.ConfigSection
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.navigateToOctopusPost
import kotlinx.serialization.Serializable

/**
 * The SDK configuration screen — onboarding on first launch, reconfiguration afterwards.
 *
 * [section] is the anchor a link passes when it is about one Config setting: the screen opens with
 * that section expanded and scrolled into view, every other section keeping its default state.
 * `null` (Settings → Configuration, first launch, reset) opens the screen at the top.
 * `?section=INTEGRATION` predates [HostIntegrationRoute]: once the SDK is up it forwards there.
 */
@Serializable
data class ConfigRoute(
    val section: ConfigSection? = null,
) {
    companion object {
        /**
         * QA + power-user shortcut, also matched without the query:
         * `adb shell am start -a android.intent.action.VIEW -d "octopus-sample://config?section=SERVER"`.
         */
        const val DEEP_LINK_PATTERN = "octopus-sample://config?section={section}"
    }
}

/**
 * Host app integration: which host callbacks the sample wires and its URL policy, reached from
 * Settings › Developer tools. A root route, like
 * Login, because deep links only resolve on the root graph.
 */
@Serializable
data object HostIntegrationRoute {
    /** QA shortcut: `adb shell am start -a android.intent.action.VIEW -d "octopus-sample://host-integration"`. */
    const val DEEP_LINK = "octopus-sample://host-integration"
}

/** Main screen with bottom navigation (Scenarios, Community, Settings — DS v1). */
@Serializable
data object MainRoute

/**
 * Full-screen community view (standalone, not embedded in bottom nav) — the host presentation the
 * catalog's `fullscreen` scenario is about: a first-class pushed destination of the host's own
 * navigation graph, not a container the SDK opens for itself.
 *
 * Both arguments default to the plain pushed shape, so `CommunityFullScreenRoute()` is the
 * scenario's base case. They exist for the three
 * `qa-preset-fullscreen-*` presets:
 *
 * - [postId] — preset 2. Non-null means "open this presentation, then go straight to that post":
 *   the screen pushes the SDK's post destination once, on the HOST's graph, so the post replaces
 *   the presentation on screen and Back returns to the presented community rather than out of it.
 *   Read that literally — the host top bar and preset 3's bottom bar are NOT visible while the post
 *   is shown, because the post is a sibling destination of this one, not a child of it. That is a
 *   deliberate difference from `sheet` and `modal`, whose deep link stays inside the container:
 *   this scenario's whole point is that the presentation is the host's own graph, and a host graph
 *   is exactly where a pushed post lands. It is not a defect, and it is not a chrome regression.
 *   What Back does IS worth checking — see `DeepLinkToPostOnce` for the loop it used to cause.
 * - [showHostBottomBar] — preset 3. Puts a host-owned bottom bar under the embedded content, which
 *   is what makes the bottom safe-area inset question observable at all: the SDK must clear the
 *   bar, not paint under it.
 */
@Serializable
data class CommunityFullScreenRoute(
    val postId: String? = null,
    val showHostBottomBar: Boolean = false,
)

/** SSO login screen. */
@Serializable
data object LoginRoute

/** SSO profile edit screen. */
@Serializable
data object EditUserRoute

// SSO login screen and profile editor, as *root* destinations.
//
// These are not Settings entries any more — Settings → Account owns both flows inline. They stay
// in the root graph because the SDK itself asks for them through `onNavigateToLogin` /
// `onNavigateToProfileEdit` from inside community content, where the host is expected to present
// its own screen over the SDK surface.

/**
 * Host-owned profile screen (Unified Profile pillar 1 demo). Reached when the SDK invokes the host's
 * `onNavigateToProfile` callback: the sample pushes this screen instead of the Octopus native
 * profile, for the tapped member's host-side [clientUserId].
 */
@Serializable
data class ClientProfileRoute(
    val clientUserId: String,
)

/** External post link; SDK-backed navigation waits for initialization. */
@Serializable
data class SamplePostLinkRoute(
    val postId: String,
)

/** Consumes the external route after SDK initialization, including empty IDs accepted by Navigation. */
fun NavController.openSamplePostLink(
    route: SamplePostLinkRoute,
    mode: CommunityNavigation = CommunityNavigation.InTab,
) {
    navigate(MainRoute) {
        popUpTo<SamplePostLinkRoute> { inclusive = true }
        launchSingleTop = true
    }
    route.postId
        .trim()
        .takeIf { it.isNotEmpty() }
        ?.let {
            if (mode == CommunityNavigation.InTab) {
                currentBackStackEntry?.savedStateHandle?.set(COMMUNITY_POST_ID, it)
            } else {
                navigateToOctopusPost(it)
            }
        }
}

internal const val UNIFIED_PROFILE_SCENARIO = "unifiedProfileScenario"
internal const val COMMUNITY_POST_ID = "communityPostId"
internal const val COMMUNITY_LINK_CONSUMED = "communityLinkConsumed"
internal const val COMMUNITY_DEEP_LINK = "communityDeepLink"

/** An incoming argument can be used once, even if a host screen later changes presentation. */
internal fun unconsumedCommunityDeepLink(entry: NavBackStackEntry): Uri? {
    if (entry.savedStateHandle.get<Boolean>(COMMUNITY_LINK_CONSUMED) == true) return null
    @Suppress("DEPRECATION")
    return entry.arguments?.getParcelable<Intent>(NavController.KEY_DEEP_LINK_INTENT)?.data
}

/** Relay an SDK push/deep link from the root graph into the saved Community tab. */
internal fun NavController.openCommunityDeepLink(uri: String) {
    val hasMainEntry = runCatching { getBackStackEntry<MainRoute>() }.isSuccess
    navigate(MainRoute) {
        if (hasMainEntry) popUpTo<MainRoute> { inclusive = false } else popUpTo(graph.id)
        launchSingleTop = true
    }
    currentBackStackEntry?.savedStateHandle?.set(COMMUNITY_DEEP_LINK, uri)
}

/**
 * Handle links after the root graph exists, without briefly presenting an in-tab SDK root screen.
 * False means an SDK link must remain pending until initialization finishes (including onboarding).
 */
internal fun NavHostController.navigateToDeepLink(
    uri: Uri,
    mode: CommunityNavigation,
    isSdkInitialized: Boolean,
): Boolean {
    val match = graph.matchDeepLink(NavDeepLinkRequest.Builder.fromUri(uri).build()) ?: return true
    val isSdkLink = match.destination.route?.startsWith(OctopusDestination::class.qualifiedName + ".") == true
    if (isSdkLink && !isSdkInitialized) return false
    if (isSdkLink && mode == CommunityNavigation.InTab) {
        openCommunityDeepLink(uri.toString())
    } else {
        navigate(
            deepLink = uri,
            navOptions =
                NavOptions
                    .Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .build(),
        )
    }
    return true
}
