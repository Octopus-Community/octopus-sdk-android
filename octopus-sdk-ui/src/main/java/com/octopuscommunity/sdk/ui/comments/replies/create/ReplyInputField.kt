package com.octopuscommunity.sdk.ui.comments.replies.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.Comment.Reply
import com.octopuscommunity.sdk.test.mock.MockComments
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.comments.components.OctopusItemInputField
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.consent.ConsentContribution
import com.octopuscommunity.sdk.ui.consent.TermsConsentSheet
import com.octopuscommunity.sdk.ui.consent.resolveConsentWordings
import org.koin.core.parameter.parametersOf

@Composable
fun ReplyInputField(
    comment: Comment,
    onDraftChanged: (Reply.Draft) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocused: () -> Unit = {},
) = OctopusContent {
    val context = LocalContext.current

    val viewModel =
        octopusViewModel<ReplyInputFieldViewModel>(
            key = "replyInputField_${comment.id}",
            parameters = { parametersOf(comment) },
        )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.item) {
        onDraftChanged(state.item)
    }

    OctopusItemInputField(
        modifier = modifier,
        state = state,
        placeholder = stringResource(R.string.reply_create_text_placeholder),
        focusRequester = focusRequester,
        onTextChanged = viewModel::updateText,
        onImageUpdated = { viewModel.updateImage(context = context, localUri = it) },
        onFocused = onFocused,
        onSend = viewModel::send,
        onNavigateToTermsOfUse = viewModel::navigateToTermsOfUse,
        onNavigateToPrivacyPolicy = viewModel::navigateToPrivacyPolicy,
        onNavigateToCommunityGuidelines = viewModel::navigateToCommunityGuidelines,
        onNavigateToUrl = viewModel::navigateToUrl,
        addPictureIcon =
            OctopusTheme.icons.content.reply.creation
                .addPicture(),
        deleteImageIcon =
            OctopusTheme.icons.content.reply.creation
                .deletePicture(),
        sendIcon =
            OctopusTheme.icons.content.reply.creation
                .create(),
    )

    TermsConsentSheet(
        visible = state.showConsentSheet,
        mode = state.termsAcceptanceMode,
        contribution = ConsentContribution.REPLY,
        wordings =
            resolveConsentWordings(
                mode = state.termsAcceptanceMode,
                consentTexts = state.consentTexts,
                contribution = ConsentContribution.REPLY,
            ),
        nicknameChoiceFollows = state.consentSheetLeadsToNicknameChoice,
        onAccept = viewModel::acceptConsentAndSend,
        onDismiss = viewModel::dismissConsentSheet,
        onNavigateToTerms = viewModel::navigateToTermsOfUse,
        onNavigateToPrivacy = viewModel::navigateToPrivacyPolicy,
        onNavigateToRules = viewModel::navigateToCommunityGuidelines,
        onNavigateToUrl = viewModel::navigateToUrl,
    )
}

@Preview(showBackground = true)
@Composable
private fun ReplyInputFieldPreview() {
    OctopusTheme {
        ReplyInputField(
            comment = MockComments.default,
            onDraftChanged = {},
        )
    }
}
