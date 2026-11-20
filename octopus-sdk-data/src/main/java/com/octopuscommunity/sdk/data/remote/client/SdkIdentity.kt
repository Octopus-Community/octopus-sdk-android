package com.octopuscommunity.sdk.data.remote.client

import com.octopuscommunity.sdk.data.BuildConfig

/**
 * The SDK the host app integrates, as reported to the backend in the `sdkfamily` and
 * `sdkfamilyversion` headers.
 *
 * A host app built on a wrapper (Flutter, React Native, Unity) embeds this native SDK, so
 * `sdkversion` alone cannot tell it apart from a native integration. `sdkversion` stays the native
 * version because the backend gates features on it; this pair carries the wrapper's own family and
 * version.
 *
 * @property family the SDK family.
 * @property version version of the SDK the host app integrates: the wrapper's version for a wrapper.
 */
data class SdkIdentity(
    val family: Family,
    val version: String,
) {
    /**
     * SDK family.
     *
     * @property wireValue what the backend receives in the `sdkfamily` header.
     */
    enum class Family(
        val wireValue: String,
    ) {
        NATIVE("Native"),
        FLUTTER("Flutter"),
        REACT_NATIVE("ReactNative"),
        UNITY("Unity"),
    }

    companion object {
        /** What the SDK reports when no wrapper declared itself. */
        val NATIVE = SdkIdentity(family = Family.NATIVE, version = BuildConfig.SDK_VERSION_NAME)
    }
}
