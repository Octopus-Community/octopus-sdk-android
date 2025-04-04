package com.octopuscommunity.sample.sso.octopus

import android.app.Application
import com.octopuscommunity.sample.sso.octopus.data.TokenProvider
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ConnectionMode

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        OctopusSDK.initialize(
            context = this,
            connectionMode = ConnectionMode.SSO(
                userTokenProvider = { user ->
                    // Your Octopus token provider logic here
                    TokenProvider.getToken(userId = user.id)
                }
            ),
            apiKey = BuildConfig.OCTOPUS_API_KEY
        )
    }
}