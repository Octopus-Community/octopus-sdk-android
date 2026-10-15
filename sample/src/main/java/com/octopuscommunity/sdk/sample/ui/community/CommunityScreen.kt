package com.octopuscommunity.sdk.sample.ui.community

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.ui.components.SampleBanner
import com.octopuscommunity.sdk.sample.ui.components.SampleBannerTone
import com.octopuscommunity.sdk.sample.ui.components.openUrlOrCopy
import com.octopuscommunity.sdk.sample.ui.navigation.CommunityHostNavigation
import com.octopuscommunity.sdk.sample.ui.navigation.EditUserRoute
import com.octopuscommunity.sdk.sample.ui.navigation.LoginRoute
import com.octopuscommunity.sdk.sample.ui.theme.CommunityTheme
import com.octopuscommunity.sdk.ui.components.UrlOpeningStrategy
import com.octopuscommunity.sdk.ui.home.OctopusHomeContent
import com.octopuscommunity.sdk.ui.navigateToOctopusPost

/**
 * Community tab: the one tab that belongs to the SDK.
 *
 * The SDK surface carries **no sample chrome** — no border, no "SDK UI" badge, nothing drawn on top
 * of it. The SDK draws the feed, its error state and its empty state; the sample adds exactly one
 * thing, in its own layer above the surface (DS screen 04): for a guest session, the read-only
 * warning band under the app bar, which names the cause and carries the Connect action. A sample
 * "couldn't reach" band would only repeat the SDK's own error state.
 */
