package com.octopuscommunity.sdk.sample.ui.community

import com.octopuscommunity.sdk.OctopusSDK
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression coverage for internal change's re-review finding A: the `groupAccessDenied` scenario's fire
 * counter and `registered` flag must live on [HostOverrides] — a process-level singleton — rather
 * than as `rememberSaveable` inside the scenario's own composable. The callback under test fires
 * from the Octopus screen, a different NavHost destination than the scenario screen that installs
 * it; a `rememberSaveable` var there gets disposed and orphaned before the count is ever read back.
 * A singleton has no such lifecycle, so this is exactly the part of the fix testable off-device:
 * whether the installed callback actually reaches a value both "screens" would observe.
 *
 * [HostOverrides] is a real JVM singleton shared across this whole test class (and process), so
 * every assertion below is phrased as a delta against a captured "before" value rather than an
 * absolute count, keeping the tests independent of run order.
 */
class HostOverridesGroupAccessDeniedTest {
    @Test
    fun `registering installs a counting callback that increments on every fire`() {
        HostOverrides.registerGroupAccessDeniedCallback()
        assertEquals(true, HostOverrides.groupAccessDeniedRegistered)

        val before = HostOverrides.groupAccessDeniedFireCount
        OctopusSDK.groupAccessDeniedCallback?.invoke("group-1")
        OctopusSDK.groupAccessDeniedCallback?.invoke("group-2")

        assertEquals(before + 2, HostOverrides.groupAccessDeniedFireCount)
    }

    @Test
    fun `unregistering installs a real no-op that never increments the count`() {
        HostOverrides.unregisterGroupAccessDeniedCallback()
        assertEquals(false, HostOverrides.groupAccessDeniedRegistered)

        val before = HostOverrides.groupAccessDeniedFireCount
        OctopusSDK.groupAccessDeniedCallback?.invoke("group-1")

        assertEquals(before, HostOverrides.groupAccessDeniedFireCount)
    }

    @Test
    fun `re-registering after unregistering resumes counting from where it left off`() {
        HostOverrides.unregisterGroupAccessDeniedCallback()
        OctopusSDK.groupAccessDeniedCallback?.invoke("group-ignored")
        val before = HostOverrides.groupAccessDeniedFireCount

        HostOverrides.registerGroupAccessDeniedCallback()
        assertEquals(true, HostOverrides.groupAccessDeniedRegistered)
        OctopusSDK.groupAccessDeniedCallback?.invoke("group-1")

        // The count is never reset by (un)registering — only real fires of the counting callback
        // move it, which is what makes it a trustworthy assertion target for QA.
        assertEquals(before + 1, HostOverrides.groupAccessDeniedFireCount)
    }
}
