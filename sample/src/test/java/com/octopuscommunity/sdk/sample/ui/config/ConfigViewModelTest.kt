package com.octopuscommunity.sdk.sample.ui.config

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.sample.data.datastore.SampleDataStoreTestFixture
import com.octopuscommunity.sdk.sample.data.datastore.closeSampleViewModels
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.data.resolveAuthPreset
import com.octopuscommunity.sdk.sample.data.usecase.CommunityConnectUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ConfigViewModelTest {
    @Test
    fun `live stored auth follows Unified Profile on off while a theme draft survives`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val context = RuntimeEnvironment.getApplication()
            val dataStores = SampleDataStoreTestFixture(context, backgroundScope)
            val configs = dataStores.configs
            val users = dataStores.users
            val store = ViewModelStore()
            try {
                configs.updateConfig { SDKConfig(authKind = SDKConfig.AuthKind.SSO()) }
                users.clear()
                val viewModel = ConfigViewModel(configs, users, CommunityConnectUseCase(users), backgroundScope)
                store.put("config", viewModel)
                // Wait for initial storage hydration rather than relying on an already-default state.
                configs.updateConfig { it!!.copy(appearance = SDKConfig.Appearance.Dark) }
                viewModel.state.first { it.appearance == SDKConfig.Appearance.Dark }
                viewModel.setThemePreset(navy = false)
                configs.updateConfig {
                    it!!.withUnifiedProfile(true).copy(authKind = SDKConfig.AuthKind.SSO(ProfileField.ALL.toList(), true))
                }
                val on = viewModel.state.first { it.forceLogin && it.managedFields.isNotEmpty() }
                assertEquals("ssoAllManagedForced", resolveAuthPreset(on.forceLogin, on.managedFields).id)
                configs.updateConfig { it!!.withUnifiedProfile(false).copy(authKind = SDKConfig.AuthKind.SSO()) }
                val off = viewModel.state.first { !it.forceLogin && it.managedFields.isEmpty() }
                assertEquals("ssoNoManagedNoForced", resolveAuthPreset(off.forceLogin, off.managedFields).id)
                assertEquals(SDKConfig.Theme.SdkDefault, off.theme)
            } finally {
                closeSampleViewModels(store, dataStores)
            }
        }

    @Test
    fun `live config updates preserve an unfinished managed fields selection`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val context = RuntimeEnvironment.getApplication()
            val dataStores = SampleDataStoreTestFixture(context, backgroundScope)
            val configs = dataStores.configs
            val users = dataStores.users
            val store = ViewModelStore()
            try {
                configs.updateConfig { SDKConfig(authKind = SDKConfig.AuthKind.SSO(), appearance = SDKConfig.Appearance.Dark) }
                val viewModel = ConfigViewModel(configs, users, CommunityConnectUseCase(users), backgroundScope)
                store.put("config", viewModel)
                viewModel.state.first { it.appearance == SDKConfig.Appearance.Dark }
                viewModel.toggleManagedField(ProfileField.ALL.first())
                configs.updateConfig { it!!.copy(authKind = SDKConfig.AuthKind.SSO(forceLoginOnStrongActions = true)) }
                val live = viewModel.state.first { it.forceLogin }
                assertTrue(live.managedFields.contains(ProfileField.ALL.first()))
            } finally {
                closeSampleViewModels(store, dataStores)
            }
        }

    @Test
    fun `saving before initial hydration still loads the persisted configuration after restoration`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val context = RuntimeEnvironment.getApplication()
            val dataStores = SampleDataStoreTestFixture(context, backgroundScope)
            val configs = dataStores.configs
            val users = dataStores.users
            val store = ViewModelStore()
            try {
                val persisted = SDKConfig(authKind = SDKConfig.AuthKind.SSO(), appearance = SDKConfig.Appearance.Dark)
                configs.updateConfig { persisted }
                users.clear()
                val handle = SavedStateHandle()
                store.put(
                    "config",
                    ConfigViewModel(configs, users, CommunityConnectUseCase(users), backgroundScope, savedStateHandle = handle),
                )
                // No scheduler turn: save and destroy the ViewModel before its DataStore read.
                val saved = handle.savedStateProvider().saveState()
                store.clear()
                val restoredHandle = SavedStateHandle.createHandle(saved, null)
                val restored =
                    ConfigViewModel(configs, users, CommunityConnectUseCase(users), backgroundScope, savedStateHandle = restoredHandle)
                store.put("restored", restored)
                restored.state.first { it.appearance == SDKConfig.Appearance.Dark }
                assertEquals(persisted, configs.configFlow.first())
            } finally {
                closeSampleViewModels(store, dataStores)
            }
        }

    @Test
    fun `unsaved fields restore after ViewModel destruction without applying the draft`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val context = RuntimeEnvironment.getApplication()
            val dataStores = SampleDataStoreTestFixture(context, backgroundScope)
            val configs = dataStores.configs
            val users = dataStores.users
            val store = ViewModelStore()
            try {
                val persisted = SDKConfig(authKind = SDKConfig.AuthKind.SSO(), appearance = SDKConfig.Appearance.Dark)
                configs.updateConfig { persisted }
                users.clear()
                val handle = SavedStateHandle()
                val original = ConfigViewModel(configs, users, CommunityConnectUseCase(users), backgroundScope, savedStateHandle = handle)
                store.put("config", original)
                original.state.first { it.appearance == SDKConfig.Appearance.Dark }
                original.setServerEnvKind(ConfigViewModel.ServerEnvKind.CUSTOM)
                original.setCustomServerHost("draft.example.invalid")
                original.setCustomServerPort("8443")
                original.setAppearance(SDKConfig.Appearance.Light)
                val draft = original.state.value
                val saved = handle.savedStateProvider().saveState()
                store.clear()

                val restoredHandle = SavedStateHandle.createHandle(saved, null)
                val restored =
                    ConfigViewModel(configs, users, CommunityConnectUseCase(users), backgroundScope, savedStateHandle = restoredHandle)
                store.put("restored", restored)
                testScheduler.runCurrent()
                assertEquals(draft, restored.state.value)
                assertEquals(persisted, configs.configFlow.first())
            } finally {
                closeSampleViewModels(store, dataStores)
            }
        }

    @Test
    fun `a restored draft keeps its edits while untouched fields follow later storage changes`() =
        runTest {
            withRestoredDraft { restored, configs ->
                testScheduler.runCurrent()
                assertEquals(SDKConfig.Theme.Default, restored.state.value.theme)
                configs.value = configs.value.copy(theme = SDKConfig.Theme.SdkDefault, appearance = SDKConfig.Appearance.System)
                testScheduler.runCurrent()
                assertEquals(SDKConfig.Theme.SdkDefault, restored.state.value.theme)
                assertEquals(SDKConfig.Appearance.Light, restored.state.value.appearance)
            }
        }

    @Test
    fun `a restored draft keeps a new System appearance edit over later storage changes`() =
        runTest {
            withRestoredDraft { restored, configs ->
                restored.setAppearance(SDKConfig.Appearance.System)
                testScheduler.runCurrent()
                assertEquals(SDKConfig.Appearance.System, restored.state.value.appearance)
                assertEquals(SDKConfig.Appearance.Dark, configs.value.appearance)

                configs.value = configs.value.copy(appearance = SDKConfig.Appearance.Light)
                testScheduler.runCurrent()
                assertEquals(SDKConfig.Appearance.System, restored.state.value.appearance)
            }
        }

    @Test
    fun `a restored draft follows storage changes already present in its first emission`() =
        runTest {
            withRestoredDraft(
                beforeRestore = { handle, _ ->
                    val draft = assertNotNull(handle.get<Bundle>("configDraft"))
                    val baseline = Json.decodeFromString<ConfigViewModel.State>(assertNotNull(draft.getString("baseline")))
                    assertEquals(SDKConfig.Appearance.Dark, baseline.appearance)
                    assertEquals(SDKConfig.Theme.Default, baseline.theme)
                },
            ) { restored, configs ->
                // The restored collector has not started: its first snapshot is the changed one.
                configs.value = configs.value.copy(theme = SDKConfig.Theme.SdkDefault, appearance = SDKConfig.Appearance.System)
                testScheduler.runCurrent()
                assertEquals(SDKConfig.Theme.SdkDefault, restored.state.value.theme)
                assertEquals(SDKConfig.Appearance.Light, restored.state.value.appearance)
            }
        }

    private suspend fun TestScope.withRestoredDraft(
        beforeRestore: (SavedStateHandle, MutableStateFlow<SDKConfig>) -> Unit = { _, _ -> },
        block: (ConfigViewModel, MutableStateFlow<SDKConfig>) -> Unit,
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val context = RuntimeEnvironment.getApplication()
        val dataStores = SampleDataStoreTestFixture(context, backgroundScope)
        val configs = dataStores.configs
        val users = dataStores.users
        val storedConfig = MutableStateFlow(SDKConfig(authKind = SDKConfig.AuthKind.SSO(), appearance = SDKConfig.Appearance.Dark))
        val store = ViewModelStore()

        fun viewModel(handle: SavedStateHandle) =
            ConfigViewModel(
                configs,
                users,
                CommunityConnectUseCase(users),
                backgroundScope,
                savedStateHandle = handle,
                configFlow = storedConfig,
                userFlow = flowOf(null),
            )
        try {
            val handle = SavedStateHandle()
            val original = viewModel(handle)
            store.put("config", original)
            testScheduler.runCurrent()
            assertEquals(SDKConfig.Appearance.Dark, original.state.value.appearance)
            original.setAppearance(SDKConfig.Appearance.Light)
            val saved = handle.savedStateProvider().saveState()
            store.clear()

            val restoredHandle = SavedStateHandle.createHandle(saved, null)
            beforeRestore(restoredHandle, storedConfig)
            val restored = viewModel(restoredHandle)
            store.put("restored", restored)
            assertEquals(SDKConfig.Appearance.Light, restored.state.value.appearance)
            block(restored, storedConfig)
        } finally {
            closeSampleViewModels(store, dataStores)
        }
    }

    @Test
    fun `saveAndStart preserves stored appearance and disabled push registration`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val context = RuntimeEnvironment.getApplication()
            val dataStores = SampleDataStoreTestFixture(context, backgroundScope)
            val configs = dataStores.configs
            val users = dataStores.users
            val store = ViewModelStore()
            try {
                configs.updateConfig {
                    SDKConfig(authKind = SDKConfig.AuthKind.SSO(), appearance = SDKConfig.Appearance.Dark, pushRegistrationEnabled = false)
                }
                users.clear()
                val viewModel = ConfigViewModel(configs, users, CommunityConnectUseCase(users), backgroundScope)
                store.put("config", viewModel)
                viewModel.state.first { it.appearance == SDKConfig.Appearance.Dark }
                viewModel.setThemePreset(navy = false)
                viewModel.saveAndStart()
                val saved = configs.configFlow.first { it?.theme == SDKConfig.Theme.SdkDefault }!!
                assertEquals(SDKConfig.Appearance.Dark, saved.appearance)
                assertFalse(saved.pushRegistrationEnabled)
            } finally {
                closeSampleViewModels(store, dataStores)
            }
        }
}
