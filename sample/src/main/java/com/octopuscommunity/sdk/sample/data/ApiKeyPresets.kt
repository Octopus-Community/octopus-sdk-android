package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.domain.model.ProfileField

/**
 * Auth-shape presets backing the Config screen's single-choice API-key picker
 * (sample-UX parity with the Flutter example's named-key picker).
 *
 * Android intentionally keeps the derive-from-auth model as the single source
 * of truth: picking a preset applies its auth *shape* (forced login +
 * app-managed fields) to the Config screen, and [SDKConfigResolver] keeps
 * deriving the actual key from that shape. Unlike Flutter, the same Config
 * inputs also build the `ConnectionMode.SSO(appManagedFields)` handed to the
 * SDK, so a raw "pick any key" picker could silently mismatch the key and the
 * connection mode — the preset layer makes them consistent by construction.
 *
 * Both the label and the key resolve from the build-injected [injectedApiKeys]
 * blob, matched by slot id. A single build-time path keeps the four slots consistent.
 * Committing slot ids/labels here is safe regardless of mirroring: an id like
 * [AuthPreset.SSO_NO_MANAGED_NO_FORCED] and a label like "SSO · no managed
 * fields · no forced login" name an auth *shape*, not a credential — unlike
 * [AuthPreset.key], neither one can be used to authenticate against a
 * community. That is also why only [key] documents the keyless-build fallback
 * as blank with no committed default, while [label] is allowed one.
 *
 * Only SSO configurations are supported across the SDKs and their samples.
 */
data class AuthPreset(
    /** Stable slot id — MUST match the `OCTOPUS_NAMED_API_KEYS` blob ids. */
    val id: String,
    /** Committed label used when the named-key blob is not injected. */
    val fallbackLabel: String,
    /** Force-login flag this preset applies to the Config screen. */
    val forceLogin: Boolean,
    /**
     * Representative app-managed fields this preset applies. For
     * [SSO_SOME_MANAGED_FORCED] any non-empty proper subset resolves to the
     * same key, so this is only the default shape applied on first pick.
     */
    val managedFields: Set<ProfileField>,
) {
    /** Launcher-injected label when available, committed fallback otherwise. */
    val label: String
        get() =
            injectedApiKeys
                .firstOrNull { it.id == id }
                ?.label
                ?.ifBlank { null }
                ?: fallbackLabel

    /**
     * The build-injected key value this preset resolves to — blank on a
     * keyless build (cold clone / public CI), same as [label]'s fallback
     * path but with no committed fallback: a key is a secret, a label isn't.
     */
    val key: String
        get() = injectedApiKeys.firstOrNull { it.id == id }?.key ?: ""

    companion object {
        const val SSO_NO_MANAGED_NO_FORCED = "ssoNoManagedNoForced"
        const val SSO_NO_MANAGED_FORCED = "ssoNoManagedForced"
        const val SSO_ALL_MANAGED_FORCED = "ssoAllManagedForced"
        const val SSO_SOME_MANAGED_FORCED = "ssoSomeManagedForced"
    }
}

/**
 * The four SSO demo-community presets, in the same order as the Flutter
 * picker (and the `OCTOPUS_NAMED_API_KEYS` blob).
 */
val authPresets: List<AuthPreset> =
    listOf(
        AuthPreset(
            id = AuthPreset.SSO_NO_MANAGED_NO_FORCED,
            fallbackLabel = "SSO · no managed fields · no forced login",
            forceLogin = false,
            managedFields = emptySet(),
        ),
        AuthPreset(
            id = AuthPreset.SSO_NO_MANAGED_FORCED,
            fallbackLabel = "SSO · no managed fields · forced login",
            forceLogin = true,
            managedFields = emptySet(),
        ),
        AuthPreset(
            id = AuthPreset.SSO_ALL_MANAGED_FORCED,
            fallbackLabel = "SSO · all managed fields · forced login",
            forceLogin = true,
            managedFields = ProfileField.ALL,
        ),
        AuthPreset(
            id = AuthPreset.SSO_SOME_MANAGED_FORCED,
            fallbackLabel = "SSO · some managed fields · forced login",
            forceLogin = true,
            managedFields = setOf(ProfileField.NICKNAME, ProfileField.PICTURE),
        ),
    )

/**
 * Presets whose key is actually present in this build. Empty on a keyless
 * build (cold clone / public CI) → the Config screen falls back to the legacy
 * "Use custom API key" switch, iso the Flutter keyless fallback.
 */
val availableAuthPresets: List<AuthPreset> = authPresets.filter { it.key.isNotBlank() }

/**
 * Maps an auth shape to the preset (and therefore the key) it resolves to.
 *
 * This is THE key-derivation rule — [SDKConfigResolver.resolveApiKey]
 * delegates here, and the Config screen uses it both to highlight the picker
 * row matching the current shape and to display the resolved key. Note that
 * with a non-empty field set the [forceLogin] flag does not change the slot
 * (there are only forced-login communities for managed fields).
 */
fun resolveAuthPreset(
    forceLogin: Boolean,
    appManagedFields: Set<ProfileField>,
): AuthPreset {
    val id =
        if (appManagedFields.isEmpty()) {
            if (forceLogin) {
                AuthPreset.SSO_NO_MANAGED_FORCED
            } else {
                AuthPreset.SSO_NO_MANAGED_NO_FORCED
            }
        } else if (appManagedFields.containsAll(ProfileField.ALL)) {
            AuthPreset.SSO_ALL_MANAGED_FORCED
        } else {
            AuthPreset.SSO_SOME_MANAGED_FORCED
        }
    return authPresets.first { it.id == id }
}
