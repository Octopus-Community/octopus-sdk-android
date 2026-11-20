package com.octopuscommunity.sdk.ui.comments.list

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.test.mock.MockPosts
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusScreenStateDefaults
import com.octopuscommunity.sdk.ui.responses.ResponseCard
import com.octopuscommunity.sdk.ui.responses.ResponseDisplayMode
import com.octopuscommunity.sdk.ui.responses.ResponsesList
import com.octopuscommunity.sdk.ui.utils.OctopusMotion
import org.koin.core.parameter.parametersOf

@Composable
fun CommentsList(
    post: Post,
    header: @Composable LazyItemScope.() -> Unit,
    placeHolder: @Composable LazyItemScope.() -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    scrollToLatest: Boolean = false,
    scrollToId: String? = null,
    composerRequest: Int = 0,
) = OctopusContent {
    val viewModel =
        octopusViewModel<CommentsListViewModel>(
            key = "commentsList_${post.id}_${post.commentsFeeds.id}_${scrollToLatest}_$scrollToId",
            parameters = { parametersOf(post, scrollToLatest, scrollToId) },
        )

    ResponsesList(
        viewModel = viewModel,
        header = header,
        placeHolder = placeHolder,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        failureVerticalPadding = OctopusScreenStateDefaults.PostErrorPadding,
        modifier = modifier,
        bottomPadding = bottomPadding,
        composerRequest = composerRequest,
    ) { comment ->
        ResponseCard(
            modifier =
                Modifier
                    .animateItem(
                        fadeInSpec = tween(OctopusMotion.SHORT_DURATION_MS),
                        placementSpec = tween(OctopusMotion.SHORT_DURATION_MS),
                        fadeOutSpec = tween(OctopusMotion.SHORT_DURATION_MS),
                    ).fillMaxWidth()
                    .padding(horizontal = 16.dp),
            viewModel = viewModel,
            comment = comment,
            displayMode = ResponseDisplayMode.Summary(onCardTap = null),
            onReplyClick = { viewModel.onReplyButtonClicked(comment) },
            onSeeRepliesClick = { viewModel.onSeeRepliesClicked(comment) },
            onAccessibilityClick = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CommentsListPreview() {
    OctopusTheme {
        CommentsList(
            post = MockPosts.default,
            header = {},
            placeHolder = {},
            isRefreshing = false,
            onRefresh = {},
        )
    }
}
