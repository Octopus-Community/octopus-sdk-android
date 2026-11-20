package com.octopuscommunity.sdk

import com.octopuscommunity.sdk.data.remote.client.SdkIdentity

/**
 * The SDK families a wrapper can declare with [OctopusSDK.setSdkFamily]. Native is the default and
 * cannot be set.
 *
 * It is **not** part of the supported public API (see [InternalOctopusApi]): only the Octopus
 * wrapper SDKs use it. The values sent to the backend live in [SdkIdentity.Family].
 */
@InternalOctopusApi
enum class SdkFamily {
    /** The Octopus SDK for Flutter. */
    FLUTTER,

    /** The Octopus SDK for React Native. */
    REACT_NATIVE,

    /** The Octopus SDK for Unity. */
    UNITY,
}

@OptIn(InternalOctopusApi::class)
internal val SdkFamily.coreValue: SdkIdentity.Family
    get() =
        when (this) {
            SdkFamily.FLUTTER -> SdkIdentity.Family.FLUTTER
            SdkFamily.REACT_NATIVE -> SdkIdentity.Family.REACT_NATIVE
            SdkFamily.UNITY -> SdkIdentity.Family.UNITY
        }
