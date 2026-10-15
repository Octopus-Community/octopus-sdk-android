package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.CommunityConfigPanelState
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.unifiedProfileEffectLine
import com.octopuscommunity.sdk.test.mock.MockCommunityConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UnifiedProfileActivationTest {
    @Test
    fun `Force flag off shows native profiles even with the scenario switch on`() =
        runTest {
            val enabled = SDKConfig(authKind = SDKConfig.AuthKind.SSO()).withUnifiedProfile(true)
            val config = MutableStateFlow<SDKConfig?>(enabled)
            val community =
                MutableStateFlow<CommunityConfigPanelState>(
                    CommunityConfigPanelState.Loaded(MockCommunityConfig.default.copy(exposeClientUserId = true)),
                )
            var active = false
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                unifiedProfileActivation(community, config).collect { active = it }
            }
            assertTrue(active)
            assertEquals("Profile taps open Client profile", unifiedProfileEffectLine(active))

            community.value = CommunityConfigPanelState.Loaded(MockCommunityConfig.default.copy(exposeClientUserId = false))
            assertTrue(config.value!!.unifiedProfileEnabled)
            assertFalse(active)
            assertEquals("Profile taps open native profiles", unifiedProfileEffectLine(active))

            community.value = CommunityConfigPanelState.Loaded(MockCommunityConfig.default.copy(exposeClientUserId = true))
            assertTrue(active)
            config.value = enabled.copy(hostCallbacks = enabled.hostCallbacks.copy(profileWired = false))
            assertFalse(active)
            config.value = enabled
            assertTrue(active)
            community.value = CommunityConfigPanelState.NotInitialised
            assertFalse(active)
        }
}
