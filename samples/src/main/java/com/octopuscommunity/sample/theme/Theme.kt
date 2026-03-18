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

// Brand colors — change these to match your app's identity
private val BrandPrimary = Color(0xFF4F46E5)       // Indigo
private val BrandPrimaryDark = Color(0xFF818CF8)    // Indigo light (for dark mode)
private val BrandBackground = Color(0xFFFAFAFA)     // Warm off-white
private val BrandBackgroundDark = Color(0xFF111118)  // Deep charcoal
private val BrandSurface = Color(0xFFFFFFFF)
private val BrandSurfaceDark = Color(0xFF1C1C24)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) {
            darkColorScheme(
                primary = BrandPrimaryDark,
                onPrimary = Color(0xFF1E1B4B),
                secondaryContainer = Color(0xFF312E81),
                onSecondaryContainer = Color(0xFFC7D2FE),
                surface = BrandSurfaceDark,
                background = BrandBackgroundDark
            )
        } else {
            lightColorScheme(
                primary = BrandPrimary,
                onPrimary = Color.White,
                secondaryContainer = Color(0xFFE0E7FF),
                onSecondaryContainer = Color(0xFF312E81),
                surface = BrandSurface,
                background = BrandBackground
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
                primary = BrandPrimaryDark,
                primaryLow = Color(0xFF1E1B4B),
                primaryHigh = Color(0xFFC7D2FE),
                onPrimary = Color(0xFF1E1B4B),
                background = BrandBackgroundDark,
                onHover = Color(0xFF1C1C24)
            )
        } else {
            octopusLightColorScheme(
                primary = BrandPrimary,
                primaryLow = Color(0xFFE0E7FF),
                primaryHigh = Color(0xFF6366F1),
                onPrimary = Color.White,
                background = BrandBackground,
                onHover = Color.White
            )
        },
        images = OctopusImagesDefaults.images(
            logo = { painterResource(R.drawable.ic_logo) }
        ),
        content = content
    )
}