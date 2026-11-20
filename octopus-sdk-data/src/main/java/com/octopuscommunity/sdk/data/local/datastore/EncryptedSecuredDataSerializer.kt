package com.octopuscommunity.sdk.data.local.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.octopuscommunity.sdk.data.local.data.SecuredData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import javax.crypto.BadPaddingException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** A transient storage failure must abort DataStore's transaction without replacing its file. */
internal class SecuredStorageException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

/** A key lookup that can distinguish absence from a Keystore service failure confirmed absence. */
internal class MissingSecuredKeyException : GeneralSecurityException("Secured session key is absent")

/**
 * Keeps the existing Base64(IV + ciphertext) envelope with UTF-8 JSON inside the encryption.
 *
 * All fields, including defaults and nulls, are written deliberately: a later Kotlin default
 * change must not reinterpret a saved session. Missing fields in older files still use their
 * defaults. Unknown keys are ignored so removing a field does not discard the rest of the session;
 * the next write omits those obsolete keys. Crypto is injectable for JVM tests.
 */
@OptIn(ExperimentalEncodingApi::class)
internal class EncryptedSecuredDataSerializer(
    private val encrypt: (ByteArray) -> ByteArray,
    private val decrypt: (ByteArray) -> ByteArray,
) : Serializer<SecuredData> {
    override val defaultValue = SecuredData()
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    override suspend fun readFrom(input: InputStream): SecuredData =
        storageOperation("Unable to read secured session") {
            val bytes = withContext(Dispatchers.IO) { input.readBytes() }
            if (bytes.isEmpty()) throw CorruptionException("Secured session file is empty")
            val ciphertext =
                try {
                    Base64.decode(bytes)
                } catch (failure: IllegalArgumentException) {
                    throw CorruptionException("Secured session encoding is invalid", failure)
                }
            val plaintext =
                try {
                    decrypt(ciphertext)
                } catch (failure: MissingSecuredKeyException) {
                    throw CorruptionException("Secured session key is absent", failure)
                } catch (failure: BadPaddingException) {
                    throw CorruptionException("Secured session ciphertext is invalid", failure)
                }
            try {
                json.decodeFromString<SecuredData>(plaintext.decodeToString(throwOnInvalidSequence = true))
            } catch (failure: SerializationException) {
                throw CorruptionException("Secured session content is invalid", failure)
            } catch (failure: java.nio.charset.CharacterCodingException) {
                throw CorruptionException("Secured session text is invalid", failure)
            }
        }

    override suspend fun writeTo(
        t: SecuredData,
        output: OutputStream,
    ) {
        storageOperation("Unable to write secured session") {
            val bytes = Base64.encodeToByteArray(encrypt(json.encodeToString(t).toByteArray()))
            withContext(Dispatchers.IO) { output.write(bytes) }
        }
    }

    private suspend fun <T> storageOperation(
        message: String,
        block: suspend () -> T,
    ): T =
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (corruption: CorruptionException) {
            throw corruption
        } catch (failure: Exception) {
            throw SecuredStorageException(message, failure)
        }
}
