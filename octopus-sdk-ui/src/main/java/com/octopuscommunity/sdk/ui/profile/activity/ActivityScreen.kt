package com.octopuscommunity.sdk.ui.profile.activity

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.domain.model.Profile
import com.octopuscommunity.sdk.ui.OctopusDestination.HowToReportContent
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.snackbar.OctopusSnackbarHost
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.common.viewmodel.unifiedProfileActive
import com.octopuscommunity.sdk.ui.components.LocalNavigationHandler
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusPoweredBy
import com.octopuscommunity.sdk.ui.components.OctopusScaffold
import com.octopuscommunity.sdk.ui.components.OctopusScreenContent
import com.octopuscommunity.sdk.ui.components.OctopusScreenState
import com.octopuscommunity.sdk.ui.components.OctopusScreenStateDefaults
import com.octopuscommunity.sdk.ui.components.OctopusSystemBackHandler
import com.octopuscommunity.sdk.ui.components.OctopusTabs
import com.octopuscommunity.sdk.ui.components.OctopusTopAppBar
import com.octopuscommunity.sdk.ui.components.octopusScreenStateItem
import com.octopuscommunity.sdk.ui.notifications.list.NotificationsList
import com.octopuscommunity.sdk.ui.posts.list.emptyPostsItem
import com.octopuscommunity.sdk.ui.profile.comments.ProfileCommentsList
import com.octopuscommunity.sdk.ui.profile.comments.emptyCommentsItem
import com.octopuscommunity.sdk.ui.profile.comments.visibleOtherUserCommentsFeedId
import com.octopuscommunity.sdk.ui.profile.posts.ProfilePostsList
import org.koin.core.parameter.parametersOf

