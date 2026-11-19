package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HostLifecycleSetupTest {
    private val config =
        SDKConfig(
            authKind = SDKConfig.AuthKind.SSO(),
            customCommunity = true,
            customApiKey = "demo-placeholder",
        )

    @Test
    fun `configured prod-style key needs no named community for presets 1 2 3 and 5`() {
        HostLifecyclePreset.entries.filter { it != HostLifecyclePreset.Switch }.forEach { preset ->
            assertNull(issue(preset, hasOtherCommunity = false))
        }
    }

    @Test
    fun `only switch needs two distinct configured communities`() {
        assertNotNull(issue(HostLifecyclePreset.Switch, hasOtherCommunity = false))
        assertNull(issue(HostLifecyclePreset.Switch, hasOtherCommunity = true))
    }

    @Test
    fun `missing configuration key or SDK initialization is a setup issue`() {
        assertNotNull(hostLifecycleSetupIssue(HostLifecyclePreset.Remount, null, "", "fixture", false, false))
        assertNotNull(hostLifecycleSetupIssue(HostLifecyclePreset.Remount, config, "", "fixture", true, false))
        assertNotNull(hostLifecycleSetupIssue(HostLifecyclePreset.Remount, config, "demo-placeholder", "fixture", false, false))
    }

    @Test
    fun `blank fixture skips only the presets that deep link to that fixture`() {
        assertNotNull(issue(HostLifecyclePreset.Remount, postId = " "))
        assertNotNull(issue(HostLifecyclePreset.Detach, postId = " "))
        assertNull(issue(HostLifecyclePreset.TabBack, postId = " "))
        assertNull(issue(HostLifecyclePreset.Restart, postId = " "))
        assertNull(issue(HostLifecyclePreset.Switch, postId = " ", hasOtherCommunity = true))
    }

    private fun issue(
        preset: HostLifecyclePreset,
        postId: String = "fixture",
        hasOtherCommunity: Boolean = false,
    ) = hostLifecycleSetupIssue(preset, config, "demo-placeholder", postId, true, hasOtherCommunity)
}
