package com.octopuscommunity.sdk.data.local.datastore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.Serializer
import com.octopuscommunity.sdk.data.local.data.SecuredData
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * Session key lookup and cipher behind [SecuredDataStore.SecuredDataSerializer]. The Keystore,
 * API level, key generation and cipher are injected so JVM tests can drive the real lookup code.
 */
internal class SecuredSessionCrypto(
    private val apiLevel: Int,
    openKeyStore: () -> KeyStore,
    private val generateKey: (alias: String) -> SecretKey,
    private val newCipher: () -> Cipher,
) {
    private val keyStore by lazy { openKeyStore() }

    @Volatile
    private var sessionFile: File? = null

    @Volatile
    private var leftoverProbeChecked = false

    val serializer: Serializer<SecuredData> = EncryptedSecuredDataSerializer(encrypt = ::encrypt, decrypt = ::decrypt)

    private val probe =
        SecuredKeyAbsenceProbe(
            generate = { generateKey(PROBE_ALIAS) },
            exists = { keyStore.getKey(PROBE_ALIAS, null) != null },
            delete = { keyStore.deleteEntry(PROBE_ALIAS) },
        )

    fun bindSessionFile(file: File) {
        sessionFile = file
    }

    fun encrypt(bytes: ByteArray): ByteArray {
        val cipher = newCipher()
        cipher.init(Cipher.ENCRYPT_MODE, key(createIfMissing = true))
        val iv = cipher.iv
        val encrypted = cipher.doFinal(bytes)
        return iv + encrypted
    }

    fun decrypt(bytes: ByteArray): ByteArray {
        val cipher = newCipher()
        validateSecuredCiphertext(bytes, cipher.blockSize)
        val iv = bytes.copyOfRange(0, cipher.blockSize)
        val data = bytes.copyOfRange(cipher.blockSize, bytes.size)
        cipher.init(Cipher.DECRYPT_MODE, key(createIfMissing = false), IvParameterSpec(iv))
        return cipher.doFinal(data)
    }

    private fun key(createIfMissing: Boolean): SecretKey {
        forgetLeftoverProbe()
        return securedSessionKey(
            apiLevel = apiLevel,
            createIfMissing = createIfMissing,
            // Without the SDK storage context, key creation cannot be proven safe either.
            sessionFileExists = { sessionFile?.exists() ?: true },
            // getKey, not getEntry: getEntry asks containsAlias first, which swallows keystore2 errors.
            lookup = { keyStore.getKey(KEY_ALIAS, null) as? SecretKey },
            generate = { generateKey(KEY_ALIAS) },
            probe = probe,
        )
    }

    /** Once per instance, removes a leftover probe, including after an upgrade to API 31+. */
    private fun forgetLeftoverProbe() {
        if (leftoverProbeChecked) return
        leftoverProbeChecked = true
        try {
            if (keyStore.containsAlias(PROBE_ALIAS)) keyStore.deleteEntry(PROBE_ALIAS)
        } catch (failure: Exception) {
            // Best effort: a fresh instance retries, and a legacy absence check replaces the probe.
        }
    }

    companion object {
        const val KEY_ALIAS = "octopus_secret"
        const val PROBE_ALIAS = "octopus_secret_absence_probe"
    }
}

/** Binds [sessionFile] to [crypto] first: unbound, key creation is refused and nothing persists. */
internal fun securedSessionDataStore(
    sessionFile: File,
    crypto: SecuredSessionCrypto,
    scope: CoroutineScope,
    serializer: Serializer<SecuredData> = crypto.serializer,
    migrations: List<DataMigration<SecuredData>> = emptyList(),
    state: SecuredStorageState = SecuredStorageState(::logSecuredStorageFailure),
    forgetLegacyPreferences: () -> Unit = {},
): SessionPreservingDataStore {
    crypto.bindSessionFile(sessionFile)
    return sessionPreservingDataStore(
        serializer = serializer,
        scope = scope,
        migrations = migrations,
        state = state,
        forgetLegacyPreferences = forgetLegacyPreferences,
        produceFile = { sessionFile },
    )
}
