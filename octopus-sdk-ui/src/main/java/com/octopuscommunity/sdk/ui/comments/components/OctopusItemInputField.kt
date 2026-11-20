package com.octopuscommunity.sdk.ui.comments.components

import android.net.Uri
import android.util.Size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.test.mock.MockComments
import com.octopuscommunity.sdk.test.mock.MockPosts
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.validation.ValidateImage
import com.octopuscommunity.sdk.ui.common.validation.ValidateText
import com.octopuscommunity.sdk.ui.components.OctopusFilledIconButton
import com.octopuscommunity.sdk.ui.components.OctopusMediaField
import com.octopuscommunity.sdk.ui.components.OctopusOutlinedIconButton
import com.octopuscommunity.sdk.ui.components.OctopusOutlinedTextFieldDefaults
import com.octopuscommunity.sdk.ui.components.OctopusTextField
import com.octopuscommunity.sdk.ui.consent.ImplicitConsentNotice
import com.octopuscommunity.sdk.ui.consent.resolveImplicitNotice
import com.octopuscommunity.sdk.ui.utils.OctopusMotion
import com.octopuscommunity.sdk.ui.utils.rememberImagePicker

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T : OctopusItem.Draft> OctopusItemInputField(
    state: OctopusItemInputFieldViewModel.UiState<T>,
    placeholder: String,
    onTextChanged: (String) -> Unit,
    onImageUpdated: (Uri?) -> Unit,
    onFocused: () -> Unit,
    onSend: () -> Unit,
    onNavigateToTermsOfUse: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToCommunityGuidelines: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    addPictureIcon: Painter =
        OctopusTheme.icons.content.comment.creation
            .addPicture(),
    deleteImageIcon: Painter = OctopusTheme.icons.content.delete(),
    sendIcon: Painter =
        OctopusTheme.icons.content.comment.creation
            .create(),
    // Opens a link written in the community's own consent notice (host URL handler).
    onNavigateToUrl: (url: String) -> Unit = {},
) = with(state) {
    val hapticFeedback = LocalHapticFeedback.current
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = item.text,
                selection = TextRange(item.text.length),
            ),
        )
    }

    var isFocused by remember { mutableStateOf(false) }
    val imagePicker = rememberImagePicker(onImageSelected = onImageUpdated)

    LaunchedEffect(item.text) {
        if (textFieldValue.text != item.text) {
            textFieldValue = textFieldValue.copy(item.text)
        }
    }

    // Focus is a writing intent even with a hardware keyboard. The thread follows IME geometry
    // separately, rather than waiting on two different keyboard-visibility signals here.
    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocused()
        }
    }

    val shape =
        remember {
            RoundedCornerShape(
                topStart = 32.dp,
                topEnd = 32.dp,
            )
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(OctopusTheme.colorScheme.background)
                .border(
                    width = 1.dp,
                    shape = shape,
                    brush =
                        Brush.verticalGradient(
                            0f to OctopusTheme.colorScheme.gray300,
                            0.5f to Color.Transparent,
                        ),
                ).verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (enablePictures) {
                OctopusOutlinedIconButton(
                    modifier = Modifier.size(40.dp),
                    icon = addPictureIcon,
                    contentDescription = stringResource(R.string.accessibility_content_add_picture),
                    enabled = state.isEditable,
                    onClick = imagePicker::launch,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color =
                                    OctopusOutlinedTextFieldDefaults
                                        .borderColors()
                                        .let {
                                            when {
                                                !isEditable -> it.disabledColor
                                                isFocused -> it.focusedColor
                                                hasErrors -> it.errorColor
                                                else -> it.unfocusedColor
                                            }
                                        },
                                shape = RoundedCornerShape(8.dp),
                            ).clip(RoundedCornerShape(8.dp))
                            .background(
                                color =
                                    OctopusOutlinedTextFieldDefaults
                                        .colors()
                                        .let {
                                            when {
                                                !isEditable -> it.disabledContainerColor
                                                isFocused -> it.focusedContainerColor
                                                hasErrors -> it.errorContainerColor
                                                else -> it.unfocusedContainerColor
                                            }
                                        },
                            ),
                ) {
                    OctopusTextField(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    isFocused = focusState.isFocused
                                },
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            onTextChanged(it.text)
                        },
                        enabled = isEditable,
                        error = hasErrors,
//                            readOnly = !isEditable,
                        textStyle = OctopusTheme.typography.body2,
                        placeholder = placeholder,
                        keyboardOptions =
                            KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                        singleLine = false,
                        minLines = 1,
                        maxLines = 7,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    )
                    item.media?.let {
                        OctopusMediaField(
                            modifier =
                                Modifier
                                    .heightIn(max = 150.dp)
                                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            media = it,
                            deleteIcon = deleteImageIcon,
                            enabled = isEditable,
                            contentScale = ContentScale.FillHeight,
                            onDeleteImage = { onImageUpdated(null) },
                        )
                    }
                }
                listOfNotNull(
                    textError?.asString(),
                    mediaError?.asString(),
                    serverErrors?.asString(),
                ).takeIf { it.isNotEmpty() }?.let { errors ->
                    errors.forEach {
                        Text(
                            modifier =
                                Modifier
                                    .padding(top = 4.dp),
                            text = it,
                            color = OctopusTheme.colorScheme.alert,
                            style = OctopusTheme.typography.caption2,
                        )
                    }
                }
            }

            OctopusFilledIconButton(
                modifier = Modifier.size(40.dp),
                icon = sendIcon,
                contentDescription = stringResource(R.string.accessibility_content_send),
                enabled = canSend,
                loading = isSending,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSend()
                },
            )
        }

        AnimatedVisibility(
            visible = state.showCguText,
            enter =
                fadeIn(tween(OctopusMotion.SHORT_DURATION_MS)) +
                    expandVertically(tween(OctopusMotion.SHORT_DURATION_MS)),
            exit =
                fadeOut(tween(OctopusMotion.SHORT_DURATION_MS)) +
                    shrinkVertically(tween(OctopusMotion.SHORT_DURATION_MS)),
        ) {
            Column {
                HorizontalDivider(color = OctopusTheme.colorScheme.gray300)

                ImplicitConsentNotice(
                    wording = resolveImplicitNotice(mode = termsAcceptanceMode, consentTexts = consentTexts),
                    onNavigateToTermsOfUse = onNavigateToTermsOfUse,
                    onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy,
                    onNavigateToCommunityGuidelines = onNavigateToCommunityGuidelines,
                    onNavigateToUrl = onNavigateToUrl,
                )
            }
        }
    }
}

