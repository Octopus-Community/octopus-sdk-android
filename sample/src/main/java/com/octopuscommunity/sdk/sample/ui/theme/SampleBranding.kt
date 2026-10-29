package com.octopuscommunity.sdk.sample.ui.theme

/**
 * Brand constants of the Octopus sample: the platform wording. Theme-dependent roles (surfaces,
 * text, statuses) live in [SampleColors]; nothing here changes with light or dark.
 *
 * Nothing here belongs to the SDK. The palette the sample *hands to* the SDK is built from
 * [SampleColors] in `CommunityTheme`.
 */
object SampleBranding {
    /** The platform name the platform marker shows. */
    const val PLATFORM_LABEL = "Android"

    /** Trademark attribution for naming the platform in a third-party app, shown once on About. */
    const val PLATFORM_ATTRIBUTION = "Android is a trademark of Google LLC."
}
