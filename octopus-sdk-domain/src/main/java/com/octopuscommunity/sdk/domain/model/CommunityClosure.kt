package com.octopuscommunity.sdk.domain.model

/**
 * The community is closed by Octopus: the SDK makes no call to the server until [nextCheckAtMillis], when it checks
 * the config again.
 *
 * @property kind whether the community is closed for good or disabled for a while.
 * @property message the text to display to the member, under the SDK's own title.
 * @property nextCheckAtMillis the epoch millis before which the SDK does not check the community again.
 */
data class CommunityClosure(
    val kind: Kind,
    val message: String,
    val nextCheckAtMillis: Long,
) {
    /** Why the community is closed. */
    enum class Kind {
        /** Closed for good, as far as the SDK knows. */
        Closed,

        /** Disabled for a while. */
        TemporarilyDisabled,
    }
}
