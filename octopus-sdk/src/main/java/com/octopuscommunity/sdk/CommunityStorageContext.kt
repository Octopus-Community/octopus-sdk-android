package com.octopuscommunity.sdk

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import java.io.File

/** Isolates a new community from both old files and live DataStore instances holding old data. */
internal class CommunityStorageContext(
    context: Context,
    private val storageId: String,
) : ContextWrapper(context) {
    override fun getApplicationContext(): Context = this

    override fun getFilesDir(): File = baseContext.filesDir.resolve("octopus_communities/$storageId").apply { mkdirs() }

    override fun getCacheDir(): File = baseContext.cacheDir.resolve("octopus_communities/$storageId").apply { mkdirs() }

    private fun name(name: String) = "octopus_$storageId-$name"

    override fun getDatabasePath(name: String): File = baseContext.getDatabasePath(name(name))

    override fun deleteDatabase(name: String): Boolean = baseContext.deleteDatabase(name(name))

    override fun openOrCreateDatabase(
        name: String,
        mode: Int,
        factory: CursorFactory?,
    ): SQLiteDatabase = baseContext.openOrCreateDatabase(name(name), mode, factory)

    override fun openOrCreateDatabase(
        name: String,
        mode: Int,
        factory: CursorFactory?,
        errorHandler: DatabaseErrorHandler?,
    ): SQLiteDatabase = baseContext.openOrCreateDatabase(name(name), mode, factory, errorHandler)

    override fun getSharedPreferences(
        name: String,
        mode: Int,
    ) = baseContext.getSharedPreferences(name(name), mode)

    companion object {
        fun forStorage(
            context: Context,
            storageId: String,
        ): Context = if (storageId.isEmpty()) context else CommunityStorageContext(context, storageId)
    }
}
