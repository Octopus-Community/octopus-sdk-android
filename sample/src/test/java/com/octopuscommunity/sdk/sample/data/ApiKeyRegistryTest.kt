package com.octopuscommunity.sdk.sample.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the named-key wire parser (`OCTOPUS_NAMED_API_KEYS`),
 * exercised with synthetic wire strings — iso the Flutter example's
 * named-key parser tests.
 */
class ApiKeyRegistryTest {
    @Test
    fun `prod build never embeds named demo keys`() {
        if (com.octopuscommunity.sdk.sample.BuildConfig.IS_PROD_FLAVOR) {
            assertTrue(
                com.octopuscommunity.sdk.sample.BuildConfig.OCTOPUS_NAMED_API_KEYS
                    .isEmpty(),
                "Prod must not embed named demo keys",
            )
        }
    }

    @Test
    fun `empty or whitespace-only input yields no keys`() {
        assertTrue(parseNamedApiKeys("").isEmpty())
        assertTrue(parseNamedApiKeys("   ").isEmpty())
        assertTrue(parseNamedApiKeys(";;").isEmpty())
    }

    @Test
    fun `parses a single id-label-key entry`() {
        val keys = parseNamedApiKeys("slotA~Human label A~AbC123")
        assertEquals(listOf(InjectedApiKey("slotA", "Human label A", "AbC123")), keys)
    }

    @Test
    fun `parses multiple entries in order`() {
        val keys = parseNamedApiKeys("a~Label A~key-a;b~Label B~key_b;c~Label C~keyC")
        assertEquals(listOf("a", "b", "c"), keys.map { it.id })
        assertEquals(listOf("Label A", "Label B", "Label C"), keys.map { it.label })
        assertEquals(listOf("key-a", "key_b", "keyC"), keys.map { it.key })
    }

    @Test
    fun `preserves middots and spaces inside labels`() {
        val keys = parseNamedApiKeys("id~Alpha · beta gamma · delta~kkkk")
        assertEquals("Alpha · beta gamma · delta", keys.single().label)
    }

    @Test
    fun `drops trailing empty or whitespace entries`() {
        val keys = parseNamedApiKeys("a~A~ka; ;b~B~kb;")
        assertEquals(listOf("a", "b"), keys.map { it.id })
    }

    @Test
    fun `trims surrounding whitespace on each field`() {
        val keys = parseNamedApiKeys("  id  ~  My label  ~  thekey  ")
        assertEquals(InjectedApiKey("id", "My label", "thekey"), keys.single())
    }

    @Test
    fun `drops malformed entries with wrong field count`() {
        val keys = parseNamedApiKeys("bad~onlytwo;ok~Good~key;too~many~fields~here")
        assertEquals("ok", keys.single().id)
    }

    @Test
    fun `drops entries with an empty id or empty key`() {
        assertTrue(parseNamedApiKeys("~Label~key").isEmpty())
        assertTrue(parseNamedApiKeys("id~Label~").isEmpty())
    }

    @Test
    fun `allows an empty label kept as empty string`() {
        val keys = parseNamedApiKeys("id~~thekey")
        assertEquals("", keys.single().label)
        assertEquals("thekey", keys.single().key)
    }
}
