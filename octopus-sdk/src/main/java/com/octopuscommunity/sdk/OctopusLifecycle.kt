package com.octopuscommunity.sdk

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.KoinApplication
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.definition.BeanDefinition
import org.koin.core.instance.FactoryInstanceFactory
import org.koin.core.instance.InstanceFactory
import org.koin.core.instance.ScopedInstanceFactory
import org.koin.core.instance.SingleInstanceFactory
import org.koin.core.module.Module
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Opt-in bridge used by the separately published SDK UI module to lease the live graph. */
@InternalOctopusApi
object OctopusLifecycle {
    internal val lock = Any()
    private val sequence = AtomicLong()
    private val closingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableContainer = MutableStateFlow<Container?>(null)

    /** Immutable identities; changes only when the SDK publishes or retires a container. */
    val container: StateFlow<Container?> = mutableContainer.asStateFlow()

    /** A UI lease keeps synchronous lookups valid through the admitted Main frame. */
    class Container internal constructor(
        /** Monotonic identity shared by the UI subtree and its ViewModel keys. */
        val generation: Long,
        /** Dependency graph of this generation. */
        val application: KoinApplication,
        internal val scope: CoroutineScope,
    ) {
        private val retired = AtomicBoolean()
        private var readers = 0
        private var close: (() -> Unit)? = null
        private var closing: CompletableDeferred<Unit>? = null

        /** Acquires this graph if it is still live, without taking the SDK lifecycle lock. */
        @Synchronized
        fun acquire(): Boolean {
            if (retired.get()) return false
            readers++
            return true
        }

        /** Releases an admitted composition. Physical teardown always runs off Main. */
        @Synchronized
        fun release() {
            check(readers > 0)
            readers--
            if (readers == 0) scheduleClose()
        }

        @Synchronized
        internal fun retire(action: () -> Unit): Deferred<Unit> {
            retired.set(true)
            scope.cancel()
            close = action
            // Readers normally release after the subtree has been removed. A double frame callback
            // spans a complete frame (including traversal/prefetch) before bounding a retained root.
            // The delayed Main fallback also covers hosts that stop requesting display frames.
            val completion = CompletableDeferred<Unit>()
            closing = completion
            if (readers == 0) {
                scheduleClose()
            } else {
                val main = Handler(Looper.getMainLooper())
                main.post {
                    Choreographer.getInstance().postFrameCallback {
                        Choreographer.getInstance().postFrameCallback {
                            synchronized(this) { scheduleClose() }
                        }
                    }
                }
                main.postDelayed({ synchronized(this) { scheduleClose() } }, 1_000L)
            }
            return completion
        }

        private fun scheduleClose() {
            val action = close ?: return
            close = null
            val completion = checkNotNull(closing)
            closingScope.async {
                try {
                    action()
                    completion.complete(Unit)
                } catch (failure: Throwable) {
                    OctopusSDK.logWarning("Unable to close retired SDK graph (${failure.javaClass.simpleName})")
                    completion.completeExceptionally(failure)
                }
            }
        }
    }

    internal fun publish(
        application: KoinApplication,
        scope: CoroutineScope,
    ) {
        application.koin.setProperty("octopusViewModelScope", scope)
        mutableContainer.value = Container(sequence.incrementAndGet(), application, scope)
    }

    internal fun retire(close: () -> Unit): Deferred<Unit> {
        val previous = mutableContainer.value
        mutableContainer.value = null
        return previous?.retire(close) ?: closingScope.async {
            try {
                close()
            } catch (failure: Throwable) {
                OctopusSDK.logWarning("Unable to close retired SDK graph (${failure.javaClass.simpleName})")
                throw failure
            }
        }
    }

    internal val copyWarningReported = AtomicBoolean()
    internal var moduleCopier: (List<Module>) -> List<Module> = { copyModules(*it.toTypedArray()) }

    /**
     * Prepares isolated factories synchronously. Returns null only when copying is unavailable
     * and the original factories still belong to a retiring graph. Retry after that graph closes.
     */
    fun isolatedModulesOrNull(vararg modules: Module): List<Module>? =
        try {
            modulesForContainer(modules.toList())
        } catch (_: AwaitModuleRetirement) {
            null
        }

    internal fun modulesForContainer(modules: List<Module>): List<Module> {
        try {
            return moduleCopier(modules)
        } catch (failure: Exception) {
            return originalModules(modules, failure)
        } catch (failure: LinkageError) {
            return originalModules(modules, failure)
        }
    }

    private fun originalModules(
        modules: List<Module>,
        failure: Throwable,
    ): List<Module> {
        if (copyWarningReported.compareAndSet(false, true)) {
            OctopusSDK.logWarning("Unable to isolate SDK modules (${failure.javaClass.simpleName}); loading original modules")
        }
        OctopusSDK.lastRetirement?.takeUnless { it.isCompleted }?.let { throw AwaitModuleRetirement(it) }
        return modules
    }

    internal class AwaitModuleRetirement(
        val retirement: Deferred<Unit>,
        var expectedVersion: Long? = null,
    ) : RuntimeException()

    /**
     * Copies factories, bindings and includes so retiring one graph cannot clear another graph's
     * instances. The public module templates remain extensible before initialization.
     */
    @OptIn(KoinInternalApi::class)
    internal fun copyModules(vararg modules: Module): List<Module> {
        val copiedModules = IdentityHashMap<Module, Module>()
        val copiedFactories = IdentityHashMap<InstanceFactory<*>, InstanceFactory<*>>()

        fun copyFactory(source: InstanceFactory<*>): InstanceFactory<*> =
            copiedFactories.getOrPut(source) {
                @Suppress("UNCHECKED_CAST")
                val definition = source.beanDefinition as BeanDefinition<Any>
                when (source) {
                    is SingleInstanceFactory<*> -> SingleInstanceFactory(definition)
                    is ScopedInstanceFactory<*> -> ScopedInstanceFactory(definition, source.holdInstance)
                    is FactoryInstanceFactory<*> -> FactoryInstanceFactory(definition)
                    else -> error("Unsupported Koin instance factory: ${source::class.simpleName}")
                }
            }

        fun copyModule(source: Module): Module {
            copiedModules[source]?.let { return it }
            val copy = Module()
            copiedModules[source] = copy
            copy.scopes.addAll(source.scopes)
            source.mappings.forEach { (key, factory) -> copy.mappings[key] = copyFactory(factory) }
            source.eagerInstances.forEach { copy.prepareForCreationAtStart(copyFactory(it) as SingleInstanceFactory<*>) }
            copy.includes(source.includedModules.map(::copyModule))
            return copy
        }
        return modules.map(::copyModule)
    }
}
