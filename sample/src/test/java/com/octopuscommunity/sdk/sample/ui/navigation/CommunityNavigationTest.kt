package com.octopuscommunity.sdk.sample.ui.navigation

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.octopuscommunity.sdk.sample.data.CommunitySessionTracker
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.data.model.SDKConfig.CommunityNavigation
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.components.UrlOpeningStrategy
import com.octopuscommunity.sdk.ui.navigateToOctopusPost
import com.octopuscommunity.sdk.ui.octopusInternalNavDeepLinks
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CommunityNavigationTest {
    @Test
    fun `a shown deep link is consumed before a host screen changes presentation`() {
        val root = root()
        val uri = Uri.parse("octopus-sdk://post/shown-post")
        root.navigate(uri)
        val entry = root.currentBackStackEntry!!
        assertEquals(uri, unconsumedCommunityDeepLink(entry))
        entry.savedStateHandle[COMMUNITY_LINK_CONSUMED] = true
        root.navigate(LoginRoute)
        root.popBackStack()
        assertNull(unconsumedCommunityDeepLink(root.currentBackStackEntry!!))
        assertFalse(shouldRelayCommunityDeepLink(CommunityNavigation.InTab, unconsumedCommunityDeepLink(entry)?.toString(), root.graph))
    }

    @Test
    fun `restored Community details survive a process reset of the session tracker`() {
        for (saved in listOf(false, true)) {
            val original = tabs()
            val previousBaseline = CommunityTabSessionViewModel()
            previousBaseline.reloadAction(4, !saved, CommunityNavigation.InTab, CommunityNavigation.InTab)
            original.selectTab(CommunityGraph)
            original.navigateToOctopusPost("restored-post")
            val detailId = original.currentBackStackEntry!!.id
            if (saved) original.selectTab(SettingsTab)
            val restored = tabs(original.saveState())
            val resetTracker = CommunitySessionTracker()
            val freshBaseline = CommunityTabSessionViewModel()
            val action =
                freshBaseline.reloadAction(
                    resetTracker.generation.value,
                    !saved,
                    CommunityNavigation.InTab,
                    CommunityNavigation.InTab,
                )
            assertEquals(CommunityTabReload.NONE, action)
            reloadCommunityStack(restored, action)
            resetTracker.onCommunityTargeted()
            val seeded =
                freshBaseline.reloadAction(
                    resetTracker.generation.value,
                    !saved,
                    CommunityNavigation.InTab,
                    CommunityNavigation.InTab,
                )
            assertEquals(CommunityTabReload.NONE, seeded)
            reloadCommunityStack(restored, seeded)
            if (saved) restored.selectTab(CommunityGraph)
            assertEquals(detailId, restored.currentBackStackEntry!!.id)
            assertEquals("restored-post", restored.currentBackStackEntry!!.toRoute<OctopusDestination.PostDetails>().postId)
        }
    }

    @Test
    fun `MainRoute session baseline survives a root host screen covering the shell`() {
        val root = root()
        val baseline = ViewModelProvider(root.currentBackStackEntry!!)[CommunityTabSessionViewModel::class.java]
        assertEquals(
            CommunityTabReload.NONE,
            baseline.reloadAction(4, false, CommunityNavigation.InTab, CommunityNavigation.InTab),
        )
        root.navigate(LoginRoute)
        val coveredBaseline = ViewModelProvider(root.getBackStackEntry<MainRoute>())[CommunityTabSessionViewModel::class.java]
        assertSame(baseline, coveredBaseline)
        assertEquals(
            CommunityTabReload.CLEAR_SAVED_STATE,
            coveredBaseline.reloadAction(5, false, CommunityNavigation.InTab, CommunityNavigation.InTab),
        )
        root.popBackStack()
        assertSame(baseline, ViewModelProvider(root.currentBackStackEntry!!)[CommunityTabSessionViewModel::class.java])
        assertEquals(
            CommunityTabReload.NONE,
            baseline.reloadAction(5, false, CommunityNavigation.InTab, CommunityNavigation.InTab),
        )
    }

    private fun controller() =
        NavHostController(RuntimeEnvironment.getApplication()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            setViewModelStore(ViewModelStore())
        }

    private fun tabs(savedState: Bundle? = null) =
        controller().apply {
            if (savedState != null) restoreState(savedState)
            graph =
                createGraph(startDestination = ScenariosTab()) {
                    composable<ScenariosTab> {}
                    composable<SettingsTab> {}
                    navigation<CommunityGraph>(startDestination = CommunityTab) {
                        composable<CommunityTab> {}
                        composable<OctopusDestination.PostDetails>(
                            deepLinks = octopusInternalNavDeepLinks<OctopusDestination.PostDetails>(),
                        ) {}
                        composable<OctopusDestination.CommentDetails> {}
                        composable<OctopusDestination.GroupDetails> {}
                        composable<OctopusDestination.ProfileSummary> {}
                    }
                }
        }

    private fun root() =
        controller().apply {
            graph =
                createGraph(startDestination = MainRoute) {
                    composable<MainRoute> {}
                    composable<LoginRoute> {}
                    composable<EditUserRoute> {}
                    composable<ClientProfileRoute> {}
                    composable<SamplePostLinkRoute> {}
                    composable<OctopusDestination.PostDetails>(
                        deepLinks = octopusInternalNavDeepLinks<OctopusDestination.PostDetails>(),
                    ) {}
                }
        }

    @Test
    fun `SDK details use the active controller and tab selection stays Community`() {
        for (mode in CommunityNavigation.entries) {
            val root = root()
            val tabs = tabs()
            tabs.selectTab(CommunityGraph)
            val content = communityContentController(mode, root, tabs)
            content.navigateToOctopusPost("demo-post")
            if (mode == CommunityNavigation.InTab) {
                assertTrue(root.currentDestination!!.hasRoute<MainRoute>())
                assertTrue(tabs.currentDestination!!.hierarchy.any { it.hasRoute<CommunityGraph>() })
                for (detail in listOf(
                    OctopusDestination.CommentDetails("comment"),
                    OctopusDestination.GroupDetails("group"),
                    OctopusDestination.ProfileSummary("member"),
                )) {
                    tabs.navigate(detail)
                    assertTrue(tabs.currentDestination!!.hierarchy.any { it.hasRoute<CommunityGraph>() })
                    assertTrue(tabs.popBackStack())
                }
            } else {
                assertTrue(tabs.currentDestination!!.hasRoute<CommunityTab>())
            }
            assertTrue(content.popBackStack())
            assertTrue(tabs.currentDestination!!.hasRoute<CommunityTab>())
            assertTrue(root.currentDestination!!.hasRoute<MainRoute>())
        }
    }

    @Test
    fun `a tab switch restores details and reselecting Community pops to its root`() {
        val tabs = tabs()
        tabs.selectTab(CommunityGraph)
        tabs.navigateToOctopusPost("demo-post")
        val detail = tabs.currentBackStackEntry!!
        tabs.selectTab(SettingsTab)
        tabs.selectTab(CommunityGraph)
        assertEquals(detail.id, tabs.currentBackStackEntry!!.id)
        assertEquals("demo-post", tabs.currentBackStackEntry!!.toRoute<OctopusDestination.PostDetails>().postId)
        tabs.selectTab(CommunityGraph, reselect = true)
        assertTrue(tabs.currentDestination!!.hasRoute<CommunityTab>())
    }

    @Test
    fun `reload removes both live and saved details including home entries`() {
        for (saved in listOf(false, true)) {
            val tabs = tabs()
            tabs.selectTab(CommunityGraph)
            val home = tabs.currentBackStackEntry!!.id
            tabs.navigateToOctopusPost("old-post")
            if (saved) tabs.selectTab(SettingsTab)
            reloadCommunityStack(tabs, if (saved) CommunityTabReload.CLEAR_SAVED_STATE else CommunityTabReload.RENAVIGATE)
            if (saved) {
                assertTrue(tabs.currentDestination!!.hasRoute<SettingsTab>())
                tabs.selectTab(CommunityGraph)
            }
            assertTrue(tabs.currentDestination!!.hasRoute<CommunityTab>())
            assertFalse(home == tabs.currentBackStackEntry!!.id)
            tabs.selectTab(SettingsTab)
            tabs.selectTab(CommunityGraph)
            assertTrue(tabs.currentDestination!!.hasRoute<CommunityTab>())
        }
    }

    @Test
    fun `a restored saved tab stack can be invalidated after the host screen changes mode`() {
        val original = tabs()
        original.selectTab(CommunityGraph)
        original.navigateToOctopusPost("old-post")
        val oldDetailId = original.currentBackStackEntry!!.id
        original.selectTab(SettingsTab)
        val restored = tabs(original.saveState())
        restored.selectTab(CommunityGraph)
        assertEquals(oldDetailId, restored.currentBackStackEntry!!.id)
        restored.selectTab(SettingsTab)
        reloadCommunityStack(restored, communityTabReload(1, 1, false, CommunityNavigation.InTab, CommunityNavigation.FullScreen))
        restored.selectTab(CommunityGraph)
        assertTrue(restored.currentDestination!!.hasRoute<CommunityTab>())
    }

    @Test
    fun `host callbacks route to the root even when SDK content is in the tab`() {
        for (mode in CommunityNavigation.entries) {
            val root = root()
            val tabs = tabs()
            tabs.selectTab(CommunityGraph)
            communityContentController(mode, root, tabs).navigateToOctopusPost("demo-post")
            val detail = tabs.currentBackStackEntry!!.id
            val hostCalls = mutableListOf<String>()
            val callbacks =
                communityHostNavigation(
                    SDKConfig.HostCallbacks(profileWired = true),
                    navigateHost = { root.navigate(it) },
                    onClientObject = { hostCalls += "object:$it" },
                    onGroupAccessDenied = { hostCalls += "group:$it" },
                    onUrl = {
                        hostCalls += "url:$it"
                        UrlOpeningStrategy.HandledByApp
                    },
                )
            callbacks.login!!.invoke()
            assertTrue(root.currentDestination!!.hasRoute<LoginRoute>())
            root.popBackStack()
            callbacks.profileEdit!!.invoke(null)
            assertTrue(root.currentDestination!!.hasRoute<EditUserRoute>())
            root.popBackStack()
            callbacks.profile!!.invoke("host-user")
            assertEquals("host-user", root.currentBackStackEntry!!.toRoute<ClientProfileRoute>().clientUserId)
            callbacks.clientObject!!.invoke("demo-object")
            callbacks.groupAccessDenied!!.invoke("demo-group")
            assertEquals(UrlOpeningStrategy.HandledByApp, callbacks.url("https://example.org"))
            assertEquals(listOf("object:demo-object", "group:demo-group", "url:https://example.org"), hostCalls)
            assertEquals(detail, tabs.currentBackStackEntry!!.id)
        }
    }

    @Test
    fun `external posts queue inside Main and SDK push links land inside Community`() {
        val root = root()
        val tabs = tabs()
        root.navigate(SamplePostLinkRoute("demo-post"))
        root.openSamplePostLink(root.currentBackStackEntry!!.toRoute())
        assertTrue(root.currentDestination!!.hasRoute<MainRoute>())
        assertEquals("demo-post", root.currentBackStackEntry!!.savedStateHandle.get<String>(COMMUNITY_POST_ID))
        tabs.selectTab(CommunityGraph)
        tabs.navigateToOctopusPost(root.currentBackStackEntry!!.savedStateHandle.get<String>(COMMUNITY_POST_ID)!!)
        assertEquals("demo-post", tabs.currentBackStackEntry!!.toRoute<OctopusDestination.PostDetails>().postId)
        root.navigateToOctopusPost("push-post")
        root.openCommunityDeepLink("octopus-sdk://post/push-post")
        assertEquals("octopus-sdk://post/push-post", root.currentBackStackEntry!!.savedStateHandle.get<String>(COMMUNITY_DEEP_LINK))
        tabs.selectTab(CommunityGraph)
        tabs.navigate(Uri.parse(root.currentBackStackEntry!!.savedStateHandle.get<String>(COMMUNITY_DEEP_LINK)))
        assertEquals("push-post", tabs.currentBackStackEntry!!.toRoute<OctopusDestination.PostDetails>().postId)
        assertTrue(tabs.popBackStack())
        assertTrue(tabs.currentDestination!!.hierarchy.any { it.hasRoute<CommunityGraph>() })
    }

    @Test
    fun `unwired optional host callbacks stay absent`() {
        val callbacks =
            communityHostNavigation(
                SDKConfig.HostCallbacks(
                    loginWired = false,
                    profileEditWired = false,
                    profileWired = false,
                    clientObjectWired = false,
                    groupAccessDeniedWired = false,
                ),
                navigateHost = { error("unexpected host navigation") },
                onClientObject = { error("unexpected host object") },
                onGroupAccessDenied = { error("unexpected group callback") },
                onUrl = { UrlOpeningStrategy.HandledByOctopus },
            )
        assertNull(callbacks.login)
        assertNull(callbacks.profileEdit)
        assertNull(callbacks.profile)
        assertNull(callbacks.clientObject)
        assertNull(callbacks.groupAccessDenied)
    }

    @Test
    fun `cold and warm SDK links carry the original URI and relay without leaving a root detail`() {
        for (cold in listOf(false, true)) {
            val root = root()
            val uri = Uri.parse("octopus-sdk://post/push-post")
            if (cold) {
                assertTrue(root.handleDeepLink(Intent(Intent.ACTION_VIEW, uri)))
            } else {
                root.navigate(uri)
            }
            @Suppress("DEPRECATION")
            val received =
                root.currentBackStackEntry!!
                    .arguments!!
                    .getParcelable<Intent>(NavController.KEY_DEEP_LINK_INTENT)!!
                    .data
            assertEquals(uri, received)
            root.openCommunityDeepLink(received.toString())
            assertTrue(root.currentDestination!!.hasRoute<MainRoute>())
            assertEquals(uri.toString(), root.currentBackStackEntry!!.savedStateHandle.get<String>(COMMUNITY_DEEP_LINK))
            assertFalse(root.popBackStack())
        }
    }

    @Test
    fun `warm links route immediately in the active mode and unmatched links do nothing`() {
        for (mode in CommunityNavigation.entries) {
            val root = root()
            root.navigateToDeepLink(Uri.parse("octopus-sdk://post/push-post"), mode, isSdkInitialized = true)
            if (mode == CommunityNavigation.InTab) {
                assertTrue(root.currentDestination!!.hasRoute<MainRoute>())
                assertEquals("octopus-sdk://post/push-post", root.currentBackStackEntry!!.savedStateHandle.get<String>(COMMUNITY_DEEP_LINK))
            } else {
                assertEquals("push-post", root.currentBackStackEntry!!.toRoute<OctopusDestination.PostDetails>().postId)
            }
            val currentId = root.currentBackStackEntry!!.id
            root.navigateToDeepLink(Uri.parse("octopus-sample://unknown"), mode, isSdkInitialized = true)
            assertEquals(currentId, root.currentBackStackEntry!!.id)
        }
    }
}
