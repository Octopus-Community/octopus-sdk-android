package com.octopuscommunity.sdk.sample.ui.config

import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import kotlin.test.Test
import kotlin.test.assertTrue

/** Apply must always say what it did: restart, no restart, or nothing to apply. */
class ApplyOutcomeMessageTest {
    private val stored =
        SDKConfig(
            authKind = SDKConfig.AuthKind.SSO(),
            customCommunity = true,
            customApiKey = "abcd1234efgh5678",
        )

    @Test
    fun `user-only Apply in fakes mode never promises a host connection`() {
        val message = applyOutcomeMessage(stored, stored, userChanged = true, testFakesActive = true)
        assertTrue(message.contains("test fakes"))
        assertTrue(!message.contains("connecting"))
    }

    @Test
    fun `onboarding says the SDK is starting`() {
        val message = applyOutcomeMessage(persisted = null, next = stored, userChanged = false)
        assertTrue(message.startsWith("Starting the SDK"), message)
    }

    @Test
    fun `a re-init key change says the SDK restarts, naming the masked target`() {
        val next = stored.copy(customApiKey = "wxyz1234efgh9999")
        val message = applyOutcomeMessage(persisted = stored, next = next, userChanged = false)
        assertTrue(message.contains("restarting the SDK"), message)
        assertTrue(message.contains("wxyz…9999"), message)
        assertTrue(!message.contains("wxyz1234efgh9999"), "the raw key must never be shown")
    }

    @Test
    fun `with SDK test fakes active a re-init key change never announces a restart`() {
        val next = stored.copy(customApiKey = "wxyz1234efgh9999")
        val message = applyOutcomeMessage(persisted = stored, next = next, userChanged = false, testFakesActive = true)
        assertTrue(!message.contains("restarting"), message)
        assertTrue(message.contains("test fakes"), message)
    }

    @Test
    fun `a theme-only change applies without a restart`() {
        val next = stored.copy(appearance = SDKConfig.Appearance.Dark)
        val message = applyOutcomeMessage(persisted = stored, next = next, userChanged = false)
        assertTrue(message.contains("no SDK restart needed"), message)
    }

    @Test
    fun `a user-only change says the new user is connected`() {
        val message = applyOutcomeMessage(persisted = stored, next = stored, userChanged = true)
        assertTrue(message.contains("connecting the new user"), message)
    }

    @Test
    fun `an unchanged config says there is nothing to apply`() {
        val message = applyOutcomeMessage(persisted = stored, next = stored, userChanged = false)
        assertTrue(message.startsWith("Nothing to apply"), message)
    }
}
