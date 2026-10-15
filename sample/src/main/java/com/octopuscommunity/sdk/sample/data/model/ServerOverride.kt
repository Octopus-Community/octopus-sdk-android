package com.octopuscommunity.sdk.sample.data.model

data class ServerOverride(
    val host: String,
    val port: Int,
)

fun SDKConfig.ServerEnvironment.toOverrideOrNull(): ServerOverride? =
    when (this) {
        SDKConfig.ServerEnvironment.Default -> null
        SDKConfig.ServerEnvironment.Prod ->
            ServerOverride(
                host = "api.8pus.io",
                port = 443,
            )
        is SDKConfig.ServerEnvironment.Custom ->
            ServerOverride(
                host = host,
                port = port,
            )
    }
