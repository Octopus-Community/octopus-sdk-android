package com.octopuscommunity.sdk.sample.ui.navigation

import androidx.core.net.toUri
import androidx.navigation.NavGraph
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.ui.components.UrlOpeningStrategy

/** Host callbacks always leave SDK navigation through the host graph, in either presentation mode. */
internal data class CommunityHostNavigation(
    val login: (() -> Unit)?,
    val profileEdit: ((ProfileField?) -> Unit)?,
    val profile: ((String) -> Unit)?,
    val clientObject: ((String) -> Unit)?,
    val groupAccessDenied: ((String) -> Unit)?,
    val url: (String) -> UrlOpeningStrategy,
)

internal fun communityHostNavigation(
    callbacks: SDKConfig.HostCallbacks,
    navigateHost: (Any) -> Unit,
    onClientObject: (String) -> Unit,
    onGroupAccessDenied: (String) -> Unit,
    onUrl: (String) -> UrlOpeningStrategy,
): CommunityHostNavigation =
    CommunityHostNavigation(
        login = if (callbacks.loginWired) ({ navigateHost(LoginRoute) }) else null,
        profileEdit = if (callbacks.profileEditWired) ({ navigateHost(EditUserRoute) }) else null,
        profile = if (callbacks.profileWired) ({ navigateHost(ClientProfileRoute(it)) }) else null,
        clientObject = if (callbacks.clientObjectWired) onClientObject else null,
        groupAccessDenied = if (callbacks.groupAccessDeniedWired) onGroupAccessDenied else null,
        url = onUrl,
    )

/** SDK destinations follow the chosen presentation; host callbacks never use this controller. */
internal fun <T> communityContentController(
    mode: SDKConfig.CommunityNavigation,
    root: T,
    tab: T,
): T = if (mode == SDKConfig.CommunityNavigation.InTab) tab else root

/**
 * Only incoming links need relaying into the tab. Navigation also attaches an Intent with a
 * synthetic URI to typed route navigation; replaying that URI cannot resolve an explicit deep link.
 */
internal fun shouldRelayCommunityDeepLink(
    mode: SDKConfig.CommunityNavigation,
    uri: String?,
    graph: NavGraph,
): Boolean =
    mode == SDKConfig.CommunityNavigation.InTab &&
        uri != null &&
        graph.hasDeepLink(uri.toUri())
