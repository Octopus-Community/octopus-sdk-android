package com.octopuscommunity.sdk.ui.profile.current.summary

import androidx.annotation.StringRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.ui.OctopusDestination.HowToReportContent
import com.octopuscommunity.sdk.ui.OctopusDestination.ProfileAvatar
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerKey
import com.octopuscommunity.sdk.ui.common.medias.viewer.LocalImageViewerController
import com.octopuscommunity.sdk.ui.common.medias.viewer.openOrElse
import com.octopuscommunity.sdk.ui.common.snackbar.OctopusSnackbarHost
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.GamificationAutoPrompt
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.ConstrainedContentColumn
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusScaffold
import com.octopuscommunity.sdk.ui.components.OctopusScreenContent
import com.octopuscommunity.sdk.ui.components.OctopusScreenStateDefaults
import com.octopuscommunity.sdk.ui.components.OctopusSystemBackHandler
import com.octopuscommunity.sdk.ui.components.OctopusTabs
import com.octopuscommunity.sdk.ui.components.OctopusTopAppBar
import com.octopuscommunity.sdk.ui.notifications.list.NotificationsList
import com.octopuscommunity.sdk.ui.posts.list.emptyPostsItem
import com.octopuscommunity.sdk.ui.profile.comments.ProfileCommentsList
import com.octopuscommunity.sdk.ui.profile.comments.emptyCommentsItem
import com.octopuscommunity.sdk.ui.profile.components.ProfileSummary
import com.octopuscommunity.sdk.ui.profile.posts.ProfilePostsList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentUserProfileSummaryScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    selectedTabIndex: Int = 0,
) = OctopusContent(navController) {
    val viewModel = octopusViewModel<CurrentUserProfileSummaryViewModel>()
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
                // navController.navigateUp()) so it funnels through
                // OctopusScreenContent's handler, which falls back to the host
                // onBack when this screen is the START/root of the back stack
                // (bridge mode — the host opened the SDK directly on the current-user
                // profile). A direct navigateUp() would be a dead no-op there.
                onNavigationIconClick = { viewModel.navigateUp() },
                actions = {
                    // Community legal links and content reporting.
                    ProfileOverflowMenu(
                        onCommunityGuidelines = viewModel::navigateToCommunityGuidelines,
                        onPrivacyPolicy = viewModel::navigateToPrivacyPolicy,
                        onTermsOfUse = viewModel::navigateToTermsOfUse,
                        onReportContent = { viewModel.navigateTo(HowToReportContent) },
                    )
                },
                contentVisible = contentVisible,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        CurrentUserProfileSummaryContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            navController = navController,
            selectedTabIndex = selectedTabIndex,
            scrollState = scrollState,
        )
    }
}

@Composable
fun CurrentUserProfileSummaryContent(
    navController: NavHostController,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    selectedTabIndex: Int = 0,
) = OctopusScreenContent(
    modifier = modifier,
    navController = navController,
    gamificationAutoPrompt = GamificationAutoPrompt.CurrentUserProfile,
) { screenViewModel ->
    val viewModel = octopusViewModel<CurrentUserProfileSummaryViewModel>()

    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    // System back: the NavHost pops on its own (keeping the predictive-back preview) except at
    // the SDK back-stack root, where NavigateUp falls back to the host `onBack` (bridge mode).
    // Never enabled there without a host `onBack` (internal change). See OctopusSystemBackHandler.
    OctopusSystemBackHandler(navController = navController, onBack = viewModel::navigateUp)

    val imageViewer = LocalImageViewerController.current
    state.profile?.let { profile ->
        // Cap & center the profile column on large screens (internal ticket).
        ConstrainedContentColumn {
            ProfileSummary(
                modifier = Modifier.fillMaxSize(),
                profile = profile,
                communityConfig = state.communityConfig,
                scrollState = scrollState,
                editable = true,
                editability = state.editability,
                onAvatarClick = { source ->
                    imageViewer.openOrElse(
                        key = ImageViewerKey(kind = ImageViewerKey.Kind.Avatar, id = profile.userId),
                        media = profile.picture?.let { Media.Image(source = it) },
                        source = source,
                    ) { viewModel.navigateTo(ProfileAvatar(userId = profile.userId)) }
                },
                onEditClick = viewModel::navigateToProfileEdit,
                onShowGamificationSheet = screenViewModel::showGamificationSheet,
            ) {
                // The Comments tab only exists once a usable comment feed is known: the id is "" until
                // the backend ships `descCommentFeedId`, and null on the first launch after the
                // 109->111 migration (cached rows carry NULL by design) — showing the label in either
                // case would put a permanently, or briefly, dead tab in front of every user.
                // Consequence to accept: the tab appears when the profile fetch lands, shifting the
                // position of Notifications for a user who switched tabs inside that short window.
                val commentsFeedId =
                    state.profile
                        ?.commentsFeeds
                        ?.descendingId
                        ?.takeIf { it.isNotEmpty() }
                val tabs =
                    buildList {
                        add(CurrentUserProfileTab.POSTS)
                        if (commentsFeedId != null) add(CurrentUserProfileTab.COMMENTS)
                        add(CurrentUserProfileTab.NOTIFICATIONS)
                    }
                OctopusTabs(
                    modifier = Modifier.fillMaxSize(),
                    tabs = tabs.map { stringResource(it.labelRes) },
                    // `selectedTabIndex` is the host-facing index (Posts=0, Comments=1,
                    // Notifications=2) and stays stable whatever is displayed, so it is resolved
                    // against the visible tabs instead of used as a position. An index whose tab is
                    // hidden (Comments) falls back to the first tab.
                    initialTabIndex = tabs.indexOfFirst { it.publicIndex == selectedTabIndex }.coerceAtLeast(0),
                ) { page ->
                    when (tabs[page]) {
                        CurrentUserProfileTab.POSTS ->
                            state.profile?.postsFeeds?.descendingId?.let { feedId ->
                                ProfilePostsList(
                                    modifier = Modifier.fillMaxSize(),
                                    feedId = feedId,
                                    showModeratedPosts = true,
                                    emptyPlaceHolder = {
                                        emptyPostsItem(
                                            text = R.string.profile_posts_empty_state_title_self_formal,
                                            onCreatePostClick = viewModel::navigateToCreatePost,
                                            displayCreatePost = state.canCreatePost,
                                            verticalPadding = OctopusScreenStateDefaults.ProfilePadding,
                                        )
                                    },
                                    contentPadding = PaddingValues(top = 8.dp),
                                )
                            }

                        CurrentUserProfileTab.COMMENTS ->
                            commentsFeedId?.let { feedId ->
                                ProfileCommentsList(
                                    modifier = Modifier.fillMaxSize(),
                                    feedId = feedId,
                                    emptyPlaceHolder = {
                                        emptyCommentsItem(R.string.profile_comments_empty_state_title_self_formal)
                                    },
                                    contentPadding = PaddingValues(top = 8.dp),
                                )
                            }

                        CurrentUserProfileTab.NOTIFICATIONS ->
                            NotificationsList(
                                modifier = Modifier.fillMaxSize(),
                                pullToRefresh = false,
                            )
                    }
                }
            }
        }
        OctopusSnackbarHost(
            modifier = Modifier.align(Alignment.BottomCenter),
            screenViewModel = screenViewModel,
        )
    }
}

