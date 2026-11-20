package com.octopuscommunity.sdk.test.observer

import com.octopuscommunity.sdk.domain.event.LifecycleEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** [LifecycleEvents] that emits only the events passed to it, with no process lifecycle observer. */
class FakeLifecycleEvents : LifecycleEvents {
    private val _eventsFlow = MutableSharedFlow<LifecycleEvents.Event>()

    override fun getEventsFlow(): Flow<LifecycleEvents.Event> = _eventsFlow

    override fun emitEvent(event: LifecycleEvents.Event) {
        _eventsFlow.tryEmit(event)
    }

    override fun onOctopusScreenStarted() {
        emitEvent(LifecycleEvents.Event.OctopusStarted)
    }

    override fun onOctopusScreenStopped() {
        emitEvent(LifecycleEvents.Event.OctopusStopped)
    }
}
