package com.octopuscommunity.sdk.ui.components

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.octopuscommunity.sdk.ui.OctopusTheme

@Composable
fun OctopusProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = OctopusTheme.colorScheme.primary,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth
) = CircularProgressIndicator(
    modifier = modifier,
    color = color,
    strokeWidth = strokeWidth
)

@Preview(showBackground = true)
@Composable
private fun OctopusProgressIndicatorPreview() {
    OctopusTheme {
        OctopusProgressIndicator()
    }
}