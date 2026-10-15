package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.domain.model.ProfileField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Shape → preset resolution backing both `SDKConfigResolver.resolveApiKey`
 * and the Config screen's named-key picker highlight. Assertions stick to
 * slot ids (never key values) so they hold on keyless builds too.
 */
class ApiKeyPresetsTest {
    @Test
    fun `no managed fields and no forced login resolves to the no-forced slot`() {
        assertEquals(
            AuthPreset.SSO_NO_MANAGED_NO_FORCED,
            resolveAuthPreset(forceLogin = false, appManagedFields = emptySet()).id,
        )
    }

    @Test
    fun `no managed fields with forced login resolves to the forced slot`() {
        assertEquals(
            AuthPreset.SSO_NO_MANAGED_FORCED,
            resolveAuthPreset(forceLogin = true, appManagedFields = emptySet()).id,
        )
    }

    @Test
    fun `all managed fields resolves to the all-managed slot regardless of force login`() {
        assertEquals(
            AuthPreset.SSO_ALL_MANAGED_FORCED,
            resolveAuthPreset(forceLogin = true, appManagedFields = ProfileField.ALL).id,
        )
        // With a non-empty field set the force-login flag does not change the
        // slot (only forced-login communities exist for managed fields).
        assertEquals(
            AuthPreset.SSO_ALL_MANAGED_FORCED,
            resolveAuthPreset(forceLogin = false, appManagedFields = ProfileField.ALL).id,
        )
    }

    @Test
    fun `any proper non-empty subset resolves to the some-managed slot`() {
        ProfileField.ALL.forEach { single ->
            assertEquals(
                AuthPreset.SSO_SOME_MANAGED_FORCED,
                resolveAuthPreset(forceLogin = true, appManagedFields = setOf(single)).id,
            )
        }
        assertEquals(
            AuthPreset.SSO_SOME_MANAGED_FORCED,
            resolveAuthPreset(
                forceLogin = true,
                appManagedFields = setOf(ProfileField.NICKNAME, ProfileField.BIO),
            ).id,
        )
    }

    @Test
    fun `every preset resolves back to itself from its own representative shape`() {
        authPresets.forEach { preset ->
            assertEquals(
                preset.id,
                resolveAuthPreset(preset.forceLogin, preset.managedFields).id,
                "Preset ${preset.id} must be the fixed point of its own shape",
            )
        }
    }

    @Test
    fun `preset ids are unique and match the injected blob id convention`() {
        assertEquals(authPresets.size, authPresets.map { it.id }.toSet().size)
        // Ids are the contract with the OCTOPUS_NAMED_API_KEYS blob emitted by
        // build-time configuration (label matching) — keep them stable.
        assertEquals(
            listOf(
                "ssoNoManagedNoForced",
                "ssoNoManagedForced",
                "ssoAllManagedForced",
                "ssoSomeManagedForced",
            ),
            authPresets.map { it.id },
        )
    }

    @Test
    fun `available presets is a filtered view of the full list`() {
        assertTrue(authPresets.containsAll(availableAuthPresets))
        assertTrue(availableAuthPresets.all { it.key.isNotBlank() })
    }
}
