package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.model.SDKEvent
import com.octopuscommunity.sdk.domain.repository.EventRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** In-memory [EventRepository]. */
class FakeEventRepository : EventRepository {
    private val _events = MutableSharedFlow<SDKEvent>()
    override val events = _events.asSharedFlow()

    override fun tryEmitEvent(event: SDKEvent) {
        _events.tryEmit(event)
    }

    override suspend fun emitEvent(event: SDKEvent) {
        _events.emit(event)
    }
}
