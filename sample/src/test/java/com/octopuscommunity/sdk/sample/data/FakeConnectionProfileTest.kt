package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.domain.model.ClientUser
import com.octopuscommunity.sdk.domain.model.ConnectionMode
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.test.mock.MockCurrentUserProfiles
import com.octopuscommunity.sdk.test.repository.FakeConnectionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

/** Evidence for internal change: the SDK fake supplies a fixture nickname independently of the host SSO id. */
class FakeConnectionProfileTest {
    @Test
    fun `guest upgrade without a managed nickname keeps the fake SDK fixture profile`() =
        runTest {
            val repository = FakeConnectionRepository(connectionMode = ConnectionMode.SSO())
            repository.connectAsGuest()
            repository.connectUser(ClientUser(userId = "qa-tester-android")) { "sample-fake-token" }
            val connection = assertIs<ConnectionState.Connected>(repository.getConnectionState())
            assertFalse(connection.isGuest)
            assertEquals("sso-id", connection.profile.userId)
            assertEquals(MockCurrentUserProfiles.default.nickname, connection.profile.nickname)
            assertEquals("qa-tester-android", repository.getClientUser()?.userId)
        }
}
