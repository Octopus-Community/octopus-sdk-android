package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.CommunityConfigPanelState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Config and Scenarios read the same effective flag, including debug overrides and host wiring. */
internal fun unifiedProfileActivation(
    community: Flow<CommunityConfigPanelState>,
    config: Flow<SDKConfig?>,
): Flow<Boolean> =
    combine(community, config) { live, persisted ->
        (live as? CommunityConfigPanelState.Loaded)?.config?.exposeClientUserId == true &&
            persisted?.effectiveHostCallbacks?.profileWired == true
    }
