package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.sdk.data.local.datastore.SecuredDataStore
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.WeakHashMap

/** Serializes root-feed cache commits with session replacement, without locking network calls. */
internal class RootFeedSession {
    val writes = Mutex()
    private var generation = 0L

    // Called while writes is held, before changing the session or purging its content.
    fun invalidate() {
        generation++
    }

    suspend fun snapshot(): Long = writes.withLock { generation }

    suspend fun commit(
        generation: Long,
        write: suspend () -> Unit,
    ) {
        writes.withLock {
            currentCoroutineContext().ensureActive()
            if (generation == this.generation) write()
        }
    }
}

// User and connection repositories share a store, but are separate instances in production DI.
// Weak keys keep this per-installation coordination from retaining a destroyed SDK container.
private val rootFeedSessions = WeakHashMap<SecuredDataStore, RootFeedSession>()
internal val SecuredDataStore.rootFeedSession: RootFeedSession
    get() = synchronized(rootFeedSessions) { rootFeedSessions.getOrPut(this) { RootFeedSession() } }
