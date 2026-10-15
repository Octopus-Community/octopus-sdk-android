package com.octopuscommunity.sdk.sample.ui.navigation

import android.app.Application
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import com.octopuscommunity.sdk.ui.OctopusDestination
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Which tab-graph moves fade through (a bottom-bar switch) and which slide (a push or pop). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TabSwitchTest {
    // The routes exactly as Navigation renders them for these destinations (arguments included),
    // read from a graph shaped like MainScaffold's tab graph.
    private val graph: NavGraph =
        NavHostController(RuntimeEnvironment.getApplication())
            .apply { navigatorProvider.addNavigator(ComposeNavigator()) }
            .createGraph(startDestination = ScenariosTab()) {
                composable<ScenariosTab> {}
                composable<ScenarioDetailRoute> {}
                composable<CommunityTab> {}
                composable<SettingsTab> {}
                composable<AccountRoute> {}
                composable<AboutRoute> {}
                composable<OctopusDestination.PostDetails> {}
            }

    private inline fun <reified T : Any> route(): String = graph.first { it.hasRoute<T>() }.route!!

    private val scenarios = route<ScenariosTab>()
    private val scenarioDetail = route<ScenarioDetailRoute>()
    private val community = route<CommunityTab>()
    private val settings = route<SettingsTab>()
    private val about = route<AboutRoute>()
    private val account = route<AccountRoute>()
    private val post = route<OctopusDestination.PostDetails>()

    @Test
    fun `switching to or from restored Community details fades through while drill down slides`() {
        assertTrue(isTabSwitch(post, settings))
        assertTrue(isTabSwitch(settings, post))
        assertTrue(isTabSwitch(scenarios, post))
        assertFalse(isTabSwitch(community, post))
        assertFalse(isTabSwitch(post, community, isPop = true))
    }

    @Test
    fun `a move between two tab roots is a tab switch`() {
        assertTrue(isTabSwitch(scenarios, community))
        assertTrue(isTabSwitch(community, settings))
        assertTrue(isTabSwitch(settings, scenarios))
    }

    @Test
    fun `a tab tapped from a screen pushed inside another tab is a tab switch`() {
        assertTrue(isTabSwitch(scenarioDetail, settings))
        assertTrue(isTabSwitch(scenarioDetail, community))
        assertTrue(isTabSwitch(about, scenarios))
        assertTrue(isTabSwitch(about, community))
    }

    @Test
    fun `a push or a pop inside one tab is not a tab switch`() {
        assertFalse(isTabSwitch(scenarios, scenarioDetail))
        assertFalse(isTabSwitch(scenarioDetail, scenarios))
        assertFalse(isTabSwitch(settings, about))
        assertFalse(isTabSwitch(about, settings))
        assertFalse(isTabSwitch(settings, account))
    }

    @Test
    fun `a screen pushed from another tab's root is not a tab switch`() {
        // Scenarios' "connect a user" pushes Account, which belongs to the Settings tree.
        assertFalse(isTabSwitch(scenarios, account))
        assertFalse(isTabSwitch(null, scenarioDetail))
    }

    @Test
    fun `a move from a route no tab claims is not a tab switch`() {
        assertFalse(isTabSwitch(null, scenarios))
        assertFalse(isTabSwitch("com.example.Unknown", community))
    }

    @Test
    fun `popping Account back to the root that pushed it is not a tab switch`() {
        assertFalse(isTabSwitch(account, scenarios, isPop = true))
        assertFalse(isTabSwitch(account, community, isPop = true))
        assertFalse(isTabSwitch(account, settings, isPop = true))
        // A bottom-bar tap from Account (pushed inside Settings) to Community still fades through.
        assertTrue(isTabSwitch(account, community))
    }
}
