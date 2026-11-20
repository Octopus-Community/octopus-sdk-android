package com.octopuscommunity.sdk

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import com.octopuscommunity.sdk.domain.repository.TrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Defers only the A/B event; the host assignment and request header remain process-local. */
internal class ClientABTestTracker(
    scope: CoroutineScope,
    private val sessionRepository: SessionRepository,
    private val trackerRepository: TrackerRepository,
    private val publishAccess: (Boolean?) -> Unit,
    lifecycleOwner: () -> LifecycleOwner,
) {
    private val lifecycle = lifecycleOwner().lifecycle
    private val lock = Any()
    private val foregroundCalls = ArrayDeque<Boolean>()
    private var backgroundPending: Boolean? = null
    private val sessionsRecovered = MutableStateFlow(false)
    private val changes = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch(Dispatchers.Main.immediate) {
            // LifecycleEvents delays AppStopped for session grace. Process visibility must still
            // gate tracking immediately, even while that app session remains open.
            combine(lifecycle.currentStateFlow, sessionRepository.getAppSessionFlow(), sessionsRecovered) { _, _, _ -> Unit }
                .collect { changes.trySend(Unit) }
        }
        scope.launch(Dispatchers.Main.immediate) {
            for (change in changes) {
                while (isActive) {
                    if (!sessionsRecovered.value) break
                    // Read the current store, not combine's cached session: recovery can complete
                    // before its stopped-session emission reaches the lifecycle collector.
                    val session = sessionRepository.getAppSession()
                    if (session == null || session.isStopped) break
                    val value =
                        synchronized(lock) {
                            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                                null
                            } else {
                                promoteBackgroundValue()
                                foregroundCalls.removeFirstOrNull()
                            }
                        } ?: break
                    // Consume before delivery. The repository owns its durable queue and retries;
                    // cancellation must not replay an already queued event in a replacement graph.
                    scope.launch { trackerRepository.trackClientSetABTest(value) }
                }
            }
        }
    }

    /** Called by the lifecycle flow's onStart, after stopStartedSessions has completed. */
    fun onSessionsRecovered() {
        sessionsRecovered.value = true
    }

    fun set(hasAccess: Boolean?) {
        synchronized(lock) {
            publishAccess(hasAccess)
            // Like origin/main, null changes only the header; it does not cancel earlier calls.
            if (hasAccess == null) return
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                promoteBackgroundValue()
                foregroundCalls.addLast(hasAccess)
            } else {
                backgroundPending = hasAccess
            }
            changes.trySend(Unit)
        }
    }

    private fun promoteBackgroundValue() {
        backgroundPending?.let { foregroundCalls.addLast(it) }
        backgroundPending = null
    }
}