/**
 * Community activity screen (Unified Profile, internal ticket).
 *
 * Presents a user's community activity as an activity-centric entry point — no profile header, no
 * gamification (exposed to the host via the read-only community-data API instead). Two modes,
 * derived from [userId]:
 * - **Connected user** ([userId] `null`, from the home floating button when Unified Profile is
 *   active — the community exposes client user ids AND the host wired `onNavigateToProfile`): a
 *   static "Activity" title, a top-right overflow menu (profile · legal · report), and three tabs
 *   (Notifications, Comments, Posts — Comments in the middle, internal ticket).
 * - **Other user** ([userId] set, opened on demand via
 *   [com.octopuscommunity.sdk.ui.navigateToOctopusActivity]): that member's content, with no
 *   header, no overflow menu and no notifications tab. Posts only, titled "{author}'s Posts" — or
 *   Posts + Comments tabs titled "{author}'s Activity" when the community exposes other members'
 *   comments, exactly like
 *   [ProfileSummaryScreen][com.octopuscommunity.sdk.ui.profile.others.summary.ProfileSummaryScreen]
 *   (internal ticket).
 *
 * It is backed by the dedicated [ActivityViewModel] — not the profile-summary ViewModel — so it
 * emits its own [com.octopuscommunity.sdk.domain.model.OctopusEvent.ScreenDisplayed] event.
 *
 * @param navController Navigation controller used to drive screen transitions
 * @param userId Id of the user whose activity to display, or `null` for the connected user
 * @param modifier The [Modifier] to be applied to this screen
 * @param selectedTabIndex Index of the initially selected tab in connected-user mode
 * (0 = Notifications, 1 = Comments, 2 = Posts); ignored in other-user mode, whose own tabs always
 * open on Posts. When a [userId] /
 * [clientUserId] entry point resolves to the connected user (a self-tap), the landing tab follows
 * the unseen-notifications rule instead (see `connectedUserInitialTabIndex`)
 * @param clientUserId The host app's own id for the member whose activity to display (Unified
 * Profile), resolved to an Octopus id via the client-user-id lookup; mutually exclusive with
 * [userId]. `null` unless opened via
 * [com.octopuscommunity.sdk.ui.navigateToOctopusActivityByClientUserId].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    navController: NavHostController,
    userId: String? = null,
    modifier: Modifier = Modifier,
    selectedTabIndex: Int = 0,
    clientUserId: String? = null,
) = OctopusContent(navController) {
    val viewModel =
        octopusViewModel<ActivityViewModel>(
            key = "activity_${userId ?: clientUserId}",
            parameters = { parametersOf(userId, clientUserId) },
        )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val otherUserCommentsFeedId =
        if (state.isOtherUser) {
            visibleOtherUserCommentsFeedId(
                commentsFeedId = state.profile?.commentsFeeds?.descendingId,
                showCommentsOnOtherProfiles = state.communityConfig?.showCommentsOnOtherProfiles == true,
            )
        } else {
            null
        }
    // Host callback wiring — gates the overflow's profile actions.
    val onNavigateToProfileWired = LocalNavigationHandler.current?.onNavigateToProfile != null
    val onNavigateToProfileEditWired = LocalNavigationHandler.current?.onNavigateToProfileEdit != null

    OctopusScaffold(
        modifier = modifier,
        viewModel = viewModel,
        topBar = { contentVisible ->
            OctopusTopAppBar(
                title =
                    if (state.isOtherUser) {
                        // While the member's profile is still preloading (no cache), show no title
                        // rather than an empty-name "'s Posts".
                        state.profile
                            ?.nickname
                            ?.let { nickname ->
                                // The title follows what the screen actually lists: "'s Activity"
                                // once the Comments tab is there, "'s Posts" while it stays
                                // posts-only. Same gate as the tabs below, so the two can't disagree.
                                val showsComments = otherUserCommentsFeedId != null
                                stringResource(
                                    if (showsComments) {
                                        R.string.profile_activity_screen_title
                                    } else {
                                        R.string.profile_posts_screen_title
                                    },
                                    nickname,
                                )
                            }.orEmpty()
                    } else {
                        stringResource(R.string.activity_screen_title)
                    },
                // Route through the ViewModel's NavigateUp event (not a direct
                // navController.navigateUp()) so it funnels through
                // OctopusScreenContent's handler, which falls back to the host
                // onBack when this screen is the START/root of the back stack
                // (bridge mode, e.g. navigateToOctopusActivity). A direct
                // navigateUp() would be a dead no-op there.
                onNavigationIconClick = { viewModel.navigateUp() },
                actions = {
                    // The overflow menu is a connected-user affordance only: another member's
                    // posts view carries no profile/legal/report actions.
                    if (!state.isOtherUser) {
                        // Both profile actions need Unified Profile ACTIVE (the backend
                        // exposeClientUserId flag AND onNavigateToProfile wired) plus a reachable
                        // host profile: a non-guest member with a client id (a guest / BO / admin
                        // gets neither). Without the
                        // unified gate, "Edit my profile" would fire the host's edit callback even
                        // when the community never turned Unified Profile on. Each item also needs its
                        // own host callback, so a menu entry never dead-ends.
                        val canOpenHostProfile =
                            unifiedProfileActive(
                                exposeClientUserId = state.communityConfig?.exposeClientUserId == true,
                                onNavigateToProfileWired = onNavigateToProfileWired,
                            ) &&
                                !state.isGuest &&
                                state.profile?.clientUserId != null
                        ActivityOverflowMenu(
                            // "View my profile" opens the host's profile screen (onNavigateToProfile).
                            showViewProfile = canOpenHostProfile,
                            // "Edit my profile" goes straight to the host's editor; shown only when
                            // onNavigateToProfileEdit is wired, so the item never dead-ends.
                            showEditProfile = canOpenHostProfile && onNavigateToProfileEditWired,
                            onViewProfile = { state.profile?.let(viewModel::navigateToProfile) },
                            // Unified Profile: open the host editor directly rather than the SDK's
                            // native edit screen — navigateToClientProfileEdit(null) invokes
                            // onNavigateToProfileEdit with no field (no confirmation dialog).
                            onEditProfile = { viewModel.navigateToClientProfileEdit(fieldToEdit = null) },
                            onCommunityGuidelines = viewModel::navigateToCommunityGuidelines,
                            onPrivacyPolicy = viewModel::navigateToPrivacyPolicy,
                            onTermsOfUse = viewModel::navigateToTermsOfUse,
                            onReportContent = { viewModel.navigateTo(HowToReportContent) },
                        )
                    }
                },
                contentVisible = contentVisible,
            )
        },
    ) { innerPadding ->
        ActivityContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            navController = navController,
            userId = userId,
            clientUserId = clientUserId,
            selectedTabIndex = selectedTabIndex,
            otherUserCommentsFeedId = otherUserCommentsFeedId,
        )
    }
}

@Composable
private fun ActivityContent(
    navController: NavHostController,
    userId: String?,
    clientUserId: String?,
    selectedTabIndex: Int,
    otherUserCommentsFeedId: String?,
    modifier: Modifier = Modifier,
) = OctopusScreenContent(
    modifier = modifier,
    navController = navController,
) { screenViewModel ->
    val viewModel =
        octopusViewModel<ActivityViewModel>(
            key = "activity_${userId ?: clientUserId}",
            parameters = { parametersOf(userId, clientUserId) },
        )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val screenState by screenViewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    // System back: the NavHost pops on its own (keeping the predictive-back preview) except at
    // the SDK back-stack root, where NavigateUp falls back to the host `onBack` (bridge mode).
    // Never enabled there without a host `onBack` (internal change). See OctopusSystemBackHandler.
    OctopusSystemBackHandler(navController = navController, onBack = viewModel::navigateUp)

    Column(modifier = Modifier.fillMaxSize()) {
        when {
            // Preloading another member's profile with nothing cached — show a loader instead of the
            // empty "'s Posts" header + Powered-by chrome.
            state.isOtherUser && state.isLoading && state.profile == null -> {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            // No profile means no feed id, so the posts list draws nothing: without this the screen
            // would be its header over a blank page.
            state.isOtherUser && state.hasProfileLoadFailure && state.profile == null -> {
                Box(modifier = Modifier.weight(1f)) {
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
                        action = stringResource(R.string.common_retry) to viewModel::retryProfileLoad,
                    )
                }
            }

            state.isOtherUser -> {
                // Same gate, same tab order (Posts, Comments) as ProfileSummaryScreen — the other
                // screen that can show this member. Which of the two a profile tap lands on depends
                // only on whether Unified Profile is active and whether the member has a client id,
                // neither of which has anything to say about their comments (internal ticket).
                val commentsFeedId = otherUserCommentsFeedId
                if (commentsFeedId != null) {
                    OctopusTabs(
                        modifier = Modifier.weight(1f),
                        tabs =
                            listOf(
                                stringResource(R.string.profile_tabs_posts),
                                stringResource(R.string.profile_tabs_comments),
                            ),
                    ) { page ->
                        when (page) {
                            0 ->
                                OtherUserPostsList(
                                    modifier = Modifier.fillMaxSize(),
                                    profile = state.profile,
                                )

                            1 ->
                                ProfileCommentsList(
                                    modifier = Modifier.fillMaxSize(),
                                    feedId = commentsFeedId,
                                    isOwnProfile = false,
                                    emptyPlaceHolder = {
                                        emptyCommentsItem(R.string.profile_comments_empty_state_other)
                                    },
                                    contentPadding = PaddingValues(top = 8.dp),
                                )
                        }
                    }
                } else {
                    // No tab bar at all when the community keeps other members' comments hidden —
                    // the screen's historical posts-only look.
                    OtherUserPostsList(
                        modifier = Modifier.weight(1f),
                        profile = state.profile,
                    )
                }
                OctopusPoweredBy(communityConfig = screenState.communityConfig)
            }

            else -> {
                // Own content, so no community-config gate — but the tab still requires a usable feed
                // id: "" until the backend ships `descCommentFeedId`, null on the first launch after
                // the 109->111 migration. See CurrentUserProfileTab for the same reasoning.
                val commentsFeedId =
                    state.profile
                        ?.commentsFeeds
                        ?.descendingId
                        ?.takeIf { it.isNotEmpty() }
                val tabs =
                    buildList {
                        add(ActivityTab.NOTIFICATIONS)
                        if (commentsFeedId != null) add(ActivityTab.COMMENTS)
                        add(ActivityTab.POSTS)
                    }
                OctopusTabs(
                    modifier = Modifier.weight(1f),
                    tabs = tabs.map { stringResource(it.labelRes) },
                    // A userId/clientUserId entry that resolved to the connected user (self-tap)
                    // lands per the unseen-notifications rule; otherwise the destination's own tab.
                    // One-shot by construction: these tabs first compose only after the mode
                    // resolves (the ViewModel sets the landing tab atomically with the mode flip),
                    // and the pager only reads its initial page once — a late resolution can never
                    // override a tab the user already picked.
                    //
                    // Both values are host-facing indices (Notifications=0, Comments=1, Posts=2), so
                    // they are resolved against the visible tabs rather than used as positions.
                    initialTabIndex =
                        tabs
                            .indexOfFirst {
                                it.publicIndex == (state.connectedUserInitialTabIndex ?: selectedTabIndex)
                            }.coerceAtLeast(0),
                ) { page ->
                    when (tabs[page]) {
                        ActivityTab.NOTIFICATIONS ->
                            NotificationsList(
                                modifier = Modifier.fillMaxSize(),
                                pullToRefresh = false,
                            )

                        ActivityTab.COMMENTS ->
                            // Reuses the same ProfileCommentsList as the current-user profile summary
                            // (internal ticket). This tab only ever shows the connected user's own comments,
                            // hence the "_self" empty-state wording.
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

                        ActivityTab.POSTS ->
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
                    }
                }
                OctopusPoweredBy(communityConfig = screenState.communityConfig)
            }
        }
    }

    OctopusSnackbarHost(
        modifier = Modifier.align(Alignment.BottomCenter),
        screenViewModel = screenViewModel,
    )
}

/**
 * Posts feed of another member (other-user mode of the [ActivityScreen]). Mirrors the posts tab of
 * the profile summary — moderated posts hidden, other-user empty placeholder — but without the
 * profile header. It is the whole screen when the community keeps other members' comments hidden,
 * and the first of two tabs otherwise.
 */
