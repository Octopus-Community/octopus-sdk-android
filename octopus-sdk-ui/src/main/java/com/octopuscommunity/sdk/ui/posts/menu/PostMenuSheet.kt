package com.octopuscommunity.sdk.ui.posts.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.test.mock.MockPosts
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.MenuItem
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusItemMenuSheet
import org.koin.core.parameter.parametersOf

@Composable
fun PostMenuSheet(
    post: Post,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    // When non-null, prepends a non-destructive "View group" entry that navigates to the post's
    // group. Null hides it (e.g. on the group feed itself).
    onViewGroup: (() -> Unit)? = null,
) = OctopusContent {
    val viewModel =
        octopusViewModel<PostMenuSheetViewModel>(
            key = "postMenuSheet-${post.id}",
            parameters = { parametersOf(post) },
        )

    val leadingItems =
        if (onViewGroup != null) {
            listOf(
                MenuItem(
                    icon = OctopusTheme.icons.groups.viewGroup(),
                    title = stringResource(R.string.post_menu_view_group_button),
                    isDestructive = false,
                    onClick = {
                        onDismiss()
                        onViewGroup()
                    },
                ),
            )
        } else {
            emptyList()
        }

    OctopusItemMenuSheet(
        modifier = modifier,
        viewModel = viewModel,
        deleteText = stringResource(R.string.post_delete_button),
        reportText = stringResource(R.string.moderation_content_button),
        confirmDeleteText = stringResource(R.string.post_delete_confirmation_title_formal),
        onDismiss = onDismiss,
        leadingItems = leadingItems,
    )
}

@Preview(showBackground = true)
@Composable
private fun PostMenuSheetPreview() {
    OctopusTheme {
        PostMenuSheet(
            post = MockPosts.default,
            onDismiss = {},
        )
    }
}