/**
 * Top-right overflow menu of [CurrentUserProfileSummaryScreen] — the whole community Settings hub
 * (legal links and content reporting) shown inline as a dropdown.
 *
 * Uniformized with [com.octopuscommunity.sdk.ui.profile.activity.ActivityScreen]'s
 * `ActivityOverflowMenu` (PO feedback 2026-07-20/22): every row carries a leading icon and blocks
 * are separated by gray300 dividers (gray100 renders as pure white — invisible — on the dropdown's
 * white surface in light mode).
 */
@Composable
private fun ProfileOverflowMenu(
    onCommunityGuidelines: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onTermsOfUse: () -> Unit,
    onReportContent: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = OctopusTheme.icons.moreActions(),
                contentDescription = stringResource(R.string.accessibility_common_more),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            // Block 2 — community legal links (Figma order: terms, privacy, guidelines — same as
            // ActivityOverflowMenu, reusing its drawables for the shared rows)
            ProfileMenuItem(
                textRes = R.string.settings_terms_of_use,
                icon = { painterResource(R.drawable.ic_octopus_activity_menu_terms_of_use) },
            ) {
                expanded = false
                onTermsOfUse()
            }
            ProfileMenuItem(
                textRes = R.string.settings_privacy_policy,
                icon = { painterResource(R.drawable.ic_octopus_activity_menu_privacy_policy) },
            ) {
                expanded = false
                onPrivacyPolicy()
            }
            ProfileMenuItem(
                textRes = R.string.settings_community_guidelines,
                icon = { painterResource(R.drawable.ic_octopus_activity_menu_community_guidelines) },
            ) {
                expanded = false
                onCommunityGuidelines()
            }

            HorizontalDivider(color = OctopusTheme.colorScheme.gray300)

            // Block 3 — report
            ProfileMenuItem(
                textRes = R.string.settings_report_content,
                icon = { painterResource(R.drawable.ic_octopus_activity_menu_report_content) },
            ) {
                expanded = false
                onReportContent()
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    textRes: Int,
    icon: @Composable () -> Painter,
    textColor: Color? = null,
    onClick: () -> Unit,
) = DropdownMenuItem(
    text = {
        Text(
            text = stringResource(textRes),
            color = textColor ?: Color.Unspecified,
        )
    },
    leadingIcon = {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = icon(),
            contentDescription = null,
            // Pin the icon tint to the label's weight (or the label's override color, e.g. the
            // destructive actions) instead of M3's dimmer onSurfaceVariant default — matches
            // ActivityMenuItem / Figma / iOS's native Menu rows.
            tint = textColor ?: OctopusTheme.colorScheme.gray900,
        )
    },
    onClick = onClick,
)

/**
 * The tabs of the current user's profile, in display order.
 *
 * [publicIndex] is the value hosts pass through
 * [com.octopuscommunity.sdk.ui.OctopusDestination.CurrentUserProfileSummary.selectedTabIndex]. It is a
 * fixed public contract, so it is kept separate from the tab's position on screen — which shifts when
 * [COMMENTS] is not displayed.
 */
private enum class CurrentUserProfileTab(
    val publicIndex: Int,
    @StringRes val labelRes: Int,
) {
    POSTS(0, R.string.profile_tabs_posts),
    COMMENTS(1, R.string.profile_tabs_comments),
    NOTIFICATIONS(2, R.string.profile_tabs_notifications),
}

@Preview
@Composable
private fun CurrentUserProfileSummaryScreenPreview() {
    OctopusTheme {
        CurrentUserProfileSummaryScreen(navController = rememberNavController())
    }
}
