package com.octopuscommunity.sdk.data.local.datastore

import androidx.datastore.core.DataStore
import com.octopuscommunity.sdk.data.local.data.ClientUserData
import com.octopuscommunity.sdk.data.local.data.SecuredData
import com.octopuscommunity.sdk.data.local.data.UserData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/**
 * Transient failures retain disk and the last readable session; derived updates stay in memory
 * until restart. Explicit logout/reset deletes retained storage even while crypto is unavailable.
 * Permanent corruption is handled separately by the storage factory's replacement handler.
 */
internal class SessionPreservingDataStore(
    private val delegate: DataStore<SecuredData>,
    private val state: SecuredStorageState,
    private val forgetRetainedFile: suspend (SecuredData) -> Unit,
    private val forgetObsoleteStorage: suspend () -> Unit,
) : DataStore<SecuredData> {
    private val fallback get() = state.fallback
    private val updateMutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val data =
        fallback.flatMapLatest { inMemory ->
            if (inMemory != null) {
                flowOf(inMemory)
            } else {
                delegate.data.catch { failure ->
                    if (failure !is IOException) throw failure
                    state.markUnavailable(failure)
                }
            }
        }

    override suspend fun updateData(transform: suspend (SecuredData) -> SecuredData): SecuredData =
        updateData(transform, forgetWhen = { false })

    suspend fun clear() {
        updateData({ SecuredData() }, forgetWhen = { true })
    }

    /**
     * Forgets the member session and its client user in one write, only while [predicate] holds,
     * and deletes the retained storage as a logout does. Returns the state it replaced, or `null`.
     */
    suspend fun forgetSessionIf(predicate: (SecuredData) -> Boolean): SecuredData? {
        var forgotten: SecuredData? = null
        updateData(
            { current ->
                forgotten = current.takeIf(predicate)
                if (forgotten != null) current.copy(user = null, clientUser = null) else current
            },
            forgetWhen = { forgotten != null },
        )
        return forgotten
    }

    suspend fun updateUser(transform: (UserData?) -> UserData?): SecuredData =
        updateData { current -> current.copy(user = transform(current.user)) }

    suspend fun updateClientUser(transform: (ClientUserData?) -> ClientUserData?): SecuredData =
        updateData(
            { current ->
                val clientUser = transform(current.clientUser)
                // The repository still needs the user to delete its profile and connect a guest.
                current.copy(clientUser = clientUser)
            },
            forgetWhen = { it.clientUser == null },
        )

    private suspend fun updateData(
        transform: suspend (SecuredData) -> SecuredData,
        forgetWhen: (SecuredData) -> Boolean,
    ): SecuredData =
        updateMutex.withLock {
            suspend fun updateMemory(proposed: SecuredData): SecuredData {
                if (forgetWhen(proposed)) forgetRetainedFile(proposed) else fallback.value = proposed
                return proposed
            }
            val inMemory = fallback.value
            if (inMemory != null) return@withLock updateMemory(transform(inMemory))
            var proposed: SecuredData? = null
            var transformFailure: Throwable? = null
            try {
                val result =
                    delegate.updateData { current ->
                        if (fallback.value != null) throw SecuredStorageException("Session storage is unavailable")
                        try {
                            transform(current).also { proposed = it }
                        } catch (failure: Throwable) {
                            transformFailure = failure
                            throw failure
                        }
                    }
                if (forgetWhen(result)) forgetObsoleteStorage()
                result
            } catch (failure: IOException) {
                transformFailure?.let { throw it }
                state.markUnavailable(failure)
                // The transform may not have run if the read failed or the latch closed while waiting.
                updateMemory(proposed ?: transform(fallback.value!!))
            }
        }
}
