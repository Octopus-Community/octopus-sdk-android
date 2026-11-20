package com.octopuscommunity.sdk.ui.profile.current.nickname

import androidx.annotation.StringRes
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel.UserAction

/** The contribution a nickname choice holds back; the screen's button names it. */
internal enum class NicknameChoiceContribution(
    @StringRes val buttonRes: Int,
) {
    POST(R.string.post_create_button),
    COMMENT(R.string.content_aggregated_info_comment),
    REPLY(R.string.content_aggregated_info_reply),
    ;

    companion object {
        /** The contribution for a nickname-validating [action]; anything else is labelled as a post. */
        fun of(action: UserAction): NicknameChoiceContribution =
            when (action) {
                UserAction.Comment -> COMMENT
                UserAction.Reply -> REPLY
                else -> POST
            }
    }
}
