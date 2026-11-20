package com.octopuscommunity.sdk.data.utils

import java.security.MessageDigest

/**
 * Extension function for MD5 hashing
 */
internal fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(this.toByteArray())
    return digest.joinToString("") {
        "%02x".format(it)
    }
}