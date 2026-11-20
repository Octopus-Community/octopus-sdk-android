package com.octopuscommunity.sdk.ui.comments.replies.medias

import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.repository.ReplyRepository
import com.octopuscommunity.sdk.ui.common.medias.MediasViewModel
import kotlinx.coroutines.launch

class ReplyMediasViewModel(
    commonParameters: CommonParameters,
    private val replyRepository: ReplyRepository,
    private val replyId: String
) : MediasViewModel(commonParameters) {

    init {
        viewModelScope.launch {
            replyRepository.getLocalItemFlow(itemId = replyId).collect { reply ->
                setMedias(reply?.medias)
            }
        }
    }
}