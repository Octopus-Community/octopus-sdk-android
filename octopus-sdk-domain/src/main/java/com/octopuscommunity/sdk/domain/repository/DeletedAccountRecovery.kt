package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.ServerError

/**
 * Optional capability of a [ConnectionRepository]: bringing back the user of a deleted account. Repositories that
 * cannot do it need not implement this capability.
 */
interface DeletedAccountRecovery {
    /**
     * Gives up the session of a deleted account, then connects the client user again when it and its token
     * provider are known, else a guest. A failure means no session could be connected: nothing changed for the
     * user, who can try again.
     */
    suspend fun recoverFromDeletedAccount(): OctopusResult<ConnectionRepository.ConnectionResult, ServerError>
}

/**
 * [DeletedAccountRecovery.recoverFromDeletedAccount] when the repository has this capability, else a plain
 * connection check — including for existing, precompiled implementations.
 */
suspend fun ConnectionRepository.recoverFromDeletedAccount(): OctopusResult<ConnectionRepository.ConnectionResult, ServerError> =
    (this as? DeletedAccountRecovery)?.recoverFromDeletedAccount() ?: checkConnection()
