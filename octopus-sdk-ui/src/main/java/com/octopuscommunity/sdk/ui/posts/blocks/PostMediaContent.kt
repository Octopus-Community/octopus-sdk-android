package com.octopuscommunity.sdk.ui.posts.blocks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.ui.common.medias.MediaPinchState
import com.octopuscommunity.sdk.ui.common.medias.mediaPinch
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerSource
import com.octopuscommunity.sdk.ui.common.medias.viewer.imageViewerSource
import com.octopuscommunity.sdk.ui.common.medias.viewer.rememberImageViewerSource
import com.octopuscommunity.sdk.ui.components.OctopusImage
import com.octopuscommunity.sdk.ui.components.OctopusImageDefaults
import com.octopuscommunity.sdk.ui.components.video.OctopusVideo

/**
 * Image attachments — edge-to-edge (no horizontal padding) so the images reach the card's horizontal
 * bounds. One image shows as is; several are swiped in a [PostImagesCarousel]. Tapping image `i`
 * opens the full-screen media viewer on it, flying the image from its thumbnail
 * ([ImageViewerSource]). The gap above the images grows from 4dp to 8dp when the post has no text.
 */
@Composable
internal fun PostMediaContent(
    images: List<Media.Image>,
    postId: String,
    hasText: Boolean,
    onClick: (index: Int, source: ImageViewerSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val topPadding = if (hasText) 4.dp else 8.dp
    if (images.size >= 2) {
        PostImagesCarousel(
            postId = postId,
            images = images,
            onImageClick = onClick,
            modifier = modifier.padding(top = topPadding),
        )
        return
    }
    val source = rememberImageViewerSource()
    val pinchState = remember(postId) { MediaPinchState() }
    OctopusImage(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .imageViewerSource(source)
                .mediaPinch(pinchState)
                .clickable(onClick = { onClick(0, source) }),
        media = images.first(),
        maxAspectRatio = OctopusImageDefaults.ITEM_MAX_ASPECT_RATIO,
    )
}

/**
 * Video attachment — edge-to-edge. Auto-plays when the surrounding scroller marks the post
 * as visible (`autoPlay = true`).
 */
@Composable
internal fun PostMediaContent(
    video: Media.Video,
    postId: String,
    autoPlay: Boolean,
    modifier: Modifier = Modifier,
) {
    OctopusVideo(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        media = video,
        contentId = postId,
        autoPlay = autoPlay,
    )
}
