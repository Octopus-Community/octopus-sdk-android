package com.octopuscommunity.sdk.sample.data.usecase

import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.sample.data.datastore.UserDataStore
import kotlinx.coroutines.CancellationException

internal const val DISCONNECT_FAILURE_MESSAGE =
    "Could not complete sign-out. The SDK disconnect was attempted; local sign-out may not be saved. Sign in explicitly to reconnect."

/**
 * Attempts to remember sign-out before the SDK can emit a guest profile. A local write failure
 * must not prevent the requested SDK disconnect. Returns either failure (SDK failure first if
 * both fail, with the write failure suppressed) while preserving coroutine cancellation.
 */
suspend fun UserDataStore.disconnectFromCommunity(): Result<Unit> {
    val writeFailure =
        try {
            setSignedOut(true)
            null
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            e
        }
    return try {
        OctopusSDK.disconnectUser()
        if (writeFailure == null) Result.success(Unit) else Result.failure(writeFailure)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (e: Exception) {
        if (writeFailure != null && writeFailure !== e) e.addSuppressed(writeFailure)
        Result.failure(e)
    }
}
