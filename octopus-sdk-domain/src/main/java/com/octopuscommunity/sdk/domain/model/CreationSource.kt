package com.octopuscommunity.sdk.domain.model

/**
 * Marks how a post creation request was initiated — surfaced on
 * `PutPostRequest.creation_source` for backend analytics.
 *
 * Not exposed as part of the host-app API surface: it is set by the
 * SDK based on the entry point the host used (regular new post →
 * [USER]; Bridge Share with a `prefilledPost` payload →
 * [PREFILLED_FROM_CLIENT]).
 */
enum class CreationSource {
    /** User opened the editor on their own (regular new-post flow). */
    USER,

    /** Editor opened with a payload prefilled by the host app via Bridge Share. */
    PREFILLED_FROM_CLIENT,
}
