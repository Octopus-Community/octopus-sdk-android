package com.octopuscommunity.sdk.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostClicked
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.snackbar.OctopusSnackbarHost
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.GamificationAutoPrompt
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.ConstrainedContentColumn
import com.octopuscommunity.sdk.ui.components.OctopusButtonDefaults
import com.octopuscommunity.sdk.ui.components.OctopusCreatePostBar
import com.octopuscommunity.sdk.ui.components.OctopusMediumOutlinedButton
import com.octopuscommunity.sdk.ui.components.OctopusNavigationHandler
import com.octopuscommunity.sdk.ui.components.OctopusScreenContent
import com.octopuscommunity.sdk.ui.components.OctopusScreenState
import com.octopuscommunity.sdk.ui.components.OctopusScreenStateDefaults
import com.octopuscommunity.sdk.ui.components.UrlOpeningStrategy
import com.octopuscommunity.sdk.ui.octopusComposables
import com.octopuscommunity.sdk.ui.posts.list.PostsList
import com.octopuscommunity.sdk.ui.utils.OctopusMotion
import com.octopuscommunity.sdk.ui.utils.onScrollDownChanged
import org.koin.core.parameter.parametersOf

/**
 * Main content container for the home screen.
 *
 * **Note:** For integrated navigation only. You need to include the Octopus Navigation by calling
 * [NavGraphBuilder.octopusComposables] inside your NavHost to use this Composable.
 *
 * This component handles the core functionality of the home screen, including state management,
 * animations, and layout coordination. It provides a rich user experience with feed filtering,
 * post browsing, and content creation capabilities.
 *
 * ## Features:
 * - **Animated feed filter bar**: Slides in/out based on scroll direction
 * - **Post list with pagination**: Displays posts from the selected feed with infinite scrolling
 * - **Loading and error states**: Handles data fetching states gracefully
 * - **Bottom action bar**: Contains profile avatar and post creation button
 * - **Pull-to-refresh**: Allows users to refresh content manually
 *
 * ## Embedding under your own `Scaffold`
 * The Octopus screens manage their own window insets (they re-apply `imePadding()` so the comment
 * composer sits right above the keyboard). When you host this content — or the sub-screens
 * registered by [com.octopuscommunity.sdk.ui.octopusComposables] — inside your own `Scaffold`
 * (e.g. under a bottom navigation bar) and pass the Scaffold's `innerPadding` to [modifier], you
 * MUST also consume those insets so they are not applied twice:
 * ```kotlin
 * Scaffold(bottomBar = { /* … */ }) { innerPadding ->
 *     OctopusHomeContent(
 *         navController = navController,
 *         modifier = Modifier
 *             .fillMaxSize()
 *             .padding(innerPadding)
 *             .consumeWindowInsets(innerPadding), // <-- required for nested Scaffolds
 *     )
 * }
 * ```
 * Without `consumeWindowInsets`, the system-bar/IME insets are counted twice, leaving a gap below
 * the composer and floating it well above the keyboard. This mirrors the standard Compose guidance
 * for nested `Scaffold`s.
 *
 * Missing optional login or host profile-edit callbacks never prevent opening a screen. A requested
 * action without its callback logs the missing parameter once, then throws the documented
 * [com.octopuscommunity.sdk.MissingOnNavigateToLoginException] or
 * [com.octopuscommunity.sdk.MissingOnNavigateToProfileEditException] at action time.
 *
 * @param navController Navigation controller for handling screen transitions
 * @param modifier Optional [Modifier] for customizing the layout. When hosting under your own
 * `Scaffold`, pass `Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)` (see above).
 * @param contentPadding Padding values for the bottom floating actions area
 * @param onNavigateToLogin Called when OctopusSDK needs a logged in user.
 * @param onNavigateToProfileEdit Called when the user tries to modify fields related to their profile.
 * @param onNavigateToClientObject Called when a user taps on a client object post button.
 * @param onNavigateToUrl Called when a user tries to open a link inside the community.
 * @param onNavigateToProfile Called when a user taps a member's profile (including their own),
 * with that member's `clientUserId`, so the host can open its own profile screen. Unified Profile
 * requires BOTH the community's backend flag (`CommunityConfig.exposeClientUserId`) and this
 * callback. When active, members without a client id (guest / BO / admin) open the Octopus activity
 * screen; this callback never receives a `null` id. With the flag off or this callback absent,
 * the SDK keeps native profiles.
 */
@Composable
fun OctopusHomeContent(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = OctopusHomeDefaults.contentPadding(),
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToProfileEdit: ((fieldToEdit: ProfileField?) -> Unit)? = null,
    onNavigateToClientObject: ((id: String) -> Unit)? = null,
    onNavigateToUrl: ((url: String) -> UrlOpeningStrategy) = { UrlOpeningStrategy.HandledByOctopus },
    onNavigateToProfile: ((clientUserId: String) -> Unit)? = null,
) = OctopusNavigationHandler(
    onNavigateToLogin = onNavigateToLogin,
    onNavigateToProfileEdit = onNavigateToProfileEdit,
    onNavigateToClientObject = onNavigateToClientObject,
    onNavigateToUrl = onNavigateToUrl,
    onNavigateToProfile = onNavigateToProfile,
) {
    OctopusHomeContent(
        modifier = modifier,
        navController = navController,
        contentPadding = contentPadding,
    )
}

@Composable
internal fun OctopusHomeContent(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = OctopusHomeDefaults.contentPadding(),
) = OctopusScreenContent(
    modifier = modifier,
    navController = navController,
    contentPadding = contentPadding,
    gamificationAutoPrompt = GamificationAutoPrompt.Home,
    communityRoot = true,
) { screenViewModel ->
    val density = LocalDensity.current
    val viewModel = octopusViewModel<OctopusHomeViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val feed = state.feed

    var isScrollingDown by rememberSaveable { mutableStateOf(false) }
    var topPadding by rememberSaveable { mutableIntStateOf(0) }
    var bottomPadding by rememberSaveable { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (feed != null) {
            val postsViewModel =
                octopusViewModel<HomePostsListViewModel>(
                    key = "homePosts_${feed.id}",
                    parameters = { parametersOf(feed) },
                )
            // Cap & center the feed column on large screens; margins show the separator color (internal ticket).
            ConstrainedContentColumn {
                PostsList(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .onScrollDownChanged { isScrollingDown = it },
                    viewModel = postsViewModel,
                    source = PostClicked.Source.FEED,
                    showModeratedPosts = false,
                    contentPadding =
                        PaddingValues(
                            top = with(density) { topPadding.toDp() },
                            bottom = with(density) { bottomPadding.toDp() } + 16.dp,
                        ),
                    pullToRefresh = true,
                    enableProfileClick = true,
                    showTopic = true,
                    onScrollToTop = { isScrollingDown = false },
                )
            }
        } else if (state.isLoading) {
            // Nothing cached and the root-feed fetch still in flight. Also what a Retry paints, so
            // the button below reports that it did something (Screen states spec).
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = OctopusScreenStateDefaults.FeedLoaderOffset),
            )
        } else if (state.error != null) {
            // No feed to render: the root-feed fetch failure takes the empty screen, which is
            // exactly when the ViewModel routes it here rather than to the snackbar (see
            // OctopusHomeViewModel). Before this branch existed the screen stayed blank whenever
            // the very first root-feed fetch failed; it then showed a dead end with no way out,
            // which is why it now carries the shared screen state and its Retry.
            OctopusScreenState(
                modifier = Modifier.align(Alignment.TopCenter),
                illustration =
                    if (state.loadFailureIsNoNetwork) {
                        OctopusTheme.icons.screenStates.networkError()
                    } else {
                        OctopusTheme.icons.screenStates.error()
                    },
                text =
                    stringResource(
                        if (state.loadFailureIsNoNetwork) {
                            R.string.error_no_network_formal
                        } else {
                            R.string.error_unknown_formal
                        },
                    ),
                verticalPadding = OctopusScreenStateDefaults.FeedErrorPadding,
                action = stringResource(R.string.common_retry) to viewModel::retryFirstLoad,
            )
        }
        AnimatedVisibility(
            visible = !isScrollingDown,
            enter =
                slideInVertically(tween(OctopusMotion.MEDIUM_DURATION_MS)) { -it } +
                    fadeIn(tween(OctopusMotion.MEDIUM_DURATION_MS)),
            exit =
                slideOutVertically(tween(OctopusMotion.MEDIUM_DURATION_MS)) { -it } +
                    fadeOut(tween(OctopusMotion.MEDIUM_DURATION_MS)),
        ) {
            // An opaque bar, not a floating button: it slides back in over a scrolled feed, and
            // with no background the posts below showed through around the button and ran under
            // it (QA, 2026-10-02).
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(OctopusTheme.colorScheme.background)
                        .onSizeChanged { size -> topPadding = size.height }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OctopusMediumOutlinedButton(
                    text = stringResource(R.string.feed_groups_button),
                    icon = OctopusTheme.icons.groups.openList(),
                    colors =
                        OctopusButtonDefaults.outlineButtonColors(
                            containerColor = OctopusTheme.colorScheme.background,
                        ),
                    onClick = viewModel::navigateToGroups,
                )
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
    ) {
        OctopusSnackbarHost(screenViewModel)
        OctopusCreatePostBar(
            isScrollingDown = isScrollingDown,
            onCreatePost = viewModel::navigateToCreatePost,
            contentPadding = contentPadding,
            displayCreateButton = state.canCreatePost,
            onHeightChanged = { bottomPadding = it },
        )
    }
}

/**
 * Default configuration values for Octopus home screen components.
 */
object OctopusHomeDefaults {
    fun contentPadding(
        start: Dp = 0.dp,
        top: Dp = 0.dp,
        end: Dp = 0.dp,
        bottom: Dp = 0.dp,
    ) = PaddingValues(start = start, top = top, end = end, bottom = bottom)
}

@Preview(showBackground = true)
@Composable
private fun OctopusHomeContentPreview() {
    OctopusTheme {
        OctopusHomeContent(navController = rememberNavController())
    }
}
