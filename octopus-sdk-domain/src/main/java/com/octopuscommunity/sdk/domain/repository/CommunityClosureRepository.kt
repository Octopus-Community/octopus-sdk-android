package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.CommunityClosure
import kotlinx.coroutines.flow.StateFlow

/**
 * Whether Octopus closed the community, as the last config said.
 *
 * While it is closed, every call to the server is blocked, until [CommunityClosure.nextCheckAtMillis] comes and the
 * config is checked again. Closing purges what was cached.
 */
interface CommunityClosureRepository {
    /** The closure of the community, `null` while it is open. */
    val closureFlow: StateFlow<CommunityClosure?>

    /** Whether the community is closed. */
    val isClosed: Boolean get() = closureFlow.value != null

    /** Whether the community is closed and the date to check it again has come. */
    fun isCheckDue(nowMillis: Long = System.currentTimeMillis()): Boolean =
        closureFlow.value?.let { nowMillis >= it.nextCheckAtMillis } ?: false

    /**
     * Applies what a config just received says: [closure] closes the community (or extends its closure), `null`
     * reopens it.
     */
    suspend fun handle(closure: CommunityClosure?)

    /** Runs again the purge of a closure that did not end, cut off by a kill or failed. Called once per launch. */
    suspend fun resumeInterruptedPurge() {}
}