@Composable
private fun OtherUserPostsList(
    profile: Profile?,
    modifier: Modifier = Modifier,
) {
    profile?.postsFeeds?.descendingId?.let { feedId ->
        ProfilePostsList(
            modifier = modifier,
            feedId = feedId,
            showModeratedPosts = false,
            emptyPlaceHolder = {
                octopusScreenStateItem(
                    illustration = { OctopusTheme.icons.screenStates.emptyContent() },
                    text = R.string.profile_posts_empty_state_other,
                    verticalPadding = OctopusScreenStateDefaults.ProfilePadding,
                )
            },
            contentPadding = PaddingValues(top = 8.dp),
        )
    }
}

/**
 * Top-right overflow menu of the [ActivityScreen], organised in three visually-separated blocks
 * (per internal ticket): profile actions, community legal links, and content reporting.
 */
@Composable
private fun ActivityOverflowMenu(
    showViewProfile: Boolean,
    showEditProfile: Boolean,
    onViewProfile: () -> Unit,
    onEditProfile: () -> Unit,
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
            // Block 1 — profile actions (each shown only when its host target is reachable)
            if (showViewProfile || showEditProfile) {
                if (showViewProfile) {
                    ActivityMenuItem(
                        textRes = R.string.menu_view_profile,
                        iconRes = R.drawable.ic_octopus_activity_menu_view_profile,
                    ) {
                        expanded = false
                        onViewProfile()
                    }
                }
                if (showEditProfile) {
                    ActivityMenuItem(
                        textRes = R.string.menu_edit_profile,
                        iconRes = R.drawable.ic_octopus_activity_menu_edit_profile,
                    ) {
                        expanded = false
                        onEditProfile()
                    }
                }

                HorizontalDivider(color = OctopusTheme.colorScheme.gray300)
            }

            // Block 2 — community legal links (Figma order: terms, privacy, guidelines)
            ActivityMenuItem(
                textRes = R.string.settings_terms_of_use,
                iconRes = R.drawable.ic_octopus_activity_menu_terms_of_use,
            ) {
                expanded = false
                onTermsOfUse()
            }
            ActivityMenuItem(
                textRes = R.string.settings_privacy_policy,
                iconRes = R.drawable.ic_octopus_activity_menu_privacy_policy,
            ) {
                expanded = false
                onPrivacyPolicy()
            }
            ActivityMenuItem(
                textRes = R.string.settings_community_guidelines,
                iconRes = R.drawable.ic_octopus_activity_menu_community_guidelines,
            ) {
                expanded = false
                onCommunityGuidelines()
            }

            HorizontalDivider(color = OctopusTheme.colorScheme.gray300)
            // NB: gray300, not gray100 — gray100 is pure white in light mode, so a divider painted
            // with it is invisible on the dropdown's white surface (PO feedback 2026-07-20).

            // Block 3 — report
            ActivityMenuItem(
                textRes = R.string.settings_report_content,
                iconRes = R.drawable.ic_octopus_activity_menu_report_content,
            ) {
                expanded = false
                onReportContent()
            }
        }
    }
}

