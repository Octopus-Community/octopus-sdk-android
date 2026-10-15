package com.octopuscommunity.sdk.sample.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.octopuscommunity.sdk.OctopusProfile
import com.octopuscommunity.sdk.sample.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class UserData(
    val hasData: Boolean = false,
    val user: User? = null,
    val isSignedOut: Boolean = false,
)

internal object UserDataSerializer : Serializer<UserData> {
    override val defaultValue = UserData()

    override suspend fun readFrom(input: InputStream): UserData =
        withContext(Dispatchers.IO) {
            runCatching {
                input.use {
                    val content = it.readBytes().decodeToString().trim()
                    if (content.isBlank() || content == "{}") {
                        UserData()
                    } else {
                        Json.decodeFromString(UserData.serializer(), content)
                    }
                }
            }.getOrElse { UserData() }
        }

    override suspend fun writeTo(
        t: UserData,
        output: OutputStream,
    ) = withContext(Dispatchers.IO) {
        output.use {
            it.write(Json.encodeToString(UserData.serializer(), t).encodeToByteArray())
        }
    }
}

/** DataStore for User persistence with JSON serialization. */
private val Context.dataStore by dataStore(
    fileName = "user.json",
    serializer = UserDataSerializer,
)

/**
 * Sample user data store for SSO mode.
 *
 * Persists logged-in user across app restarts. In a real app, you likely already
 * have your own user management system.
 */
class UserDataStore(
    context: Context,
    private val store: DataStore<UserData> = context.dataStore,
) {
    /**
     * Reactive flow of user state changes.
     */
    val userFlow get() = store.data.map { it.user }

    suspend fun getUser() = userFlow.first()

    /** Stored identity to reconnect on launch or after a community switch. */
    suspend fun getUserToConnect() =
        store.data.first().let {
            if (it.isSignedOut) null else it.user
        }

    suspend fun isSignedOut() =
        store.data
            .first()
            .isSignedOut

    /** Remembers a disconnect without removing the app's user. */
    suspend fun setSignedOut(signedOut: Boolean) =
        store.updateData {
            it.copy(isSignedOut = signedOut)
        }

    /**
     * Updates user data atomically. Callers must explicitly decide whether to clear sign-out:
     * true for sign-in or reset, false for edits that keep the current connection state.
     */
    suspend fun updateUser(
        clearSignOut: Boolean,
        transform: (t: User?) -> User?,
    ) = store.updateData {
        transform(it.user).let { user ->
            it.copy(hasData = user != null, user = user, isSignedOut = if (clearSignOut) false else it.isSignedOut)
        }
    }

    /**
     * Mirrors grants only for the stored host identity, checked inside the write transaction.
     * Guests and restored sessions without a host identity leave the saved SSO grants intact.
     * Uses the emitted profile directly; it never waits for SDK initialization or re-reads a
     * connection that may already belong to a different user.
     */
    suspend fun mirrorGrantedEntitlements(profile: OctopusProfile?): UserData {
        if (profile?.clientUserId == null) return store.data.first()
        return store.updateData { data ->
            val user = data.user
            if (data.isSignedOut ||
                user == null ||
                profile.clientUserId != user.id ||
                user.entitlements.toSet() == profile.entitlements
            ) {
                data
            } else {
                data.copy(user = user.copy(entitlements = profile.entitlements.toList()))
            }
        }
    }

    suspend fun clear() = updateUser(clearSignOut = true) { null }
}
