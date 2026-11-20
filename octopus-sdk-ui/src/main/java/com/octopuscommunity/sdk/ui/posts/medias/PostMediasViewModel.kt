package com.octopuscommunity.sdk.ui.posts.medias

import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.repository.PostRepository
import com.octopuscommunity.sdk.ui.common.medias.MediasViewModel
import kotlinx.coroutines.launch

class PostMediasViewModel(
    commonParameters: CommonParameters,
    private val postRepository: PostRepository,
    private val postId: String
) : MediasViewModel(commonParameters) {

    init {
        viewModelScope.launch {
            postRepository.getLocalItemFlow(itemId = postId).collect { post ->
                setMedias(post?.medias)
            }
        }
    }
}