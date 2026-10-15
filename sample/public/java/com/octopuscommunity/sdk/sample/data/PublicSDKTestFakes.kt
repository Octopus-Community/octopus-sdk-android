package com.octopuscommunity.sdk.sample.data

import android.content.Context

/** The prod flavor does not contain the debug fake bootstrap. */
object SDKTestFakes {
    suspend fun start(
        context: Context,
        request: SDKTestFakesRequest,
    ) = Unit
}
