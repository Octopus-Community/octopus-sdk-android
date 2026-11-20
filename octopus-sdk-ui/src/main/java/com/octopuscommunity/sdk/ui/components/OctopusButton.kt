package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.medium

object OctopusButtonDefaults {
    @Composable
    fun buttonColors(
        containerColor: Color = OctopusTheme.colorScheme.primary,
        contentColor: Color = OctopusTheme.colorScheme.onPrimary,
        disabledContainerColor: Color = OctopusTheme.colorScheme.gray200,
        disabledContentColor: Color = OctopusTheme.colorScheme.gray500,
    ) = ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

    @Composable
    fun outlineButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = OctopusTheme.colorScheme.gray900,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = OctopusTheme.colorScheme.gray500,
    ) = ButtonDefaults.outlinedButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

    @Composable
    fun outlineButtonBorder(color: Color = OctopusTheme.colorScheme.gray300) =
        BorderStroke(
            width = 1.dp,
            color = color,
        )

    val shape = RoundedCornerShape(24.dp)
    val mediumShape = RoundedCornerShape(90.dp)
    val smallShape = RoundedCornerShape(90.dp)

    val heightIn: Dp = 48.dp
    val mediumHeightIn: Dp = 32.dp
    val smallHeightIn: Dp = 32.dp

    fun contentPadding(bigContent: Boolean) =
        PaddingValues(
            horizontal = 32.dp,
            vertical = if (bigContent) 12.dp else 14.dp,
        )

    fun mediumContentPadding(bigContent: Boolean) =
        if (bigContent) {
            PaddingValues(
                start = 4.dp,
                top = 4.dp,
                end = 12.dp,
                bottom = 4.dp,
            )
        } else {
            PaddingValues(
                horizontal = 12.dp,
                vertical = 6.dp,
            )
        }

    fun smallContentPadding(bigContent: Boolean) =
        PaddingValues(
            horizontal = 12.dp,
            vertical = if (bigContent) 4.dp else 6.dp,
        )

    val horizontalSpace: Dp = 8.dp
    val mediumHorizontalSpace: Dp = 2.dp
    val smallHorizontalSpace: Dp = 2.dp

    val textStyle: TextStyle @Composable get() = OctopusTheme.typography.body2.medium
    val mediumTextStyle: TextStyle @Composable get() = OctopusTheme.typography.body2.medium
    val smallTextStyle: TextStyle @Composable get() = OctopusTheme.typography.caption1.medium

    const val maxLines = Int.MAX_VALUE
}

@Composable
private fun OctopusButtonContent(
    text: String?,
    icon: Any?,
    image: Painter?,
    emoji: String?,
    loading: Boolean,
    horizontalSpace: Dp,
    textStyle: TextStyle,
    maxLines: Int,
) {
    Row(
        horizontalArrangement =
            Arrangement.spacedBy(
                space = horizontalSpace,
                alignment = Alignment.CenterHorizontally,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            OctopusProgressIndicator(
                modifier =
                    Modifier
                        .size(16.dp)
                        .padding(8.dp),
                strokeWidth = 2.dp,
                color = OctopusTheme.colorScheme.primary,
            )
        } else if (icon != null) {
            when (icon) {
                is ImageVector ->
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = icon,
                        contentDescription = text,
                    )

                is Painter ->
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = icon,
                        contentDescription = text,
                    )
            }
        } else if (image != null) {
            Image(
                modifier = Modifier.size(24.dp),
                painter = image,
                contentDescription = text,
            )
        } else if (emoji != null) {
            OctopusEmoji(
                modifier = Modifier.size(24.dp),
                unicode = emoji,
            )
        }
        if (text != null) {
            Text(
                text = text,
                textAlign = TextAlign.Center,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                style = textStyle,
            )
        }
    }
}

@Composable
internal fun OctopusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Any? = null,
    image: Painter? = null,
    emoji: String? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    shape: Shape = OctopusButtonDefaults.shape,
    colors: ButtonColors = OctopusButtonDefaults.buttonColors(),
    heightIn: Dp = OctopusButtonDefaults.heightIn,
    contentPadding: PaddingValues = OctopusButtonDefaults.contentPadding(bigContent = icon != null || loading),
    horizontalSpace: Dp = OctopusButtonDefaults.horizontalSpace,
    textStyle: TextStyle = OctopusButtonDefaults.textStyle,
    maxLines: Int = OctopusButtonDefaults.maxLines,
) {
    Button(
        modifier =
            modifier
                .widthIn(1.dp)
                .heightIn(heightIn),
        onClick = onClick,
        enabled = enabled && !loading,
        colors = colors,
        contentPadding = contentPadding,
        shape = shape,
    ) {
        OctopusButtonContent(
            text = text,
            icon = icon,
            image = image,
            emoji = emoji,
            loading = loading,
            horizontalSpace = horizontalSpace,
            textStyle = textStyle,
            maxLines = maxLines,
        )
    }
}

