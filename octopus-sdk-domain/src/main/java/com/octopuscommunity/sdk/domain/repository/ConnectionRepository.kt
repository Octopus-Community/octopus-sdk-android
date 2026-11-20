package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.ClientUser
import com.octopuscommunity.sdk.domain.model.ConnectionMode
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.ServerError
import kotlinx.coroutines.flow.Flow

/**
 * Repository for guest sessions and users authenticated by the host app (SSO).
 */
interface ConnectionRepository : ProfileRepository {
    /** Connection state of the current user. */
    sealed interface ConnectionState {
        /** No user is connected, not even a guest. */
        data object NotConnected : ConnectionState

        /** A user is connected with [profile], which may be a guest profile. */
        data class Connected(
            override val profile: CurrentUserProfile,
        ) : ConnectionState {
            /** Whether the connected user is a guest. */
            val isGuest get() = profile.isGuest
        }

        /** Profile of the connected user, or `null` when not connected. */
        val profile: CurrentUserProfile? get() = null
    }

    /** Outcome of a connection call. */
    data class ConnectionResult(
        /** Connection state after the call. */
        val connectionState: ConnectionState,
        /** Profile update performed while connecting, if any. */
        val profileUpdate: ProfileUpdateResult? = null,
    )

    /** How users connect to the community, as configured at SDK init. */
    val connectionMode: ConnectionMode

    /** Profile fields that the host app manages and the community UI must not edit. */
    val appManagedFields: Set<ProfileField>

    /** Returns the current connection state. */
    suspend fun getConnectionState(): ConnectionState

    /** Observes the connection state. */
    fun getConnectionStateFlow(): Flow<ConnectionState>

    /** Checks the connection of the cached profile; see the overload taking a profile. */
    suspend fun checkConnection(): OctopusResult<ConnectionResult, ServerError> = checkConnection(profile = getProfile())

    /**
     * Checks the connection for [profile]. Without a profile, reconnects the stored client user
     * or connects as guest.
     */
    suspend fun checkConnection(profile: CurrentUserProfile?): OctopusResult<ConnectionResult, ServerError>

    /** Connects the current user as a guest. */
    suspend fun connectAsGuest(): OctopusResult<ConnectionResult, GuestError>

    /** Logs out a non-guest user: deletes the local profile and user, then reconnects as guest. */
    suspend fun logout()

    // SSO

    /** Returns the client user passed to [connectUser], or `null` when there is none. */
    suspend fun getClientUser(): ClientUser?

    /** Observes the client user passed to [connectUser]. */
    fun getClientUserFlow(): Flow<ClientUser?>

    /**
     * Connects [clientUser]. [tokenProvider] returns the client-signed JWT and is called again when a
     * new one is needed, for example by [refreshEntitlements].
     */
    suspend fun connectUser(
        clientUser: ClientUser,
        tokenProvider: (suspend () -> String),
    ): OctopusResult<ConnectionResult, ClientUserError>

    /**
     * Refreshes the connected user's entitlements with the backend.
     *
     * Re-invokes the `tokenProvider` callback registered at [connectUser] time to obtain a fresh
     * client-signed JWT, exchanges it for a new Octopus JWT (which carries the updated entitlement
     * claims), and updates the cached profile so observers receive the new entitlements.
     *
     * Returns [RefreshEntitlementsError.NoClientTokenProvider] until [connectUser] has
     * registered a token provider, including after a stored session is restored at launch.
     *
     * @return [OctopusResult] indicating success or failure with [RefreshEntitlementsError]
     */
    suspend fun refreshEntitlements(): OctopusResult<Unit, RefreshEntitlementsError>
}

/**
 * Errors that may be returned by [ConnectionRepository.connectAsGuest] inside an
 * [OctopusResult.Failure.InvalidArguments].
 *
 * Of these, only [UserBanned] carries a user-facing string (BE-provided ban reason, suitable
 * for direct display). [Unknown]'s message is not guaranteed to be localized or user-facing.
 */
sealed interface GuestError : ServerError {
    /**
     * The backend refused to hand out a guest session because the user has been banned. The
     * associated message is BE-provided and appropriate for direct display to the user.
     */
    data class UserBanned(
        override val errorMessage: String,
    ) : GuestError

    /** The backend returned a generic error. */
    data class Unknown(
        override val errorMessage: String,
    ) : GuestError
}

/**
 * Errors that may be returned by [ConnectionRepository.connectUser] inside an
 * [OctopusResult.Failure.InvalidArguments].
 */
sealed interface ClientUserError : ProfileUpdateError {
    /** No client-signed token could be obtained. */
    data class MissingToken(
        override val errorMessage: String = "Missing user Token",
    ) : ClientUserError

    /** The user is banned. The message is provided by the backend. */
    data class UserBanned(
        override val errorMessage: String,
    ) : ClientUserError

    /** The profile of the client user could not be created or updated; [cause] is the profile error. */
    data class ProfileError(
        val cause: ProfileUpdateError,
        override val errorMessage: String = cause.errorMessage,
    ) : ClientUserError

    /** Any other error. */
    data class Other(
        override val errorMessage: String,
    ) : ClientUserError
}

/**
 * Errors that may be returned by [ConnectionRepository.refreshEntitlements] inside an
 * [OctopusResult.Failure.InvalidArguments].
 *
 * Of these, only [UserBanned] carries a user-facing string (BE-provided ban reason, suitable
 * for direct display). The other cases are programmatic — the host app should map them to its
 * own UI strings.
 */
sealed interface RefreshEntitlementsError : ServerError {
    /**
     * No token provider has been registered through [ConnectionRepository.connectUser],
     * or that provider returned an empty token. A restored session still needs the host
     * to register its token provider before refreshing entitlements.
     */
    data class NoClientTokenProvider(
        override val errorMessage: String = "No client token provider registered",
    ) : RefreshEntitlementsError

    /** No connected (non-guest) user. The caller must call [ConnectionRepository.connectUser] first. */
    data class UserNotConnected(
        override val errorMessage: String = "No connected user",
    ) : RefreshEntitlementsError

    /** No network connection available. */
    data class NoNetwork(
        override val errorMessage: String = "No network",
    ) : RefreshEntitlementsError

    /**
     * The backend detected the user has been banned. The associated message is BE-provided and
     * appropriate for direct display to the user.
     */
    data class UserBanned(
        override val errorMessage: String,
    ) : RefreshEntitlementsError

    /**
     * The backend returned a generic error. [throwable] is the underlying exception when one
     * is available (e.g. a gRPC `StatusException` for non-network status errors), `null` when
     * the failure was reported as a typed protobuf error without an exception.
     */
    data class ServerError(
        override val errorMessage: String,
        val throwable: Throwable? = null,
    ) : RefreshEntitlementsError
}
