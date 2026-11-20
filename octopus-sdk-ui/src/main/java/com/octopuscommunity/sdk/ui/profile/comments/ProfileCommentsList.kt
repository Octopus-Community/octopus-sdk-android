//
//  Copyright © 2026 Octopus Community. All rights reserved.
//
package com.octopuscommunity.sdk.ui.profile.comments

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.test.mock.MockFeeds
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusScreenStateDefaults
import com.octopuscommunity.sdk.ui.components.octopusErrorItem
import com.octopuscommunity.sdk.ui.components.octopusLoadingItem
import com.octopuscommunity.sdk.ui.components.octopusScreenStateItem
import com.octopuscommunity.sdk.ui.posts.list.PostCommentViewModel
import org.koin.core.parameter.parametersOf

/**
 * The connected user's profile "Comments" tab (internal ticket): the member's authored comments & replies,
 * each rendered with their parent post (and, for a reply, its parent comment) as context. Mirrors
 * [com.octopuscommunity.sdk.ui.profile.posts.ProfilePostsList]'s shape.
 *
 * A single [ProfileCommentPostViewModel] / `PostCommentViewModel` / [ProfileCommentReplyViewModel]
 * instance is shared across every row (resolved once, not per item) — mirrors how
 * [com.octopuscommunity.sdk.ui.posts.list.PostsList] shares one `PostCommentViewModel` across every
 * row's embedded featured comment.
 *
 * @param emptyPlaceHolder what to show once the first page has settled with zero comments. Mandatory
 * (unlike [com.octopuscommunity.sdk.ui.profile.posts.ProfilePostsList], which defaults it) because
 * this composable is shared by both profile screens and the wording differs: the connected user's own
 * profile and another member's profile use different strings, so each caller supplies its own
 * `octopusScreenStateItem` (internal ticket empty-state follow-up).
 */
@Composable
fun ProfileCommentsList(
    feedId: String,
    emptyPlaceHolder: LazyListScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) = ProfileCommentsList(feedId, emptyPlaceHolder, modifier, contentPadding, isOwnProfile = true)

@Composable
internal fun ProfileCommentsList(
    feedId: String,
    emptyPlaceHolder: LazyListScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    isOwnProfile: Boolean,
) = OctopusContent {
    val viewModel = rememberProfileCommentsListViewModel(feedId, isOwnProfile)
    val postViewModel = octopusViewModel<ProfileCommentPostViewModel>()
    val commentViewModel = octopusViewModel<PostCommentViewModel>()
    val replyViewModel = octopusViewModel<ProfileCommentReplyViewModel>()

    val state by viewModel.state.collectAsStateWithLifecycle()
    val comments = state.comments

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        when {
            // `isLoading` is an explicit view-model flag, not `comments == null` — see
            // [ProfileCommentsListViewModel.UiState]. It is false once a failed first page has
            // settled, which is what makes the error branch below reachable at all; and true again
            // for the duration of a Retry, which is what makes the Retry visibly do something.
            state.isLoading -> octopusLoadingItem()
            state.error != null -> octopusErrorItem(state.error, onRetry = viewModel::refresh)
            comments != null && comments.isEmpty() -> emptyPlaceHolder()

            comments != null -> {
                items(items = comments, key = { it.id }) { entry ->
                    // One root per entry so the cell and its hairline animate as a unit.
                    Column(modifier = Modifier.animateItem()) {
                        UserCommentCell(
                            userComment = entry,
                            postViewModel = postViewModel,
                            commentViewModel = commentViewModel,
                            replyViewModel = replyViewModel,
                            onOpenPost = viewModel::onOpenPost,
                            onOpenComment = viewModel::onOpenComment,
                            onSeeReplies = viewModel::onSeeReplies,
                        )
                        // 2dp hairline between entries (Figma internal ticket) — not the feed's thick 8dp
                        // inter-post band. Parity with iOS ProfileCommentsListView.
                        Spacer(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(OctopusTheme.colorScheme.gray300),
                        )
                    }
                }
            }
        }
        if (state.hasMore && comments?.isNotEmpty() == true) {
            octopusLoadingItem(
                onVisibilityChanged = { isVisible -> if (isVisible) viewModel.onLoadMoreVisible() },
            )
        }
    }
}

@Composable
internal fun rememberProfileCommentsListViewModel(
    feedId: String,
    isOwnProfile: Boolean = true,
): ProfileCommentsListViewModel =
    octopusViewModel(
        key = "profileCommentsListViewModel_$feedId${if (isOwnProfile) "" else "_other"}",
        parameters = { parametersOf(feedId, isOwnProfile) },
    )

/**
 * The "no comments yet" state every [ProfileCommentsList] caller passes as its `emptyPlaceHolder`.
 *
 * The wording is the caller's — the connected user's own profile and another member's use different
 * strings — but everything around it is the same on all four call sites, which is why they hand in
 * a [text] and nothing else. Mirrors [com.octopuscommunity.sdk.ui.posts.list.emptyPostsItem], minus
 * the CTAs: there is no "write a comment" entry point outside a post.
 */
internal fun LazyListScope.emptyCommentsItem(
    @StringRes text: Int,
    verticalPadding: Dp = OctopusScreenStateDefaults.ProfilePadding,
) = octopusScreenStateItem(
    illustration = { OctopusTheme.icons.screenStates.emptyContent() },
    text = text,
    verticalPadding = verticalPadding,
)

@Preview(showBackground = true)
@Composable
private fun ProfileCommentsListPreview() {
    OctopusTheme {
        ProfileCommentsList(
            feedId =
                MockFeeds.Comment.default.descendingId
                    .orEmpty(),
            emptyPlaceHolder = { emptyCommentsItem(R.string.profile_comments_empty_state_other) },
        )
    }
}