@Composable
internal fun CommunityTab(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    hostNavigation: CommunityHostNavigation,
    connectionState: ConnectionState = ConnectionState.NotConnected,
    onConnectUser: () -> Unit = {},
) {
    val isGuestSession =
        (connectionState as? ConnectionState.Connected)?.isGuest != false

    Column(modifier = modifier.fillMaxSize()) {
        if (isGuestSession) {
            SampleBanner(
                tone = SampleBannerTone.Warning,
                title = "Read-only",
                message = "Connect an SSO user to post and react.",
                actionLabel = "Connect",
                onAction = onConnectUser,
                inline = true,
                modifier = Modifier.testTag("community-band-readonly"),
            )
        }

        OctopusHomeContent(
            navController = navController,
            onNavigateToLogin = hostNavigation.login,
            onNavigateToProfileEdit = hostNavigation.profileEdit,
            onNavigateToProfile = hostNavigation.profile,
            onNavigateToClientObject = hostNavigation.clientObject,
            onNavigateToUrl = hostNavigation.url,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Full-screen community screen with its own navigation handling.
 * Pushed by the catalog's `fullscreen` scenario.
 * Includes a TopAppBar with a back button to return to the main scaffold.
 *
 * [deepLinkPostId] and [showHostBottomBar] are the two variations the catalog's `fullscreen`
 * presets ask for; both keep their pre-existing defaults, so the base case is the plain pushed shape.
 * See [com.octopuscommunity.sdk.sample.ui.navigation.CommunityFullScreenRoute] for what each one
 * proves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFullScreen(
    navController: NavHostController,
    onBack: () -> Unit,
    theme: SDKConfig.Theme,
    modifier: Modifier = Modifier,
    hostCallbacks: SDKConfig.HostCallbacks = SDKConfig.HostCallbacks(),
    deepLinkPostId: String? = null,
    showHostBottomBar: Boolean = false,
) {
    val context = LocalContext.current

    DeepLinkToPostOnce(postId = deepLinkPostId, navController = navController)

    CommunityTheme(theme = theme) {
        Scaffold(
            bottomBar = {
                // Host-owned chrome, deliberately inert: what `qa-preset-fullscreen-3` checks is
                // that the embedded content clears this bar and its safe-area inset, not that the
                // bar does anything. A working tab bar here would only add a second thing to
                // debug when the inset is wrong.
                if (showHostBottomBar) HostBottomBar()
            },
            topBar = {
                TopAppBar(
                    title = { Text("Community") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                )
            },
            modifier = modifier,
        ) { innerPadding ->
            OctopusHomeContent(
                navController = navController,
                onNavigateToLogin =
                    if (hostCallbacks.loginWired) {
                        { navController.navigateToLogin() }
                    } else {
                        null
                    },
                onNavigateToProfileEdit =
                    if (hostCallbacks.profileEditWired) {
                        { navController.navigateToProfileEdit() }
                    } else {
                        null
                    },
                onNavigateToUrl = { url -> handleUrl(context, url, hostCallbacks.urlHandling) },
                onNavigateToProfile =
                    if (hostCallbacks.profileWired) {
                        { clientUserId -> navController.onClientProfileTap(clientUserId) }
                    } else {
                        null
                    },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        // Consume the insets this Scaffold already applied so the embedded
                        // Octopus content doesn't re-pad for the same system bars / IME
                        // (nested-Scaffold double-padding).
                        .consumeWindowInsets(innerPadding),
            )
        }
    }
}

/**
 * A host-owned bottom bar sitting under the embedded community, for `qa-preset-fullscreen-3`.
 *
 * Stands for the app bar a real host already has when it pushes the community as one destination
 * among its own. It carries a test id so a QA run can assert the bar is on screen — an inset check
 * against a bar that silently failed to render would pass for the wrong reason.
 */
@Composable
private fun HostBottomBar() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 12.dp)
                .testTag("fullscreen-host-bottom-bar"),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("Home", "Search", "Profile").forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun NavController.navigateToLogin() = navigate(LoginRoute)

fun NavController.navigateToProfileEdit() = navigate(EditUserRoute)

fun handleUrl(
    context: Context,
    url: String,
    mode: SDKConfig.UrlHandling,
): UrlOpeningStrategy =
    when (mode) {
        SDKConfig.UrlHandling.ForceHandledByOctopus -> UrlOpeningStrategy.HandledByOctopus

        SDKConfig.UrlHandling.ForceHandledByApp -> {
            // The host claims the link: open it with a plain ACTION_VIEW (browser). Via
            // `openUrlOrCopy` so the AOSP emulator images the QA pipeline runs on — which ship with
            // no browser at all — fall back to the clipboard with a message, rather than swallowing
            // the tap in a `runCatching` that leaves the tester staring at a button that does
            // nothing. Same helper, and so the same behaviour, as every other outbound link here.
            openUrlOrCopy(context, url, clipLabel = "Octopus link")
            UrlOpeningStrategy.HandledByApp
        }

        SDKConfig.UrlHandling.Default -> {
            val uri = url.toUri()
            when {
                uri.host == "www.octopuscommunity.com" && uri.path == "/contact" -> {
                    // Same reasoning, one degree worse: a bare emulator has no mail client either,
                    // and an unguarded ACTION_VIEW on a `mailto:` there is an
                    // ActivityNotFoundException — a crash, not a dead button.
                    openUrlOrCopy(
                        context,
                        url = "mailto:contact@octopuscommunity.com?subject=Contact",
                        clipLabel = "Contact email",
                    )
                    UrlOpeningStrategy.HandledByApp
                }

                else -> UrlOpeningStrategy.HandledByOctopus
            }
        }
    }

/**
 * Navigates [navController] to [postId] exactly once per instance of the calling composition —
 * the deep-link half of the catalog's three presentation presets (`qa-preset-fullscreen-2`,
 * `qa-preset-modal-2`, `qa-preset-sheet-2`).
 *
 * A plain `LaunchedEffect(postId)` is a Back-button trap, not a style nit. `NavHost` composes only
 * the top entry, so pushing the post destination DISPOSES the presenting route's composition;
 * pressing Back pops the post, the route recomposes with the same non-null id, the effect launches
 * again, and the post is pushed straight back. Back never escapes.
 *
 * `rememberSaveable` is what breaks the loop, and it is load-bearing in two different ways
 * depending on the caller. On the pushed route it is backed by the `NavBackStackEntry`'s saved
 * state, which OUTLIVES the disposed composition — so the restored flag is already `true` on the
 * way back. In the host-owned sheet and modal there is no back-stack entry, but the same flag also
 * survives a configuration change, which is the rotation re-fire the review flagged separately.
 * Leaving the container drops the state, so re-opening a preset deep-links again, as it should.
 */
@Composable
internal fun DeepLinkToPostOnce(
    postId: String?,
    navController: NavHostController,
) {
    // Keyed on the id: a container re-opened on a DIFFERENT post is a new deep link, not a
    // re-fire of the consumed one.
    var consumed by rememberSaveable(postId) { mutableStateOf(false) }
    LaunchedEffect(postId) {
        if (consumed) return@LaunchedEffect
        postId?.takeUnless { it.isBlank() }?.let {
            consumed = true
            navController.navigateToOctopusPost(it)
        }
    }
}
