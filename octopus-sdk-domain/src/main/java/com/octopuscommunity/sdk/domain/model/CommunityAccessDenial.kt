package com.octopuscommunity.sdk.domain.model

/**
 * The denial of the community access as the SDK screens need it, with the details of a ban.
 *
 * Not surfaced on the SDK entry point: host apps read [reason].
 */
sealed interface CommunityAccessDenial {
    /** The reason host apps are given for this denial. */
    val reason: CommunityAccessDeniedReason

    /** The user is not in the group of users the community is open to yet. */
    data object NotInCohort : CommunityAccessDenial {
        override val reason = CommunityAccessDeniedReason.NOT_IN_COHORT
    }

    /** The community is disabled for a while and is expected to come back. */
    data object CommunityTemporarilyDisabled : CommunityAccessDenial {
        override val reason = CommunityAccessDeniedReason.COMMUNITY_TEMPORARILY_DISABLED
    }

    /** The community is closed. */
    data object CommunityClosed : CommunityAccessDenial {
        override val reason = CommunityAccessDeniedReason.COMMUNITY_CLOSED
    }

    /** The member is excluded from the community. */
    data object MemberExcluded : CommunityAccessDenial {
        override val reason = CommunityAccessDeniedReason.MEMBER_EXCLUDED
    }

    /** The account of the member is banned for breaking a policy of the community. */
    data class AccountBanned(
        /** Nickname of the banned account, when the server gives one. */
        val nickname: String?,
        /** Name of the policy the member broke. */
        val policyName: String,
        /** Text of that policy, as the community wrote it. Empty when there is none. */
        val policyDescription: String,
        /** Address the member can write to in order to appeal, when the community gives one. */
        val contactEmail: String?,
    ) : CommunityAccessDenial {
        override val reason get() = CommunityAccessDeniedReason.ACCOUNT_BANNED
    }

    /** The account of the member was deleted. */
    data object AccountDeleted : CommunityAccessDenial {
        override val reason = CommunityAccessDeniedReason.ACCOUNT_DELETED
    }

    /** The server gave no reason, or one this version of the SDK does not know. */
    data object Unknown : CommunityAccessDenial {
        override val reason = CommunityAccessDeniedReason.UNKNOWN
    }
}
