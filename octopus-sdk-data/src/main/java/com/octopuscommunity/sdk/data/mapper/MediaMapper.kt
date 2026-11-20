package com.octopuscommunity.sdk.data.mapper

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.request.allowHardware
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.toBitmap
import com.google.protobuf.ByteString
import com.octopuscommunity.GrpcModelProto
import com.octopuscommunity.image
import com.octopuscommunity.sdk.data.local.data.ImageData
import com.octopuscommunity.sdk.data.local.entity.MediaEntity
import com.octopuscommunity.sdk.data.local.entity.MediasEntity
import com.octopuscommunity.sdk.data.utils.ImageRequestBuilder
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.domain.model.Medias
import com.octopuscommunity.sdk.domain.model.Resource
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ClientPostError
import com.octopuscommunity.videoOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.time.Duration.Companion.milliseconds

class MediaMapper(
    private val imageLoader: ImageLoader,
    private val imageRequestBuilder: ImageRequestBuilder,
) {
    fun MediaEntity.ResourceEntity.toDomain() =
        when (type) {
            MediaEntity.ResourceEntity.Type.LOCAL -> Resource.Local(uri = value.toUri())
            MediaEntity.ResourceEntity.Type.REMOTE -> Resource.Remote(url = value)
        }

    fun Resource.toEntity() =
        when (this) {
            is Resource.Local ->
                MediaEntity.ResourceEntity(
                    type = MediaEntity.ResourceEntity.Type.LOCAL,
                    value = uri.toString(),
                )

            is Resource.Remote ->
                MediaEntity.ResourceEntity(
                    type = MediaEntity.ResourceEntity.Type.REMOTE,
                    value = url,
                )
        }

    fun ImageData.toDomain() =
        when (type) {
            ImageData.Type.LOCAL -> Resource.Local(uri = value.toUri())
            ImageData.Type.REMOTE -> Resource.Remote(url = value)
        }

    fun Resource.toData() =
        when (this) {
            is Resource.Local -> ImageData(type = ImageData.Type.LOCAL, value = uri.toString())
            is Resource.Remote -> ImageData(type = ImageData.Type.REMOTE, value = url)
        }

    fun Media.Image.toEntity() =
        MediaEntity.ImageEntity(
            source = source.toEntity(),
            width = dimensions?.width,
            height = dimensions?.height,
            size = fileSize,
            name = name,
            contentType = contentType,
        )

    fun Media.Video.toEntity() =
        MediaEntity.VideoEntity(
            id = id,
            source = source.toEntity(),
            width = dimensions?.width,
            height = dimensions?.height,
            size = fileSize,
            thumbnailUrl = thumbnailUrl,
            duration = duration?.inWholeMilliseconds,
        )

    fun MediaEntity.ImageEntity.toDomain() =
        Media.Image(
            source = source.toDomain(),
            dimensions =
                if (width != null && height != null) {
                    android.util.Size(
                        width,
                        height,
                    )
                } else {
                    null
                },
            fileSize = size,
            name = name,
            contentType = contentType,
        )

    fun MediaEntity.VideoEntity.toDomain() =
        Media.Video(
            id = id,
            source = source.toDomain(),
            dimensions =
                if (width != null && height != null) {
                    android.util.Size(
                        width,
                        height,
                    )
                } else {
                    null
                },
            fileSize = size,
            thumbnailUrl = thumbnailUrl,
            duration = duration?.milliseconds,
        )

    fun GrpcModelProto.Image.toEntity() =
        MediaEntity.ImageEntity(
            source = MediaEntity.ResourceEntity(url = url),
            name = name,
            contentType = contentType,
            width = if (hasWidth()) width else null,
            height = if (hasHeight()) height else null,
            size = null,
        )

    fun GrpcModelProto.MediaContent.toEntity() =
        with(data.videoData) {
            MediaEntity.VideoEntity(
                id = id,
                source = MediaEntity.ResourceEntity(url = url),
                width = if (hasWidth()) width else null,
                height = if (hasHeight()) height else null,
                size = null,
                thumbnailUrl = if (hasThumbnailUrl()) thumbnailUrl.ifEmpty { null } else null,
                duration = if (hasDurationMs()) durationMs else null,
            )
        }

    fun Medias.toEntity() =
        MediasEntity(
            images = images.map { it.toEntity() },
            video = video?.toEntity(),
        )

    fun MediasEntity.toDomain() =
        Medias(
            images = images.map { it.toDomain() },
            video = video?.toDomain(),
        )

    fun GrpcModelProto.StrictMedia.toEntity() =
        MediasEntity(
            images = imagesList.mapNotNull { it?.toEntity() },
            video = videoOrNull?.toEntity(),
        )

    /**
     * Encodes an image for upload.
     *
     * Unlike [Resource.toProto], this **fails loudly**: an image that cannot be decoded, resized or
     * compressed raises [MediaEncodingException] instead of producing a proto with no `bytes` at
     * all. Every caller is a user-authored content path — a post, a comment, a reply — where
     * dropping the picture and reporting success is the bug this replaces (internal change).
     *
     * @throws MediaEncodingException if the image could not be encoded.
     */
    suspend fun Media.Image.toProto(
        imageMaxSize: Int? = Media.Image.UPLOAD_MAX_DIMENSION,
        imageQuality: Int = Media.Image.UPLOAD_QUALITY,
    ) = this@toProto.source.toProto(
        maxSize = imageMaxSize,
        quality = imageQuality,
        name = this@toProto.name,
        contentType = this@toProto.contentType,
        localDownload = true,
        failOnEncodingError = true,
    )

    /**
     * Encodes a resource into the image proto.
     *
     * @param failOnEncodingError When `true`, an image that cannot be encoded raises
     *   [MediaEncodingException] rather than yielding a proto with no bytes. It carries **no
     *   default**: tolerance is the behaviour that hid internal change for as long as it did, so every call
     *   site states which one it wants. `true` is for the member-authored paths; `false` is for the
     *   host-driven bridge paths (`ClientPost`, `OctopusItem`), whose images are supplied by the
     *   integrating app for content it already owns and where failing the whole call over one
     *   unreachable thumbnail would be a worse regression than the missing thumbnail. The failure
     *   is logged either way.
     *
     * @throws MediaEncodingException if [failOnEncodingError] is `true` and the image could not be
     *   encoded.
     */
    suspend fun Resource.toProto(
        maxSize: Int? = Media.Image.UPLOAD_MAX_DIMENSION,
        quality: Int = Media.Image.UPLOAD_QUALITY,
        name: String = "${System.currentTimeMillis()}",
        contentType: String = "image/*",
        localDownload: Boolean,
        failOnEncodingError: Boolean,
    ) = image {
        this.name = name
        this.contentType = contentType

        // `Resource.Remote` without `localDownload` hands the backend a URL to fetch itself, so
        // there is nothing to encode and nothing that can fail. Every other branch encodes.
        val encodes = this@toProto is Resource.Local || localDownload
        when {
            !encodes -> this@image.url = (this@toProto as Resource.Remote).url

            failOnEncodingError -> {
                val (bytes, isCompressed) =
                    this@toProto.encodeToByteString(maxSize = maxSize, quality = quality)
                this@image.bytes = bytes
                this@image.isOptimized = isCompressed
            }

            else -> {
                this@toProto
                    .toByteString(maxSize = maxSize, quality = quality)
                    ?.let { (bytes, isCompressed) ->
                        this@image.bytes = bytes
                        this@image.isOptimized = isCompressed
                    }
            }
        }
    }

    /**
     * Compresses this image for an upload, failing loudly as [Media.Image.toProto] does.
     *
     * @throws MediaEncodingException if the image could not be encoded.
     */
    internal suspend fun Media.Image.toEncodedImage(
        imageMaxSize: Int? = Media.Image.UPLOAD_MAX_DIMENSION,
        imageQuality: Int = Media.Image.UPLOAD_QUALITY,
    ): EncodedImage {
        val (bytes, isCompressed) = source.encodeToByteString(maxSize = imageMaxSize, quality = imageQuality)
        return EncodedImage(
            bytes = bytes,
            isOptimized = isCompressed,
            name = name,
            contentType = contentType,
            uploadContentType = if (isCompressed) "image/jpeg" else "image/png",
        )
    }

    /**
     * The bytes of this host-supplied local image, at [index] in its client post, as they are —
     * neither resized nor re-encoded — for an upload, declared optimized; or the file error that
     * refuses the post when they cannot be read (see [readRawImageBytes]).
     */
    internal suspend fun Resource.Local.toRawEncodedImage(index: Int): OctopusResult<EncodedImage, ClientPostError> =
        withContext(Dispatchers.IO) {
            val description = describeForLog()
            val bytes =
                when (
                    val read =
                        readRawImageBytes(
                            index = index,
                            open = { imageRequestBuilder.openInputStream(uri) },
                            log = { message, cause -> Log.w(LOG_TAG, "$message: $description", cause) },
                        )
                ) {
                    is RawImageBytes.Read -> read.bytes
                    is RawImageBytes.Refused -> return@withContext OctopusResult.Failure.InvalidArguments(listOf(read.error))
                }
            OctopusResult.Success(
                EncodedImage(
                    bytes = ByteString.copyFrom(bytes),
                    isOptimized = true,
                    name = "${System.currentTimeMillis()}",
                    contentType = "image/*",
                    uploadContentType = sniffImageMimeType(bytes) ?: imageRequestBuilder.mimeType(uri) ?: "application/octet-stream",
                ),
            )
        }

    suspend fun Resource.toFingerprint() =
        when (this@toFingerprint) {
            is Resource.Remote -> url
            is Resource.Local -> toBase64String()
        }

    suspend fun Resource.Local.toBase64String() =
        withContext(Dispatchers.IO) {
            toByteString()?.let { (bytes, _) ->
                Base64.encodeToString(
                    bytes.toByteArray(),
                    Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING,
                )
            }
        }

    /**
     * Tolerant encoder: returns `null` when the image cannot be encoded.
     *
     * Kept for the paths where a missing image must not fail the call — see
     * [Resource.toProto]'s `failOnEncodingError`, [Resource.Local.toBase64String] and
     * [Resource.toFingerprint]. Unlike the pre-internal change version it no longer swallows the failure in
     * silence: [encodeToByteString] logs it before this function turns it into `null`.
     */
    suspend fun Resource.toByteString(
        maxSize: Int? = Media.Image.UPLOAD_MAX_DIMENSION,
        quality: Int = Media.Image.UPLOAD_QUALITY,
    ): Pair<ByteString, Boolean>? =
        try {
            encodeToByteString(maxSize = maxSize, quality = quality)
        } catch (_: MediaEncodingException) {
            null
        }

    /**
     * Decodes, resizes and compresses this resource into the bytes the backend expects, paired with
     * whether the result is a lossy (compressed) encoding.
     *
     * Failure modes, all of them reported rather than swallowed:
     * - the loader threw (unreadable URI, revoked permission, no network for a remote source);
     * - the loader returned no image at all (`result.image == null`, which is how Coil reports a
     *   decode failure without throwing) — the half of internal change that took the `?: return null` path
     *   rather than the `catch`;
     * - the bitmap could not be compressed — `compress` threw, ran out of memory, or simply
     *   returned `false`, which is how it reports a failure for a format/quality combination it
     *   could not honour and which used to yield an empty `ByteString` reported as a success.
     *
     * It is `internal`: it raises [MediaEncodingException], itself an internal type, so a consumer
     * outside this module could neither name nor catch what it throws.
     *
     * @throws MediaEncodingException in every one of those cases, always after logging.
     */
    internal suspend fun Resource.encodeToByteString(
        maxSize: Int? = Media.Image.UPLOAD_MAX_DIMENSION,
        quality: Int = Media.Image.UPLOAD_QUALITY,
    ): Pair<ByteString, Boolean> =
        withContext(Dispatchers.IO) {
            val description = describeForLog()
            try {
                val request =
                    imageRequestBuilder
                        .create(
                            data =
                                when (this@encodeToByteString) {
                                    is Resource.Local -> uri
                                    is Resource.Remote -> url
                                },
                            enableCache = false,
                            crossFade = false,
                        ).size(if (maxSize != null) Size(maxSize, maxSize) else Size.ORIGINAL)
                        .scale(Scale.FIT) // Maintains aspect ratio and prevents upscaling
                        .precision(Precision.INEXACT)
                        .allowHardware(false) // Required to access bitmap
                        .build()

                val result = imageLoader.execute(request)
                val bitmap =
                    result.image?.toBitmap()
                        ?: throw MediaEncodingException(
                            resourceDescription = description,
                            reason = MediaEncodingException.Reason.UNREADABLE_IMAGE,
                        )

                val (format, isCompressed) =
                    if (bitmap.hasAlpha()) {
                        Bitmap.CompressFormat.PNG to false
                    } else {
                        Bitmap.CompressFormat.JPEG to true
                    }

                // The bitmap is released whatever happens next — `compress` can throw, and on the
                // throwing path the pixels used to stay allocated until the GC noticed, which is
                // exactly when the heap is already under pressure.
                val encoded =
                    try {
                        ByteArrayOutputStream().use { outputStream ->
                            val compressed = bitmap.compress(format, quality, outputStream)
                            compressedBytesOrThrow(
                                compressed = compressed,
                                bytes = outputStream.toByteArray(),
                                resourceDescription = description,
                            )
                        }
                    } finally {
                        if (!bitmap.isRecycled) {
                            bitmap.recycle()
                        }
                    }

                encoded to isCompressed
            } catch (error: OutOfMemoryError) {
                // Covers request creation, loading, decoding and compression: preparing a large
                // picture can exhaust the heap at any of these stages. Once compression starts,
                // its finally block releases the bitmap. Every other `Error` keeps propagating.
                val failure =
                    MediaEncodingException(
                        resourceDescription = description,
                        reason = MediaEncodingException.Reason.ENCODING_FAILED,
                        cause = error,
                    )
                Log.w(LOG_TAG, failure.message, failure.cause)
                throw failure
            } catch (exception: Exception) {
                // A cancelled coroutine is not an encoding failure: rethrowing it untouched keeps
                // structured concurrency intact, instead of reporting "the image is broken" to a user
                // who simply navigated away mid-upload.
                if (exception is CancellationException) throw exception

                val failure =
                    exception as? MediaEncodingException
                        ?: MediaEncodingException(
                            resourceDescription = description,
                            reason = MediaEncodingException.Reason.ENCODING_FAILED,
                            cause = exception,
                        )
                Log.w(LOG_TAG, failure.message, failure.cause)
                throw failure
            }
        }

    /**
     * A log-safe description of the image that failed.
     *
     * Local pictures are identified by their URI scheme and authority only — the path segment of a
     * `content://` URI can carry a filename the member chose, and logcat is world-readable to
     * anyone with the device plugged in. A remote source is a URL the backend already holds, so it
     * is logged whole: without it there is nothing actionable to look at.
     */
    private fun Resource.describeForLog() =
        when (this) {
            is Resource.Local -> "local ${uri.scheme}://${uri.authority.orEmpty()}/…"
            is Resource.Remote -> "remote $url"
        }

    private companion object {
        const val LOG_TAG = "OctopusMedia"
    }
}

