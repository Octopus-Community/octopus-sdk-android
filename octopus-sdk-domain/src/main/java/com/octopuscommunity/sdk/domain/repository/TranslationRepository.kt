package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.OctopusItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

/**
 * Repository for the display mode of translated content: translated or original.
 *
 * Content is displayed translated unless the user toggled it.
 */
interface TranslationRepository {
    /** Observes the toggled display modes, by content id. */
    fun getTranslationsFlow(): Flow<Map<String, Boolean>>

    /** Observes whether [contentId] is displayed translated. */
    fun getTranslationFlow(contentId: String): Flow<Boolean>

    /** Whether [contentId] is displayed translated. Defaults to `true`. */
    suspend fun isTranslated(contentId: String) = getTranslationFlow(contentId).firstOrNull() ?: true

    /** Whether [item] is displayed translated. */
    suspend fun isTranslated(item: OctopusItem) = isTranslated(item.id)

    /**
     * Toggles the display mode for a given content ID.
     */
    suspend fun toggleTranslation(contentId: String): Boolean
}
