package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.SDKEvent
import kotlinx.coroutines.flow.Flow

/** Bus of the [SDKEvent]s the SDK emits, observed by the host app and the SDK itself. */
interface EventRepository {
    /**
     * Flow that emits new events as they occur.
     */
    val events: Flow<SDKEvent>

    /** Returns [events]. */
    fun getEventsFlow() = events

    /** Emits [event] without suspending. The event may be dropped when it cannot be delivered at once. */
    fun tryEmitEvent(event: SDKEvent)

    /** Emits [event], suspending until it is delivered to the current collectors. */
    suspend fun emitEvent(event: SDKEvent)
}
