package com.octopuscommunity.sdk.sample.data.model

import com.octopuscommunity.sdk.sample.data.datastore.SDKConfigData
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CommunityNavigationConfigTest {
    @Test
    fun `legacy persisted config without the navigation field defaults to In tab`() {
        val legacy =
            Json.encodeToString(
                SDKConfigData.serializer(),
                SDKConfigData(hasData = true, config = SDKConfig(authKind = SDKConfig.AuthKind.SSO())),
            )
        assertFalse(legacy.contains("communityNavigation"))
        val decoded = Json.decodeFromString(SDKConfigData.serializer(), legacy)
        assertEquals(SDKConfig.CommunityNavigation.InTab, decoded.config!!.communityNavigation)
    }

    @Test
    fun `both choices persist and switching navigation leaves SDK initialization unchanged`() {
        val original = SDKConfig(authKind = SDKConfig.AuthKind.SSO(), appearance = SDKConfig.Appearance.Dark)
        for (mode in SDKConfig.CommunityNavigation.entries) {
            val updated = original.copy(communityNavigation = mode)
            val decoded = Json.decodeFromString(SDKConfig.serializer(), Json.encodeToString(SDKConfig.serializer(), updated))
            assertEquals(mode, decoded.communityNavigation)
            assertEquals(original.sdkInitKey, decoded.sdkInitKey)
            assertEquals(original.appearance, decoded.appearance)
            assertEquals(original.hostCallbacks, decoded.hostCallbacks)
        }
    }
}
