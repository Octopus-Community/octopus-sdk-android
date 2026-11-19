package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnAttach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.sample.ui.theme.CommunityTheme
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.home.OctopusHomeContent
import com.octopuscommunity.sdk.ui.octopusComposables

@Composable
internal fun HostLifecycleContent(session: HostLifecycleSession) {
    DisposableEffect(session) {
        onDispose {
            session.communityStore.viewModelStore.clear()
            session.disposed.complete(Unit)
        }
    }
    when (session.preset) {
        HostLifecyclePreset.TabBack -> HostTabDisplay(session)
        HostLifecyclePreset.Detach -> DetachableCommunityView(session)
        else -> HostCommunity(session)
    }
}

@Composable
private fun HostCommunity(session: HostLifecycleSession) {
    CompositionLocalProvider(LocalViewModelStoreOwner provides session.communityStore) {
        val nav = rememberNavController()
        val revision = session.revision
        SideEffect { session.navController = nav }
        val openLogin: () -> Unit = { nav.navigate("host-lifecycles-login") }
        val openProfileEdit: (ProfileField?) -> Unit = { nav.navigate("host-lifecycles-profile-edit") }
        CommunityTheme(session.theme) {
            OctopusContent {
                val view = LocalView.current
                // The SDK shows one or the other depending on the community's tone (internal ticket).
                val openPostLabels =
                    setOf(
                        stringResource(com.octopuscommunity.sdk.ui.R.string.accessibility_post_list_open_detail_hint_formal),
                        stringResource(com.octopuscommunity.sdk.ui.R.string.accessibility_post_list_open_detail_hint_informal),
                    )
                SideEffect {
                    session.semantics = HostCommunitySemantics(view, openPostLabels)
                }
                NavHost(
                    navController = nav,
                    startDestination = "host-lifecycles-home",
                    modifier = Modifier.fillMaxSize().testTag(HOST_COMMUNITY_CONTENT_TAG).semantics { testTagsAsResourceId = true },
                ) {
                    composable("host-lifecycles-home") {
                        HostDestinationContent(session) {
                            OctopusHomeContent(
                                navController = nav,
                                onNavigateToLogin = openLogin,
                                onNavigateToProfileEdit = openProfileEdit,
                            )
                        }
                    }
                    composable("host-lifecycles-login") {
                        HostDestinationContent(session) {
                            Text("Host login — connect from Account, then replay this preset.")
                        }
                    }
                    composable("host-lifecycles-profile-edit") {
                        HostDestinationContent(session) {
                            Text("Host profile editor — edit your profile from Account, then replay this preset.")
                        }
                    }
                    octopusComposables(
                        navController = nav,
                        onNavigateToLogin = openLogin,
                        onNavigateToProfileEdit = openProfileEdit,
                        container = { entry, content ->
                            val modifier =
                                if (entry.destination.hasRoute<OctopusDestination.PostDetails>()) {
                                    Modifier.testTag(HOST_POST_CONTENT_TAG)
                                } else {
                                    Modifier
                                }
                            HostDestinationContent(session) {
                                Box(modifier.fillMaxSize()) { content() }
                            }
                        },
                    )
                }
            }
        }
        LaunchedEffect(revision) {
            settleHostFrames()
            session.ready.complete(Unit)
        }
    }
}

// A changed static local disables skipping throughout the destination's existing subtree.
// A revision read only above NavHost would leave equal-argument SDK screens skipped.
private val LocalHostDestinationRevision = staticCompositionLocalOf { 0 }

@Composable
private fun HostDestinationContent(
    session: HostLifecycleSession,
    content: @Composable () -> Unit,
) {
    val revision = session.revision
    CompositionLocalProvider(LocalHostDestinationRevision provides revision) {
        content()
        SideEffect { session.appliedRevision.value = revision }
    }
}

