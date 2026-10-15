package com.octopuscommunity.sdk.sample.data

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SDKTestFakesLauncher {
    val isAvailable: Boolean get() = false
    val request = MutableStateFlow<SDKTestFakesRequest?>(null).asStateFlow()

    fun activate(request: SDKTestFakesRequest) = Unit

    fun consume(intent: Intent?): SDKTestFakesRequest? = null
}

data class SDKTestFakesRequest(
    val clientUserId: String?,
    val activation: Long = 0,
)
