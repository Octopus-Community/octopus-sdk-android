package com.octopuscommunity.sdk.data.local.datastore

import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.ReadScope
import androidx.datastore.core.Serializer
import androidx.datastore.core.Storage
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.WriteScope
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import com.octopuscommunity.sdk.data.local.data.SecuredData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File
import java.io.IOException

internal class SecuredStorageState(
    private val logFailure: (Throwable) -> Unit = {},
) {
    val fallback = MutableStateFlow<SecuredData?>(null)
    val transactionMutex = Mutex()

    @Volatile
    var lastRead: SecuredData? = null

    fun markUnavailable(failure: IOException) {
        if (fallback.compareAndSet(null, lastRead ?: SecuredData())) logFailure(failure)
    }
}

/** Closes the write gate before a failed read releases the whole storage transaction lock. */
private class SessionPreservingStorage(
    private val delegate: Storage<SecuredData>,
    private val state: SecuredStorageState,
    private val moveUnreadableFile: () -> Unit,
) : Storage<SecuredData> {
    private fun handleFailure(failure: IOException) {
        if (failure is CorruptionException) {
            if (state.fallback.value != null) throw SecuredStorageException("Session storage is unavailable")
            try {
                // Called under the transaction mutex, after the backend closes the read scope.
                moveUnreadableFile()
            } catch (moveFailure: IOException) {
                state.markUnavailable(moveFailure)
                throw moveFailure
            }
        } else {
            state.markUnavailable(failure)
        }
    }

    override fun createConnection(): StorageConnection<SecuredData> {
        val connection =
            try {
                delegate.createConnection()
            } catch (failure: IOException) {
                state.markUnavailable(failure)
                throw failure
            }
        return object : StorageConnection<SecuredData> by connection {
            override suspend fun <R> readScope(block: suspend ReadScope<SecuredData>.(Boolean) -> R): R =
                state.transactionMutex.withLock {
                    try {
                        connection.readScope { locked ->
                            val scope = this
                            val rememberingScope =
                                object : ReadScope<SecuredData> by scope {
                                    override suspend fun readData() = scope.readData().also { state.lastRead = it }
                                }
                            rememberingScope.block(locked)
                        }
                    } catch (failure: IOException) {
                        handleFailure(failure)
                        throw failure
                    }
                }

            override suspend fun writeScope(block: suspend WriteScope<SecuredData>.() -> Unit) {
                state.transactionMutex.withLock {
                    if (state.fallback.value != null) throw SecuredStorageException("Session storage is unavailable")
                    var written: SecuredData? = null
                    try {
                        connection.writeScope {
                            val scope = this
                            val rememberingScope =
                                object : WriteScope<SecuredData> by scope {
                                    override suspend fun readData() = scope.readData().also { state.lastRead = it }

                                    override suspend fun writeData(value: SecuredData) {
                                        scope.writeData(value)
                                        written = value
                                    }
                                }
                            rememberingScope.block()
                        }
                        written?.let { state.lastRead = it }
                    } catch (failure: IOException) {
                        handleFailure(failure)
                        throw failure
                    }
                }
            }
        }
    }
}

/** Uses the same Okio backend and serializer bridge as AndroidX's Context.dataStore delegate. */
internal fun sessionPreservingDataStore(
    serializer: Serializer<SecuredData>,
    scope: CoroutineScope,
    migrations: List<DataMigration<SecuredData>> = emptyList(),
    state: SecuredStorageState = SecuredStorageState(::logSecuredStorageFailure),
    forgetLegacyPreferences: () -> Unit = {},
    logLegacyForgetFailure: (Throwable) -> Unit = ::logLegacyForgetFailure,
    produceFile: () -> File,
): SessionPreservingDataStore {
    val path by lazy { produceFile().absolutePath.toPath() }
    val unreadablePath by lazy { (path.toString() + ".unreadable").toPath() }

    fun forgetLegacyPreferencesBestEffort() {
        try {
            forgetLegacyPreferences()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            logLegacyForgetFailure(failure)
        }
    }
    val storage =
        OkioStorage(
            FileSystem.SYSTEM,
            object : OkioSerializer<SecuredData> {
                override val defaultValue get() = serializer.defaultValue

                override suspend fun readFrom(source: BufferedSource) = serializer.readFrom(source.inputStream())

                override suspend fun writeTo(
                    t: SecuredData,
                    sink: BufferedSink,
                ) = serializer.writeTo(t, sink.outputStream())
            },
        ) { path }
    return SessionPreservingDataStore(
        delegate =
            DataStoreFactory.create(
                storage =
                    SessionPreservingStorage(storage, state) {
                        // Keep one copy, overwriting an older unreadable backup.
                        if (FileSystem.SYSTEM.exists(path)) FileSystem.SYSTEM.atomicMove(path, unreadablePath)
                    },
                migrations = migrations,
                scope = scope,
                corruptionHandler = ReplaceFileCorruptionHandler { serializer.defaultValue },
            ),
        state = state,
        forgetRetainedFile = { value ->
            withContext(Dispatchers.IO) {
                state.transactionMutex.withLock {
                    FileSystem.SYSTEM.delete(path, mustExist = false)
                    FileSystem.SYSTEM.delete(unreadablePath, mustExist = false)
                    state.lastRead = value
                    state.fallback.value = value
                    forgetLegacyPreferencesBestEffort()
                }
            }
        },
        forgetObsoleteStorage = {
            withContext(Dispatchers.IO) {
                state.transactionMutex.withLock {
                    FileSystem.SYSTEM.delete(unreadablePath, mustExist = false)
                    forgetLegacyPreferencesBestEffort()
                }
            }
        },
    )
}

internal fun clearExistingLegacyPreferences(
    preferencesFile: File,
    clearPreferences: () -> Boolean,
) {
    // Committing a clear can create an empty XML file even if no legacy preferences existed.
    val backup = File(preferencesFile.path + ".bak")
    if (!preferencesFile.exists() && !backup.exists()) return
    try {
        if (!clearPreferences()) throw SecuredStorageException("Unable to forget legacy secured preferences")
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        // A failed commit must not leave an old identity available to the next launch's migration.
        preferencesFile.delete()
        backup.delete()
        throw failure // The caller logs this best-effort cleanup failure without failing logout.
    }
}

private fun logLegacyForgetFailure(failure: Throwable) {
    Log.w("OctopusSecuredStorage", "Unable to forget legacy secured preferences (${failure.javaClass.simpleName})")
}

internal fun logSecuredStorageFailure(failure: Throwable) {
    // Serializer messages are fixed and never include decoded content or authentication tokens.
    Log.w(
        "OctopusSecuredStorage",
        "Session storage unavailable; updates remain in memory (${(failure.cause ?: failure).javaClass.simpleName}: ${failure.message})",
    )
}
