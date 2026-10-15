package com.octopuscommunity.sdk.sample.extensions

import androidx.compose.runtime.Composable
import com.octopuscommunity.sdk.sample.ui.config.ConfigViewModel
import kotlinx.serialization.json.JsonObject

/** Optional build-specific configuration controls. */
interface LocalConfigOptions {
    fun reload() = Unit

    fun keySource(state: ConfigViewModel.State): String? = null

    @Composable
    fun ServerOptions(
        state: ConfigViewModel.State,
        onKindChange: (ConfigViewModel.ServerEnvKind) -> Unit,
        onSelectionChange: (JsonObject?) -> Unit,
    ) = Unit
}

/** A validated endpoint and community override; validation belongs to its provider. */
data class LocalBackendTarget(
    val apiHost: String,
    val apiKey: String,
    val communityName: String,
    val environmentLabel: String,
    val keySourceLabel: String,
)
