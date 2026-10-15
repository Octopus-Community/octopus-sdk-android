package com.octopuscommunity.sdk.sample

import android.app.Application
import androidx.lifecycle.ViewModelStore
import com.octopuscommunity.sdk.sample.data.AppUpdateChecker
import com.octopuscommunity.sdk.sample.data.SDKTestFakesRequest
import com.octopuscommunity.sdk.sample.data.datastore.SampleDataStoreTestFixture
import com.octopuscommunity.sdk.sample.data.datastore.closeSampleViewModels
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MainViewModelTest {
    @Test
    fun `fake mode supplies only an in-memory config and clearing it restores the config screen`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val context = RuntimeEnvironment.getApplication()
            val dataStores = SampleDataStoreTestFixture(context, backgroundScope)
            val configDataStore = dataStores.configs
            val requests = MutableStateFlow<SDKTestFakesRequest?>(SDKTestFakesRequest("test-user"))
            val store = ViewModelStore()
            try {
                configDataStore.clear()
                val viewModel = MainViewModel(configDataStore, AppUpdateChecker(context), requests)
                store.put("main", viewModel)
                val fakeState = viewModel.state.first { it.sdkConfig != null }
                assertEquals(SDKConfig.AuthKind.SSO(forceLoginOnStrongActions = true), fakeState.sdkConfig?.authKind)
                assertNull(configDataStore.configFlow.first(), "Activating fakes must leave the persisted config absent")

                // Process death loses this request; the same persisted config must yield the Config screen.
                requests.value = null
                assertNull(viewModel.state.first { it.sdkConfig == null }.sdkConfig)
                assertNull(configDataStore.configFlow.first())
            } finally {
                closeSampleViewModels(store, dataStores)
            }
        }

    @Test
    fun `fake activation keeps an existing persisted backend selection and appearance`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val context = RuntimeEnvironment.getApplication()
            val dataStores = SampleDataStoreTestFixture(context, backgroundScope)
            val configDataStore = dataStores.configs
            val requests = MutableStateFlow<SDKTestFakesRequest?>(null)
            val store = ViewModelStore()
            val persisted =
                SDKConfig(
                    authKind = SDKConfig.AuthKind.SSO(),
                    appearance = SDKConfig.Appearance.Dark,
                    serverEnvironment = SDKConfig.ServerEnvironment.Custom("qa.example.com"),
                )
            try {
                configDataStore.updateConfig { persisted }
                val viewModel = MainViewModel(configDataStore, AppUpdateChecker(context), requests)
                store.put("main", viewModel)
                assertEquals(persisted, viewModel.state.first { it.sdkConfig != null }.sdkConfig)

                requests.value = SDKTestFakesRequest("test-user")
                testScheduler.runCurrent()
                assertEquals(persisted, viewModel.state.value.sdkConfig)
                assertEquals(persisted, configDataStore.configFlow.first())
            } finally {
                closeSampleViewModels(store, dataStores)
            }
        }

    /** Builds a ViewModel on in-memory config and SDK-readiness flows, cleared after [block]. */
    private suspend fun TestScope.withStartViewModel(
        storedConfig: SDKConfig?,
        block: (MainViewModel, MutableStateFlow<Boolean>) -> Unit,
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val context = RuntimeEnvironment.getApplication()
        val dataStores = SampleDataStoreTestFixture(context, backgroundScope)
        val sdkReady = MutableStateFlow(false)
        val store = ViewModelStore()
        try {
            val viewModel =
                MainViewModel(
                    configDataStore = dataStores.configs,
                    appUpdateChecker = AppUpdateChecker(context),
                    sdkTestFakesRequests = MutableStateFlow(null),
                    configFlow = MutableStateFlow(storedConfig),
                    isSdkInitializedFlow = sdkReady,
                )
            store.put("main", viewModel)
            block(viewModel, sdkReady)
        } finally {
            closeSampleViewModels(store, dataStores)
        }
    }

    @Test
    fun `without a stored config the start resolves at once, on Config`() =
        runTest {
            withStartViewModel(storedConfig = null) { viewModel, _ ->
                testScheduler.runCurrent()
                val state = viewModel.state.value
                assertTrue(state.isStartResolved)
                assertFalse(state.isSdkInitialized)
                assertEquals(0L, testScheduler.currentTime)
            }
        }

    @Test
    fun `a stored config resolves the start only once the SDK reports ready`() =
        runTest {
            withStartViewModel(storedConfig = SDKConfig(authKind = SDKConfig.AuthKind.SSO())) { viewModel, sdkReady ->
                testScheduler.runCurrent()
                assertFalse(viewModel.state.value.isStartResolved)

                testScheduler.advanceTimeBy(MainViewModel.START_RESOLUTION_TIMEOUT_MS / 2)
                testScheduler.runCurrent()
                assertFalse(viewModel.state.value.isStartResolved)

                sdkReady.value = true
                testScheduler.runCurrent()
                val state = viewModel.state.value
                assertTrue(state.isStartResolved)
                assertTrue(state.isSdkInitialized)
                assertTrue(testScheduler.currentTime < MainViewModel.START_RESOLUTION_TIMEOUT_MS)
            }
        }

    @Test
    fun `a SDK that never reports ready resolves the start after the timeout`() =
        runTest {
            withStartViewModel(storedConfig = SDKConfig(authKind = SDKConfig.AuthKind.SSO())) { viewModel, _ ->
                testScheduler.advanceTimeBy(MainViewModel.START_RESOLUTION_TIMEOUT_MS - 1)
                testScheduler.runCurrent()
                assertFalse(viewModel.state.value.isStartResolved)

                testScheduler.advanceTimeBy(2)
                testScheduler.runCurrent()
                val state = viewModel.state.value
                assertTrue(state.isStartResolved)
                assertFalse(state.isSdkInitialized)
            }
        }
}
