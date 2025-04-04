package com.octopuscommunity.sample.sso.client

import android.app.Application
import com.octopuscommunity.sample.sso.client.data.TokenProvider
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ConnectionMode
import com.octopuscommunity.sdk.domain.model.ProfileField

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        OctopusSDK.initialize(
            context = this,
            connectionMode = ConnectionMode.SSO(
                configuration = ConnectionMode.SSO.Configuration(
                    // Set the appManagedFields that matches your configuration here
                    appManagedFields = ProfileField.ALL
                ),
                userTokenProvider = { user ->
                    // Your Octopus token provider logic here
                    TokenProvider.getToken(userId = user.id)
                }
            ),
            apiKey = BuildConfig.OCTOPUS_API_KEY
        )
    }
}