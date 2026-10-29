package com.octopuscommunity.sdk.sample.data.usecase

import android.app.Application
import com.octopuscommunity.sdk.domain.model.ClientUser
import com.octopuscommunity.sdk.domain.model.ConnectionMode
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.mapSuccess
import com.octopuscommunity.sdk.domain.repository.ClientUserError
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.sample.data.datastore.InMemorySampleDataStores
import com.octopuscommunity.sdk.sample.data.model.User
import com.octopuscommunity.sdk.test.mock.MockCurrentUserProfiles
import com.octopuscommunity.sdk.test.repository.FakeConnectionRepository
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CommunityConnectUseCaseTest {
    @Test
    fun `recreated Activity acknowledgment cannot consume a different failure with the same message`() =
        runTest {
            val users = InMemorySampleDataStores(RuntimeEnvironment.getApplication()).users
            val connect = CommunityConnectUseCase(users, isSigningAvailable = { true })
            connect.reportConfigConnectionFailure(OctopusResult.Failure.NoNetwork)
            connect.reportConfigConnectionFailure(OctopusResult.Failure.NoNetwork)
            val pending = connect.configConnectionFailures.value
            assertEquals(2, pending.size)
            assertEquals(pending[0].message, pending[1].message)

            connect.consumeConfigConnectionFailure(pending[0])
            // A stale collector from the old Activity may finish after the new one.
            connect.consumeConfigConnectionFailure(pending[0])

            assertEquals(listOf(pending[1]), connect.configConnectionFailures.value)
        }

    @Test
    fun `unavailable signing preserves the current member and remembered sign-out without connecting`() =
        runTest {
            val users = InMemorySampleDataStores(RuntimeEnvironment.getApplication()).users
            val current = User(id = "current-user", nickname = "current member")
            users.updateUser(clearSignOut = true) { current }
            users.setSignedOut(true)
            val repository = FakeConnectionRepository(connectionMode = ConnectionMode.SSO())
            assertIs<OctopusResult.Success<*>>(
                repository.connectUser(ClientUser(userId = current.id!!)) { "synthetic-test-jwt" },
            )
            val before = repository.getConnectionState()
            var connectCalls = 0
            val connect =
                CommunityConnectUseCase(users, isSigningAvailable = { false }) { clientUser, token ->
                    connectCalls++
                    repository.connectUser(clientUser, token).mapSuccess { it.connectionState }
                }

            val result = assertIs<OctopusResult.Failure.InvalidArguments<ClientUserError>>(connect(User(id = "different-user")))

            assertEquals(ClientUserError.MissingToken("SSO sign-in is unavailable in this build."), result.error)
            assertEquals(0, connectCalls)
            assertEquals(before, repository.getConnectionState())
            assertEquals(current.id, repository.getClientUser()?.userId)
            assertEquals(current, users.getUser())
            assertTrue(users.isSignedOut())
        }

    @Test
    fun `guest upgrade keeps the host-stored nickname as the source of truth`() =
        runTest {
            val users = InMemorySampleDataStores(RuntimeEnvironment.getApplication()).users
            val user = User(id = "host-user", nickname = "stored nickname", bio = "host bio", entitlements = listOf("test:grant"))
            users.updateUser(clearSignOut = true) { user }
            val repository = FakeConnectionRepository(connectionMode = ConnectionMode.SSO())
            repository.connectAsGuest()
            val connect =
                CommunityConnectUseCase(users, isSigningAvailable = { true }) { clientUser, _ ->
                    assertEquals(user.nickname, clientUser.profile?.nickname)
                    // Only synthetic authentication: never request a real sample JWT.
                    repository.connectUser(clientUser) { "synthetic-test-jwt" }.mapSuccess {
                        // Simulate an SDK that keeps its own confirmed nickname: the fake would
                        // otherwise adopt the host nickname and the assertion below would be vacuous.
                        val connected = assertIs<ConnectionState.Connected>(it.connectionState)
                        connected.copy(profile = connected.profile.copy(nickname = SDK_NICKNAME, hasConfirmedNickname = true))
                    }
                }

            val connection = assertIs<OctopusResult.Success<ConnectionState>>(connect(user)).data
            assertIs<ConnectionState.Connected>(connection)
            // The SDK may keep its own confirmed nickname; the host copy is never rewritten from it,
            // so a nickname just saved from Edit user or Login cannot be silently undone.
            assertEquals(SDK_NICKNAME, connection.profile.nickname)
            assertEquals(user, users.getUser())
        }

    @Test
    fun `a failed connection leaves the stored nickname intact`() =
        runTest {
            val users = InMemorySampleDataStores(RuntimeEnvironment.getApplication()).users
            val user = User(id = "host-user", nickname = "stored nickname")
            users.updateUser(clearSignOut = true) { user }
            val connect = CommunityConnectUseCase(users, isSigningAvailable = { true }) { _, _ -> OctopusResult.Failure.NoNetwork }
            assertIs<OctopusResult.Failure<*>>(connect(user))
            assertEquals(user, users.getUser())
        }

    @Test
    fun `a connect completion cannot overwrite a newer host identity or nickname edit`() =
        runTest {
            for (updated in listOf(User(id = "other-user", nickname = "other"), User(id = "host-user", nickname = "new edit"))) {
                val users = InMemorySampleDataStores(RuntimeEnvironment.getApplication()).users
                val user = User(id = "host-user", nickname = "stored nickname")
                users.updateUser(clearSignOut = true) { user }
                val connect =
                    CommunityConnectUseCase(users, isSigningAvailable = { true }) { _, _ ->
                        users.updateUser(clearSignOut = true) { updated }
                        OctopusResult.Success(ConnectionState.Connected(MockCurrentUserProfiles.default.copy(isGuest = false)))
                    }
                connect(user)
                assertEquals(updated, users.getUser())
            }
        }

    private companion object {
        const val SDK_NICKNAME = "sdk confirmed nickname"
    }
}
