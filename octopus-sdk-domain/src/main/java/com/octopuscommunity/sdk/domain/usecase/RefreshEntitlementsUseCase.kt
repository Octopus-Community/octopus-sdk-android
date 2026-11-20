package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.CommentRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import com.octopuscommunity.sdk.domain.repository.GroupRepository
import com.octopuscommunity.sdk.domain.repository.PostRepository
import com.octopuscommunity.sdk.domain.repository.RefreshEntitlementsError
import com.octopuscommunity.sdk.domain.repository.ReplyRepository

/**
 * Refreshes the connected user's entitlements and updates the SDK caches so the next access
 * to a group / post / comment / reply re-resolves its `canAccess` / `canCreateChildren`
 * permissions against the new entitlement set.
 *
 * Short-circuits with [RefreshEntitlementsError.UserNotConnected] when there is no non-guest
 * user — either no user is connected at all, or the SDK is in guest mode
 * ([ConnectionState.Connected] with `isGuest == true`). The underlying refresh call would
 * otherwise hit the connection repository and surface a less specific `NoClientTokenProvider`
 * error (the SSO token provider isn't registered at SDK init for guests). The host app is
 * expected to route to its sign-in flow on this error rather than show a cryptic message.
 *
 * On a successful refresh, every cached item is marked stale via [resetCache] so subsequent
 * fetches go back to the network instead of returning a stale-but-recent cache hit, then the
 * groups list is refetched so observing screens recompose with the new visibility / access
 * decisions returned by the backend.
 */
class RefreshEntitlementsUseCase(
    val connectionRepository: ConnectionRepository,
    val groupRepository: GroupRepository,
    val postRepository: PostRepository,
    val commentRepository: CommentRepository,
    val replyRepository: ReplyRepository
) {
    suspend operator fun invoke(): OctopusResult<Unit, RefreshEntitlementsError> {
        val state = connectionRepository.getConnectionState()
        if (state is ConnectionState.NotConnected ||
            (state is ConnectionState.Connected && state.isGuest)
        ) {
            return OctopusResult.Failure.InvalidArguments(
                listOf(RefreshEntitlementsError.UserNotConnected())
            )
        }
        return connectionRepository.refreshEntitlements().also { result ->
            if (result is OctopusResult.Success) {
                postRepository.resetCache()
                commentRepository.resetCache()
                replyRepository.resetCache()
                groupRepository.fetchGroups()
            }
        }
    }
}
