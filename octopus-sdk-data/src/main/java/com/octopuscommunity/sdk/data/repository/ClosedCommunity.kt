package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.sdk.domain.model.CommunityAccessDenial
import com.octopuscommunity.sdk.domain.model.CommunityClosure
import com.octopuscommunity.sdk.domain.model.UserConfig
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.CommunityClosureRepository
import io.grpc.Status
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A community never closed: the default of the repositories built without the closure (tests, previews). */
internal object OpenCommunityClosureRepository : CommunityClosureRepository {
    override val closureFlow: StateFlow<CommunityClosure?> = MutableStateFlow(null)

    override suspend fun handle(closure: CommunityClosure?) = Unit
}

/** The community access while it is closed: none, with the closure message and the kind of closure as the reason. */
internal fun CommunityClosure.toUserConfig() =
    UserConfig(
        canAccessCommunity = false,
        accessDeniedMessage = message.ifEmpty { null },
        accessDenial =
            when (kind) {
                CommunityClosure.Kind.Closed -> CommunityAccessDenial.CommunityClosed
                CommunityClosure.Kind.TemporarilyDisabled -> CommunityAccessDenial.CommunityTemporarilyDisabled
            },
    )

/** The failure of a call not sent because the community is closed, as the blocked call itself would fail. */
internal fun closedCommunityFailure() =
    OctopusResult.Failure.StatusError(
        code = Status.Code.FAILED_PRECONDITION.value(),
        description = "Calls are blocked while the community is closed",
        exception = IllegalStateException("Calls are blocked while the community is closed"),
    )
