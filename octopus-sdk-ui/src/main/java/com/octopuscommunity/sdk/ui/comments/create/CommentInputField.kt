package com.octopuscommunity.sdk.ui.comments.create

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
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.test.mock.MockPosts
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
fun CommentInputField(
    post: Post,
    onDraftChanged: (Comment.Draft) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocused: () -> Unit = {},
) = OctopusContent {
    val context = LocalContext.current

    val viewModel =
        octopusViewModel<CommentInputFieldViewModel>(
            key = "commentInputField_${post.id}",
            parameters = { parametersOf(post) },
        )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.item) {
        onDraftChanged(state.item)
    }

    OctopusItemInputField(
        modifier = modifier,
        state = state,
        placeholder = stringResource(R.string.comment_create_text_placeholder),
        addPictureIcon =
            OctopusTheme.icons.content.comment.creation
                .addPicture(),
        deleteImageIcon =
            OctopusTheme.icons.content.comment.creation
                .deletePicture(),
        sendIcon =
            OctopusTheme.icons.content.comment.creation
                .create(),
        focusRequester = focusRequester,
        onTextChanged = viewModel::updateText,
        onImageUpdated = { viewModel.updateImage(context = context, localUri = it) },
        onFocused = onFocused,
        onSend = viewModel::send,
        onNavigateToTermsOfUse = viewModel::navigateToTermsOfUse,
        onNavigateToPrivacyPolicy = viewModel::navigateToPrivacyPolicy,
        onNavigateToCommunityGuidelines = viewModel::navigateToCommunityGuidelines,
        onNavigateToUrl = viewModel::navigateToUrl,
    )

    TermsConsentSheet(
        visible = state.showConsentSheet,
        mode = state.termsAcceptanceMode,
        contribution = ConsentContribution.COMMENT,
        wordings =
            resolveConsentWordings(
                mode = state.termsAcceptanceMode,
                consentTexts = state.consentTexts,
                contribution = ConsentContribution.COMMENT,
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
private fun CommentInputFieldPreview() {
    OctopusTheme {
        CommentInputField(
            post = MockPosts.default,
            onDraftChanged = {},
        )
    }
}
