package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.LanguageRepository
import java.util.Locale

/**
 * Use case that overrides the default locale and refreshes the community config.
 *
 * This ensures that when the user overrides the SDK locale, the community config is refetched
 * with the new language for proper translations.
 */
class OverrideDefaultLocaleUseCase(
    private val languageRepository: LanguageRepository,
    private val communityConfigRepository: CommunityConfigRepository
) {
    /**
     * Overrides the default locale and refreshes the community config.
     *
     * @param locale The locale to use, or null to use the system default.
     */
    suspend operator fun invoke(locale: Locale?) {
        languageRepository.overrideDefaultLocale(locale)
        communityConfigRepository.fetchCommunityConfig()
    }
}
