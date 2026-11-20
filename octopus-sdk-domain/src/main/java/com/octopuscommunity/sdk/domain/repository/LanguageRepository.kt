package com.octopuscommunity.sdk.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale

/**
 * Repository for managing the language/locale used by the Octopus SDK.
 *
 * This repository allows overriding the default system locale for all SDK operations,
 * including API requests and content display.
 */
interface LanguageRepository {
    /**
     * The current locale identifier used by the SDK.
     * Format: "language-country" (e.g., "en-US", "fr-FR")
     *
     * This value is used in the Accept-Language header for all API requests.
     */
    val localeIdentifierFlow: Flow<String>

    /**
     * The overridden locale, or null if using the system default.
     */
    val overriddenLocaleIdentifierFlow: Flow<String?>

    val overriddenLocaleFlow: Flow<Locale?>
        get() = overriddenLocaleIdentifierFlow.map { overriddenLocaleIdentifier ->
            overriddenLocaleIdentifier?.let {
                Locale.forLanguageTag(it.replace("_", "-"))
            }
        }

    suspend fun getLocaleIdentifier(): String = localeIdentifierFlow.first()

    /**
     * Override the default (system or app-based) locale.
     *
     * Some apps do not use the default way of handling the language which provides
     * the system/app-defined language by the user. If you have a custom setting inside
     * your app that does not set the system AppLanguage, you can call this function
     * to customize the language used (so Octopus does not use the system language but
     * yours instead).
     *
     * That being said, we recommend using the default system way of handling the locale,
     * so system alerts are displayed in the desired language.
     *
     * @param locale The locale to use, or null to use the system default.
     */
    suspend fun overrideDefaultLocale(locale: Locale?)
}
