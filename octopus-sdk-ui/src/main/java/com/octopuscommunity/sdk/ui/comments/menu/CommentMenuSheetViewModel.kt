package com.octopuscommunity.sdk.ui.comments.menu

import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.usecase.DeleteCommentUseCase
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemMenuSheetViewModel

class CommentMenuSheetViewModel internal constructor(
    commonParameters: CommonParameters,
    deleteCommentUseCase: DeleteCommentUseCase,
    comment: Comment
) : OctopusItemMenuSheetViewModel<Comment>(
    commonParameters = commonParameters,
    item = comment,
    deleteItem = deleteCommentUseCase::invoke
)