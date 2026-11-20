package com.octopuscommunity.sdk.ui.responses.blocks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.ui.common.medias.MediaPinchState
import com.octopuscommunity.sdk.ui.common.medias.mediaPinch
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerSource
import com.octopuscommunity.sdk.ui.common.medias.viewer.imageViewerSource
import com.octopuscommunity.sdk.ui.common.medias.viewer.rememberImageViewerSource
import com.octopuscommunity.sdk.ui.components.OctopusImage
import com.octopuscommunity.sdk.ui.components.OctopusImageDefaults

/**
 * Image attachment inside a response card — flush with the card edges horizontally, small 2.dp
 * top strip separating it from the text block above. Iso iOS `ResponseImageContentView`.
 */
@Composable
internal fun ResponseImageContent(
    image: Media.Image,
    onClick: (ImageViewerSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = rememberImageViewerSource()
    val pinchState = remember(image) { MediaPinchState() }
    OctopusImage(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .imageViewerSource(source)
                .mediaPinch(pinchState)
                .clickable(role = Role.Image, onClick = { onClick(source) }),
        media = image,
        maxAspectRatio = OctopusImageDefaults.ITEM_MAX_ASPECT_RATIO,
    )
}
