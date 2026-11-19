package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import android.app.Application
import android.content.ComponentName
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.ui.OctopusDestination
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
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class HostLifecycleHostsTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()
    private val host =
        object : ExternalResource() {
            override fun before() {
                val application = RuntimeEnvironment.getApplication()
                shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
            }
        }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(host).around(compose)

    private class Probe : ViewModel() {
        var cleared = false

        override fun onCleared() {
            cleared = true
        }
    }

    @OptIn(InternalComposeTracingApi::class)
    private class SdkCompositionProbe : CompositionTracer {
        val calls = mutableSetOf<String>()

        override fun isTraceInProgress() = true

        override fun traceEventStart(
            key: Int,
            dirty1: Int,
            dirty2: Int,
            info: String,
        ) {
            if (info.startsWith("com.octopuscommunity.sdk.ui.")) calls += info.substringBefore(" (")
        }

        override fun traceEventEnd() = Unit
    }

    @OptIn(InternalComposeTracingApi::class)
    @Test
    fun `real SDK preview subtree mounts with host callbacks and acknowledges recomposition without remount`() {
        val session = HostLifecycleSession(HostLifecyclePreset.Remount, SDKConfig.Theme.Default)
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                HostLifecycleContent(session)
            }
        }
        compose.waitForIdle()
        lateinit var originalNav: NavHostController
        val probe = Probe()
        val trace = SdkCompositionProbe()
        try {
            compose.runOnIdle {
                assertTrue(session.ready.isCompleted)
                assertEquals(0, session.appliedRevision.value)
                originalNav = checkNotNull(session.navController)
                session.communityStore.viewModelStore.put("retained-probe", probe)
                Composer.setTracer(trace)
                session.revision++
            }
            compose.waitForIdle()
            compose.runOnIdle {
                assertEquals(1, session.appliedRevision.value)
                assertTrue(
                    "com.octopuscommunity.sdk.ui.home.OctopusHomeContent" in trace.calls,
                    "The SDK home must execute: ${trace.calls}",
                )
                assertSame(originalNav, session.navController)
                assertFalse(probe.cleared, "Recomposition must retain the tab's store")
                assertFalse(session.disposed.isCompleted, "Recomposition must not remount the community")
            }
        } finally {
            compose.runOnIdle { Composer.setTracer(null) }
        }
    }

    @OptIn(InternalComposeTracingApi::class)
    @Test
    fun `SDK post destination recomposes after tab clear during the Nav3 exit animation`() {
        val session = HostLifecycleSession(HostLifecyclePreset.TabBack, SDKConfig.Theme.Default)
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) { HostLifecycleContent(session) }
        }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(session.semantics?.clickFirstPost() == true) }
        compose.waitUntil(10_000) {
            session.navController?.currentDestination?.hasRoute<OctopusDestination.PostDetails>() == true &&
                session.semantics?.postContentDisplayed() == true
        }
        compose.waitForIdle()
        val originalNav = checkNotNull(session.navController)
        val cleared = Probe()
        val trace = SdkCompositionProbe()
        compose.mainClock.autoAdvance = false
        try {
            compose.runOnIdle {
                session.communityStore.viewModelStore.put("probe", cleared)
                Composer.setTracer(trace)
                session.popTab()
                assertTrue(cleared.cleared)
                assertFalse(session.tabExited.isCompleted)
            }
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            compose.mainClock.advanceTimeBy(200)
            compose.waitForIdle()
            compose.runOnIdle {
                assertEquals(1, session.appliedRevision.value)
                assertFalse(session.tabExited.isCompleted, "Destination must recompose during the 800ms exit")
                assertSame(originalNav, session.navController)
                assertTrue(
                    "com.octopuscommunity.sdk.ui.posts.details.PostDetailsScreen" in trace.calls,
                    "The SDK post screen must execute: ${trace.calls}",
                )
                assertTrue(
                    "com.octopuscommunity.sdk.ui.components.OctopusNavigationScope" in trace.calls,
                    "The SDK scope must execute: ${trace.calls}",
                )
            }
        } finally {
            compose.runOnIdle { Composer.setTracer(null) }
            compose.mainClock.autoAdvance = true
        }
    }

    @Test
    fun `SDK semantics click opens a rendered post through its own handler`() {
        val session = HostLifecycleSession(HostLifecyclePreset.Restart, SDKConfig.Theme.Default)
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                Box(Modifier.fillMaxWidth().height(520.dp)) { HostLifecycleContent(session) }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle {
            session.revision++
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, session.appliedRevision.value)
            assertTrue(session.semantics?.clickFirstPost() == true, "The first SDK card must expose its real click handler")
        }
        compose.waitUntil(10_000) {
            session.navController?.currentDestination?.hasRoute<OctopusDestination.PostDetails>() == true &&
                session.semantics?.postContentDisplayed() == true
        }
    }

    @Test
    fun `preset labels and unique result are exposed and failures scroll into view`() {
        compose.setContent {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                HostLifecyclesSection(config = null, onSdkSessionEnded = {})
            }
        }
        HostLifecyclePreset.entries.forEachIndexed { index, preset ->
            compose
                .onNodeWithTag("qa-preset-hostLifecycles-${index + 1}")
                .assertTextEquals(preset.label)
        }
        compose.onAllNodesWithTag("hostLifecycles-result").assertCountEquals(1)
        compose.onNodeWithTag("qa-preset-hostLifecycles-1").performScrollTo().performClick()
        compose.waitForIdle()
        compose
            .onNodeWithTag("hostLifecycles-result")
            .assertTextEquals("SKIPPED: Configure a community first")
            .assertIsDisplayed()
    }

    @Test
    fun `tab Back uses isolated dispatchers and cannot fall through to the Activity at the host root`() {
        val session = HostLifecycleSession(HostLifecyclePreset.TabBack, SDKConfig.Theme.Default)
        var navigationOwner: NavigationEventDispatcherOwner? = null
        var backOwner: OnBackPressedDispatcherOwner? = null
        var outerBacks = 0
        val outer =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    outerBacks++
                }
            }
        compose.activity.onBackPressedDispatcher.addCallback(outer)
        try {
            compose.setContent {
                HostTabDisplay(session) {
                    navigationOwner = LocalNavigationEventDispatcherOwner.current
                    backOwner = LocalOnBackPressedDispatcherOwner.current
                    RevisionAwareCommunity(session) {}
                }
            }
            compose.waitForIdle()
            compose.runOnIdle {
                assertNotSame(compose.activity.navigationEventDispatcher, checkNotNull(navigationOwner).navigationEventDispatcher)
                assertNotSame(compose.activity.onBackPressedDispatcher, checkNotNull(backOwner).onBackPressedDispatcher)
                session.popTab()
            }
            compose.waitForIdle()
            compose.runOnIdle {
                assertTrue(session.tabExited.isCompleted)
                session.popTab() // A repeated local Back at root has no parent fallback.
                checkNotNull(backOwner).onBackPressedDispatcher.onBackPressed()
                assertEquals(0, outerBacks)
                assertFalse(compose.activity.isFinishing)
            }
        } finally {
            outer.remove()
        }
    }

    @Test
    fun `Nav3 Back clears tab store while exiting content is still composed`() {
        val session = HostLifecycleSession(HostLifecyclePreset.TabBack, SDKConfig.Theme.Default)
        val probe = Probe()
        session.communityStore.viewModelStore.put("probe", probe)
        var composed = false
        compose.setContent {
            HostTabDisplay(session) {
                RevisionAwareCommunity(session) { composed = it }
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            assertTrue(composed)
            session.popTab()
            assertTrue(probe.cleared)
            assertEquals(1, session.revision)
            assertTrue(composed, "Store must clear before the composition leaves")
            assertFalse(session.tabExited.isCompleted)
        }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(composed, "The 800ms exit animation must retain the content")
            assertEquals(1, session.appliedRevision.value, "Content must recompose after its store was cleared")
        }
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        compose.runOnIdle {
            assertFalse(composed)
            assertTrue(session.tabExited.isCompleted)
        }
    }

    @Test
    fun `native owner caps reattachment by host state and clears only on final destruction`() {
        compose.runOnIdle {
            val host =
                object : LifecycleOwner {
                    override val lifecycle = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
                }
            val store = ViewModelStore()
            val probe = Probe()
            store.put("probe", probe)
            val owner = HostAttachedViewOwner(store, host.lifecycle)
            owner.observeHost()
            assertEquals(Lifecycle.State.CREATED, owner.lifecycle.currentState)
            owner.attach()
            assertEquals(Lifecycle.State.RESUMED, owner.lifecycle.currentState)
            host.lifecycle.currentState = Lifecycle.State.CREATED
            owner.detach()
            owner.attach()
            assertEquals(Lifecycle.State.CREATED, owner.lifecycle.currentState, "Reattach must not resume a background host")
            host.lifecycle.currentState = Lifecycle.State.STARTED
            assertEquals(Lifecycle.State.STARTED, owner.lifecycle.currentState)
            owner.detach()
            host.lifecycle.currentState = Lifecycle.State.RESUMED
            assertEquals(Lifecycle.State.CREATED, owner.lifecycle.currentState, "Host resume must not resume a detached view")
            owner.attach()
            assertEquals(Lifecycle.State.RESUMED, owner.lifecycle.currentState)
            assertFalse(probe.cleared)
            host.lifecycle.currentState = Lifecycle.State.DESTROYED
            assertEquals(Lifecycle.State.DESTROYED, owner.lifecycle.currentState)
            assertTrue(probe.cleared)
            assertEquals(0, host.lifecycle.observerCount)
            owner.attach()
            owner.destroy()
            assertEquals(Lifecycle.State.DESTROYED, owner.lifecycle.currentState)
        }
    }

    @Test
    fun `native container detaches and reattaches the same view without disposing its composition`() {
        val session = HostLifecycleSession(HostLifecyclePreset.Detach, SDKConfig.Theme.Default)
        var rendered: View? = null
        var disposed = false
        var lifecycleOwner: LifecycleOwner? = null
        var shown by mutableStateOf(true)
        val probe = Probe()
        session.communityStore.viewModelStore.put("probe", probe)
        compose.setContent {
            if (shown) {
                DetachableCommunityView(session) {
                    rendered = LocalView.current
                    lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(Unit) { onDispose { disposed = true } }
                    Text("Community content")
                }
            }
        }
        compose.waitForIdle()
        val original = checkNotNull(rendered)
        compose.runOnIdle {
            assertTrue(original.isAttachedToWindow)
            assertSame(lifecycleOwner, original.findViewTreeLifecycleOwner())
            assertSame(session.communityStore.viewModelStore, original.findViewTreeViewModelStoreOwner()?.viewModelStore)
            assertEquals(Lifecycle.State.RESUMED, lifecycleOwner?.lifecycle?.currentState)
        }
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(original.isAttachedToWindow, "Pausing must exercise the attached view")
            assertEquals(Lifecycle.State.STARTED, lifecycleOwner?.lifecycle?.currentState)
        }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(Lifecycle.State.CREATED, lifecycleOwner?.lifecycle?.currentState)
            assertFalse(probe.cleared, "Backgrounding must keep the store")
            assertFalse(disposed, "Backgrounding must keep the composition")
        }
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(Lifecycle.State.RESUMED, lifecycleOwner?.lifecycle?.currentState)
            session.covered = true
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertFalse(original.isAttachedToWindow)
            assertEquals(Lifecycle.State.CREATED, lifecycleOwner?.lifecycle?.currentState)
            assertFalse(probe.cleared)
            assertFalse(disposed)
            session.covered = false
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertSame(original, rendered)
            assertTrue(original.isAttachedToWindow)
            assertEquals(Lifecycle.State.RESUMED, lifecycleOwner?.lifecycle?.currentState)
            assertFalse(probe.cleared)
            assertFalse(disposed)
            shown = false
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(Lifecycle.State.DESTROYED, lifecycleOwner?.lifecycle?.currentState)
            assertTrue(probe.cleared)
            assertTrue(disposed)
        }
    }
}

@Composable
private fun RevisionAwareCommunity(
    session: HostLifecycleSession,
    onComposed: (Boolean) -> Unit,
) {
    val revision = session.revision
    SideEffect { session.appliedRevision.value = revision }
    DisposableEffect(Unit) {
        onComposed(true)
        onDispose { onComposed(false) }
    }
    Text("Community content")
}
