package com.octopuscommunity.sdk.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import com.octopuscommunity.sdk.InternalOctopusApi
import com.octopuscommunity.sdk.OctopusLifecycle
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.ui.common.LocalOctopusCommunityConfig
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.ComposeContextWrapper
import org.koin.compose.LocalKoinScope
import org.koin.compose.getKoin
import org.koin.core.Koin
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import java.lang.ref.WeakReference
import java.util.UUID

@OptIn(InternalOctopusApi::class)
internal val LocalOctopusContainer = staticCompositionLocalOf<OctopusLifecycle.Container?> { null }
internal val LocalOctopusGeneration = staticCompositionLocalOf { "preview" }

internal val octopusNavigationScopeQualifier = named("octopusNavigationHost")

internal val LocalOctopusKoin = staticCompositionLocalOf<Koin?> { null }
private val LocalOctopusNavController = staticCompositionLocalOf<NavHostController?> { null }
private val LocalOctopusNavigationHost = staticCompositionLocalOf<OctopusNavigationHost?> { null }

private class OctopusNavigationHost(
    val controller: NavHostController,
    val holder: OctopusNavigationScopeViewModel,
    val generation: String,
) {
    val scope get() = holder.scope
}

/** Supplies the same navigation host to the screen and all its nested SDK components. */
@Composable
internal fun OctopusContent(
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalOctopusNavController provides navController) {
        OctopusContent(content = content)
    }
}

/** Retains navigation state with the host's back stack, including across configuration changes. */
@Composable
internal fun OctopusNavigationScope(content: @Composable () -> Unit) {
    OctopusCommunityConfigProvider { OctopusNavigationHostScope(content) }
}

/**
 * Provides [LocalOctopusCommunityConfig] to [content], so SDK texts follow the community's tone
 * (internal ticket). Every navigation destination is its own root, so a root starts from the enclosing
 * one's value, else from the last configuration any root of this Koin read: a screen pushed on an
 * informal community does not flash its formal texts while its own reader starts. Only the very
 * first root of a process, before the cached configuration is read, starts formal.
 */
@Composable
private fun OctopusCommunityConfigProvider(content: @Composable () -> Unit) {
    val koin = getKoin()
    val seed = LocalOctopusCommunityConfig.current ?: LastCommunityConfig.of(koin)
    val configFlow =
        remember(koin) {
            koin.getOrNull<CommunityConfigRepository>()?.getCommunityConfigFlow() ?: flowOf(null)
        }
    val config by configFlow.collectAsStateWithLifecycle(initialValue = seed)
    val current = config ?: seed
    SideEffect { LastCommunityConfig.remember(koin, current) }
    CompositionLocalProvider(LocalOctopusCommunityConfig provides current, content = content)
}

/** The last community configuration a root read, per Koin, to seed the next root (see above). */
private object LastCommunityConfig {
    // Weak, so a stopped SDK's Koin is not kept alive by the seed it left behind.
    @Volatile private var last: Pair<WeakReference<Koin>, CommunityConfig>? = null

    fun of(koin: Koin): CommunityConfig? = last?.takeIf { it.first.get() === koin }?.second

    fun remember(
        koin: Koin,
        config: CommunityConfig?,
    ) {
        if (config != null) last = WeakReference(koin) to config
    }
}

