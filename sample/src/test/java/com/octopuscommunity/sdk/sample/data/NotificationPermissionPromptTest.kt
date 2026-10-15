package com.octopuscommunity.sdk.sample.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPermissionPromptTest {
    @Test
    fun `denial is not asked again on SDK restart or Activity recreation`() {
        val prompt = NotificationPermissionPrompt()
        assertTrue(prompt.shouldLaunch(33, permissionGranted = false))
        assertFalse(prompt.shouldLaunch(33, permissionGranted = false))
        val restored = NotificationPermissionPrompt(prompt.hasRequested)
        assertFalse(restored.shouldLaunch(33, permissionGranted = false))
        assertTrue(NotificationPermissionPrompt().shouldLaunch(33, permissionGranted = false))
    }

    @Test
    fun `older Android and granted permission need no dialog`() {
        for (api in listOf(23, 32, 33, 36)) {
            assertFalse(NotificationPermissionPrompt().shouldLaunch(api, permissionGranted = true))
        }
        assertFalse(NotificationPermissionPrompt().shouldLaunch(32, permissionGranted = false))
    }
}
