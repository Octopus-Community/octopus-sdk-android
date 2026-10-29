package com.octopuscommunity.sdk.sample.data.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class TokenProviderTest {
    @Test
    fun `missing SSO secret returns no token without invoking crypto`() {
        assertEquals("", TokenProvider.getToken(userId = "sample-test-user", secret = ""))
    }

    @Test
    fun `missing bridge secret returns no signature without invoking crypto`() {
        assertEquals("", TokenProvider.getBridgeSignature(bridgeFingerprint = "sample-test-fingerprint", secret = ""))
    }
}
