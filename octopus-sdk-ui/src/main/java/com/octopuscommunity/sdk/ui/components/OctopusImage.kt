package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.octopuscommunity.sdk.data.utils.ImageRequestBuilder
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.test.mock.MockMedias
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import org.koin.compose.koinInject
import kotlin.math.max

object OctopusImageDefaults {
    const val ITEM_MAX_ASPECT_RATIO = 4f / 5f
}

@Composable
internal fun OctopusImage(
    media: Media.Image,
    modifier: Modifier = Modifier,
    maxAspectRatio: Float? = null,
    contentDescription: String? = media.name,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Painter = ColorPainter(OctopusTheme.colorScheme.gray200),
) = OctopusImage(
    modifier =
        modifier.then(
            when (val dimensions = media.dimensions) {
                null -> Modifier
                else -> {
                    val aspectRatio = dimensions.width.toFloat() / dimensions.height.toFloat()
                    Modifier.aspectRatio(max(aspectRatio, maxAspectRatio ?: 0f))
                }
            },
        ),
    data = media.source.data,
    contentDescription = contentDescription,
    contentScale = contentScale,
    placeholder = placeholder,
)

@Composable
fun OctopusImage(
    data: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: Painter? = null,
    crossFade: Boolean = true,
) = OctopusContent {
    if (LocalInspectionMode.current) {
        Image(
            modifier =
                modifier
                    .background(Color.LightGray),
            painter = painterResource(R.drawable.ic_octopus_content_not_available),
            contentDescription = contentDescription,
            contentScale = contentScale,
        )
    } else {
        val imageLoader = koinInject<ImageLoader>()
        val imageRequestBuilder = koinInject<ImageRequestBuilder>()

        AsyncImage(
            modifier = modifier,
            model =
                imageRequestBuilder
                    .create(
                        data = data,
                        crossFade = crossFade,
                    ).build(),
            imageLoader = imageLoader,
            placeholder = placeholder,
            contentDescription = contentDescription,
            contentScale = contentScale,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusImagePreview() {
    OctopusTheme {
        OctopusImage(media = MockMedias.default)
    }
}
