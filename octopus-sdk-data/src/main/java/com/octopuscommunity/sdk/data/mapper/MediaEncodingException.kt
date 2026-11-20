package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.ServerError
import com.octopuscommunity.sdk.domain.repository.PostCreationError
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateError

/**
 * Raised by [MediaMapper.encodeToByteString] when an image cannot be decoded, resized or compressed
 * into the bytes the backend expects.
 *
 * Before internal change that failure had no representation at all: the encoder returned `null`, the proto
 * builder left `bytes` unset, and the request went out as a post (or a comment, a reply, a profile
 * picture) whose image had silently disappeared — a success for the host, a missing image for the
 * member, nothing anywhere saying why. This type carries the failure up to the repository, which
 * turns it into the typed error that path already publishes, so the public contract stays
 * [com.octopuscommunity.sdk.domain.network.OctopusResult] and no host ever catches an exception.
 *
 * @property resourceDescription The image that failed, in a form safe to write to a log.
 * @property reason What went wrong, which decides the typed error the member ends up seeing.
 * @property imageIndex The image that failed among the post's photos, counted from 0; `null` outside
 *   them (see [namingImage]).
 */
internal class MediaEncodingException(
    val resourceDescription: String,
    val reason: Reason,
    cause: Throwable? = null,
    val imageIndex: Int? = null,
) : Exception("Failed to encode image for upload: $resourceDescription ($reason)", cause) {
    internal enum class Reason {
        /** The loader returned no bitmap: unsupported, truncated or corrupted image. */
        UNREADABLE_IMAGE,

        /**
         * Preparing the image failed: an I/O error, a revoked URI permission, an
         * [OutOfMemoryError] during request creation, loading, decoding or compression, or a `Bitmap.compress`
         * that returned `false` — or an empty buffer — instead of throwing.
         */
        ENCODING_FAILED,
    }

    /**
     * What the member reads.
     *
     * Deliberately not [message]: that one names the resource and the underlying cause, which
     * belong in logcat and not in an error banner. It is English-only — the localized strings of
     * `octopus-sdk-ui` come from the shared Sheet and the
     * screens on this path render the server's message verbatim, so there is no localized slot to
     * fill without a Sheet round-trip.
     */
    val userMessage: String
        get() =
            when (reason) {
                Reason.UNREADABLE_IMAGE -> "This image could not be read. Please select another one."
                Reason.ENCODING_FAILED -> "This image could not be prepared. Please try again."
            }

    /**
     * The post-creation flavour of "this image could not be encoded".
     *
     * Both variants already exist in the sealed hierarchy, and a host that handles `FileError`
     * handles these too — so surfacing this failure adds no public type and breaks no exhaustive
     * `when`.
     */
    fun toPostFileError(): PostCreationError.FileError =
        when (reason) {
            Reason.UNREADABLE_IMAGE -> PostCreationError.FileError.BadFileFormat(userMessage, imageIndex)
            Reason.ENCODING_FAILED -> PostCreationError.FileError.UploadIssue(userMessage, imageIndex)
        }

    /** The comment-creation flavour of [toPostFileError]. */
    fun toCommentFileError(): Comment.CreationError.FileError =
        when (reason) {
            Reason.UNREADABLE_IMAGE -> Comment.CreationError.FileError.BadFileFormat(userMessage)
            Reason.ENCODING_FAILED -> Comment.CreationError.FileError.UploadIssue(userMessage)
        }

    /** The reply-creation flavour of [toPostFileError]. */
    fun toReplyFileError(): Comment.Reply.CreationError.FileError =
        when (reason) {
            Reason.UNREADABLE_IMAGE ->
                Comment.Reply.CreationError.FileError
                    .BadFileFormat(userMessage)

            Reason.ENCODING_FAILED ->
                Comment.Reply.CreationError.FileError
                    .UploadIssue(userMessage)
        }

    /**
     * The profile flavour of [toPostFileError].
     *
     * [ProfileUpdateError.PictureError] has a single variant, so there is no choice to make here.
     */
    fun toProfilePictureError(): ProfileUpdateError.PictureError = ProfileUpdateError.PictureError.Unknown(userMessage)
}

/**
 * Turns a [MediaEncodingException] escaping [block] into the typed failure of that path.
 *
 * It wraps the whole call rather than the encode alone because the encode happens deep inside the
 * mapping to proto (`MediaMapper.toProto` runs inside `rwOctoObject { … }`, or just before it for
 * posts), so this is the first place that knows which error hierarchy the caller publishes.
 * Anything else [block] throws is left alone.
 *
 * @param onEncodingFailure The path's own conversion, e.g.
 *   [MediaEncodingException.toPostFileError].
 */
internal inline fun <Data, Error : ServerError> catchingMediaEncoding(
    onEncodingFailure: (MediaEncodingException) -> Error,
    block: () -> OctopusResult<Data, Error>,
): OctopusResult<Data, Error> =
    try {
        block()
    } catch (exception: MediaEncodingException) {
        OctopusResult.Failure.InvalidArguments(onEncodingFailure(exception))
    }

/**
 * [encode] of the post's photo at [index]: when it fails, the failure names that photo, so the
 * composer can point at it among the others.
 */
internal inline fun <T> namingImage(
    index: Int,
    encode: () -> T,
): T =
    try {
        encode()
    } catch (exception: MediaEncodingException) {
        throw MediaEncodingException(
            resourceDescription = exception.resourceDescription,
            reason = exception.reason,
            cause = exception.cause,
            imageIndex = index,
        )
    }
