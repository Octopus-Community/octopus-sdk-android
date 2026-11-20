package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource

@Composable
internal fun OctopusMediaField(
    media: Media?,
    onDeleteImage: () -> Unit,
    modifier: Modifier = Modifier,
    deleteIcon: Painter = OctopusTheme.icons.content.delete(),
    enabled: Boolean = true,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Box(modifier = modifier) {
        OctopusImage(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(8.dp)),
            data = media?.source?.data,
            contentDescription = null,
            contentScale = contentScale,
        )
        OctopusFilledIconButtonSecondary(
            modifier =
                Modifier
                    .padding(4.dp)
                    .size(40.dp)
                    .align(Alignment.TopEnd),
            icon = deleteIcon,
            contentDescription = stringResource(R.string.accessibility_image_delete),
            enabled = enabled,
            onClick = onDeleteImage,
        )
    }
}
