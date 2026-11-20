package com.octopuscommunity.sdk.ui.groups.details

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostClicked
import com.octopuscommunity.sdk.test.mock.MockTopics
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.snackbar.OctopusSnackbarHost
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.ConstrainedContentColumn
import com.octopuscommunity.sdk.ui.components.NavigationIconType
import com.octopuscommunity.sdk.ui.components.OctopusAccessDenied
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusCreatePostBar
import com.octopuscommunity.sdk.ui.components.OctopusExpandableText
import com.octopuscommunity.sdk.ui.components.OctopusMediumButton
import com.octopuscommunity.sdk.ui.components.OctopusScaffold
import com.octopuscommunity.sdk.ui.components.OctopusScreenContent
import com.octopuscommunity.sdk.ui.components.OctopusScreenStateDefaults
import com.octopuscommunity.sdk.ui.components.OctopusSystemBackHandler
import com.octopuscommunity.sdk.ui.components.OctopusTopAppBar
import com.octopuscommunity.sdk.ui.groups.components.FollowButton
import com.octopuscommunity.sdk.ui.posts.list.PostsList
import com.octopuscommunity.sdk.ui.posts.list.emptyPostsItem
import com.octopuscommunity.sdk.ui.semiBold
import com.octopuscommunity.sdk.ui.utils.onScrollDownChanged
import org.koin.core.parameter.parametersOf

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupDetailsScreen(
    navController: NavHostController,
    groupId: String,
    origin: OctopusDestination.Origin = OctopusDestination.Origin.SDK,
    modifier: Modifier = Modifier,
) = OctopusContent(navController) {
    val viewModel =
        octopusViewModel<GroupDetailsViewModel>(
            key = "groupDetails_$groupId",
            parameters = { parametersOf(groupId) },
        )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    OctopusScaffold(
        modifier = modifier,
        viewModel = viewModel,
        topBar = { contentVisible ->
            OctopusTopAppBar(
                title = {
                    Box(
                        modifier =
                            Modifier.graphicsLayer {
                                alpha = scrollBehavior.state.overlappedFraction
                            },
                    ) {
                        OctopusTheme.topAppBar.title(
                            if (contentVisible) state.group?.name ?: "" else "",
                        )
                    }
                },
                navigationIcon = NavigationIconType.Back,
                // Route through the ViewModel's NavigateUp event (not a direct
                // navController.navigateUp()) so it funnels through
                // OctopusScreenContent's handler, which falls back to the host
                // onBack when this screen is the START/root of the back stack
                // (bridge mode). A direct navigateUp() would be a dead no-op there.
                onNavigationIconClick = { viewModel.navigateUp() },
                actions = {
                    Box(
                        modifier =
                            Modifier.graphicsLayer {
                                alpha = scrollBehavior.state.overlappedFraction
                            },
                    ) {
                        if (state.showFollowButton) {
                            FollowButton(
                                modifier = Modifier.padding(end = 8.dp),
                                isFollowed = state.isFollowed,
                                loading = state.isUpdatingFollow,
                                onFollowChanged = { viewModel.setFollowed(it) },
                            )
                        }
                    }
                },
                contentVisible = contentVisible,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        GroupDetailsContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            navController = navController,
            groupId = groupId,
            origin = origin,
        )
    }
}

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupDetailsContent(
    navController: NavHostController,
    groupId: String,
    origin: OctopusDestination.Origin = OctopusDestination.Origin.SDK,
    modifier: Modifier = Modifier,
) = OctopusScreenContent(
    modifier = modifier,
    navController = navController,
) { screenViewModel ->
    val density = LocalDensity.current

    val viewModel =
        octopusViewModel<GroupDetailsViewModel>(
            key = "groupDetails_$groupId",
            parameters = { parametersOf(groupId) },
        )
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Resolve the feed and header data from the shared ViewModel state (same Koin key as
    // GroupDetailsScreen) instead of taking them as parameters. This lets the public bridge
    // wrapper OctopusGroupDetailsContent render the full group feed without pre-resolving the
    // feed id itself — mirrors how PostDetailsContent derives everything from its ViewModel.
    val group = state.group

    @Suppress("DEPRECATION")
    val feedId = group?.postsFeeds?.descendingId

    @Suppress("DEPRECATION")
    val description = group?.description

    var isScrollingDown by rememberSaveable { mutableStateOf(false) }
    var bottomPadding by rememberSaveable { mutableIntStateOf(0) }

    // System back: the NavHost pops on its own (keeping the predictive-back preview) except at
    // the SDK back-stack root, where NavigateUp falls back to the host `onBack` (bridge mode).
    // Never enabled there without a host `onBack` (internal change). See OctopusSystemBackHandler.
    OctopusSystemBackHandler(navController = navController, onBack = viewModel::navigateUp)

    if (state.isAccessDeniedOnArrival) {
        OctopusAccessDenied(
            text = stringResource(R.string.group_permissions_access_denied_text_formal),
            ctaText = stringResource(R.string.group_permissions_access_denied_button),
            onCtaClick = viewModel::notifyGroupAccessDenied,
        )
    } else if (feedId != null) {
        val postsViewModel =
            octopusViewModel<GroupPostsListViewModel>(
                key = "groupPosts_${feedId}_${groupId}_${origin}_${state.feedReloadKey}",
                parameters = { parametersOf(feedId, groupId, origin) },
            )
        // Cap & center the group feed column (header + posts) on large screens (internal ticket).
        ConstrainedContentColumn {
            PostsList(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .onScrollDownChanged { isScrollingDown = it },
                viewModel = postsViewModel,
                source = PostClicked.Source.FEED,
                showModeratedPosts = false,
                loaderTopPadding = OctopusScreenStateDefaults.GroupLoaderOffset,
                emptyPlaceHolder = {
                    // A member who cannot post here gets the neutral wording and no CTA.
                    emptyPostsItem(
                        text = R.string.content_empty_state_be_first_to_post_formal,
                        onCreatePostClick = postsViewModel::navigateToCreatePost,
                        displayCreatePost = state.canCreateAnyPost,
                    )
                },
                contentPadding = PaddingValues(bottom = with(density) { bottomPadding.toDp() } + 16.dp),
                pullToRefresh = true,
                enableProfileClick = true,
                header = {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth(),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = group?.name ?: "",
                                style = OctopusTheme.typography.title2.semiBold,
                                color = OctopusTheme.colorScheme.gray900,
                            )
                            if (state.showFollowButton) {
                                Spacer(modifier = Modifier.width(8.dp))
                                FollowButton(
                                    isFollowed = state.isFollowed,
                                    loading = state.isUpdatingFollow,
                                    onFollowChanged = { viewModel.setFollowed(it) },
                                )
                            }
                        }

                        // internal ticket: description + Topic CTA. Spacings = 16dp everywhere per
                        // Thomas P (PO) DM 2026-05-13 18:38: "tt passé a 16px padding top
                        // / bottom + spacing description et bouton. Le figma est a jour".
                        // Note: at time of writing, the Figma frame `13393:18549` measures
                        // 12dp vertical — the DM intent (16dp) is the authoritative spec
                        // since it was written after the Figma update.
                        @Suppress("DEPRECATION")
                        val customAction = group?.customAction
                        if (description != null || customAction != null) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp, bottom = 16.dp),
                            ) {
                                if (description != null) {
                                    OctopusExpandableText(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp),
                                        text = description,
                                        animateSize = false,
                                    )
                                }
                                if (description != null && customAction != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                if (customAction != null) {
                                    // `padding(horizontal = 16.dp)` here is outer padding around the
                                    // button, not a max-width constraint. A long localized label
                                    // (FR/DE strings can be 30+ chars) sized intrinsically wider than
                                    // `parent - 32dp` could push the button background to touch the
                                    // screen edge; `maxLines = 1` still truncates the text via
                                    // ellipsis. Matches the iOS pattern `HStack { Spacer; Button;
                                    // Spacer }.padding(.horizontal, 16)` in `GroupCTAContentView`. If
                                    // gutter creep ever shows up on a long label, move horizontal
                                    // padding onto the wrapper Column or add `widthIn(max = ...)`.
                                    OctopusMediumButton(
                                        modifier =
                                            Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .padding(horizontal = 16.dp),
                                        text = customAction.ctaText.getText(translated = true),
                                        onClick = { viewModel.navigateToCustomAction(group) },
                                        maxLines = 1,
                                    )
                                }
                            }
                        } else {
                            // Legacy fallback when neither description nor CTA is present —
                            // keeps the visual gap title row → divider that pre-internal ticket builds
                            // shipped.
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        HorizontalDivider(color = OctopusTheme.colorScheme.gray300)
                    }
                },
                showTopic = false,
            )
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
            displayCreateButton = state.canCreateAnyPost,
            onHeightChanged = { bottomPadding = it },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupDetailsScreenPreview() {
    OctopusSDK.koinTestApp
    OctopusTheme {
        GroupDetailsScreen(
            navController = rememberNavController(),
            groupId = MockTopics.default.id,
        )
    }
}
