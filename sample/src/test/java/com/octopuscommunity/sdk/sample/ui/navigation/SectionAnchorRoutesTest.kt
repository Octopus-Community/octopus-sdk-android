package com.octopuscommunity.sdk.sample.ui.navigation

import android.app.Application
import android.net.Uri
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.octopuscommunity.sdk.sample.ui.config.ConfigSection
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenarioSection
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The section anchors ride on real Navigation routes: an enum argument that must survive the
 * Bundle round-trip, and the Config deep link that must still match without its query.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SectionAnchorRoutesTest {
    private fun controller(): NavHostController =
        NavHostController(RuntimeEnvironment.getApplication()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            graph =
                createGraph(startDestination = MainRoute) {
                    composable<MainRoute> {}
                    composable<ConfigRoute>(
                        deepLinks = listOf(navDeepLink { uriPattern = ConfigRoute.DEEP_LINK_PATTERN }),
                    ) {}
                    composable<ScenariosTab> {}
                }
        }

    @Test
    fun `every Config section survives the route round-trip`() {
        val controller = controller()
        for (section in listOf(null) + ConfigSection.entries) {
            controller.navigate(ConfigRoute(section))
            assertEquals(section, controller.currentBackStackEntry!!.toRoute<ConfigRoute>().section)
        }
    }

    @Test
    fun `every Scenarios section survives the route round-trip`() {
        val controller = controller()
        for (section in listOf(null) + ScenarioSection.entries) {
            controller.navigate(ScenariosTab(section))
            assertEquals(section, controller.currentBackStackEntry!!.toRoute<ScenariosTab>().section)
        }
    }

    @Test
    fun `the Config deep link opens the top without a query and a section with one`() {
        val controller = controller()

        controller.navigate(Uri.parse("octopus-sample://config"))
        assertTrue(controller.currentDestination!!.hasRoute<ConfigRoute>())
        assertNull(controller.currentBackStackEntry!!.toRoute<ConfigRoute>().section)

        controller.navigate(Uri.parse("octopus-sample://config?section=INTEGRATION"))
        assertEquals(
            ConfigSection.INTEGRATION,
            controller.currentBackStackEntry!!.toRoute<ConfigRoute>().section,
        )
    }

    @Test
    fun `an optional argument only adds a query suffix to the destination route`() {
        // MainScaffold compares `destination.route.substringBefore('?')` with the qualified name to
        // find the current tab; that holds only while the argument stays a query parameter.
        val controller = controller()
        controller.navigate(ScenariosTab(ScenarioSection.NOTIFICATIONS))
        assertEquals(
            ScenariosTab::class.qualifiedName,
            controller.currentDestination!!.route!!.substringBefore('?'),
        )
    }
}
