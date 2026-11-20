package com.octopuscommunity.sdk

import android.content.Context
import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Fixed-count filesystem operations; never opens Room, decrypts credentials or vacuums tables. */
internal fun purgeCommunityStorage(
    previous: Context,
    replacement: Context,
    warn: (String) -> Unit,
    retainInstallationIdentity: Boolean = true,
) {
    if (retainInstallationIdentity) retainCommunityInstallationIdentity(previous, replacement)
    val databaseNames =
        buildList {
            add("octopus_database")
            // The legacy media3 database is shared with host caches and offline downloads.
            // Only CommunityStorageContext gives it an SDK-owned namespace.
            if (previous is CommunityStorageContext) add("exoplayer_internal.db")
        }
    for (name in databaseNames) {
        val database = previous.getDatabasePath(name)
        for (suffix in listOf("", "-wal", "-shm", "-journal")) deleteStorageFile(java.io.File(database.path + suffix))
    }
    for (name in listOf(
        "octopus_preferences.preferences_pb",
        "octopus_sessions.json",
        "octopus_settings.json",
        "octopus_secured_data.json",
    )) {
        for (suffix in listOf("", ".tmp", ".bak", ".unreadable")) {
            deleteStorageFile(previous.filesDir.resolve("datastore/$name$suffix"))
        }
    }
    // Never open EncryptedSharedPreferences (or Keystore). Clear its raw XML preferences instead,
    // including legacy login files, so no old identity can migrate into a later container.
    for (name in listOf(
        "octopus_preferences",
        "octopus_secured_preferences",
        "octopus_gamification_rules",
    )) {
        if (!previous
                .getSharedPreferences(name, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        ) {
            throw IOException("Unable to clear previous community preferences")
        }
    }
    // Retire only SDK-owned caches: legacy storage shares the host's cache directory.
    for (name in listOf("octopus_image_cache", "octopus_medias_cache")) {
        val cache = previous.cacheDir.resolve(name)
        if (cache.exists()) {
            val retired = cache.parentFile!!.resolve("octopus_retired_${UUID.randomUUID()}")
            if (!cache.renameTo(retired)) throw IOException("Unable to retire SDK cache")
            // Cache size is unbounded; only the directory rename belongs on the calling thread.
            cleanupScope.launch {
                try {
                    if (!retired.deleteRecursively()) warn("Unable to delete retired SDK cache")
                } catch (failure: Exception) {
                    warn("Unable to delete retired SDK cache (${failure.javaClass.simpleName})")
                }
            }
        }
    }
}

private fun deleteStorageFile(file: java.io.File) {
    if (file.exists() && !file.delete()) throw IOException("Unable to remove previous community storage")
}

/** Copies only the installation identity before the replacement graph starts. */
internal fun retainCommunityInstallationIdentity(
    previous: Context,
    replacement: Context,
) {
    // Preferences contain four small fields. Read only this store to retain install_id on the
    // automatic path. A legacy migration seeds the replacement; explicit reset still removes it.
    val installId = PreferencesDataStore(previous, cleanupScope).installId
    if (installId != null) {
        check(
            replacement
                .getSharedPreferences("octopus_preferences", Context.MODE_PRIVATE)
                .edit()
                .putString("install_id", installId)
                .commit(),
        ) { "Unable to retain installation identity" }
    }
}
