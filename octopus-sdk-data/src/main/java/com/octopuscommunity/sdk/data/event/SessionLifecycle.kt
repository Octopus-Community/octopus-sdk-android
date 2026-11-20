package com.octopuscommunity.sdk.data.event

import com.octopuscommunity.sdk.domain.event.LifecycleEvents.Event
import com.octopuscommunity.sdk.domain.model.Sessions.Session
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import com.octopuscommunity.sdk.domain.repository.TrackerRepository
import com.octopuscommunity.sdk.domain.usecase.StopAppSessionUseCase
import com.octopuscommunity.sdk.domain.usecase.StopOctopusSessionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal val APP_SESSION_BACKGROUND_GRACE_PERIOD = 10.seconds

/** Timestamped at the observer, before a slow session collector can delay delivery. */
internal data class LifecycleTransition(
    val event: Event,
    val time: Long,
    val octopusVisible: Boolean = false,
    val elapsedTime: Long = 0L,
)

/** Serializes foreground transitions, expiry and teardown without changing LifecycleEvents' ABI. */
internal class SessionLifecycle(
    private val sessionRepository: SessionRepository? = null,
    private val eventRepository: EventRepository? = null,
    private val trackerRepository: TrackerRepository? = null,
    private val gracePeriod: Duration = APP_SESSION_BACKGROUND_GRACE_PERIOD,
) {
    private val activeInputs = AtomicReference<Channel<Input>?>(null)

    /** Finishes an observed background before reset can discard its stored sessions. */
    suspend fun flushPendingBackground() {
        val inputs = activeInputs.get() ?: return
        val completed = CompletableDeferred<Unit>()
        inputs.send(Input.Flush(completed))
        completed.await()
    }

    private class Background(
        val transition: LifecycleTransition,
    ) {
        var appId: String? = null
        var octopusId: String? = null
    }

    private fun stoppingRepository(background: Background): SessionRepository {
        val sessionRepository = requireNotNull(sessionRepository)
        return object : SessionRepository by sessionRepository {
            override suspend fun updateAppSession(transform: suspend (Session.App?) -> Session.App?) =
                sessionRepository.updateAppSession {
                    if (it != null && it.id == background.appId) transform(it) else it
                }

            override suspend fun updateOctopusSession(transform: suspend (Session.App?, Session.Octopus?) -> Session.Octopus?) =
                sessionRepository.updateOctopusSession { app, octopus ->
                    if (octopus != null && octopus.id == background.octopusId) transform(app, octopus) else octopus
                }
        }
    }

    private suspend fun stopOctopus(background: Background) {
        if (sessionRepository == null) return
        StopOctopusSessionUseCase(
            stoppingRepository(background),
            requireNotNull(eventRepository),
            requireNotNull(trackerRepository),
        )(background.transition.time)
    }

    private sealed interface Input {
        data class Flush(
            val completed: CompletableDeferred<Unit>,
        ) : Input

        data class Transition(
            val value: LifecycleTransition,
        ) : Input

        data class Backgrounded(
            val background: Background,
        ) : Input

        data class Expired(
            val background: Background,
        ) : Input
    }

    fun events(transitions: Flow<LifecycleTransition>): Flow<Event> =
        flow {
            coroutineScope {
                val inputs =
                    Channel<Input>(Channel.UNLIMITED, onUndeliveredElement = {
                        if (it is Input.Flush) it.completed.cancel(CancellationException("Session collector closed"))
                    })
                activeInputs.set(inputs)
                val observedBackground = AtomicReference<Background?>(null)
                val producer =
                    launch {
                        var pending: Background? = null
                        var expiry: Job? = null
                        try {
                            transitions.collect { transition ->
                                when (transition.event) {
                                    Event.AppStopped -> {
                                        if (pending == null) {
                                            val next = Background(transition)
                                            pending = next
                                            observedBackground.set(next)
                                            inputs.send(Input.Backgrounded(next))
                                            // Start timing upstream of session work. Slow access or
                                            // storage calls must not extend the ten-second window.
                                            expiry =
                                                launch {
                                                    delay(gracePeriod)
                                                    inputs.send(Input.Expired(next))
                                                }
                                        }
                                    }
                                    Event.AppStarted -> {
                                        expiry?.cancel()
                                        pending = null
                                        observedBackground.set(null)
                                        inputs.send(Input.Transition(transition))
                                    }
                                    else -> inputs.send(Input.Transition(transition))
                                }
                            }
                        } finally {
                            expiry?.cancel()
                        }
                    }
                var background: Background? = null
                var octopusRunning = false
                var ownedAppId: String? = null

                suspend fun finishBackground(pending: Background) {
                    // Persist/journal the captured bounds before publishing lifecycle events. The
                    // ordinary collector's repeated stops are idempotent (including internal change's end).
                    // A replacement graph may already have started a new session while this
                    // cancellation flush is finishing. Only close the ids owned by this graph.
                    stopOctopus(pending)
                    if (sessionRepository != null) {
                        StopAppSessionUseCase(
                            stoppingRepository(pending),
                            requireNotNull(eventRepository),
                            requireNotNull(trackerRepository),
                        )(pending.transition.time)
                    }
                }

                suspend fun expireBackground(pending: Background) {
                    finishBackground(pending)
                    background = null
                    observedBackground.compareAndSet(pending, null)
                    if (octopusRunning) emit(Event.OctopusStopped)
                    octopusRunning = false
                    emit(Event.AppStopped)
                }

                suspend fun startApp() {
                    emit(Event.AppStarted)
                    ownedAppId = sessionRepository?.getAppSession()?.id
                }

                try {
                    for (input in inputs) {
                        when (input) {
                            is Input.Flush -> {
                                try {
                                    background?.let { expireBackground(it) }
                                    input.completed.complete(Unit)
                                } catch (failure: Throwable) {
                                    input.completed.completeExceptionally(failure)
                                    throw failure
                                }
                            }
                            is Input.Expired -> {
                                if (background !== input.background) continue
                                expireBackground(input.background)
                            }
                            is Input.Backgrounded -> {
                                val pending = input.background
                                background = pending
                                // Reuse the persisted last-alive fields for process-death
                                // recovery; no pending-stop field or Room migration needed.
                                sessionRepository?.updateAppSession {
                                    it?.also { pending.appId = it.id }
                                    if (it == null || it.isStopped) it else it.copy(lastAliveTime = pending.transition.time)
                                }
                                sessionRepository?.updateOctopusSession { _, session ->
                                    session?.also { pending.octopusId = it.id }
                                    if (session == null ||
                                        session.isStopped
                                    ) {
                                        session
                                    } else {
                                        session.copy(lastAliveTime = pending.transition.time)
                                    }
                                }
                            }
                            is Input.Transition -> {
                                val transition = input.value
                                when (transition.event) {
                                    Event.AppStopped -> Unit // Delivered as Backgrounded above.
                                    Event.AppStarted -> {
                                        val pending = background
                                        if (pending == null) {
                                            startApp()
                                        } else {
                                            val app = sessionRepository?.getAppSession()
                                            val canResumeApp =
                                                sessionRepository == null ||
                                                    (app != null && !app.isStopped && app.id == pending.appId)
                                            // elapsedRealtime includes device sleep. delay() alone
                                            // can still be pending when the process thaws hours later.
                                            val elapsed = (transition.elapsedTime - pending.transition.elapsedTime).milliseconds
                                            if (elapsed >= gracePeriod || !canResumeApp) {
                                                expireBackground(pending)
                                                startApp()
                                                continue
                                            }
                                            background = null
                                            val octopus = sessionRepository?.getOctopusSession()
                                            val canResumeOctopus =
                                                sessionRepository == null ||
                                                    (
                                                        octopus != null &&
                                                            !octopus.isStopped &&
                                                            octopus.id == pending.octopusId &&
                                                            octopus.appSessionId == pending.appId
                                                    )
                                            if (!canResumeOctopus) octopusRunning = false
                                            // A screen may have been disposed while backgrounded.
                                            if (octopusRunning && !transition.octopusVisible) {
                                                stopOctopus(pending)
                                                octopusRunning = false
                                                emit(Event.OctopusStopped)
                                            }
                                        }
                                    }
                                    Event.OctopusStopped -> {
                                        if (background == null) {
                                            octopusRunning = false
                                            emit(Event.OctopusStopped)
                                        }
                                    }
                                    Event.OctopusStarted -> {
                                        if (!octopusRunning) {
                                            octopusRunning = true
                                            emit(Event.OctopusStarted)
                                        }
                                    }
                                    Event.AppAlive -> if (background == null) emit(Event.AppAlive)
                                    Event.AppCreated -> emit(Event.AppCreated)
                                }
                            }
                        }
                    }
                } finally {
                    activeInputs.compareAndSet(inputs, null)
                    producer.cancel()
                    inputs.cancel()
                    // Koin closes Room immediately after cancelling the graph. Flush only the
                    // checkpoints here: stopStartedSessions() closes both records at those times
                    // with the next graph's tracker, including played-video analytics. Calling
                    // either stop use case here could access a closed Room database. Stopping
                    // records without their use cases would lose tracking side effects.
                    // A slow downstream operation may still be processing a foreground event
                    // when cancellation arrives. The upstream observer has already captured its
                    // onStop, even if Backgrounded has not reached the reducer yet.
                    observedBackground.get()?.let { pending ->
                        pending.appId = pending.appId ?: ownedAppId
                        withContext(NonCancellable) {
                            if (sessionRepository != null) {
                                stoppingRepository(pending).updateAppSession {
                                    if (it == null || it.isStopped) it else it.copy(lastAliveTime = pending.transition.time)
                                }
                                sessionRepository.updateOctopusSession { _, session ->
                                    if (session != null &&
                                        !session.isStopped &&
                                        session.appSessionId == pending.appId
                                    ) {
                                        session.copy(lastAliveTime = pending.transition.time)
                                    } else {
                                        session
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}
