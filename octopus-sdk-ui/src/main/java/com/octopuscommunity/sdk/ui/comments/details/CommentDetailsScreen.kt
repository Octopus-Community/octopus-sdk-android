package com.octopuscommunity.sdk.ui.comments.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.test.mock.MockComments
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.comments.components.initialComposerFocus
import com.octopuscommunity.sdk.ui.comments.replies.create.ReplyInputField
import com.octopuscommunity.sdk.ui.comments.replies.list.RepliesList
import com.octopuscommunity.sdk.ui.common.snackbar.OctopusSnackbarHost
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusConfirmAlertDialog
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusContentNotAvailable
import com.octopuscommunity.sdk.ui.components.OctopusMediumOutlinedButton
import com.octopuscommunity.sdk.ui.components.OctopusProgressIndicator
import com.octopuscommunity.sdk.ui.components.OctopusScaffold
import com.octopuscommunity.sdk.ui.components.OctopusScreenContent
import com.octopuscommunity.sdk.ui.components.OctopusScreenStateFailure
import com.octopuscommunity.sdk.ui.components.OctopusSystemBackHandler
import com.octopuscommunity.sdk.ui.responses.ResponseCard
import com.octopuscommunity.sdk.ui.responses.ResponseDisplayMode
import com.octopuscommunity.sdk.ui.utils.safeImePadding
import org.koin.core.parameter.parametersOf

@Composable
internal fun CommentDetailsScreen(
    navController: NavHostController,
    commentId: String,
    modifier: Modifier = Modifier,
    replyId: String? = null,
    showViewPostButton: Boolean = false,
    scrollToLatestReply: Boolean = false,
    focusReplyInputField: Boolean = false,
) = OctopusContent(navController) {
    val focusManager = LocalFocusManager.current

    val viewModel =
        octopusViewModel<CommentDetailsViewModel>(
            key = "commentDetails_${commentId}_$replyId",
            parameters = { parametersOf(commentId, replyId) },
        )

    OctopusScaffold(
        modifier = modifier,
        viewModel = viewModel,
        title = stringResource(R.string.comment_detail_title),
        onBack = {
            focusManager.clearFocus()
            viewModel.onBack()
        },
    ) { innerPadding ->
        CommentDetailsContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            navController = navController,
            commentId = commentId,
            replyId = replyId,
            showViewPostButton = showViewPostButton,
            scrollToLatestReply = scrollToLatestReply,
            focusInputField = focusReplyInputField,
        )
    }
}

@Composable
internal fun CommentDetailsContent(
    navController: NavHostController,
    commentId: String,
    modifier: Modifier = Modifier,
    replyId: String? = null,
    showViewPostButton: Boolean = false,
    scrollToLatestReply: Boolean = false,
    focusInputField: Boolean = false,
) = OctopusScreenContent(
    modifier =
        modifier
            .safeImePadding(),
    navController = navController,
) { screenViewModel ->
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current

    val viewModel =
        octopusViewModel<CommentDetailsViewModel>(
            key = "commentDetails_${commentId}_$replyId",
            parameters = { parametersOf(commentId, replyId) },
        )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val comment = state.comment

    var inputFieldHeight by remember { mutableStateOf(0.dp) }
    val inputFieldFocusRequester = remember { FocusRequester() }
    var composerRequest by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    // System back: the NavHost pops on its own (keeping the predictive-back preview) except
    // while a discard-reply-draft dialog must be shown first, or at the SDK back-stack root, where
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
                    OctopusTheme.icons.content.comment
                        .notAvailable(),
                text = stringResource(R.string.content_detail_not_available),
            )

        state.hasLoadFailure && comment == null ->
            OctopusScreenStateFailure(
                isNoNetwork = state.loadFailureIsNoNetwork,
                onRetry = viewModel::refresh,
            )

        state.isLockedOwnContent && comment != null ->
            // GDPR carve-out: own comment in a now non-accessible group stays visible to
            // its author. Replies feed and reply input are hidden — mirrors iOS 95bb3b6
            // which gates both `CommentDetailRepliesView` and `CreateReplyView`.
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showViewPostButton) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OctopusMediumOutlinedButton(
                        text = stringResource(R.string.comment_see_parent),
                        onClick = viewModel::navigateToPostDetails,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                ResponseCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    viewModel = viewModel,
                    comment = comment,
                    displayMode = ResponseDisplayMode.Detail,
                    containerColor = OctopusTheme.colorScheme.primaryLow,
                    onReplyClick = {},
                    onSeeRepliesClick = {},
                    onAccessibilityClick = null,
                )
            }

        comment != null ->
            RepliesList(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null, // Remove ripple effect
                            onClick = focusManager::clearFocus,
                        ),
                bottomPadding = inputFieldHeight + 16.dp,
                comment = comment,
                scrollToLatest = scrollToLatestReply,
                scrollToId = replyId,
                composerRequest = composerRequest,
                header = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (showViewPostButton) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OctopusMediumOutlinedButton(
                                text = stringResource(R.string.comment_see_parent),
                                onClick = viewModel::navigateToPostDetails,
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        ResponseCard(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            viewModel = viewModel,
                            comment = comment,
                            displayMode = ResponseDisplayMode.Detail,
                            containerColor = OctopusTheme.colorScheme.primaryLow,
                            onReplyClick = {
                                viewModel.onReplyButtonClicked(comment)
                                composerRequest++
                                inputFieldFocusRequester.requestFocus()
                            },
                            onSeeRepliesClick = {},
                            onAccessibilityClick = null,
                        )
                    }
                },
                placeHolder = {},
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
            )
    }
    Column(modifier = Modifier.align(Alignment.BottomCenter)) {
        OctopusSnackbarHost(screenViewModel)

        if (comment != null && state.canCreateReply) {
            ReplyInputField(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            with(density) { inputFieldHeight = size.height.toDp() }
                        }.initialComposerFocus(focusInputField) { inputFieldFocusRequester.requestFocus() },
                comment = comment,
                focusRequester = inputFieldFocusRequester,
                onDraftChanged = viewModel::updateDraftReply,
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
private fun CommentDetailsScreenPreview() {
    OctopusTheme {
        CommentDetailsScreen(
            navController = rememberNavController(),
            commentId = MockComments.all.random().id,
        )
    }
}
