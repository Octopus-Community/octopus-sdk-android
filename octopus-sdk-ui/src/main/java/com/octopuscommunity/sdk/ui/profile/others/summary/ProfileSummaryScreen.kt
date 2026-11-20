package com.octopuscommunity.sdk.ui.profile.others.summary

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.test.mock.MockProfiles
import com.octopuscommunity.sdk.ui.OctopusDestination.ProfileAvatar
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerKey
import com.octopuscommunity.sdk.ui.common.medias.viewer.LocalImageViewerController
import com.octopuscommunity.sdk.ui.common.medias.viewer.openOrElse
import com.octopuscommunity.sdk.ui.common.snackbar.OctopusSnackbarHost
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.ConstrainedContentColumn
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusScaffold
import com.octopuscommunity.sdk.ui.components.OctopusScreenContent
import com.octopuscommunity.sdk.ui.components.OctopusScreenState
import com.octopuscommunity.sdk.ui.components.OctopusScreenStateDefaults
import com.octopuscommunity.sdk.ui.components.OctopusSystemBackHandler
import com.octopuscommunity.sdk.ui.components.OctopusTabs
import com.octopuscommunity.sdk.ui.components.OctopusTopAppBar
import com.octopuscommunity.sdk.ui.components.octopusScreenStateItem
import com.octopuscommunity.sdk.ui.profile.comments.ProfileCommentsList
import com.octopuscommunity.sdk.ui.profile.comments.emptyCommentsItem
import com.octopuscommunity.sdk.ui.profile.comments.visibleOtherUserCommentsFeedId
import com.octopuscommunity.sdk.ui.profile.components.ProfileSummary
import com.octopuscommunity.sdk.ui.profile.components.zoomableAvatar
import com.octopuscommunity.sdk.ui.profile.others.menu.ProfileMenuSheet
import com.octopuscommunity.sdk.ui.profile.posts.ProfilePostsList
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSummaryScreen(
    navController: NavHostController,
    userId: String? = null,
    modifier: Modifier = Modifier,
    clientUserId: String? = null,
) = OctopusContent(navController) {
    val viewModel =
        octopusViewModel<ProfileSummaryViewModel>(
            key = "profileSummary_${userId ?: clientUserId}",
            parameters = { parametersOf(userId, clientUserId) },
        )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollState = rememberScrollState()
    val titleFadeThresholdPx = with(LocalDensity.current) { 100.dp.toPx() }
    val titleAlpha = (scrollState.value / titleFadeThresholdPx).coerceIn(0f, 1f)

    OctopusScaffold(
        modifier = modifier,
        viewModel = viewModel,
        topBar = { contentVisible ->
            OctopusTopAppBar(
                title = {
                    Box(
                        modifier =
                            Modifier.graphicsLayer {
                                alpha = titleAlpha
                            },
                    ) {
                        OctopusTheme.topAppBar.title(
                            if (contentVisible) state.profile?.nickname ?: "" else "",
                        )
                    }
                },
                // Route through the ViewModel's NavigateUp event (not a direct
                // navController.navigateUp()) so it funnels through OctopusScreenContent's
                // handler, which falls back to the host onBack when this screen is the
                // START/root of the back stack (bridge mode, e.g. navigateToOctopusProfile /
                // navigateToOctopusProfileByClientUserId). A direct navigateUp() would be a
                // dead no-op there. Mirrors GroupDetailsScreen / ActivityScreen.
                onNavigationIconClick = { viewModel.navigateUp() },
                actions = {
                    if (state.hasMoreActions) {
                        IconButton(onClick = { viewModel.showMenuSheet(true) }) {
                            Icon(
                                painter = OctopusTheme.icons.moreActions(),
                                contentDescription = stringResource(R.string.accessibility_common_more),
                            )
                        }
                    }
                },
                contentVisible = contentVisible,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        ProfileSummaryContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            navController = navController,
            userId = userId,
            clientUserId = clientUserId,
            scrollState = scrollState,
        )
    }
}

@Composable
fun ProfileSummaryContent(
    navController: NavHostController,
    userId: String?,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    clientUserId: String? = null,
) = OctopusScreenContent(
    modifier = modifier,
    navController = navController,
) { screenViewModel ->
    val viewModel =
        octopusViewModel<ProfileSummaryViewModel>(
            key = "profileSummary_${userId ?: clientUserId}",
            parameters = { parametersOf(userId, clientUserId) },
        )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = state.profile
    val imageViewer = LocalImageViewerController.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    // System back: the NavHost pops on its own (keeping the predictive-back preview) except at
    // the SDK back-stack root, where NavigateUp falls back to the host `onBack` (bridge mode).
    // Never enabled there without a host `onBack` (internal change). See OctopusSystemBackHandler.
    OctopusSystemBackHandler(navController = navController, onBack = viewModel::navigateUp)

    when {
        profile != null -> {
            // Cap & center the profile column on large screens (internal ticket).
            ConstrainedContentColumn {
                ProfileSummary(
                    modifier = Modifier.fillMaxSize(),
                    profile = profile,
                    communityConfig = state.communityConfig,
                    scrollState = scrollState,
                    editable = false,
                    onAvatarClick = { source ->
                        imageViewer.openOrElse(
                            key = ImageViewerKey(kind = ImageViewerKey.Kind.Avatar, id = profile.userId),
                            media = profile.zoomableAvatar?.let { Media.Image(source = it) },
                            source = source,
                        ) { viewModel.navigateTo(ProfileAvatar(userId = profile.userId)) }
                    },
                    onShowGamificationSheet = screenViewModel::showGamificationSheet,
                ) {
                    // Comments tab on another member's profile is gated by the community config
                    // `showCommentsOnOtherProfiles` (and requires an available, permitted comment feed) — mirrors
                    // iOS ProfileSummaryView's `showCommentsTab`. Off => Posts only, and OctopusTabs then
                    // drops its tab row: the posts sit directly under the header (internal ticket). Turned off
                    // while the Comments page is shown, the pager falls back to Posts.
                    //
                    // The rule is shared with ActivityScreen's other-user mode (the Unified Profile
                    // path onto the same member) so the tab can never depend on which route the tap
                    // took — see [visibleOtherUserCommentsFeedId].
                    val commentsFeedId =
                        visibleOtherUserCommentsFeedId(
                            commentsFeedId = state.profile?.commentsFeeds?.descendingId,
                            showCommentsOnOtherProfiles =
                                state.communityConfig?.showCommentsOnOtherProfiles == true,
                        )
                    val showComments = commentsFeedId != null
                    val postsLabel = stringResource(R.string.profile_tabs_posts)
                    val commentsLabel = stringResource(R.string.profile_tabs_comments)
                    OctopusTabs(
                        modifier = Modifier.fillMaxSize(),
                        tabs =
                            if (showComments) {
                                listOf(postsLabel, commentsLabel)
                            } else {
                                listOf(postsLabel)
                            },
                    ) { page ->
                        when (page) {
                            0 ->
                                state.profile?.postsFeeds?.descendingId?.let { feedId ->
                                    ProfilePostsList(
                                        feedId = feedId,
                                        showModeratedPosts = false,
                                        emptyPlaceHolder = {
                                            octopusScreenStateItem(
                                                illustration = {
                                                    OctopusTheme.icons.screenStates.emptyContent()
                                                },
                                                text = R.string.profile_posts_empty_state_other,
                                                verticalPadding =
                                                    OctopusScreenStateDefaults.ProfilePadding,
                                            )
                                        },
                                        contentPadding = PaddingValues(top = 8.dp),
                                    )
                                }

                            1 ->
                                // Only reachable when showComments added the Comments tab above,
                                // reusing the own-profile ProfileCommentsList (internal ticket).
                                commentsFeedId?.let { feedId ->
                                    ProfileCommentsList(
                                        modifier = Modifier.fillMaxSize(),
                                        feedId = feedId,
                                        isOwnProfile = false,
                                        emptyPlaceHolder = {
                                            emptyCommentsItem(R.string.profile_comments_empty_state_other)
                                        },
                                        contentPadding = PaddingValues(top = 8.dp),
                                    )
                                }
                        }
                    }
                }
            }
        }

        state.lookupFailed || state.profileNotFound -> {
            // Generic error state, never an indefinite spinner and never the connected user's own
            // profile: either the clientUserId entry point didn't resolve to an Octopus id
            // (lookupFailed — unknown/deleted mapping, network failure, or the community not
            // exposing client ids), or the plain-userId entry point's first fetch settled with no
            // profile ever found (profileNotFound — stale/unknown/deleted id).
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
                verticalPadding = OctopusScreenStateDefaults.ProfilePadding,
                action = stringResource(R.string.common_retry) to viewModel::retryFirstLoad,
            )
        }

        else -> {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = OctopusScreenStateDefaults.ProfileLoaderOffset),
            )
        }
    }

    OctopusSnackbarHost(
        modifier = Modifier.align(Alignment.BottomCenter),
        screenViewModel = screenViewModel,
    )
    if (state.showMenuSheet) {
        ProfileMenuSheet(
            profile = state.profile,
            onDismiss = { viewModel.showMenuSheet(false) },
            onBlocked = { viewModel.navigateUp() },
        )
    }
}

@Preview
@Composable
private fun ProfileSummaryScreenPreview() {
    OctopusTheme {
        ProfileSummaryScreen(
            userId = MockProfiles.default.userId,
            navController = rememberNavController(),
        )
    }
}
