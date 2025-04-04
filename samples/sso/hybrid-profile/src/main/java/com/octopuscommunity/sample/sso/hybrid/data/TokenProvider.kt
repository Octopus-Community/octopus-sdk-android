package com.octopuscommunity.sample.sso.hybrid.data

import android.util.Base64
import com.octopuscommunity.sample.sso.hybrid.BuildConfig
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * ⚠️ The content of this class is for sample purposes only, to create a token easily without
 * depending on a backend.
 * In your app, you should probably call a backend route that provides you this token.
 */
object TokenProvider {

    private data class Header(
        val alg: String = "HS256",
        val typ: String = "JWT"
    )

    private data class Payload(
        val sub: String,
        val exp: Long
    )

    fun getToken(userId: String): String {
        // ⚠️ The content of this function is for sample purposes only
        // In your app, you should probably call a backend route that provides you this token
        val secret = BuildConfig.OCTOPUS_SSO_CLIENT_USER_TOKEN_SECRET

        val header = Header()
        val payload = Payload(
            sub = userId,
            exp = System.currentTimeMillis() / 1000L + (365 * 24 * 60 * 60)  // One year
        )

        val headerJson = JSONObject().apply {
            put("alg", header.alg)
            put("typ", header.typ)
        }.toString()

        val payloadJson = JSONObject().apply {
            put("sub", payload.sub)
            put("exp", payload.exp)
        }.toString()

        val headerBase64 = headerJson.toUrlSafeBase64()
        val payloadBase64 = payloadJson.toUrlSafeBase64()

        val toSign = "$headerBase64.$payloadBase64"
        val signature = createHmacSha256(toSign, secret)

        return "$headerBase64.$payloadBase64.$signature"
    }

    private fun createHmacSha256(data: String, secret: String): String {
        val algorithm = "HmacSHA256"
        val secretKeySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(secretKeySpec)
        val hmacSha256 = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(hmacSha256, Base64.NO_WRAP)
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")
    }

    private fun String.toUrlSafeBase64(): String {
        return Base64.encodeToString(toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")
    }
}