@Composable
internal fun HostTabDisplay(
    session: HostLifecycleSession,
    community: @Composable () -> Unit = { HostCommunity(session) },
) {
    // Exactly ONE store per tab, owned ABOVE NavDisplay. No Nav3 view-model decorator:
    // that decorator defers clearing until after the animation and would miss the regression.
    val homeStore = remember { HostTabStoreOwner() }
    val backStack = remember { mutableStateListOf("home", "community") }
    // This is an embedded test host, not part of the scenario's navigation history.
    // Neither Nav3 nor its nested NavHost may register on the Activity's dispatchers.
    val navigationOwner = rememberNavigationEventDispatcherOwner(parent = null)
    val lifecycleOwner = LocalLifecycleOwner.current
    val backOwner =
        remember(lifecycleOwner) {
            object : OnBackPressedDispatcherOwner {
                override val lifecycle = lifecycleOwner.lifecycle
                override val onBackPressedDispatcher = OnBackPressedDispatcher()
            }
        }
    val pop: () -> Unit = {
        if (backStack.lastOrNull() == "community") {
            session.communityStore.viewModelStore.clear()
            session.revision++
            backStack.removeAt(backStack.lastIndex)
        }
    }
    // The preset invokes exactly the hosted NavDisplay's onBack callback, never a dispatcher.
    SideEffect { session.popTab = pop }
    DisposableEffect(homeStore) { onDispose { homeStore.viewModelStore.clear() } }
    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides navigationOwner,
        LocalOnBackPressedDispatcherOwner provides backOwner,
    ) {
        Column(Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.weight(1f),
                onBack = pop,
                transitionSpec = { fadeIn(tween(800)) togetherWith fadeOut(tween(800)) },
                popTransitionSpec = { fadeIn(tween(800)) togetherWith fadeOut(tween(800)) },
                entryProvider = { tab ->
                    NavEntry(tab) {
                        if (tab == "community") {
                            DisposableEffect(session) {
                                onDispose { session.tabExited.complete(Unit) }
                            }
                            community()
                        } else {
                            CompositionLocalProvider(LocalViewModelStoreOwner provides homeStore) {
                                Text("Host home — community tab store cleared on Back")
                            }
                        }
                    }
                },
            )
            NavigationBar {
                listOf("home", "community").forEach { tab ->
                    NavigationBarItem(
                        selected = backStack.last() == tab,
                        // A completed preset stays inspectable; replay it using its preset button.
                        onClick = { if (tab == "home") pop() },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(tab) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun DetachableCommunityView(
    session: HostLifecycleSession,
    community: @Composable () -> Unit = { HostCommunity(session) },
) {
    val context = LocalContext.current
    // The enclosing screen lifecycle is itself capped by the Activity lifecycle.
    val hostLifecycle = LocalLifecycleOwner.current.lifecycle
    val owner = remember(session, hostLifecycle) { HostAttachedViewOwner(session.communityStore.viewModelStore, hostLifecycle) }
    val view =
        remember(session, owner) {
            ComposeView(context).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                addOnAttachStateChangeListener(
                    object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(view: View) {
                            owner.attach()
                        }

                        override fun onViewDetachedFromWindow(view: View) {
                            owner.detach()
                        }
                    },
                )
                // Keep composition/store during a push, but deliver the host's pause/stop/resume.
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    CompositionLocalProvider(LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                        community()
                    }
                }
            }
        }
    DisposableEffect(view) {
        owner.observeHost()
        onDispose {
            view.disposeComposition()
            owner.destroy()
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { FrameLayout(it) },
        update = { container ->
            if (session.covered) {
                if (view.parent === container) container.removeView(view)
                if (container.childCount == 0) {
                    container.addView(TextView(context).apply { text = "Host screen pushed over community" })
                }
                check(!view.isAttachedToWindow)
            } else {
                if (view.parent == null) {
                    container.removeAllViews()
                    container.addView(view, ViewGroup.LayoutParams(-1, -1))
                }
                // AndroidView's initial update can run before its container reaches the window.
                val acknowledgement = session.coverApplied
                view.doOnAttach { acknowledgement.complete(Unit) }
            }
            if (session.covered) session.coverApplied.complete(Unit)
        },
    )
}

/** Native embedded host: the lesser of window attachment and the enclosing host lifecycle. */
internal class HostAttachedViewOwner(
    override val viewModelStore: ViewModelStore,
    private val hostLifecycle: Lifecycle,
) : LifecycleOwner,
    ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.CREATED }
    private var attached = false
    private val hostObserver = LifecycleEventObserver { _, _ -> syncState() }
    override val lifecycle: Lifecycle get() = registry

    fun observeHost() {
        hostLifecycle.addObserver(hostObserver)
        syncState()
    }

    fun attach() {
        attached = true
        syncState()
    }

    fun detach() {
        attached = false
        syncState()
    }

    private fun syncState() {
        if (registry.currentState == Lifecycle.State.DESTROYED) return
        val hostState = hostLifecycle.currentState
        if (hostState == Lifecycle.State.DESTROYED) {
            destroy()
        } else if (hostState.isAtLeast(Lifecycle.State.CREATED)) {
            registry.currentState = minOf(hostState, if (attached) Lifecycle.State.RESUMED else Lifecycle.State.CREATED)
        }
    }

    fun destroy() {
        if (registry.currentState == Lifecycle.State.DESTROYED) return
        hostLifecycle.removeObserver(hostObserver)
        registry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
