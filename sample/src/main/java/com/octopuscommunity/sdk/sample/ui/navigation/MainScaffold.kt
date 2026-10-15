package com.octopuscommunity.sdk.sample.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.octopuscommunity.sdk.domain.model.OctopusEvent
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.sample.data.AppUpdateChecker
import com.octopuscommunity.sdk.sample.data.CommunitySessionTracker
import com.octopuscommunity.sdk.sample.data.SampleSdkStatus
import com.octopuscommunity.sdk.sample.data.model.DebugEntry
import com.octopuscommunity.sdk.sample.data.model.FeatureToggles
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.ui.community.onClientProfileTap
import com.octopuscommunity.sdk.sample.ui.components.SampleTopAppBar
import com.octopuscommunity.sdk.sample.ui.components.SampleTopAppBarSize
import com.octopuscommunity.sdk.sample.ui.components.rememberIsOnline
import com.octopuscommunity.sdk.sample.ui.console.DebugConsoleSheet
import com.octopuscommunity.sdk.sample.ui.console.SendFeedbackSheet
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenarioCatalog
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenarioDetailScreen
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenarioSection
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenariosListScreen
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenariosViewModel
import com.octopuscommunity.sdk.sample.ui.settings.AboutScreen
import com.octopuscommunity.sdk.sample.ui.settings.AccountScreen
import com.octopuscommunity.sdk.sample.ui.settings.AppearanceScreen
import com.octopuscommunity.sdk.sample.ui.settings.DebugInfoScreen
import com.octopuscommunity.sdk.sample.ui.settings.DeveloperToolsScreen
import com.octopuscommunity.sdk.sample.ui.settings.EventsScreen
import com.octopuscommunity.sdk.sample.ui.settings.LanguageScreen
import com.octopuscommunity.sdk.sample.ui.settings.SettingsScreen
import com.octopuscommunity.sdk.sample.ui.theme.CommunityTheme
import com.octopuscommunity.sdk.sample.ui.theme.SampleMotion
import com.octopuscommunity.sdk.sample.ui.theme.SampleTheme
import com.octopuscommunity.sdk.sample.ui.theme.communityColorScheme
import com.octopuscommunity.sdk.sample.ui.theme.rememberSharedAxisX
import com.octopuscommunity.sdk.sample.ui.theme.sampleBottomBarColor
import com.octopuscommunity.sdk.sample.ui.theme.samplePageColor
import com.octopuscommunity.sdk.sample.ui.theme.samplePageContentColor
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.navigateToOctopusActivity
import com.octopuscommunity.sdk.ui.navigateToOctopusActivityByClientUserId
import com.octopuscommunity.sdk.ui.navigateToOctopusCreatePost
import com.octopuscommunity.sdk.ui.navigateToOctopusGroup
import com.octopuscommunity.sdk.ui.navigateToOctopusHome
import com.octopuscommunity.sdk.ui.navigateToOctopusPost
import com.octopuscommunity.sdk.ui.navigateToOctopusProfile
import com.octopuscommunity.sdk.ui.navigateToOctopusProfileByClientUserId
import com.octopuscommunity.sdk.ui.octopusComposables
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.octopuscommunity.sdk.sample.ui.community.CommunityTab as CommunityTabContent

// --- Tab routes ---------------------------------------------------------------------------------

/**
 * The scenario catalog, and the tab the sample opens on (DS v1: Scenarios · Community · Settings;
 * the Home and API Explorer tabs were dropped, their capabilities rehomed — inventory in internal change).
 *
 * [section] is the anchor a link passes when it is about one section — a scenario's "Change it in
 * Scenarios" line, for the section whose header owns the switch: the list opens with that section
 * expanded and scrolled to. `null` is the plain tab. An optional argument renders as a query suffix
 * of the destination route (`…ScenariosTab?section={section}`), which is why [MainScaffold]
 * compares routes without it.
 */
@Serializable
data class ScenariosTab(
    val section: ScenarioSection? = null,
)

/** Owns the feed and every SDK detail, including its saved tab stack. */
@Serializable
internal data object CommunityGraph

