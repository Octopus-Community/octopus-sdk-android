package com.octopuscommunity.sdk.ui.common.viewmodel

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.koin.core.error.ClosedScopeException
import org.koin.core.error.InstanceCreationException

/** Applies the same container lifetime and teardown policy to feature VMs and the scope holder. */
internal fun octopusViewModelScope(parent: CoroutineScope?): CoroutineScope {
    val lifetime = SupervisorJob(parent?.coroutineContext?.get(Job))
    val handler =
        CoroutineExceptionHandler { _, failure ->
            val closed =
                failure is ClosedScopeException ||
                    (
                        failure is InstanceCreationException &&
                            generateSequence(failure.cause) { it.cause }.any { it is ClosedScopeException }
                    )
            if (lifetime.isActive || !closed) throw failure
        }
    return CoroutineScope(lifetime + Dispatchers.Main.immediate + handler)
}
