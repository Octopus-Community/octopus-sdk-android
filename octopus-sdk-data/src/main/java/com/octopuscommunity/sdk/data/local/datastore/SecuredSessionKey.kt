package com.octopuscommunity.sdk.data.local.datastore

import androidx.datastore.core.CorruptionException
import javax.crypto.SecretKey

/** Keystore operations on a throwaway alias, used to confirm a null session key lookup below API 31. */
internal class SecuredKeyAbsenceProbe(
    val generate: () -> Unit,
    val exists: () -> Boolean,
    val delete: () -> Unit,
)

internal fun securedSessionKey(
    apiLevel: Int,
    createIfMissing: Boolean,
    sessionFileExists: () -> Boolean,
    lookup: () -> SecretKey?,
    generate: () -> SecretKey,
    probe: SecuredKeyAbsenceProbe,
): SecretKey {
    lookup()?.let { return it }
    if (createIfMissing) {
        // Even a previously successful read cannot make a later null lookup safe to replace.
        if (sessionFileExists()) throw SecuredStorageException("Unable to access retained session key")
        return generate()
    }
    // keystore2 engineGetKey returns null only for KEY_NOT_FOUND.
    if (apiLevel >= 31) throw MissingSecuredKeyException()
    return confirmLegacyKeyAbsence(lookup, probe)
}

/**
 * Legacy providers (API 23-30) answer getKey through contains(), which also returns false when the
 * binder call fails, so a null lookup alone never proves absence. Key generation throws instead
 * (ProviderException): a probe key that can be generated and then read back proves the lookup path
 * works at that moment. A service failure affecting only the real-key lookup can still look like
 * absence. A wrong verdict moves the session file to `.unreadable`, which is never restored and
 * may be overwritten by the next move or deleted by logout/clear; a replacement key cannot read it.
 */
private fun confirmLegacyKeyAbsence(
    lookup: () -> SecretKey?,
    probe: SecuredKeyAbsenceProbe,
): SecretKey {
    val key: SecretKey?
    val probeVisible: Boolean
    try {
        probe.generate()
        key = lookup()
        probeVisible = key == null && probe.exists()
    } catch (failure: Exception) {
        deleteProbe(probe)
        throw SecuredStorageException("Unable to confirm secured session key absence", failure)
    }
    // A probe left behind by a failed delete is removed on the next launch.
    val probeDeleted = deleteProbe(probe)
    if (key != null) return key
    if (!probeVisible || !probeDeleted) {
        throw SecuredStorageException("Unable to confirm secured session key absence")
    }
    throw MissingSecuredKeyException()
}

private fun deleteProbe(probe: SecuredKeyAbsenceProbe): Boolean =
    try {
        probe.delete()
        true
    } catch (failure: Exception) {
        false
    }

internal fun validateSecuredCiphertext(
    bytes: ByteArray,
    blockSize: Int,
) {
    if (bytes.size < blockSize * 2 || bytes.size % blockSize != 0) {
        throw CorruptionException("Secured session ciphertext is incomplete")
    }
    // IllegalBlockSizeException from doFinal instead represents arbitrary Keystore failures.
}
