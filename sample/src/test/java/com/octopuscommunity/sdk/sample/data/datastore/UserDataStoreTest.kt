package com.octopuscommunity.sdk.sample.data.datastore

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.lifecycle.ViewModelStore
import com.octopuscommunity.sdk.OctopusProfile
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ClientUser
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.usecase.ConnectUseCase
import com.octopuscommunity.sdk.domain.usecase.DisconnectUseCase
import com.octopuscommunity.sdk.domain.usecase.RefreshEntitlementsUseCase
import com.octopuscommunity.sdk.sample.data.CommunitySessionTracker
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.data.model.User
import com.octopuscommunity.sdk.sample.data.usecase.CommunityConnectUseCase
import com.octopuscommunity.sdk.sample.data.usecase.DISCONNECT_FAILURE_MESSAGE
import com.octopuscommunity.sdk.sample.data.usecase.disconnectFromCommunity
import com.octopuscommunity.sdk.sample.ui.config.ConfigViewModel
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenariosViewModel
import com.octopuscommunity.sdk.sample.ui.settings.AccountViewModel
import com.octopuscommunity.sdk.sample.ui.settings.EditUserViewModel
import com.octopuscommunity.sdk.sample.ui.settings.SettingsViewModel
import com.octopuscommunity.sdk.test.di.octopusTestModule
import com.octopuscommunity.sdk.test.repository.FakeConnectionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class UserDataStoreTest {
    private lateinit var userFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var diskStore: DataStore<UserData>
    private val user = User(id = "user-1", nickname = "Ada", entitlements = listOf("customer:premium"))

    @Test
    fun `legacy JSON without a sign-out flag keeps the user signed in`() =
        runTest {
            val legacy = """{"hasData":true,"user":{"id":"user-1","nickname":"Ada"}}"""
            withStores(initialUserJson = legacy) { users, _, _ ->
                assertFalse(users.isSignedOut())
                assertEquals(User(id = "user-1", nickname = "Ada"), users.getUserToConnect())
            }
        }

    @Test
    fun `fresh storage has no remembered sign-out`() =
        runTest {
            withStores { users, _, _ ->
                assertFalse(users.isSignedOut())
                assertNull(users.getUserToConnect())
            }
        }

    @Test
    fun `disconnect persists across launches and community changes without losing the user`() =
        runTest {
            withStores { users, configs, _ ->
                users.updateUser(clearSignOut = true) { user }
                assertEquals(user, users.getUserToConnect())
                users.setSignedOut(true)

                val context = RuntimeEnvironment.getApplication()
                val persisted = Json.decodeFromString(UserData.serializer(), userFile.readText())
                assertTrue(persisted.isSignedOut)
                assertEquals(user, persisted.user)
                dataStoreScope.coroutineContext[kotlinx.coroutines.Job]!!.cancelAndJoin()
                dataStoreScope = newDataStoreScope()
                val relaunched = createUserStore(context)
                assertTrue(relaunched.isSignedOut())
                assertEquals(user, relaunched.getUser())
                assertNull(relaunched.getUserToConnect())

                configs.updateConfig { SDKConfig(authKind = SDKConfig.AuthKind.SSO()) }
                configs.updateConfig { it!!.copy(customCommunity = true, customApiKey = "sample-test-community") }
                assertNull(relaunched.getUserToConnect())
                assertTrue(relaunched.isSignedOut())
            }
        }

    @Test
    fun `each Disconnect action remembers sign-out and keeps the same user for Connect`() =
        runTest {
            withStores { users, configs, viewModels ->
                val fake = FakeConnectionRepository()
                val disconnects = MutableStateFlow(0)
                installConnectionUseCases(
                    object : ConnectionRepository by fake {
                        override suspend fun logout() {
                            assertTrue(users.isSignedOut(), "Sign-out must be stored before SDK logout")
                            assertNull(users.getUserToConnect())
                            fake.logout()
                            disconnects.value++
                        }
                    },
                )
                val connect = CommunityConnectUseCase(users)
                val account = AccountViewModel(users, connect)
                val scenarios = ScenariosViewModel(users, connect, configs, CommunitySessionTracker())
                viewModels.put("account", account)
                viewModels.put("scenarios", scenarios)

                for (disconnect in listOf(account::disconnect, scenarios::executeDisconnectUser)) {
                    users.updateUser(clearSignOut = true) { user }
                    assertIs<OctopusResult.Success<*>>(connect(user))
                    val previousDisconnects = disconnects.value
                    disconnect()
                    disconnects.first { it > previousDisconnects }
                    assertEquals(user, users.getUser())
                    assertNull(users.getUserToConnect())
                }

                scenarios.executeConnectUser()
                scenarios.state.first { it.methods["connectUser"]?.result?.startsWith("Success:") == true }
                assertFalse(users.isSignedOut())
                assertEquals(user, users.getUserToConnect())
                assertEquals(user.id, OctopusSDK.connectionRepository.getClientUser()?.userId)
            }
        }

    @Test
    fun `Disconnect failure is handled by Account and Scenarios and keeps local sign-out`() =
        runTest {
            withStores { users, configs, viewModels ->
                val fake = FakeConnectionRepository()
                installConnectionUseCases(
                    object : ConnectionRepository by fake {
                        override suspend fun logout() {
                            assertTrue(users.isSignedOut())
                            fake.logout()
                            throw IllegalStateException("Simulated disconnect failure")
                        }
                    },
                )
                val connect = CommunityConnectUseCase(users)
                val account = AccountViewModel(users, connect)
                val scenarios = ScenariosViewModel(users, connect, configs, CommunitySessionTracker())
                viewModels.put("account", account)
                viewModels.put("scenarios", scenarios)

                users.updateUser(clearSignOut = true) { user }
                account.disconnect()
                val accountFailure = account.state.first { it.error != null }
                assertEquals(DISCONNECT_FAILURE_MESSAGE, accountFailure.error)
                assertNull(accountFailure.message, "A failed disconnect must not report success")
                assertTrue(users.isSignedOut())
                assertNull(users.getUserToConnect())
                assertEquals(user, users.getUser())

                users.updateUser(clearSignOut = true) { user }
                scenarios.executeDisconnectUser()
                val scenarioFailure =
                    scenarios.state
                        .first { it.methods["disconnectUser"]?.isError == true }
                        .methods
                        .getValue("disconnectUser")
                assertFalse(scenarioFailure.isLoading)
                assertTrue(scenarioFailure.result!!.contains(DISCONNECT_FAILURE_MESSAGE))
                assertTrue(scenarioFailure.result.contains("Simulated disconnect failure"))
                assertTrue(users.isSignedOut())
                assertNull(users.getUserToConnect())
                assertEquals(user, users.getUser())
            }
        }

    @Test
    fun `Disconnect helper returns SDK failures but propagates cancellation`() =
        runTest {
            withStores { users, _, _ ->
                val failure = IllegalStateException("Simulated disconnect failure")
                var thrown: Exception = failure
                installConnectionUseCases(
                    object : ConnectionRepository by FakeConnectionRepository() {
                        override suspend fun logout(): Unit = throw thrown
                    },
                )
                users.updateUser(clearSignOut = true) { user }
                assertSame(failure, users.disconnectFromCommunity().exceptionOrNull())
                assertTrue(users.isSignedOut())

                thrown = CancellationException("Cancelled disconnect")
                assertFailsWith<CancellationException> { users.disconnectFromCommunity() }
                assertTrue(users.isSignedOut())
                assertEquals(user, users.getUser())
            }
        }

    @Test
    fun `Account refresh while signed out preserves sign-out and requested entitlements`() =
        runTest {
            withStores { users, _, viewModels ->
                installConnectionUseCases()
                OctopusSDK.connectionRepository.connectAsGuest()
                users.updateUser(clearSignOut = true) { user }
                users.setSignedOut(true)
                val account = AccountViewModel(users, CommunityConnectUseCase(users))
                viewModels.put("account", account)
                account.state.first { it.userId == user.id }
                account.toggleEntitlement("customer:moderator", true)
                account.refreshEntitlements()
                val refreshed = account.state.first { it.error != null && !it.isRefreshing }
                assertTrue(refreshed.error!!.startsWith("Refresh failed:"))
                assertTrue(users.isSignedOut())
                assertNull(users.getUserToConnect())
                assertEquals(user.copy(entitlements = listOf("customer:premium", "customer:moderator")), users.getUser())
            }
        }

    @Test
    fun `Edit profile emission while signed out preserves stored SSO entitlements`() =
        runTest {
            withStores { users, _, viewModels ->
                users.updateUser(clearSignOut = true) { user }
                users.setSignedOut(true)
                val profiles = MutableStateFlow<OctopusProfile?>(null)
                val edit = EditUserViewModel(users, CommunityConnectUseCase(users), profiles)
                viewModels.put("edit", edit)
                // A non-empty guest set catches replacements as well as accidentally cleared grants.
                profiles.value = OctopusProfile(entitlements = setOf("guest:read"))
                edit.state.first { it.entitlements.isNotEmpty() }
                assertEquals(user.entitlements.toSet(), edit.state.value.entitlements)
                assertTrue(users.isSignedOut())
                assertNull(users.getUserToConnect())
                assertEquals(user, users.getUser())
            }
        }

    @Test
    fun `Edit refresh after a guest profile while signed out preserves saved entitlements`() =
        runTest {
            withStores { users, _, viewModels ->
                installConnectionUseCases()
                OctopusSDK.connectionRepository.connectAsGuest()
                users.updateUser(clearSignOut = true) { user }
                users.setSignedOut(true)
                val profiles = MutableStateFlow<OctopusProfile?>(OctopusProfile(entitlements = emptySet()))
                val edit = EditUserViewModel(users, CommunityConnectUseCase(users), profiles)
                viewModels.put("edit", edit)
                edit.state.first { it.entitlements == user.entitlements.toSet() }
                edit.refreshEntitlements()
                val refreshed = edit.state.first { it.error != null && !it.isRefreshingEntitlements }
                assertTrue(refreshed.error!!.startsWith("Refresh failed:"))
                assertTrue(users.isSignedOut())
                assertNull(users.getUserToConnect())
                assertEquals(user, users.getUser())
            }
        }

    @Test
    fun `Edit save after reinitialization guest connection keeps stored SSO entitlements`() =
        runTest {
            withStores { users, _, viewModels ->
                users.updateUser(clearSignOut = true) { user }
                val original = FakeConnectionRepository()
                installConnectionUseCases(original)
                original.connectUser(ClientUser(userId = user.id!!)) { "sample-fake-token" }
                val profiles = MutableStateFlow<OctopusProfile?>(OctopusProfile(user.entitlements.toSet(), user.id))
                val handled = Channel<OctopusProfile?>(Channel.UNLIMITED)
                val edit =
                    EditUserViewModel(
                        users,
                        CommunityConnectUseCase(users),
                        flow {
                            profiles.collect {
                                emit(it)
                                handled.send(it)
                            }
                        },
                    )
                viewModels.put("edit", edit)
                handled.receive()
                edit.state.first { it.nickname == user.nickname }

                // Model initialize/switchCommunity replacing the SDK container while this editor
                // survives. Capture a guest emission, then reconnect SSO before processing it.
                val replacement = FakeConnectionRepository()
                installConnectionUseCases(replacement)
                replacement.connectAsGuest()
                assertTrue(assertIs<ConnectionRepository.ConnectionState.Connected>(replacement.getConnectionState()).isGuest)
                assertFalse(users.isSignedOut())
                val guest = OctopusProfile(entitlements = setOf("guest:read"))
                replacement.connectUser(ClientUser(userId = user.id!!)) { "sample-fake-token" }
                profiles.value = guest
                assertEquals(guest, handled.receive(), "Wait until the guest mirror and editor update have completed")

                edit.setNickname("Updated")
                edit.save()
                edit.state.first { it.isSaved }
                assertEquals(user.entitlements, users.getUserToConnect()?.entitlements)
                assertEquals(user.entitlements.toSet(), edit.state.value.entitlements)
                assertEquals("Updated", users.getUser()?.nickname)
            }
        }

    @Test
    fun `Edit signed-in profile mirrors backend grants without creating a missing user`() =
        runTest {
            withStores { users, _, viewModels ->
                val connection = FakeConnectionRepository()
                installConnectionUseCases(connection)
                connection.connectUser(ClientUser(userId = user.id!!)) { "sample-fake-token" }
                users.updateUser(clearSignOut = true) { user }
                val profiles = MutableStateFlow<OctopusProfile?>(null)
                val edit = EditUserViewModel(users, CommunityConnectUseCase(users), profiles)
                viewModels.put("edit", edit)
                profiles.value = OctopusProfile(entitlements = setOf("customer:moderator"), clientUserId = user.id)
                edit.state.first { it.entitlements == setOf("customer:moderator") }
                assertEquals(user.copy(entitlements = listOf("customer:moderator")), users.getUserToConnect())

                users.clear()
                profiles.value = OctopusProfile(entitlements = setOf("guest:read"))
                edit.state.first { it.entitlements.isEmpty() }
                assertNull(users.getUser())
                assertNull(users.getUserToConnect())
            }
        }

    @Test
    fun `explicit connect clears sign-out without needing a user write`() =
        runTest {
            withStores { users, _, _ ->
                installConnectionUseCases()
                users.updateUser(clearSignOut = true) { user }
                users.setSignedOut(true)
                assertIs<OctopusResult.Success<*>>(CommunityConnectUseCase(users)(user))
                assertFalse(users.isSignedOut())
                assertEquals(user, users.getUserToConnect())
            }
        }

    @Test
    fun `entitlement writes keep sign-out across launches while updating the stored user`() =
        runTest {
            withStores { users, _, _ ->
                users.updateUser(clearSignOut = true) { user }
                users.setSignedOut(true)
                val entitlements = listOf("customer:moderator")
                users.updateUser(clearSignOut = false) { it?.copy(entitlements = entitlements) }
                assertTrue(users.isSignedOut())
                assertNull(users.getUserToConnect())
                assertEquals(user.copy(entitlements = entitlements), users.getUser())

                dataStoreScope.coroutineContext[kotlinx.coroutines.Job]!!.cancelAndJoin()
                dataStoreScope = newDataStoreScope()
                val relaunched = createUserStore(RuntimeEnvironment.getApplication())
                assertTrue(relaunched.isSignedOut())
                assertNull(relaunched.getUserToConnect())
                assertEquals(user.copy(entitlements = entitlements), relaunched.getUser())
            }
        }

    @Test
    fun `entitlement writes keep signed-in users available to connect`() =
        runTest {
            withStores { users, _, _ ->
                users.updateUser(clearSignOut = true) { user }
                users.updateUser(clearSignOut = false) { it?.copy(entitlements = emptyList()) }
                assertFalse(users.isSignedOut())
                assertEquals(user.copy(entitlements = emptyList()), users.getUserToConnect())
            }
        }

    @Test
    fun `sign-in write clears sign-out before publishing the same user`() =
        runTest {
            withStores { users, _, _ ->
                users.updateUser(clearSignOut = true) { user }
                users.setSignedOut(true)
                users.updateUser(clearSignOut = true) { user }
                users.userFlow.first { !users.isSignedOut() }
                assertFalse(users.isSignedOut())
                assertEquals(user, users.getUserToConnect())
            }
        }

    @Test
    fun `Config user change clears sign-out even when SDK reinitialization handles the connect`() =
        runTest {
            withStores { users, configs, viewModels ->
                configs.updateConfig { SDKConfig(authKind = SDKConfig.AuthKind.SSO()) }
                users.updateUser(clearSignOut = true) { user }
                users.setSignedOut(true)
                val config = ConfigViewModel(configs, users, CommunityConnectUseCase(users), backgroundScope)
                viewModels.put("config", config)
                config.state.first { it.ssoUserId == user.id }
                config.setSsoUserId("user-2")
                config.setUseCustomCommunity(true)
                config.setCustomApiKey("sample-test-community")
                config.saveAndStart()
                users.userFlow.first { it?.id == "user-2" }
                configs.configFlow.first { it?.customCommunity == true }
                assertFalse(users.isSignedOut())
                assertEquals(User(id = "user-2"), users.getUserToConnect())
            }
        }

    @Test
    fun `Reset configuration clears the user and remembered sign-out`() =
        runTest {
            withStores { users, configs, viewModels ->
                configs.updateConfig { SDKConfig(authKind = SDKConfig.AuthKind.SSO()) }
                users.updateUser(clearSignOut = true) { user }
                users.setSignedOut(true)
                val settings = settingsViewModel(users, configs)
                viewModels.put("settings", settings)
                settings.resetConfig()
                users.userFlow.first { it == null && !users.isSignedOut() }
                assertFalse(users.isSignedOut())
                assertNull(users.getUser())
                assertNull(configs.configFlow.first())
            }
        }

    @Test
    fun `mirroring uses the emitted host identity without waiting for SDK initialization`() =
        runTest {
            withStores { users, _, _ ->
                users.updateUser(clearSignOut = true) { user }
                OctopusSDK.stop()
                assertFalse(OctopusSDK.isInitialisedFlow.value)
                for (profile in listOf(null, OctopusProfile(setOf("guest:read")), OctopusProfile(emptySet(), "other-user"))) {
                    users.mirrorGrantedEntitlements(profile)
                    assertEquals(user, users.getUser())
                }
                users.mirrorGrantedEntitlements(OctopusProfile(emptySet(), user.id))
                assertEquals(user.copy(entitlements = emptyList()), users.getUser())

                // Null host ids must not match a stored user whose id is also null.
                val anonymous = user.copy(id = null)
                users.updateUser(clearSignOut = true) { anonymous }
                users.mirrorGrantedEntitlements(OctopusProfile(emptySet()))
                assertEquals(anonymous, users.getUser())

                users.updateUser(clearSignOut = true) { user }
                users.setSignedOut(true)
                users.mirrorGrantedEntitlements(OctopusProfile(emptySet(), user.id))
                assertEquals(user, users.getUser())
                assertTrue(users.isSignedOut())
                users.clear()
                users.mirrorGrantedEntitlements(OctopusProfile(emptySet(), user.id))
                assertNull(users.getUser())
            }
        }

    @Test
    fun `mirroring checks the saved identity inside the transaction after a user switch`() =
        runTest {
            withStores { users, _, _ ->
                users.updateUser(clearSignOut = true) { user }
                val beforeTransaction = CompletableDeferred<Unit>()
                val proceed = CompletableDeferred<Unit>()
                val backing = diskStore
                val delayed =
                    UserDataStore(
                        RuntimeEnvironment.getApplication(),
                        object : DataStore<UserData> by backing {
                            override suspend fun updateData(transform: suspend (UserData) -> UserData): UserData {
                                beforeTransaction.complete(Unit)
                                proceed.await()
                                return backing.updateData(transform)
                            }
                        },
                    )
                val mirror = async { delayed.mirrorGrantedEntitlements(OctopusProfile(emptySet(), user.id)) }
                beforeTransaction.await()
                val replacement = user.copy(id = "user-2", entitlements = listOf("customer:moderator"))
                users.updateUser(clearSignOut = true) { replacement }
                proceed.complete(Unit)
                assertEquals(replacement, mirror.await().user)
                assertEquals(replacement, users.getUser())
            }
        }

    @Test
    fun `editor processes identity changes even when profile grants are unchanged`() =
        runTest {
            withStores { users, _, viewModels ->
                users.updateUser(clearSignOut = true) { user }
                val profiles = Channel<OctopusProfile?>(Channel.UNLIMITED)
                val handled = Channel<Unit>(Channel.UNLIMITED)
                val edit =
                    EditUserViewModel(
                        users,
                        CommunityConnectUseCase(users),
                        flow {
                            for (profile in profiles) {
                                emit(profile)
                                handled.send(Unit)
                            }
                        },
                    )
                viewModels.put("edit", edit)
                edit.state.first { it.entitlements == user.entitlements.toSet() }
                val grants = setOf("customer:moderator")
                profiles.send(OctopusProfile(grants, "other-user"))
                handled.receive()
                assertEquals(user, users.getUser())
                profiles.send(OctopusProfile(grants, user.id))
                handled.receive()
                edit.state.first { it.entitlements == grants }
                assertEquals(grants, users.getUser()?.entitlements?.toSet())
            }
        }

    @Test
    fun `editor follows saved entitlements without a new profile and keeps drafts on unrelated writes`() =
        runTest {
            withStores { users, _, viewModels ->
                users.updateUser(clearSignOut = true) { user }
                val profiles = MutableStateFlow<OctopusProfile?>(null)
                val edit = EditUserViewModel(users, CommunityConnectUseCase(users), profiles)
                viewModels.put("edit", edit)
                edit.state.first { it.entitlements == user.entitlements.toSet() }
                edit.toggleEntitlement("customer:moderator", true)
                val draft = edit.state.value.entitlements
                users.updateUser(clearSignOut = false) { it?.copy(nickname = "Updated") }
                users.userFlow.first { it?.nickname == "Updated" }
                runCurrent()
                assertEquals(draft, edit.state.value.entitlements)

                users.updateUser(clearSignOut = false) { it?.copy(entitlements = listOf("customer:verified")) }
                edit.state.first { it.entitlements == setOf("customer:verified") }
                users.clear()
                edit.state.first { it.entitlements.isEmpty() }
                assertNull(profiles.value)
            }
        }

    @Test
    fun `local sign-out write failure still ends the SDK session and returns the write error`() =
        runTest {
            val failure = IOException("Simulated sign-out write failure")
            withStores(signOutWriteFailure = failure) { users, _, _ ->
                val connection = FakeConnectionRepository()
                installConnectionUseCases(connection)
                users.updateUser(clearSignOut = true) { user }
                assertIs<OctopusResult.Success<*>>(CommunityConnectUseCase(users)(user))
                val returned = assertIs<IOException>(users.disconnectFromCommunity().exceptionOrNull())
                assertEquals(failure.message, returned.message)
                assertNull(connection.getClientUser())
                assertTrue(assertIs<ConnectionRepository.ConnectionState.Connected>(connection.getConnectionState()).isGuest)
                assertFalse(users.isSignedOut())
                assertEquals(user, users.getUser())
            }
        }

    @Test
    fun `local sign-out write failure is shown by Account and Scenarios`() =
        runTest {
            withStores(signOutWriteFailure = IOException("Simulated sign-out write failure")) { users, configs, viewModels ->
                val connection = FakeConnectionRepository()
                var disconnects = 0
                installConnectionUseCases(
                    object : ConnectionRepository by connection {
                        override suspend fun logout() {
                            connection.logout()
                            disconnects++
                        }
                    },
                )
                users.updateUser(clearSignOut = true) { user }
                val connect = CommunityConnectUseCase(users)
                val account = AccountViewModel(users, connect)
                val scenarios = ScenariosViewModel(users, connect, configs, CommunitySessionTracker())
                viewModels.put("account", account)
                viewModels.put("scenarios", scenarios)
                account.disconnect()
                val accountFailure = account.state.first { it.error != null }
                assertEquals(DISCONNECT_FAILURE_MESSAGE, accountFailure.error)
                assertNull(accountFailure.message)
                scenarios.executeDisconnectUser()
                val scenarioFailure =
                    scenarios.state.first { it.methods["disconnectUser"]?.isError == true }.methods.getValue(
                        "disconnectUser",
                    )
                assertFalse(scenarioFailure.isLoading)
                assertTrue(scenarioFailure.result!!.contains(DISCONNECT_FAILURE_MESSAGE))
                assertEquals(2, disconnects)
            }
        }

    @Test
    fun `both sign-out write and SDK failures are preserved`() =
        runTest {
            val writeFailure = IOException("Simulated sign-out write failure")
            val sdkFailure = IllegalStateException("Simulated disconnect failure")
            withStores(signOutWriteFailure = writeFailure) { users, _, _ ->
                installConnectionUseCases(
                    object : ConnectionRepository by FakeConnectionRepository() {
                        override suspend fun logout(): Unit = throw sdkFailure
                    },
                )
                val result = users.disconnectFromCommunity()
                assertSame(sdkFailure, result.exceptionOrNull())
                val suppressed = assertIs<IOException>(sdkFailure.suppressed.single())
                assertEquals(writeFailure.message, suppressed.message)
            }
        }

    @Test
    fun `cancellation during the sign-out write propagates without starting SDK disconnect`() =
        runTest {
            withStores(signOutWriteFailure = CancellationException("Cancelled write")) { users, _, _ ->
                var disconnects = 0
                installConnectionUseCases(
                    object : ConnectionRepository by FakeConnectionRepository() {
                        override suspend fun logout() {
                            disconnects++
                        }
                    },
                )
                assertFailsWith<CancellationException> { users.disconnectFromCommunity() }
                assertEquals(0, disconnects)
            }
        }

    @Test
    fun `sign-in notification is emitted only on success including a same-user reconnect`() =
        runTest {
            withStores { users, _, _ ->
                users.updateUser(clearSignOut = true) { user }
                val connect = CommunityConnectUseCase(users)
                var successfulSignIns = 0
                backgroundScope.launch { connect.successfulSignIns.collect { successfulSignIns++ } }
                runCurrent()
                assertIs<OctopusResult.Failure<*>>(connect(user.copy(id = "banned-user")))
                runCurrent()
                assertEquals(0, successfulSignIns)
                assertIs<OctopusResult.Success<*>>(connect(user))
                runCurrent()
                assertEquals(1, successfulSignIns)
                assertIs<OctopusResult.Success<*>>(connect(user))
                runCurrent()
                assertEquals(2, successfulSignIns)
            }
        }

    @Test
    fun `teardown resets Main even when closing the config stores fails`() =
        runTest {
            val originalMain = Dispatchers.Main.immediate
            val closeFailure = IllegalStateException("Simulated close failure")
            try {
                val failure =
                    assertFailsWith<IllegalStateException> {
                        withStores(closeFailure = closeFailure) { _, _, _ -> }
                    }
                assertSame(closeFailure, failure)
                assertSame(originalMain, Dispatchers.Main.immediate, "resetMain must restore the real Main dispatcher")
            } finally {
                // Keep a regressed teardown from leaking resources or Main into subsequent tests.
                try {
                    dataStoreScope.coroutineContext[Job]!!.cancelAndJoin()
                    userFile.parentFile!!.deleteRecursively()
                    OctopusSDK.stop()
                } finally {
                    Dispatchers.resetMain()
                }
            }
        }

    private fun installConnectionUseCases(connectionRepository: ConnectionRepository = FakeConnectionRepository()) {
        // Only fake repositories and SDK use cases; no initialization or network.
        OctopusSDK.koinApp =
            koinApplication {
                modules(
                    octopusTestModule,
                    module {
                        single<ConnectionRepository> { connectionRepository }
                        factory { ConnectUseCase(get(), get(), get()) }
                        factory { DisconnectUseCase(get(), get()) }
                        factory { RefreshEntitlementsUseCase(get(), get(), get(), get(), get()) }
                    },
                )
            }
    }

    private fun createUserStore(
        context: Context,
        signOutWriteFailure: Exception? = null,
    ): UserDataStore {
        val backing = DataStoreFactory.create(serializer = UserDataSerializer, scope = dataStoreScope) { userFile }
        diskStore = backing
        val store =
            object : DataStore<UserData> by backing {
                override suspend fun updateData(transform: suspend (UserData) -> UserData): UserData =
                    backing.updateData { data ->
                        val updated = transform(data)
                        if (updated.isSignedOut && signOutWriteFailure != null) throw signOutWriteFailure
                        updated
                    }
            }
        return UserDataStore(context, store)
    }

    private fun TestScope.newDataStoreScope() =
        CoroutineScope(backgroundScope.coroutineContext + SupervisorJob(backgroundScope.coroutineContext[Job]) + Dispatchers.IO)

    private fun settingsViewModel(
        users: UserDataStore,
        configs: SDKConfigDataStore,
    ): SettingsViewModel {
        val context = RuntimeEnvironment.getApplication()
        return SettingsViewModel(configs, users, context.getSharedPreferences("signed-out-test", Context.MODE_PRIVATE))
    }

    private suspend fun TestScope.withStores(
        initialUserJson: String? = null,
        signOutWriteFailure: Exception? = null,
        closeFailure: Exception? = null,
        block: suspend (UserDataStore, SDKConfigDataStore, ViewModelStore) -> Unit,
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val context = RuntimeEnvironment.getApplication()
        userFile = Files.createTempDirectory(context.cacheDir.toPath(), "sample-user-").resolve("user.json").toFile()
        initialUserJson?.let { userFile.writeText(it) }
        dataStoreScope = newDataStoreScope()
        val users = createUserStore(context, signOutWriteFailure)
        val configStores = SampleDataStoreTestFixture(context, backgroundScope, closeFailure)
        val configs = configStores.configs
        val viewModels = ViewModelStore()
        try {
            installConnectionUseCases()
            if (initialUserJson == null) users.clear()
            configs.clear()
            block(users, configs, viewModels)
        } finally {
            try {
                viewModels.clear()
                testScheduler.runCurrent()
            } finally {
                try {
                    configStores.close()
                } finally {
                    try {
                        dataStoreScope.coroutineContext[kotlinx.coroutines.Job]!!.cancelAndJoin()
                    } finally {
                        try {
                            userFile.parentFile!!.deleteRecursively()
                        } finally {
                            try {
                                OctopusSDK.stop()
                            } finally {
                                Dispatchers.resetMain()
                            }
                        }
                    }
                }
            }
        }
    }
}
