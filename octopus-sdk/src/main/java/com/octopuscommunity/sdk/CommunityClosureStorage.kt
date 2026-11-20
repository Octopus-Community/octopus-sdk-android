package com.octopuscommunity.sdk

import android.content.Context
import android.content.SharedPreferences
import com.octopuscommunity.sdk.data.repository.CommunityClosureRepositoryImpl

/**
 * The preferences that keep the closure of the community of [apiKey], one file per API key.
 *
 * They live outside the storage of the community, which [context] may be: switching to another community empties that
 * storage, and coming back must still find the community closed instead of calling it until its config answers. A
 * reset keeps them too, for the same reason.
 */
internal fun communityClosurePreferences(
    context: Context,
    apiKey: String,
): SharedPreferences {
    val root = (context as? CommunityStorageContext)?.baseContext ?: context
    return root.getSharedPreferences(
        "${CommunityClosureRepositoryImpl.PREFERENCES_NAME}_${ApiKeyStore.fingerprint(apiKey)}",
        Context.MODE_PRIVATE,
    )
}
