package com.octopuscommunity.sdk.ui.profile.current.nickname

import android.content.Context
import android.net.Uri
import android.util.Size
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ScreenDisplayed
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.domain.model.ProfileFieldLockState
import com.octopuscommunity.sdk.domain.model.Resource
import com.octopuscommunity.sdk.domain.model.getDimensions
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.usecase.UpdateProfileUseCase
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.OctopusDialogState
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.UiText.StringResource
import com.octopuscommunity.sdk.ui.common.errorText
import com.octopuscommunity.sdk.ui.common.validation.ValidateImage
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import com.octopuscommunity.sdk.ui.profile.components.withEditPreviewDefaultAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The nickname choice shown before a member's first post, comment or reply goes. It closes itself once
 * the member's nickname is confirmed (the holding composer then sends the contribution), or when
 * another member or none is connected (the composer drops it). The member leaves it by back, or from
 * the alert of a failed confirmation: the contribution is never sent, the draft stays in the composer
 * and the choice is asked again on the next try.
 */
internal class ValidateNicknameViewModel internal constructor(
    commonParameters: CommonParameters,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val nicknameChoiceSession: NicknameChoiceSession,
    private val validateImage: ValidateImage,
) : OctopusViewModel(commonParameters) {
    @Immutable
    data class UiState(
        val contribution: NicknameChoiceContribution = NicknameChoiceContribution.POST,
        val profile: CurrentUserProfile? = null,
        val form: NicknameChoiceForm = NicknameChoiceForm(nickname = ""),
        val picture: Resource? = null,
        val pictureChanged: Boolean = false,
        val canEditPicture: Boolean = false,
        val isSubmitting: Boolean = false,
    ) {
        /**
         * The avatar follows the nickname being typed and the picture being picked, and shows the default
         * avatar the member gets once they remove their picture.
         */
        val avatarProfile: CurrentUserProfile?
            get() = profile?.copy(nickname = form.nickname, picture = picture)?.withEditPreviewDefaultAvatar()
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    /**
     * Set once the screen has asked to close, whatever the reason: it then closes only once, and
     * [onCleared] knows the held contribution was already resolved. After a SEND, emitting
     * closedWithoutChoice there could make a composer drop a hold it has not sent yet; after a DROP
     * the composers already released their hold.
     */
    private var closed = false

    /**
     * The failure alert this screen shows, if any. The dialog state lives on the shared navigator, so
     * the screen only hides it on closing while it is still this one: it would otherwise stay over
     * the composer, which may also have shown its own dialog by then.
     */
    private var failureDialog: OctopusDialogState? = null

    init {
        val contribution = nicknameChoiceSession.pending.value
        if (contribution == null) {
            // Opened without a held contribution (a deep link): there is nothing to choose a nickname for.
            close()
        } else {
            _state.update { it.copy(contribution = contribution) }
            viewModelScope.launch { open() }
            viewModelScope.launch { eventRepository.emitEvent(ScreenDisplayed.ValidateNickname) }
        }
    }

    private suspend fun open() {
        // Read from the repository rather than `currentUserProfile`: the initial null of that state
        // would resolve to DROP below and close the screen at once.
        val profile = connectionRepository.getProfile()
        if (profile == null) {
            close()
            return
        }
        val config = configRepository.getCommunityConfig()
        val appManagedPicture = !profile.isGuest && ProfileField.PICTURE in appManagedFields
        val avatarLock = config?.profileFieldsLock?.avatar ?: ProfileFieldLockState.EDITABLE
        _state.update {
            it.copy(
                profile = profile,
                form = NicknameChoiceForm.opening(profile, forceNicknameChoice = config?.forceNicknameChoice == true),
                picture = profile.picture,
                canEditPicture = !appManagedPicture && avatarLock == ProfileFieldLockState.EDITABLE,
            )
        }
        // The same resolution as the composer's hold, so the screen leaves when the composer acts.
        val hold = NicknameChoiceHold(profileId = profile.userId)
        connectionRepository.getProfileFlow().collect { current ->
            when (hold.resolution(current)) {
                NicknameChoiceHold.Resolution.SEND ->
                    if (!closed) {
                        nicknameChoiceSession.finish()
                        close()
                    }

                // The composers drop their hold on their own: the screen only leaves.
                NicknameChoiceHold.Resolution.DROP -> close()

                // Keep the latest profile of the member as the base of the update.
                NicknameChoiceHold.Resolution.KEEP_WAITING -> _state.update { it.copy(profile = current) }
            }
        }
    }

    fun edit(text: String) = updateForm { it.edit(text) }

    fun clear() = updateForm { it.clear() }

    fun pick(suggestion: String) = updateForm { it.pick(suggestion) }

    /** Ignored while the nickname is being sent: the field is read-only then, but keeps the keyboard. */
    private fun updateForm(change: (NicknameChoiceForm) -> NicknameChoiceForm) {
        _state.update { if (it.isSubmitting) it else it.copy(form = change(it.form)) }
    }

    fun pickPicture(
        context: Context,
        uri: Uri,
    ) {
        if (!_state.value.canEditPicture || _state.value.isSubmitting) return
        val appContext = context.applicationContext
        viewModelScope.launch {
            // An unreadable uri (a revoked permission, a deleted file) reads like an undecodable
            // image: a size that fails the validation, so it shows the invalid picture error.
            val size =
                withContext(Dispatchers.IO) {
                    runCatching { uri.getDimensions(appContext) }.getOrDefault(Size(0, 0))
                }
            val error = validateImage(size)
            _state.update {
                when {
                    it.isSubmitting -> it
                    error != null -> it.copy(form = it.form.withPictureError(error.message))
                    else ->
                        it.copy(
                            picture = Resource.Local(uri = uri),
                            pictureChanged = true,
                            form = it.form.withPictureError(null),
                        )
                }
            }
        }
    }

    fun deletePicture() {
        _state.update {
            if (it.isSubmitting || !it.canEditPicture) {
                it
            } else {
                it.copy(picture = null, pictureChanged = true, form = it.form.withPictureError(null))
            }
        }
    }

    fun submit() {
        val state = _state.value
        val profile = state.profile ?: return
        if (!state.form.canSubmit || state.isSubmitting || closed) return
        state.form.localError(currentNickname = profile.nickname)?.let { error ->
            _state.update { it.copy(form = it.form.withError(error)) }
            return
        }
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            // A call stuck on a dead connection would leave the member on a spinner, with back as the only
            // way out, so it gives up after a while and offers the failure alert's Close.
            val result =
                withDeadline(CONFIRMATION_TIMEOUT) {
                    updateProfileUseCase(
                        newProfile = state.form.updatedProfile(profile, state.picture, state.pictureChanged),
                        oldProfile = profile,
                        findAvailableNickname = false,
                        wantNicknameSuggestions = true,
                    )
                }
            // Closed meanwhile (another member connected, the nickname confirmed elsewhere): an alert
            // would now show over the composer.
            if (closed) return@launch
            when (result) {
                // Not "no network": a slow but alive picture upload can take that long too.
                null -> {
                    _state.update { it.copy(isSubmitting = false) }
                    showFailure(StringResource(R.string.error_unknown_formal))
                }

                is OctopusResult.Success ->
                    if (!result.data.profileUpdate.newProfile.hasConfirmedNickname) {
                        _state.update { it.copy(isSubmitting = false) }
                        showFailure(StringResource(R.string.error_unknown_formal))
                    }
                // Otherwise the spinner stays: the confirmed profile closes the screen.

                is OctopusResult.Failure.InvalidArguments -> {
                    var alert: UiText? = null
                    _state.update {
                        val (form, formAlert) = it.form.applying(result.errors)
                        alert = formAlert
                        it.copy(form = form, isSubmitting = false)
                    }
                    alert?.let(::showFailure)
                }

                is OctopusResult.Failure -> {
                    _state.update { it.copy(isSubmitting = false) }
                    showFailure(result.errorText)
                }
            }
        }
    }

    /**
     * The result of [block], or `null` once [timeout] has passed, whatever [block] is doing then. Unlike
     * a plain [withTimeoutOrNull] around [block], it does not wait for work that ignores its
     * cancellation (a repository step run as non-cancellable): [block] runs in its own coroutine, which
     * is cancelled, best effort, at the deadline, and whose late result is dropped.
     */
    private suspend fun <T : Any> withDeadline(
        timeout: Duration,
        block: suspend () -> T,
    ): T? {
        val call = viewModelScope.async { block() }
        // Only the wait times out, and waiting always stops at once, whatever the call is doing.
        val result = withTimeoutOrNull(timeout) { call.await() }
        if (result == null) call.cancel()
        return result
    }

    /**
     * Close is the confirm action, so that dismissing the alert (outside tap, back) keeps the member on
     * the screen to try again.
     */
    private fun showFailure(text: UiText) {
        if (closed) return
        showConfirmDialog(
            title = StringResource(R.string.common_error),
            text = text,
            confirmButtonText = StringResource(R.string.common_close),
            dismissButtonText = StringResource(R.string.common_ok),
            onConfirm = ::closeWithoutChoice,
        )
        failureDialog = alertState.value
    }

    /**
     * Leaves the screen without a nickname (back, or the failure alert's Close): the contribution is
     * never sent; the draft stays in the composer. A confirmation still in flight is cancelled, best
     * effort, along with the screen.
     */
    fun closeWithoutChoice() {
        if (!markClosed()) return
        nicknameChoiceSession.closeWithoutChoice()
        navigateUp()
    }

    private fun close() {
        if (markClosed()) navigateUp()
    }

    /**
     * Marks the screen closed and hides its failure alert if that alert is still the one shown.
     * Returns `false` when the screen was already closed.
     */
    private fun markClosed(): Boolean {
        if (closed) return false
        closed = true
        val dialog = failureDialog
        failureDialog = null
        if (dialog != null && alertState.value === dialog) hideDialog()
        return true
    }

    override fun onCleared() {
        // Removed by anything other than its own closing paths: drop the held contribution, so that it
        // cannot be sent later behind the member's back.
        if (markClosed()) nicknameChoiceSession.closeWithoutChoice()
        super.onCleared()
    }

    internal companion object {
        /** How long confirming the nickname may take before the screen gives up on it, a picture included. */
        val CONFIRMATION_TIMEOUT = 30.seconds
    }
}
