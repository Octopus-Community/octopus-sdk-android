package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.domain.network.OctopusResult
import kotlin.test.Test
import kotlin.test.assertEquals

class OctopusFailureTextTest {
    @Test
    fun `payload-less failures print their name`() {
        assertEquals("NoNetwork", OctopusResult.Failure.NoNetwork.describe())
        assertEquals("ContentUnavailable", OctopusResult.Failure.ContentUnavailable.describe())
    }

    @Test
    fun `data class failures keep their generated text`() {
        val failure = OctopusResult.Failure.UserNotAuthenticated(reason = "no session")

        assertEquals(failure.toString(), failure.describe())
    }
}
