package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.data.utils.identifier
import com.octopuscommunity.sdk.domain.repository.LanguageRepository
import java.util.Locale

class LanguageRepositoryImpl(
    private val preferencesDataStore: PreferencesDataStore
) : LanguageRepository {

    override val overriddenLocaleIdentifierFlow
        get() = preferencesDataStore.overriddenLocaleIdentifierFlow

    override val localeIdentifierFlow
        get() = preferencesDataStore.localeIdentifierFlow

    override suspend fun overrideDefaultLocale(locale: Locale?) {
        preferencesDataStore.setOverriddenLocaleIdentifier(locale?.identifier)
    }
}
