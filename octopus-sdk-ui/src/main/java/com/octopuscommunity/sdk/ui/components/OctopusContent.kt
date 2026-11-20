package com.octopuscommunity.sdk.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coil3.annotation.ExperimentalCoilApi
import com.octopuscommunity.sdk.InternalOctopusApi
import com.octopuscommunity.sdk.OctopusLifecycle
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.di.octopusImageModule
import com.octopuscommunity.sdk.di.octopusUseCaseModule
import com.octopuscommunity.sdk.test.di.octopusTestModule
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.di.octopusFakeVideoModule
import com.octopuscommunity.sdk.ui.di.octopusValidationModule
import com.octopuscommunity.sdk.ui.di.octopusVideoModule
import com.octopuscommunity.sdk.ui.di.octopusViewModelModule
import com.octopuscommunity.sdk.ui.utils.IntegrationDiagnostics
import kotlinx.coroutines.delay
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinApplication
import org.koin.compose.KoinIsolatedContext
import org.koin.core.module.Module

private const val kOctopusUiLoadedProperty = "octopusUiLoaded"

@Composable
fun <T> OctopusContent(
    receiver: T,
    content: @Composable T.() -> Unit,
) = OctopusContent {
    content(receiver)
}

/**
 * Main UI container for Octopus SDK components.
 * Provides dependency injection context for SDK UI elements.
 *
 * Requires an initialized SDK: [OctopusSDK.initialize] must have been called — typically from
 * `Application.onCreate()` — before this container is composed. Composing it earlier is not a
 * crash, but it renders nothing and logs a warning; the content appears on its own once the host
 * initializes the SDK. It tracks the SDK container generation, so a host that calls
 * [OctopusSDK.stop] while this container is composed also gets an empty container rather than
 * a subtree bound to a closed dependency graph. Container replacements recreate the subtree and
 * its ViewModels, even if the SDK stays initialized throughout the replacement.
 *
 * @param content Composable content to be themed
 */
