package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.GrpcModelProto
import com.octopuscommunity.OctoObjectServiceProto.CreateImageUploadsRequest
import com.octopuscommunity.OctoObjectServiceProto.CreateImageUploadsResponse
import com.octopuscommunity.createImageUploadsRequest
import com.octopuscommunity.image
import com.octopuscommunity.sdk.data.mapper.EncodedImage
import com.octopuscommunity.sdk.data.remote.upload.ImageUploadClient
import com.octopuscommunity.sdk.data.remote.upload.ImageUploadHttpException
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.IOException

/**
 * The client object post an upload is for: [clientObjectId], and the [clientToken] the host signed for
 * the post — the very one its creation sends — or `null` for an unsigned post.
 */
internal data class BridgeUpload(
    val clientObjectId: String,
    val clientToken: String?,
)

/**
 * Uploads the photos of a post to storage before the post is created:
 * 1. asks the backend for one upload per photo, with its size **after** compression, which the
 *    signed URL enforces;
 * 2. puts each photo to its URL, [maxParallelUploads] at a time;
 * 3. hands back one `Image(uploadId)` per photo, in display order, for the post to reference.
 *
 * All or nothing: the first failure fails the whole upload, and nothing is kept for a new attempt
 * (the URLs expire). A failed put — unreachable storage, or an answer outside 2xx — and a count of
 * uploads that does not match the photos are thrown as a [StatusException], so the send fails with
 * the usual network or server error; a refusal of the uploads is handed back with the backend's
 * errors, which may name the photo at fault.
 */
internal class PostImageUploader(
    private val createUploads: suspend (CreateImageUploadsRequest) -> CreateImageUploadsResponse,
    private val client: ImageUploadClient,
    private val maxParallelUploads: Int = MAX_PARALLEL_UPLOADS,
) {
    sealed interface Result {
        /** Every photo arrived: [images] reference them by upload id, in display order. */
        data class Uploaded(
            val images: List<GrpcModelProto.Image>,
        ) : Result

        /** The backend refused the uploads, for the reasons in [errors]; nothing was put. */
        data class Refused(
            val errors: List<CreateImageUploadsResponse.Error>,
        ) : Result
    }

    /**
     * Uploads [images], for a member's post, or for the client object post [bridge] names: the
     * backend files those under the client object, where the post creation looks for them.
     */
    suspend fun upload(
        images: List<EncodedImage>,
        bridge: BridgeUpload? = null,
    ): Result {
        if (images.isEmpty()) return Result.Uploaded(emptyList())
        val response =
            createUploads(
                createImageUploadsRequest {
                    this.images +=
                        images.map { image ->
                            CreateImageUploadsRequest.ImageUploadRequest
                                .newBuilder()
                                .setSizeBytes(image.bytes.size().toLong())
                                .build()
                        }
                    bridge?.let { bridge ->
                        this.bridge =
                            CreateImageUploadsRequest.BridgeUpload
                                .newBuilder()
                                .setClientObjectId(bridge.clientObjectId)
                                .apply { bridge.clientToken?.let { setClientToken(it) } }
                                .build()
                    }
                },
            )
        if (!response.hasSuccess()) {
            // A refusal names its reasons; one without any, or no answer at all, is a server fault.
            if (response.hasFail() && response.fail.errorsCount > 0) return Result.Refused(response.fail.errorsList)
            throw StatusException(Status.INTERNAL.withDescription("Image uploads refused without a reason"))
        }
        val uploads = response.success.uploadsList
        if (uploads.size != images.size) {
            throw StatusException(
                Status.INTERNAL.withDescription("${uploads.size} uploads for ${images.size} images"),
            )
        }
        val permits = Semaphore(maxParallelUploads)
        coroutineScope {
            images
                .zip(uploads)
                .map { (image, upload) ->
                    async { permits.withPermit { put(image = image, url = upload.uploadUrl) } }
                }.awaitAll()
        }
        return Result.Uploaded(
            images.zip(uploads) { image, upload ->
                image {
                    this.uploadId = upload.uploadId
                    this.name = image.name
                    this.contentType = image.contentType
                    this.isOptimized = image.isOptimized
                }
            },
        )
    }

    private suspend fun put(
        image: EncodedImage,
        url: String,
    ) {
        try {
            client.put(
                url = url,
                body = image.bytes,
                contentType = image.uploadContentType,
            )
        } catch (exception: IOException) {
            throw StatusException(Status.UNAVAILABLE.withDescription("Image upload failed").withCause(exception))
        } catch (exception: ImageUploadHttpException) {
            throw StatusException(Status.INTERNAL.withDescription(exception.message).withCause(exception))
        }
    }

    private companion object {
        /** How many photos are put at the same time. */
        const val MAX_PARALLEL_UPLOADS = 3
    }
}
