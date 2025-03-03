package com.octopuscommunity.sample.embed

import android.app.Application
import com.octopuscommunity.sdk.OctopusSDK

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        OctopusSDK.initialize(
            context = this,
            apiKey = BuildConfig.OCTOPUS_API_KEY
        )
    }
}