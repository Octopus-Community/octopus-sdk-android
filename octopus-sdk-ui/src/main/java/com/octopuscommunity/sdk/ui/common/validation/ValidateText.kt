package com.octopuscommunity.sdk.ui.common.validation

import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.UiText.StringResource

sealed class TextValidationError(
    override val message: UiText?,
) : ValidationError(message) {
    data class TooShort(
        val length: Int,
        val minLength: Int,
        override val message: UiText?,
    ) : TextValidationError(message)

    data class TooLong(
        val length: Int,
        val maxLength: Int,
        override val message: UiText?,
    ) : TextValidationError(message)
}

class ValidateText {
    operator fun invoke(
        text: String,
        mandatory: Boolean = true,
        maxLength: Int = Int.MAX_VALUE,
    ): TextValidationError? =
        invoke(
            text = text,
            lengthRange = (if (mandatory) 1 else 0)..maxLength,
        )

    operator fun invoke(
        text: String,
        lengthRange: IntRange = 0..Int.MAX_VALUE,
        tooShortMessage: (length: Int, minLength: Int) -> StringResource? = { _, _ -> null },
        tooLongMessage: (length: Int, maxLength: Int) -> StringResource? = { length, maxLength ->
            StringResource(
                R.string.error_text_too_long_current_length_max_length_formal,
                listOf(length, maxLength),
            )
        },
    ): TextValidationError? =
        when (val textLength = text.length) {
            in 0..<lengthRange.first ->
                TextValidationError.TooShort(
                    length = textLength,
                    minLength = lengthRange.first,
                    message = tooShortMessage(textLength, lengthRange.first),
                )

            in lengthRange -> null
            else ->
                TextValidationError.TooLong(
                    length = textLength,
                    maxLength = lengthRange.last,
                    message = tooLongMessage(textLength, lengthRange.last),
                )
        }
}
