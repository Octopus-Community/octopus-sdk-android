package com.octopuscommunity.sdk.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.navigation.NavHostController
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.access.CommunityAccessDeniedContent
import com.octopuscommunity.sdk.ui.access.RegisterCommunityRoot
import com.octopuscommunity.sdk.ui.access.popPushedOctopusScreens
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.NavigateTo
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.NavigateToClientLogin
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.NavigateToClientObject
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.NavigateToClientProfileEdit
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.NavigateToGroupAccessDenied
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.NavigateToProfile
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.NavigateToUrl
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.NavigateUp
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.PopBackStack
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.PopBackStackOrNavigateTo
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.PopBackStackTo
import com.octopuscommunity.sdk.ui.common.OctopusNavigator
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerHost
import com.octopuscommunity.sdk.ui.common.viewmodel.GamificationAutoPrompt
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusScreenViewModel
import com.octopuscommunity.sdk.ui.common.viewmodel.ProfileTapTarget
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.common.viewmodel.resolveProfileTapTarget
import com.octopuscommunity.sdk.ui.common.viewmodel.unifiedProfileActive
import com.octopuscommunity.sdk.ui.gamification.OctopusGamificationSheet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import org.koin.compose.getKoin

@Composable
internal fun OctopusScreenContent(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    gamificationAutoPrompt: GamificationAutoPrompt = GamificationAutoPrompt.None,
    communityRoot: Boolean = false,
    content: @Composable BoxScope.(OctopusScreenViewModel) -> Unit = {},
) = OctopusContent(navController) {
    // The navigation collector survives recomposition; read the latest host wiring on each event.
    val navigationHandler by rememberUpdatedState(LocalNavigationHandler.current)
    val uriHandler by rememberUpdatedState(LocalUriHandler.current)

    val viewModel = octopusViewModel<OctopusScreenViewModel>()
    val activity = LocalActivity.current

    // A screen counts towards the Octopus session while it is displayed, like iOS onAppear and
    // onDisappear, not merely while it is composed: a React Native view keeps its composition
    // while hidden or detached and only lowers its view-tree lifecycle to CREATED. The view-tree
    // owner hosts the whole navigation, so moving between Octopus destinations (whose back-stack
    // entries do drop to CREATED) never reaches it.
    val view = LocalView.current
    val hostLifecycle = remember(view) { view.findViewTreeLifecycleOwner()?.lifecycle }
    val activityLifecycle = (activity as? LifecycleOwner)?.lifecycle
    DisposableEffect(viewModel, hostLifecycle, activityLifecycle) {
        var displayed = false

        fun update(next: Boolean) {
            if (next == displayed) return
            displayed = next
            if (next) viewModel.onOctopusScreenStarted() else viewModel.onOctopusScreenStopped()
        }
        val observer =
            LifecycleEventObserver { _, _ ->
                update(nextScreenDisplayed(displayed, hostLifecycle!!.currentState, activityLifecycle?.currentState))
            }
        // Without a view-tree owner, keep the composition-scoped behaviour.
        if (hostLifecycle == null) update(true) else hostLifecycle.addObserver(observer)
        onDispose {
            hostLifecycle?.removeObserver(observer)
            update(false)
        }
    }

    val registration = remember(viewModel) { Any() }
    var isOnScreen by remember(viewModel) { mutableStateOf(false) }
    if (isOnScreen) {
        DisposableEffect(viewModel, registration) {
            viewModel.onOctopusScreenVisible(registration)
            onDispose {
                // A configuration change recreates every screen within the same UI opening.
                viewModel.onOctopusScreenHidden(registration, activity?.isChangingConfigurations == true)
            }
        }
    }

    val navigator = viewModel.navigator
    // Direct Koin consumers and standalone components retain the shared, pre-host-scope path.
    val rootNavigator = getKoin().get<OctopusNavigator>()

    val navigateToLogin by rememberUpdatedState(navigationHandler?.onNavigateToLogin)
    val navigateToProfileEdit by rememberUpdatedState(navigationHandler?.onNavigateToProfileEdit)

    val showCommunityAccessDenied by viewModel.showCommunityAccessDenied.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()

    if (communityRoot) RegisterCommunityRoot(navController)

    // The access denied screen stands in place of this one: the screens pushed over the community are left, so that
    // back does not show it once per screen. Nothing is left to pop from the first screen of the community.
    LaunchedEffect(showCommunityAccessDenied, navController) {
        if (showCommunityAccessDenied) navController.popPushedOctopusScreens()
    }

    LifecycleResumeEffect(viewModel, gamificationAutoPrompt, showCommunityAccessDenied to isOnScreen) {
        viewModel.setGamificationScreenVisible(isOnScreen && !showCommunityAccessDenied, gamificationAutoPrompt)
        onPauseOrDispose {
            viewModel.setGamificationScreenVisible(false, gamificationAutoPrompt)
        }
    }

    LaunchedEffect(navigator, rootNavigator, navController) {
        val events =
            if (navigator === rootNavigator) {
                navigator.navigationEvents
            } else {
                merge(navigator.navigationEvents, rootNavigator.navigationEvents)
            }
        events.collectLatest { action ->
            when (action) {
                is NavigateTo ->
                    navController.navigate(action.destination) {
                        restoreState = true
                        action.navOptions(this)
                    }

                is NavigateUp ->
                    if (!navController.popBackStack()) {
                        // We're at the START/root of the SDK's internal back stack
                        // (bridge mode: the host opened the SDK directly on this
                        // sub-screen), so there is nothing to pop. Fall back to the
                        // host back callback so the host can dismiss its container.
                        // On non-root screens popBackStack() succeeds and this branch
                        // is not reached.
                        navigationHandler?.onBack?.invoke()
                    }
                is PopBackStack -> navController.popBackStack()
                is PopBackStackTo ->
                    navController.popBackStack(
                        route = action.destination,
                        inclusive = action.inclusive,
                    )

                is PopBackStackOrNavigateTo ->
                    if (!navController.popBackStack(
                            route = action.destination,
                            inclusive = false,
                        )
                    ) {
                        navController.navigate(action.destination) {
                            popUpTo(navController.currentDestination?.route ?: "") {
                                inclusive = true
                            }
                        }
                    }

                is NavigateToClientLogin -> HostNavigationCallbacks.login(navigateToLogin)
                is NavigateToClientProfileEdit -> HostNavigationCallbacks.edit(navigateToProfileEdit, action.fieldToEdit)
                is NavigateToClientObject ->
                    navigationHandler?.onNavigateToClientObject?.invoke(
                        action.id,
                    )

                is NavigateToGroupAccessDenied ->
                    navigationHandler?.onGroupAccessDenied?.invoke(
                        action.groupId,
                    )

                is NavigateToUrl -> uriHandler.openUri(action.url)

                is NavigateToProfile -> {
                    val onNavigateToProfile = navigationHandler?.onNavigateToProfile
                    when (
                        resolveProfileTapTarget(
                            unifiedActive =
                                unifiedProfileActive(
                                    exposeClientUserId = action.exposeClientUserId,
                                    onNavigateToProfileWired = onNavigateToProfile != null,
                                ),
                            clientUserId = action.clientUserId,
                        )
                    ) {
                        ProfileTapTarget.HOST_CLIENT_PROFILE -> {
                            // Unified Profile active with a client id: hand it to the host's own
                            // profile screen (the SDK shows no Octopus screen). Both the wired callback
                            // and the non-null client id are guaranteed by resolveProfileTapTarget
                            // returning HOST_CLIENT_PROFILE; the native fallback is defensive only.
                            val clientUserId = action.clientUserId
                            if (onNavigateToProfile != null && clientUserId != null) {
                                onNavigateToProfile(clientUserId)
                            } else {
                                navController.navigate(action.destination) {
                                    restoreState = true
                                }
                            }
                        }

                        ProfileTapTarget.OCTOPUS_ACTIVITY ->
                            // Unified Profile active but the member has no client id (guest / BO /
                            // admin): open the posts-only Octopus activity screen instead of handing
                            // the host a null id.
                            navController.navigate(action.activityDestination) {
                                restoreState = true
                            }

                        ProfileTapTarget.NATIVE_PROFILE ->
                            navController.navigate(action.destination) {
                                restoreState = true
                            }
                    }
                }
            }
        }
    }

    Box(
        modifier =
            modifier
                .onGloballyPositioned { coordinates ->
                    // Clip against ancestors too: a pager may be smaller than the host window.
                    val visibleBounds = coordinates.boundsInWindow()
                    isOnScreen = visibleBounds.width > 0f && visibleBounds.height > 0f
                }.background(OctopusTheme.colorScheme.background),
    ) {
        if (showCommunityAccessDenied) {
            OctopusLocaleWrapper {
                CommunityAccessDeniedContent(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                    screenViewModel = viewModel,
                )
            }
        } else {
            // Thumbnails of this screen open their images in a full-screen viewer window.
            ImageViewerHost { content(viewModel) }
        }

        val config = state.communityConfig
        if (isOnScreen &&
            lifecycleState == Lifecycle.State.RESUMED &&
            !showCommunityAccessDenied &&
            state.showGamificationSheet &&
            config?.gamification != null
        ) {
            OctopusGamificationSheet(
                communityConfig = config,
                onDismissRequest = { viewModel.showGamificationSheet(false) },
            )
            // Like iOS onAppear, only the actual visible presentation marks the rules as seen.
            SideEffect { viewModel.onGamificationSheetShown() }
        }
    }

    val hostDialog by navigator.dialogState.collectAsStateWithLifecycle()
    val rootDialog by rootNavigator.dialogState.collectAsStateWithLifecycle()
    // Keep a host's own dialog in front; a pending shared dialog appears once it is dismissed.
    OctopusAlertDialog(hostDialog ?: rootDialog)
}
