package com.octopuscommunity.sample.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.octopuscommunity.sample.R
import com.octopuscommunity.sdk.ui.OctopusImagesDefaults
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.octopusDarkColorScheme
import com.octopuscommunity.sdk.ui.octopusLightColorScheme

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) {
            darkColorScheme(
                primary = Color(0xFF3AD9B1),
                onPrimary = Color(0xFF141414),
                secondaryContainer = Color(0xFF3AD9B1),
                onSecondaryContainer = Color(0xFF141414),
                surface = Color(0xFF141414),
                background = Color(0xFF141414)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF068677),
                onPrimary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFF068677),
                onSecondaryContainer = Color(0xFFFFFFFF),
                surface = Color(0xFFFFFFFF),
                background = Color(0xFFFFFFFF)
            )
        },
        content = content
    )
}

@Composable
fun CommunityTheme(content: @Composable () -> Unit) {
    OctopusTheme(
        colorScheme = if (isSystemInDarkTheme()) {
            octopusDarkColorScheme(
                primary = Color(0xFF3AD9B1),
                primaryLow = Color(0xFF083B2F),
                primaryHigh = Color(0xFFD8F4F1),
                onPrimary = Color(0xFF141414),
                background = Color(0xFF141414),
                onHover = Color(0xFF242526)
            )
        } else {
            octopusLightColorScheme(
                primary = Color(0xFF068677),
                primaryLow = Color(0xFFD8F4F1),
                primaryHigh = Color(0xFF15D1A2),
                onPrimary = Color(0xFFFFFFFF),
                background = Color(0xFFFFFFFF),
                onHover = Color(0xFFFFFFFF)
            )
        },
        images = OctopusImagesDefaults.images(
            logo = painterResource(R.drawable.ic_logo)
        ),
        content = content
    )
}