/**
 * Turns the outcome of [Bitmap.compress] into the bytes to upload, or fails loudly.
 *
 * `compress` does not throw when it cannot honour the requested format and quality: it returns
 * `false` and leaves the stream as it found it. That return value was ignored until internal change, so the
 * empty buffer travelled on as a perfectly successful encoding and the picture disappeared from the
 * published content — the same silent drop internal change fixed everywhere else. An empty buffer is treated
 * the same way even when `compress` claims success, since there is no image in it either way.
 *
 * Extracted from [MediaMapper.encodeToByteString] so this decision can be tested without a
 * `Bitmap`, which has no JVM implementation.
 *
 * @throws MediaEncodingException with [MediaEncodingException.Reason.ENCODING_FAILED] in both
 *   cases.
 */
internal fun compressedBytesOrThrow(
    compressed: Boolean,
    bytes: ByteArray,
    resourceDescription: String,
): ByteString {
    if (!compressed || bytes.isEmpty()) {
        throw MediaEncodingException(
            resourceDescription = resourceDescription,
            reason = MediaEncodingException.Reason.ENCODING_FAILED,
        )
    }
    return ByteString.copyFrom(bytes)
}

/** A host's local image, read for the upload of a client post. */
internal sealed interface RawImageBytes {
    /** The image's [bytes], as the host supplied them. */
    class Read(
        val bytes: ByteArray,
    ) : RawImageBytes

