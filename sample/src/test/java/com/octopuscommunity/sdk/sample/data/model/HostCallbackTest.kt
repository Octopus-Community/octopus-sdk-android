package com.octopuscommunity.sdk.sample.data.model

import com.octopuscommunity.sdk.domain.model.ProfileField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HostCallbackTest {
    @Test
    fun `each callback flips its own persisted flag and no other`() {
        val allWired = SDKConfig.HostCallbacks(profileWired = true)
        HostCallback.entries.forEach { callback ->
            val flipped = callback.withWired(allWired, wired = false)
            assertFalse(callback.isWired(flipped), "$callback should read back as not wired")
            HostCallback.entries.filter { it != callback }.forEach { other ->
                assertTrue(other.isWired(flipped), "$callback must not touch $other")
            }
            assertEquals(allWired.urlHandling, flipped.urlHandling)
            assertEquals(allWired, callback.withWired(flipped, wired = true))
        }
    }

    @Test
    fun `test tag ids and URL policy tag ids are unique`() {
        val ids = HostCallback.entries.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        val urlIds = SDKConfig.UrlHandling.entries.map { it.tagId }
        assertEquals(urlIds.size, urlIds.distinct().size)
    }

    @Test
    fun `every callback names its SDK symbol`() {
        assertEquals(
            listOf(
                "onNavigateToProfile",
                "onNavigateToProfileEdit",
                "onNavigateToLogin",
                "onNavigateToClientObject",
                "onGroupAccessDenied",
            ),
            HostCallback.entries.map { it.apiName },
        )
    }

    @Test
    fun `SSO keeps the login callback wired even when stored off`() {
        val allOff = HostCallback.entries.fold(SDKConfig.HostCallbacks()) { acc, cb -> cb.withWired(acc, wired = false) }
        val config = SDKConfig(authKind = SDKConfig.AuthKind.SSO(), hostCallbacks = allOff)

        val effective = config.effectiveHostCallbacks

        assertTrue(HostCallback.Login.isWired(effective))
        assertFalse(HostCallback.ProfileEdit.isWired(effective), "no managed field: the stored off applies")
        HostCallback.entries.filter { it != HostCallback.Login }.forEach { callback ->
            assertEquals(null, callback.requiredReason(config), "$callback is optional without managed fields")
            assertFalse(callback.isWired(effective), "$callback keeps its stored off")
        }
    }

    @Test
    fun `an app-managed field keeps the profile edit callback wired`() {
        val allOff = HostCallback.entries.fold(SDKConfig.HostCallbacks()) { acc, cb -> cb.withWired(acc, wired = false) }
        val config =
            SDKConfig(
                authKind = SDKConfig.AuthKind.SSO(appManagedFields = listOf(ProfileField.NICKNAME)),
                hostCallbacks = allOff,
            )

        val effective = config.effectiveHostCallbacks

        assertTrue(HostCallback.Login.isWired(effective))
        assertTrue(HostCallback.ProfileEdit.isWired(effective))
        assertFalse(HostCallback.Profile.isWired(effective))
        assertFalse(HostCallback.ClientObject.isWired(effective))
        assertFalse(HostCallback.GroupAccessDenied.isWired(effective))
        assertEquals(allOff, config.hostCallbacks, "the stored choice is left untouched")
    }
}
