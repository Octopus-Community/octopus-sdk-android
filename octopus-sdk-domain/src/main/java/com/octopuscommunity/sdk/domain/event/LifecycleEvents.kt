package com.octopuscommunity.sdk.domain.event

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.seconds

/** Source of the app and Octopus lifecycle events that drive the app and Octopus sessions. */
interface LifecycleEvents {
    /** A lifecycle event. */
    sealed interface Event {
        /** The app process came to the foreground for the first time. */
        data object AppCreated : Event

        /** The app came to the foreground. */
        data object AppStarted : Event

        /** The app is still in the foreground; emitted every [APP_ALIVE_UPDATE_INTERVAL]. */
        data object AppAlive : Event

        /** The app went to the background. */
        data object AppStopped : Event

        /** An Octopus screen became visible. */
        data object OctopusStarted : Event

        /** No Octopus screen is visible anymore. */
        data object OctopusStopped : Event
    }

    /** Observes the lifecycle events. */
    fun getEventsFlow(): Flow<Event>

    /** Emits [event] to the collectors of [getEventsFlow]. */
    fun emitEvent(event: Event)

    /** Called when an Octopus screen starts. Emits [Event.OctopusStarted] when it is the first visible one. */
    fun onOctopusScreenStarted()

    /** Called when an Octopus screen stops. Emits [Event.OctopusStopped] when it was the last visible one. */
    fun onOctopusScreenStopped()

    companion object {
        /** Interval between two [Event.AppAlive] events. */
        val APP_ALIVE_UPDATE_INTERVAL = 60.seconds
    }
}
