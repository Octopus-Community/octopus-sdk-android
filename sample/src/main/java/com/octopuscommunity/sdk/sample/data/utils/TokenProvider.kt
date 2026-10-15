package com.octopuscommunity.sdk.sample.data.utils

import android.util.Base64
import com.octopuscommunity.sdk.sample.BuildConfig
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * ⚠️ The content of this class is for sample purposes only, to create a token easily without
 * depending on a backend.
 * In your app, you should probably call a backend route that provides you this token.
 */
object TokenProvider {
    fun getToken(
        userId: String,
        entitlements: List<String> = emptyList(),
    ): String {
        // ⚠️ The content of this function is for sample purposes only
        // In your app, you should probably call a backend route that provides you this token
        val secret = BuildConfig.OCTOPUS_SSO_CLIENT_USER_TOKEN_SECRET

        val headerJson =
            buildJsonObject {
                put("alg", "HS256")
                put("typ", "JWT")
            }.toString()

        val payloadJson =
            buildJsonObject {
                put("sub", userId)
                put("exp", System.currentTimeMillis() / 1000L + (365 * 24 * 60 * 60)) // One year
                // Entitlements claim: opaque ids the host app issues for this user. The SDK does
                // not gate on this client-side — it forwards the JWT to the Octopus backend, which
                // resolves entitlements against the per-group requirements and returns
                // canAccess / canCreateChildren on each group/post/comment. PRD internal ticket §3.
                if (entitlements.isNotEmpty()) {
                    putJsonArray("entitlements") {
                        entitlements.forEach { add(it) }
                    }
                }
            }.toString()

        val headerBase64 = headerJson.toUrlSafeBase64()
        val payloadBase64 = payloadJson.toUrlSafeBase64()

        val toSign = "$headerBase64.$payloadBase64"
        val signature = createHmacSha256(toSign, secret)

        return "$headerBase64.$payloadBase64.$signature"
    }

    fun getBridgeSignature(bridgeFingerprint: String): String {
        // ⚠️ The content of this function is for sample purposes only
        // In your app, you should probably call a backend route that provides you this token
        val secret = BuildConfig.OCTOPUS_SSO_CLIENT_USER_TOKEN_SECRET

        val currentTime = (System.currentTimeMillis() / 1000L).toInt()

        val headerJson =
            buildJsonObject {
                put("alg", "HS256")
                put("typ", "JWT")
            }.toString()

        val payloadJson =
            buildJsonObject {
                put("bridge_fingerprint", bridgeFingerprint)
                // No `iat`: the bridge verifier doesn't use it (only signature, exp, and
                // bridge_fingerprint matter), and stamping iat with the device clock breaks the
                // bridge on any device whose clock runs ahead of the server — the server rejects
                // a token "issued in the future" ("Invalid token"). Keep just exp.
                put("exp", currentTime + (60 * 60)) // One hour
            }.toString()

        val headerBase64 = headerJson.toUrlSafeBase64()
        val payloadBase64 = payloadJson.toUrlSafeBase64()

        val toSign = "$headerBase64.$payloadBase64"
        val signature = createHmacSha256(toSign, secret)

        return "$headerBase64.$payloadBase64.$signature"
    }

    private fun createHmacSha256(
        data: String,
        secret: String,
    ): String {
        val algorithm = "HmacSHA256"
        val secretKeySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(secretKeySpec)
        val hmacSha256 = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return Base64
            .encodeToString(hmacSha256, Base64.NO_WRAP)
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")
    }

    private fun String.toUrlSafeBase64(): String =
        Base64
            .encodeToString(toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")
}
