package com.octopuscommunity.sdk.domain

/**
 * Process-wide holder for the callback that signs a prefilled (bridge / Share-in-game) post, so
 * the server accepts its image in a community that forbids member pictures.
 *
 * Wired per editor session by the create-post entry points (`navigateToOctopusCreatePost` /
 * `OctopusCreatePostScreen`) from
 * [com.octopuscommunity.sdk.domain.model.CreatePostScreenInfo.bridgeShareTokenProvider] when the
 * editor opens, and cleared when it closes. `null` (the default) when no provider was supplied —
 * the common case, where no signing is needed and prefilled shares are sent unsigned. The holder
 * reflects the single post editor currently open: the SDK opens at most one at a time, and the
 * publish path reads it while that editor is still composed.
 *
 * The callback receives the bridge fingerprint (SHA-256 of the final post content) and must
 * return a client-signed JWT carrying that fingerprint in its `bridge_fingerprint` claim, or
 * `null` to send the post unsigned. It is consulted only for a prefilled share that actually
 * carries an image — the only case the server gates.
 */
class BridgeShareTokenProviderHolder {
    @Volatile
    var tokenProvider: (suspend (bridgeFingerprint: String) -> String?)? = null
}
