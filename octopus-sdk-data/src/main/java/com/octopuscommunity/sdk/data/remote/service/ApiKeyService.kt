package com.octopuscommunity.sdk.data.remote.service

import com.octopuscommunity.ApiKeyServiceGrpcKt
import com.octopuscommunity.empty
import com.octopuscommunity.sdk.data.remote.client.OctopusGrpcClient

open class ApiKeyService(
    private val grpcClient: OctopusGrpcClient,
) {
    private val serviceStub by lazy {
        ApiKeyServiceGrpcKt.ApiKeyServiceCoroutineStub(grpcClient.unaryChannel)
    }

    /**
     * @param bypassingCallsGate whether the call is sent even though calls are blocked, to learn whether a closed
     * community has reopened.
     */
    suspend fun getConfig(bypassingCallsGate: Boolean = false) =
        if (bypassingCallsGate) {
            serviceStub.withOption(OctopusGrpcClient.BYPASS_CALLS_GATE, true).getConfig(empty {})
        } else {
            serviceStub.getConfig(empty {})
        }
}
