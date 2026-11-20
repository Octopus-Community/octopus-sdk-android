package com.octopuscommunity.sdk.ui.common.validation


sealed interface EmailValidationError {
    data object InvalidFormat : EmailValidationError
}

class ValidateEmail {
    private val emailRegex by lazy { "^[A-Za-z][A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]+\$".toRegex() }

    operator fun invoke(text: String): EmailValidationError? = when (text.matches(emailRegex)) {
        true -> null
        false -> EmailValidationError.InvalidFormat
    }
}