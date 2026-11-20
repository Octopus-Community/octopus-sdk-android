package com.octopuscommunity.sdk.ui.components

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.core.net.toUri
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.ui.OctopusColorScheme
import com.octopuscommunity.sdk.ui.OctopusTheme

/**
 * Strategy for handling URL opening within the Octopus SDK.
 *
 * When a user taps on a link inside the community (in posts, comments, replies, or CTA buttons),
 * this sealed interface determines how the URL should be handled.
 */
sealed interface UrlOpeningStrategy {
    /**
     * The URL is handled by the app. This means you, as a developer, take care of handling
     * this URL and opening a screen, a website, an app, or something else with it.
     * The Octopus SDK will not do anything further with this URL.
     */
    data object HandledByApp : UrlOpeningStrategy

    /**
     * The URL is handled by Octopus Community. It will be opened in Chrome Custom Tabs
     * with app theming, or the default browser as fallback.
     */
    data object HandledByOctopus : UrlOpeningStrategy
}

internal data class OctopusNavigationHandler(
    val onNavigateToLogin: (() -> Unit)?,
    val onNavigateToProfileEdit: ((fieldToEdit: ProfileField?) -> Unit)?,
    val onNavigateToClientObject: ((id: String) -> Unit)?,
    val onGroupAccessDenied: ((groupId: String) -> Unit)?,
    val onNavigateToProfile: ((clientUserId: String) -> Unit)?,
    val onBack: (() -> Unit)? = null,
)

internal val LocalNavigationHandler =
    staticCompositionLocalOf<OctopusNavigationHandler?> { null }

/**
 * Custom UriHandler that intercepts URL opening and delegates to the callback.
 * If HandledByOctopus, opens in Chrome Custom Tabs with theming.
 * If HandledByApp, does nothing (app handles it).
 */
private class OctopusUriHandler(
    private val context: Context,
    private val colorScheme: OctopusColorScheme,
    private val onNavigateToUrl: (url: String) -> UrlOpeningStrategy,
) : UriHandler {
    override fun openUri(uri: String) {
        when (onNavigateToUrl(uri)) {
            UrlOpeningStrategy.HandledByOctopus -> {
                runCatching {
                    val colors = colorScheme.toCustomTabColors()
                    CustomTabsIntent
                        .Builder()
                        .setShowTitle(true)
                        .setUrlBarHidingEnabled(true)
                        .setColorScheme(colors.colorScheme)
                        .setDefaultColorSchemeParams(
                            CustomTabColorSchemeParams
                                .Builder()
                                .apply {
                                    colors.toolbar?.let { setToolbarColor(it) }
                                    colors.secondaryToolbar?.let { setSecondaryToolbarColor(it) }
                                    colors.navigationBar?.let { setNavigationBarColor(it) }
                                    colors.navigationBarDivider?.let {
                                        setNavigationBarDividerColor(it)
                                    }
                                }.build(),
                        ).build()
                        .launchUrl(context, uri.toUri())
                }.onFailure {
                    runCatching {
                        // Fallback to default browser if Chrome Custom Tabs is not available
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, uri.toUri())
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }
            }

            UrlOpeningStrategy.HandledByApp -> { // App handles it, do nothing
            }
        }
    }
}

/**
 * The palette handed to Chrome Custom Tabs: raw ARGB for every slot Chrome can be trusted with
 * (`null` for the others, see [toCustomTabColors]), plus the [CustomTabsIntent] `COLOR_SCHEME_*`
 * constant those colors belong to.
 */
internal data class CustomTabColors(
    val toolbar: Int?,
    val secondaryToolbar: Int?,
    val navigationBar: Int?,
    val navigationBarDivider: Int?,
    val colorScheme: Int,
)

/**
 * Projects this palette onto Chrome Custom Tabs.
 *
 * **The scheme must be pinned, not left to Chrome.** Chrome draws the tab's *own* chrome — page
 * title, overflow and share icons, the toolbar's ripple — from the color scheme it is told to use,
 * and absent [CustomTabsIntent.Builder.setColorScheme] it picks that from the *system* dark/light
 * setting while the toolbar underneath comes from us. Any host whose theme is decoupled from the OS
 * — an in-app theme toggle, a light-only Material theme on a device in dark mode — therefore got
 * light-scheme content on a dark toolbar or the reverse, leaving the page title and the overflow
 * icons invisible. Pinning the scheme to the palette the toolbar is painted from keeps the two
 * halves consistent. For a host whose theme does follow the system setting, this pins what Chrome
 * would have chosen anyway; the one thing it gives up is that an already-open tab no longer
 * re-themes itself mid-session when the setting flips — the right trade, since its toolbar colors
 * were pinned all along and would not have followed either.
 *
 * **The scheme is chosen by Chrome's own content, not by our dark/light split.** Chrome paints that
 * content either white or black, so the scheme follows whichever of the two contrasts better against
 * the toolbar — the WCAG crossover, at luminance ≈ `0.179`. Deliberately *not* the `0.5` split
 * [com.octopuscommunity.sdk.ui.toOctopusColorScheme] uses to pick a palette: a mid-luminance
 * background (a saturated brand color, a mid grey) counts as "dark" under that rule while being far
 * too light to carry white text — `0xFFBBBBBB` would have taken a white title at `1.92:1`, well
 * under the `4.5:1` WCAG AA floor this module already holds itself to elsewhere.
 *
 * **Colors reach Chrome as raw ARGB, and Chrome does not composite alpha the way Compose does.** A
 * translucent slot — or one a host never wired, which arrives as [Color.Unspecified], alpha `0f` —
 * would render as a transparent or black toolbar rather than blending onto what is behind it. Such
 * a slot is dropped instead of sent, so Chrome falls back to its own default for it, which is
 * always self-consistent with the scheme pinned above. That single alpha check covers the
 * unspecified case too, so no separate [Color.isSpecified] arm is needed here — unlike the scheme
 * below, where alpha is ignored and `Unspecified` would read as pitch black.
 */