@Composable
internal fun OctopusOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: Any? = null,
    image: Painter? = null,
    emoji: String? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    shape: Shape = OctopusButtonDefaults.shape,
    colors: ButtonColors = OctopusButtonDefaults.outlineButtonColors(),
    border: BorderStroke? = OctopusButtonDefaults.outlineButtonBorder(),
    heightIn: Dp = OctopusButtonDefaults.heightIn,
    contentPadding: PaddingValues =
        OctopusButtonDefaults.contentPadding(
            bigContent = icon != null || loading,
        ),
    horizontalSpace: Dp = OctopusButtonDefaults.horizontalSpace,
    textStyle: TextStyle = OctopusButtonDefaults.textStyle,
    maxLines: Int = OctopusButtonDefaults.maxLines,
    contentDescription: String? = null,
) {
    OutlinedButton(
        modifier =
            modifier
                .widthIn(min = 1.dp)
                .heightIn(heightIn)
                .then(
                    if (contentDescription != null) {
                        Modifier.semantics { this.contentDescription = contentDescription }
                    } else {
                        Modifier
                    },
                ),
        onClick = onClick,
        enabled = enabled && !loading,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        shape = shape,
    ) {
        OctopusButtonContent(
            text = text,
            icon = icon,
            image = image,
            emoji = emoji,
            loading = loading,
            horizontalSpace = horizontalSpace,
            textStyle = textStyle,
            maxLines = maxLines,
        )
    }
}

@Composable
internal fun OctopusMediumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Any? = null,
    image: Painter? = null,
    emoji: String? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    colors: ButtonColors = OctopusButtonDefaults.buttonColors(),
    maxLines: Int = OctopusButtonDefaults.maxLines,
) = OctopusButton(
    modifier = modifier,
    text = text,
    icon = icon,
    image = image,
    emoji = emoji,
    onClick = onClick,
    enabled = enabled,
    loading = loading,
    shape = OctopusButtonDefaults.mediumShape,
    colors = colors,
    heightIn = OctopusButtonDefaults.mediumHeightIn,
    contentPadding = OctopusButtonDefaults.mediumContentPadding(bigContent = icon != null || loading),
    horizontalSpace = OctopusButtonDefaults.mediumHorizontalSpace,
    textStyle = OctopusButtonDefaults.mediumTextStyle,
    maxLines = maxLines,
)

@Composable
internal fun OctopusMediumOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Any? = null,
    image: Painter? = null,
    emoji: String? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    colors: ButtonColors = OctopusButtonDefaults.outlineButtonColors(),
    border: BorderStroke? = OctopusButtonDefaults.outlineButtonBorder(),
    maxLines: Int = OctopusButtonDefaults.maxLines,
) = OctopusOutlinedButton(
    modifier = modifier,
    text = text,
    icon = icon,
    image = image,
    emoji = emoji,
    onClick = onClick,
    enabled = enabled,
    loading = loading,
    shape = OctopusButtonDefaults.mediumShape,
    colors = colors,
    border = border,
    heightIn = OctopusButtonDefaults.mediumHeightIn,
    contentPadding = OctopusButtonDefaults.mediumContentPadding(bigContent = icon != null || loading),
    horizontalSpace = OctopusButtonDefaults.mediumHorizontalSpace,
    textStyle = OctopusButtonDefaults.mediumTextStyle,
    maxLines = maxLines,
)

@Composable
internal fun OctopusSmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Any? = null,
    image: Painter? = null,
    emoji: String? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    colors: ButtonColors = OctopusButtonDefaults.buttonColors(),
    maxLines: Int = OctopusButtonDefaults.maxLines,
) = OctopusButton(
    modifier = modifier,
    text = text,
    icon = icon,
    image = image,
    emoji = emoji,
    onClick = onClick,
    enabled = enabled,
    loading = loading,
    shape = OctopusButtonDefaults.smallShape,
    colors = colors,
    heightIn = OctopusButtonDefaults.smallHeightIn,
    contentPadding = OctopusButtonDefaults.smallContentPadding(bigContent = icon != null || loading),
    horizontalSpace = OctopusButtonDefaults.smallHorizontalSpace,
    textStyle = OctopusButtonDefaults.smallTextStyle,
    maxLines = maxLines,
)

