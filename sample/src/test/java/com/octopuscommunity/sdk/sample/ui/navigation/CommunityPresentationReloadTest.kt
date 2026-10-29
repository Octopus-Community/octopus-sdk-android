package com.octopuscommunity.sdk.sample.ui.navigation

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.navigateToOctopusHome
import com.octopuscommunity.sdk.ui.navigateToOctopusPost
import kotlinx.serialization.Serializable
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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Stands in for the sheet/modal's private home route: the start destination it pops inclusive. */
@Serializable
internal data object PresentationHomeRoute

/**
 * Composes the real session-change reloads over a real `NavHost`, as `MainActivity` does: the
 * reload is called before its `NavHost`, so its first pass has no graph yet. SDK destinations are
 * stubs with the SDK's own routes, so nothing resolves SDK DI.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CommunityPresentationReloadTest {
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

    private var session by mutableIntStateOf(0)
    private var reloadComposed by mutableStateOf(true)
    private lateinit var navController: NavHostController

    class ClearingViewModel : ViewModel() {
        var cleared = false
            private set

        override fun onCleared() {
            cleared = true
        }
    }

    private fun NavBackStackEntry.trackedViewModel() = ViewModelProvider(this)[ClearingViewModel::class.java]

    private fun graphEntryId() = navController.getBackStackEntry(navController.graph.id).id

    private fun setEmbeddedContent() {
        compose.setContent {
            navController = rememberContentNavController()
            if (reloadComposed) {
                ReloadCommunityPresentationOnSessionChange(navController, session, PresentationHomeRoute)
            }
            NavHost(navController, startDestination = PresentationHomeRoute) {
                composable<PresentationHomeRoute> {}
                composable<OctopusDestination.PostDetails> {}
                composable<LoginRoute> {}
            }
        }
    }

    private fun setRootContent() {
        compose.setContent {
            navController = rememberContentNavController()
            if (reloadComposed) ReloadRootCommunityEntriesOnSessionChange(navController, session)
            NavHost(navController, startDestination = MainRoute) {
                composable<MainRoute> {}
                composable<ConfigRoute> {}
                composable<LoginRoute> {}
                composable<CommunityFullScreenRoute> {}
                composable<OctopusDestination.Home> {}
                composable<OctopusDestination.PostDetails> {}
                composable<OctopusDestination.CommentDetails> {}
            }
        }
    }

    /** Startup announces generation 1 after the graph may already exist: never a switch. */
    private fun startSession() {
        compose.runOnIdle { session = 1 }
        compose.waitForIdle()
    }

    private fun switchCommunity() {
        compose.runOnIdle { session += 1 }
        compose.waitForIdle()
    }

    private fun applyConfig() {
        compose.runOnIdle { navController.navigate(ConfigRoute()) }
        // Config Apply, SDK reset and onboarding completion all replace the root graph this way.
        compose.runOnIdle {
            navController.navigate(MainRoute) { popUpTo(0) { inclusive = true } }
        }
        compose.waitForIdle()
    }

    @Test
    fun `embedded presentation replaces its start home and details on every switch`() {
        setEmbeddedContent()
        startSession()
        val graphId = compose.runOnIdle { graphEntryId() }
        repeat(2) { switch ->
            lateinit var homeId: String
            lateinit var home: ClearingViewModel
            lateinit var detail: ClearingViewModel
            compose.runOnIdle {
                homeId = navController.currentBackStackEntry!!.id
                home = navController.currentBackStackEntry!!.trackedViewModel()
                navController.navigateToOctopusPost("post-$switch")
            }
            compose.runOnIdle {
                detail = navController.currentBackStackEntry!!.trackedViewModel()
                navController.navigate(LoginRoute)
            }
            switchCommunity()
            compose.runOnIdle {
                assertTrue(navController.currentDestination!!.hasRoute<PresentationHomeRoute>(), "switch $switch")
                assertNotEquals(homeId, navController.currentBackStackEntry!!.id)
                assertEquals(2, navController.currentBackStack.value.size, "graph + home only")
                assertTrue(home.cleared)
                assertTrue(detail.cleared)
                assertEquals(graphId, graphEntryId(), "the baseline-holding graph entry is kept")
            }
        }
    }

    @Test
    fun `startup generation and an unchanged session do not reload`() {
        setEmbeddedContent()
        lateinit var homeId: String
        compose.runOnIdle {
            homeId = navController.currentBackStackEntry!!.id
            navController.navigateToOctopusPost("post")
        }
        startSession()
        compose.runOnIdle {
            assertTrue(navController.currentDestination!!.hasRoute<OctopusDestination.PostDetails>())
            assertTrue(navController.popBackStack())
        }
        // Recomposing the caller with the same session must not count as a switch either.
        compose.runOnIdle { reloadComposed = false }
        compose.runOnIdle { reloadComposed = true }
        compose.runOnIdle { assertEquals(homeId, navController.currentBackStackEntry!!.id) }
    }

    @Test
    fun `a switch while the reload is out of composition is still seen when it returns`() {
        setEmbeddedContent()
        startSession()
        lateinit var homeId: String
        compose.runOnIdle {
            homeId = navController.currentBackStackEntry!!.id
            reloadComposed = false
        }
        switchCommunity()
        compose.runOnIdle { assertEquals(homeId, navController.currentBackStackEntry!!.id) }
        compose.runOnIdle { reloadComposed = true }
        compose.runOnIdle {
            assertTrue(navController.currentDestination!!.hasRoute<PresentationHomeRoute>())
            assertNotEquals(homeId, navController.currentBackStackEntry!!.id)
        }
    }

    @Test
    fun `fullscreen preset reloads its home without the old post on every switch`() {
        setRootContent()
        startSession()
        val graphId = compose.runOnIdle { graphEntryId() }
        val mainId = compose.runOnIdle { navController.currentBackStackEntry!!.id }
        compose.runOnIdle { navController.navigate(CommunityFullScreenRoute(postId = "old-post", showHostBottomBar = true)) }
        repeat(2) { switch ->
            lateinit var presetId: String
            lateinit var preset: ClearingViewModel
            lateinit var detail: ClearingViewModel
            compose.runOnIdle {
                presetId = navController.currentBackStackEntry!!.id
                preset = navController.currentBackStackEntry!!.trackedViewModel()
                navController.navigateToOctopusPost("post-$switch")
            }
            compose.runOnIdle { detail = navController.currentBackStackEntry!!.trackedViewModel() }
            switchCommunity()
            compose.runOnIdle {
                assertEquals(
                    CommunityFullScreenRoute(showHostBottomBar = true),
                    navController.currentBackStackEntry!!.toRoute<CommunityFullScreenRoute>(),
                    "switch $switch",
                )
                assertNotEquals(presetId, navController.currentBackStackEntry!!.id)
                assertTrue(preset.cleared)
                assertTrue(detail.cleared)
                assertEquals(mainId, navController.previousBackStackEntry!!.id)
                assertEquals(graphId, graphEntryId())
            }
        }
    }

    @Test
    fun `root SDK details are dropped back to the host screen on every switch`() {
        setRootContent()
        startSession()
        val mainId = compose.runOnIdle { navController.currentBackStackEntry!!.id }
        repeat(2) { switch ->
            lateinit var post: ClearingViewModel
            lateinit var repeated: ClearingViewModel
            compose.runOnIdle { navController.navigateToOctopusPost("post-$switch") }
            compose.runOnIdle {
                post = navController.currentBackStackEntry!!.trackedViewModel()
                navController.navigate(OctopusDestination.CommentDetails("comment-$switch"))
            }
            // The same route twice: popping by route would stop at the upper post.
            compose.runOnIdle { navController.navigateToOctopusPost("other-$switch") }
            compose.runOnIdle {
                repeated = navController.currentBackStackEntry!!.trackedViewModel()
                navController.navigate(LoginRoute)
            }
            switchCommunity()
            compose.runOnIdle {
                assertEquals(mainId, navController.currentBackStackEntry!!.id, "switch $switch")
                assertFalse(navController.currentBackStack.value.any { it.destination.isCommunityBacked() })
                assertFalse(navController.currentBackStack.value.any { it.destination.hasRoute<LoginRoute>() })
                assertTrue(post.cleared)
                assertTrue(repeated.cleared)
            }
        }
    }

    @Test
    fun `root SDK post is dropped on the first switch after the root graph is replaced`() {
        setRootContent()
        startSession()
        val originalGraphId = compose.runOnIdle { graphEntryId() }
        applyConfig()
        val replacementGraphId = compose.runOnIdle { graphEntryId() }
        assertNotEquals(originalGraphId, replacementGraphId)
        val mainId = compose.runOnIdle { navController.currentBackStackEntry!!.id }
        compose.runOnIdle { navController.navigateToOctopusPost("old-post") }
        val post = compose.runOnIdle { navController.currentBackStackEntry!!.trackedViewModel() }

        switchCommunity()

        compose.runOnIdle {
            assertEquals(mainId, navController.currentBackStackEntry!!.id)
            assertFalse(navController.currentBackStack.value.any { it.destination.isCommunityBacked() })
            assertTrue(post.cleared)
            assertEquals(replacementGraphId, graphEntryId())
        }
    }

    @Test
    fun `fullscreen preset is rebuilt on the first switch after the root graph is replaced`() {
        setRootContent()
        startSession()
        val originalGraphId = compose.runOnIdle { graphEntryId() }
        applyConfig()
        val replacementGraphId = compose.runOnIdle { graphEntryId() }
        assertNotEquals(originalGraphId, replacementGraphId)
        val mainId = compose.runOnIdle { navController.currentBackStackEntry!!.id }
        compose.runOnIdle { navController.navigate(CommunityFullScreenRoute(postId = "old-post", showHostBottomBar = true)) }
        val presetId = compose.runOnIdle { navController.currentBackStackEntry!!.id }
        val preset = compose.runOnIdle { navController.currentBackStackEntry!!.trackedViewModel() }

        switchCommunity()

        val rebuiltId = compose.runOnIdle { navController.currentBackStackEntry!!.id }
        compose.runOnIdle {
            assertEquals(
                CommunityFullScreenRoute(showHostBottomBar = true),
                navController.currentBackStackEntry!!.toRoute<CommunityFullScreenRoute>(),
            )
            assertNotEquals(presetId, rebuiltId)
            assertTrue(preset.cleared)
            assertEquals(mainId, navController.previousBackStackEntry!!.id)
            assertEquals(replacementGraphId, graphEntryId())
        }
        // Recreating the effect with the same graph and session must not reload a second time.
        compose.runOnIdle { reloadComposed = false }
        compose.runOnIdle { reloadComposed = true }
        compose.runOnIdle { assertEquals(rebuiltId, navController.currentBackStackEntry!!.id) }
    }

    @Test
    fun `a root SDK home is rebuilt and its details dropped`() {
        setRootContent()
        startSession()
        val mainId = compose.runOnIdle { navController.currentBackStackEntry!!.id }
        compose.runOnIdle { navController.navigateToOctopusHome() }
        repeat(2) { switch ->
            lateinit var homeId: String
            lateinit var home: ClearingViewModel
            compose.runOnIdle {
                homeId = navController.currentBackStackEntry!!.id
                home = navController.currentBackStackEntry!!.trackedViewModel()
                navController.navigateToOctopusPost("post-$switch")
            }
            switchCommunity()
            compose.runOnIdle {
                assertTrue(navController.currentDestination!!.hasRoute<OctopusDestination.Home>(), "switch $switch")
                assertNotEquals(homeId, navController.currentBackStackEntry!!.id)
                assertTrue(home.cleared)
                assertEquals(mainId, navController.previousBackStackEntry!!.id)
            }
        }
    }

    @Test
    fun `host-only root stacks are left alone`() {
        setRootContent()
        startSession()
        lateinit var loginId: String
        compose.runOnIdle {
            navController.navigate(LoginRoute)
        }
        compose.runOnIdle { loginId = navController.currentBackStackEntry!!.id }
        switchCommunity()
        compose.runOnIdle {
            assertEquals(loginId, navController.currentBackStackEntry!!.id)
            assertTrue(navController.previousBackStackEntry!!.destination.hasRoute<MainRoute>())
        }
    }
}