@Composable
private fun ActivityMenuItem(
    textRes: Int,
    iconRes: Int,
    onClick: () -> Unit,
) = DropdownMenuItem(
    text = { Text(stringResource(textRes)) },
    leadingIcon = {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            // DropdownMenuItem's default leading-icon tint is M3's onSurfaceVariant (dimmer than
            // the label text), but Figma specs the icon at the same near-black weight as the text
            // (grey/100, matching gray900) — pin it explicitly to match Figma and iOS's native
            // Menu rows, which render icon and label in the same color (PO feedback follow-up).
            tint = OctopusTheme.colorScheme.gray900,
        )
    },
    onClick = onClick,
)

/**
 * The tabs of the Activity screen, in display order — Comments sits in the middle, matching iOS. Each
 * destination owns its own order; nothing is shared with [CurrentUserProfileTab]
 * [com.octopuscommunity.sdk.ui.profile.current.summary].
 *
 * [publicIndex] is the host-facing index carried by
 * [com.octopuscommunity.sdk.ui.OctopusDestination.Activity.selectedTabIndex], kept separate from the
 * on-screen position, which shifts when [COMMENTS] is not displayed.
 */
private enum class ActivityTab(
    val publicIndex: Int,
    @StringRes val labelRes: Int,
) {
    NOTIFICATIONS(0, R.string.profile_tabs_notifications),
    COMMENTS(1, R.string.profile_tabs_comments),
    POSTS(2, R.string.profile_tabs_posts),
}

@Preview
@Composable
private fun ActivityScreenPreview() {
    OctopusTheme {
        ActivityScreen(navController = rememberNavController())
    }
}
