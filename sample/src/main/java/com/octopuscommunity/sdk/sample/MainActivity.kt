package com.octopuscommunity.sdk.sample

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.sample.data.AppUpdateChecker
import com.octopuscommunity.sdk.sample.data.NotificationPermissionPrompt
import com.octopuscommunity.sdk.sample.data.SampleFixtures
import com.octopuscommunity.sdk.sample.data.consumeSampleLaunchExtras
import com.octopuscommunity.sdk.sample.data.datastore.SDKConfigDataStore
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.extensions.LocalExtensions
import com.octopuscommunity.sdk.sample.ui.community.ClientProfileScreen
import com.octopuscommunity.sdk.sample.ui.community.CommunityFullScreen
import com.octopuscommunity.sdk.sample.ui.community.DeepLinkToPostOnce
import com.octopuscommunity.sdk.sample.ui.community.handleUrl
import com.octopuscommunity.sdk.sample.ui.community.navigateToLogin
import com.octopuscommunity.sdk.sample.ui.community.navigateToProfileEdit
import com.octopuscommunity.sdk.sample.ui.components.SampleSnackbar
import com.octopuscommunity.sdk.sample.ui.config.ConfigScreen
import com.octopuscommunity.sdk.sample.ui.config.ConfigSection
import com.octopuscommunity.sdk.sample.ui.navigation.COMMUNITY_DEEP_LINK
import com.octopuscommunity.sdk.sample.ui.navigation.COMMUNITY_LINK_CONSUMED
import com.octopuscommunity.sdk.sample.ui.navigation.COMMUNITY_POST_ID
import com.octopuscommunity.sdk.sample.ui.navigation.ClientProfileRoute
import com.octopuscommunity.sdk.sample.ui.navigation.CommunityFullScreenRoute
import com.octopuscommunity.sdk.sample.ui.navigation.ConfigRoute
import com.octopuscommunity.sdk.sample.ui.navigation.EditUserRoute
import com.octopuscommunity.sdk.sample.ui.navigation.HostIntegrationRoute
import com.octopuscommunity.sdk.sample.ui.navigation.LoginRoute
import com.octopuscommunity.sdk.sample.ui.navigation.MainRoute
import com.octopuscommunity.sdk.sample.ui.navigation.MainScaffold
import com.octopuscommunity.sdk.sample.ui.navigation.SamplePostLinkRoute
import com.octopuscommunity.sdk.sample.ui.navigation.UNIFIED_PROFILE_SCENARIO
import com.octopuscommunity.sdk.sample.ui.navigation.communityHostNavigation
import com.octopuscommunity.sdk.sample.ui.navigation.finishSdkOnboarding
import com.octopuscommunity.sdk.sample.ui.navigation.navigateToDeepLink
import com.octopuscommunity.sdk.sample.ui.navigation.openCommunityDeepLink
import com.octopuscommunity.sdk.sample.ui.navigation.openSamplePostLink
import com.octopuscommunity.sdk.sample.ui.navigation.rememberContentNavController
import com.octopuscommunity.sdk.sample.ui.navigation.shouldRelayCommunityDeepLink
import com.octopuscommunity.sdk.sample.ui.navigation.unconsumedCommunityDeepLink
import com.octopuscommunity.sdk.sample.ui.settings.EditUserScreen
import com.octopuscommunity.sdk.sample.ui.settings.GroupAccessDeniedSheet
import com.octopuscommunity.sdk.sample.ui.settings.HostIntegrationScreen
import com.octopuscommunity.sdk.sample.ui.settings.LoginScreen
import com.octopuscommunity.sdk.sample.ui.theme.CommunityTheme
import com.octopuscommunity.sdk.sample.ui.theme.LocalSampleHostStatusBarColor
import com.octopuscommunity.sdk.sample.ui.theme.SampleAppTheme
import com.octopuscommunity.sdk.sample.ui.theme.SampleModalBottomSheet
import com.octopuscommunity.sdk.sample.ui.theme.SampleWindowFrame
import com.octopuscommunity.sdk.sample.ui.theme.SyncSampleSystemBars
import com.octopuscommunity.sdk.sample.ui.theme.communityColorScheme
import com.octopuscommunity.sdk.sample.ui.theme.rememberSharedAxisX
import com.octopuscommunity.sdk.sample.ui.theme.sampleBottomBarColor
import com.octopuscommunity.sdk.sample.ui.theme.sampleTopBarColor
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.components.NavigationIconType
import com.octopuscommunity.sdk.ui.components.UrlOpeningStrategy
import com.octopuscommunity.sdk.ui.home.OctopusHomeContent
import com.octopuscommunity.sdk.ui.home.OctopusHomeScreen
import com.octopuscommunity.sdk.ui.navigateToOctopusActivityByClientUserId
import com.octopuscommunity.sdk.ui.octopusComposables
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

