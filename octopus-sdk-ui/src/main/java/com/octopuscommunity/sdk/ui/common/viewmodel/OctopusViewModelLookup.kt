package com.octopuscommunity.sdk.ui.common.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.octopuscommunity.sdk.ui.components.LocalOctopusGeneration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.currentKoinScope
import org.koin.compose.getKoin
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

/** Keeps host-owned ViewModel stores from returning a previous SDK generation's instances. */
@Composable
internal inline fun <reified VM : OctopusViewModel> octopusViewModel(
    qualifier: Qualifier? = null,
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current),
    key: String? = null,
    noinline parameters: ParametersDefinition? = null,
): VM {
    val generation = LocalOctopusGeneration.current
    val parent =
        currentKoinScope().getOrNull<com.octopuscommunity.sdk.ui.components.OctopusHostCoroutineScope>()?.scope
            ?: getKoin().getProperty<CoroutineScope>("octopusViewModelScope")
    val retained =
        remember(viewModelStoreOwner, generation) {
            ViewModelProvider(
                viewModelStoreOwner,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = OctopusViewModelStore(parent) as T
                },
            )["octopusViewModels:$generation", OctopusViewModelStore::class.java]
        }
    remember(retained) { OctopusViewModelStoreLease(retained) }
    return koinViewModel(
        qualifier = qualifier,
        viewModelStoreOwner = retained,
        key = "octopus:${LocalOctopusGeneration.current}:${VM::class.qualifiedName}:${key ?: qualifier?.value.orEmpty()}",
        parameters = parameters,
    )
}

/** SDK work survives an owner's clear until its outgoing composition has left. */
internal class OctopusViewModelStore(
    parent: CoroutineScope?,
) : ViewModel(octopusViewModelScope(parent)),
    ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
    private var readers = 0
    private var cleared = false
    private var retired = false

    init {
        viewModelScope.coroutineContext[Job]?.invokeOnCompletion {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                retired = true
                closeIfUnused()
            }
        }
    }

    fun acquire() {
        readers++
    }

    fun release() {
        readers--
        closeIfUnused()
    }

    override fun onCleared() {
        cleared = true
        closeIfUnused()
    }

    private fun closeIfUnused() {
        if ((cleared || retired) && readers == 0) viewModelStore.clear()
    }
}

internal class OctopusViewModelStoreLease(
    private val store: OctopusViewModelStore,
) : RememberObserver {
    init {
        store.acquire()
    }

    private var released = false

    override fun onRemembered() = Unit

    override fun onForgotten() = release()

    override fun onAbandoned() = release()

    private fun release() {
        if (!released) {
            released = true
            store.release()
        }
    }
}
