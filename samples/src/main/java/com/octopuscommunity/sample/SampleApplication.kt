package com.octopuscommunity.sample

import android.app.Application
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ConnectionMode
import com.octopuscommunity.sdk.domain.model.ProfileField

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        OctopusSDK.initialize(
            context = this,
            apiKey = BuildConfig.OCTOPUS_API_KEY,
            connectionMode = ConnectionMode.SSO(
                // Set the appManagedFields that matches your configuration here
                appManagedFields = ProfileField.ALL
            )
        )
    }
}