@Composable
internal fun OctopusSmallOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: Any? = null,
    image: Painter? = null,
    emoji: String? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    colors: ButtonColors = OctopusButtonDefaults.outlineButtonColors(),
    border: BorderStroke? = OctopusButtonDefaults.outlineButtonBorder(),
    contentPadding: PaddingValues =
        OctopusButtonDefaults.smallContentPadding(
            bigContent = icon != null || loading,
        ),
    textStyle: TextStyle = OctopusButtonDefaults.smallTextStyle,
    maxLines: Int = OctopusButtonDefaults.maxLines,
    contentDescription: String? = null,
) = OctopusOutlinedButton(
    modifier = modifier,
    text = text,
    icon = icon,
    image = image,
    emoji = emoji,
    onClick = onClick,
    enabled = enabled,
    loading = loading,
    shape = OctopusButtonDefaults.smallShape,
    colors = colors,
    border = border,
    heightIn = OctopusButtonDefaults.smallHeightIn,
    contentPadding = contentPadding,
    horizontalSpace = OctopusButtonDefaults.smallHorizontalSpace,
    textStyle = textStyle,
    maxLines = maxLines,
    contentDescription = contentDescription,
)

@Preview(
    showBackground = false,
    widthDp = 1000,
)
@Composable
private fun OctopusButtonPreview() {
    OctopusTheme {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Normal state
            OctopusButton(
                text = "Normal",
                onClick = {},
            )

            // Icon
            OctopusButton(
                text = "Icon",
                icon =
                    OctopusTheme.icons.content.post.creation
                        .create(),
                onClick = {},
            )

            // Loading state
            OctopusButton(
                modifier = Modifier,
                text = "Loading state",
                loading = true,
                onClick = {},
            )

            // Disabled state
            OctopusButton(
                text = "Disabled state",
                enabled = false,
                onClick = {},
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 1000,
)
@Composable
private fun OctopusOutlinedButtonPreview() {
    OctopusTheme {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Normal state
            OctopusOutlinedButton(
                text = "Normal",
                onClick = {},
            )

            // Icon
            OctopusOutlinedButton(
                text = "Icon",
                icon =
                    OctopusTheme.icons.content.post.creation
                        .create(),
                onClick = {},
            )

            // Loading state
            OctopusOutlinedButton(
                text = "Loading state",
                loading = true,
                onClick = {},
            )

            // Disabled state
            OctopusOutlinedButton(
                text = "Disabled state",
                enabled = false,
                onClick = {},
            )
        }
    }
}

@Preview(
    showBackground = false,
    widthDp = 500,
)
@Composable
private fun OctopusMediumButtonPreview() {
    OctopusTheme {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Normal
            OctopusMediumButton(
                text = "Normal",
                onClick = {},
            )

            // Icon
            OctopusMediumButton(
                text = "Icon",
                icon =
                    OctopusTheme.icons.content.post.creation
                        .create(),
                onClick = {},
            )

            // Loading state
            OctopusMediumButton(
                text = "Loading state",
                loading = true,
                onClick = {},
            )

            // Disabled state
            OctopusMediumButton(
                text = "Disabled state",
                enabled = false,
                onClick = {},
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 1000,
)
@Composable
private fun OctopusMediumOutlinedButtonPreview() {
    OctopusTheme {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Normal state
            OctopusMediumOutlinedButton(
                text = "Normal",
                onClick = {},
            )

            // Icon
            OctopusMediumOutlinedButton(
                text = "Icon",
                icon =
                    OctopusTheme.icons.content.post.creation
                        .create(),
                onClick = {},
            )

            // Loading state
            OctopusMediumOutlinedButton(
                text = "Loading state",
                loading = true,
                onClick = {},
            )

            // Disabled state
            OctopusMediumOutlinedButton(
                text = "Disabled state",
                enabled = false,
                onClick = {},
            )
        }
    }
}

@Preview(
    showBackground = false,
    widthDp = 500,
)
@Composable
private fun OctopusSmallButtonPreview() {
    OctopusTheme {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Normal
            OctopusSmallButton(
                text = "Normal",
                onClick = {},
            )

            // Icon
            OctopusSmallButton(
                text = "Icon",
                icon =
                    OctopusTheme.icons.content.post.creation
                        .create(),
                onClick = {},
            )

            // Loading state
            OctopusSmallButton(
                text = "Loading state",
                loading = true,
                onClick = {},
            )

            // Disabled state
            OctopusSmallButton(
                text = "Disabled state",
                enabled = false,
                onClick = {},
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 500,
)
@Composable
private fun OctopusSmallOutlinedButtonPreview() {
    OctopusTheme {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Normal
            OctopusSmallOutlinedButton(
                text = "Normal",
                onClick = {},
            )

            // Icon
            OctopusSmallOutlinedButton(
                text = "Icon",
                icon =
                    OctopusTheme.icons.content.post.creation
                        .create(),
                onClick = {},
            )

            // Loading state
            OctopusSmallOutlinedButton(
                text = "Loading state",
                loading = true,
                onClick = {},
            )

            // Disabled state
            OctopusSmallOutlinedButton(
                text = "Disabled state",
                enabled = false,
                onClick = {},
            )

            // Emoji
            OctopusSmallOutlinedButton(
                emoji = "❤\uFE0F",
                onClick = {},
            )
        }
    }
}
