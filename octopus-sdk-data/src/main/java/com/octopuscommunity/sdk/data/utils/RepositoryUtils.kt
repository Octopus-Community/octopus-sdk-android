package com.octopuscommunity.sdk.data.utils

import android.util.Log
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.ServerError
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.CancellationException

inline fun <Result> call(block: () -> Result): OctopusResult<Result, Nothing> = callWithValidation(block)

inline fun <Result, Mapper> call(
    mapper: Mapper,
    block: Mapper.() -> Result,
): OctopusResult<Result, Nothing> = callWithValidation(mapper, block)

inline fun <Result, ValidationError : ServerError, Mapper> callWithValidation(
    mapper: Mapper,
    block: Mapper.() -> Result,
): OctopusResult<Result, ValidationError> = callWithValidation { with(mapper) { block() } }

inline fun <Result, ValidationError : ServerError> callWithValidation(block: () -> Result): OctopusResult<Result, ValidationError> =
    try {
        OctopusResult.Success(block())
    } catch (cancellation: CancellationException) {
        // The caller was cancelled: not an error to log, and the shared fetches tell it from a failure.
        throw cancellation
    } catch (exception: Exception) {
        // A logout invalidates the session: the UNAUTHENTICATED that follows is expected, not an
        // error worth a stack trace.
        if (exception is StatusException && exception.status.code == Status.Code.UNAUTHENTICATED) {
            Log.i("OctopusRepository", "Not authenticated: ${exception.message}")
        } else {
            Log.e("OctopusRepository", "Error + ${exception.message}", exception)
        }
        when (exception) {
            is StatusException ->
                when (exception.status.code) {
                    Status.Code.UNAVAILABLE -> OctopusResult.Failure.NoNetwork
                    Status.Code.NOT_FOUND -> OctopusResult.Failure.ContentUnavailable
                    Status.Code.UNAUTHENTICATED ->
                        OctopusResult.Failure.UserNotAuthenticated(
                            reason = exception.status.description?.ifEmpty { null },
                        )

                    Status.Code.PERMISSION_DENIED ->
                        OctopusResult.Failure.PermissionDenied(
                            reason = exception.status.description?.ifEmpty { null },
                        )

                    else ->
                        OctopusResult.Failure.StatusError(
                            code = exception.status.code.value(),
                            description = exception.status.description?.ifEmpty { null },
                            exception = exception,
                        )
                }

            else -> throw exception
        }
    }
