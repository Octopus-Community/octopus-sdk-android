package com.octopuscommunity.sdk.sample.ui.settings

import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DebugConnectionInfoTest {
    @Test
    fun `a guest shows its read-only status and exposes only the raw ID to copy`() {
        val info = debugConnectionInfo(ConnectionState.Connected(CurrentUserProfile(userId = "guest-user", isGuest = true)))

        assertEquals("Guest · read-only · guest-user", info.label)
        assertEquals("guest-user", info.userId)
    }

    @Test
    fun `a signed-in user keeps its nickname but copies only the ID`() {
        val profile = CurrentUserProfile(userId = "octopus-user", isGuest = false, nickname = "Test user")
        val info = debugConnectionInfo(ConnectionState.Connected(profile))

        assertEquals("Connected · Test user · octopus-user", info.label)
        assertEquals("octopus-user", info.userId)
        assertEquals("Connected · octopus-user", debugConnectionInfo(ConnectionState.Connected(profile.copy(nickname = ""))).label)
    }

    @Test
    fun `disconnecting removes the ID and copy action`() {
        val info = debugConnectionInfo(ConnectionState.NotConnected)

        assertEquals("Not connected", info.label)
        assertNull(info.userId)
    }

    @Test
    fun `blank IDs do not offer a copy action`() {
        listOf("", " \t").forEach { blankId ->
            val guest = debugConnectionInfo(ConnectionState.Connected(CurrentUserProfile(userId = blankId, isGuest = true)))
            val user = debugConnectionInfo(ConnectionState.Connected(CurrentUserProfile(userId = blankId, isGuest = false)))

            assertEquals("Guest · read-only", guest.label)
            assertNull(guest.userId)
            assertEquals("Connected", user.label)
            assertNull(user.userId)
        }
    }
}
