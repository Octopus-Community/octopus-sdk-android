package com.octopuscommunity.sdk.ui.responses

import androidx.annotation.StringRes
import com.octopuscommunity.sdk.ui.R

/**
 * Kind of response rendered by the shared `ResponseView` composable.
 *
 * Equivalent to iOS `ResponseKind`.
 */
enum class ResponseKind {
    Comment,
    Reply,
    ;

    /** Whether this kind of response can itself be replied to. Comments yes, replies no. */
    val canReply: Boolean
        get() =
            when (this) {
                Comment -> true
                Reply -> false
            }

    /** Localized string resource for the "Delete" menu entry. */
    @get:StringRes
    val deleteMenuText: Int
        get() =
            when (this) {
                Comment -> R.string.comment_delete_button
                Reply -> R.string.reply_delete_button
            }

    /** Localized string resource for the delete-confirmation alert title. */
    @get:StringRes
    val deleteConfirmationTitle: Int
        get() =
            when (this) {
                Comment -> R.string.comment_delete_confirmation_title_formal
                Reply -> R.string.reply_delete_confirmation_title_formal
            }
}
