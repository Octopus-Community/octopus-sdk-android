package com.octopuscommunity.sdk.data.local.datastore

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import com.octopuscommunity.sdk.data.local.data.ClientUserData
import com.octopuscommunity.sdk.data.local.data.PushNotificationsData
import com.octopuscommunity.sdk.data.local.data.SecuredData
import com.octopuscommunity.sdk.data.local.data.UserData
import com.octopuscommunity.sdk.data.local.datastore.SecuredDataStore.SecuredDataSerializer
import com.octopuscommunity.sdk.data.local.datastore.SecuredDataStore.SecuredPreferencesMigration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private val Context.dataStore by object : ReadOnlyProperty<Context, SessionPreservingDataStore> {
    private val instances = mutableMapOf<String, SessionPreservingDataStore>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun getValue(
        thisRef: Context,
        property: KProperty<*>,
    ): SessionPreservingDataStore =
        synchronized(this) {
            val context = thisRef.applicationContext
            val file = context.dataStoreFile("octopus_secured_data.json")
            instances.getOrPut(file.absolutePath) {
                securedSessionDataStore(
                    sessionFile = file,
                    // minSdk is 23. Initialization is deferred until read/write so a Keystore
                    // failure cannot select a serializer that overwrites the file with defaults.
                    crypto = SecuredDataSerializer.crypto,
                    serializer = SecuredDataSerializer,
                    scope = scope,
                    migrations =
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
                            listOf(
                                bestEffortMigrationCleanup(SecuredPreferencesMigration.create(context)),
                                legacyLoginDataMigration(context),
                            )
                        } else {
                            listOf(legacyLoginDataMigration(context))
                        },
                    forgetLegacyPreferences = {
                        // Explicit logout/reset may discard even unopened encrypted preferences.
                        clearExistingLegacyPreferences(
                            File(context.filesDir.parentFile, "shared_prefs/octopus_secured_preferences.xml"),
                        ) {
                            context
                                .getSharedPreferences("octopus_secured_preferences", Context.MODE_PRIVATE)
                                .edit()
                                .clear()
                                .commit()
                        }
                    },
                )
            }
        }
}

class SecuredDataStore(
    val context: Context,
    val sdkScope: CoroutineScope,
) {
    private val sessionStore = context.dataStore

    val data get() = sessionStore.data

    val userFlow get() = data.map { it.user }
    val clientUserFlow get() = data.map { it.clientUser }
    val pushNotificationsFlow get() = data.map { it.pushNotifications }

    suspend fun getUser() = userFlow.first()

    suspend fun getClientUser() = clientUserFlow.first()

    suspend fun getPushNotifications() = pushNotificationsFlow.first()

    suspend fun update(transform: (t: SecuredData) -> SecuredData) = sessionStore.updateData(transform)

    suspend fun updateUser(transform: (UserData?) -> UserData?) = sessionStore.updateUser(transform)

    /** Forgets user and client user in one write while [predicate] holds; returns the replaced state, or `null`. */
    internal suspend fun forgetSessionIf(predicate: (SecuredData) -> Boolean): SecuredData? = sessionStore.forgetSessionIf(predicate)

    suspend fun updateClientUser(transform: (ClientUserData?) -> ClientUserData?) = sessionStore.updateClientUser(transform)

    suspend fun updatePushNotifications(transform: (PushNotificationsData?) -> PushNotificationsData?) =
        update {
            it.copy(pushNotifications = transform(it.pushNotifications))
        }

    suspend fun clear() {
        sessionStore.clear()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    object SecuredDataSerializer : Serializer<SecuredData> {
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"

        override var defaultValue = SecuredData()

        internal val crypto =
            SecuredSessionCrypto(
                apiLevel = Build.VERSION.SDK_INT,
                openKeyStore = { KeyStore.getInstance("AndroidKeyStore").apply { load(null) } },
                generateKey = ::generateKey,
                newCipher = { Cipher.getInstance(TRANSFORMATION) },
            )

        private fun generateKey(alias: String): SecretKey =
            KeyGenerator
                .getInstance(ALGORITHM, "AndroidKeyStore")
                .apply {
                    init(
                        KeyGenParameterSpec
                            .Builder(
                                alias,
                                KeyProperties.PURPOSE_ENCRYPT or
                                    KeyProperties.PURPOSE_DECRYPT,
                            ).setBlockModes(BLOCK_MODE)
                            .setEncryptionPaddings(PADDING)
                            .setRandomizedEncryptionRequired(true)
                            .setUserAuthenticationRequired(false)
                            .build(),
                    )
                }.generateKey()

        override suspend fun readFrom(input: InputStream): SecuredData = crypto.serializer.readFrom(input)

        override suspend fun writeTo(
            t: SecuredData,
            output: OutputStream,
        ) = crypto.serializer.writeTo(t, output)

        fun encrypt(bytes: ByteArray): ByteArray = crypto.encrypt(bytes)

        fun decrypt(bytes: ByteArray): ByteArray = crypto.decrypt(bytes)
    }

    internal object LegacySecuredDataSerializer : Serializer<SecuredData> {
        override var defaultValue: SecuredData = SecuredData()

        override suspend fun readFrom(input: InputStream) = defaultValue

        override suspend fun writeTo(
            t: SecuredData,
            output: OutputStream,
        ) {
            defaultValue = t
        }
    }

    object SecuredPreferencesMigration {
        private const val ENCRYPTED_PREFERENCES_NAME = "octopus_secured_preferences"

        // security-crypto is deprecated as a whole; this only reads the legacy store to migrate away from it.
        @Suppress("DEPRECATION")
        fun create(context: Context) =
            securedPreferencesMigration(
                readRawKeys = {
                    context.getSharedPreferences(ENCRYPTED_PREFERENCES_NAME, Context.MODE_PRIVATE).all.keys
                },
                openPreferences = {
                    androidx.security.crypto.EncryptedSharedPreferences.create(
                        context,
                        ENCRYPTED_PREFERENCES_NAME,
                        androidx.security.crypto.MasterKey
                            .Builder(context)
                            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                            .setUserAuthenticationRequired(false)
                            .build(),
                        androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                    )
                },
            )
    }
}

// security-crypto is deprecated as a whole; this only clears an obsolete entry of the legacy store.
@Suppress("DEPRECATION")
private fun legacyLoginDataMigration(context: Context) =
    legacyLoginDataMigration {
        // Old preferences may still exist on OS versions where session migration was skipped.
        // Delete only the obsolete request, never a stored session.
        val name = "octopus_secured_preferences"
        cleanUpLegacyLoginPreferences(
            readRawKeys = { context.getSharedPreferences(name, Context.MODE_PRIVATE).all.keys },
            removeRequest = {
                val preferences =
                    androidx.security.crypto.EncryptedSharedPreferences.create(
                        context,
                        name,
                        androidx.security.crypto.MasterKey
                            .Builder(context)
                            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                            .build(),
                        androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                    )
                preferences.edit(commit = true) {
                    remove("magic_link_request_id")
                    remove("magic_link_request_email")
                }
            },
            clearRawPreferences = {
                context.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) { clear() }
            },
            logFailure = ::logLegacyCleanupFailure,
        )
    }

internal fun logLegacyCleanupFailure(failure: Throwable) {
    Log.w("OctopusSecuredStorage", "Unable to clean up legacy login preferences", failure)
}
