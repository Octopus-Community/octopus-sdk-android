package com.octopuscommunity.sdk.sample.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Uses the same read/write source as production, without files, actors or IO dispatchers. */
internal class InMemorySampleDataStores(
    context: Context,
) {
    val configs = SDKConfigDataStore(context, InMemoryDataStore(SDKConfigData()))
    val users = UserDataStore(context, InMemoryDataStore(UserData()))
}

private class InMemoryDataStore<T>(
    initial: T,
) : DataStore<T> {
    private val state = MutableStateFlow(initial)
    private val mutex = Mutex()
    override val data = state.asStateFlow()

    override suspend fun updateData(transform: suspend (t: T) -> T): T =
        mutex.withLock {
            transform(state.value).also { state.value = it }
        }
}
