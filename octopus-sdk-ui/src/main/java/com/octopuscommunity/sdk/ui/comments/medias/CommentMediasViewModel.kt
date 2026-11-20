package com.octopuscommunity.sdk.ui.comments.medias

import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.repository.CommentRepository
import com.octopuscommunity.sdk.ui.common.medias.MediasViewModel
import kotlinx.coroutines.launch

class CommentMediasViewModel(
    commonParameters: CommonParameters,
    private val commentRepository: CommentRepository,
    private val commentId: String
) : MediasViewModel(commonParameters) {

    init {
        viewModelScope.launch {
            commentRepository.getLocalItemFlow(itemId = commentId).collect { comment ->
                setMedias(comment?.medias)
            }
        }
    }
}