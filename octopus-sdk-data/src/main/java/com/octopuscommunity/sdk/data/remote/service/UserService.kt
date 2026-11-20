package com.octopuscommunity.sdk.data.remote.service

import com.octopuscommunity.GrpcModelProto
import com.octopuscommunity.UserServiceGrpcKt.UserServiceCoroutineStub
import com.octopuscommunity.UserServiceProto
import com.octopuscommunity.UserServiceProto.ProfileUpdate
import com.octopuscommunity.blockUserRequest
import com.octopuscommunity.byPassAbTestingRequest
import com.octopuscommunity.canAccessCommunityRequest
import com.octopuscommunity.enteringOctopusRequest
import com.octopuscommunity.followUnfollowTopicRequest
import com.octopuscommunity.getGuestJwtRequest
import com.octopuscommunity.getJwtFromClientSignedTokenRequest
import com.octopuscommunity.getPrivateProfileRequest
import com.octopuscommunity.getPublicProfileRequest
import com.octopuscommunity.reportUserRequest
import com.octopuscommunity.sdk.data.remote.client.OctopusGrpcClient
import com.octopuscommunity.syncFollowTopicsRequest
import com.octopuscommunity.updateProfileRequest

class UserService(
    private val grpcClient: OctopusGrpcClient,
) {
    val serviceStub by lazy {
        UserServiceCoroutineStub(grpcClient.unaryChannel)
    }

    /** Registers what runs when the server rejects the JWT a call of any service was sent with. */
    internal fun onSessionRejected(handler: suspend (rejectedJwt: String) -> Unit) {
        grpcClient.onSessionRejected = handler
    }

    /** Registers what runs when the server reads the access of a session it ended. */
    internal fun onSessionEnded(handler: suspend (endedUserId: String) -> Unit) {
        grpcClient.onSessionEnded = handler
    }

    /** Runs, off this call, what [onSessionEnded] registered, for the session of [endedUserId]. */
    internal fun notifySessionEnded(endedUserId: String) = grpcClient.notifySessionEnded(endedUserId)

    suspend fun getGuestJwt() =
        serviceStub.getGuestJwt(
            getGuestJwtRequest {},
        )

    /** [getGuestJwt], sent without the stored session: the guest is not an upgrade path of that session. */
    suspend fun getGuestJwtWithoutSession() =
        serviceStub.withOption(OctopusGrpcClient.WITHOUT_SESSION, true).getGuestJwt(
            getGuestJwtRequest {},
        )

    suspend fun getClientJwt(clientUserToken: String) =
        serviceStub.getJwtFromClientSignedToken(
            getJwtFromClientSignedTokenRequest {
                this.clientToken = clientUserToken
            },
        )

    /** [getClientJwt], sent without the stored session: the account it gives is not tied to that session. */
    suspend fun getClientJwtWithoutSession(clientUserToken: String) =
        serviceStub.withOption(OctopusGrpcClient.WITHOUT_SESSION, true).getJwtFromClientSignedToken(
            getJwtFromClientSignedTokenRequest {
                this.clientToken = clientUserToken
            },
        )

    suspend fun canAccessCommunity() =
        serviceStub.canAccessCommunity(
            canAccessCommunityRequest {},
        )

    suspend fun bypassABTesting(
        userId: String,
        giveCommunityAccess: Boolean,
    ) = serviceStub.byPassAbTesting(
        byPassAbTestingRequest {
            this.userId = userId
            this.giveCommunityAccess = giveCommunityAccess
        },
    )

    suspend fun enteringOctopus(fetchGamificationToasts: Boolean = true) =
        serviceStub.enteringOctopus(
            enteringOctopusRequest {
                this.fetchGamificationToasts = fetchGamificationToasts
            },
        )

    suspend fun getPublicProfile(
        userId: String,
        fetchGamification: Boolean = true,
        fetchTotalMessages: Boolean = true,
    ) = serviceStub.getPublicProfile(
        getPublicProfileRequest {
            this.userId = userId
            this.fetchGamification = fetchGamification
            this.fetchTotalMessages = fetchTotalMessages
        },
    )

    // Lookup by the host's own client user id via the GetPublicProfileRequest.user_ref oneof
    // (clientUserId = 4, gRPC contract). May throw FAILED_PRECONDITION when the community does
    // not expose client user ids.
    suspend fun getPublicProfileByClientUserId(
        clientUserId: String,
        fetchGamification: Boolean = true,
        fetchTotalMessages: Boolean = true,
    ) = serviceStub.getPublicProfile(
        getPublicProfileRequest {
            this.clientUserId = clientUserId
            this.fetchGamification = fetchGamification
            this.fetchTotalMessages = fetchTotalMessages
        },
    )

    suspend fun getPrivateProfile(
        userId: String,
        fetchUserBlockList: Boolean = false,
        fetchNotificationsBadge: Boolean = false,
        fetchGamification: Boolean = false,
        fetchTotalMessages: Boolean = false,
    ) = serviceStub.getPrivateProfile(
        getPrivateProfileRequest {
            this.userId = userId
            this.fetchUserBlockList = fetchUserBlockList
            this.fetchNotificationsBadge = fetchNotificationsBadge
            this.fetchGamification = fetchGamification
            this.fetchTotalMessages = fetchTotalMessages
        },
    )

    suspend fun createProfile(
        userId: String,
        profileUpdate: ProfileUpdate,
    ) = serviceStub.createProfile(
        updateProfileRequest {
            this.userId = userId
            this.update = profileUpdate
            this.fetchUserBlockList = true
            this.fetchNotificationsBadge = true
            this.fetchTotalMessages = true
            this.fetchGamification = true
        },
    )

    suspend fun updateProfile(
        userId: String,
        profileUpdate: ProfileUpdate,
        wantNicknameSuggestions: Boolean = false,
    ) = serviceStub.updateProfile(
        updateProfileRequest {
            this.userId = userId
            this.update = profileUpdate
            this.fetchUserBlockList = true
            this.fetchNotificationsBadge = true
            this.fetchTotalMessages = true
            this.fetchGamification = true
            if (wantNicknameSuggestions) {
                this.wantNicknameSuggestions = true
            }
        },
    )

    suspend fun reportUser(
        id: String,
        reasonCodes: List<GrpcModelProto.ReportReasonCode>,
        customReason: String? = null,
    ) = serviceStub.reportUser(
        reportUserRequest {
            this.userId = id
            this.reasonCodes += reasonCodes
            customReason?.let {
                this.customReason = it
            }
        },
    )

    suspend fun blockUser(userId: String) =
        serviceStub.blockUser(
            blockUserRequest {
                this.userId = userId
            },
        )

    suspend fun followGroup(groupId: String) =
        serviceStub.followTopic(
            followUnfollowTopicRequest {
                this.topicId = groupId
            },
        )

    suspend fun unfollowGroup(groupId: String) =
        serviceStub.unfollowTopic(
            followUnfollowTopicRequest {
                this.topicId = groupId
            },
        )

    suspend fun syncFollowTopics(actions: List<UserServiceProto.SyncFollowTopicAction>) =
        serviceStub.syncFollowTopics(
            syncFollowTopicsRequest {
                this.actions += actions
            },
        )
}
