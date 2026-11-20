package com.octopuscommunity.sdk.domain.model

/**
 * Info needed to open the post editor as an SDK entry point (Bridge
 * Share).
 *
 * When [prefilledPost] is `null`, the editor opens empty — equivalent
 * to today's in-SDK new-post flow. When non-null, the editor opens
 * prefilled with the provided text / image / group / CTA.
 *
 * @property prefilledPost The prefill payload, or `null` for an empty
 * editor.
 * @property bridgeShareTokenProvider Optional callback that signs this
 * prefilled share so the server accepts its image in a community
 * configured to forbid member pictures.
 *
 * When the member publishes this prefilled share carrying at least one
 * image, the SDK computes a fingerprint of the final content (text + CTA
 * + images) and invokes this provider, whatever the community setting:
 * only the server knows it for sure. Your backend must return a compact
 * JWT signed with HS256 (the same shared secret as your SSO tokens),
 * carrying that value in its `bridge_fingerprint` claim.
 * See the backend "Generate JWT for a bridge Share with an image" doc.
 *
 * Return `null` when you cannot sign: the post is sent unsigned and the
 * server decides. A community that allows pictures publishes it with
 * its images; one that forbids them refuses it and the editor shows an
 * error. Throwing does not send the post: the editor stays open and
 * shows an error, whatever the community setting. An unset provider
 * sends the post unsigned, as when it returns `null`.
 *
 * The provider is scoped to this editor session: the create-post
 * entry points wire it when the editor opens (a `suspend` lambda can't
 * ride the type-safe nav route, so it travels here rather than as a nav
 * argument), it is only consulted for this prefilled share, and it is
 * cleared when the editor closes — never registered globally.
 */
data class CreatePostScreenInfo(
    val prefilledPost: OctopusPrefilledPost? = null,
    val bridgeShareTokenProvider: (suspend (bridgeFingerprint: String) -> String?)? = null,
)
