package com.octopuscommunity.sdk.domain.network

import kotlin.reflect.KClass

/** Result of an SDK call: a [Success] carrying data, or a [Failure]. */
sealed interface OctopusResult<out Data, out R : ServerError> {
    /** Successful result carrying [data]. */
    data class Success<Data>(
        val data: Data,
    ) : OctopusResult<Data, Nothing>

    /** Failed result. */
    sealed interface Failure<R : ServerError> : OctopusResult<Nothing, R> {
        /** Failure that carries no typed error: network, access or server status. */
        sealed interface Connection : Failure<Nothing>

        /** No network connection is available. */
        object NoNetwork : Connection

        /** The requested content is not available. */
        object ContentUnavailable : Connection

        /** The call requires an authenticated user. */
        data class UserNotAuthenticated(
            val reason: String? = null,
        ) : Connection

        /** The user is not allowed to perform the call. */
        data class PermissionDenied(
            val reason: String? = null,
        ) : Connection

        /** The server answered with the error status [code]. */
        data class StatusError(
            val code: Int,
            val description: String?,
            val exception: Exception,
        ) : Connection

        /** The server rejected the call with typed [errors]. */
        data class InvalidArguments<R : ServerError>(
            val errors: List<R>,
        ) : Failure<R> {
            /** First of [errors]. */
            val error = errors.first()

            /** Messages of [errors], as a bulleted list when there are several. */
            val errorMessage = errors.joinWithBulletPoints { it.errorMessage }

            constructor(error: R) : this(listOf(error))

            /** Whether [errors] holds an error of type [T]. */
            inline fun <reified T : Any> hasError() = hasError(T::class)

            /** Whether [errors] holds an error of class [error]. */
            fun <T : Any> hasError(error: KClass<T>): Boolean = errors.any { error.isInstance(it) }

            /** Errors of type [R] in [errors]. */
            inline fun <reified R : ServerError> anyError() = errors.filterIsInstance<R>()

            /** Whether any of [errors] matches [predicate]. */
            fun anyError(predicate: (R) -> Boolean) = errors.any(predicate)

            /** Errors of type [R] in [errors]. */
            inline fun <reified R : ServerError> filterErrors() = errors.filterIsInstance<R>()
        }
    }

    /**
     * Returns `true` if this instance represents a successful outcome.
     * In this case [isFailure] returns `false`.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Returns `true` if this instance represents a failed outcome.
     * In this case [isSuccess] returns `false`.
     */
    val isFailure: Boolean get() = this is Failure
}

/** Typed error returned by the server inside [OctopusResult.Failure.InvalidArguments]. */
interface ServerError {
    /** Message describing the error. */
    val errorMessage: String
}

/** A [ServerError] of no known type. */
data class UnknownServerError(
    override val errorMessage: String,
) : ServerError

/** Messages of these errors as a bulleted list, or `null` when the list is empty. */
val List<ServerError>.errorMessage
    get() = ifEmpty { null }?.joinWithBulletPoints { it.errorMessage }

/** Joins the elements with "- " bullets, one per line. A single element gets no bullet. */
fun <T> List<T>.joinWithBulletPoints(transform: ((T) -> CharSequence)? = null) =
    joinToString(
        prefix = if (size > 1) "- " else "",
        separator = "\n- ",
        transform = transform,
    )

/**
 * Transforms the success value of an [OctopusResult] using the provided function.
 * If this is a failure, returns the failure unchanged.
 *
 * @param transform Function to transform the success data
 * @return A new [OctopusResult] with the transformed success value, or the original failure
 */
inline fun <Data, Error : ServerError, NewData> OctopusResult<Data, Error>.mapSuccess(
    transform: (Data) -> NewData,
): OctopusResult<NewData, Error> =
    when (this) {
        is OctopusResult.Success -> OctopusResult.Success(transform(this.data))
        is OctopusResult.Failure -> this
    }

/**
 * Transforms the failure error of an [OctopusResult] using the provided function.
 * If this is a success, returns the success unchanged.
 *
 * @param transform Function to transform the error
 * @return A new [OctopusResult] with the transformed failure, or the original success
 */
