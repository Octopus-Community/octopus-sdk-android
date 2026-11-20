package com.octopuscommunity.sdk.ui.common.viewmodel

import android.content.Context

/** UI-owned persistence, matching iOS's rules manager without extending the host repository API. */
internal interface GamificationHomeAppearanceCounter {
    var count: Int
}

internal class GamificationHomeAppearanceStore(
    context: Context,
) : GamificationHomeAppearanceCounter {
    private val preferences = context.applicationContext.getSharedPreferences("octopus_gamification_rules", Context.MODE_PRIVATE)

    override var count: Int
        get() = preferences.getInt("home_appearance_count", 0)
        set(value) {
            preferences.edit().putInt("home_appearance_count", value).apply()
        }
}