@Preview
@Composable
private fun OctopusItemInputFieldEmptyPreview() {
    OctopusTheme {
        OctopusItemInputField(
            state =
                OctopusItemInputFieldViewModel.UiState(
                    item = Comment.Draft(post = MockPosts.default),
                ),
            placeholder = "Add a comment",
            onTextChanged = {},
            onImageUpdated = {},
            onFocused = {},
            onSend = {},
            onNavigateToTermsOfUse = {},
            onNavigateToPrivacyPolicy = {},
            onNavigateToCommunityGuidelines = {},
        )
    }
}

@Preview
@Composable
private fun OctopusItemInputFieldShortTextPreview() {
    OctopusTheme {
        OctopusItemInputField(
            state =
                OctopusItemInputFieldViewModel.UiState(
                    item =
                        Comment.Draft(
                            post = MockPosts.default,
                            text = MockComments.withShortText.text.original.text,
                            media =
                                MockComments.default.medias
                                    ?.images
                                    ?.firstOrNull(),
                        ),
                    textError =
                        ValidateText().invoke(
                            text = "Error",
                            maxLength = 2,
                        ),
                    mediaError = ValidateImage().invoke(Size(45000, 4500)),
                ),
            placeholder = "Add a comment",
            onTextChanged = {},
            onImageUpdated = {},
            onFocused = {},
            onSend = {},
            onNavigateToTermsOfUse = {},
            onNavigateToPrivacyPolicy = {},
            onNavigateToCommunityGuidelines = {},
        )
    }
}

@Preview
@Composable
private fun OctopusItemInputFieldMultilinePreview() {
    OctopusTheme {
        OctopusItemInputField(
            state =
                OctopusItemInputFieldViewModel.UiState(
                    item =
                        Comment.Draft(
                            post = MockPosts.default,
                            text = MockComments.default.text.getText(translated = true),
                        ),
                    isSending = true,
                ),
            placeholder = "Add a comment",
            onTextChanged = {},
            onImageUpdated = {},
            onFocused = {},
            onSend = {},
            onNavigateToTermsOfUse = {},
            onNavigateToPrivacyPolicy = {},
            onNavigateToCommunityGuidelines = {},
        )
    }
}
