package com.octopuscommunity.sdk.data.remote.client

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Reads the expiry of an Octopus JWT without verifying it, as iOS `Jwt.isExpired` does. */
internal object Jwt {
    /**
     * Whether [token] expired at [nowEpochSeconds]: `true` once its numeric `exp` claim is reached,
     * `false` before, and `null` when the token is not a JWT or carries no readable `exp`.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun isExpired(
        token: String,
        nowEpochSeconds: Double = System.currentTimeMillis() / 1000.0,
    ): Boolean? {
        val parts = token.split('.')
        if (parts.size != 3) return null
        val payload = parts[1].replace('-', '+').replace('_', '/')
        val padded = payload.padEnd(payload.length + (4 - payload.length % 4) % 4, '=')
        return try {
            val claims = Json.parseToJsonElement(Base64.Default.decode(padded).decodeToString()).jsonObject
            val exp =
                claims["exp"]
                    ?.jsonPrimitive
                    ?.takeIf { !it.isString }
                    ?.content
                    ?.toDoubleOrNull()
            exp?.let { it <= nowEpochSeconds }
        } catch (_: IllegalArgumentException) {
            // Not base64, not JSON, or not a JSON object.
            null
        }
    }
}