@Serializable
data object CommunityTab

@Serializable
data object SettingsTab

/**
 * One scenario's detail screen, reached from a card on [ScenariosListScreen] (the [ScenariosTab]
 * destination).
 *
 * A **flat sibling of [ScenariosTab]** in `tabNavController`'s graph — both are `composable<…>`
 * entries of the same `NavHost` — and not in `Routes.kt`'s root graph. The tab graph is what keeps
 * the tab bar on screen while the tester walks into a scenario and back out, which is the same
 * reason the Settings sub-destinations below live there; being a sibling rather than a child is
 * simply how this `NavHost` is built, and nothing here depends on a parent-child relation.
 */
@Serializable
data class ScenarioDetailRoute(
    val scenarioId: String,
)

// --- Settings sub-destinations -------------------------------------------------------------------
//
// All in the tab graph, not the root one: they are branches of the Settings summary, so the tab
// bar has to stay put while the tester walks down and back up. Pushing them at root level (which
// is where Language and Events log used to live, as top-level duplicates) is exactly what let the
// same screen be reachable from two places with two different pieces of chrome.

@Serializable
data object AccountRoute

@Serializable
data object AppearanceRoute

@Serializable
data object LanguageSettingsRoute

@Serializable
data object DeveloperToolsRoute

@Serializable
data object EventsLogRoute

@Serializable
data object DebugInfoRoute

@Serializable
data object AboutRoute