@OptIn(KoinInternalApi::class, InternalOctopusApi::class)
@Composable
private fun OctopusNavigationHostScope(content: @Composable () -> Unit) {
    val koin = getKoin()
    val controller = LocalOctopusNavController.current
    val inheritedHost =
        LocalOctopusNavigationHost.current?.takeIf {
            !it.scope.closed &&
                it.scope.getKoin() === koin &&
                (controller == null || controller === it.controller)
        }
    if (inheritedHost != null) {
        val lease = remember(inheritedHost.holder) { NavigationScopeLease(inheritedHost.holder) }
        if (!lease.acquired) return
        // Nested wrappers restore the enclosing host's scope without a second ViewModel lookup
        // for each controller-less leaf.
        CompositionLocalProvider(
            LocalOctopusGeneration provides inheritedHost.generation,
            LocalKoinScope provides ComposeContextWrapper(inheritedHost.scope),
            content = content,
        )
        return
    }
    if (controller == null) {
        // Standalone SDK components keep using the shared navigator collected by live SDK hosts.
        // They must not acquire an orphan scope from the app's Activity or back-stack entry.
        CompositionLocalProvider(
            LocalOctopusKoin provides koin,
            LocalOctopusNavigationHost provides null,
            LocalKoinScope provides ComposeContextWrapper(koin.scopeRegistry.rootScope),
            content = content,
        )
        return
    }
    // The root graph belongs to this NavController. All its destinations must share the
    // navigator, so an event from a retained ViewModel still reaches this host's collector.
    val graphOwner =
        controller.currentBackStackEntry?.let {
            controller.getBackStackEntry(controller.graph.id)
        }
    // A new SDK graph must not reuse a scope attached to a stopped community. Previews and
    // explicit preview controllers may have no navigation graph; distinguish them there.
    val controllerKey = if (graphOwner != null) "" else ":${System.identityHashCode(controller)}"
    val generation = LocalOctopusGeneration.current
    var recovery by remember(koin, controller) { mutableIntStateOf(0) }
    val owner = graphOwner ?: checkNotNull(LocalViewModelStoreOwner.current)
    val scopeGeneration = "$generation:$recovery$controllerKey"
    // Retain the admitted holder through an outgoing composition, even if its owner clears its
    // store during a pop animation (internal change). An open scope never triggers recovery or a warning.
    val scopeHolder =
        remember(owner, koin, scopeGeneration) {
            ViewModelProvider(
                owner,
                object : Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = OctopusNavigationScopeViewModel(koin) as T
                },
            )["octopusNavigationScope:$scopeGeneration", OctopusNavigationScopeViewModel::class.java]
        }
    val lease = remember(scopeHolder) { NavigationScopeLease(scopeHolder) }
    if (scopeHolder.closed || scopeHolder.scope.closed || !lease.acquired) {
        SideEffect {
            Log.w("OctopusSDK", "Recreating SDK host after its navigation scope was closed")
            recovery++
        }
        return
    }
    val hostGeneration = "$generation:${scopeHolder.identity}"
    val host = remember(controller, scopeHolder.scope) { OctopusNavigationHost(controller, scopeHolder, hostGeneration) }
    CompositionLocalProvider(
        LocalOctopusGeneration provides hostGeneration,
        LocalOctopusKoin provides koin,
        LocalOctopusNavigationHost provides host,
        LocalKoinScope provides ComposeContextWrapper(scopeHolder.scope),
        content = content,
    )
}

internal class OctopusNavigationScopeViewModel internal constructor(
    koin: Koin,
) : ViewModel(octopusViewModelScope(koin.getProperty("octopusViewModelScope"))) {
    val identity = identities.incrementAndGet()

    private companion object {
        val identities =
            java.util.concurrent.atomic
                .AtomicLong()
    }

    val scope = koin.createScope(UUID.randomUUID().toString(), octopusNavigationScopeQualifier)

    var closed by mutableStateOf(false)
        private set

    init {
        val work = OctopusHostCoroutineScope(octopusViewModelScope(koin.getProperty("octopusViewModelScope")))
        scope.declare(work)
        scope.registerCallback(
            object : org.koin.core.scope.ScopeCallback {
                override fun onScopeClose(scope: Scope) {
                    work.scope.cancel()
                    closed = true
                }
            },
        )
    }

    // A cleared owner or retired SDK generation keeps its scope through the outgoing frame.
    // The scope closes only after the last composition has released it.
    private var compositions = 0
    private var retired = false
    private var cleared = false

    init {
        viewModelScope.coroutineContext[Job]?.invokeOnCompletion { retire() }
    }

    @Synchronized
    private fun retire() {
        retired = true
        closeIfUnused()
    }

    @Synchronized
    fun acquire(): Boolean {
        if (scope.closed) return false
        compositions++
        return true
    }

    @Synchronized
    fun release() {
        compositions--
        closeIfUnused()
    }

    @Synchronized
    override fun onCleared() {
        cleared = true
        closeIfUnused()
    }

    private fun closeIfUnused() {
        if ((cleared || retired) && compositions == 0) scope.close()
    }
}

/** Protects the interval between ViewModel lookup and composition effects, including abandonment. */
private class NavigationScopeLease(
    private val holder: OctopusNavigationScopeViewModel,
) : RememberObserver {
    val acquired = holder.acquire()
    private var released = false

    override fun onRemembered() = Unit

    override fun onForgotten() = release()

    override fun onAbandoned() = release()

    private fun release() {
        if (acquired && !released) {
            released = true
            holder.release()
        }
    }
}

/** Work belongs to one navigation host; an owner's clear alone does not retire it. */
internal class OctopusHostCoroutineScope(
    val scope: CoroutineScope,
)
