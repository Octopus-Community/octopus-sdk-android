package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.domain.repository.PreferenceRepository
import com.octopuscommunity.sdk.domain.utils.toBase64
import java.util.UUID

class PreferenceRepositoryImpl(
    private val preferencesDataStore: PreferencesDataStore
) : PreferenceRepository {

    override val installId: String
        get() = preferencesDataStore.installId ?: UUID.randomUUID().toBase64().also {
            preferencesDataStore.installId = it
        }

    override var hasDisplayedGamificationSheet: Boolean
        get() = preferencesDataStore.hasDisplayedGamificationSheet
        set(value) {
            preferencesDataStore.hasDisplayedGamificationSheet = value
        }
}