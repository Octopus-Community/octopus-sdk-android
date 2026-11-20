package com.octopuscommunity.sdk

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.CancellationException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.Properties
import java.util.UUID

/** Small, installation-scoped metadata; neither credentials nor community content belong here. */
internal class ApiKeyStore(
    private val readState: () -> State,
    private val writeState: (State) -> Unit,
    private val warn: (String) -> Unit,
    private val newStorageId: () -> String = { UUID.randomUUID().toString() },
) {
    internal data class State(
        val fingerprint: String? = null,
        val storageId: String = "",
        val pendingFingerprint: String? = null,
        val pendingStorageId: String? = null,
    )

    internal data class Prepared(
        val storageId: String,
        val changed: Boolean,
    )

    // Reuse isolated storage and retry only the tiny metadata write after a failure. In particular,
    // a repeated push initialize must not repeat a failed cleanup or abandon this run's new user.
    private var uncommitted: State? = null

    @Synchronized
    fun prepare(
        apiKey: String,
        alreadyReset: Boolean = false,
        purge: (String, String) -> Unit,
    ): Prepared {
        val fingerprint = fingerprint(apiKey)
        var selected = uncommitted?.takeIf { it.fingerprint == fingerprint }
        try {
            if (selected != null) {
                writeState(selected)
                uncommitted = null
                return Prepared(selected.storageId, true)
            }
            val stored = uncommitted ?: readState()
            val previous =
                if (stored.pendingFingerprint != null &&
                    stored.pendingStorageId != null &&
                    stored.pendingFingerprint != fingerprint
                ) {
                    State(stored.pendingFingerprint, stored.pendingStorageId)
                } else {
                    stored
                }
            // A pending transition selected fresh storage BEFORE attempting cleanup. On recovery,
            // retry its commit, not the failed cleanup: old files remain unreachable either way.
            if (previous.pendingFingerprint == fingerprint && previous.pendingStorageId != null) {
                selected = State(fingerprint, previous.pendingStorageId)
                writeState(selected)
                return Prepared(selected.storageId, true)
            }
            if (previous.fingerprint == fingerprint) return Prepared(previous.storageId, false)
            if (previous.fingerprint == null || alreadyReset) {
                selected = State(fingerprint, previous.storageId)
                writeState(selected)
                return Prepared(selected.storageId, previous.fingerprint != null)
            }
            selected = State(fingerprint, newStorageId())
            // Do not start filesystem cleanup unless its one-attempt recovery marker is durable.
            writeState(previous.copy(pendingFingerprint = fingerprint, pendingStorageId = selected.storageId))
            purge(previous.storageId, selected.storageId)
            writeState(selected)
            return Prepared(selected.storageId, true)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            val isolated = selected ?: State(fingerprint, newStorageId())
            uncommitted = isolated
            if (selected == null) {
                // A failed read has no durable recovery marker. Repair it now so the next process
                // reuses this guest's folder instead of abandoning it for another random namespace.
                try {
                    writeState(isolated)
                    uncommitted = null
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (writeFailure: Exception) {
                    warn("Unable to persist isolated community storage (${writeFailure.javaClass.simpleName})")
                }
            }
            // Exception messages may contain storage or decoded content; log the class only.
            warn("Community storage preparation failed (${failure.javaClass.simpleName}); using isolated storage")
            return Prepared(isolated.storageId, true)
        }
    }

    companion object {
        private val stores = mutableMapOf<String, ApiKeyStore>()

        /** Identifies [apiKey] without storing it: its SHA-256, in hexadecimal. */
        fun fingerprint(apiKey: String): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(apiKey.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

        fun forContext(
            context: Context,
            warn: (String) -> Unit,
        ): ApiKeyStore {
            val file = context.noBackupFilesDir.resolve("octopus_installation.properties")
            return synchronized(stores) {
                stores.getOrPut(file.absolutePath) {
                    val atomic = AtomicFile(file)
                    ApiKeyStore(
                        readState = {
                            val properties = Properties()
                            if (file.exists() || java.io.File(file.path + ".bak").exists()) {
                                atomic.openRead().use { input ->
                                    val buffer = ByteArray(4097)
                                    var size = 0
                                    while (size < buffer.size) {
                                        val count = input.read(buffer, size, buffer.size - size)
                                        if (count < 0) break
                                        size += count
                                    }
                                    if (size > 4096) throw IOException("Invalid installation metadata")
                                    properties.load(ByteArrayInputStream(buffer, 0, size))
                                    if (properties.getProperty("version") != "1" || properties.getProperty("fingerprint") == null) {
                                        throw IOException("Invalid installation metadata")
                                    }
                                    if ((properties.getProperty("pending_fingerprint") == null) !=
                                        (properties.getProperty("pending_storage_id") == null)
                                    ) {
                                        throw IOException("Invalid installation metadata")
                                    }
                                }
                            }

                            fun field(
                                name: String,
                                pattern: Regex,
                            ): String? =
                                properties.getProperty(name)?.also {
                                    if (!it.matches(pattern)) throw IOException("Invalid installation metadata")
                                }
                            val digest = Regex("[0-9a-f]{64}")
                            val namespace = Regex("[0-9a-f-]{36}")
                            State(
                                field("fingerprint", digest),
                                field("storage_id", namespace) ?: "",
                                field("pending_fingerprint", digest),
                                field("pending_storage_id", namespace),
                            )
                        },
                        writeState = { state ->
                            val properties =
                                Properties().apply {
                                    setProperty("version", "1")
                                    state.fingerprint?.let { setProperty("fingerprint", it) }
                                    state.storageId.takeIf { it.isNotEmpty() }?.let { setProperty("storage_id", it) }
                                    state.pendingFingerprint?.let { setProperty("pending_fingerprint", it) }
                                    state.pendingStorageId?.let { setProperty("pending_storage_id", it) }
                                }
                            val output = atomic.startWrite()
                            try {
                                properties.store(output, null)
                                atomic.finishWrite(output)
                            } catch (failure: Exception) {
                                atomic.failWrite(output)
                                throw failure
                            }
                        },
                        warn = warn,
                    )
                }
            }
        }
    }
}
