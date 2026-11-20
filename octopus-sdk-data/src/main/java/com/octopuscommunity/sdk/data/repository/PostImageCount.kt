package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.sdk.domain.model.ClientPost
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.repository.ClientPostError
import com.octopuscommunity.sdk.domain.repository.PostCreationError

/**
 * The error a client post with more images than a post carries is refused with, on the file field,
 * or `null` when its image count is accepted. Checked before any network call; the backend refuses
 * more than [Post.Draft.MAX_IMAGE_COUNT] images too.
 */
internal fun clientPostImageCountError(clientPost: ClientPost): ClientPostError.FileError.Unknown? =
    tooManyImagesMessage(clientPost.images.size)?.let { ClientPostError.FileError.Unknown(errorMessage = it) }

/** The same refusal for a member's draft, on its file field, or `null` when its image count is accepted. */
internal fun draftImageCountError(draft: Post.Draft): PostCreationError.FileError.Unknown? =
    tooManyImagesMessage(draft.images.size)?.let { PostCreationError.FileError.Unknown(errorMessage = it) }

private fun tooManyImagesMessage(count: Int): String? =
    if (count <= Post.Draft.MAX_IMAGE_COUNT) {
        null
    } else {
        "A post accepts at most ${Post.Draft.MAX_IMAGE_COUNT} images, $count were provided."
    }