@Serializable
private data object ModalCommunityHomeRoute

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()
    private val sdkConfigDataStore: SDKConfigDataStore by inject()

    private val notificationPermissionPrompt = NotificationPermissionPrompt()

    /**
     * The last deep link delivered through [onNewIntent], consumed by the composition. Held by
     * the activity rather than by a Compose `addOnNewIntentListener`: on a cold start the second
     * intent can be delivered before the first composition has registered any listener, and the
     * link was silently lost (internal change).
     */
    private var newIntentLink by mutableStateOf<Uri?>(null)

    /**
     * The only two places the sample registers a push token on its own — permission just granted,
     * or already granted at SDK init — both go through here, so the Notifications toggle on the
     * Scenarios tab gates every registration path and not only the token-rotation one in
     * [MessagingService]. A switch without an observable effect is a bug, not a setting.
     */
    private fun registerPushTokenIfEnabled() {
        lifecycleScope.launch {
            if (sdkConfigDataStore.configFlow.first()?.pushRegistrationEnabled == false) return@launch
            if (FirebaseApp.initializeApp(applicationContext) == null) return@launch
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    OctopusSDK.registerNotificationsToken(task.result)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("notificationsPermissionRequested", notificationPermissionPrompt.hasRequested)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationPermissionPrompt.hasRequested = savedInstanceState?.getBoolean("notificationsPermissionRequested") == true
        enableEdgeToEdge()

        // Keep the splash screen until the first screen is known (MainViewModel.State.isStartResolved,
        // bounded by START_RESOLUTION_TIMEOUT_MS) instead of drawing a placeholder frame: the
        // documented way to extend the splash screen without a dependency.
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (!viewModel.state.value.isStartResolved) return false
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            },
        )

        consumeSampleLaunchExtras(intent, savedInstanceState, sdkConfigDataStore, lifecycleScope)
        (application as SampleApplication).startSDK()

        val permissionLauncher =
            registerForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                callback = { granted -> if (granted) registerPushTokenIfEnabled() },
            )

        lifecycleScope.launch {
            viewModel.state
                .map { it.isSdkInitialized }
                .distinctUntilChanged()
                .collect { isInitialized ->
                    if (isInitialized) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                            if (notificationPermissionPrompt.shouldLaunch(Build.VERSION.SDK_INT, granted)) {
                                permissionLauncher.launch(POST_NOTIFICATIONS)
                            } else if (granted) {
                                registerPushTokenIfEnabled()
                            }
                        }
                    }
                }
        }

        // A cold start through `octopus-sample://config[?section=…]`: the NavHost opens that Config
        // entry, then the "SDK ready" effect below leaves Config for Main — the same effect that ends
        // onboarding. The link is kept here and replayed on top of Main so it lands where a warm start
        // does (internal change). Only on a genuine launch, for the same reason as the extras above.
        // `octopus-sample://host-integration` is a settings link of the same kind and is replayed alike.
        val coldStartConfigLink =
            intent?.data?.takeIf {
                savedInstanceState == null &&
                    it.scheme == "octopus-sample" &&
                    (it.host == "config" || it.host == "host-integration")
            }

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val communityNavigation by rememberUpdatedState(
                state.sdkConfig?.communityNavigation ?: SDKConfig.CommunityNavigation.InTab,
            )
            var pendingConfigLink by rememberSaveable { mutableStateOf(coldStartConfigLink?.toString()) }
            // Appearance is a persisted config value, not only the system setting: the tester
            // picks it on Config → Theme or on Settings › Appearance, and it has to reach
            // the theme ABOVE the NavHost, or half the shell would follow the system while the
            // other half followed the choice.
            SampleAppTheme(
                darkTheme =
                    when (state.appearance) {
                        SDKConfig.Appearance.System -> isSystemInDarkTheme()
                        SDKConfig.Appearance.Light -> false
                        SDKConfig.Appearance.Dark -> true
                    },
            ) {
                val navController = rememberContentNavController()
                val sharedAxisX = rememberSharedAxisX()

                var shellHasSdkTopBar by rememberSaveable { mutableStateOf(false) }
                val currentEntry by navController.currentBackStackEntryAsState()
                val destination = currentEntry?.destination
                val isShell = destination?.hasRoute<MainRoute>() == true
                val hasSampleTopBar =
                    destination == null ||
                        (isShell && !shellHasSdkTopBar) ||
                        destination.hasRoute<ConfigRoute>() ||
                        destination.hasRoute<LoginRoute>() ||
                        destination.hasRoute<HostIntegrationRoute>() ||
                        destination.hasRoute<EditUserRoute>() ||
                        destination.hasRoute<SamplePostLinkRoute>()
                val isHostProfile = destination?.hasRoute<ClientProfileRoute>() == true
                val sdkColors = communityColorScheme(state.theme)
                val showServerWarning =
                    LocalExtensions.shouldWarn(state.serverEnvironment)
                val statusBarColor =
                    when {
                        showServerWarning ->
                            MaterialTheme.colorScheme.errorContainer
                        hasSampleTopBar -> sampleTopBarColor
                        isHostProfile -> MaterialTheme.colorScheme.surface
                        else -> sdkColors.background
                    }
                val navigationBarColor =
                    when {
                        isShell -> sampleBottomBarColor
                        destination?.hasRoute<CommunityFullScreenRoute>() == true &&
                            currentEntry?.toRoute<CommunityFullScreenRoute>()?.showHostBottomBar == true ->
                            MaterialTheme.colorScheme.surfaceVariant
                        hasSampleTopBar || isHostProfile -> MaterialTheme.colorScheme.background
                        else -> sdkColors.background
                    }
                // The SDK's image viewer is a window of its own: the host bars are never touched.
                SyncSampleSystemBars(statusBarColor, navigationBarColor, window)
                CompositionLocalProvider(
                    LocalSampleHostStatusBarColor provides statusBarColor,
                ) {
                    val displayedPostId =
                        currentEntry
                            ?.takeIf {
                                it.destination.hasRoute<OctopusDestination.PostDetails>()
                            }?.toRoute<OctopusDestination.PostDetails>()
                            ?.postId

                    // Handle deep links. The root NavHost is not built until the first screen is
                    // known (state.isStartResolved, internal change), and the navController has no graph
                    // before it: a link that arrives during the splash hold is kept and replayed by
                    // the start effect below, never resolved against a missing graph (a crash).
                    var pendingNewIntentLink by rememberSaveable {
                        mutableStateOf(intent?.data?.takeIf { savedInstanceState == null }?.toString())
                    }
                    val incomingLink = newIntentLink
                    LaunchedEffect(incomingLink) {
                        if (incomingLink == null) return@LaunchedEffect
                        newIntentLink = null
                        if (navController.currentBackStackEntry == null) {
                            pendingNewIntentLink = incomingLink.toString()
                        } else {
                            val consumed =
                                navController.navigateToDeepLink(
                                    incomingLink,
                                    state.sdkConfig?.communityNavigation ?: SDKConfig.CommunityNavigation.InTab,
                                    state.isSdkInitialized,
                                )
                            pendingNewIntentLink = if (consumed) null else incomingLink.toString()
                        }
                    }
                    val hasGraph = currentEntry != null

                    // Track whether SDK was ever initialized to detect resets. Keyed on the graph
                    // being set as well and idle until then: after a process death the saved
                    // `wasInitialized = true` meets a SDK still coming up, and navigating to Config
                    // before the NavHost has set its graph throws (internal change).
                    var wasInitialized by rememberSaveable { mutableStateOf(false) }
                    LaunchedEffect(state.isSdkInitialized, hasGraph) {
                        if (!hasGraph) return@LaunchedEffect
                        if (state.isSdkInitialized) {
                            wasInitialized = true
                            // Fresh start / post-reset "Start SDK": initialize() finished asynchronously.
                            // Enter the app only now that the SDK actually reports ready — a tab must
                            // never resolve SDK-backed DI before it exists. Guarded on the Config screen
                            // so an already-running session (a reconfigure that keeps the SDK up) is not
                            // disturbed. A cold-start Host app integration link opened over the onboarding
                            // Config counts as onboarding too: it is replayed over Main just below.
                            navController.finishSdkOnboarding(hasPendingConfigLink = pendingConfigLink != null)
                            // A cold-start Config link is replayed over Main whatever the graph shows now:
                            // when initialization completes late, the NavHost rebuilds on its Main start
                            // destination and has already dropped the Config screen the link opened.
                            // An unmatched link (e.g. a trailing slash) is dropped, as the warm handler does.
                            val coldLink = pendingConfigLink?.toUri()
                            if (coldLink != null && navController.graph.hasDeepLink(coldLink)) {
                                navController.navigate(deepLink = coldLink)
                            }
                            if (pendingNewIntentLink == pendingConfigLink) pendingNewIntentLink = null
                            pendingConfigLink = null
                        } else if (wasInitialized) {
                            // SDK was initialized then reset — navigate back to config
                            wasInitialized = false
                            pendingConfigLink = null
                            navController.navigate(ConfigRoute()) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        // A link held back during the splash, replayed on the screen the start
                        // settled on — after the "SDK ready" pass above, which would otherwise take
                        // a replayed Config link for onboarding and leave it for Main. Kept while a
                        // stored configuration is still bringing the SDK up (a start resolved by
                        // the timeout), for the same reason.
                        val heldLink = pendingNewIntentLink
                        if (heldLink != null && (state.isSdkInitialized || state.sdkConfig == null)) {
                            val consumed =
                                navController.navigateToDeepLink(
                                    heldLink.toUri(),
                                    state.sdkConfig?.communityNavigation ?: SDKConfig.CommunityNavigation.InTab,
                                    state.isSdkInitialized,
                                )
                            if (consumed) pendingNewIntentLink = null
                        }
                    }

                    val context = LocalContext.current

                    // In-app update: one check per foregrounding, plus one when the tester
                    // comes back from Play's update flow. ON_START alone, not
                    // ON_START || ON_RESUME: the two fire back to back on every foregrounding,
                    // so pairing them ran the check twice each time and once more for every
                    // dialog, permission prompt or system sheet that only pauses the Activity.
                    val lifecycleOwner = LocalLifecycleOwner.current
                    val updateLauncher =
                        rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartIntentSenderForResult(),
                        ) {
                            // Re-check explicitly rather than leaning on the lifecycle: whether
                            // Play's flow stops this Activity (and so replays ON_START) is not
                            // ours to guarantee, and a cancelled update must leave the card
                            // showing "available" again.
                            viewModel.checkForAppUpdate()
                        }

                    DisposableEffect(lifecycleOwner) {
                        val observer =
                            LifecycleEventObserver { _, event ->
                                if (event == Lifecycle.Event.ON_START) {
                                    viewModel.checkForAppUpdate()
                                }
                            }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    var showCommunitySheet by rememberSaveable { mutableStateOf(false) }
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    // The full-screen modal shape (catalog scenario `modal`), and the post each
                    // container should land on when opened by its deep-link preset. Held next to the
                    // visibility flag rather than passed into the container: the id has to survive the
                    // recomposition that opens it, and be cleared on dismissal so the next plain open
                    // does not replay the last deep link.
                    var showCommunityModal by rememberSaveable { mutableStateOf(false) }
                    var communitySheetPostId by rememberSaveable { mutableStateOf<String?>(null) }
                    var communityModalPostId by rememberSaveable { mutableStateOf<String?>(null) }

                    val snackbarHostState = remember { SnackbarHostState() }
                    // The last Apply outcome line, so a new Apply replaces only that one and never
                    // dismisses another snackbar (the update-available prompt) sharing the host.
                    var applySnackbarJob by remember { mutableStateOf<Job?>(null) }
                    val coroutineScope = rememberCoroutineScope()
                    // Sample-side handler wired into the SDK via `onGroupAccessDenied` on
                    // `octopusComposables(...)`. Fires when the connected user taps a
                    // visible-but-locked group, its follow button, or the locked-screen CTA on
                    // a locked Post/Comment detail. A real host app would route to its
                    // upsell/paywall here; for the sample we present a sheet that lets the
                    // tester adjust their entitlements and retry — mirrors iOS internal change's
                    // `GroupAccessDeniedScreen`.
                    var groupAccessDeniedId by rememberSaveable { mutableStateOf<String?>(null) }
                    val onGroupAccessDenied: (String) -> Unit = { groupId ->
                        groupAccessDeniedId = groupId
                    }
                    // Sample-side handler for the "View in app" button (ClientPost.viewObjectButtonText)
                    // on a bridge post — wired into the SDK via `onNavigateToClientObject` below. The
                    // SDK fires it with the client object id when the user taps that CTA. A real host
                    // would deep-link to its own content; the sample confirms the event arrived with a
                    // toast (+ the id) so QA can verify the callback fires end-to-end.
                    val onNavigateToClientObject: (String) -> Unit = { objectId ->
                        Toast
                            .makeText(
                                context,
                                "Bridge: open client object \"$objectId\"",
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                    val hostNavigation =
                        communityHostNavigation(
                            callbacks = state.hostCallbacks,
                            navigateHost = { navController.navigate(it) },
                            onClientObject = onNavigateToClientObject,
                            onGroupAccessDenied = onGroupAccessDenied,
                            onUrl = { handleUrl(context, it, state.hostCallbacks.urlHandling) },
                        )
                    val startUpdate: () -> Unit = startUpdate@{
                        val info =
                            (state.appUpdateStatus as? AppUpdateChecker.Status.UpdateAvailable)
                                ?.info ?: return@startUpdate
                        viewModel.appUpdateManager.startUpdateFlowForResult(
                            info,
                            updateLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        )
                    }

                    LaunchedEffect(state.appUpdateStatus) {
                        val newVersionToNotify = viewModel.acknowledgeUpdateNotificationIfNew()
                        if (newVersionToNotify != null) {
                            val result =
                                snackbarHostState.showSnackbar(
                                    message = "A new version of the sample app is available.",
                                    actionLabel = "Update",
                                    withDismissAction = true,
                                )
                            if (result == SnackbarResult.ActionPerformed) startUpdate()
                        }
                    }

                    SampleWindowFrame(
                        statusBarColor = statusBarColor,
                        navigationBarColor = navigationBarColor,
                        // Hoisted here once, at the Activity's own composition root: every screen /
                        // section below (main NavHost destinations, the community and debug-console
                        // modals — each their own composition root) is a descendant of this Column,
                        // and testTagsAsResourceId resolves by walking UP the semantics ancestors. A
                        // modal that renders into its OWN Window (Dialog/Popup/ModalBottomSheet) is
                        // NOT reached by this flag: one declaration per composition root that carries
                        // test ids — see DebugConsole.kt's ModalBottomSheet. The community
                        // ModalBottomSheet below carries none today; add the declaration together
                        // with its first id.
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .semantics { testTagsAsResourceId = true },
                    ) {
                        LocalExtensions.WarningBanner(serverEnvironment = state.serverEnvironment)
                        // Identifies the post destination for QA; backend resolution still requires
                        // its content marker and the absence of an error state in the device dump.
                        Box(
                            modifier =
                                Modifier.fillMaxSize().then(
                                    displayedPostId?.let { Modifier.testTag(it) } ?: Modifier,
                                ),
                        ) {
                            // configFlow recomposes hostNavigation and this graph's builder when wiring
                            // changes. SDK navigation handlers refresh their callbacks on recomposition,
                            // and existing event collectors read the current handler. No app restart or
                            // NavHost key is needed; re-keying would race startDestination during reset.
                            //
                            // Not built before the first screen is known (internal change): a stored
                            // configuration initializes the SDK asynchronously, and a graph built in
                            // the meantime started on Config, then rebuilt on Main once the SDK came
                            // up — a Config flash on every cold start, and a first tap swallowed by
                            // the screen being replaced. The window keeps its splash screen until
                            // then (see the pre-draw hold in onCreate).
                            if (!state.isStartResolved) return@Box
                            NavHost(
                                navController = navController,
                                startDestination = if (state.isSdkInitialized) MainRoute else ConfigRoute(),
                                // Standard motion: Material shared axis X for a push, the
                                // predictive-back pop for a pop (see SharedAxisX). The SDK screens
                                // below inherit both from this NavHost (internal change).
                                enterTransition = { sharedAxisX.enter },
                                exitTransition = { sharedAxisX.exit },
                                popEnterTransition = { sharedAxisX.popEnter },
                                popExitTransition = { sharedAxisX.popExit },
                            ) {
                                composable<ConfigRoute>(
                                    // QA + power-user shortcut: `adb shell am start -a android.intent.action.VIEW
                                    // -d "octopus-sample://config"` (or Settings → Configuration) reaches
                                    // this screen without going through Reset Configuration first;
                                    // `?section=SERVER` opens it on one section.
                                    deepLinks = listOf(navDeepLink { uriPattern = ConfigRoute.DEEP_LINK_PATTERN }),
                                ) { backStackEntry ->
                                    val section = backStackEntry.toRoute<ConfigRoute>().section
                                    if (section == ConfigSection.INTEGRATION && state.isSdkInitialized) {
                                        // The host wiring left Config for its own screen. An older link
                                        // aimed at it is forwarded there, replacing this entry so Back
                                        // does not bounce through Config into the same redirect.
                                        LaunchedEffect(Unit) {
                                            navController.navigate(HostIntegrationRoute) {
                                                popUpTo<ConfigRoute> { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                        return@composable
                                    }
                                    ConfigScreen(
                                        section = section,
                                        onOpenHostIntegration = { navController.navigate(HostIntegrationRoute) },
                                        onOpenUnifiedProfile = {
                                            navController.navigate(MainRoute) {
                                                popUpTo<ConfigRoute> { inclusive = true }
                                                launchSingleTop = true
                                            }
                                            navController.currentBackStackEntry?.savedStateHandle?.set(UNIFIED_PROFILE_SCENARIO, true)
                                        },
                                        // The SDK being up is exactly what separates onboarding from a
                                        // revisit through Settings → Configuration, and it is what
                                        // the button label reads: "Start SDK" then, "Apply" now.
                                        isRevisit = state.isSdkInitialized,
                                        // A reconfigure that keeps the SDK initialized (switchCommunity)
                                        // can return to the app immediately — DI is already up. A fresh
                                        // start / post-reset initialize() runs async, so that navigation
                                        // is deferred to the isSdkInitialized effect above (entering
                                        // MainRoute before the SDK is ready would let a tab resolve
                                        // SDK-backed DI and crash).
                                        onStarted = { outcome ->
                                            // Started from the linked Config itself: nothing left to replay.
                                            pendingConfigLink = null
                                            if (state.isSdkInitialized) {
                                                navController.navigate(MainRoute) {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            }
                                            // Apply used to leave the screen with no word on what
                                            // it did. The host sits at the Activity root, so the
                                            // line outlives the navigation that follows the tap.
                                            // Cancelling the previous Apply's call dismisses (or
                                            // unqueues) that line only.
                                            applySnackbarJob?.cancel()
                                            applySnackbarJob =
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = outcome,
                                                        withDismissAction = true,
                                                    )
                                                }
                                        },
                                    )
                                }

                                composable<MainRoute> { mainEntry ->
                                    val pendingPostId by mainEntry.savedStateHandle
                                        .getStateFlow<String?>(
                                            COMMUNITY_POST_ID,
                                            null,
                                        ).collectAsStateWithLifecycle()
                                    val pendingDeepLink by mainEntry.savedStateHandle
                                        .getStateFlow<String?>(
                                            COMMUNITY_DEEP_LINK,
                                            null,
                                        ).collectAsStateWithLifecycle()
                                    val openUnifiedProfile by mainEntry.savedStateHandle
                                        .getStateFlow(UNIFIED_PROFILE_SCENARIO, false)
                                        .collectAsStateWithLifecycle()
                                    MainScaffold(
                                        openUnifiedProfile = openUnifiedProfile,
                                        onUnifiedProfileOpened = { mainEntry.savedStateHandle[UNIFIED_PROFILE_SCENARIO] = false },
                                        rootNavController = navController,
                                        connectionState = state.connectionState,
                                        isSdkInitialized = state.isSdkInitialized,
                                        sdkEvents = state.sdkEvents,
                                        debugLog = state.debugLog,
                                        onClearEvents = viewModel::clearEvents,
                                        onClearDebugLog = viewModel::clearDebugLog,
                                        onLogDebug = viewModel::addDebugEntry,
                                        theme = state.theme,
                                        hostNavigation = hostNavigation,
                                        communityNavigation = communityNavigation,
                                        pendingPostId = pendingPostId,
                                        pendingDeepLink = pendingDeepLink,
                                        onContentLinkConsumed = {
                                            mainEntry.savedStateHandle[COMMUNITY_POST_ID] = null
                                            mainEntry.savedStateHandle[COMMUNITY_DEEP_LINK] = null
                                        },
                                        onSdkDestinationChanged = { shellHasSdkTopBar = it },
                                        appUpdateStatus = state.appUpdateStatus,
                                        onCheckForUpdate = viewModel::checkForAppUpdate,
                                        onStartUpdate = startUpdate,
                                        onOpenCommunityInSheet = { postId ->
                                            communitySheetPostId = postId
                                            showCommunitySheet = true
                                        },
                                        onOpenCommunityInFullScreenModal = { postId ->
                                            communityModalPostId = postId
                                            showCommunityModal = true
                                        },
                                        serverEnvironment = state.serverEnvironment,
                                    )
                                }

                                composable<SamplePostLinkRoute>(
                                    deepLinks = listOf(navDeepLink { uriPattern = SampleFixtures.postLinkPattern }),
                                ) { entry ->
                                    val route = entry.toRoute<SamplePostLinkRoute>()
                                    LaunchedEffect(state.isSdkInitialized) {
                                        if (state.isSdkInitialized) {
                                            navController.openSamplePostLink(
                                                route,
                                                state.sdkConfig?.communityNavigation ?: SDKConfig.CommunityNavigation.InTab,
                                            )
                                        }
                                    }
                                    if (!state.isSdkInitialized) {
                                        ConfigScreen(isRevisit = false, onStarted = {})
                                    }
                                }

                                composable<CommunityFullScreenRoute> { backStackEntry ->
                                    val route = backStackEntry.toRoute<CommunityFullScreenRoute>()
                                    CommunityFullScreen(
                                        navController = navController,
                                        onBack = { navController.navigateUp() },
                                        theme = state.theme,
                                        hostCallbacks = state.hostCallbacks,
                                        deepLinkPostId = route.postId,
                                        showHostBottomBar = route.showHostBottomBar,
                                    )
                                }

                                composable<LoginRoute> {
                                    LoginScreen(onBack = { navController.navigateUp() })
                                }

                                composable<EditUserRoute> {
                                    EditUserScreen(onBack = { navController.navigateUp() })
                                }

                                composable<HostIntegrationRoute>(
                                    deepLinks = listOf(navDeepLink { uriPattern = HostIntegrationRoute.DEEP_LINK }),
                                ) {
                                    HostIntegrationScreen(onBack = { navController.navigateUp() })
                                }

                                // Unified Profile (pillar 1) demo: the host's own profile screen,
                                // pushed when `onNavigateToProfile` returns HandledByApp below.
                                composable<ClientProfileRoute> { backStackEntry ->
                                    val clientUserId =
                                        backStackEntry.toRoute<ClientProfileRoute>().clientUserId
                                    ClientProfileScreen(
                                        clientUserId = clientUserId,
                                        onBack = { navController.navigateUp() },
                                        // Jump into the member's Octopus posts, keyed on the host's own
                                        // client id (resolved by the SDK).
                                        onSeePosts = {
                                            navController.navigateToOctopusActivityByClientUserId(
                                                clientUserId,
                                            )
                                        },
                                    )
                                }

                                // Each callback comes from effectiveHostCallbacks. Config changes
                                // recompose this builder, so the Unified Profile scenario and Host app
                                // integration update the SDK handlers immediately, including null wiring.
                                octopusComposables(
                                    navController = navController,
                                    onNavigateToLogin = hostNavigation.login,
                                    onNavigateToProfileEdit = hostNavigation.profileEdit,
                                    onNavigateToProfile = hostNavigation.profile,
                                    onNavigateToClientObject = hostNavigation.clientObject,
                                    onGroupAccessDenied = hostNavigation.groupAccessDenied,
                                    onNavigateToUrl = hostNavigation.url,
                                    container = { entry: NavBackStackEntry, content ->
                                        val link = unconsumedCommunityDeepLink(entry)
                                        val relayToTab =
                                            shouldRelayCommunityDeepLink(communityNavigation, link?.toString(), navController.graph)
                                        LaunchedEffect(state.isSdkInitialized, relayToTab) {
                                            if (!state.isSdkInitialized) return@LaunchedEffect
                                            // Once shown in full screen or handed to the tab, Back must not replay it.
                                            entry.savedStateHandle[COMMUNITY_LINK_CONSUMED] = true
                                            if (relayToTab) navController.openCommunityDeepLink(link.toString())
                                        }
                                        if (state.isSdkInitialized && !relayToTab) {
                                            CommunityTheme(theme = state.theme) { content() }
                                        } else if (!state.isSdkInitialized) {
                                            ConfigScreen(isRevisit = false, onStarted = {})
                                        }
                                    },
                                )
                            }
                            // DS screen 09, state D (transient): the DS Snackbar, Undo-style action.
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            ) { data -> SampleSnackbar(data) }
                        }
                    }

                    // The two host-owned containers the catalog's `sheet` and `modal` presentation
                    // scenarios are about. Both embed the SAME community surface through
                    // [EmbeddedCommunity] below; what differs is only the container the host wraps it
                    // in, which is exactly what those scenarios test. Hoisted here, at the Activity
                    // root, rather than inside the scenario screens: a container opened from a tab
                    // destination would be torn down with that destination.
                    if (showCommunitySheet) {
                        SampleModalBottomSheet(
                            containerColor = sdkColors.background,
                            sheetState = sheetState,
                            onDismissRequest = {
                                showCommunitySheet = false
                                communitySheetPostId = null
                            },
                        ) {
                            EmbeddedCommunity(
                                rootNavController = navController,
                                hostCallbacks = state.sdkConfig?.effectiveHostCallbacks,
                                theme = state.theme,
                                serverEnvironment = state.serverEnvironment,
                                deepLinkPostId = communitySheetPostId,
                                onGroupAccessDenied = onGroupAccessDenied,
                                onNavigateToClientObject = onNavigateToClientObject,
                                contentModifier = Modifier.fillMaxSize(),
                                onClose = null,
                            )
                        }
                    }

                    if (showCommunityModal) {
                        // Compose's equivalent of Flutter's `fullscreenDialog` route and iOS's
                        // `.fullScreenCover`: a Dialog allowed to fill the window.
                        Dialog(
                            onDismissRequest = {
                                showCommunityModal = false
                                communityModalPostId = null
                            },
                            properties =
                                DialogProperties(
                                    usePlatformDefaultWidth = false,
                                    decorFitsSystemWindows = false,
                                ),
                        ) {
                            // Edge to edge under the SDK header (DS card Host modal, v19): with
                            // no host bar left, the status-bar strip is the SDK background the
                            // header is painted on, not the host surface.
                            SyncSampleSystemBars(sdkColors.background, sdkColors.background)
                            Surface(modifier = Modifier.fillMaxSize(), color = sdkColors.background) {
                                SampleWindowFrame(
                                    statusBarColor = sdkColors.background,
                                    navigationBarColor = sdkColors.background,
                                    // A Dialog renders into its OWN Window / SemanticsOwner, which the
                                    // Activity-root declaration does not reach — the flag is
                                    // re-declared here so the ids inside surface as resource-ids to
                                    // uiautomator (same reason DebugConsole.kt's sheet carries one).
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .semantics { testTagsAsResourceId = true },
                                ) {
                                    // One header, the SDK's: the catalog phrases this scenario's
                                    // dismissal as "the SDK's own close action" (the iOS / Flutter
                                    // shape), and `OctopusHomeScreen(leadingNavigationIcon = Close)` is
                                    // the public way to get it. A host TopAppBar on top of it stacked
                                    // two headers, with the close X on neither the SDK's nor a
                                    // recognisable host bar. The X is the leading control.
                                    EmbeddedCommunity(
                                        rootNavController = navController,
                                        hostCallbacks = state.sdkConfig?.effectiveHostCallbacks,
                                        theme = state.theme,
                                        serverEnvironment = state.serverEnvironment,
                                        deepLinkPostId = communityModalPostId,
                                        onGroupAccessDenied = onGroupAccessDenied,
                                        onNavigateToClientObject = onNavigateToClientObject,
                                        contentModifier = Modifier.weight(1f),
                                        onClose = {
                                            showCommunityModal = false
                                            communityModalPostId = null
                                        },
                                    )
                                }
                            }
                        }
                    }

                    groupAccessDeniedId?.let { id ->
                        GroupAccessDeniedSheet(
                            groupId = id,
                            onDismiss = { groupAccessDeniedId = null },
                            onNavigateToEditProfile = {
                                groupAccessDeniedId = null
                                navController.navigate(EditUserRoute)
                            },
                            serverEnvironment = state.serverEnvironment,
                        )
                    }
                }
            }
        }
    }

    /**
     * The activity is `singleTop`, so the `adb shell am start` command documented on
     * the launch extension reaches an already-running Sample here rather than through `onCreate` —
     * which is the common case during a QA run, where the app is typically already up.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { newIntentLink = it }
        consumeSampleLaunchExtras(intent, null, sdkConfigDataStore, lifecycleScope)
    }
}

/**
 * The embedded community surface, as a host puts it inside a container it owns — the shared body of
 * the bottom sheet (`sheet` scenario) and the full-screen modal (`modal` scenario).
 *
 * It gets its OWN `NavHostController` on purpose: the community presented in a host container has
 * to navigate INSIDE that container (tap a post, come back) without touching the Activity's back
 * stack, which is also what makes "does the SDK's sub-navigation stay in the container" a question
 * a tester can answer. The host callbacks that leave the community for a host screen
 * (login, profile edit) navigate the ROOT controller instead — those genuinely exit the container.
 *
 * Extracted from the sheet, which already carried this exact wiring inline: the modal needs the
 * same thing, and two copies of a 60-line callback block are two chances to wire one of them
 * differently and call the difference a finding.
 */
@Composable
private fun EmbeddedCommunity(
    rootNavController: NavHostController,
    hostCallbacks: SDKConfig.HostCallbacks?,
    theme: SDKConfig.Theme,
    serverEnvironment: SDKConfig.ServerEnvironment,
    deepLinkPostId: String?,
    onGroupAccessDenied: (String) -> Unit,
    onNavigateToClientObject: (String) -> Unit,
    // Deliberately NOT named `modifier`, and deliberately without a default. This composable emits
    // two siblings — the production banner and the community NavHost — so there is no single
    // element a `modifier` could decorate, and a parameter named `modifier` that lands on the
    // second one silently breaks the Compose contract the next caller will assume. The name says
    // which of the two it reaches, and the required argument makes each container state its own
    // sizing (`fillMaxSize()` in the sheet, `weight(1f)` under the modal's banner).
    contentModifier: Modifier,
    // Non-null in the full-screen modal: the community root then draws the SDK's own header with a
    // close X as its leading control, the container's only way out. Null in the sheet, which the
    // host dismisses by drag or scrim and which keeps the header-less `OctopusHomeContent`.
    onClose: (() -> Unit)?,
) {
    val context = LocalContext.current
    val embeddedNavController = rememberContentNavController()

    // The deep-link presets (`qa-preset-sheet-2` / `qa-preset-modal-2`): land on the post INSIDE
    // this container rather than opening a second instance of the community somewhere else. Fired
    // once per container instance — see DeepLinkToPostOnce for why a plain keyed LaunchedEffect
    // traps the Back button and re-fires on rotation.
    DeepLinkToPostOnce(postId = deepLinkPostId, navController = embeddedNavController)

    LocalExtensions.WarningBanner(serverEnvironment = serverEnvironment)
    val sharedAxisX = rememberSharedAxisX()
    CommunityTheme(theme = theme) {
        NavHost(
            navController = embeddedNavController,
            startDestination = ModalCommunityHomeRoute,
            modifier = contentModifier,
            // The same standard push/pop as the root NavHost; the SDK screens inherit it (internal change).
            enterTransition = { sharedAxisX.enter },
            exitTransition = { sharedAxisX.exit },
            popEnterTransition = { sharedAxisX.popEnter },
            popExitTransition = { sharedAxisX.popExit },
        ) {
            composable<ModalCommunityHomeRoute> {
                val onLogin: (() -> Unit)? =
                    if (hostCallbacks?.loginWired != false) {
                        { rootNavController.navigateToLogin() }
                    } else {
                        null
                    }
                val onProfileEdit: ((ProfileField?) -> Unit)? =
                    if (hostCallbacks?.profileEditWired != false) {
                        { rootNavController.navigateToProfileEdit() }
                    } else {
                        null
                    }
                val onClientObject: ((String) -> Unit)? =
                    if (hostCallbacks?.clientObjectWired != false) {
                        onNavigateToClientObject
                    } else {
                        null
                    }
                val onUrl: (String) -> UrlOpeningStrategy = { url ->
                    handleUrl(context, url, hostCallbacks?.urlHandling ?: SDKConfig.UrlHandling.Default)
                }
                if (onClose != null) {
                    OctopusHomeScreen(
                        navController = embeddedNavController,
                        leadingNavigationIcon = NavigationIconType.Close,
                        onBack = onClose,
                        onNavigateToLogin = onLogin,
                        onNavigateToProfileEdit = onProfileEdit,
                        onNavigateToClientObject = onClientObject,
                        onNavigateToUrl = onUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    OctopusHomeContent(
                        navController = embeddedNavController,
                        onNavigateToLogin = onLogin,
                        onNavigateToProfileEdit = onProfileEdit,
                        onNavigateToClientObject = onClientObject,
                        onNavigateToUrl = onUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            octopusComposables(
                navController = embeddedNavController,
                onNavigateToLogin =
                    if (hostCallbacks?.loginWired != false) {
                        { rootNavController.navigateToLogin() }
                    } else {
                        null
                    },
                onNavigateToProfileEdit =
                    if (hostCallbacks?.profileEditWired != false) {
                        { rootNavController.navigateToProfileEdit() }
                    } else {
                        null
                    },
                onGroupAccessDenied =
                    if (hostCallbacks?.groupAccessDeniedWired != false) {
                        onGroupAccessDenied
                    } else {
                        null
                    },
                onNavigateToClientObject =
                    if (hostCallbacks?.clientObjectWired != false) {
                        onNavigateToClientObject
                    } else {
                        null
                    },
                onNavigateToUrl = { url ->
                    handleUrl(context, url, hostCallbacks?.urlHandling ?: SDKConfig.UrlHandling.Default)
                },
                container = { _: NavBackStackEntry, content -> content() },
            )
        }
    }
}
