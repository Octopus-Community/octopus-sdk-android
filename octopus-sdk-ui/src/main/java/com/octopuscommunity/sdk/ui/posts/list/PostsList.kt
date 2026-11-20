package com.octopuscommunity.sdk.ui.posts.list

import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostClicked
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.Status
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusPullToRefresh
import com.octopuscommunity.sdk.ui.components.OctopusScreenStateDefaults
import com.octopuscommunity.sdk.ui.components.octopusLoadingItem
import com.octopuscommunity.sdk.ui.components.octopusScreenStateFailureItem
import com.octopuscommunity.sdk.ui.components.octopusScreenStateItem
import com.octopuscommunity.sdk.ui.posts.PostCard
import com.octopuscommunity.sdk.ui.posts.PostDisplayMode
import com.octopuscommunity.sdk.ui.responses.ResponseCard
import com.octopuscommunity.sdk.ui.responses.ResponseDisplayMode
import com.octopuscommunity.sdk.ui.utils.animateScrollToItemOrInterrupt
import kotlin.math.abs

@OptIn(UnstableApi::class)
@Composable
internal fun PostsList(
    viewModel: PostsListViewModel,
    source: PostClicked.Source,
    modifier: Modifier = Modifier,
    header: (@Composable LazyItemScope.() -> Unit)? = null,
    showTopic: Boolean,
    emptyPlaceHolder: LazyListScope.() -> Unit = {
        emptyPostsItem(
            text = R.string.content_empty_state_be_first_to_post_formal,
            onCreatePostClick = viewModel::navigateToCreatePost,
        )
    },
    showModeratedPosts: Boolean = false,
    loaderTopPadding: Dp = OctopusScreenStateDefaults.FeedLoaderOffset,
    contentPadding: PaddingValues = PaddingValues(),
    pullToRefresh: Boolean = true,
    enableProfileClick: Boolean = true,
    onScrollToTop: () -> Unit = {},
) {
    val commentViewModel = octopusViewModel<PostCommentViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onPause() }

    val lastVisible by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: 0
        }
    }
    val totalItems by remember { derivedStateOf { listState.layoutInfo.totalItemsCount } }
    LaunchedEffect(lastVisible, totalItems) {
        if (state.hasMorePosts && !state.isLoading && totalItems > 0 && lastVisible >= totalItems - 3) {
            viewModel.loadNextPage()
        }
    }

    val items =
        state.posts.filter {
            showModeratedPosts || it.status !is Status.Moderated
        }

    val playingVideoItem by remember(items) {
        derivedStateOf {
            listState.activeItem(
                items = items,
                filter = { it.video != null },
                headerOffset = if (header != null) 1 else 0,
            )
        }
    }

    val contentDescription = stringResource(R.string.accessibility_header_post_list)

    OctopusPullToRefresh(
        modifier =
            modifier.semantics {
                heading()
                this.contentDescription = contentDescription
            },
        refreshing = state.isRefreshing,
        enabled = pullToRefresh,
//            indicatorPadding = contentPadding,
        onRefresh = { viewModel.reload(true) },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = contentPadding,
        ) {
            if (header != null) {
                item(key = "header", content = header)
            }

            if (state.isLoading) {
                // First load with nothing on screen yet: the spinner sits where the first row will be,
                // at the offset the embedding screen gives it.
                octopusLoadingItem(topPadding = loaderTopPadding)
            } else if (state.hasLoadFailure) {
                octopusScreenStateFailureItem(
                    isNoNetwork = state.loadFailureIsNoNetwork,
                    verticalPadding = OctopusScreenStateDefaults.FeedErrorPadding,
                    onRetry = { viewModel.reload() },
                )
            } else if (state.posts.isEmpty()) {
                emptyPlaceHolder()
            } else {
                items(
                    items = items,
                    key = { it.id },
                ) { post ->
                    // internal ticket Q8: commentless tap toggles inline expansion; childCount > 0
                    // keeps navigation so pre-existing comments stay reachable.
                    val canOpenDetail = post.canCreateChildren || post.commentCount > 0
                    var isTextExpanded by rememberSaveable(post.id) { mutableStateOf(false) }
                    val onTap: () -> Unit = {
                        if (canOpenDetail) {
                            viewModel.onPostClicked(post = post, source = source)
                        } else {
                            isTextExpanded = !isTextExpanded
                        }
                    }
                    Column(
                        modifier =
                            Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .clickable(onClick = onTap),
                    ) {
                        PostCard(
                            modifier = Modifier.fillMaxWidth(),
                            viewModel = viewModel,
                            post = post,
                            displayMode =
                                PostDisplayMode.Summary(
                                    onCardTap = onTap,
                                    onChildrenTap = {
                                        viewModel.onCommentButtonClicked(
                                            post = post,
                                            focusInputField = false,
                                        )
                                    },
                                    displayGroupName = showTopic,
                                ),
                            playVideo = playingVideoItem == post,
                            enableProfileClick = enableProfileClick,
                            onAccessibilityClick =
                                if (canOpenDetail) {
                                    { viewModel.onPostClicked(post = post, source = source) }
                                } else {
                                    null
                                },
                            onCommentButtonTap = {
                                viewModel.onCommentButtonClicked(
                                    post = post,
                                    focusInputField = true,
                                )
                            },
                            forceFullText = isTextExpanded,
                        )

                        post.featuredComment?.let { comment ->
                            // Tapping the featured comment in a post summary opens the parent
                            // post's detail on that comment (internal ticket). The rest of
                            // the app (comments inside post detail, replies inside comment
                            // detail) keeps this `null` so only the avatar, action bar, and
                            // "See N replies" row are tappable.
                            // `ellipsizeText = true` is iso iOS `DisplayablePost.featuredComment`.
                            // `remember(post, source)` re-creates the `Summary` only when the
                            // identity (by data-class equality) of `post` or `source` changes —
                            // pure parent recompositions don't churn the lambda. Note that `Post`
                            // is not Compose-`@Stable`, so a VM tick that yields a *new* equal
                            // `Post` instance won't re-create here, but a VM tick that yields a
                            // *non-equal* `Post` (e.g. updated reactionCount) still will. Net win
                            // vs. recreating on every recomposition, but not "stable forever".
                            val featuredCommentDisplayMode =
                                remember(post, source) {
                                    ResponseDisplayMode.Summary(
                                        onCardTap = {
                                            viewModel.onFeaturedCommentClicked(
                                                post = post,
                                                comment = comment,
                                                source = source,
                                            )
                                        },
                                        ellipsizeText = true,
                                    )
                                }
                            ResponseCard(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                viewModel = commentViewModel,
                                comment = comment,
                                displayMode = featuredCommentDisplayMode,
                                showsRepliesRow = false,
                                onAccessibilityClick = null,
                                onReplyClick = { commentViewModel.onReplyButtonClicked(comment) },
                                onSeeRepliesClick = {},
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        HorizontalDivider(
                            thickness = 2.dp,
                            color = OctopusTheme.colorScheme.gray300,
                        )
                    }
                }
            }
            if (state.hasMorePosts) {
                octopusLoadingItem()
            }
        }
    }

    LaunchedEffect(state.scrollToTop) {
        if (state.scrollToTop) {
            listState.animateScrollToItemOrInterrupt(0)
            onScrollToTop()
            viewModel.resetScrollToTop()
        }
    }

    // Leaving the screen cancels an animated scroll before it clears its request: clear it here,
    // or it replays over the restored position on return. A configuration change keeps it, so the
    // recreated list resumes the scroll.
    val activity = LocalActivity.current
    DisposableEffect(viewModel) {
        onDispose { if (activity?.isChangingConfigurations != true) viewModel.resetScrollToTop() }
    }
}

