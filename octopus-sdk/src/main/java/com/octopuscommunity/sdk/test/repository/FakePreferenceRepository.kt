package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.repository.PreferenceRepository
import java.util.UUID

/** In-memory [PreferenceRepository] with a random install id. */
open class FakePreferenceRepository : PreferenceRepository {
    override var installId = UUID.randomUUID().toString()
    override var hasDisplayedGamificationSheet = false
}
