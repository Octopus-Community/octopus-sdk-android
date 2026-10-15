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
import com.octopuscommunity.sdk.sample.data.SampleFixtures
import com.octopuscommunity.sdk.sample.data.model.SDKConfig.CommunityNavigation
import com.octopuscommunity.sdk.ui.OctopusDestination
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SamplePostLinkTest {
    private fun controller(): NavHostController =
        NavHostController(RuntimeEnvironment.getApplication()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            graph =
                createGraph(startDestination = ConfigRoute()) {
                    composable<ConfigRoute> {}
                    composable<MainRoute> {}
                    composable<OctopusDestination.PostDetails> {}
                    composable<SamplePostLinkRoute>(
                        deepLinks = listOf(navDeepLink { uriPattern = SampleFixtures.postLinkPattern }),
                    ) {}
                }
        }

    @Test
    fun `external post links preserve both catalog and operator supplied ids`() {
        val controller = controller()
        for (id in listOf(SampleFixtures.postText, "another-demo-post")) {
            controller.navigate(Uri.parse("octopus-sample://post/$id"))
            val route = controller.currentBackStackEntry!!.toRoute<SamplePostLinkRoute>()
            controller.openSamplePostLink(route, CommunityNavigation.FullScreen)
            assertEquals(id, controller.currentBackStackEntry!!.toRoute<OctopusDestination.PostDetails>().postId)
            controller.popBackStack()
            assertTrue(controller.currentDestination!!.hasRoute<MainRoute>())
        }
    }

    @Test
    fun `empty id does not open a post and another host is not matched`() {
        val controller = controller()
        controller.navigate(Uri.parse("octopus-sample://post/"))
        controller.openSamplePostLink(controller.currentBackStackEntry!!.toRoute<SamplePostLinkRoute>())
        assertTrue(controller.currentDestination!!.hasRoute<MainRoute>())
        assertFalse(controller.graph.hasDeepLink(Uri.parse("octopus-sample://profile/${SampleFixtures.userOther}")))
    }
}
