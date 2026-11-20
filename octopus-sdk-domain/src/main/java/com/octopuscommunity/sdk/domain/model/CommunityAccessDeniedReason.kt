package com.octopuscommunity.sdk.domain.model

/**
 * Why the current user has no access to the community.
 *
 * Later versions of the SDK may add reasons: keep an `else` branch in a `when` over this type, so that a new reason
 * does not stop your code from compiling.
 */
enum class CommunityAccessDeniedReason {
    /** The user is not in the group of users the community is open to yet. */
    NOT_IN_COHORT,

    /** The community is disabled for a while and is expected to come back. */
    COMMUNITY_TEMPORARILY_DISABLED,

    /** The community is closed. */
    COMMUNITY_CLOSED,

    /** The member is excluded from the community. */
    MEMBER_EXCLUDED,

    /** The account of the member is banned. */
    ACCOUNT_BANNED,

    /** The account of the member was deleted. The host app can sign its member out of the community. */
    ACCOUNT_DELETED,

    /** The server gave no reason, or one this version of the SDK does not know. */
    UNKNOWN,
}
