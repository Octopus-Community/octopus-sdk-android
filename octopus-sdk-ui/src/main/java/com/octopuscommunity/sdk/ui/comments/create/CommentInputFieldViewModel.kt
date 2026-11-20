package com.octopuscommunity.sdk.ui.comments.create

import android.content.Context
import android.net.Uri
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.usecase.SendCommentUseCase
import com.octopuscommunity.sdk.ui.comments.components.OctopusItemInputFieldViewModel
import com.octopuscommunity.sdk.ui.common.validation.ValidateImage
import com.octopuscommunity.sdk.ui.common.validation.ValidateText
import com.octopuscommunity.sdk.ui.profile.current.nickname.NicknameChoiceSession
import kotlinx.coroutines.flow.update

class CommentInputFieldViewModel internal constructor(
    commonParameters: CommonParameters,
    sendCommentUseCase: SendCommentUseCase,
    private val validateText: ValidateText,
    private val validateImage: ValidateImage,
    nicknameChoiceSession: NicknameChoiceSession,
    post: Post,
) : OctopusItemInputFieldViewModel<Comment, Comment.Draft>(
        commonParameters = commonParameters,
        userAction = UserAction.Comment,
        draftItem = Comment.Draft(post = post),
        sendItem = sendCommentUseCase::invoke,
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
                        lengthRange = Comment.Draft.TEXT_LENGTH_RANGE,
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
