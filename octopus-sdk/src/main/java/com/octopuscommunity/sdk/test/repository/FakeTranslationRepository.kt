package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.repository.TranslationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [TranslationRepository]; content is displayed translated until toggled. */
class FakeTranslationRepository : TranslationRepository {
    private val _displayedTranslations = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    override fun getTranslationsFlow(): Flow<Map<String, Boolean>> = _displayedTranslations

    override fun getTranslationFlow(contentId: String): Flow<Boolean> = _displayedTranslations.map { it[contentId] ?: true }

    override suspend fun toggleTranslation(contentId: String): Boolean {
        var newValue = false
        _displayedTranslations.value =
            _displayedTranslations.value.toMutableMap().apply {
                newValue = !(this[contentId] ?: true)
                this[contentId] = newValue
            }
        return newValue
    }
}
