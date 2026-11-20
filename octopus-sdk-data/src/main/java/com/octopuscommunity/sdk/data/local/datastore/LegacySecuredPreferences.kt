package com.octopuscommunity.sdk.data.local.datastore

private val encryptedPreferencesKeysetKeys =
    setOf(
        "__androidx_security_crypto_encrypted_prefs_key_keyset__",
        "__androidx_security_crypto_encrypted_prefs_value_keyset__",
    )

/** Keysets alone are not legacy session data and must never re-arm migration. */
internal fun hasLegacySecuredPreferenceData(keys: Set<String>): Boolean = keys.any { it !in encryptedPreferencesKeysetKeys }

/** Runs obsolete-request cleanup without letting preference or logging failures block DataStore. */
internal fun cleanUpLegacyLoginPreferences(
    readRawKeys: () -> Set<String>,
    removeRequest: () -> Unit,
    clearRawPreferences: () -> Unit,
    logFailure: (Throwable) -> Unit,
) {
    runCatching {
        // Opening encrypted preferences creates keysets, so never do it for an empty raw file.
        if (readRawKeys().isEmpty()) return@runCatching
        removeRequest()
        val remainingKeys = readRawKeys()
        if (remainingKeys.isNotEmpty() && !hasLegacySecuredPreferenceData(remainingKeys)) {
            clearRawPreferences()
        }
    }.onFailure { failure ->
        // Logging is best-effort too: it must not turn cleanup into an initialization failure.
        runCatching { logFailure(failure) }
    }
}
