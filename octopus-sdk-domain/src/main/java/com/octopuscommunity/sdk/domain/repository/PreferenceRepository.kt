package com.octopuscommunity.sdk.domain.repository

/** Repository for the SDK's local preferences. */
interface PreferenceRepository {
    /** Id of this SDK installation. */
    val installId: String

    /** Whether the gamification sheet was already shown. */
    var hasDisplayedGamificationSheet: Boolean
}
