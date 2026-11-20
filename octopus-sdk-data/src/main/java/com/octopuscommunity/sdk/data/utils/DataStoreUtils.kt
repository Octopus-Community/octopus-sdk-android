package com.octopuscommunity.sdk.data.utils

import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

internal inline fun <reified T> KSerializer<T>.dataStoreSerializer(defaultValue: T) =
    object : Serializer<T> {
        // encodeDefaults is required, not cosmetic: kotlinx.serialization otherwise omits any
        // field whose value equals its default, and SessionsData's timestamps default to
        // System.currentTimeMillis() — a default re-evaluated on every serialisation. A startTime
        // written in the same millisecond was therefore dropped from the stored record, and the
        // next read re-defaulted it to the moment of reading, collapsing the reported session
        // duration towards zero (internal ticket). The defaults themselves have to stay: a record written
        // before this fix may be missing startTime — and any other field that happened to equal its
        // default, such as isFirstSession when true or stopTime when null — so making them required
        // would throw MissingFieldException on those installs.
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        override val defaultValue = defaultValue

        override suspend fun readFrom(input: InputStream): T =
            withContext(Dispatchers.IO) {
                input.use {
                    json.decodeFromString(this@dataStoreSerializer, input.readBytes().decodeToString())
                }
            }

        override suspend fun writeTo(
            t: T,
            output: OutputStream,
        ) = withContext(Dispatchers.IO) {
            output.use {
                it.write(json.encodeToString(this@dataStoreSerializer, t).encodeToByteArray())
            }
        }
    }
