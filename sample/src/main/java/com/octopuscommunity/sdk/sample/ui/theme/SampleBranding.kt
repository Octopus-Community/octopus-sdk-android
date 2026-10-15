package com.octopuscommunity.sdk.sample.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand constants of the Octopus sample: the two brand hues, this platform's marker colour and the
 * platform wording. Theme-dependent roles (surfaces, text, statuses) live in [SampleColors]; nothing
 * here changes with light or dark.
 *
 * Nothing here belongs to the SDK. The palette the sample *hands to* the SDK is built from
 * [SampleColors] in `CommunityTheme`.
 */
object SampleBranding {
    /** The Octopus Community brand navy (`--navy`): primary button in light, the code block, the icon. */
    val Navy = Color(0xFF0F1B2D)

    /** The brand blue (`--accent`): the focus ring and the launcher-icon gradient. A fill, never a label. */
    val Accent = Color(0xFF1D88FE)

    /** The Android robot of the platform marker (`--platform-android`). */
    val PlatformAndroid = Color(0xFF34A853)

    /** The platform name the platform marker shows. */
    const val PLATFORM_LABEL = "Android"

    /** Trademark attribution for naming the platform in a third-party app, shown once on About. */
    const val PLATFORM_ATTRIBUTION = "Android is a trademark of Google LLC."
}
