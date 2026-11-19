package com.octopuscommunity.sdk.sample.ui.navigation

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.octopuscommunity.sdk.domain.model.UserConfig
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.test.repository.FakeConnectionRepository
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.home.OctopusHomeContent
import com.octopuscommunity.sdk.ui.navigateToOctopusPost
import com.octopuscommunity.sdk.ui.octopusComposables
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.koin.compose.getKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PendingCommunityContentTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()
    private val testHost =
        object : ExternalResource() {
            override fun before() {
                val app = RuntimeEnvironment.getApplication()
                shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
            }
        }

    @get:Rule val rules: RuleChain = RuleChain.outerRule(testHost).around(compose)

    @Test
    fun `cold incoming post composes its community root before pushing and denial returns there`() {
        assertPendingContent(initialAccess = true)
    }

    @Test
    fun `cold incoming post while already denied returns to its community root`() {
        assertPendingContent(initialAccess = false)
    }

    private fun assertPendingContent(initialAccess: Boolean) {
        lateinit var nav: NavHostController
        lateinit var connection: FakeConnectionRepository
        var homeWasComposed = false
        var originalAccess: UserConfig? = null
        var accessCaptured = false
        val show = mutableStateOf(true)
        compose.setContent {
            if (show.value) {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    OctopusContent {
                        connection = getKoin().get<ConnectionRepository>() as FakeConnectionRepository
                        nav = rememberNavController()
                        OctopusTheme {
                            NavHost(
                                nav,
                                startDestination = "host",
                                enterTransition = { EnterTransition.None },
                                exitTransition = { ExitTransition.None },
                            ) {
                                composable("host") { Text("Host") }
                                navigation<CommunityGraph>(startDestination = CommunityTab) {
                                    composable<CommunityTab> {
                                        DisposableEffect(Unit) {
                                            homeWasComposed = true
                                            onDispose { }
                                        }
                                        OctopusHomeContent(nav, onNavigateToLogin = {})
                                    }
                                    octopusComposables(navController = nav, onNavigateToLogin = {})
                                }
                            }
                        }
                    }
                }
            }
        }
        try {
            compose.waitForIdle()
            compose.runOnIdle {
                originalAccess = runBlocking { connection.getUserConfig() }
                accessCaptured = true
                connection.setUserConfig(UserConfig(canAccessCommunity = initialAccess))
                compose.activity.lifecycleScope.launch {
                    nav.selectCommunityTabForContent()
                    nav.navigateToOctopusPost("notification-post")
                }
            }
            compose.waitForIdle()
            compose.runOnIdle { connection.setUserConfig(UserConfig(canAccessCommunity = false)) }
            compose.waitForIdle()
            compose.runOnIdle {
                assertTrue(homeWasComposed, "The notification must let Home compose before pushing the post")
                assertTrue(nav.currentDestination!!.hasRoute<CommunityTab>())
            }
        } finally {
            compose.runOnIdle {
                if (accessCaptured) connection.setUserConfig(originalAccess)
                show.value = false
            }
            compose.waitForIdle()
        }
    }
}