internal fun OctopusColorScheme.toCustomTabColors(): CustomTabColors =
    CustomTabColors(
        toolbar = background.toOpaqueArgbOrNull(),
        secondaryToolbar = primary.toOpaqueArgbOrNull(),
        navigationBar = background.toOpaqueArgbOrNull(),
        navigationBarDivider = gray200.toOpaqueArgbOrNull(),
        colorScheme = background.customTabColorScheme(),
    )

private fun Color.toOpaqueArgbOrNull(): Int? = takeIf { it.alpha == 1f }?.toArgb()

/**
 * The `COLOR_SCHEME_*` constant whose content stays legible on this color: the dark scheme, whose
 * content is white, only where white beats black. An unspecified background falls to the light
 * scheme — [luminance] ignores alpha and would otherwise read it as pitch black.
 */
private fun Color.customTabColorScheme(): Int =
    when {
        isSpecified && contrastRatio(this, Color.White) > contrastRatio(this, Color.Black) ->
            CustomTabsIntent.COLOR_SCHEME_DARK

        else -> CustomTabsIntent.COLOR_SCHEME_LIGHT
    }

/** WCAG contrast ratio between two opaque colors — [luminance] ignores alpha. */
private fun contrastRatio(
    first: Color,
    second: Color,
): Float {
    val lighter = first.luminance() + 0.05f
    val darker = second.luminance() + 0.05f
    return if (lighter > darker) lighter / darker else darker / lighter
}

/**
 * Composable that provides navigation callbacks for the Octopus SDK screens.
 *
 * This handler allows the host app to intercept various navigation events and handle them
 * according to the app's needs.
 *
 * @param onNavigateToLogin Called when the SDK needs a logged-in user. This should launch
 * navigation to your login screen. Once login is complete, call [OctopusSDK.connectUser].
 * @param onNavigateToProfileEdit Called when the user tries to modify profile fields — a specific
 * app-managed field, or general editing (parameter `null`). In Unified Profile mode it is also
 * invoked with `null` from the Activity screen's "Edit my profile" action, so the host owns the
 * whole edit flow. The parameter indicates which field should be focused, or null for general
 * editing.
 * @param onNavigateToClientObject Called when a user taps on the CTA button of a post
 * related to a client object (article, product, etc.). The parameter is the object ID.
 * @param onGroupAccessDenied Called when a user attempts to interact with a visible-but-locked
 * group (tap on the group row, follow button, picker selection). The parameter is the
 * `groupId` of the locked group. The host app decides what to do (open an upsell screen,
 * present a paywall, etc.). The SDK does not navigate on the user's behalf.
 * @param onNavigateToUrl Called when a user tries to open a link inside the community.
 * This link can come from a Post, Comment, Reply content, or when tapping on a Post's CTA button.
 * Return [UrlOpeningStrategy.HandledByApp] if you handle the URL yourself, or
 * [UrlOpeningStrategy.HandledByOctopus] to let the SDK open it in Chrome Custom Tabs.
 * By default, URLs will be opened in Chrome Custom Tabs with app theming.
 * @param onNavigateToProfile Called when a user taps a member's profile (including their own),
 * with that member's `clientUserId`, so the host can open its own profile screen. Unified Profile
 * requires BOTH the community's backend flag (`CommunityConfig.exposeClientUserId`) and this
 * callback. When active, members without a client id (guest / BO / admin) open the Octopus activity
 * screen; this callback never receives a `null` id. With the flag off or this callback absent,
 * the SDK keeps native profiles.
 * @param onBack Called when the SDK's top-bar back chevron is tapped on a screen that is the
 * START/root destination of the SDK's internal back stack — i.e. bridge mode, where the host opened
 * the SDK directly on a sub-screen (a post, group, create-post, member activity, or the current-user
 * profile) so there is nothing left to pop internally. This lets the host dismiss its own container (pop its route). On non-root screens the
 * SDK still pops its own internal back stack and this is not called.
 * @param content The composable content wrapped by this handler.
 */
@Composable
fun OctopusNavigationHandler(
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToProfileEdit: ((fieldToEdit: ProfileField?) -> Unit)? = null,
    onNavigateToClientObject: ((id: String) -> Unit)? = null,
    onGroupAccessDenied: ((groupId: String) -> Unit)? = null,
    onNavigateToUrl: ((url: String) -> UrlOpeningStrategy) = { UrlOpeningStrategy.HandledByOctopus },
    onNavigateToProfile: ((clientUserId: String) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = OctopusTheme.colorScheme

    CompositionLocalProvider(
        LocalNavigationHandler provides
            remember(
                onNavigateToLogin,
                onNavigateToProfileEdit,
                onNavigateToClientObject,
                onGroupAccessDenied,
                onNavigateToUrl,
                onNavigateToProfile,
                onBack,
            ) {
                OctopusNavigationHandler(
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToProfileEdit = onNavigateToProfileEdit,
                    onNavigateToClientObject = onNavigateToClientObject,
                    onGroupAccessDenied = onGroupAccessDenied,
                    onNavigateToProfile = onNavigateToProfile,
                    onBack = onBack,
                )
            },
        LocalUriHandler provides
            remember(context, colorScheme, onNavigateToUrl) {
                OctopusUriHandler(
                    context = context,
                    colorScheme = colorScheme,
                    onNavigateToUrl = onNavigateToUrl,
                )
            },
    ) {
        content()
    }
}
