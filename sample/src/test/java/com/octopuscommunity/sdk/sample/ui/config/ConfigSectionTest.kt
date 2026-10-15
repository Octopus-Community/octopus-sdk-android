package com.octopuscommunity.sdk.sample.ui.config

import com.octopuscommunity.sdk.sample.ui.scenarios.ScenarioSection
import com.octopuscommunity.sdk.sample.ui.scenarios.scenarioSectionItemIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigSectionTest {
    @Test
    fun `server falls back to community only where the server section is absent`() {
        assertEquals(ConfigSection.SERVER, ConfigSection.SERVER.resolve(serverSectionShown = true))
        assertEquals(ConfigSection.COMMUNITY, ConfigSection.SERVER.resolve(serverSectionShown = false))
        ConfigSection.entries.filter { it != ConfigSection.SERVER }.forEach { section ->
            assertEquals(section, section.resolve(serverSectionShown = false))
            assertEquals(section, section.resolve(serverSectionShown = true))
        }
    }

    @Test
    fun `no anchor keeps every section collapsed`() {
        ConfigSection.entries.forEach {
            assertFalse(it.isInitiallyExpanded(anchor = null), "$it")
        }
    }

    @Test
    fun `an anchor opens its own section and nothing else`() {
        // The reported case: a legacy `config?section=INTEGRATION` link opened before the SDK is up.
        val anchor = ConfigSection.INTEGRATION
        val open = ConfigSection.entries.filter { it.isInitiallyExpanded(anchor) }
        assertEquals(listOf(ConfigSection.INTEGRATION), open)
        assertTrue(ConfigSection.COMMUNITY.isInitiallyExpanded(ConfigSection.COMMUNITY))
    }

    @Test
    fun `server environment comes before community, in display order`() {
        assertEquals(
            listOf(ConfigSection.SERVER, ConfigSection.COMMUNITY, ConfigSection.AUTH),
            ConfigSection.entries.take(3),
        )
    }

    @Test
    fun `section tags are unique`() {
        val tags = ConfigSection.entries.map { it.tag }
        assertEquals(tags.size, tags.toSet().size)
    }

    @Test
    fun `scenario section index counts the live panel and skips filtered sections`() {
        val all = ScenarioSection.entries
        assertEquals(4, scenarioSectionItemIndex(all, ScenarioSection.NOTIFICATIONS, hasLivePanel = true))
        assertEquals(0, scenarioSectionItemIndex(all, ScenarioSection.SSO, hasLivePanel = false))
        val filtered = listOf(ScenarioSection.PRESENTATION, ScenarioSection.NOTIFICATIONS)
        assertEquals(1, scenarioSectionItemIndex(filtered, ScenarioSection.NOTIFICATIONS, hasLivePanel = false))
        assertNull(scenarioSectionItemIndex(filtered, ScenarioSection.SSO, hasLivePanel = true))
    }
}
