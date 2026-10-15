package com.octopuscommunity.sdk.sample.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class SDKConfigData(
    val hasData: Boolean = false,
    val config: SDKConfig? = null,
)

/**
 * Tolerant JSON for the on-disk sample config. `ignoreUnknownKeys = true`
 * lets us drop fields from [SDKConfig] (or its nested types) without
 * crashing on old payloads still carrying them. Removing a whole sealed
 * subtype (e.g. the former `AuthKind.Octopus`) instead fails at polymorphic
 * type resolution — that case is absorbed by the `runCatching` fallback in
 * `readFrom`, which resets to the default config.
 */
private val sampleConfigJson = Json { ignoreUnknownKeys = true }

private val Context.dataStore by dataStore(
    fileName = "config.json",
    serializer =
        object : Serializer<SDKConfigData> {
            override val defaultValue = SDKConfigData()

            override suspend fun readFrom(input: InputStream): SDKConfigData =
                withContext(Dispatchers.IO) {
                    runCatching {
                        input.use {
                            val content = it.readBytes().decodeToString().trim()
                            decodeSDKConfig(content)
                        }
                    }.getOrElse { SDKConfigData() }
                }

            override suspend fun writeTo(
                t: SDKConfigData,
                output: OutputStream,
            ) = withContext(Dispatchers.IO) {
                output.use {
                    it.write(
                        sampleConfigJson
                            .encodeToString(SDKConfigData.serializer(), t)
                            .encodeToByteArray(),
                    )
                }
            }
        },
)

class SDKConfigDataStore(
    context: Context,
    private val store: DataStore<SDKConfigData> = context.dataStore,
) {
    val configFlow get() = store.data.map { it.config }

    suspend fun updateConfig(transform: (t: SDKConfig?) -> SDKConfig?) =
        store.updateData { data ->
            val newConfig = transform(data.config)
            data.copy(hasData = newConfig != null, config = newConfig)
        }

    suspend fun clear() =
        store.updateData { data ->
            data.copy(hasData = false, config = null)
        }
}

/** Decodes the stored config, resetting configurations using a removed auth subtype. */
internal fun decodeSDKConfig(content: String): SDKConfigData =
    runCatching {
        if (content.isBlank() || content == "{}") {
            SDKConfigData()
        } else {
            sampleConfigJson.decodeFromString(SDKConfigData.serializer(), content)
        }
    }.getOrElse { SDKConfigData() }
