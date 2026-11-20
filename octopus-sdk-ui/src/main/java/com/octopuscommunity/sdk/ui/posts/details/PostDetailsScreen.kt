package com.octopuscommunity.sdk.ui.posts.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.test.mock.MockPosts
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.comments.components.initialComposerFocus
import com.octopuscommunity.sdk.ui.comments.create.CommentInputField
import com.octopuscommunity.sdk.ui.comments.list.CommentsList
import com.octopuscommunity.sdk.ui.common.snackbar.OctopusSnackbarHost
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.ConstrainedContentColumn
import com.octopuscommunity.sdk.ui.components.OctopusConfirmAlertDialog
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusContentNotAvailable
import com.octopuscommunity.sdk.ui.components.OctopusProgressIndicator
import com.octopuscommunity.sdk.ui.components.OctopusScaffold
import com.octopuscommunity.sdk.ui.components.OctopusScreenContent
import com.octopuscommunity.sdk.ui.components.OctopusScreenState
import com.octopuscommunity.sdk.ui.components.OctopusScreenStateDefaults
import com.octopuscommunity.sdk.ui.components.OctopusScreenStateFailure
import com.octopuscommunity.sdk.ui.components.OctopusSystemBackHandler
import com.octopuscommunity.sdk.ui.components.OctopusTopAppBar
import com.octopuscommunity.sdk.ui.posts.PostCard
import com.octopuscommunity.sdk.ui.posts.PostDisplayMode
import com.octopuscommunity.sdk.ui.posts.menu.PostMenuSheet
import com.octopuscommunity.sdk.ui.profile.common.getUiNickname
import com.octopuscommunity.sdk.ui.utils.safeImePadding
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PostDetailsScreen(
    navController: NavHostController,
    postId: String,
    modifier: Modifier = Modifier,
    commentId: String? = null,
    scrollToLatestComment: Boolean = false,
    focusCommentInputField: Boolean = false,
    origin: OctopusDestination.Origin = OctopusDestination.Origin.SDK,
    hasFeaturedComment: Boolean = false,
) = OctopusContent(navController) {
    val focusManager = LocalFocusManager.current

    val viewModel =
        octopusViewModel<PostDetailsViewModel>(
            key = "postDetails_${postId}_${origin}_$hasFeaturedComment",
            parameters = { parametersOf(postId, origin, hasFeaturedComment) },
        )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val currentUserProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val communityConfig by viewModel.communityConfig.collectAsStateWithLifecycle(null)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showMenuSheet by remember { mutableStateOf(false) }

    val authorName =
        state.post
            ?.author
            .getUiNickname(currentUserProfile)
            .asString()

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
                            if (contentVisible) {
                                stringResource(R.string.post_title, authorName)
                            } else {
                                ""
                            },
                        )
                    }
                },
                onNavigationIconClick = {
                    focusManager.clearFocus()
                    viewModel.onBack()
                },
                actions = {
                    // Always show the menu on the post detail: at minimum it offers "View group"
                    // (a post detail is never the group feed itself), plus any user actions.
                    if (state.post != null) {
                        IconButton(onClick = { showMenuSheet = true }) {
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
        PostDetailsContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            navController = navController,
            postId = postId,
            commentId = commentId,
            scrollToLatestComment = scrollToLatestComment,
            focusCommentInput = focusCommentInputField,
            origin = origin,
            hasFeaturedComment = hasFeaturedComment,
        )

        if (showMenuSheet) {
            state.post?.let {
                PostMenuSheet(
                    post = it,
                    onDismiss = { showMenuSheet = false },
                    // Post detail is never the group feed itself → always offer "View group".
                    onViewGroup = {
                        viewModel.navigateTo(OctopusDestination.GroupDetails(groupId = it.parentId))
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PostDetailsContent(
    navController: NavHostController,
    postId: String,
    modifier: Modifier = Modifier,
    commentId: String? = null,
    scrollToLatestComment: Boolean = false,
    focusCommentInput: Boolean = false,
    origin: OctopusDestination.Origin = OctopusDestination.Origin.SDK,
    hasFeaturedComment: Boolean = false,
) = OctopusScreenContent(
    modifier =
        modifier
            .safeImePadding(),
    navController = navController,
) { screenViewModel ->
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current

    val viewModel =
        octopusViewModel<PostDetailsViewModel>(
            key = "postDetails_${postId}_${origin}_$hasFeaturedComment",
            parameters = { parametersOf(postId, origin, hasFeaturedComment) },
        )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val post = state.post

    var inputFieldHeight by remember { mutableStateOf(0.dp) }
    val inputFieldFocusRequester = remember { FocusRequester() }
    var composerRequest by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onPause() }

    // System back: the NavHost pops on its own (keeping the predictive-back preview) except
    // while a discard-comment-draft dialog must be shown first, or at the SDK back-stack root, where
    // NavigateUp falls back to the host `onBack` (bridge mode). Never enabled at the root
    // without a host `onBack` (internal change). See OctopusSystemBackHandler.
    OctopusSystemBackHandler(
        navController = navController,
        hasPendingConfirmation = state.hasUnsavedChanges,
        onBack = viewModel::onBack,
    )

    when {
        state.isLoading ->
            OctopusProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

        state.isContentUnavailable ->
            OctopusContentNotAvailable(
                icon =
                    OctopusTheme.icons.content.post
                        .notAvailable(),
                text = stringResource(R.string.content_detail_not_available),
            )

        // The load failed with no post to show behind it — the inline half of
        // `loadFailureChannel`, and the only branch that offers Retry. Without it this state
        // matched nothing and left a blank screen once the snackbar auto-dismissed (internal change).
        state.hasLoadFailure && post == null ->
            OctopusScreenStateFailure(
                modifier = Modifier.align(Alignment.Center),
                isNoNetwork = state.loadFailureIsNoNetwork,
                verticalPadding = OctopusScreenStateDefaults.PostErrorPadding,
                onRetry = viewModel::refresh,
            )

        state.isLockedOwnContent && post != null ->
            // GDPR carve-out: own post in a now non-accessible group stays visible to its
            // author. Comments feed and input are hidden — mirrors iOS d655011 which gates
            // both `PostDetailCommentsView` and `CreateCommentView` on `lockedState`.
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
            ) {
                PostCard(
                    modifier = Modifier.fillMaxWidth(),
                    viewModel = viewModel,
                    post = post,
                    displayMode = PostDisplayMode.Detail,
                    playVideo = true,
                    onAccessibilityClick = {},
                    onCommentButtonTap = {},
                )
            }

        post != null ->
            // Cap & center the post + comments column on large screens (internal ticket).
            ConstrainedContentColumn {
                CommentsList(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null, // Remove ripple effect
                                onClick = focusManager::clearFocus,
                            ),
                    bottomPadding = inputFieldHeight + 16.dp,
                    post = post,
                    scrollToLatest = scrollToLatestComment,
                    scrollToId = commentId,
                    composerRequest = composerRequest,
                    header = {
                        PostCard(
                            modifier = Modifier.fillMaxWidth(),
                            viewModel = viewModel,
                            post = post,
                            displayMode = PostDisplayMode.Detail,
                            playVideo = true,
                            onAccessibilityClick = {},
                            onCommentButtonTap = {
                                viewModel.onCommentClicked(post)
                                composerRequest++
                                inputFieldFocusRequester.requestFocus()
                            },
                        )
                        HorizontalDivider(
                            thickness = 2.dp,
                            color = OctopusTheme.colorScheme.gray300,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    },
                    placeHolder = {
                        // The whole state stays tappable, as it was before the Screen states
                        // migration: it is the fastest way into the composer from a post nobody has
                        // commented yet. `post_detail_no_comments` already carries the ticket's
                        // wording in all 24 locales.
                        // The illustration + "What do you think?" wording says nothing about what
                        // the tap does, so a screen reader would announce a bare "double tap to
                        // activate" on an element it cannot even tell is actionable: name the action
                        // and the role. `comment_create_text_placeholder` ("Add a comment") is the
                        // composer's own label — the exact thing this tap opens — and already exists
                        // in all 24 locales, so no new key is needed.
                        OctopusScreenState(
                            modifier =
                                Modifier.clickable(
                                    onClickLabel = stringResource(R.string.comment_create_text_placeholder),
                                    role = Role.Button,
                                ) {
                                    inputFieldFocusRequester.requestFocus()
                                },
                            illustration = OctopusTheme.icons.screenStates.emptyContent(),
                            text = stringResource(R.string.post_detail_no_comments_formal),
                            verticalPadding = OctopusScreenStateDefaults.PostEmptyPadding,
                        )
                    },
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                )
            }
    }

    Column(modifier = Modifier.align(Alignment.BottomCenter)) {
        OctopusSnackbarHost(screenViewModel)

        if (post != null && state.canCreateComment) {
            CommentInputField(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            with(density) { inputFieldHeight = size.height.toDp() }
                        }.initialComposerFocus(focusCommentInput) { inputFieldFocusRequester.requestFocus() },
                post = post,
                focusRequester = inputFieldFocusRequester,
                onDraftChanged = viewModel::updateDraftComment,
                onFocused = { composerRequest++ },
            )
        }
    }

    if (state.showUnsavedChangesDialog) {
        OctopusConfirmAlertDialog(
            text = stringResource(R.string.common_cancel_modifications_formal),
            confirmButtonText = stringResource(R.string.common_yes),
            dismissButtonText = stringResource(R.string.common_no),
            onConfirm = { viewModel.dismissUnsavedChangesDialog(true) },
            onDismiss = { viewModel.dismissUnsavedChangesDialog(false) },
        )
    }
}

@Preview
@Composable
private fun PostDetailsScreenPreview() {
    OctopusTheme {
        PostDetailsScreen(
            navController = rememberNavController(),
            postId = MockPosts.all.random().id,
        )
    }
}
