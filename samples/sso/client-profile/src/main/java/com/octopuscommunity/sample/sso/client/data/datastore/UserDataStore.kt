package com.octopuscommunity.sample.sso.client.data.datastore

import android.content.Context
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.octopuscommunity.sample.sso.client.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

private val Context.dataStore by dataStore(
    fileName = "user.json",
    serializer = object : Serializer<User?> {
        override val defaultValue = null

        override suspend fun readFrom(input: InputStream): User? = withContext(Dispatchers.IO) {
            runCatching {
                input.use {
                    Json.decodeFromString(User.serializer(), it.readBytes().decodeToString())
                }
            }.getOrNull()
        }

        override suspend fun writeTo(t: User?, output: OutputStream) =
            withContext(Dispatchers.IO) {
                output.use {
                    if (t != null) {
                        it.write(Json.encodeToString(User.serializer(), t).encodeToByteArray())
                    } else {
                        it.write("{}".encodeToByteArray())
                    }
                }
            }
    }
)

/**
 * User store
 * Only used to keep the logged in app user across app restart.
 * You probably do not need this class as you already have your own way to store your users.
 */
class UserDataStore(private val context: Context) {

    val user get() = context.dataStore.data

    suspend fun updateUser(transform: (t: User?) -> User?) = context.dataStore.updateData(transform)
}