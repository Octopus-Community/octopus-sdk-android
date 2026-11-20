package com.octopuscommunity.sdk.ui.comments.replies.create

import android.content.Context
import android.net.Uri
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.Comment.Reply
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.domain.usecase.SendReplyUseCase
import com.octopuscommunity.sdk.ui.comments.components.OctopusItemInputFieldViewModel
import com.octopuscommunity.sdk.ui.common.validation.ValidateImage
import com.octopuscommunity.sdk.ui.common.validation.ValidateText
import com.octopuscommunity.sdk.ui.profile.current.nickname.NicknameChoiceSession
import kotlinx.coroutines.flow.update

class ReplyInputFieldViewModel internal constructor(
    commonParameters: CommonParameters,
    sendReplyUseCase: SendReplyUseCase,
    private val validateText: ValidateText,
    private val validateImage: ValidateImage,
    nicknameChoiceSession: NicknameChoiceSession,
    comment: Comment,
) : OctopusItemInputFieldViewModel<Reply, Reply.Draft>(
        commonParameters = commonParameters,
        userAction = UserAction.Reply,
        draftItem = Reply.Draft(comment = comment),
        sendItem = sendReplyUseCase::invoke,
        nicknameChoiceSession = nicknameChoiceSession,
    ) {
    override fun updateText(text: String) {
        _state.update { state ->
            state.copy(
                item =
                    state.item.copy(
                        text = text,
                    ),
                textError =
                    validateText(
                        text = text,
                        lengthRange = Reply.Draft.TEXT_LENGTH_RANGE,
                    ),
            )
        }
    }

    override fun updateImage(
        context: Context,
        localUri: Uri?,
    ) {
        _state.update { state ->
            val media =
                localUri?.let { uri ->
                    Media.Image(context = context, localUri = uri)
                }
            state.copy(
                item = state.item.copy(media = media),
                mediaError = media?.dimensions?.let { validateImage(it) },
            )
        }
    }
}
