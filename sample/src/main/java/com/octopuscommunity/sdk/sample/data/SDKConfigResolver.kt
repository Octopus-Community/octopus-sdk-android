package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.domain.model.ConnectionMode
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.extensions.LocalExtensions

object SDKConfigResolver {
    /** Resolve the configured community, including a locally supplied override. */
    fun resolveApiKey(config: SDKConfig): String = LocalExtensions.resolve(config)?.apiKey ?: resolveDefaultApiKey(config)

    private fun resolveDefaultApiKey(config: SDKConfig): String =
        when (config.customCommunity) {
            true -> config.customApiKey ?: ""
            false ->
                when (val authKind = config.authKind) {
                    is SDKConfig.AuthKind.SSO ->
                        resolveAuthPreset(
                            forceLogin = authKind.forceLoginOnStrongActions,
                            appManagedFields = authKind.appManagedFields.toSet(),
                        ).key
                }
        }

    fun resolveConnectionMode(config: SDKConfig): ConnectionMode =
        when (val authKind = config.authKind) {
            is SDKConfig.AuthKind.SSO ->
                ConnectionMode.SSO(
                    appManagedFields = authKind.appManagedFields.toSet(),
                )
        }

    /**
     * Reapply the scenario's backend flag override after every SDK (re)initialisation, since the
     * override lives in the rebuilt SDK Koin container. On forces the flag, off clears the override
     * and leaves native profiles available because the sample also omits `onNavigateToProfile`.
     */
    fun resolveExposeClientUserIdOverride(config: SDKConfig): Boolean? = if (config.unifiedProfileEnabled) true else null

    /** Obfuscates an API key for display, including short keys: "abcd1234efgh5678" → "abcd…5678". */
    fun maskApiKey(apiKey: String): String =
        when {
            apiKey.isBlank() -> "—"
            apiKey.length <= 2 -> "…"
            apiKey.length < 10 -> "…${apiKey.takeLast(2)}"
            else -> "${apiKey.take(4)}…${apiKey.takeLast(4)}"
        }
}
