package com.octopuscommunity.sdk.data.remote.service

import com.octopuscommunity.TrackingServiceGrpcKt.TrackingServiceCoroutineStub
import com.octopuscommunity.TrackingServiceProto.TrackRequest
import com.octopuscommunity.sdk.data.remote.client.OctopusGrpcClient
import com.octopuscommunity.trackRequest

open class TrackingService(
    private val grpcClient: OctopusGrpcClient
) {
    private val serviceStub by lazy {
        TrackingServiceCoroutineStub(grpcClient.unaryChannel)
    }

    suspend fun track(events: List<TrackRequest.Event>) = serviceStub.track(
        trackRequest {
            this.events += events
        }
    )
}