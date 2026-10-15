package com.octopuscommunity.sdk.sample.ui.navigation

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.octopuscommunity.sdk.sample.data.model.SDKConfig.CommunityNavigation
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.octopusComposables
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ContentNavControllerTest {
    private val pushUri = Uri.parse("octopus-sdk://post/push-post")

    private fun activity(flags: Int) =
        Robolectric
            .buildActivity(
                ComponentActivity::class.java,
                Intent(Intent.ACTION_VIEW, pushUri).addFlags(flags),
            ).create()
            .get()

    private fun NavHostController.tabGraph(): NavGraph =
        createGraph(startDestination = ScenariosTab()) {
            composable<ScenariosTab> {}
            composable<SettingsTab> {}
            navigation<CommunityGraph>(startDestination = CommunityTab) {
                composable<CommunityTab> {}
                octopusComposables(navController = this@tabGraph)
            }
        }

    private fun createContentNavController(activity: ComponentActivity): NavHostController {
        lateinit var controller: NavHostController
        val applier =
            object : AbstractApplier<Unit>(Unit) {
                override fun insertTopDown(
                    index: Int,
                    instance: Unit,
                ) = Unit

                override fun insertBottomUp(
                    index: Int,
                    instance: Unit,
                ) = Unit

                override fun remove(
                    index: Int,
                    count: Int,
                ) = Unit

                override fun move(
                    from: Int,
                    to: Int,
                    count: Int,
                ) = Unit

                override fun onClear() = Unit
            }
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = Composition(applier, recomposer)
        composition.setContent {
            CompositionLocalProvider(LocalContext provides activity) {
                controller = rememberContentNavController()
            }
        }
        composition.dispose()
        recomposer.cancel()
        return controller
    }

    private fun tabs(activity: ComponentActivity) =
        createContentNavController(activity).apply {
            setViewModelStore(ViewModelStore())
            graph = tabGraph()
        }

    private fun NavHostController.rootGraph(start: Any = MainRoute): NavGraph {
        val controller = this
        return createGraph(startDestination = start) {
            composable<MainRoute> {}
            composable<ConfigRoute> {}
            octopusComposables(navController = controller)
        }
    }

    private fun root(activity: ComponentActivity) =
        createContentNavController(activity).apply {
            setViewModelStore(ViewModelStore())
            graph = rootGraph()
        }

    private fun NavHostController.postCount(): Int =
        currentBackStack.value.count { it.destination.hasRoute<OctopusDestination.PostDetails>() }

    private fun assertScenariosOnly(controller: NavHostController) {
        val screens = controller.currentBackStack.value.filter { it.destination !is NavGraph }
        assertEquals(1, screens.size)
        assertTrue(screens.single().destination.hasRoute<ScenariosTab>())
    }

    @Test
    fun `SDK launch link stays pending through onboarding graph replacement in both modes`() {
        for (mode in CommunityNavigation.entries) {
            val activity = activity(0)
            try {
                val root = root(activity)
                root.graph = root.rootGraph(ConfigRoute())
                assertFalse(root.navigateToDeepLink(pushUri, mode, isSdkInitialized = false))
                assertTrue(root.currentDestination!!.hasRoute<ConfigRoute>())
                assertEquals(0, root.postCount())

                // SDK ready rebuilds Config's root graph on Main; the caller still owns the URI.
                root.graph = root.rootGraph(MainRoute)
                assertTrue(root.navigateToDeepLink(pushUri, mode, isSdkInitialized = true))
                if (mode == CommunityNavigation.InTab) {
                    assertTrue(root.currentDestination!!.hasRoute<MainRoute>())
                    assertEquals(pushUri.toString(), root.currentBackStackEntry!!.savedStateHandle.get<String>(COMMUNITY_DEEP_LINK))
                    assertEquals(0, root.postCount())
                } else {
                    assertEquals("push-post", root.currentBackStackEntry!!.toRoute<OctopusDestination.PostDetails>().postId)
                    assertEquals(1, root.postCount())
                }
            } finally {
                activity.finish()
            }
        }
    }

    @Test
    fun `activity push is handled once by the root and never replayed by the tab graph in either mode`() {
        for (flags in listOf(0, Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) {
            for (mode in CommunityNavigation.entries) {
                val activity = activity(flags)
                try {
                    val root = root(activity)
                    val tabs = tabs(activity)
                    assertScenariosOnly(tabs)
                    assertEquals(0, root.postCount(), "Cold push must not first open an SDK root screen")
                    root.navigateToDeepLink(pushUri, mode, isSdkInitialized = true)
                    if (mode == CommunityNavigation.InTab) {
                        tabs.selectTab(CommunityGraph)
                        tabs.navigate(Uri.parse(root.currentBackStackEntry!!.savedStateHandle.get<String>(COMMUNITY_DEEP_LINK)))
                        assertTrue(root.currentDestination!!.hasRoute<MainRoute>())
                        assertEquals("push-post", tabs.currentBackStackEntry!!.toRoute<OctopusDestination.PostDetails>().postId)
                    } else {
                        assertEquals("push-post", root.currentBackStackEntry!!.toRoute<OctopusDestination.PostDetails>().postId)
                        assertScenariosOnly(tabs)
                    }
                    assertEquals(1, root.postCount() + tabs.postCount())
                    if (mode == CommunityNavigation.InTab) {
                        assertTrue(tabs.popBackStack())
                        assertTrue(tabs.currentDestination!!.hasRoute<CommunityTab>())
                        assertEquals(0, tabs.postCount())
                    } else {
                        root.popBackStack()
                        assertScenariosOnly(tabs)
                    }
                    assertEquals(pushUri, activity.intent.data)
                } finally {
                    activity.finish()
                }
            }
        }
    }

    @Test
    fun `a later shell or embedded container ignores the old Activity push intent`() {
        val activity = activity(0)
        try {
            repeat(3) { assertScenariosOnly(tabs(activity)) }
        } finally {
            activity.finish()
        }
    }

    @Test
    fun `the child controller saver restores its stack without adding the Activity push again`() {
        val activity = activity(0)
        try {
            val tabs = tabs(activity)
            tabs.selectTab(CommunityGraph)
            tabs.navigate(pushUri)
            val detailId = tabs.currentBackStackEntry!!.id
            val state = tabs.saveState()!!
            val restored =
                createContentNavController(activity).apply {
                    setViewModelStore(ViewModelStore())
                    restoreState(state)
                    graph = tabGraph()
                }
            assertEquals(detailId, restored.currentBackStackEntry!!.id)
            assertEquals(1, restored.postCount())
            assertTrue(restored.popBackStack())
            assertTrue(restored.currentDestination!!.hasRoute<CommunityTab>())
            assertFalse(restored.currentBackStack.value.any { it.destination.hasRoute<OctopusDestination.PostDetails>() })
        } finally {
            activity.finish()
        }
    }
}
