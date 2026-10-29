package com.octopuscommunity.sdk.sample.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModelStore
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.sample.data.datastore.InMemorySampleDataStores
import com.octopuscommunity.sdk.sample.data.model.User
import com.octopuscommunity.sdk.sample.data.usecase.CommunityConnectUseCase
import com.octopuscommunity.sdk.test.mock.MockCurrentUserProfiles
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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
class AccountViewModelTest {
    @Test
    fun `reconnect connects once when the user collector changes state during a slow connection`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val users = InMemorySampleDataStores(RuntimeEnvironment.getApplication()).users
            val store = ViewModelStore()
            val finishConnect = CompletableDeferred<Unit>()
            try {
                val user = User(id = "host-user")
                users.updateUser(clearSignOut = true) { user }
                var connectCalls = 0
                val connect =
                    CommunityConnectUseCase(users, isSigningAvailable = { true }) { _, _ ->
                        connectCalls++
                        finishConnect.await()
                        OctopusResult.Success(ConnectionState.Connected(MockCurrentUserProfiles.default.copy(isGuest = false)))
                    }
                val viewModel = AccountViewModel(users, connect, sdkTestFakesRequests = MutableStateFlow(null))
                store.put("account", viewModel)
                viewModel.state.first { it.userId == user.id }

                viewModel.reconnect()
                testScheduler.runCurrent()
                assertEquals(1, connectCalls)
                assertTrue(viewModel.state.value.isReconnecting)

                // The production user collector updates _state while connect is suspended.
                users.updateUser(clearSignOut = false) { it?.copy(nickname = "updated while connecting") }
                viewModel.state.first { it.nickname == "updated while connecting" }
                assertTrue(viewModel.state.value.isReconnecting)

                finishConnect.complete(Unit)
                testScheduler.runCurrent()

                assertEquals(1, connectCalls)
                assertFalse(viewModel.state.value.isReconnecting)
                assertEquals("Connected as ${user.id}.", viewModel.state.value.message)
                assertEquals("updated while connecting", viewModel.state.value.nickname)
                assertNull(viewModel.state.value.error)
            } finally {
                try {
                    store.clear()
                    testScheduler.runCurrent()
                } finally {
                    Dispatchers.resetMain()
                }
            }
        }

    @Test
    fun `refreshEntitlements refreshes once when the user collector changes state during a slow refresh`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val users = InMemorySampleDataStores(RuntimeEnvironment.getApplication()).users
            val store = ViewModelStore()
            val finishRefresh = CompletableDeferred<Unit>()
            try {
                val user = User(id = "host-user")
                users.updateUser(clearSignOut = true) { user }
                var refreshCalls = 0
                val connect = CommunityConnectUseCase(users, isSigningAvailable = { true }) { _, _ -> error("not used") }
                val viewModel =
                    AccountViewModel(users, connect, sdkTestFakesRequests = MutableStateFlow(null)) {
                        refreshCalls++
                        finishRefresh.await()
                        OctopusResult.Success(Unit)
                    }
                store.put("account", viewModel)
                viewModel.state.first { it.userId == user.id }

                viewModel.refreshEntitlements()
                testScheduler.runCurrent()
                assertEquals(1, refreshCalls)
                assertTrue(viewModel.state.value.isRefreshing)

                // The production user collector updates _state while the refresh is suspended.
                users.updateUser(clearSignOut = false) { it?.copy(nickname = "updated while refreshing") }
                viewModel.state.first { it.nickname == "updated while refreshing" }
                assertTrue(viewModel.state.value.isRefreshing)

                finishRefresh.complete(Unit)
                testScheduler.runCurrent()

                assertEquals(1, refreshCalls)
                assertFalse(viewModel.state.value.isRefreshing)
                assertEquals("Entitlements refreshed.", viewModel.state.value.message)
                assertEquals("updated while refreshing", viewModel.state.value.nickname)
                assertNull(viewModel.state.value.error)
            } finally {
                try {
                    store.clear()
                    testScheduler.runCurrent()
                } finally {
                    Dispatchers.resetMain()
                }
            }
        }
}
