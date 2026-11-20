package com.octopuscommunity.sdk.ui.comments.replies.menu

import com.octopuscommunity.sdk.domain.model.Comment.Reply
import com.octopuscommunity.sdk.domain.usecase.DeleteReplyUseCase
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemMenuSheetViewModel

class ReplyMenuSheetViewModel internal constructor(
    commonParameters: CommonParameters,
    deleteReplyUseCase: DeleteReplyUseCase,
    reply: Reply
) : OctopusItemMenuSheetViewModel<Reply>(
    commonParameters = commonParameters,
    item = reply,
    deleteItem = deleteReplyUseCase::invoke
)