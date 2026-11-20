package com.octopuscommunity.sdk.ui.posts.menu

import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.usecase.DeletePostUseCase
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemMenuSheetViewModel

class PostMenuSheetViewModel internal constructor(
    commonParameters: CommonParameters,
    deletePostUseCase: DeletePostUseCase,
    post: Post,
) : OctopusItemMenuSheetViewModel<Post>(
    commonParameters = commonParameters,
    item = post,
    deleteItem = deletePostUseCase::invoke
)