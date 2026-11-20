package com.octopuscommunity.sdk.domain.utils

import java.nio.ByteBuffer
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Encodes the UUID's 16 bytes (most-significant first) as unpadded, URL-safe Base64: 22 characters.
 *
 * Uses the Kotlin stdlib encoder rather than `android.util.Base64`, whose stub throws "not mocked"
 * under a plain JVM unit test -- which left every session identity, and so every session use case
 * in this module, untestable. The alphabet and padding match the flags this previously passed
 * (`URL_SAFE or NO_PADDING or NO_WRAP`), so the output is byte-identical and stored session ids are
 * unaffected; `UUIDUtilsTest` pins that with fixed vectors. The same encoder is already used in
 * production by `SecuredDataStore` in the data module.
 */
@OptIn(ExperimentalEncodingApi::class)
fun UUID.toBase64(): String =
    Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(
        ByteBuffer
            .allocate(16)
            .apply {
                putLong(mostSignificantBits)
                putLong(leastSignificantBits)
            }.array(),
    )
