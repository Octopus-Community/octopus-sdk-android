package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import android.app.Application
import android.content.ComponentName
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.sample.data.CommunitySessionTracker
import com.octopuscommunity.sdk.sample.data.datastore.InMemorySampleDataStores
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.data.usecase.CommunityConnectUseCase
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenarioDetailScreen
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenariosViewModel
import com.octopuscommunity.sdk.sample.ui.theme.SampleAppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class HostLifecycleScenarioNavigationTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()
    private val host =
        object : ExternalResource() {
            override fun before() {
                val app = RuntimeEnvironment.getApplication()
                shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
                ShadowLog.clear()
            }
        }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(host).around(compose)

    @Test
    fun `preset 2 keeps the actual scenario detail and its completed result visible`() {
        val stores = InMemorySampleDataStores(RuntimeEnvironment.getApplication())
        val config = SDKConfig(SDKConfig.AuthKind.SSO(), customCommunity = true, customApiKey = "test-only")
        runBlocking { stores.configs.updateConfig { config } }
        val viewModels = ViewModelStore()
        val initialized = OctopusSDK.isInitialised
        // The SDK's public preview/test graph contains only fakes. No initialize or network.
        OctopusSDK.isInitialisedFlow.value = true
        lateinit var parentNav: NavHostController
        try {
            compose.setContent {
                SampleAppTheme {
                    CompositionLocalProvider(LocalInspectionMode provides true) {
                        val nav = rememberNavController().also { parentNav = it }
                        val vm =
                            remember {
                                ScenariosViewModel(
                                    stores.users,
                                    CommunityConnectUseCase(stores.users, isSigningAvailable = { true }),
                                    stores.configs,
                                    CommunitySessionTracker(),
                                ).also { viewModels.put("scenarios", it) }
                            }
                        NavHost(nav, startDestination = "list") {
                            composable("list") { Text("Scenarios list") }
                            composable("detail") {
                                ScenarioDetailScreen(
                                    scenarioId = "hostLifecycles",
                                    viewModel = vm,
                                    onOpenClientProfile = {},
                                    onOpenPost = {},
                                    onOpenComment = {},
                                    onOpenBridgeTopic = {},
                                    onOpenHome = {},
                                    onOpenUserActivity = {},
                                    onOpenUserActivityByClientUserId = {},
                                    onOpenMyProfile = {},
                                    onOpenProfileByUserId = {},
                                    onOpenProfileByClientUserId = {},
                                    sdkEvents = emptyList(),
                                    onClearEvents = {},
                                    theme = SDKConfig.Theme.Default,
                                    onSetTheme = {},
                                    onVerifyInCommunity = {},
                                )
                            }
                        }
                        LaunchedEffect(Unit) { nav.navigate("detail") }
                    }
                }
            }
            compose.waitForIdle()
            compose.onNodeWithTag("qa-preset-hostLifecycles-2").performScrollTo().performClick()
            val expected = "OK: Preset 2 · Tab store cleared on Back completed"
            compose.waitUntil(15_000) { ShadowLog.getLogsForTag("HostLifecycles").isNotEmpty() }
            compose.waitForIdle()
            assertEquals(listOf(expected), ShadowLog.getLogsForTag("HostLifecycles").map { it.msg })
            compose.onNodeWithTag("scenario-detail-screen").assertIsDisplayed()
            compose.onNodeWithTag("hostLifecycles-result").assertTextEquals(expected).assertIsDisplayed()
            compose.runOnIdle {
                assertEquals("detail", parentNav.currentDestination?.route)
                assertFalse(compose.activity.isFinishing)
                assertEquals(listOf(expected), ShadowLog.getLogsForTag("HostLifecycles").map { it.msg })
            }
        } finally {
            compose.runOnIdle {
                viewModels.clear()
                OctopusSDK.isInitialisedFlow.value = initialized
            }
        }
    }

    private open class Actions : HostLifecycleActions {
        override suspend fun mount() = Unit

        override suspend fun unmount() = Unit

        override suspend fun openPost() = Unit

        override suspend fun clickPostInCommunity() = Unit

        override suspend fun awaitPostContent() = Unit

        override suspend fun clearTabOnBack() = Unit

        override suspend fun detachAndReattach() = Unit

        override suspend fun switchCommunity() = Unit

        override suspend fun restartSdk() = Unit

        override suspend fun recompose() = Unit
    }

    @Test
    fun `each terminal result logs once at info level without exception messages and cancellation logs nothing`() =
        runBlocking {
            val preset = HostLifecyclePreset.TabBack
            val expected =
                listOf(
                    "OK: ${preset.label} completed",
                    "SKIPPED: Configure a community first",
                    "FAILED: IllegalStateException",
                )
            val actions =
                listOf(
                    Actions(),
                    object : Actions() {
                        override fun setupIssue() = "Configure a community first"
                    },
                    object : Actions() {
                        override suspend fun mount(): Unit = error("must not be logged")
                    },
                )
            actions.forEachIndexed { index, action ->
                assertEquals(expected[index], runAndReportHostLifecyclePreset(preset, action))
            }
            assertFailsWith<CancellationException> {
                runAndReportHostLifecyclePreset(
                    preset,
                    object : Actions() {
                        override suspend fun mount(): Unit = throw CancellationException()
                    },
                )
            }
            val logs = ShadowLog.getLogsForTag("HostLifecycles")
            assertEquals(expected, logs.map { it.msg })
            assertEquals(List(3) { Log.INFO }, logs.map { it.type })
        }
}
