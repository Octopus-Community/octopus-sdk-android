package com.octopuscommunity.sdk.ui.common.validation

import androidx.compose.runtime.Composable
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.ServerError
import com.octopuscommunity.sdk.domain.network.errorMessage
import com.octopuscommunity.sdk.ui.common.UiText

open class ValidationError(open val message: UiText?) {

    constructor(serverMessage: String) : this(UiText.DynamicString(serverMessage))

    @Composable
    fun asString() = message?.asString()
}

inline fun <reified R : ServerError> OctopusResult.Failure.InvalidArguments<*>.findError() =
    filterErrors<R>().errorMessage?.let { ValidationError(it) }