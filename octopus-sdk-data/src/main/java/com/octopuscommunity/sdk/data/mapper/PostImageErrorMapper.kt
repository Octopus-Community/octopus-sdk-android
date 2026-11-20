package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.OctoObjectServiceProto.CreateImageUploadsResponse
import com.octopuscommunity.OctoObjectServiceProto.PutPostResponse
import com.octopuscommunity.sdk.domain.repository.ClientPostError
import com.octopuscommunity.sdk.domain.repository.PostCreationError

/**
 * A refused photo upload of a post, about the photo at its `fileIndex` when it names one. The
 * client token and bridge refusals only answer a client object post's upload, never a member's.
 */
internal fun CreateImageUploadsResponse.Error.toPostFileError(): PostCreationError.FileError {
    val index = if (hasFileIndex()) fileIndex else null
    return when (detailsCase) {
        CreateImageUploadsResponse.Error.DetailsCase.EMPTYFILE -> PostCreationError.FileError.EmptyFile(message, index)
        CreateImageUploadsResponse.Error.DetailsCase.FILESIZETOOBIG -> PostCreationError.FileError.FileSizeTooBig(message, index)
        else -> PostCreationError.FileError.Unknown(message, index)
    }
}

/**
 * A refused photo upload of a client object post. [ClientPostError] names no photo. The upload is
 * refused for the same token and client object reasons as the post's creation, and gives the same
 * errors as `GetOrCreateBridgePostResponse.Error.toDomain()`, so a host can sign again on
 * [ClientPostError.TokenError.Expired].
 */
internal fun CreateImageUploadsResponse.Error.toClientPostError(): ClientPostError =
    when (detailsCase) {
        CreateImageUploadsResponse.Error.DetailsCase.EMPTYFILE -> ClientPostError.FileError.EmptyFile(message)
        CreateImageUploadsResponse.Error.DetailsCase.FILESIZETOOBIG -> ClientPostError.FileError.FileSizeTooBig(message)
        CreateImageUploadsResponse.Error.DetailsCase.INVALIDCLIENTTOKEN -> ClientPostError.TokenError.Invalid(message)
        CreateImageUploadsResponse.Error.DetailsCase.EXPIREDCLIENTTOKEN -> ClientPostError.TokenError.Expired(message)
        CreateImageUploadsResponse.Error.DetailsCase.BRIDGEPOSTUNAVAILABLE -> ClientPostError.ClientObjectError.PostUnavailable(message)
        else -> ClientPostError.FileError.Unknown(message)
    }

/** A file error of `PutPost`, about the photo at its `fileIndex` when it names one. */
internal fun PutPostResponse.Error.toFileError(): PostCreationError.FileError {
    val index = if (hasFileIndex()) fileIndex else null
    return when (detailsCase) {
        PutPostResponse.Error.DetailsCase.EMPTYFILE -> PostCreationError.FileError.EmptyFile(message, index)
        PutPostResponse.Error.DetailsCase.FILESIZETOOBIG -> PostCreationError.FileError.FileSizeTooBig(message, index)
        PutPostResponse.Error.DetailsCase.BADFILEFORMAT -> PostCreationError.FileError.BadFileFormat(message, index)
        PutPostResponse.Error.DetailsCase.UPLOADISSUE -> PostCreationError.FileError.UploadIssue(message, index)
        PutPostResponse.Error.DetailsCase.DETAILS_NOT_SET -> PostCreationError.FileError.DetailsNotSet(message, index)
        else -> PostCreationError.FileError.Unknown(message, index)
    }
}
