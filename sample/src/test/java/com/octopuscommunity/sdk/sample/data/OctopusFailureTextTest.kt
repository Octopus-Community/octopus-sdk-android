package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ClientUserError
import kotlin.test.Test
import kotlin.test.assertEquals

class OctopusFailureTextTest {
    @Test
    fun `typed failures display their readable message`() {
        val failure = OctopusResult.Failure.InvalidArguments(ClientUserError.MissingToken("SSO sign-in is unavailable in this build."))

        assertEquals("SSO sign-in is unavailable in this build.", failure.describe())
    }

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