    /** The image could not be read: [error] refuses the client post, naming the image. */
    data class Refused(
        val error: ClientPostError.FileError,
    ) : RawImageBytes
}

/**
 * The bytes [open] gives, as they are, for the host image at [index] of a client post, or the file
 * error that refuses the post when there are none or reading them fails in any way: an unreadable or
 * revoked URI, a misbehaving provider, an image too big for memory. A bridge post is created once,
 * so a photo left out could never be added afterwards: the host gets the error instead, and the
 * failure is logged through [log]. A cancellation goes through.
 *
 * @param index The image's index in [com.octopuscommunity.sdk.domain.model.ClientPost.images],
 *   counted from 0, which the error message names.
 */
internal fun readRawImageBytes(
    index: Int,
    open: () -> InputStream?,
    log: (message: String, cause: Throwable?) -> Unit,
): RawImageBytes {
    val bytes =
        try {
            open()?.use { it.readBytes() }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            log("Cannot read the local image", exception)
            return RawImageBytes.Refused(
                ClientPostError.FileError.Unknown(
                    errorMessage =
                        "The local image at index $index cannot be read: " +
                            (exception.message ?: exception::class.java.simpleName),
                ),
            )
        } catch (error: OutOfMemoryError) {
            log("Local image too big to read", error)
            return RawImageBytes.Refused(
                ClientPostError.FileError.FileSizeTooBig(
                    errorMessage = "The local image at index $index is too large to be held in memory.",
                ),
            )
        }
    if (bytes == null || bytes.isEmpty()) {
        log(if (bytes == null) "No local image" else "Empty local image", null)
        return RawImageBytes.Refused(
            ClientPostError.FileError.EmptyFile(
                errorMessage = "The local image at index $index is ${if (bytes == null) "missing" else "empty"}.",
            ),
        )
    }
    return RawImageBytes.Read(bytes)
}
