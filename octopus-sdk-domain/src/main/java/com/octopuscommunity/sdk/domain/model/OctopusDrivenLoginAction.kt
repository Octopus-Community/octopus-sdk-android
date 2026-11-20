package com.octopuscommunity.sdk.domain.model

/**
 * The user action that triggered an Octopus login request.
 *
 * Emitted as the `action_type` property of the `octopus_driven_login` analytics event; the value
 * sent to the backend is this entry's name (e.g. `POST`), derived server-side from the tracking
 * proto. Keep the set in sync across SDKs.
 */
enum class OctopusDrivenLoginAction {
    POST,
    COMMENT,
    REPLY,
    REACTION,
    VOTE,
    MODERATION,
    BLOCK_USER,
    VIEW_OWN_PROFILE,
}
