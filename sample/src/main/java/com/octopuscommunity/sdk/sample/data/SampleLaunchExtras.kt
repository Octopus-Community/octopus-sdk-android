package com.octopuscommunity.sdk.sample.data

import android.content.Intent
import android.os.Bundle
import com.octopuscommunity.sdk.sample.data.datastore.SDKConfigDataStore
import com.octopuscommunity.sdk.sample.extensions.LocalExtensions
import kotlinx.coroutines.CoroutineScope

/** QA activation belongs to a fresh intent, never an activity or Recents restoration. */
internal fun consumeSampleLaunchExtras(
    intent: Intent?,
    savedInstanceState: Bundle?,
    store: SDKConfigDataStore,
    scope: CoroutineScope,
    activateFakes: (SDKTestFakesRequest) -> Unit = SDKTestFakesLauncher::activate,
) {
    if (intent == null || savedInstanceState != null || intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
    // Fake activation is process-only: do not write the backend config or host user.
    SDKTestFakesLauncher.consume(intent)?.let(activateFakes)
    LocalExtensions.consumeLaunchIntent(intent, store, scope)
}
