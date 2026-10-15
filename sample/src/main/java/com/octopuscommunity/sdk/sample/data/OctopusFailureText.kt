package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.domain.network.OctopusResult

/**
 * The failure as the sample prints it. The two payload-less failures are plain `object`s, whose
 * `toString()` is `OctopusResult$Failure$NoNetwork@1a2b3c`: their class name is the readable part.
 */
fun OctopusResult.Failure<*>.describe(): String =
    when (this) {
        OctopusResult.Failure.NoNetwork,
        OctopusResult.Failure.ContentUnavailable,
        -> this::class.simpleName ?: toString()

        else -> toString()
    }
