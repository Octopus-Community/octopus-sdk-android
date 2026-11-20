package com.octopuscommunity.sdk.ui.comments.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.test.mock.MockComments
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusItemMenuSheet
import org.koin.core.parameter.parametersOf

@Composable
fun CommentMenuSheet(
    comment: Comment,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = OctopusContent {
    val viewModel =
        octopusViewModel<CommentMenuSheetViewModel>(
            key = "commentMenuSheet-${comment.id}",
            parameters = { parametersOf(comment) },
        )

    OctopusItemMenuSheet(
        modifier = modifier,
        viewModel = viewModel,
        deleteText = stringResource(R.string.comment_delete_button),
        reportText = stringResource(R.string.moderation_content_button),
        confirmDeleteText = stringResource(R.string.comment_delete_confirmation_title_formal),
        onDismiss = onDismiss,
    )
}

@Preview(showBackground = true)
@Composable
private fun CommentMenuSheetPreview() {
    OctopusTheme {
        CommentMenuSheet(
            comment = MockComments.default,
            onDismiss = {},
        )
    }
}
