package com.octopuscommunity.sdk.data.event

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.octopuscommunity.sdk.domain.event.LifecycleEvents
import com.octopuscommunity.sdk.domain.event.LifecycleEvents.Companion.APP_ALIVE_UPDATE_INTERVAL
import com.octopuscommunity.sdk.domain.event.LifecycleEvents.Event.AppAlive
import com.octopuscommunity.sdk.domain.event.LifecycleEvents.Event.AppCreated
import com.octopuscommunity.sdk.domain.event.LifecycleEvents.Event.AppStarted
import com.octopuscommunity.sdk.domain.event.LifecycleEvents.Event.AppStopped
import com.octopuscommunity.sdk.domain.event.LifecycleEvents.Event.OctopusStarted
import com.octopuscommunity.sdk.domain.event.LifecycleEvents.Event.OctopusStopped
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import com.octopuscommunity.sdk.domain.repository.TrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Default [LifecycleEvents] implementation, backed by the process lifecycle.
 *
 * The [AppAlive] ticker and the [emitEvent] emissions run on a main-thread child of [graphScope],
 * so cancelling that scope (`OctopusSDK.stop()`, and the `initialize()` / `switchCommunity()` paths
 * that call it) stops them. They stay on the main thread on purpose: [OctopusScreenSessionGate] is
 * main-only, unsynchronised state, and [emitEvent] must stay ordered against the lifecycle
 * observer's own sends, which run on the main thread too.
 */
class LifecycleEventsImpl internal constructor(
    graphScope: CoroutineScope?,
    private val lifecycleOwnerProvider: () -> LifecycleOwner,
    private val mainThreadPoster: (block: () -> Unit) -> Unit,
    private val sessionLifecycle: SessionLifecycle = SessionLifecycle(),
    private val now: () -> Long = System::currentTimeMillis,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
) : LifecycleEvents {
    /** Builds the graph-owned source, preserving sessions across brief background transitions. */
    constructor(
        graphScope: CoroutineScope,
        sessionRepository: SessionRepository,
        eventRepository: EventRepository,
        trackerRepository: TrackerRepository,
    ) : this(
        graphScope,
        { ProcessLifecycleOwner.get() },
        { block -> Handler(Looper.getMainLooper()).post(block) },
        SessionLifecycle(sessionRepository, eventRepository, trackerRepository),
    )

    /**
     * @param graphScope Scope of the SDK graph that owns this instance. When `null`, the ticker and
     * the emissions fall back to the process lifecycle scope and outlive any SDK teardown.
     * `@JvmOverloads` keeps the no-argument constructor in the binary.
     */
    @JvmOverloads
    constructor(graphScope: CoroutineScope? = null) : this(
        graphScope = graphScope,
        lifecycleOwnerProvider = { ProcessLifecycleOwner.get() },
        mainThreadPoster = { block -> Handler(Looper.getMainLooper()).post(block) },
    )

    private val mutableEvents = MutableSharedFlow<LifecycleEvents.Event>()
    private val events = mutableEvents.asSharedFlow()

    private val lifecycleOwner get() = lifecycleOwnerProvider()
    private val lifecycleScope get() = lifecycleOwner.lifecycleScope

    // Same Job as the graph scope, so its cancellation reaches the ticker; Main instead of the
    // graph's Default dispatcher, for the reasons given in the class KDoc.
    private val eventsScope: CoroutineScope =
        graphScope?.let { CoroutineScope(it.coroutineContext + Dispatchers.Main.immediate) }
            ?: lifecycleScope

    private val screenSessionGate = OctopusScreenSessionGate()

    init {
        eventsScope.launch {
            while (isActive) {
                delay(APP_ALIVE_UPDATE_INTERVAL)
                if (!screenSessionGate.isAppInBackground) {
                    emitEvent(AppAlive)
                }
            }
        }
    }

    override fun getEventsFlow() =
        transitions().let { transitions ->
            sessionLifecycle.events(transitions)
        }

    /** Closes a pending background at its captured time before reset clears the session store. */
    suspend fun flushPendingBackground() {
        sessionLifecycle.flushPendingBackground()
    }

    private fun transitions() =
        callbackFlow {
            val observer =
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        val appCreated = !screenSessionGate.isAppInBackground
                        screenSessionGate.onAppForegrounded()
                        val octopusStarted = screenSessionGate.isOctopusStarted
                        val time = now()
                        val elapsedTime = elapsedRealtime()
                        launch(Dispatchers.Main.immediate) {
                            if (appCreated) {
                                send(LifecycleTransition(AppCreated, time, elapsedTime = elapsedTime))
                            }

                            send(LifecycleTransition(AppStarted, time, octopusStarted, elapsedTime))
                            if (octopusStarted) {
                                send(LifecycleTransition(OctopusStarted, time, elapsedTime = elapsedTime))
                            }
                        }
                    }

                    override fun onStop(owner: LifecycleOwner) {
                        val time = now()
                        val elapsedTime = elapsedRealtime()
                        screenSessionGate.onAppBackgrounded()
                        launch(Dispatchers.Main.immediate) {
                            // AppStopped first lets the grace policy recognize the accompanying
                            // screen stop as a background stop rather than a navigation event.
                            send(LifecycleTransition(AppStopped, time, elapsedTime = elapsedTime))

                            if (screenSessionGate.isOctopusStarted) {
                                send(LifecycleTransition(OctopusStopped, time, elapsedTime = elapsedTime))
                            }
                        }
                    }
                }
            mainThreadPoster {
                launch(Dispatchers.Main.immediate) {
                    // Registration replays the current screen state first. Subscribe in the same
                    // main-thread turn, before a screen callback can emit a live transition: a
                    // start belongs to either the replay or the live stream, never both.
                    lifecycleOwner.lifecycle.addObserver(observer)
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        events.collect {
                            send(LifecycleTransition(it, now(), elapsedTime = elapsedRealtime()))
                        }
                    }
                }
            }
            awaitClose {
                // The process lifecycle outlives the SDK graph: without this, every collection
                // (one per initialize()) would leave an observer behind for the rest of the process.
                mainThreadPoster {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
        }

    override fun emitEvent(event: LifecycleEvents.Event) {
        eventsScope.launch {
            mutableEvents.emit(event)
        }
    }

    override fun onOctopusScreenStarted() {
        if (screenSessionGate.onScreenStarted()) {
            emitEvent(OctopusStarted)
        }
    }

    override fun onOctopusScreenStopped() {
        if (screenSessionGate.onScreenStopped()) {
            emitEvent(OctopusStopped)
        }
    }
}
