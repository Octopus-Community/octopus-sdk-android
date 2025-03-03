package com.octopuscommunity.sample.sso.octopusprofile

import android.app.Application
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ConnectionMode
import com.octopuscommunity.sdk.domain.model.ConnectionMode.SSO.Configuration
import com.octopuscommunity.sdk.domain.model.ConnectionMode.SSO.Configuration.ProfileField

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        OctopusSDK.initialize(
            context = this,
            apiKey = BuildConfig.OCTOPUS_API_KEY,
            connectionMode = ConnectionMode.SSO(
                configuration = Configuration(
                    // All Profile fields are managed by your app
                    appManagedFields = ProfileField.entries.toSet()
                )
            )
        )
    }
}