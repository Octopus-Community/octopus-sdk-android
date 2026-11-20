package com.octopuscommunity.sdk.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import kotlin.properties.ReadOnlyProperty

/** Share a store only with callers using the same file, including across SDK containers. */
internal fun <T> contextDataStore(
    fileName: String,
    serializer: Serializer<T>,
): ReadOnlyProperty<Context, DataStore<T>> {
    val stores = mutableMapOf<String, DataStore<T>>()
    return ReadOnlyProperty { context, _ ->
        val file = context.applicationContext.dataStoreFile(fileName)
        synchronized(stores) {
            stores.getOrPut(file.absolutePath) {
                DataStoreFactory.create(serializer = serializer, produceFile = { file })
            }
        }
    }
}
