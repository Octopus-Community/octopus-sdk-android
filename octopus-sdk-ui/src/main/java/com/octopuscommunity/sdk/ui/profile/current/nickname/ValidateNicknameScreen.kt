package com.octopuscommunity.sdk.ui.profile.current.nickname

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.NavigationIconType
import com.octopuscommunity.sdk.ui.components.OctopusButton
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusOutlinedTextField
import com.octopuscommunity.sdk.ui.components.OctopusScaffold
import com.octopuscommunity.sdk.ui.components.OctopusScreenContent
import com.octopuscommunity.sdk.ui.profile.components.ProfileAvatar
import com.octopuscommunity.sdk.ui.profile.components.ProfilePictureEditMenu
import com.octopuscommunity.sdk.ui.profile.components.rememberProfilePictureEditor
import com.octopuscommunity.sdk.ui.semiBold
import com.octopuscommunity.sdk.ui.utils.safeImePadding
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * Asks a member for their nickname, and optionally a picture, before their first post, comment or
 * reply goes. It is presented like post creation, so its close button (as on iOS) and system back
 * both leave it without a choice: the contribution is not sent and its draft stays in the composer,
 * which asks again on the next try.
 */
@Composable
fun ValidateNicknameScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) = OctopusContent(navController) {
    val viewModel = octopusViewModel<ValidateNicknameViewModel>()

    // Drops the held contribution at once: a plain pop would only drop it in onCleared, after the exit
    // transition, and a confirmation landing meanwhile would make the composer send it. Predictive back
    // is therefore off on this screen.
    BackHandler(onBack = viewModel::closeWithoutChoice)

    OctopusScaffold(
        modifier = modifier,
        viewModel = viewModel,
        // Not null: a null title shows the host's logo instead.
        title = "",
        onBack = viewModel::closeWithoutChoice,
        navigationIcon = NavigationIconType.Close,
    ) { innerPadding ->
        OctopusScreenContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .safeImePadding(),
            navController = navController,
        ) {
            val context = LocalContext.current
            val state by viewModel.state.collectAsStateWithLifecycle()

            val pictureEditor =
                rememberProfilePictureEditor(
                    hasPicture = state.picture != null,
                    onPicked = { uri -> viewModel.pickPicture(context = context, uri = uri) },
                )

            val nicknameFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                // Lets the navigation transition settle before the keyboard opens.
                delay(0.1.seconds)
                nicknameFocusRequester.requestFocus()
            }

            ValidateNicknameContent(
                modifier = Modifier.fillMaxSize(),
                state = state,
                onNicknameChange = viewModel::edit,
                onClear = viewModel::clear,
                onPick = viewModel::pick,
                onSubmit = viewModel::submit,
                onEditPicture = pictureEditor::edit,
                nicknameFocusRequester = nicknameFocusRequester,
            )

            ProfilePictureEditMenu(
                editor = pictureEditor,
                onDeleted = viewModel::deletePicture,
            )
        }
    }
}

@Composable
internal fun ValidateNicknameContent(
    state: ValidateNicknameViewModel.UiState,
    onNicknameChange: (String) -> Unit,
    onClear: () -> Unit,
    onPick: (String) -> Unit,
    onSubmit: () -> Unit,
    onEditPicture: () -> Unit,
    modifier: Modifier = Modifier,
    nicknameFocusRequester: FocusRequester = remember { FocusRequester() },
) {
    Column(
        modifier =
            modifier
                .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            ProfileAvatar(
                modifier = Modifier.size(72.dp),
                profile = state.avatarProfile,
                // The initials follow the nickname being typed, the color stays the stored nickname's.
                colorSeed = state.profile?.nickname,
                actionIcon = if (state.canEditPicture) OctopusTheme.icons.profile.editPicture() else null,
                onClick = if (state.canEditPicture) onEditPicture else null,
                onActionClick = onEditPicture,
            )
            state.form.pictureError?.let { pictureError ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = pictureError.asString(),
                    color = OctopusTheme.colorScheme.alert,
                    textAlign = TextAlign.Center,
                    style = OctopusTheme.typography.caption2,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { heading() },
                text = stringResource(R.string.validate_nickname_choose_title_formal),
                color = OctopusTheme.colorScheme.gray900,
                textAlign = TextAlign.Center,
                style = OctopusTheme.typography.title1.semiBold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.validate_nickname_choose_subtitle_formal),
                color = OctopusTheme.colorScheme.gray700,
                textAlign = TextAlign.Center,
                style = OctopusTheme.typography.body2,
            )

            Spacer(modifier = Modifier.height(24.dp))

            NicknameField(
                state = state,
                onNicknameChange = onNicknameChange,
                onClear = onClear,
                onSubmit = onSubmit,
                focusRequester = nicknameFocusRequester,
            )

            if (state.form.suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                NicknameSuggestions(
                    suggestions = state.form.suggestions,
                    onPick = onPick,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OctopusButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(state.contribution.buttonRes),
            onClick = onSubmit,
            enabled = state.form.canSubmit,
            loading = state.isSubmitting,
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun NicknameField(
    state: ValidateNicknameViewModel.UiState,
    onNicknameChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
) {
    val nickname = state.form.nickname
    // Typing never resets the field, so late echoes cannot overwrite keystrokes and the IME composition
    // is kept; only changes made by the screen itself (prefill, clear, pick) move the text and cursor.
    var fieldValue by remember { mutableStateOf(TextFieldValue(nickname, TextRange(nickname.length))) }
    LaunchedEffect(state.form.revision) {
        fieldValue = TextFieldValue(nickname, TextRange(nickname.length))
    }

    OctopusOutlinedTextField(
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        value = fieldValue,
        onValueChange = {
            fieldValue = it
            onNicknameChange(it.text)
        },
        // Read-only rather than disabled while sending, so that the field keeps the focus and the keyboard.
        readOnly = state.isSubmitting,
        placeholder = stringResource(R.string.profile_nickname_placeholder_formal),
        error = state.form.error?.asString(),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        singleLine = true,
        trailingIcon =
            if (state.form.showsClearButton) {
                {
                    IconButton(onClick = onClear) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = OctopusTheme.icons.clearText(),
                            contentDescription = stringResource(R.string.common_delete),
                            tint = OctopusTheme.colorScheme.gray700,
                        )
                    }
                }
            } else {
                null
            },
    )
}

/** Available nicknames proposed after a taken one; the last visible one may be cut to show there are more. */
@Composable
private fun NicknameSuggestions(
    suggestions: List<String>,
    onPick: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        suggestions.forEach { suggestion ->
            SuggestionChip(
                onClick = { onPick(suggestion) },
                label = {
                    Text(
                        text = suggestion,
                        style = OctopusTheme.typography.caption1,
                        maxLines = 1,
                    )
                },
                shape = CircleShape,
                colors =
                    SuggestionChipDefaults.suggestionChipColors(
                        containerColor = OctopusTheme.colorScheme.background,
                        labelColor = OctopusTheme.colorScheme.gray900,
                    ),
                border =
                    SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = OctopusTheme.colorScheme.gray300,
                    ),
            )
        }
    }
}
