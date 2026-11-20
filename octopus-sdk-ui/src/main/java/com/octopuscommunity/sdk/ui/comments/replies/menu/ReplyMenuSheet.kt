package com.octopuscommunity.sdk.ui.comments.replies.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.octopuscommunity.sdk.domain.model.Comment.Reply
import com.octopuscommunity.sdk.test.mock.MockReplies
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusItemMenuSheet
import org.koin.core.parameter.parametersOf

@Composable
fun ReplyMenuSheet(
    reply: Reply,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = OctopusContent {
    val viewModel =
        octopusViewModel<ReplyMenuSheetViewModel>(
            key = "replyMenuSheet-${reply.id}",
            parameters = { parametersOf(reply) },
        )

    OctopusItemMenuSheet(
        modifier = modifier,
        viewModel = viewModel,
        deleteText = stringResource(R.string.reply_delete_button),
        reportText = stringResource(R.string.moderation_content_button),
        confirmDeleteText = stringResource(R.string.reply_delete_confirmation_title_formal),
        onDismiss = onDismiss,
    )
}

@Preview(showBackground = true)
@Composable
private fun ReplyMenuSheetPreview() {
    OctopusTheme {
        ReplyMenuSheet(
            reply = MockReplies.default,
            onDismiss = {},
        )
    }
}