/**
 * Empty-feed placeholder with the create-post call to action.
 *
 * When [displayCreatePost] is `false` (internal ticket — no topic in scope allows post creation by
 * the current user), the call to action is dropped and [text] gives way to the neutral
 * `profile_posts_empty_state_other`: inviting the member to post would offer something they are not
 * allowed to do. Mirrors iOS `CurrentUserProfileSummaryView`, `ActivityView` and `GroupDetailView`,
 * which show `Profile.Posts.EmptyState.Other` with no CTA (internal ticket).
 *
 * @param text the invitation shown with the call to action; only used when [displayCreatePost] is
 * `true`.
 */
internal fun LazyListScope.emptyPostsItem(
    @StringRes text: Int,
    onCreatePostClick: () -> Unit,
    displayCreatePost: Boolean = true,
    verticalPadding: Dp = OctopusScreenStateDefaults.FeedEmptyPadding,
) = octopusScreenStateItem(
    illustration = { OctopusTheme.icons.screenStates.emptyContent() },
    text = if (displayCreatePost) text else R.string.profile_posts_empty_state_other,
    verticalPadding = verticalPadding,
    // One CTA now, not the six incentive chips nor a poll button (iso iOS internal ticket): only shown where
    // the member can actually post (internal ticket — no topic in scope allows creation), otherwise the
    // state is read-only.
    action = (R.string.profile_posts_empty_state_cta_self to onCreatePostClick).takeIf { displayCreatePost },
)

private fun LazyListState.activeItem(
    items: List<OctopusItem>,
    filter: (OctopusItem) -> Boolean,
    headerOffset: Int = 0,
): OctopusItem? {
    val visibleItems =
        layoutInfo.visibleItemsInfo.takeIf {
            it.isNotEmpty() && items.isNotEmpty()
        } ?: return null

    return if (firstVisibleItemIndex <= headerOffset && firstVisibleItemScrollOffset == 0) {
        // If scrolled to the top, return first item so it can be played even if never in center
        items.first()
    } else {
        val lastVisibleItem = visibleItems.last()
        if (lastVisibleItem.index == items.size - 1 + headerOffset &&
            layoutInfo.viewportEndOffset - lastVisibleItem.offset >= lastVisibleItem.size
        ) {
            // If fully scrolled to the bottom, return last item so it can be played even if never
            // in center
            items.last()
        } else {
            val centerOffset = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            val centerItems =
                visibleItems.sortedBy {
                    // Sort by center proximity
                    abs(it.offset + it.size / 2 - centerOffset)
                }
            centerItems.firstNotNullOfOrNull { centerItem ->
                items.getOrNull(centerItem.index - headerOffset)?.takeIf(filter)
            }
        }
    }
}
