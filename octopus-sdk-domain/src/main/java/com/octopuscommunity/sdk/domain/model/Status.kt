package com.octopuscommunity.sdk.domain.model

/** Moderation status of an item. */
sealed class Status {
    /** The status is unknown. */
    data object Unknown : Status()

    /** The item is published. */
    data object Published : Status()

    /** The item was moderated for [reasons]. */
    data class Moderated(
        val reasons: List<Moderation.Reason>,
    ) : Status()
}
