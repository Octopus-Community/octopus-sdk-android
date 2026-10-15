package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.sample.data.datastore.SDKConfigData
import com.octopuscommunity.sdk.sample.data.datastore.decodeSDKConfig
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SDKConfigLegacyAuthTest {
    @Test
    fun `persisted Octopus auth resets the sample to defaults`() {
        val legacy = """{"hasData":true,"config":{"authKind":{
            "type":"com.octopuscommunity.sdk.sample.data.model.SDKConfig.AuthKind.Octopus"}}}"""
        assertEquals(SDKConfigData(), decodeSDKConfig(legacy))
    }

    @Test
    fun `SSO configuration keeps decoding with unknown legacy fields`() {
        val stored = SDKConfigData(hasData = true, config = SDKConfig(authKind = SDKConfig.AuthKind.SSO()))
        val encoded = Json.encodeToString(SDKConfigData.serializer(), stored)
        val decoded = decodeSDKConfig(encoded.dropLast(1) + ",\"legacyField\":true}")
        assertTrue(decoded.hasData)
        assertIs<SDKConfig.AuthKind.SSO>(decoded.config?.authKind)
        assertEquals(stored.config, decoded.config)
    }
}
