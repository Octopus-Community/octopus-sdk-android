package com.octopuscommunity.sdk.sample.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SDKConfigResolverTest {
    @Test
    fun `short keys never appear in full`() {
        mapOf("a" to "…", "ab" to "…", "abc" to "…bc", "123456789" to "…89").forEach { (key, expected) ->
            val masked = SDKConfigResolver.maskApiKey(key)
            assertEquals(expected, masked)
            assertFalse(masked.contains(key))
        }
    }

    @Test
    fun `long keys keep only their first and last four characters`() {
        assertEquals("abcd…wxyz", SDKConfigResolver.maskApiKey("abcd12wxyz"))
        assertEquals("abcd…5678", SDKConfigResolver.maskApiKey("abcd1234efgh5678"))
    }

    @Test
    fun `blank keys show an unknown value`() {
        assertEquals("—", SDKConfigResolver.maskApiKey(""))
        assertEquals("—", SDKConfigResolver.maskApiKey(" \t\n"))
    }
}
