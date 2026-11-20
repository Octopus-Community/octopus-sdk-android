package com.octopuscommunity.sdk.ui.comments.replies.list

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.test.mock.MockComments
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
fun RepliesList(
    comment: Comment,
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
        octopusViewModel<RepliesListViewModel>(
            key = "repliesList_${comment.id}_${comment.repliesFeeds.id}_${scrollToLatest}_$scrollToId",
            parameters = { parametersOf(comment, scrollToLatest, scrollToId) },
        )

    ResponsesList(
        viewModel = viewModel,
        header = header,
        placeHolder = placeHolder,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        failureVerticalPadding = OctopusScreenStateDefaults.CommentPadding,
        modifier = modifier,
        bottomPadding = bottomPadding,
        composerRequest = composerRequest,
        isReply = true,
    ) { reply ->
        ResponseCard(
            modifier =
                Modifier
                    .animateItem(
                        fadeInSpec = tween(OctopusMotion.SHORT_DURATION_MS),
                        placementSpec = tween(OctopusMotion.SHORT_DURATION_MS),
                        fadeOutSpec = tween(OctopusMotion.SHORT_DURATION_MS),
                    ).fillMaxWidth()
                    .padding(start = 56.dp, end = 16.dp),
            viewModel = viewModel,
            reply = reply,
            displayMode = ResponseDisplayMode.Summary(onCardTap = null),
            onAccessibilityClick = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RepliesListPreview() {
    OctopusTheme {
        RepliesList(
            comment = MockComments.default,
            header = {},
            placeHolder = {},
            isRefreshing = false,
            onRefresh = {},
        )
    }
}
