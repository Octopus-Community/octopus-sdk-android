package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.model.CommunityClosure
import com.octopuscommunity.sdk.domain.repository.CommunityClosureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory [CommunityClosureRepository]: open by default, [handle] sets the closure as is. */
open class FakeCommunityClosureRepository(
    closure: CommunityClosure? = null,
) : CommunityClosureRepository {
    private val closure = MutableStateFlow(closure)
    override val closureFlow: StateFlow<CommunityClosure?> = this.closure

    override suspend fun handle(closure: CommunityClosure?) {
        this.closure.value = closure
    }
}