@OptIn(ExperimentalCoilApi::class, InternalOctopusApi::class)
@Composable
fun OctopusContent(content: @Composable () -> Unit) {
    // Some embedded hosts do not implement ViewModelStoreOwner. Give their SDK content an owner
    // whose lifetime is the composition, rather than requiring an AndroidX Activity from the host.
    val owner = LocalViewModelStoreOwner.current
    if (owner == null) {
        val fallback =
            remember {
                object : ViewModelStoreOwner {
                    override val viewModelStore = ViewModelStore()
                }
            }
        DisposableEffect(fallback) { onDispose { fallback.viewModelStore.clear() } }
        CompositionLocalProvider(LocalViewModelStoreOwner provides fallback) { OctopusContent(content) }
        return
    }
    val inheritedContainer = LocalOctopusContainer.current
    if (inheritedContainer != null) {
        // Nested wrappers belong to the enclosing generation. Independently collecting a newer
        // generation here could pair a fresh container with an old parent's navigation host.
        val lease = remember(inheritedContainer) { ContainerLease(inheritedContainer) }
        if (lease.acquired) OctopusNavigationScope(content)
        return
    }
    if (LocalInspectionMode.current) {
        // Nested SDK wrappers reuse the preview graph and its navigation scope.
        if (LocalOctopusKoin.current != null) {
            OctopusNavigationScope(content)
            return
        }
        val context = LocalContext.current
        val previewModules =
            rememberIsolatedModules(
                remember {
                    listOf(
                        octopusTestModule,
                        octopusFakeVideoModule,
                        octopusImageModule,
                        octopusValidationModule,
                        octopusViewModelModule,
                        octopusUseCaseModule,
                    )
                },
            ) ?: return

        // For preview mode, create a simple Koin instance and use KoinContext
        KoinApplication(
            application = {
                androidContext(context)
                modules(previewModules)
            },
        ) {
            OctopusNavigationScope(content)
        }
    } else {
        // A Boolean can conflate stop/reinitialize on Main. The generation changes for every
        // replacement and therefore also resets remembered Koin scopes and host-owned ViewModels.
        val container by OctopusLifecycle.container.collectAsState()
        if (container == null) {
            LaunchedEffect(Unit) {
                Log.w(
                    "OctopusSDK",
                    "OctopusContent was composed while the Octopus SDK is not initialized " +
                        "(initialize() has not been called yet, or stop() has been called " +
                        "since), so it rendered nothing. Call OctopusSDK.initialize() early in " +
                        "your application lifecycle (typically Application.onCreate()); the " +
                        "Octopus UI appears as soon as the SDK is initialized.",
                )
            }
            return
        }

        val current = container ?: return
        key(current.generation) {
            // Acquire synchronously with remember (not in an effect after composition). stop() on
            // a worker cannot close Koin between this check and a child's synchronous resolution.
            val lease = remember(current) { ContainerLease(current) }
            if (lease.acquired) {
                val koin = current.application.koin
                val requested =
                    remember(current) {
                        if (koin.getProperty(kOctopusUiLoadedProperty, false)) {
                            emptyList()
                        } else {
                            buildList {
                                if (koin.getOrNull<coil3.ImageLoader>() == null) add(octopusImageModule)
                                add(octopusValidationModule)
                                add(octopusVideoModule)
                                if (koin.getOrNull<com.octopuscommunity.sdk.ui.common.viewmodel.OctopusUiOpenings>() == null) {
                                    add(octopusViewModelModule)
                                }
                            }
                        }
                    }
                val modules = if (requested.isEmpty()) emptyList() else rememberIsolatedModules(requested)
                if (modules != null && OctopusLifecycle.container.value === current) {
                    CompositionLocalProvider(
                        LocalOctopusContainer provides current,
                        LocalOctopusGeneration provides current.generation.toString(),
                    ) {
                        KoinIsolatedContext(
                            context =
                                current.application.apply {
                                    if (!koin.getProperty(kOctopusUiLoadedProperty, false)) {
                                        modules(modules)
                                        koin.setProperty(kOctopusUiLoadedProperty, true)
                                    }
                                },
                        ) {
                            OctopusNavigationScope(content)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(InternalOctopusApi::class)
@Composable
private fun rememberIsolatedModules(requested: List<Module>): List<Module>? {
    val immediate = remember(requested) { OctopusLifecycle.isolatedModulesOrNull(*requested.toTypedArray()) }
    if (immediate != null) return immediate
    // Only a compatibility fallback can suspend; normal mounting and navigation render in the
    // first composition, including roots whose UI modules have already been loaded.
    val waiting by produceState(ModulePreparation(), requested) {
        var firstFrame: Long? = null
        while (true) {
            val now = withFrameNanos { it }
            val started = firstFrame ?: now.also { firstFrame = it }
            val modules = OctopusLifecycle.isolatedModulesOrNull(*requested.toTypedArray())
            if (modules != null) {
                value = ModulePreparation(modules)
                break
            }
            if (now - started >= 1_500_000_000L) {
                value = ModulePreparation(failed = true)
                while (true) {
                    delay(500)
                    val available = OctopusLifecycle.isolatedModulesOrNull(*requested.toTypedArray())
                    if (available != null) {
                        value = ModulePreparation(available)
                        return@produceState
                    }
                }
            }
        }
    }
    if (waiting.failed) {
        LaunchedEffect(requested) {
            IntegrationDiagnostics.reportOnce(
                "module-preparation-timeout",
                "Unable to prepare Octopus UI modules: the previous Koin container did not retire within 1.5 seconds. Check host Koin compatibility.",
            )
        }
        OctopusTheme { OctopusError(text = stringResource(R.string.error_unknown_formal)) }
    }
    return waiting.modules
}

private class ModulePreparation(
    val modules: List<Module>? = null,
    val failed: Boolean = false,
)

@OptIn(InternalOctopusApi::class)
private class ContainerLease(
    private val container: OctopusLifecycle.Container,
) : RememberObserver {
    val acquired = container.acquire()
    private var released = false

    override fun onRemembered() = Unit

    override fun onForgotten() = release()

    override fun onAbandoned() = release()

    private fun release() {
        if (acquired && !released) {
            released = true
            container.release()
        }
    }
}
