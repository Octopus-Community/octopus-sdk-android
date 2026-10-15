package com.octopuscommunity.sdk.sample.ui.scenarios

import kotlin.test.Test
import kotlin.test.assertEquals

class ScenarioLiveSubtitleTest {
    @Test
    fun `Unified Profile card changes in both directions without Apply`() {
        fun subtitle(active: Boolean) =
            ScenarioCatalog
                .liveSections(active)
                .values
                .flatten()
                .single { it.id == "unifiedProfile" }
                .subtitle
        assertEquals("Profile taps open native profiles", subtitle(false))
        assertEquals("Profile taps open Client profile", subtitle(true))
        assertEquals("Profile taps open native profiles", subtitle(false))
    }
}
