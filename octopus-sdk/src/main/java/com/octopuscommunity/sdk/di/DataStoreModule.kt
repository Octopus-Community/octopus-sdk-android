package com.octopuscommunity.sdk.di

import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.data.local.datastore.SecuredDataStore
import com.octopuscommunity.sdk.data.local.datastore.SessionsDataStore
import com.octopuscommunity.sdk.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal fun dataStoreModule(sdkScope: CoroutineScope) = module {
    single { PreferencesDataStore(context = androidContext(), sdkScope = sdkScope) }
    single { SecuredDataStore(androidContext(), sdkScope = sdkScope) }
    single { SessionsDataStore(context = androidContext(), sdkScope = sdkScope) }
    single { SettingsDataStore(context = androidContext(), sdkScope = sdkScope) }
}