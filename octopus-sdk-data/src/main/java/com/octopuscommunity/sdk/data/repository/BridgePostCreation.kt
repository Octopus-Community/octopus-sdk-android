package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.GrpcModelProto
import com.octopuscommunity.OctoObjectServiceProto.CreateImageUploadsResponse
import com.octopuscommunity.OctoObjectServiceProto.GetOrCreateBridgePostResponse
import com.octopuscommunity.sdk.data.mapper.EncodedImage
import com.octopuscommunity.sdk.data.mapper.toPostProto
import com.octopuscommunity.sdk.domain.model.ClientPost
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ClientPostError

/** How the creation of a client object post went. */
internal sealed interface BridgePostCreation {
    /** The backend refused the photo uploads, for the reasons in [errors]; nothing was created. */
    data class Refused(
        val errors: List<CreateImageUploadsResponse.Error>,
    ) : BridgePostCreation

    /** The post creation was sent and answered [response]. */
    data class Answered(
        val response: GetOrCreateBridgePostResponse,
    ) : BridgePostCreation
}

/**
 * Creates [clientPost]: uploads its [localImages] for the client object, under [clientToken] — the
 * token the host signed for the post, or `null` when unsigned — then [create]s the post with the
 * same token, its remote images as URLs and its local ones by upload.
 *
 * @param localImages The local images read for upload, keyed by their index in [ClientPost.images].
 */
internal suspend fun createBridgePostWithUploads(
    clientPost: ClientPost,
    localImages: List<Pair<Int, EncodedImage>>,
    clientToken: String?,
    uploader: PostImageUploader,
    create: suspend (post: GrpcModelProto.Post, clientToken: String?) -> GetOrCreateBridgePostResponse,
): BridgePostCreation {
    val upload =
        uploader.upload(
            images = localImages.map { (_, image) -> image },
            bridge = BridgeUpload(clientObjectId = clientPost.objectId, clientToken = clientToken),
        )
    val uploadedLocalImages =
        when (upload) {
            is PostImageUploader.Result.Uploaded -> localImages.map { (index, _) -> index }.zip(upload.images).toMap()
            is PostImageUploader.Result.Refused -> return BridgePostCreation.Refused(upload.errors)
        }
    return BridgePostCreation.Answered(
        create(clientPost.toPostProto(uploadedLocalImages = uploadedLocalImages), clientToken),
    )
}

/**
 * Runs [block], which holds a client post's local images in memory to hash and upload them,
 * answering an [OutOfMemoryError] — images too large for the device to hold — with a refused file
 * rather than letting it crash the host app.
 */
internal inline fun <T> withLocalImagesInMemory(block: () -> OctopusResult<T, ClientPostError>): OctopusResult<T, ClientPostError> =
    try {
        block()
    } catch (error: OutOfMemoryError) {
        OctopusResult.Failure.InvalidArguments(
            ClientPostError.FileError.FileSizeTooBig(
                errorMessage = "The local images of this client post are too large to be held in memory: ${error.message}",
            ),
        )
    }
