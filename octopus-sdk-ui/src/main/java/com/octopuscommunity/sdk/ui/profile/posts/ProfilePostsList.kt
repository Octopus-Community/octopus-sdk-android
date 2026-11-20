package com.octopuscommunity.sdk.ui.profile.posts

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.common.util.UnstableApi
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostClicked
import com.octopuscommunity.sdk.test.mock.MockFeeds
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusScreenStateDefaults
import com.octopuscommunity.sdk.ui.posts.list.PostsList
import org.koin.core.parameter.parametersOf

@OptIn(UnstableApi::class)
@Composable
fun ProfilePostsList(
    feedId: String,
    modifier: Modifier = Modifier,
    showModeratedPosts: Boolean = false,
    emptyPlaceHolder: LazyListScope.() -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) = OctopusContent {
    val postViewModel =
        octopusViewModel<ProfilePostsListViewModel>(
            key = "profilePostsListViewModel_$feedId",
            parameters = { parametersOf(feedId) },
        )

    PostsList(
        modifier = modifier,
        viewModel = postViewModel,
        source = PostClicked.Source.PROFILE,
        emptyPlaceHolder = emptyPlaceHolder,
        showModeratedPosts = showModeratedPosts,
        loaderTopPadding = OctopusScreenStateDefaults.ProfileLoaderOffset,
        contentPadding = contentPadding,
        pullToRefresh = false,
        enableProfileClick = false,
        showTopic = true,
        onScrollToTop = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfilePostsListPreview() {
    OctopusTheme {
        ProfilePostsList(
            feedId = MockFeeds.Post.defaultRoot.id,
            emptyPlaceHolder = {},
        )
    }
}
