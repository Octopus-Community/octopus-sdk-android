package com.octopuscommunity.sdk.ui.posts.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.Post.Draft
import com.octopuscommunity.sdk.domain.model.Post.Draft.Poll.Answer
import com.octopuscommunity.sdk.domain.model.Post.Draft.Poll.Companion.ANSWER_COUNT_RANGE
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.validation.ValidationError
import com.octopuscommunity.sdk.ui.components.OctopusIconButton
import com.octopuscommunity.sdk.ui.components.OctopusIconButtonDefaults
import com.octopuscommunity.sdk.ui.components.OctopusOutlinedButton
import com.octopuscommunity.sdk.ui.components.OctopusOutlinedTextField
import com.octopuscommunity.sdk.ui.lineHeight125

@Composable
fun PollField(
    poll: Draft.Poll,
    errors: Map<Answer, ValidationError?>,
    onPollChanged: (Draft.Poll?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focused: Boolean = true,
) = with(poll) {
    val focusRequesters =
        remember(answers) {
            List(answers.size) { FocusRequester() }
        }

    var shouldFocusAnswer by remember {
        mutableStateOf(answers.firstOrNull()?.takeIf { focused })
    }
    var focusedAnswer by remember { mutableStateOf<Answer?>(null) }

    val canAddAnswer =
        remember(answers) {
            answers.size < ANSWER_COUNT_RANGE.last
        }
    val canDeleteAnswer =
        remember(answers) {
            answers.size > ANSWER_COUNT_RANGE.first
        }
    val addAnswer = {
        if (canAddAnswer) {
            onPollChanged(
                copy(
                    answers =
                        answers +
                            Answer()
                                .also { shouldFocusAnswer = it },
                ),
            )
        }
    }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = OctopusTheme.colorScheme.gray300,
                    shape = RoundedCornerShape(24.dp),
                ).padding(16.dp)
                .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.poll_create_title),
                color = OctopusTheme.colorScheme.gray700,
                style = OctopusTheme.typography.caption1,
            )
            OctopusIconButton(
                modifier = Modifier.size(24.dp),
                icon =
                    OctopusTheme.icons.content.post.creation
                        .deletePoll(),
                contentDescription = stringResource(R.string.accessibility_poll_delete),
                colors =
                    OctopusIconButtonDefaults.iconButtonColors(
                        contentColor = OctopusTheme.colorScheme.gray900,
                    ),
                onClick = { onPollChanged(null) },
            )
        }
        answers.forEachIndexed { index, answer ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Text field for option
                OctopusOutlinedTextField(
                    modifier =
                        Modifier
                            .weight(1f)
                            .focusRequester(focusRequesters[index])
                            .onGloballyPositioned {
                                if (answer == shouldFocusAnswer && answer != focusedAnswer) {
                                    focusRequesters[index].requestFocus()
                                    focusedAnswer = answer
                                }
                            },
                    value = answer.text,
                    onValueChange = {
                        onPollChanged(
                            copy(
                                answers =
                                    answers.toMutableList().apply {
                                        this[index] = answer.copy(text = it)
                                    },
                            ),
                        )
                    },
                    enabled = enabled,
                    placeholder =
                        stringResource(
                            R.string.poll_create_option_text_placeholder,
                            index + 1,
                        ),
                    error = errors[answer]?.message?.asString(),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction =
                                if (answer == answers.lastOrNull() && !canAddAnswer) {
                                    ImeAction.Done
                                } else {
                                    ImeAction.Next
                                },
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrectEnabled = true,
                        ),
                    keyboardActions =
                        if (answer == answers.lastOrNull() && canAddAnswer) {
                            KeyboardActions(onNext = { addAnswer() })
                        } else {
                            KeyboardActions.Default
                        },
                    singleLine = true,
                    textStyle = OctopusTheme.typography.body2.lineHeight125,
                )

                Spacer(modifier = Modifier.width(8.dp))

                OctopusIconButton(
                    modifier = Modifier.size(24.dp),
                    icon =
                        OctopusTheme.icons.content.post.creation
                            .deletePoll(),
                    contentDescription = stringResource(R.string.accessibility_poll_option_delete),
                    enabled = canDeleteAnswer,
                    colors =
                        OctopusIconButtonDefaults.iconButtonColors(
                            contentColor = OctopusTheme.colorScheme.gray900,
                        ),
                    onClick = {
                        onPollChanged(
                            copy(answers = answers - answer),
                        )
                    },
                )
            }
        }

        if (canAddAnswer) {
            OctopusOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                icon =
                    OctopusTheme.icons.content.post.creation
                        .addPoll(),
                shape = RoundedCornerShape(8.dp),
                text = stringResource(R.string.poll_create_option_add),
                textStyle = OctopusTheme.typography.body2,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                onClick = addAnswer,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PollFieldPreview() {
    OctopusTheme {
        PollField(
            modifier = Modifier.fillMaxWidth(),
            poll = Draft.Poll(),
            onPollChanged = { _ -> },
            errors = emptyMap(),
        )
    }
}
