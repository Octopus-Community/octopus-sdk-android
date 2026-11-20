package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.medium

@Composable
internal fun OctopusTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: Any? = null,
    image: Painter? = null,
    emoji: String? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    textStyle: TextStyle = OctopusTheme.typography.body2.medium,
    color: Color = OctopusTheme.colorScheme.primary,
    disabledColor: Color = if (loading) color.copy(alpha = 0.52f) else OctopusTheme.colorScheme.gray500,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    loadingIndicatorSize: Dp = 16.dp,
    loadingIndicatorStrokeWidth: Dp = 2.dp,
    loadingIndicatorColor: Color = color,
    horizontalSpacing: Dp = 16.dp,
) = TextButton(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled && !loading,
    colors =
        ButtonDefaults.textButtonColors(
            contentColor = color,
            disabledContentColor = disabledColor,
        ),
    shape = RectangleShape,
    contentPadding = contentPadding,
) {
    if (loading) {
        OctopusProgressIndicator(
            modifier = Modifier.size(loadingIndicatorSize),
            strokeWidth = loadingIndicatorStrokeWidth,
            color = loadingIndicatorColor,
        )
        Spacer(modifier = Modifier.width(horizontalSpacing))
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
            style = textStyle,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusTextButtonPreview() {
    OctopusTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Normal state
            OctopusTextButton(
                text = "Normal",
                icon = Icons.Outlined.PhotoLibrary,
                onClick = {},
            )

            // Loading state
            OctopusTextButton(
                text = "Loading state",
                icon = Icons.Outlined.PhotoLibrary,
                loading = true,
                onClick = {},
            )

            // Disabled state
            OctopusTextButton(
                text = "Disabled state",
                icon = Icons.Outlined.PhotoLibrary,
                enabled = false,
                onClick = {},
            )

            // Destructive
            OctopusTextButton(
                text = "Destructive",
                icon = Icons.Outlined.PhotoLibrary,
                color = OctopusTheme.colorScheme.alert,
                onClick = {},
            )
        }
    }
}
