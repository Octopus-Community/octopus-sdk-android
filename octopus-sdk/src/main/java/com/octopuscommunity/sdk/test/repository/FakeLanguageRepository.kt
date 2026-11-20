package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.repository.LanguageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.Locale

/** In-memory [LanguageRepository]; the locale is `en-US` until overridden. */
class FakeLanguageRepository : LanguageRepository {
    override val localeIdentifierFlow: Flow<String>
        get() =
            _overriddenLocaleIdentifier.map {
                it ?: "en-US"
            }

    private var _overriddenLocaleIdentifier: MutableStateFlow<String?> = MutableStateFlow(null)
    override val overriddenLocaleIdentifierFlow: Flow<String?> get() = _overriddenLocaleIdentifier

    override suspend fun overrideDefaultLocale(locale: Locale?) {
        _overriddenLocaleIdentifier.value = locale?.let { formatLocaleIdentifier(it) }
    }

    private fun formatLocaleIdentifier(locale: Locale): String {
        val language = locale.language
        val country = locale.country
        return if (country.isNotEmpty()) "$language-$country" else language
    }
}
