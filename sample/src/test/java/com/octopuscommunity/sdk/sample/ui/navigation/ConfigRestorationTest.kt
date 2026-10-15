package com.octopuscommunity.sdk.sample.ui.navigation

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ConfigRestorationTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()

    // The app manifest declares MainActivity, which needs the real SDK. Use a plain test host.
    private val testHost =
        object : ExternalResource() {
            override fun before() {
                val application = RuntimeEnvironment.getApplication()
                shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
            }
        }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(testHost).around(compose)

    @Test
    fun `restored Configuration survives the SDK-ready effect and Back returns to Main`() {
        val restoration = StateRestorationTester(compose)
        lateinit var navController: NavHostController
        restoration.setContent {
            navController = rememberContentNavController()
            val entry by navController.currentBackStackEntryAsState()
            val hasGraph = entry != null
            // Same initialization pass as MainActivity: runs again when the composition restores.
            LaunchedEffect(hasGraph) {
                if (hasGraph) navController.finishSdkOnboarding(hasPendingConfigLink = false)
            }
            NavHost(navController, startDestination = MainRoute) {
                composable<MainRoute> {}
                composable<ConfigRoute> {}
            }
        }
        compose.runOnIdle { navController.navigate(ConfigRoute()) }
        lateinit var configEntryId: String
        compose.runOnIdle {
            assertTrue(navController.currentDestination!!.hasRoute<ConfigRoute>())
            configEntryId = navController.currentBackStackEntry!!.id
        }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertTrue(navController.currentDestination!!.hasRoute<ConfigRoute>())
            assertEquals(configEntryId, navController.currentBackStackEntry!!.id)
            assertTrue(navController.popBackStack())
            assertTrue(navController.currentDestination!!.hasRoute<MainRoute>())
            assertFalse(navController.popBackStack())
        }
    }

    @Test
    fun `SDK readiness still completes onboarding including a pending integration link`() {
        lateinit var navController: NavHostController
        compose.setContent {
            navController = rememberContentNavController()
            NavHost(navController, startDestination = ConfigRoute()) {
                composable<MainRoute> {}
                composable<ConfigRoute> {}
                composable<HostIntegrationRoute> {}
            }
        }
        compose.runOnIdle {
            navController.navigate(HostIntegrationRoute)
            navController.finishSdkOnboarding(hasPendingConfigLink = false)
            assertTrue(navController.currentDestination!!.hasRoute<HostIntegrationRoute>())
            navController.finishSdkOnboarding(hasPendingConfigLink = true)
            assertTrue(navController.currentDestination!!.hasRoute<MainRoute>())
            assertFalse(navController.popBackStack())
        }
    }
}
