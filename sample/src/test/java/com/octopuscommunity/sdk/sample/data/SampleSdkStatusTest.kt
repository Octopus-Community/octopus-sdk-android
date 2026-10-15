package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SampleSdkStatusTest {
    private val guest = ConnectionState.Connected(CurrentUserProfile(userId = "g", isGuest = true))
    private val user = ConnectionState.Connected(CurrentUserProfile(userId = "u", isGuest = false))

    @Test
    fun `an SDK that is not started wins over every other signal`() {
        assertEquals(SampleSdkStatus.NotStarted, SampleSdkStatus.of(false, user, isOnline = false))
        assertEquals(SampleSdkStatus.NotStarted, SampleSdkStatus.of(false, user, isOnline = true))
    }

    @Test
    fun `a started SDK on an offline device is unreachable, even with a cached session`() {
        assertEquals(SampleSdkStatus.Unreachable, SampleSdkStatus.of(true, user, isOnline = false))
    }

    @Test
    fun `an online SDK reads its session`() {
        assertEquals(
            SampleSdkStatus.NoSession,
            SampleSdkStatus.of(true, ConnectionState.NotConnected, isOnline = true),
        )
        assertEquals(SampleSdkStatus.Guest, SampleSdkStatus.of(true, guest, isOnline = true))
        assertEquals(SampleSdkStatus.SignedIn, SampleSdkStatus.of(true, user, isOnline = true))
    }

    @Test
    fun `only a signed-in user needs no attention`() {
        SampleSdkStatus.entries.forEach {
            if (it == SampleSdkStatus.SignedIn) assertFalse(it.needsAttention) else assertTrue(it.needsAttention)
        }
    }
}
