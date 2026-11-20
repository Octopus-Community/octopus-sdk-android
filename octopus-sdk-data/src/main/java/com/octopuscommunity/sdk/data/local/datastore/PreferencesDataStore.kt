package com.octopuscommunity.sdk.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore.Keys.INSTALL_ID
import com.octopuscommunity.sdk.data.utils.localeIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.properties.ReadOnlyProperty

private val Context.dataStore by preferencesDelegate()

private fun preferencesDelegate(): ReadOnlyProperty<Context, DataStore<Preferences>> {
    val stores = mutableMapOf<String, DataStore<Preferences>>()
    return ReadOnlyProperty<Context, DataStore<Preferences>> { context, _ ->
        val application = context.applicationContext
        val file = application.preferencesDataStoreFile("octopus_preferences")
        synchronized(stores) {
            stores.getOrPut(file.absolutePath) {
                PreferenceDataStoreFactory.create(
                    migrations = listOf(SharedPreferencesMigration(application, "octopus_preferences")),
                    produceFile = { file },
                )
            }
        }
    }
}

class PreferencesDataStore(
    val context: Context,
    val sdkScope: CoroutineScope,
) {
    private object Keys {
        val INSTALL_ID = stringPreferencesKey("install_id")
        val COMMUNITY_START_COUNT = intPreferencesKey("community_start_count")
        val HAS_DISPLAYED_GAMIFICATION_SHEET =
            booleanPreferencesKey("has_displayed_gamification_sheet")
        val OVERRIDDEN_LOCALE_IDENTIFIER = stringPreferencesKey("overridden_locale_identifier")
    }

    val dataFlow get() = context.dataStore.data
    val data get() = runBlocking { dataFlow.first() }

    // One blocking read at construction, shared by the three initialisers below.
    private val initialData = data

    private var installIdValue = initialData[INSTALL_ID]

    var installId: String?
        get() = installIdValue
        set(value) {
            installIdValue = value
            edit { preferences ->
                if (value != null) {
                    preferences[INSTALL_ID] = value
                } else {
                    preferences.remove(INSTALL_ID)
                }
            }
        }

    private var startCount = initialData[Keys.COMMUNITY_START_COUNT] ?: 0

    var communityStartCount: Int
        get() = startCount
        set(value) {
            startCount = value
            edit { preferences ->
                preferences[Keys.COMMUNITY_START_COUNT] = value
            }
        }

    private var gamificationSheetDisplayed = initialData[Keys.HAS_DISPLAYED_GAMIFICATION_SHEET] ?: false

    var hasDisplayedGamificationSheet: Boolean
        get() = gamificationSheetDisplayed
        set(value) {
            gamificationSheetDisplayed = value
            edit { preferences ->
                preferences[Keys.HAS_DISPLAYED_GAMIFICATION_SHEET] = value
            }
        }

    val overriddenLocaleIdentifierFlow: Flow<String?>
        get() = context.dataStore.data.map { it[Keys.OVERRIDDEN_LOCALE_IDENTIFIER] }

    val localeIdentifierFlow: Flow<String>
        get() = overriddenLocaleIdentifierFlow.map { it ?: context.localeIdentifier }

    suspend fun getLocaleIdentifier() = localeIdentifierFlow.first()

    suspend fun setOverriddenLocaleIdentifier(identifier: String?) {
        context.dataStore.edit { preferences ->
            if (identifier != null) {
                preferences[Keys.OVERRIDDEN_LOCALE_IDENTIFIER] = identifier
            } else {
                preferences.remove(Keys.OVERRIDDEN_LOCALE_IDENTIFIER)
            }
        }
    }

    fun edit(transform: suspend (MutablePreferences) -> Unit) =
        sdkScope.launch {
            context.dataStore.edit(transform)
        }

    /** Clears user preferences while keeping the installation identity and host locale. */
    suspend fun clearUserData() {
        gamificationSheetDisplayed = false
        context.dataStore.edit { it.remove(Keys.HAS_DISPLAYED_GAMIFICATION_SHEET) }
    }

    /**
     * Clears every preference in one write that has landed when this returns. Going through the
     * setters instead launched one write each, which could land after a later write and undo it.
     */
    suspend fun clear() {
        installIdValue = null
        startCount = 0
        gamificationSheetDisplayed = false
        context.dataStore.edit { preferences ->
            preferences.remove(INSTALL_ID)
            preferences.remove(Keys.COMMUNITY_START_COUNT)
            preferences.remove(Keys.HAS_DISPLAYED_GAMIFICATION_SHEET)
            preferences.remove(Keys.OVERRIDDEN_LOCALE_IDENTIFIER)
        }
    }
}
