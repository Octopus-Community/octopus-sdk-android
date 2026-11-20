package com.octopuscommunity.sdk.ui.common

import android.content.Context
import androidx.compose.runtime.Composable
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.OctopusResult.Failure.InvalidArguments
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.UiText.DynamicString

val OctopusResult.Failure<*>.errorText: UiText
    get() =
        when (this) {
            is InvalidArguments<*> -> DynamicString(errorMessage)
            is OctopusResult.Failure.NoNetwork -> UiText.StringResource(R.string.error_no_network_formal)
            is OctopusResult.Failure.ContentUnavailable -> UiText.StringResource(R.string.error_content_unavailable)
            is OctopusResult.Failure.PermissionDenied ->
                reason?.let { DynamicString(it) }
                    ?: UiText.StringResource(R.string.error_permission_denied_formal)

            is OctopusResult.Failure.UserNotAuthenticated ->
                reason?.let { DynamicString(it) }
                    ?: UiText.StringResource(R.string.error_user_not_authenticated_formal)

            is OctopusResult.Failure.StatusError -> UiText.StringResource(R.string.error_unknown_formal)
        }

val OctopusResult<*, *>.errorText: UiText?
    get() =
        when (this) {
            is OctopusResult.Success -> null
            is OctopusResult.Failure -> this.errorText
        }

@Composable
fun OctopusResult.Failure<*>.asString() = errorText.asString()

fun OctopusResult.Failure<*>.asString(context: Context) = errorText.asString(context)