private enum class Tab(
    val label: String,
    val icon: ImageVector,
    val route: Any,
    // shared shell contract (`scenario catalog`) names the catalog
    // tab "scenarios" — the visible label matches that name too.
    val testId: String,
) {
    SCENARIOS("Scenarios", Icons.Default.Science, ScenariosTab(), "scenarios-tab"),
    COMMUNITY("Community", Icons.Default.Forum, CommunityGraph, "community-tab"),
    SETTINGS("Settings", Icons.Default.Settings, SettingsTab, "settings-tab"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScaffold(
    rootNavController: NavHostController,
    connectionState: ConnectionState,
    sdkEvents: List<OctopusEvent>,
    debugLog: List<DebugEntry>,
    onClearEvents: () -> Unit,
    onClearDebugLog: () -> Unit,
    /**
     * Writes one line to the Debug console. Flipping a feature toggle is the caller of record: the
     * design contract requires the console to say `feature toggled -> SDK config rebuilt` every
     * time a switch of the Scenarios tab moves, which is the developer-side half of the effect a
     * toggle owes.
     */
    onLogDebug: (DebugEntry.Category, String, String?) -> Unit = { _, _, _ -> },
    theme: SDKConfig.Theme = SDKConfig.Theme.Default,
    hostNavigation: CommunityHostNavigation,
    communityNavigation: SDKConfig.CommunityNavigation = SDKConfig.CommunityNavigation.InTab,
    openUnifiedProfile: Boolean = false,
    onUnifiedProfileOpened: () -> Unit = {},
    pendingPostId: String? = null,
    pendingDeepLink: String? = null,
    onContentLinkConsumed: () -> Unit = {},
    onSdkDestinationChanged: (Boolean) -> Unit = {},
    appUpdateStatus: AppUpdateChecker.Status = AppUpdateChecker.Status.Unknown,
    onCheckForUpdate: () -> Unit = {},
    onStartUpdate: () -> Unit = {},
    // The two host containers the `sheet` and `modal` presentation scenarios drive, opened by the
    // Activity that owns them (see MainActivity). `null` postId = plain open; a postId means the
    // deep-link preset, which must land inside the container it just opened.
    onOpenCommunityInSheet: (postId: String?) -> Unit = {},
    onOpenCommunityInFullScreenModal: (postId: String?) -> Unit = {},
    serverEnvironment: SDKConfig.ServerEnvironment = SDKConfig.ServerEnvironment.Default,
    // Feeds the Scenarios status bands. MainScaffold is only reached once the SDK has started, so
    // the default is the honest value for every caller that does not track it.
    isSdkInitialized: Boolean = true,
) {
    // Home used to carry the Play update card and its tab badge; both moved with DS v1 — the card to
    // Settings › About, the badge to the Settings tab (MainActivity's one-shot snackbar is unchanged).
    val isUpdateAvailable = appUpdateStatus is AppUpdateChecker.Status.UpdateAvailable
    val tabNavController = rememberContentNavController()
    // NavHost remembers its graph builder; its destination lambdas must read the live mode.
    val currentCommunityNavigation by rememberUpdatedState(communityNavigation)
    LaunchedEffect(openUnifiedProfile) {
        if (openUnifiedProfile) {
            tabNavController.navigate(ScenariosTab()) {
                popUpTo(tabNavController.graph.findStartDestination().id)
                launchSingleTop = true
            }
            tabNavController.navigate(ScenarioDetailRoute("unifiedProfile"))
            onUnifiedProfileOpened()
        }
    }

    val sharedAxisX = rememberSharedAxisX()
    val isOnline by rememberIsOnline()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    // Without the query suffix optional arguments add (ScenariosTab's `?section={section}`), so the
    // `==` checks below against a route's qualified name keep matching a tab that takes one.
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore('?')

    val currentTab = tabOf(navBackStackEntry?.destination)
    val isSdkDestination = currentTab == Tab.COMMUNITY && currentRoute != CommunityTab::class.qualifiedName
    LaunchedEffect(isSdkDestination) { onSdkDestinationChanged(isSdkDestination) }

    // Hoisted once here rather than resolved per-destination: koinViewModel() scopes to the
    // enclosing NavBackStackEntry, so re-resolving it inside ScenarioDetailScreen's own
    // destination would give every scenario its own instance and fragment state (e.g. a fetched
    // groups list forgotten when navigating from `groups` to `syncFollowGroups` and back). The
    // Community tab badge reads the same instance's unread count.
    val scenariosViewModel: ScenariosViewModel = koinViewModel()
    val scenariosState by scenariosViewModel.state.collectAsStateWithLifecycle()
    val unifiedProfileActive by scenariosViewModel.unifiedProfileActive.collectAsStateWithLifecycle()

    // The live position of every feature toggle, derived from the persisted config rather than
    // held as its own state: the toggles *are* config fields, and the Scenarios header, the
    // scenario screens and Debug info must never disagree about where a switch sits.
    val featureToggles: FeatureToggles =
        scenariosState.sdkConfig?.featureToggles ?: FeatureToggles.Default

    // Debug console state
    var showDebugConsole by rememberSaveable { mutableStateOf(false) }
    var showSendFeedback by rememberSaveable { mutableStateOf(false) }

    // The Community tab embeds the SDK's own home content, whose ViewModels are scoped to that
    // tab's back-stack entry and hold the repositories of the container they were built from.
    // `switchCommunity` replaces that container, so without this the tab keeps rendering the
    // previous community — blank, since the switch also clears the database — until the entry is
    // rebuilt (internal change). Scoped to the tab graph on purpose: keying the *root* NavHost on the config
    // is what raced `startDestination` during reset/start (internal ticket / internal change).
    val communitySession by
        koinInject<CommunitySessionTracker>().generation.collectAsStateWithLifecycle()
    ReloadCommunityTabOnSessionChange(
        navController = tabNavController,
        currentSession = communitySession,
        currentMode = communityNavigation,
        isCommunityTabOnScreen = currentTab == Tab.COMMUNITY,
    )

    // The last place the tester was that was not part of the Settings tree, as a breadcrumb —
    // "Scenarios > connection", "Community". Send feedback is opened from Settings, so
    // without this the one field a defect report cannot be reconstructed without would read
    // "Settings" every time.
    var lastContentBreadcrumb by rememberSaveable { mutableStateOf(Tab.SCENARIOS.label) }
    val breadcrumb =
        when {
            currentRoute == null -> null
            currentRoute in settingsSubRoutes -> null
            currentRoute == SettingsTab::class.qualifiedName -> null
            currentRoute.startsWith(ScenarioDetailRoute::class.qualifiedName!!) ->
                "Scenarios \u203A " +
                    (navBackStackEntry?.toRoute<ScenarioDetailRoute>()?.scenarioId ?: "scenario")

            currentTab == Tab.COMMUNITY -> "Community"
            else -> "Scenarios"
        }
    LaunchedEffect(breadcrumb) {
        if (breadcrumb != null) lastContentBreadcrumb = breadcrumb
    }

    val pageContainer: @Composable (NavBackStackEntry, @Composable () -> Unit) -> Unit = { entry, content ->
        SampleTabPage(entry, onBack = { tabNavController.popBackStack() }, onDebug = { showDebugConsole = true }, content)
    }

    Scaffold(
        containerColor = samplePageColor,
        contentColor = samplePageContentColor,
        bottomBar = {
            NavigationBar(
                containerColor = sampleBottomBarColor,
                tonalElevation = 0.dp,
            ) {
                // DS TabBar: the active item sits on an `accent-container` pill, its icon in
                // `on-accent-container` and its label in `text`; inactive items in `text-secondary`.
                val ds = SampleTheme.colors
                val navItemColors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = ds.onAccentContainer,
                        selectedTextColor = ds.text,
                        indicatorColor = ds.accentContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        modifier = Modifier.testTag(tab.testId),
                        colors = navItemColors,
                        selected = currentTab == tab || (currentTab == null && tab == Tab.SCENARIOS),
                        onClick = { tabNavController.selectTab(tab.route, reselect = tab == Tab.COMMUNITY && currentTab == tab) },
                        icon = {
                            // Settings: a dot when a Play update is available (the card is on
                            // Settings › About). Community: the unread count Home's "Open
                            // community" button used to carry, capped like it was.
                            val unread = scenariosState.notSeenCount
                            BadgedBox(
                                badge = {
                                    when {
                                        tab == Tab.SETTINGS && isUpdateAvailable ->
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError,
                                                modifier = Modifier.testTag("settings-tab-update-badge"),
                                            )

                                        tab == Tab.COMMUNITY && unread > 0 ->
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError,
                                                modifier = Modifier.testTag("community-tab-unread-badge"),
                                            ) {
                                                Text(if (unread > 99) "+99" else unread.toString())
                                            }
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    // The Scaffold already applied its system-bar/IME insets as `innerPadding`.
                    // Consuming them here stops the embedded Octopus content (whose IME-aware
                    // screens re-apply `imePadding()`) from counting the same insets twice —
                    // the double-padding a nested Scaffold produces otherwise.
                    .consumeWindowInsets(innerPadding),
        ) {
            NavHost(
                navController = tabNavController,
                startDestination = ScenariosTab(),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .weight(1f),
                // Standard motion only: Material fade through between two tabs (the bottom bar
                // pattern), Material shared axis X for a screen pushed inside a tab, and a pop
                // that predictive back can seek without an empty frame (see SharedAxisX). The SDK
                // screens hosted here inherit the push/pop (internal change).
                enterTransition = { if (isTabSwitch()) SampleMotion.fadeThroughEnter else sharedAxisX.enter },
                exitTransition = { if (isTabSwitch()) SampleMotion.fadeThroughExit else sharedAxisX.exit },
                popEnterTransition = {
                    if (isTabSwitch(isPop = true)) SampleMotion.fadeThroughPopEnter else sharedAxisX.popEnter
                },
                popExitTransition = {
                    if (isTabSwitch(isPop = true)) SampleMotion.fadeThroughPopExit else sharedAxisX.popExit
                },
            ) {
                samplePage<ScenariosTab>(container = pageContainer) { backStackEntry ->
                    ScenariosListScreen(
                        apiKey = scenariosState.currentApiKey,
                        unifiedProfileActive = unifiedProfileActive,
                        anchorSection = backStackEntry.toRoute<ScenariosTab>().section,
                        sdkStatus = SampleSdkStatus.of(isSdkInitialized, connectionState, isOnline),
                        onConnectUser = { tabNavController.navigate(AccountRoute) },
                        onOpenCommunityTab = {
                            tabNavController.navigate(CommunityGraph) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenCommunityModal = { onOpenCommunityInFullScreenModal(null) },
                        onOpenCommunitySheet = { onOpenCommunityInSheet(null) },
                        // Same host-graph route as the `fullscreen` scenario's preset 1.
                        onOpenCommunityFullScreen = {
                            rootNavController.navigate(CommunityFullScreenRoute()) { launchSingleTop = true }
                        },
                        onScenarioClick = { scenarioId ->
                            tabNavController.navigate(ScenarioDetailRoute(scenarioId))
                        },
                        toggles = featureToggles,
                        onSetFeatureToggle = { toggle, enabled ->
                            scenariosViewModel.executeSetFeatureToggle(toggle, enabled)
                            onLogDebug(
                                DebugEntry.Category.STATE,
                                "feature toggled \u2192 SDK config rebuilt",
                                "${toggle.label} -> ${if (enabled) "on" else "off"}\n" +
                                    toggle.effectLine(enabled),
                            )
                        },
                    )
                }

                samplePage<ScenarioDetailRoute>(container = pageContainer) { backStackEntry ->
                    val route = backStackEntry.toRoute<ScenarioDetailRoute>()
                    ScenarioDetailScreen(
                        scenarioId = route.scenarioId,
                        viewModel = scenariosViewModel,
                        onOpenCreatePost = { info ->
                            rootNavController.navigateToOctopusCreatePost(info)
                        },
                        // The host's OWN profile screen — same destination the SDK's
                        // onNavigateToProfile callback lands on, reached here from the scenario
                        // instead of from a profile tap inside the embedded community.
                        onOpenClientProfile = { clientUserId ->
                            rootNavController.onClientProfileTap(clientUserId)
                        },
                        // These API demonstrations explicitly push SDK destinations on the host
                        // graph, as the createPost/clientProfile callbacks above do. External post
                        // and notification links separately follow Community navigation's mode.
                        onOpenPost = { postId -> rootNavController.navigateToOctopusPost(postId) },
                        onOpenComment = { commentId ->
                            rootNavController.navigate(OctopusDestination.CommentDetails(commentId))
                        },
                        onOpenBridgeTopic = { groupId -> rootNavController.navigateToOctopusGroup(groupId) },
                        onOpenHome = { rootNavController.navigateToOctopusHome() },
                        // The three presentation shapes. `fullscreen` is a route of the HOST graph,
                        // so it is pushed here like every other destination above; `sheet` and
                        // `modal` are containers the Activity owns, so they go back up to it.
                        onOpenCommunityFullScreen = { postId, withBottomBar ->
                            rootNavController.navigate(
                                CommunityFullScreenRoute(
                                    postId = postId,
                                    showHostBottomBar = withBottomBar,
                                ),
                            ) { launchSingleTop = true }
                        },
                        onOpenCommunitySheet = onOpenCommunityInSheet,
                        onOpenCommunityModal = onOpenCommunityInFullScreenModal,
                        // Unified Profile free-form navigation, restored from the pre-internal change legacy
                        // accordion (see ScenarioDetailScreen's "initialScreen" case): same
                        // rootNavController, same idiom as the callbacks above.
                        onOpenUserActivity = { userId -> rootNavController.navigateToOctopusActivity(userId) },
                        onOpenUserActivityByClientUserId = { clientUserId ->
                            rootNavController.navigateToOctopusActivityByClientUserId(clientUserId)
                        },
                        onOpenMyProfile = {
                            rootNavController.navigate(OctopusDestination.CurrentUserProfileSummary.Posts)
                        },
                        onOpenProfileByUserId = { userId -> rootNavController.navigateToOctopusProfile(userId) },
                        onOpenProfileByClientUserId = { clientUserId ->
                            rootNavController.navigateToOctopusProfileByClientUserId(clientUserId)
                        },
                        sdkEvents = sdkEvents,
                        onClearEvents = onClearEvents,
                        theme = theme,
                        onSetTheme = scenariosViewModel::executeSetTheme,
                        // Zone 4 of the scenario skeleton: an effect observable in the community
                        // is verified on the Community tab, not in a screenshot of a result blob.
                        onVerifyInCommunity = {
                            tabNavController.navigate(CommunityGraph) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        // The scenario screen shows a section's toggle and links back to it; it
                        // never flips it. "Change it in Scenarios" navigates to the list explicitly
                        // rather than trusting whatever a plain popBackStack would land on, and
                        // passes the section so the list opens on the header that owns the switch.
                        // The list entry below is replaced, not reused: a fresh entry is what lets
                        // the list apply the anchor exactly once (see ScenariosListScreen).
                        featureToggles = featureToggles,
                        onOpenScenarios = { section ->
                            tabNavController.navigate(ScenariosTab(section)) {
                                popUpTo<ScenariosTab> { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }

                navigation<CommunityGraph>(startDestination = CommunityTab) {
                    samplePage<CommunityTab>(container = pageContainer) {
                        CommunityTheme(theme = theme) {
                            CommunityTabContent(
                                navController = communityContentController(currentCommunityNavigation, rootNavController, tabNavController),
                                hostNavigation = hostNavigation,
                                connectionState = connectionState,
                                onConnectUser = { tabNavController.navigate(AccountRoute) },
                            )
                        }
                    }
                    octopusComposables(
                        navController = tabNavController,
                        onNavigateToLogin = hostNavigation.login,
                        onNavigateToProfileEdit = hostNavigation.profileEdit,
                        onNavigateToProfile = hostNavigation.profile,
                        onNavigateToClientObject = hostNavigation.clientObject,
                        onGroupAccessDenied = hostNavigation.groupAccessDenied,
                        onNavigateToUrl = hostNavigation.url,
                        container = { entry, content ->
                            val postId =
                                entry
                                    .takeIf { it.destination.hasRoute<OctopusDestination.PostDetails>() }
                                    ?.toRoute<OctopusDestination.PostDetails>()
                                    ?.postId
                            CommunityTheme(theme = theme) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(
                                            communityColorScheme(theme).background,
                                        ).then(postId?.let { Modifier.testTag(it) } ?: Modifier),
                                ) {
                                    content()
                                }
                            }
                        },
                    )
                }

                samplePage<SettingsTab>(container = pageContainer) {
                    SettingsScreen(
                        onNavigateToAccount = { tabNavController.navigate(AccountRoute) },
                        onNavigateToAppearance = { tabNavController.navigate(AppearanceRoute) },
                        onNavigateToLanguage = { tabNavController.navigate(LanguageSettingsRoute) },
                        onNavigateToServerAndCommunity = { rootNavController.navigate(ConfigRoute()) },
                        onNavigateToDeveloperTools = { tabNavController.navigate(DeveloperToolsRoute) },
                        onSendFeedback = { showSendFeedback = true },
                        onNavigateToAbout = { tabNavController.navigate(AboutRoute) },
                        isUpdateAvailable = isUpdateAvailable,
                    )
                }

                samplePage<AccountRoute>(container = pageContainer) {
                    AccountScreen(
                        onEditProfile = { rootNavController.navigate(EditUserRoute) },
                    )
                }

                samplePage<AppearanceRoute>(container = pageContainer) { AppearanceScreen() }

                samplePage<LanguageSettingsRoute>(container = pageContainer) { LanguageScreen() }

                samplePage<DeveloperToolsRoute>(container = pageContainer) {
                    DeveloperToolsScreen(
                        // Counts what the Events log actually lists, so the subtitle and the
                        // screen never disagree.
                        eventCount = debugLog.size,
                        onNavigateToHostIntegration = { rootNavController.navigate(HostIntegrationRoute) },
                        onNavigateToEvents = { tabNavController.navigate(EventsLogRoute) },
                        onNavigateToDebugInfo = { tabNavController.navigate(DebugInfoRoute) },
                        onOpenDebugConsole = { showDebugConsole = true },
                        featureToggles = featureToggles,
                    )
                }

                samplePage<EventsLogRoute>(container = pageContainer) {
                    EventsScreen(
                        // The merged feed, not the typed list: it is the only source that carries
                        // both timestamps and the host-side calls the origin pill needs.
                        log = debugLog,
                        onClearEvents = {
                            onClearEvents()
                            onClearDebugLog()
                        },
                    )
                }

                samplePage<DebugInfoRoute>(container = pageContainer) {
                    DebugInfoScreen()
                }

                samplePage<AboutRoute>(container = pageContainer) {
                    AboutScreen(
                        appUpdateStatus = appUpdateStatus,
                        onCheckForUpdate = onCheckForUpdate,
                        onStartUpdate = onStartUpdate,
                    )
                }
            }
        }
    }

    LaunchedEffect(pendingPostId, pendingDeepLink, communityNavigation, navBackStackEntry != null) {
        if (navBackStackEntry == null || (pendingPostId == null && pendingDeepLink == null)) return@LaunchedEffect
        val contentController = communityContentController(currentCommunityNavigation, rootNavController, tabNavController)
        if (communityNavigation == SDKConfig.CommunityNavigation.InTab) tabNavController.selectTab(CommunityGraph)
        if (pendingPostId != null) contentController.navigateToOctopusPost(pendingPostId)
        if (pendingDeepLink != null) contentController.navigate(android.net.Uri.parse(pendingDeepLink))
        onContentLinkConsumed()
    }

    if (showSendFeedback) {
        SendFeedbackSheet(
            // Where the tester *was*, not where they are: the sheet is always opened from
            // Settings, so reporting "Settings" would throw away the one piece of context a
            // defect report cannot be reconstructed without.
            where = lastContentBreadcrumb,
            config = scenariosState.sdkConfig,
            debugLog = debugLog,
            onDismiss = { showSendFeedback = false },
        )
    }

    // Debug console bottom sheet
    if (showDebugConsole) {
        DebugConsoleSheet(
            entries = debugLog,
            onDismiss = { showDebugConsole = false },
            onClear = onClearDebugLog,
            serverEnvironment = serverEnvironment,
        )
    }
}

/** Routes that keep the Settings tab highlighted while the tester is inside its tree. */
private val settingsSubRoutes: Set<String?> =
    setOf(
        AccountRoute::class.qualifiedName,
        AppearanceRoute::class.qualifiedName,
        LanguageSettingsRoute::class.qualifiedName,
        DeveloperToolsRoute::class.qualifiedName,
        EventsLogRoute::class.qualifiedName,
        DebugInfoRoute::class.qualifiedName,
        AboutRoute::class.qualifiedName,
    )

/**
 * The tab a destination route belongs to: its root, or a screen pushed inside it. A route renders
 * as `<qualifiedName>[/{arg}][?arg={arg}]`, so it is compared without its arguments.
 */
private fun tabOf(route: String?): Tab? {
    val name = route?.substringBefore('?')?.substringBefore('/') ?: return null
    return Tab.entries.firstOrNull { it.route::class.qualifiedName == name }
        ?: when (name) {
            CommunityTab::class.qualifiedName -> Tab.COMMUNITY
            ScenarioDetailRoute::class.qualifiedName -> Tab.SCENARIOS
            in settingsSubRoutes -> Tab.SETTINGS
            else -> if (name.startsWith(OctopusDestination::class.qualifiedName + ".")) Tab.COMMUNITY else null
        }
}

/**
 * A bottom-bar switch leaves another tab's tree, including when a saved detail is restored.
 * SDK destinations belong to Community; sample routes are mapped to their host tab. Account can
 * be pushed from any tab, so it is excluded when returning to the screen that opened it.
 *
 * A route no tab claims is never a switch: without a known origin the move is a plain push or pop.
 * On a pop ([isPop]), Account belongs to no tab either: Scenarios and Community push it too, so
 * popping it back to either root returns to the screen that pushed it, not to another tab.
 */
internal fun isTabSwitch(
    initialRoute: String?,
    targetRoute: String?,
    isPop: Boolean = false,
): Boolean {
    val target = tabOf(targetRoute) ?: return false
    val targetName = targetRoute?.substringBefore('?')
    if (targetName == AccountRoute::class.qualifiedName) return false
    if (isPop && initialRoute?.substringBefore('?') == AccountRoute::class.qualifiedName) return false
    val initial = tabOf(initialRoute) ?: return false
    return initial != target
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(isPop: Boolean = false): Boolean =
    isTabSwitch(initialState.destination.route, targetState.destination.route, isPop)

/**
 * A tab-graph destination painted on the sample's page colour. The tab NavHost cross-fades and
 * slides two destinations over each other (fade through, shared axis X); a screen whose root is
 * transparent lets the other one show through for the whole overlap, which reads as two screens
 * superimposed. An opaque page behind every destination keeps each one whole while it moves.
 *
 * It is painted under the Community tab too: the embedded SDK content draws its own surface over
 * it, so at rest nothing changes, and during a switch the tab no longer shows the outgoing screen.
 */
private inline fun <reified T : Any> NavGraphBuilder.samplePage(
    noinline container: @Composable (NavBackStackEntry, @Composable () -> Unit) -> Unit,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T> { entry ->
        val scope = this
        container(entry) { scope.content(entry) }
    }
}

private fun tabOf(destination: NavDestination?): Tab? =
    if (destination?.hierarchy?.any { it.hasRoute<CommunityGraph>() } == true) Tab.COMMUNITY else tabOf(destination?.route)

/** Switches preserve a tab's entire stack; a second tap returns to that tab's feed/root. */
internal fun NavHostController.selectTab(
    route: Any,
    reselect: Boolean = false,
) {
    if (reselect) {
        val root = if (route == CommunityGraph) CommunityTab else route
        popBackStack(root, inclusive = false)
        return
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SampleTabPage(
    entry: NavBackStackEntry,
    onBack: () -> Unit,
    onDebug: () -> Unit,
    content: @Composable () -> Unit,
) {
    val route = entry.destination.route?.substringBefore('?')
    val isRoot =
        route == ScenariosTab::class.qualifiedName ||
            route == CommunityTab::class.qualifiedName ||
            route == SettingsTab::class.qualifiedName
    val title =
        when {
            route?.startsWith(ScenarioDetailRoute::class.qualifiedName!!) == true ->
                ScenarioCatalog.find(entry.toRoute<ScenarioDetailRoute>().scenarioId)?.title ?: "Scenario"
            route == CommunityTab::class.qualifiedName -> "Community"
            route == SettingsTab::class.qualifiedName -> "Settings"
            route == AccountRoute::class.qualifiedName -> "Account"
            route == AppearanceRoute::class.qualifiedName -> "Appearance"
            route == LanguageSettingsRoute::class.qualifiedName -> "Language"
            route == DeveloperToolsRoute::class.qualifiedName -> "Developer tools"
            route == EventsLogRoute::class.qualifiedName -> "Events log"
            route == DebugInfoRoute::class.qualifiedName -> "Debug info"
            route == AboutRoute::class.qualifiedName -> "About"
            else -> "Scenarios"
        }
    val size =
        when {
            route == SettingsTab::class.qualifiedName -> SampleTopAppBarSize.Large
            route == ScenariosTab::class.qualifiedName ||
                route?.startsWith(
                    ScenarioDetailRoute::class.qualifiedName!!,
                ) == true -> SampleTopAppBarSize.Medium
            else -> SampleTopAppBarSize.Small
        }
    val barState = rememberSaveable(saver = TopAppBarState.Saver) { TopAppBarState(-Float.MAX_VALUE, 0f, 0f) }
    val scrollBehavior =
        if (size ==
            SampleTopAppBarSize.Small
        ) {
            TopAppBarDefaults.pinnedScrollBehavior(barState)
        } else {
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(barState)
        }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = samplePageColor,
        contentColor = samplePageContentColor,
        topBar = {
            SampleTopAppBar(
                title = title,
                size = size,
                scrollBehavior = scrollBehavior,
                showPlatformBadge = isRoot,
                navigationIcon = {
                    if (!isRoot) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onDebug, modifier = Modifier.testTag("debug-open-button")) {
                        Icon(Icons.Default.BugReport, contentDescription = "Debug console")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) { content() }
    }
}
