package com.octopuscommunity.sdk.ui.profile.current.edit

import android.content.Context
import android.net.Uri
import android.util.Size
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ScreenDisplayed
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.domain.model.ProfileFieldLockState
import com.octopuscommunity.sdk.domain.model.ProfileFieldsLock
import com.octopuscommunity.sdk.domain.model.Resource
import com.octopuscommunity.sdk.domain.model.getDimensions
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateError.BioError
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateError.NicknameError
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateError.OtherError
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateError.PictureError
import com.octopuscommunity.sdk.domain.usecase.UpdateProfileUseCase
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.UiText.StringResource
import com.octopuscommunity.sdk.ui.common.UiTextLength
import com.octopuscommunity.sdk.ui.common.validation.ValidateImage
import com.octopuscommunity.sdk.ui.common.validation.ValidateText
import com.octopuscommunity.sdk.ui.common.validation.ValidationError
import com.octopuscommunity.sdk.ui.common.validation.findError
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CurrentUserProfileEditViewModel(
    commonParameters: CommonParameters,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val validateText: ValidateText,
    private val validateImage: ValidateImage,
) : OctopusViewModel(commonParameters) {
    @Immutable
    data class UiState(
        val originalProfile: CurrentUserProfile? = null,
        val profile: CurrentUserProfile = CurrentUserProfile(),
        private val appManagedFields: Set<ProfileField>,
        val profileFieldsLock: ProfileFieldsLock = ProfileFieldsLock.AllEditable,
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val pictureError: ValidationError? = null,
        val nicknameError: ValidationError? = null,
        val bioError: ValidationError? = null,
    ) {
        val bioLength =
            UiTextLength(
                content = profile.bio,
                maxLength = CurrentUserProfile.BIO_LENGTH.last,
            )
        val hasPicture = profile.picture != null
        val hasChanges = originalProfile != profile
        val isEditable = !isLoading && !isSaving
        private val isValid = listOf(nicknameError, bioError, pictureError).all { it == null }
        val canSave = (hasChanges || !profile.hasConfirmedNickname) && isEditable && isValid

        val appManagedNickname = !profile.isGuest && ProfileField.NICKNAME in appManagedFields
        val appManagedPicture = !profile.isGuest && ProfileField.PICTURE in appManagedFields
        val appManagedBio = !profile.isGuest && ProfileField.BIO in appManagedFields

        // A field is hidden from the edit screen when it is community-locked (read-only / disabled)
        // and NOT app-managed — the SSO app-managed redirect wins per field (internal ticket, Q4).
        val nicknameHidden =
            !appManagedNickname &&
                profileFieldsLock.nickname != ProfileFieldLockState.EDITABLE
        val pictureHidden =
            !appManagedPicture &&
                profileFieldsLock.avatar != ProfileFieldLockState.EDITABLE
        val bioHidden =
            !appManagedBio &&
                profileFieldsLock.bio != ProfileFieldLockState.EDITABLE
    }

    private val _state =
        MutableStateFlow(
            UiState(appManagedFields = appManagedFields),
        )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            currentUserProfile.collectLatest { profile ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        originalProfile = profile,
                        profile = profile ?: it.profile,
                    )
                }
            }
        }
        viewModelScope.launch {
            configRepository.getCommunityConfigFlow().collectLatest { config ->
                _state.update {
                    it.copy(
                        profileFieldsLock = config?.profileFieldsLock ?: ProfileFieldsLock.AllEditable,
                    )
                }
            }
        }
        viewModelScope.launch { eventRepository.emitEvent(ScreenDisplayed.EditProfile) }
    }

    fun updateProfile(profile: CurrentUserProfile) {
        _state.update { state ->
            state.copy(
                profile = profile,
                nicknameError =
                    when (state.appManagedNickname || state.nicknameHidden) {
                        true -> null
                        false ->
                            validateText(
                                text = profile.nickname,
                                lengthRange = CurrentUserProfile.NICKNAME_LENGTH,
                            )
                    },
                bioError =
                    when (state.appManagedBio || state.bioHidden) {
                        true -> null
                        false ->
                            validateText(
                                text = profile.bio ?: "",
                                lengthRange = CurrentUserProfile.BIO_LENGTH,
                            )
                    },
            )
        }
    }

    private var pictureUpdate: Job? = null

    fun updatePicture(
        context: Context,
        localUri: Uri?,
    ) {
        pictureUpdate?.cancel()
        val appContext = context.applicationContext ?: context
        pictureUpdate =
            viewModelScope.launch {
                val state = _state.value
                val error =
                    localUri?.takeUnless { state.appManagedPicture || state.pictureHidden }?.let { uri ->
                        withContext(Dispatchers.IO) {
                            val size =
                                runCatching { uri.getDimensions(appContext) }
                                    .getOrElse {
                                        if (it is kotlinx.coroutines.CancellationException) throw it
                                        Size(0, 0)
                                    }
                            validateImage(size)
                        }
                    }
                _state.update { current ->
                    current.copy(
                        profile =
                            current.profile.copy(picture = localUri?.let { Resource.Local(uri = it) }),
                        pictureError = error,
                    )
                }
            }
    }

    fun save() =
        viewModelScope.launch {
            // A picked URI is validated on IO. Capture the profile only after the latest pick
            // finishes, so a fast Save cannot silently submit the previous picture.
            while (pictureUpdate?.isActive == true) pictureUpdate?.join()
            val state = _state.value.takeIf { it.canSave } ?: return@launch

            _state.update { it.copy(isSaving = true) }
            when (
                val result =
                    updateProfileUseCase(
                        newProfile =
                            state.profile.copy(
                                nickname = state.profile.nickname.trim(),
                                bio = state.profile.bio?.trim(),
                                hasConfirmedNickname = true,
                                hasConfirmedBio = true,
                                hasConfirmedPicture = true,
                            ),
                        oldProfile = state.originalProfile,
                    )
            ) {
                is OctopusResult.Success -> navigateUp()
                is OctopusResult.Failure.InvalidArguments -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            pictureError = result.findError<PictureError>(),
                            nicknameError = result.findError<NicknameError>(),
                            bioError = result.findError<BioError>(),
                        )
                    }
                    result.findError<OtherError>()?.message?.let {
                        showAlertDialog(text = it)
                    }
                }

                is OctopusResult.Failure -> {
                    _state.update { it.copy(isSaving = false) }
                    showError(result)
                }
            }
        }

    fun onBack() {
        if (!forcedBack && _state.value.hasChanges) {
            showConfirmDialog(
                text = StringResource(R.string.common_cancel_modifications_formal),
                confirmButtonText = StringResource(R.string.common_yes),
                dismissButtonText = StringResource(R.string.common_no),
                onConfirm = { navigateUp() },
            )
        } else {
            navigateUp()
        }
    }
}
