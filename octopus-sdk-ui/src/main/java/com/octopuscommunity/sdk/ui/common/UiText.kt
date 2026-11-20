package com.octopuscommunity.sdk.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.ui.common.UiText.DynamicString

sealed class UiText {
    data class StringResource(
        @StringRes val resId: Int,
        val formatArgs: List<Any> = listOf(),
    ) : UiText()

    data class JoinedResource(
        val values: List<UiText>,
        val separator: CharSequence = ", ",
    ) : UiText()

    data class DynamicString(
        val value: String,
    ) : UiText()

    @Suppress("SimplifiableCallChain")
    @Composable
    fun asString(): String =
        when (this) {
            is StringResource -> {
                stringResource(
                    resId,
                    *formatArgs
                        .map {
                            when (it) {
                                is UiText -> it.asString()
                                else -> it
                            }
                        }.toTypedArray(),
                )
            }

            is JoinedResource -> values.map { it.asString() }.joinToString(separator)
            is DynamicString -> value
        }

    /**
     * For non-composable usage. [communityConfig] picks the informal variant of a text when the
     * community uses the informal tone in the language [context] resolves strings in (internal ticket);
     * `null` keeps the formal one.
     */
    @JvmOverloads
    fun asString(
        context: Context,
        communityConfig: CommunityConfig? = null,
    ): String =
        when (this) {
            is StringResource ->
                context.getString(
                    toneStringId(resId, communityConfig, context.resources.configuration),
                    *formatArgs
                        .map {
                            when (it) {
                                is UiText -> it.asString(context, communityConfig)
                                else -> it
                            }
                        }.toTypedArray(),
                )

            is JoinedResource -> values.joinToString(separator) { it.asString(context, communityConfig) }

            is DynamicString -> value
        }
}

data class UiTextLength(
    val content: String?,
    val maxLength: Int,
) {
    val length = content?.length ?: 0
    val text = DynamicString("($length/$maxLength)")
    val isValid = length <= maxLength
}
