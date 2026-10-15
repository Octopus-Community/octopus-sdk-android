package com.octopuscommunity.sdk.sample.ui.navigation

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import com.octopuscommunity.sdk.sample.data.model.SDKConfig.CommunityNavigation
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.octopusComposables
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CommunityContentRoutingTest {
    private fun root() =
        NavHostController(RuntimeEnvironment.getApplication()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            setViewModelStore(ViewModelStore())
            val controller = this
            graph =
                createGraph(startDestination = MainRoute) {
                    composable<MainRoute> {}
                    octopusComposables(navController = controller)
                }
        }

    @Test
    fun `community drill-down and pending content use the controller owning the active graph`() {
        val root = Any()
        val tab = Any()
        assertSame(tab, communityContentController(CommunityNavigation.InTab, root, tab))
        assertSame(root, communityContentController(CommunityNavigation.FullScreen, root, tab))
    }

    @Test
    fun `real typed SDK navigation never relays Navigation's generated deep link intent`() {
        for (destination in listOf(
            OctopusDestination.PostDetails("post"),
            OctopusDestination.Activity("member"),
            OctopusDestination.ProfileSummary("member"),
            OctopusDestination.CurrentUserProfileGraph,
        )) {
            val root = root()
            root.navigate(destination)
            @Suppress("DEPRECATION")
            val intent = assertNotNull(root.currentBackStackEntry!!.arguments!!.getParcelable<Intent>(NavController.KEY_DEEP_LINK_INTENT))
            val uri = assertNotNull(intent.data).toString()
            for (mode in CommunityNavigation.entries) {
                assertFalse(shouldRelayCommunityDeepLink(mode, uri, root.graph), "$mode: $destination")
            }
        }
    }

    @Test
    fun `only explicit incoming SDK links relay to the tab graph`() {
        val root = root()
        for (uri in listOf("octopus-sdk://post/demo-post", "octopus-sdk://post/second-post")) {
            assertTrue(shouldRelayCommunityDeepLink(CommunityNavigation.InTab, uri, root.graph))
            assertFalse(shouldRelayCommunityDeepLink(CommunityNavigation.FullScreen, uri, root.graph))
        }
        for (uri in listOf(null, "https://demo.example/unmatched", "octopus-sample://unknown")) {
            assertFalse(shouldRelayCommunityDeepLink(CommunityNavigation.InTab, uri, root.graph))
        }
    }
}
