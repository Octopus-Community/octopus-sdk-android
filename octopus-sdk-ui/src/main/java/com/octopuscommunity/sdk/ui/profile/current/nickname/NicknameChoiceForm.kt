package com.octopuscommunity.sdk.ui.profile.current.nickname

import androidx.compose.runtime.Immutable
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.Resource
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateError
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.UiText

/** What the nickname choice screen shows and sends, kept free of Android so its rules are JVM-tested. */
@Immutable
internal data class NicknameChoiceForm(
    val nickname: String,
    /** Shown under the field. Typing removes it. */
    val error: UiText? = null,
    /** Shown under the picture. */
    val pictureError: UiText? = null,
    /**
     * Available nicknames proposed after a taken one. They stay while the member types, even in an
     * emptied field, and are replaced by the suggestions of the next taken nickname.
     */
    val suggestions: List<String> = emptyList(),
    /**
     * Bumped each time the screen itself replaces the text (opening, clear, pick), never by typing: the
     * field resets its text and cursor on this, so a late echo of typing cannot overwrite keystrokes.
     */
    val revision: Int = 0,
) {
    /** The button only needs a non-empty field: format and availability are checked when tapped. */
    val canSubmit: Boolean get() = nickname.isNotEmpty()
    val showsClearButton: Boolean get() = nickname.isNotEmpty()

    fun edit(text: String): NicknameChoiceForm = if (text == nickname) this else copy(nickname = text, error = null)

    /** Empties the field. The proposed nickname is not given back: the member types their own. */
    fun clear(): NicknameChoiceForm = edit("").copy(revision = revision + 1)

    /** Puts a suggestion in the field without sending it: the member can still change it. */
    fun pick(suggestion: String): NicknameChoiceForm = edit(suggestion).copy(revision = revision + 1)

    fun withError(error: UiText?): NicknameChoiceForm = copy(error = error)

    fun withPictureError(error: UiText?): NicknameChoiceForm = copy(pictureError = error)

    /**
     * The profile editor's length rule, or `null` when the nickname passes. An untouched nickname is
     * not sent, so it is not checked either.
     */
    fun localError(currentNickname: String): UiText? {
        val typed = nickname.trim()
        if (typed == currentNickname) return null
        val range = CurrentUserProfile.NICKNAME_LENGTH
        return if (typed.length in range) {
            null
        } else {
            UiText.StringResource(
                R.string.profile_edit_error_nickname_min_length_max_length,
                listOf(range.first, range.last),
            )
        }
    }

    /**
     * The update that confirms the nickname. The nickname is only sent when it changed (the mapper
     * compares with [current]), so a member who keeps the proposed one never meets an error. A picture
     * picked or removed here is confirmed too, like in the profile editor, so the host's picture does
     * not replace it later.
     */
    fun updatedProfile(
        current: CurrentUserProfile,
        picture: Resource?,
        pictureChanged: Boolean,
    ): CurrentUserProfile =
        current.copy(
            nickname = nickname.trim(),
            picture = if (pictureChanged) picture else current.picture,
            hasConfirmedNickname = true,
            hasConfirmedPicture = if (pictureChanged) true else current.hasConfirmedPicture,
        )

    /**
     * Shows the server's refusal: nickname and picture errors go under their field, and a taken
     * nickname replaces the suggestions. Returns the form and what must be shown in an alert instead,
     * if anything.
     */
    fun applying(errors: List<ProfileUpdateError>): Pair<NicknameChoiceForm, UiText?> {
        val nicknameMessages = mutableListOf<String>()
        val pictureMessages = mutableListOf<String>()
        val alertMessages = mutableListOf<String>()
        var suggestions = this.suggestions
        errors.forEach { error ->
            when (error) {
                is ProfileUpdateError.NicknameError.AlreadyUsed -> {
                    nicknameMessages += error.errorMessage
                    suggestions = error.suggestions
                }
                is ProfileUpdateError.NicknameError -> nicknameMessages += error.errorMessage
                is ProfileUpdateError.PictureError -> pictureMessages += error.errorMessage
                else -> alertMessages += error.errorMessage
            }
        }
        val form =
            copy(
                error = nicknameMessages.joined() ?: error,
                pictureError = pictureMessages.joined() ?: pictureError,
                suggestions = suggestions,
            )
        return form to alertMessages.joined()
    }

    private fun List<String>.joined(): UiText? = if (isEmpty()) null else UiText.DynamicString(joinToString("\n"))

    companion object {
        /** The form as the screen opens: empty when the community requires typing, else the current nickname. */
        fun opening(
            profile: CurrentUserProfile,
            forceNicknameChoice: Boolean,
        ) = NicknameChoiceForm(nickname = if (forceNicknameChoice) "" else profile.nickname, revision = 1)
    }
}
