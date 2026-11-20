package com.octopuscommunity.sdk.data.local.datastore

import android.content.Context
import com.octopuscommunity.sdk.data.local.data.SettingsData
import com.octopuscommunity.sdk.data.utils.dataStoreSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by contextDataStore(
    fileName = "octopus_settings.json",
    serializer = SettingsData.serializer().dataStoreSerializer(SettingsData()),
)

class SettingsDataStore(
    val context: Context,
    val sdkScope: CoroutineScope,
) {
    val data get() = context.dataStore.data
    val notificationsSettings get() = data.map { it.notifications }

    fun update(transform: (t: SettingsData) -> SettingsData) {
        sdkScope.launch {
            context.dataStore.updateData {
                transform(it)
            }
        }
    }

    fun updateNotificationsSettings(settings: SettingsData.Notifications) =
        update {
            it.copy(notifications = settings)
        }

    /** Clears user settings before returning, independently of the SDK background scope. */
    suspend fun clearUserData() {
        context.dataStore.updateData { SettingsData() }
    }

    fun clear() = update { SettingsData() }
}
