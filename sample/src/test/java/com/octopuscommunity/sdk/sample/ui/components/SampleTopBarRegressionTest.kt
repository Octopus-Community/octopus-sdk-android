package com.octopuscommunity.sdk.sample.ui.components

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.sample.BuildConfig
import com.octopuscommunity.sdk.sample.data.CommunitySessionTracker
import com.octopuscommunity.sdk.sample.data.datastore.SDKConfigData
import com.octopuscommunity.sdk.sample.data.datastore.SDKConfigDataStore
import com.octopuscommunity.sdk.sample.data.datastore.UserData
import com.octopuscommunity.sdk.sample.data.datastore.UserDataStore
import com.octopuscommunity.sdk.sample.data.model.DebugEntry
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.data.usecase.CommunityConnectUseCase
import com.octopuscommunity.sdk.sample.ui.config.ConfigScreen
import com.octopuscommunity.sdk.sample.ui.config.ConfigSection
import com.octopuscommunity.sdk.sample.ui.config.ConfigViewModel
import com.octopuscommunity.sdk.sample.ui.navigation.AboutRoute
import com.octopuscommunity.sdk.sample.ui.navigation.AccountRoute
import com.octopuscommunity.sdk.sample.ui.navigation.EventsLogRoute
import com.octopuscommunity.sdk.sample.ui.navigation.SampleTabPage
import com.octopuscommunity.sdk.sample.ui.navigation.ScenarioDetailRoute
import com.octopuscommunity.sdk.sample.ui.navigation.ScenariosTab
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenarioDetailScreen
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenariosListScreen
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenariosViewModel
import com.octopuscommunity.sdk.sample.ui.settings.AboutScreen
import com.octopuscommunity.sdk.sample.ui.settings.AccountScreen
import com.octopuscommunity.sdk.sample.ui.settings.AccountViewModel
import com.octopuscommunity.sdk.sample.ui.settings.EditUserScreen
import com.octopuscommunity.sdk.sample.ui.settings.EditUserViewModel
import com.octopuscommunity.sdk.sample.ui.settings.EventsScreen
import com.octopuscommunity.sdk.sample.ui.settings.HostIntegrationScreen
import com.octopuscommunity.sdk.sample.ui.settings.HostIntegrationViewModel
import com.octopuscommunity.sdk.sample.ui.settings.LoginScreen
import com.octopuscommunity.sdk.sample.ui.settings.LoginViewModel
import com.octopuscommunity.sdk.sample.ui.theme.SampleAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Real screens and shell: reduced height models the space left above an open keyboard. */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SampleTopBarRegressionTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()
    private val viewModels = ViewModelStore()
    private val host =
        object : ExternalResource() {
            override fun before() {
                val application = RuntimeEnvironment.getApplication()
                shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
            }

            override fun after() {
                viewModels.clear()
            }
        }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(host).around(compose)

    @Test
    fun `edit profile brings fields below the bar and above the keyboard after scrolling`() {
        compose.setContent {
            SampleAppTheme {
                val context = RuntimeEnvironment.getApplication()
                val viewModel =
                    remember {
                        val users = UserDataStore(context, MemoryStore(UserData()))
                        EditUserViewModel(
                            users,
                            CommunityConnectUseCase(users, isSigningAvailable = { true }),
                            flowOf(null),
                        ).also { viewModels.put("edit", it) }
                    }
                Box(Modifier.fillMaxWidth().height(280.dp)) { EditUserScreen({}, viewModel) }
            }
        }
        compose.onNodeWithText("Save").performScrollTo()
        assertFocusedFieldVisible("Nickname")
        assertFocusedFieldVisible("Bio")
        assertFocusedFieldVisible("Nickname")
    }

    @Test
    fun `login brings fields below the bar and above the keyboard after scrolling`() {
        compose.setContent {
            SampleAppTheme {
                val context = RuntimeEnvironment.getApplication()
                val viewModel =
                    remember {
                        val users = UserDataStore(context, MemoryStore(UserData()))
                        LoginViewModel(
                            users,
                            CommunityConnectUseCase(users, isSigningAvailable = {
                                true
                            }),
                            SDKConfigDataStore(context, MemoryStore(SDKConfigData())),
                        ).also { viewModels.put("login", it) }
                    }
                Box(Modifier.fillMaxWidth().height(280.dp)) { LoginScreen({}, viewModel) }
            }
        }
        compose.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        assertFocusedFieldVisible("User ID *")
        assertFocusedFieldVisible("Nickname")
        assertFocusedFieldVisible("Bio")
        assertFocusedFieldVisible("User ID *")
    }

    @Test
    fun `account brings refocused fields below the shell bar after scrolling`() {
        compose.setContent {
            SampleAppTheme {
                val context = RuntimeEnvironment.getApplication()
                val viewModel =
                    remember {
                        val users = UserDataStore(context, MemoryStore(UserData()))
                        AccountViewModel(
                            users,
                            CommunityConnectUseCase(users, isSigningAvailable = { true }),
                        ).also { viewModels.put("account", it) }
                    }
                val nav = rememberNavController()
                Box(Modifier.fillMaxWidth().height(280.dp)) {
                    NavHost(nav, startDestination = AccountRoute) {
                        composable<AccountRoute> { entry -> SampleTabPage(entry, {}, {}) { AccountScreen({}, viewModel) } }
                    }
                }
            }
        }
        repeat(3) { compose.onNode(hasScrollAction()).performTouchInput { swipeUp() } }
        assertFocusedFieldVisible("User ID")
        assertFocusedFieldVisible("Custom entitlement")
        assertFocusedFieldVisible("Nickname")
        assertFocusedFieldVisible("User ID")
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `account reveals retained focus when tapped partly behind the bar`() = assertAccountTapRevealsField(retainFocus = true)

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `account reveals new focus when tapped partly behind the bar`() = assertAccountTapRevealsField(retainFocus = false)

    private fun assertAccountTapRevealsField(retainFocus: Boolean) {
        compose.setContent {
            SampleAppTheme(darkTheme = true) {
                val context = RuntimeEnvironment.getApplication()
                val viewModel =
                    remember {
                        val users = UserDataStore(context, MemoryStore(UserData()))
                        AccountViewModel(
                            users,
                            CommunityConnectUseCase(users, isSigningAvailable = { true }),
                        ).also { viewModels.put("account", it) }
                    }
                val nav = rememberNavController()
                Box(Modifier.fillMaxWidth().height(280.dp)) {
                    NavHost(nav, startDestination = AccountRoute) {
                        composable<AccountRoute> { entry -> SampleTabPage(entry, {}, {}) { AccountScreen({}, viewModel) } }
                    }
                }
            }
        }
        val field = compose.onNodeWithTag("account-nickname-field")
        // Dismissing the IME leaves the field focused; scrolling can then hide it under the bar.
        if (retainFocus) field.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.waitForIdle()
        val barBottom = with(compose.density) { TopAppBarDefaults.TopAppBarExpandedHeight.toPx() }
        val initial = field.fetchSemanticsNode()
        compose.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, initial.positionInRoot.y - barBottom / 2f)
        }
        compose.waitForIdle()
        val partlyHidden = field.fetchSemanticsNode()
        val before = Rect(partlyHidden.positionInRoot, partlyHidden.size.toSize())
        assertTrue(before.top > 0f && before.top < barBottom && before.bottom > barBottom, "Partly under bar: $before")
        // Tap only the exposed part, as on device; no performScrollTo or explicit bring-into-view.
        field.performTouchInput { click(Offset(center.x, height - 8f)) }
        field.assertIsFocused()
        assertFullyVisible(field, "Nickname")
        assertFullyVisible(compose.onNodeWithText("Nickname", useUnmergedTree = true), "Nickname floating label")
    }

    @Test
    fun `config brings refocused fields below its own bar after scrolling`() {
        compose.setContent {
            SampleAppTheme {
                val context = RuntimeEnvironment.getApplication()
                val scope = rememberCoroutineScope()
                val viewModel =
                    remember {
                        val users = UserDataStore(context, MemoryStore(UserData()))
                        ConfigViewModel(
                            SDKConfigDataStore(context, MemoryStore(SDKConfigData())),
                            users,
                            CommunityConnectUseCase(users, isSigningAvailable = { true }),
                            scope,
                        ).also {
                            it.setUseCustomCommunity(true)
                            viewModels.put("config", it)
                        }
                    }
                Box(Modifier.fillMaxWidth().height(280.dp)) { ConfigScreen(section = ConfigSection.COMMUNITY, viewModel = viewModel) }
            }
        }
        repeat(3) { compose.onNode(hasScrollAction()).performTouchInput { swipeUp() } }
        assertFocusedFieldVisible("API Key")
        if (!BuildConfig.IS_PROD_FLAVOR) {
            compose.onNodeWithText("Server environment").performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.onNodeWithText("Custom host").performSemanticsAction(SemanticsActions.OnClick) { it() }
            repeat(3) { compose.onNode(hasScrollAction()).performTouchInput { swipeUp() } }
            assertFocusedFieldVisible("Host")
            assertFocusedFieldVisible("Port")
            assertFocusedFieldVisible("Host")
        }
    }

    @Test
    fun `scenario detail brings refocused fields below the shell bar after scrolling`() {
        compose.setContent {
            SampleAppTheme {
                val context = RuntimeEnvironment.getApplication()
                val viewModel =
                    remember {
                        val users = UserDataStore(context, MemoryStore(UserData()))
                        ScenariosViewModel(
                            users,
                            CommunityConnectUseCase(users, isSigningAvailable = { true }),
                            SDKConfigDataStore(context, MemoryStore(SDKConfigData())),
                            CommunitySessionTracker(),
                        ).also { viewModels.put("scenario", it) }
                    }
                val nav = rememberNavController()
                Box(Modifier.fillMaxWidth().height(280.dp)) {
                    NavHost(nav, startDestination = ScenarioDetailRoute("pushNotifications")) {
                        composable<ScenarioDetailRoute> { entry ->
                            SampleTabPage(entry, {}, {}) {
                                ScenarioDetailScreen(
                                    scenarioId = "pushNotifications",
                                    viewModel = viewModel,
                                    onOpenClientProfile = {},
                                    onOpenPost = {},
                                    onOpenComment = {},
                                    onOpenBridgeTopic = {},
                                    onOpenHome = {},
                                    onOpenUserActivity = {},
                                    onOpenUserActivityByClientUserId = {},
                                    onOpenMyProfile = {},
                                    onOpenProfileByUserId = {},
                                    onOpenProfileByClientUserId = {},
                                    sdkEvents = emptyList(),
                                    onClearEvents = {},
                                    theme = SDKConfig.Theme.Default,
                                    onSetTheme = {},
                                    onVerifyInCommunity = {},
                                )
                            }
                        }
                    }
                }
            }
        }
        repeat(3) { compose.onNode(hasScrollAction()).performTouchInput { swipeUp() } }
        assertFocusedFieldVisible("Post ID")
    }

    @Test
    fun `host integration brings requested content below its own bar after scrolling`() {
        compose.setContent {
            SampleAppTheme {
                val context = RuntimeEnvironment.getApplication()
                val viewModel =
                    remember {
                        val config = SDKConfig(authKind = SDKConfig.AuthKind.SSO())
                        HostIntegrationViewModel(SDKConfigDataStore(context, MemoryStore(SDKConfigData(hasData = true, config = config))))
                            .also { viewModels.put("host", it) }
                    }
                Box(Modifier.fillMaxWidth().height(280.dp)) { HostIntegrationScreen({}, viewModel) }
            }
        }
        compose.onNodeWithText("Restart now").performScrollTo()
        val navigation = compose.onNodeWithTag("community-navigation-select")
        navigation.performScrollTo()
        val callback = compose.onNodeWithTag("host-integration-profile-edit-switch")
        callback.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        assertFullyVisible(callback, "Profile edit callback")
    }

    private fun assertFocusedFieldVisible(label: String) {
        val field = compose.onNode(hasSetTextAction() and hasText(label))
        field.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        assertFullyVisible(field, label)
    }

    private fun assertFullyVisible(
        target: SemanticsNodeInteraction,
        label: String,
    ) {
        compose.waitForIdle()
        // boundsInRoot is clipped by the scroll viewport and can hide an obscured label.
        val node = target.fetchSemanticsNode()
        val coordinates = node.layoutInfo.coordinates
        val bounds = coordinates.findRootCoordinates().localBoundingBoxOf(coordinates, clipBounds = false)
        val barBottom = with(compose.density) { TopAppBarDefaults.TopAppBarExpandedHeight.toPx() }
        val keyboardTop = with(compose.density) { 280.dp.toPx() }
        assertTrue(bounds.top >= barBottom, "$label behind bar: field=$bounds, barBottom=$barBottom")
        assertTrue(bounds.bottom <= keyboardTop, "$label below keyboard: field=$bounds, keyboardTop=$keyboardTop")
    }

    @Test
    fun `explicit requests bring content below the bar after scrolling past it`() {
        val requester = BringIntoViewRequester()
        lateinit var scroll: ScrollState
        lateinit var scope: CoroutineScope
        compose.setContent {
            SampleAppTheme {
                scope = rememberCoroutineScope()
                scroll = rememberScrollState()
                // A scenario result card or a Config anchor: requested from code, with no focus involved.
                Box(Modifier.fillMaxWidth().height(280.dp)) {
                    SampleTopBarContent(TopAppBarDefaults.TopAppBarExpandedHeight) {
                        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(top = LocalSampleTopBarInset.current)) {
                            Spacer(Modifier.height(300.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .bringIntoViewRequester(requester)
                                    .testTag("requested"),
                            )
                            Spacer(Modifier.height(1000.dp))
                        }
                    }
                }
            }
        }
        compose.runOnIdle { scope.launch { scroll.scrollTo(scroll.maxValue) } }
        compose.runOnIdle { scope.launch { requester.bringIntoView() } }
        assertFullyVisible(compose.onNodeWithTag("requested"), "Requested content")
    }

    @Test
    fun `config anchor lands below its own bar`() {
        compose.setContent {
            SampleAppTheme {
                val context = RuntimeEnvironment.getApplication()
                val scope = rememberCoroutineScope()
                val viewModel =
                    remember {
                        val users = UserDataStore(context, MemoryStore(UserData()))
                        ConfigViewModel(
                            SDKConfigDataStore(context, MemoryStore(SDKConfigData())),
                            users,
                            CommunityConnectUseCase(users, isSigningAvailable = { true }),
                            scope,
                        ).also { viewModels.put("config", it) }
                    }
                // Shorter than the expanded section, so the request aligns its top with the viewport's.
                Box(Modifier.fillMaxWidth().height(200.dp)) { ConfigScreen(section = ConfigSection.THEME, viewModel = viewModel) }
            }
        }
        compose.waitForIdle()
        val section = compose.onNodeWithTag("config-section-${ConfigSection.THEME.tag}").fetchSemanticsNode()
        val coordinates = section.layoutInfo.coordinates
        val top = coordinates.findRootCoordinates().localBoundingBoxOf(coordinates, clipBounds = false).top
        val barBottom = with(compose.density) { TopAppBarDefaults.TopAppBarExpandedHeight.toPx() }
        assertTrue(top >= barBottom, "Anchored section behind bar: top=$top, barBottom=$barBottom")
    }

    @Test
    fun `config shows a back arrow only when it has somewhere to go back to`() {
        var onBack by mutableStateOf<(() -> Unit)?>(null)
        var backs = 0
        compose.setContent {
            SampleAppTheme {
                val context = RuntimeEnvironment.getApplication()
                val scope = rememberCoroutineScope()
                val viewModel =
                    remember {
                        val users = UserDataStore(context, MemoryStore(UserData()))
                        ConfigViewModel(
                            SDKConfigDataStore(context, MemoryStore(SDKConfigData())),
                            users,
                            CommunityConnectUseCase(users, isSigningAvailable = { true }),
                            scope,
                        ).also { viewModels.put("config", it) }
                    }
                ConfigScreen(onBack = onBack, viewModel = viewModel)
            }
        }
        // Cold start: Config is the first screen, nothing to pop.
        compose.onNodeWithContentDescription("Back").assertDoesNotExist()

        onBack = { backs++ }
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
    }

    @Test
    fun `tab roots collapse a large title and only content that passes behind the bar changes its appearance`() {
        lateinit var nav: NavHostController
        lateinit var aboutState: TopAppBarState
        lateinit var eventsState: TopAppBarState
        lateinit var scenariosState: TopAppBarState
        val rows = List(80) { DebugEntry(it.toLong(), 0L, DebugEntry.Category.EVENT, "Event $it") }
        compose.setContent {
            SampleAppTheme {
                nav = rememberNavController()
                aboutState = rememberTopAppBarState()
                eventsState = rememberTopAppBarState()
                scenariosState = rememberTopAppBarState()
                Box(Modifier.height(500.dp)) {
                    NavHost(nav, startDestination = AboutRoute) {
                        composable<AboutRoute> { entry ->
                            SampleTabPage(entry, {}, {}, barState = aboutState) { AboutScreen() }
                        }
                        composable<EventsLogRoute> { entry ->
                            SampleTabPage(entry, {}, {}, barState = eventsState) { EventsScreen(rows, {}) }
                        }
                        composable<ScenariosTab> { entry ->
                            SampleTabPage(
                                entry,
                                {},
                                {},
                                barState = scenariosState,
                            ) { ScenariosListScreen(apiKey = "", onScenarioClick = {}) }
                        }
                    }
                }
            }
        }
        compose.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        compose.runOnIdle {
            assertTrue(aboutState.overlappedFraction > 0.01f, "About content passes behind bar")
            assertEquals(0f, aboutState.heightOffset, "A pushed screen keeps its small bar")
        }
        compose.runOnIdle { nav.navigate(EventsLogRoute) }
        repeat(3) { compose.onNode(hasScrollAction()).performTouchInput { swipeUp() } }
        assertListBelow(TopAppBarDefaults.TopAppBarExpandedHeight, "Events")
        compose.runOnIdle {
            assertEquals(0f, eventsState.overlappedFraction, "Events cannot overlap the bar")
            assertEquals(0f, eventsState.contentOffset, "Fixed-header scrolling must not accumulate bar offset")
        }
        compose.runOnIdle { nav.navigate(ScenariosTab()) }
        assertListBelow(TopAppBarDefaults.LargeAppBarExpandedHeight, "Expanded Scenarios")
        repeat(3) { compose.onNode(hasScrollAction()).performTouchInput { swipeUp() } }
        compose.runOnIdle { assertTrue(scenariosState.collapsedFraction > 0.99f, "Scrolling a tab root collapses its large title") }
        assertListBelow(TopAppBarDefaults.TopAppBarExpandedHeight, "Collapsed Scenarios")
    }

    private fun assertListBelow(
        barHeight: Dp,
        screen: String,
    ) {
        compose.waitForIdle()
        val listTop =
            compose
                .onNode(hasScrollAction())
                .fetchSemanticsNode()
                .positionInRoot.y
        val barBottom = with(compose.density) { barHeight.toPx() }
        assertTrue(listTop >= barBottom, "$screen list stays below its fixed header: listTop=$listTop, barBottom=$barBottom")
    }

    private class MemoryStore<T>(
        initial: T,
    ) : DataStore<T> {
        override val data = MutableStateFlow(initial)

        override suspend fun updateData(transform: suspend (T) -> T): T = transform(data.value).also { data.value = it }
    }
}
