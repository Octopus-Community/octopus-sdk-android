package com.octopuscommunity.sdk.ui.components

import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.InternalOctopusApi
import com.octopuscommunity.sdk.OctopusLifecycle
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.repository.LanguageRepository
import kotlinx.coroutines.flow.flowOf
import java.util.Locale

/**
 * Wraps content with the overridden locale from the SDK if one is set.
 *
 * This composable checks if a locale override has been set via [OctopusSDK.overrideDefaultLocale]
 * and if so, provides that locale to all child composables through [LocalConfiguration] and a
 * [LocalContext] whose resources resolve in that locale.
 *
 * This is the Android equivalent of iOS's `.environment(\.locale, overridenLocale)`.
 *
 * Usage:
 * ```kotlin
 * octopusComposables(
 *     navController = navController,
 *     container = { backStackEntry, content ->
 *         OctopusLocaleWrapper {
 *             OctopusTheme {
 *                 content()
 *             }
 *         }
 *     }
 * )
 * ```
 */
@OptIn(InternalOctopusApi::class)
@Composable
fun OctopusLocaleWrapper(content: @Composable () -> Unit) {
    val enclosingKoin = LocalOctopusKoin.current
    val localeFlow =
        if (enclosingKoin != null) {
            // Stay with the admitted frame's container when stop/reinitialize happens on a worker.
            enclosingKoin.get<LanguageRepository>().overriddenLocaleFlow
        } else {
            val container by OctopusLifecycle.container.collectAsState()
            remember(container) {
                val current = container
                if (current != null && current.acquire()) {
                    try {
                        current.application.koin
                            .get<LanguageRepository>()
                            .overriddenLocaleFlow
                    } finally {
                        current.release()
                    }
                } else {
                    flowOf(null)
                }
            }
        }
    val overriddenLocale by localeFlow.collectAsStateWithLifecycle(null)
    OctopusLocaleWrapper(locale = overriddenLocale, content = content)
}

/**
 * The locale-forcing half of [OctopusLocaleWrapper], with the locale handed in rather than read
 * from the SDK so a test can compose it without a running SDK.
 *
 * A null [locale] composes [content] untouched. Otherwise [content] sees a [LocalConfiguration],
 * a [LocalLayoutDirection] and a [LocalContext] whose resources resolve in [locale] — the layout
 * direction because Compose resolves it once at its own root and never re-reads it from the
 * configuration, so without it an RTL override renders Arabic strings in a left-to-right layout.
 * That context stays a `ContextWrapper`
 * around the one it replaces, so the host Activity remains reachable through `baseContext`:
 * Compose's text-selection "process text" actions call `startActivity` on it, and telephoto as
 * well as `LocalActivityResultRegistryOwner` walk the chain to find the Activity. A bare
 * `createConfigurationContext()` result — a `ContextImpl` wrapping nothing — broke all three.
 */
@Composable
internal fun OctopusLocaleWrapper(
    locale: Locale?,
    content: @Composable () -> Unit,
) {
    if (locale == null) {
        content()
        return
    }

    val baseContext = LocalContext.current
    val configuration =
        Configuration(LocalConfiguration.current).apply {
            setLocale(locale)
        }
    val localizedContext =
        remember(baseContext, configuration) {
            baseContext.withOverrideConfiguration(configuration)
        }

    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalContext provides localizedContext,
        LocalLayoutDirection provides configuration.composeLayoutDirection,
        content = content,
    )
}

/**
 * The Compose layout direction this configuration implies.
 *
 * `Configuration.setLocale` also sets `layoutDirection`, but Compose does not read it from
 * [LocalConfiguration]: `LocalLayoutDirection` is resolved once at the `AndroidComposeView` root
 * from the *Activity's* configuration and never re-derived. Without re-providing it, an override
 * to an RTL locale resolves its strings from `values-ar` while every `Row`, `Arrangement.Start`,
 * start/end padding and text alignment below still lays out left-to-right.
 */
private val Configuration.composeLayoutDirection: LayoutDirection
    get() =
        when (layoutDirection) {
            View.LAYOUT_DIRECTION_RTL -> LayoutDirection.Rtl
            else -> LayoutDirection.Ltr
        }

/**
 * A context whose resources resolve under [configuration] while still wrapping this one.
 *
 * Unlike [Context.createConfigurationContext], whose result is a bare `ContextImpl`, the wrapper
 * keeps `this` as its `baseContext`: `startActivity` and every other call it does not override
 * still reach the Activity underneath, and anything walking the `ContextWrapper` chain finds it.
 * The theme is the base context's own on every supported API level.
 */
internal fun Context.withOverrideConfiguration(configuration: Configuration): Context {
    val wrapper = ContextThemeWrapper(this, theme)
    // Has to happen before anything reads the wrapper's resources: the platform refuses it after.
    wrapper.applyOverrideConfiguration(configuration)
    return wrapper
}
