package com.octopuscommunity.sdk.sample.extensions

import android.content.Intent
import androidx.compose.runtime.Composable
import com.octopuscommunity.sdk.sample.data.datastore.SDKConfigDataStore
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import kotlinx.coroutines.CoroutineScope

/** Default implementation for the standalone sample. */
object LocalExtensions {
    val designReferenceUrl: String = ""

    fun resolve(config: SDKConfig): LocalBackendTarget? = null

    fun configOptions(scope: CoroutineScope): LocalConfigOptions = object : LocalConfigOptions {}

    fun consumeLaunchIntent(
        intent: Intent?,
        store: SDKConfigDataStore,
        scope: CoroutineScope,
    ) = Unit

    fun shouldWarn(serverEnvironment: SDKConfig.ServerEnvironment): Boolean = false

    @Composable
    fun WarningBanner(serverEnvironment: SDKConfig.ServerEnvironment) = Unit

    @Composable
    fun FeedbackRow(onClick: () -> Unit) = Unit
}
