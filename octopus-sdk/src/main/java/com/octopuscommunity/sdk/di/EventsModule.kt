package com.octopuscommunity.sdk.di

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.octopuscommunity.sdk.ClientABTestTracker
import com.octopuscommunity.sdk.data.event.LifecycleEventsImpl
import com.octopuscommunity.sdk.data.remote.client.OctopusGrpcClient
import com.octopuscommunity.sdk.data.repository.EventRepositoryImpl
import com.octopuscommunity.sdk.domain.event.LifecycleEvents
import com.octopuscommunity.sdk.domain.repository.EventRepository
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module

/**
 * Event dependencies of one SDK graph.
 *
 * @param sdkScope The graph's scope, cancelled by `OctopusSDK.stop()`. [LifecycleEventsImpl] runs
 * its `AppAlive` ticker under it so a stopped or re-initialized SDK does not keep one ticking.
 */
internal fun eventsModule(
    sdkScope: CoroutineScope,
    lifecycleOwner: () -> LifecycleOwner = { ProcessLifecycleOwner.get() },
) = module {
    single(createdAtStart = true) {
        val client = get<OctopusGrpcClient>()
        ClientABTestTracker(
            scope = sdkScope,
            sessionRepository = get(),
            trackerRepository = get(),
            publishAccess = { client.hasAccessToCommunity = it },
            lifecycleOwner = lifecycleOwner,
        )
    }
    single<EventRepository> {
        EventRepositoryImpl()
    }
    single<LifecycleEvents> {
        LifecycleEventsImpl(
            graphScope = sdkScope,
            sessionRepository = get(),
            eventRepository = get(),
            trackerRepository = get(),
        )
    }
}
