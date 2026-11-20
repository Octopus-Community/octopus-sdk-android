package com.octopuscommunity.sdk.data.repository

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.octopuscommunity.sdk.domain.model.CommunityClosure
import com.octopuscommunity.sdk.domain.repository.CommunityClosureRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the closure in [preferences], read synchronously at construction so that the calls are blocked before the
 * first one can leave. The preferences are the community's own: one closure per API key.
 *
 * @param setCallsBlocked blocks, or allows again, every call to the server.
 * @param purge empties what the SDK cached, once, when the community closes. Safe to run again: a purge that did not end
 * (the app killed meanwhile, or a failure) is run again, by the next closed answer or the next launch.
 */
class CommunityClosureRepositoryImpl(
    private val preferences: SharedPreferences,
    private val setCallsBlocked: (Boolean) -> Unit,
    private val purge: suspend () -> Unit,
) : CommunityClosureRepository {
    private val closure = MutableStateFlow(readClosure())
    override val closureFlow: StateFlow<CommunityClosure?> = closure.asStateFlow()

    /** One answer at a time: the store, the gate and the published closure must end in agreement. */
    private val mutex = Mutex()

    init {
        setCallsBlocked(closure.value != null)
    }

    override suspend fun handle(closure: CommunityClosure?) {
        mutex.withLock {
            val wasClosed = this.closure.value != null
            if (closure == null) {
                if (!wasClosed) return
                preferences.edit(commit = true) { clear() }
                setCallsBlocked(false)
                this.closure.value = null
                return
            }
            // The closure is stored before the purge runs: marked pending in the same write, so that a purge that
            // does not end is not taken for done.
            preferences.edit(commit = true) {
                putString(KIND_KEY, closure.kind.name)
                putString(MESSAGE_KEY, closure.message)
                putLong(NEXT_CHECK_AT_KEY, closure.nextCheckAtMillis)
                if (!wasClosed) putBoolean(PURGE_PENDING_KEY, true)
            }
            setCallsBlocked(true)
            this.closure.value = closure
            runPendingPurge()
        }
    }

    override suspend fun resumeInterruptedPurge() {
        mutex.withLock { runPendingPurge() }
    }

    /** Runs the purge marked pending, if any, and clears the mark once it ended. A failure keeps it for a next try. */
    private suspend fun runPendingPurge() {
        if (!preferences.getBoolean(PURGE_PENDING_KEY, false)) return
        try {
            purge()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Purge of the closed community failed, tried again later", e)
            return
        }
        preferences.edit(commit = true) { remove(PURGE_PENDING_KEY) }
    }

    private fun readClosure(): CommunityClosure? {
        val kind =
            preferences.getString(KIND_KEY, null)?.let { name ->
                CommunityClosure.Kind.entries.firstOrNull { it.name == name }
            } ?: return null
        return CommunityClosure(
            kind = kind,
            message = preferences.getString(MESSAGE_KEY, null).orEmpty(),
            nextCheckAtMillis = preferences.getLong(NEXT_CHECK_AT_KEY, 0),
        )
    }

    companion object {
        /** Prefix of the names of the preferences, followed by the API key they belong to. */
        const val PREFERENCES_NAME = "octopus_community_closure"

        private const val KIND_KEY = "kind"
        private const val MESSAGE_KEY = "message"
        private const val NEXT_CHECK_AT_KEY = "nextCheckAtMillis"
        private const val PURGE_PENDING_KEY = "purgePending"
        private const val TAG = "CommunityClosure"
    }
}
