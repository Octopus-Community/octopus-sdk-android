package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme

object OctopusIconButtonDefaults {
    /**
     * Colors for [OctopusIconButton].
     *
     * The tint is SDK-owned: the palette's `gray900`, which is also what `contentColorFor(background)`
     * resolves to on an SDK surface — never the host's ambient `LocalContentColor`. Same as every
     * other icon button in this file, and same as iOS, which tints each icon from `theme.colors`
     * (its `ResponseMoreMenuButton` pins `gray500`) rather than inheriting.
     *
     * Reading the ambient value instead bought nothing and cost legibility. Under an
     * [OctopusScaffold] it already resolved to this same `gray900`, one level below the host; above
     * the SDK's first surface it was the host's own foreground, unchecked against our background —
     * or Material's static `Color.Black`, since `MaterialTheme` alone never sets that composition
     * local, i.e. a near-black icon on a dark background.
     */
    @Composable
    fun iconButtonColors(
        contentColor: Color = OctopusTheme.colorScheme.gray900,
        disabledContentColor: Color = OctopusTheme.colorScheme.gray500,
    ) = IconButtonDefaults.iconButtonColors(
        contentColor = contentColor,
        disabledContentColor = disabledContentColor,
    )
}

@Composable
internal fun OctopusIconButton(
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    image: Painter? = null,
    emoji: String? = null,
    onClick: () -> Unit,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: IconButtonColors = OctopusIconButtonDefaults.iconButtonColors(),
) {
    IconButton(
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        onClick = onClick,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                painter = icon,
                contentDescription = contentDescription,
            )
        } else if (image != null) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = image,
                contentDescription = contentDescription,
            )
        } else if (emoji != null) {
            OctopusEmoji(
                modifier = Modifier.fillMaxSize(),
                unicode = emoji,
            )
        }
    }
}

@Composable
internal fun OctopusIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    colors: IconButtonColors = OctopusIconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit,
) {
    IconButton(
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        onClick = onClick,
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusIconButtonPreview() {
    OctopusTheme {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OctopusIconButton(
                modifier = Modifier.size(24.dp),
                icon = rememberVectorPainter(Icons.Default.MoreVert),
                onClick = {},
            )
            OctopusIconButton(
                modifier = Modifier.size(24.dp),
                icon = rememberVectorPainter(Icons.Default.MoreVert),
                enabled = false,
                onClick = {},
            )
        }
    }
}

@Composable
internal fun OctopusFilledIconButton(
    icon: Any,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color = OctopusTheme.colorScheme.primary,
    contentColor: Color = OctopusTheme.colorScheme.onPrimary,
    disabledContainerColor: Color = OctopusTheme.colorScheme.gray200,
    disabledContentColor: Color = OctopusTheme.colorScheme.gray500,
) {
    FilledIconButton(
        modifier = modifier,
        enabled = enabled && !loading,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor,
            ),
        onClick = onClick,
    ) {
        if (loading) {
            OctopusProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = disabledContentColor,
            )
        } else {
            when (icon) {
                is ImageVector ->
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                    )

                is Painter ->
                    Icon(
                        painter = icon,
                        contentDescription = contentDescription,
                    )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusFilledIconButtonPreview() {
    OctopusTheme {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Normal state
            OctopusFilledIconButton(
                modifier = Modifier.size(40.dp),
                icon =
                    OctopusTheme.icons.content.post.creation
                        .create(),
                contentDescription = "Normal",
                onClick = {},
            )

            // Loading state
            OctopusFilledIconButton(
                modifier = Modifier.size(40.dp),
                icon =
                    OctopusTheme.icons.content.post.creation
                        .create(),
                contentDescription = "Loading state",
                loading = true,
                onClick = {},
            )

            // Disabled state
            OctopusFilledIconButton(
                modifier = Modifier.size(40.dp),
                icon =
                    OctopusTheme.icons.content.post.creation
                        .create(),
                contentDescription = "Disabled state",
                enabled = false,
                onClick = {},
            )
        }
    }
}

@Composable
internal fun OctopusFilledIconButtonSecondary(
    icon: Any,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) = OctopusFilledIconButton(
    modifier = modifier,
    icon = icon,
    contentDescription = contentDescription,
    onClick = onClick,
    enabled = enabled,
    loading = loading,
    containerColor = OctopusTheme.colorScheme.gray300,
    contentColor = OctopusTheme.colorScheme.gray900,
)

@Preview(showBackground = true)
@Composable
private fun OctopusFilledIconButtonSecondaryPreview() {
    OctopusTheme {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Normal state
            OctopusFilledIconButtonSecondary(
                modifier = Modifier.size(40.dp),
                icon =
                    OctopusTheme.icons.content.post.creation
                        .create(),
                contentDescription = "Normal",
                onClick = {},
            )

            // Loading state
            OctopusFilledIconButtonSecondary(
                modifier = Modifier.size(40.dp),
                icon =
                    OctopusTheme.icons.content.post.creation
                        .create(),
                contentDescription = "Loading state",
                loading = true,
                onClick = {},
            )

            // Disabled state
            OctopusFilledIconButtonSecondary(
                modifier = Modifier.size(40.dp),
                icon =
                    OctopusTheme.icons.content.post.creation
                        .create(),
                contentDescription = "Disabled state",
                enabled = false,
                onClick = {},
            )
        }
    }
}

@Composable
internal fun OctopusOutlinedIconButton(
    icon: Any?,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentColor: Color = OctopusTheme.colorScheme.gray900,
    disabledContentColor: Color = OctopusTheme.colorScheme.gray500,
    borderColor: Color = OctopusTheme.colorScheme.gray300,
    shape: Shape = IconButtonDefaults.outlinedShape,
    loadingIndicatorSize: Dp = 16.dp,
    loadingIndicatorStrokeWidth: Dp = 2.dp,
) = OutlinedIconButton(
    modifier = modifier,
    enabled = enabled && !loading,
    colors =
        IconButtonDefaults.outlinedIconButtonColors(
            contentColor = contentColor,
            disabledContentColor = disabledContentColor,
        ),
    border =
        BorderStroke(
            width = 1.dp,
            color = borderColor,
        ),
    shape = shape,
    onClick = onClick,
) {
    if (loading) {
        OctopusProgressIndicator(
            modifier = Modifier.size(loadingIndicatorSize),
            strokeWidth = loadingIndicatorStrokeWidth,
            color = disabledContentColor,
        )
    } else {
        when (icon) {
            is ImageVector ->
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                )

            is Painter ->
                Icon(
                    painter = icon,
                    contentDescription = contentDescription,
                )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusOutlinedIconButtonPreview() {
    OctopusTheme {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Normal state
            OctopusOutlinedIconButton(
                modifier = Modifier.size(40.dp),
                icon =
                    OctopusTheme.icons.content.post.creation
                        .create(),
                contentDescription = "Normal",
                onClick = {},
            )

            // Loading state
            OctopusOutlinedIconButton(
                modifier = Modifier.size(40.dp),
                icon =
                    OctopusTheme.icons.content.post.creation
                        .create(),
                contentDescription = "Loading state",
                loading = true,
                onClick = {},
            )

            // Disabled state
            OctopusOutlinedIconButton(
                modifier = Modifier.size(40.dp),
                icon =
                    OctopusTheme.icons.content.post.creation
                        .create(),
                contentDescription = "Disabled state",
                enabled = false,
                onClick = {},
            )
        }
    }
}
