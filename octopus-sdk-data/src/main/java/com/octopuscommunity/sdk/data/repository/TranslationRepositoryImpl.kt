package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.sdk.data.local.datastore.SessionsDataStore
import com.octopuscommunity.sdk.domain.repository.TranslationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

open class TranslationRepositoryImpl(
    private val sessionsDataStore: SessionsDataStore
) : TranslationRepository {
    override fun getTranslationsFlow(): Flow<Map<String, Boolean>> =
        sessionsDataStore.appSessionFlow.map { it?.toggledTranslations ?: emptyMap() }

    override fun getTranslationFlow(contentId: String): Flow<Boolean> =
        getTranslationsFlow().map { it[contentId] ?: true }

    override suspend fun toggleTranslation(contentId: String) =
        sessionsDataStore.updateAppSession { (appSession, _) ->
            appSession?.copy(
                toggledTranslations = appSession.toggledTranslations
                        + (contentId to !(appSession.toggledTranslations[contentId] ?: true))
            )
        }?.toggledTranslations[contentId] ?: true
}