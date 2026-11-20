package com.octopuscommunity.sdk.ui.posts.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.components.OctopusImage

private val ThumbnailSide = 220.dp
private val ThumbnailSpacing = 8.dp
private val ThumbnailCornerRadius = 8.dp
private val OverlayInset = 8.dp
private val DeleteIconPadding = 6.dp
private val OverlayIconSize = 16.dp
private val AddMoreHorizontalPadding = 12.dp
private val AddMoreVerticalPadding = 7.dp
private val AddMoreSpacing = 6.dp
private val MinTapTarget = 48.dp

/**
 * The background of the cross and of the « Add » tile over a thumbnail: the light gray900 (internal change)
 * at 75 %, with white content. Fixed on purpose: the theme's gray900 turns light in dark mode,
 * where it would wash out over the photo.
 */
private val OverlayBackground = Color(0xFF141414).copy(alpha = 0.75f)
private val OverlayForeground = Color.White

/**
 * The images of the post being written: square thumbnails in a horizontal strip, each with a cross
 * that removes it, the last one carrying the « Add » tile while [showsAddMoreTile]. The strip runs
 * edge to edge on its trailing side, past [startPadding] on its leading one. It scrolls to its last
 * thumbnail when images are added, never on its first fill (the prefill) nor on a removal, and to
 * the photo of [faultyImage] each time the server refuses one. [listState] holds its scroll position.
 */
@Composable
internal fun CreatePostImagesStrip(
    images: List<Media.Image>,
    showsAddMoreTile: Boolean,
    enabled: Boolean,
    onDelete: (index: Int) -> Unit,
    onAddMore: () -> Unit,
    modifier: Modifier = Modifier,
    faultyImage: FaultyImage? = null,
    startPadding: Dp = 0.dp,
    listState: LazyListState = rememberLazyListState(),
) {
    // Seeded with the images the strip is created with, so the first fill does not scroll.
    var displayedCount by remember { mutableIntStateOf(images.size) }
    LaunchedEffect(images.size) {
        val scroll = shouldScrollToLast(previousCount = displayedCount, newCount = images.size)
        displayedCount = images.size
        if (scroll) listState.animateScrollToItem(images.lastIndex)
    }
    LaunchedEffect(faultyImage) {
        faultyImageScrollTarget(faultyImage, imageCount = images.size)?.let { listState.animateScrollToItem(it) }
    }
    LazyRow(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(start = startPadding),
        horizontalArrangement = Arrangement.spacedBy(ThumbnailSpacing),
    ) {
        itemsIndexed(images) { index, image ->
            Thumbnail(
                image = image,
                showsAddMoreTile = showsAddMoreTile && index == images.lastIndex,
                enabled = enabled,
                onDelete = { onDelete(index) },
                onAddMore = onAddMore,
            )
        }
    }
}

@Composable
private fun Thumbnail(
    image: Media.Image,
    showsAddMoreTile: Boolean,
    enabled: Boolean,
    onDelete: () -> Unit,
    onAddMore: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(ThumbnailSide)
                .clip(RoundedCornerShape(ThumbnailCornerRadius)),
    ) {
        OctopusImage(
            data = image.source.data,
            modifier = Modifier.size(ThumbnailSide),
            contentDescription = image.name.ifEmpty { null },
            contentScale = ContentScale.Crop,
            placeholder = ColorPainter(OctopusTheme.colorScheme.gray200),
        )
        // Keeps a 48dp tap target around the small visible circle.
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .sizeIn(minWidth = MinTapTarget, minHeight = MinTapTarget)
                    .clickable(enabled = enabled, role = Role.Button, onClick = onDelete),
            contentAlignment = Alignment.TopEnd,
        ) {
            Icon(
                modifier =
                    Modifier
                        .padding(OverlayInset)
                        .background(color = OverlayBackground, shape = CircleShape)
                        .padding(DeleteIconPadding)
                        .size(OverlayIconSize),
                painter =
                    OctopusTheme.icons.content.post.creation
                        .deletePicture(),
                contentDescription = stringResource(R.string.accessibility_image_delete),
                tint = OverlayForeground,
            )
        }
        if (showsAddMoreTile) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .sizeIn(minHeight = MinTapTarget)
                        .clickable(enabled = enabled, role = Role.Button, onClick = onAddMore),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Row(
                    modifier =
                        Modifier
                            .padding(OverlayInset)
                            .background(color = OverlayBackground, shape = CircleShape)
                            .padding(horizontal = AddMoreHorizontalPadding, vertical = AddMoreVerticalPadding),
                    horizontalArrangement = Arrangement.spacedBy(AddMoreSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.size(OverlayIconSize),
                        painter =
                            OctopusTheme.icons.content.post.creation
                                .addPicture(),
                        contentDescription = null,
                        tint = OverlayForeground,
                    )
                    Text(
                        text = stringResource(R.string.content_create_add_more_pictures),
                        color = OverlayForeground,
                        style = OctopusTheme.typography.caption1,
                    )
                }
            }
        }
    }
}

/**
 * Whether the composer's strip scrolls to its last thumbnail when its image count goes from
 * [previousCount] to [newCount]: only when photos were added, so that removing one does not move it.
 */
internal fun shouldScrollToLast(
    previousCount: Int,
    newCount: Int,
): Boolean = newCount > previousCount

/**
 * The thumbnail the strip scrolls to for [faultyImage], the photo the server refused: `null` when
 * there is none, or when the strip no longer holds it.
 */
internal fun faultyImageScrollTarget(
    faultyImage: FaultyImage?,
    imageCount: Int,
): Int? = faultyImage?.index?.takeIf { it in 0 until imageCount }