inline fun <Data, Error : ServerError, NewError : ServerError> OctopusResult<Data, Error>.mapFailure(
    transform: (OctopusResult.Failure<Error>) -> OctopusResult.Failure<NewError>,
): OctopusResult<Data, NewError> =
    when (this) {
        is OctopusResult.Success -> this
        is OctopusResult.Failure -> transform(this)
    }

/**
 * Transforms the error values inside [com.octopuscommunity.sdk.domain.network.OctopusResult.Failure.InvalidArguments] using the provided function.
 * [com.octopuscommunity.sdk.domain.network.OctopusResult.Failure.Connection] failures are passed through unchanged since they carry no error type.
 * If this is a success, returns the success unchanged.
 *
 * @param transform Function to transform each error value
 * @return A new [OctopusResult] with the transformed errors, or the original success/connection failure
 */
inline fun <Data, Error : ServerError, NewError : ServerError> OctopusResult<Data, Error>.mapErrors(
    transform: (Error) -> NewError,
): OctopusResult<Data, NewError> =
    when (this) {
        is OctopusResult.Success -> this
        is OctopusResult.Failure.InvalidArguments ->
            OctopusResult.Failure.InvalidArguments(
                errors.map(transform),
            )

        is OctopusResult.Failure.Connection -> this
    }

/**
 * When this is an [OctopusResult.Failure.InvalidArguments] holding an error of type [Error], returns
 * the result of [block] for its first error, cast to [Error]: throws [ClassCastException] when the
 * first error is of another type. Otherwise returns this result unchanged.
 */
inline fun <reified Error : ServerError, Data, NewError : ServerError> OctopusResult<Data, NewError>.mapError(
    block: (Error) -> OctopusResult<Data, NewError>,
): OctopusResult<Data, NewError> =
    when {
        this is OctopusResult.Failure.InvalidArguments && hasError<Error>() -> block(error as Error)
        else -> this
    }

/**
 * Executes a side effect when the result is successful.
 * Returns the original result unchanged, allowing for chaining.
 *
 * @param block Function to execute with the success data
 * @return The original [OctopusResult] unchanged
 */
inline fun <Data, Error : ServerError> OctopusResult<Data, Error>.onSuccess(block: (Data) -> Unit): OctopusResult<Data, Error> =
    also {
        if (this is OctopusResult.Success) {
            block(this.data)
        }
    }

/**
 * Executes a side effect when the result is a failure.
 * Returns the original result unchanged, allowing for chaining.
 *
 * @param block Function to execute with the failure
 * @return The original [OctopusResult] unchanged
 */
inline fun <Data, Error : ServerError> OctopusResult<Data, Error>.onFailure(
    block: (OctopusResult.Failure<Error>) -> Unit,
): OctopusResult<Data, Error> =
    also {
        if (this is OctopusResult.Failure) {
            block(this)
        }
    }

/**
 * Executes [block] with the first error when this is an [OctopusResult.Failure.InvalidArguments]
 * holding an error of type [Error]. Returns this result unchanged.
 */
inline fun <reified Data, reified Error : ServerError> OctopusResult<Data, Error>.onError(
    block: (Error) -> Unit,
): OctopusResult<Data, Error> =
    also {
        if (this is OctopusResult.Failure.InvalidArguments && hasError<Error>()) {
            block(error)
        }
    }

/** Returns [transform] applied to the data on success, or `null` on failure. */
inline fun <Data, Error : ServerError, NewData> OctopusResult<Data, Error>.getOrNull(transform: (Data) -> NewData): NewData? =
    when (this) {
        is OctopusResult.Success -> transform(data)
        is OctopusResult.Failure -> null
    }

/** Returns the data on success, or `null` on failure. */
fun <Data, Error : ServerError> OctopusResult<Data, Error>.getOrNull(): Data? = getOrElse { null }

/** Returns the data on success, or the result of [onFailure] on failure. */
inline fun <Data, Error : ServerError> OctopusResult<Data, Error>.getOrElse(onFailure: (OctopusResult.Failure<Error>) -> Data): Data =
    when (this) {
        is OctopusResult.Success -> data
        is OctopusResult.Failure -> onFailure(this)
    }
