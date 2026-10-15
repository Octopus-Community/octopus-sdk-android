package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.ui.navigation.ClientProfileRoute
import com.octopuscommunity.sdk.sample.ui.navigation.communityHostNavigation
import com.octopuscommunity.sdk.ui.components.UrlOpeningStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The scenario sets both conditions and re-applies its flag override after SDK reinitialisation. */
class ExposeClientUserIdOverrideTest {
    @Test
    fun `switch on with every field managed forces the flag on`() {
        val config =
            SDKConfig(
                authKind = SDKConfig.AuthKind.SSO(appManagedFields = ProfileField.ALL.toList()),
                hostCallbacks = SDKConfig.HostCallbacks(profileWired = true),
            )
        assertEquals(true, SDKConfigResolver.resolveExposeClientUserIdOverride(config))
    }

    @Test
    fun `switch on with a single managed field forces the flag on`() {
        val config =
            SDKConfig(
                authKind = SDKConfig.AuthKind.SSO(appManagedFields = listOf(ProfileField.BIO)),
                hostCallbacks = SDKConfig.HostCallbacks(profileWired = true),
            )
        assertEquals(true, SDKConfigResolver.resolveExposeClientUserIdOverride(config))
    }

    @Test
    fun `switch off clears the flag override`() {
        val config = SDKConfig(authKind = SDKConfig.AuthKind.SSO())
        assertEquals(null, SDKConfigResolver.resolveExposeClientUserIdOverride(config))
    }

    @Test
    fun `scenario switch wires and unwires Client profile together with the flag override`() {
        val original =
            SDKConfig(
                authKind = SDKConfig.AuthKind.SSO(forceLoginOnStrongActions = true),
                hostCallbacks = SDKConfig.HostCallbacks(profileWired = false, clientObjectWired = false),
                theme = SDKConfig.Theme.SdkDefault,
            )
        val enabled = original.withUnifiedProfile(true)
        assertTrue(enabled.unifiedProfileEnabled)
        assertTrue(enabled.effectiveHostCallbacks.profileWired)
        assertEquals(true, SDKConfigResolver.resolveExposeClientUserIdOverride(enabled))
        assertEquals(
            ProfileField.ALL,
            (SDKConfigResolver.resolveConnectionMode(enabled) as com.octopuscommunity.sdk.domain.model.ConnectionMode.SSO).appManagedFields,
        )
        assertEquals(true, (enabled.authKind as SDKConfig.AuthKind.SSO).forceLoginOnStrongActions)
        assertFalse(enabled.effectiveHostCallbacks.clientObjectWired)
        assertEquals(original.theme, enabled.theme)

        val disabled = enabled.withUnifiedProfile(false)
        assertFalse(disabled.unifiedProfileEnabled)
        assertFalse(disabled.effectiveHostCallbacks.profileWired)
        assertEquals(null, SDKConfigResolver.resolveExposeClientUserIdOverride(disabled))
        assertEquals(original, disabled)
    }

    @Test
    fun `switch off omits the profile callback even if stored wiring was on`() {
        val config = SDKConfig(authKind = SDKConfig.AuthKind.SSO(), hostCallbacks = SDKConfig.HostCallbacks(profileWired = true))
        assertFalse(config.effectiveHostCallbacks.profileWired)
        assertEquals(null, SDKConfigResolver.resolveExposeClientUserIdOverride(config))
    }

    @Test
    fun `unwiring Profile in Host Integration turns the scenario off and clears the override`() {
        val config =
            SDKConfig(authKind = SDKConfig.AuthKind.SSO())
                .withUnifiedProfile(true)
                .copy(hostCallbacks = SDKConfig.HostCallbacks(profileWired = false))
        assertFalse(config.unifiedProfileEnabled)
        assertFalse(config.effectiveHostCallbacks.profileWired)
        assertEquals(null, SDKConfigResolver.resolveExposeClientUserIdOverride(config))
    }

    @Test
    fun `auth presets with managed fields never enable the scenario without an explicit callback`() {
        val original = SDKConfig(authKind = SDKConfig.AuthKind.SSO())
        val legacyWiring = original.copy(hostCallbacks = SDKConfig.HostCallbacks(profileWired = true))
        for (config in listOf(original, legacyWiring)) {
            for (preset in authPresets) {
                val picked = config.withAuthKind(SDKConfig.AuthKind.SSO(preset.managedFields.toList(), preset.forceLogin))
                assertFalse(picked.unifiedProfileEnabled, preset.id)
                assertFalse(picked.effectiveHostCallbacks.profileWired, preset.id)
                assertEquals(null, SDKConfigResolver.resolveExposeClientUserIdOverride(picked), preset.id)
                assertEquals(preset.managedFields, (picked.authKind as SDKConfig.AuthKind.SSO).appManagedFields.toSet())
            }
        }
    }

    @Test
    fun `auth preset changes preserve an active scenario until managed fields are removed`() {
        val active = SDKConfig(authKind = SDKConfig.AuthKind.SSO()).withUnifiedProfile(true)
        val narrowed = active.withAuthKind(SDKConfig.AuthKind.SSO(appManagedFields = listOf(ProfileField.BIO)))
        assertTrue(narrowed.unifiedProfileEnabled)
        assertTrue(narrowed.effectiveHostCallbacks.profileWired)
        val native = narrowed.withAuthKind(SDKConfig.AuthKind.SSO())
        assertFalse(native.unifiedProfileEnabled)
        val managedAgain = native.withAuthKind(SDKConfig.AuthKind.SSO(appManagedFields = ProfileField.ALL.toList()))
        assertFalse(managedAgain.unifiedProfileEnabled)
        assertFalse(managedAgain.effectiveHostCallbacks.profileWired)
    }

    @Test
    fun `callback-only activation changes restart the SDK to reapply its flag override`() {
        val fieldsOnly = SDKConfig(authKind = SDKConfig.AuthKind.SSO(appManagedFields = ProfileField.ALL.toList()))
        val active = fieldsOnly.withUnifiedProfile(true)
        assertEquals(fieldsOnly.authKind, active.authKind)
        assertNotEquals(fieldsOnly.sdkInitKey, active.sdkInitKey)
        val unwired = active.copy(hostCallbacks = active.hostCallbacks.copy(profileWired = false))
        assertNotEquals(active.sdkInitKey, unwired.sdkInitKey)
        assertEquals(null, SDKConfigResolver.resolveExposeClientUserIdOverride(unwired))
    }

    @Test
    fun `effective callback wiring dispatches to Client profile and becomes null when disabled`() {
        val destinations = mutableListOf<Any>()

        fun wiring(config: SDKConfig) =
            communityHostNavigation(
                callbacks = config.effectiveHostCallbacks,
                navigateHost = { destinations.add(it) },
                onClientObject = {},
                onGroupAccessDenied = {},
                onUrl = { UrlOpeningStrategy.HandledByOctopus },
            )
        val active = SDKConfig(authKind = SDKConfig.AuthKind.SSO()).withUnifiedProfile(true)
        wiring(active).profile!!("client-42")
        assertEquals(listOf<Any>(ClientProfileRoute("client-42")), destinations)
        assertNull(wiring(active.withUnifiedProfile(false)).profile)
        assertNull(wiring(active.copy(hostCallbacks = active.hostCallbacks.copy(profileWired = false))).profile)
    }
}
