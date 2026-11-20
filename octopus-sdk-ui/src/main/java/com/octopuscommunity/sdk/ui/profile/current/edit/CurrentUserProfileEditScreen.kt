package com.octopuscommunity.sdk.ui.profile.current.edit

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.snackbar.OctopusSnackbarHost
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusOutlinedTextField
import com.octopuscommunity.sdk.ui.components.OctopusPoweredBy
import com.octopuscommunity.sdk.ui.components.OctopusScaffold
import com.octopuscommunity.sdk.ui.components.OctopusScreenContent
import com.octopuscommunity.sdk.ui.components.OctopusSmallButton
import com.octopuscommunity.sdk.ui.components.OctopusSystemBackHandler
import com.octopuscommunity.sdk.ui.lineHeight125
import com.octopuscommunity.sdk.ui.profile.components.ProfileAvatar
import com.octopuscommunity.sdk.ui.profile.components.ProfilePictureEditMenu
import com.octopuscommunity.sdk.ui.profile.components.rememberProfilePictureEditor
import com.octopuscommunity.sdk.ui.profile.components.withEditPreviewDefaultAvatar
import com.octopuscommunity.sdk.ui.utils.safeImePadding
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun CurrentUserProfileEditScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    focusedField: ProfileField? = null,
) = OctopusContent(navController) {
    val viewModel = octopusViewModel<CurrentUserProfileEditViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    OctopusScaffold(
        modifier = modifier,
        viewModel = viewModel,
        onBack = viewModel::onBack,
        title = stringResource(R.string.common_edit),
        actions = {
            OctopusSmallButton(
                modifier = Modifier.padding(end = 8.dp),
                onClick = viewModel::save,
                text = stringResource(R.string.common_save),
                enabled = state.canSave,
                loading = state.isSaving,
            )
        },
    ) { innerPadding ->
        CurrentUserProfileEditContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            focusedField = focusedField,
            navController = navController,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("RememberReturnType")
@Composable
internal fun CurrentUserProfileEditContent(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    focusedField: ProfileField? = null,
) = OctopusScreenContent(
    modifier =
        modifier
            .safeImePadding(),
    navController = navController,
) { screenViewModel ->
    val context = LocalContext.current

    val viewModel = octopusViewModel<CurrentUserProfileEditViewModel>()

    val state by viewModel.state.collectAsStateWithLifecycle()
    val screenState by screenViewModel.state.collectAsStateWithLifecycle()

    val pictureEditor =
        rememberProfilePictureEditor(
            hasPicture = state.hasPicture,
            onPicked = { uri -> viewModel.updatePicture(context = context, localUri = uri) },
        )

    val editPicture = {
        if (state.appManagedPicture) {
            viewModel.navigateToClientProfileEdit(fieldToEdit = ProfileField.PICTURE)
        } else {
            pictureEditor.edit()
        }
    }

    val nicknameFocusRequester = remember { FocusRequester() }
    var nicknameSelection by remember { mutableStateOf(TextRange.Zero) }

    val bioFocusRequester = remember { FocusRequester() }
    LaunchedEffect(focusedField) {
        when (focusedField) {
            // Guard on `*Hidden` too: a community-locked field is not composed, so its focus
            // requester is never attached (internal ticket).
            ProfileField.NICKNAME ->
                if (!state.appManagedNickname && !state.nicknameHidden) {
                    // Small delay to ensure the TextField is composed
                    delay(0.1.seconds)
                    nicknameSelection = TextRange(state.profile.nickname.length)
                    nicknameFocusRequester.requestFocus()
                }

            ProfileField.BIO ->
                if (!state.appManagedBio && !state.bioHidden) {
                    // Small delay to ensure the TextField is composed
                    delay(0.1.seconds)
                    bioFocusRequester.requestFocus()
                }

            ProfileField.PICTURE ->
                if (!state.appManagedPicture && !state.pictureHidden) {
                    delay(0.1.seconds)
                    editPicture()
                }

            else -> {}
        }
    }

    // System back: the NavHost pops on its own (keeping the predictive-back preview) except
    // while a cancel-modifications dialog must be shown first, or at the SDK back-stack root, where
    // NavigateUp falls back to the host `onBack` (bridge mode). Never enabled at the root
    // without a host `onBack` (internal change). See OctopusSystemBackHandler.
    OctopusSystemBackHandler(
        navController = navController,
        hasPendingConfirmation = state.hasChanges,
        onBack = viewModel::onBack,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            // Read-only / disabled picture: the avatar block is removed from the edit screen
            // (internal ticket). App-managed pictures keep their edit-in-app redirect.
            if (!state.pictureHidden) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ProfileAvatar(
                        modifier = Modifier.size(90.dp),
                        profile = state.profile.withEditPreviewDefaultAvatar(),
                        // The initials follow the nickname being typed, the color stays the saved nickname's.
                        colorSeed = state.originalProfile?.nickname,
                        actionIcon = OctopusTheme.icons.profile.editPicture(),
                        onClick = editPicture,
                        onActionClick = editPicture,
                    )
                    state.pictureError?.message?.let { pictureError ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = pictureError.asString(),
                            color = OctopusTheme.colorScheme.alert,
                            style = OctopusTheme.typography.caption1,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }

            // Read-only / disabled nickname: the field is removed from the edit screen (internal ticket).
            if (!state.nicknameHidden) {
                Text(
                    text = stringResource(R.string.profile_edit_nickname_description),
                    color = OctopusTheme.colorScheme.gray900,
                    style = OctopusTheme.typography.body2,
                )
                Spacer(modifier = Modifier.height(6.dp))
                OctopusOutlinedTextField(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(nicknameFocusRequester),
                    value =
                        TextFieldValue(
                            text = state.profile.nickname,
                            selection = nicknameSelection,
                        ),
                    onValueChange = {
                        nicknameSelection = it.selection
                        viewModel.updateProfile(
                            state.profile.copy(nickname = it.text),
                        )
                    },
                    onClick =
                        if (state.appManagedNickname) {
                            { viewModel.navigateToClientProfileEdit(fieldToEdit = ProfileField.NICKNAME) }
                        } else {
                            null
                        },
                    enabled = state.isEditable,
                    placeholder = stringResource(R.string.profile_nickname_placeholder_formal),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                            capitalization = KeyboardCapitalization.Words,
                            autoCorrectEnabled = false,
                        ),
                    error = state.nicknameError?.message?.asString(),
                    singleLine = true,
                    textStyle = OctopusTheme.typography.body2.lineHeight125,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.profile_edit_nickname_explanation_formal),
                    color = OctopusTheme.colorScheme.gray500,
                    style = OctopusTheme.typography.caption1,
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Read-only / disabled bio: the field is removed from the edit screen (internal ticket).
            if (!state.bioHidden) {
                // Bio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.profile_edit_bio_description),
                        color = OctopusTheme.colorScheme.gray900,
                        style = OctopusTheme.typography.body2,
                    )
                    if (!state.appManagedBio) {
                        Text(
                            text = state.bioLength.text.asString(),
                            color =
                                when (state.bioLength.isValid) {
                                    true -> OctopusTheme.colorScheme.gray500
                                    else -> OctopusTheme.colorScheme.alert
                                },
                            style = OctopusTheme.typography.body2,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OctopusOutlinedTextField(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(bioFocusRequester),
                    value = state.profile.bio ?: "",
                    onValueChange = { viewModel.updateProfile(state.profile.copy(bio = it)) },
                    onClick =
                        if (state.appManagedBio) {
                            { viewModel.navigateToClientProfileEdit(fieldToEdit = ProfileField.BIO) }
                        } else {
                            null
                        },
                    enabled = state.isEditable,
                    placeholder = stringResource(R.string.profile_edit_bio_placeholder_formal),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                    error = state.bioError?.message?.asString(),
                    singleLine = false,
                    minLines = 8,
                    maxLines = 8,
                    textStyle = OctopusTheme.typography.body2.lineHeight125,
                )
            }
        }

        if (!WindowInsets.isImeVisible) {
            OctopusPoweredBy(communityConfig = screenState.communityConfig)
        }
    }

    ProfilePictureEditMenu(
        editor = pictureEditor,
        onDeleted = { viewModel.updatePicture(context = context, localUri = null) },
    )

    OctopusSnackbarHost(
        modifier = Modifier.align(Alignment.BottomCenter),
        screenViewModel = screenViewModel,
    )
}

@Preview
@Composable
private fun CurrentUserProfileEditScreenPreview() {
    OctopusTheme {
        CurrentUserProfileEditScreen(
            navController = rememberNavController(),
        )
    }
}
