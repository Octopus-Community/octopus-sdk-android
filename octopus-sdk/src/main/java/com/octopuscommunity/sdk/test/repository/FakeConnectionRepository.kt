package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.data.utils.call
import com.octopuscommunity.sdk.domain.model.ClientUser
import com.octopuscommunity.sdk.domain.model.CommunityAccessDenial
import com.octopuscommunity.sdk.domain.model.ConnectionMode
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.domain.model.User
import com.octopuscommunity.sdk.domain.model.UserConfig
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.ServerError
import com.octopuscommunity.sdk.domain.repository.ClientUserError
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionResult
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.domain.repository.DeletedAccountRecovery
import com.octopuscommunity.sdk.domain.repository.GuestError
import com.octopuscommunity.sdk.domain.repository.RefreshEntitlementsError
import com.octopuscommunity.sdk.domain.repository.SsoBanState
import com.octopuscommunity.sdk.test.mock.MockCurrentUserProfiles
import com.octopuscommunity.sdk.test.mock.MockFeeds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [ConnectionRepository] that connects locally with mock profiles.
 *
 * A client user id containing `banned` makes [connectUser] fail with
 * [ClientUserError.UserBanned].
 */
class FakeConnectionRepository(
    override val connectionMode: ConnectionMode = ConnectionMode.SSO(),
    override var appManagedFields: Set<ProfileField> = emptySet(),
) : FakeProfileRepository(),
    ConnectionRepository,
    SsoBanState,
    DeletedAccountRecovery {
    private var ssoBan: ClientUserError.UserBanned? = null
    private val clientUserState = MutableStateFlow<ClientUser?>(null)
    private var clientUserTokenProvider: (suspend () -> String)? = null

    /** Reads the simulated SSO refusal without changing the current guest session. */
    override fun getUserBannedMessage(): String? = ssoBan?.errorMessage

    /** Gives the access back when the fake reports a deleted account, then checks the connection. */
    override suspend fun recoverFromDeletedAccount(): OctopusResult<ConnectionResult, ServerError> {
        if (getUserConfig()?.accessDenial == CommunityAccessDenial.AccountDeleted) {
            setUserConfig(UserConfig(canAccessCommunity = true))
        }
        return checkConnection()
    }

    override suspend fun getConnectionState() = getConnectionState(_currentUserProfile.value)

    override fun getConnectionStateFlow(): Flow<ConnectionState> =
        getProfileFlow().map { profile ->
            getConnectionState(profile)
        }

    private fun getConnectionState(profile: CurrentUserProfile? = _currentUserProfile.value): ConnectionState =
        when {
            profile != null -> ConnectionState.Connected(profile = profile)
            else -> ConnectionState.NotConnected
        }

    override suspend fun checkConnection(profile: CurrentUserProfile?): OctopusResult<ConnectionResult, ServerError> =
        when (getConnectionState()) {
            is ConnectionState.NotConnected -> ssoBan?.let { OctopusResult.Failure.InvalidArguments(it) } ?: connectAsGuest()
            is ConnectionState.Connected ->
                when (val result = fetchProfile()) {
                    is OctopusResult.Success ->
                        OctopusResult.Success(
                            ConnectionResult(
                                getConnectionState(),
                            ),
                        )

                    is OctopusResult.Failure -> result
                }
        }

    override suspend fun connectAsGuest(): OctopusResult<ConnectionResult, GuestError> =
        call {
            val guestUser = User("guest-id", "guest@example.com")
            val guestProfile =
                MockCurrentUserProfiles.default.copy(
                    userId = guestUser.id,
                    postsFeeds = MockFeeds.Post.create(id = "profile_${guestUser.id}"),
                    isGuest = true,
                )

            user.value = guestUser
            setCurrentUserProfile(guestProfile)

            ConnectionResult(ConnectionState.Connected(profile = guestProfile))
        }

    // SSO methods
    override suspend fun getClientUser() = clientUserState.value

    override fun getClientUserFlow() = clientUserState.asStateFlow()

    /**
     * Upgrades a guest session to a connected SSO user, like the real repository. User ids containing
     * `banned` simulate an SSO refusal while retaining the current guest.
     */
    override suspend fun connectUser(
        clientUser: ClientUser,
        tokenProvider: suspend () -> String,
    ): OctopusResult<ConnectionResult, ClientUserError> =
        call {
            clientUserTokenProvider = tokenProvider
            val previousClientUser = getClientUser()

            if (previousClientUser?.userId != null && previousClientUser.userId != clientUser.userId) {
                logout()
                clientUserTokenProvider = tokenProvider
            }
            if (clientUser.userId.contains("banned")) {
                val ban = ClientUserError.UserBanned("User is banned")
                ssoBan = ban
                return OctopusResult.Failure.InvalidArguments(ban)
            }
            ssoBan = null

            val branchResult: OctopusResult<ConnectionState, ClientUserError> =
                when (val connectionState = getConnectionState()) {
                    is ConnectionState.NotConnected -> {
                        connectWithClientUser(clientUser)
                    }

                    is ConnectionState.Connected -> {
                        if (connectionState.profile.isGuest) {
                            // A guest is a session, not a user: upgrade it to the SSO identity.
                            connectWithClientUser(clientUser)
                        } else if (clientUser.profile != previousClientUser?.profile) {
                            updateProfileWithClientUser(clientUser, connectionState.profile)
                        } else {
                            OctopusResult.Success(connectionState)
                        }
                    }
                }
            // A failed branch (a token that is empty or throws, like the real repository's
            // MissingToken) must not be reported as a success.
            if (branchResult is OctopusResult.Failure<ClientUserError>) return branchResult
            return OctopusResult.Success(ConnectionResult(getConnectionState()))
        }

    private suspend fun connectWithClientUser(clientUser: ClientUser): OctopusResult<ConnectionState, ClientUserError> {
        val clientUserToken =
            try {
                clientUserTokenProvider?.invoke()?.ifEmpty { null }
            } catch (e: Exception) {
                null
            }

        if (clientUserToken == null) {
            return OctopusResult.Failure.InvalidArguments(
                error = ClientUserError.MissingToken(),
            )
        }

        val mockUser = User("sso-id", "sso-${clientUser.userId}@example.com")
        val mockProfile =
            MockCurrentUserProfiles.default.copy(
                userId = mockUser.id,
                postsFeeds = MockFeeds.Post.create(id = "profile_${mockUser.id}"),
                isGuest = false,
            )

        user.value = mockUser
        clientUserState.value = clientUser
        setCurrentUserProfile(mockProfile)

        updateProfileWithClientUser(clientUser, mockProfile)

        return OctopusResult.Success(getConnectionState(mockProfile))
    }

    private suspend fun updateProfileWithClientUser(
        clientUser: ClientUser,
        octopusProfile: CurrentUserProfile,
    ): OctopusResult<ConnectionState, ClientUserError> =
        call {
            var updatedProfile = octopusProfile
            var hasUpdated = false

            // Update nickname if app-managed or not confirmed
            if ((ProfileField.NICKNAME in appManagedFields || !octopusProfile.hasConfirmedNickname) &&
                clientUser.profile?.nickname?.ifEmpty { null } != null &&
                clientUser.profile?.nickname != octopusProfile.nickname
            ) {
                updatedProfile =
                    updatedProfile.copy(
                        nickname = clientUser.profile?.nickname?.trim() ?: "",
                        hasConfirmedNickname = if (ProfileField.NICKNAME in appManagedFields) true else octopusProfile.hasConfirmedNickname,
                    )
                hasUpdated = true
            }

            // Update bio if app-managed or not confirmed
            if ((ProfileField.BIO in appManagedFields || !octopusProfile.hasConfirmedBio) &&
                clientUser.profile?.bio != octopusProfile.bio
            ) {
                updatedProfile =
                    updatedProfile.copy(
                        bio = clientUser.profile?.bio ?: "",
                        hasConfirmedBio = if (ProfileField.BIO in appManagedFields) true else octopusProfile.hasConfirmedBio,
                    )
                hasUpdated = true
            }

            // Update avatar if app-managed or not confirmed
            if ((ProfileField.PICTURE in appManagedFields || !octopusProfile.hasConfirmedPicture) &&
                clientUser.profile?.picture != octopusProfile.picture
            ) {
                updatedProfile =
                    updatedProfile.copy(
                        picture = clientUser.profile?.picture,
                        hasConfirmedPicture = if (ProfileField.PICTURE in appManagedFields) true else octopusProfile.hasConfirmedPicture,
                    )
                hasUpdated = true
            }

            if (hasUpdated) {
                setCurrentUserProfile(updatedProfile)
            }

            clientUserState.value = clientUser
            getConnectionState(profile = updatedProfile)
        }

    /** Re-invokes the SSO token provider, rejecting missing providers and guest sessions. */
    override suspend fun refreshEntitlements(): OctopusResult<Unit, RefreshEntitlementsError> {
        val tokenProvider =
            clientUserTokenProvider
                ?: return OctopusResult.Failure.InvalidArguments(RefreshEntitlementsError.NoClientTokenProvider())

        val state = getConnectionState()
        if (state !is ConnectionState.Connected || state.profile.isGuest) {
            return OctopusResult.Failure.InvalidArguments(RefreshEntitlementsError.UserNotConnected())
        }

        return call {
            if (tokenProvider().isEmpty()) {
                return OctopusResult.Failure.InvalidArguments(RefreshEntitlementsError.NoClientTokenProvider())
            }
            Unit
        }
    }

    override suspend fun logout() {
        ssoBan = null
        user.value = null
        clientUserState.value = null
        clientUserTokenProvider = null
        setCurrentUserProfile(null)

        // Auto-connect as guest after logout
        connectAsGuest()
    }
}
