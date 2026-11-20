package com.octopuscommunity.sdk.data.mapper

import android.util.Base64
import androidx.core.net.toUri
import com.octopuscommunity.GrpcModelProto
import com.octopuscommunity.OctoObjectServiceProto.GetOrCreateBridgePostResponse
import com.octopuscommunity.OctoObjectServiceProto.GetResponse
import com.octopuscommunity.OctoObjectServiceProto.PutPollVoteResponse
import com.octopuscommunity.OctoObjectServiceProto.PutPostResponse
import com.octopuscommunity.OctoObjectServiceProto.PutRequest
import com.octopuscommunity.OctoObjectServiceProto.RwOctoObject
import com.octopuscommunity.aggregateOrNull
import com.octopuscommunity.bridgeToClientObject
import com.octopuscommunity.bridgeToClientObjectOrNull
import com.octopuscommunity.cTA
import com.octopuscommunity.content
import com.octopuscommunity.createdByOrNull
import com.octopuscommunity.ctaOrNull
import com.octopuscommunity.image
import com.octopuscommunity.mediaOrNull
import com.octopuscommunity.poll
import com.octopuscommunity.pollAnswer
import com.octopuscommunity.pollOrNull
import com.octopuscommunity.pollResultOrNull
import com.octopuscommunity.post
import com.octopuscommunity.reactionCtxOrNull
import com.octopuscommunity.requesterCtxOrNull
import com.octopuscommunity.rwOctoObject
import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.data.local.entity.FeedsEntity
import com.octopuscommunity.sdk.data.local.entity.PostEntity
import com.octopuscommunity.sdk.data.local.entity.PostEntity.ClientObjectEntity
import com.octopuscommunity.sdk.data.local.entity.PostEntity.CountersEntity
import com.octopuscommunity.sdk.data.local.entity.PostEntity.CustomActionEntity
import com.octopuscommunity.sdk.data.local.entity.PostEntity.PollEntity
import com.octopuscommunity.sdk.data.local.entity.PostEntity.PollEntity.AnswerEntity
import com.octopuscommunity.sdk.data.local.entity.PostWithTopicEntity
import com.octopuscommunity.sdk.data.local.entity.TranslatableTextEntity
import com.octopuscommunity.sdk.domain.model.ClientPost
import com.octopuscommunity.sdk.domain.model.CreationSource
import com.octopuscommunity.sdk.domain.model.OctopusLocalizedText
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.model.Post.ClientObject
import com.octopuscommunity.sdk.domain.model.Post.CustomAction
import com.octopuscommunity.sdk.domain.model.Post.Poll
import com.octopuscommunity.sdk.domain.model.Post.Poll.Answer
import com.octopuscommunity.sdk.domain.model.Resource
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ClientPostError
import com.octopuscommunity.sdk.domain.repository.PollVoteError
import com.octopuscommunity.sdk.domain.repository.PostCreationError
import com.octopuscommunity.strictMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class PostMapper(
    private val preferencesDataStore: PreferencesDataStore,
    private val statusMapper: StatusMapper,
    private val translationMapper: TranslationMapper,
    private val profileMapper: ProfileMapper,
    private val groupMapper: GroupMapper,
    private val mediaMapper: MediaMapper,
    private val reactionMapper: ReactionMapper,
    private val feedMapper: FeedMapper,
) : OctopusMapper {
    fun Post.toEntity() =
        PostEntity(
            id = id,
            parentId = parentId,
            createdAt = creationDate.time,
            updatedAt = updateDate.time,
            localeLanguage = localeLanguage,
            status = with(statusMapper) { status.toEntity() },
            author = with(profileMapper) { author?.toEntity() },
            text = with(translationMapper) { text.toEntity() },
            medias = with(mediaMapper) { medias?.toEntity() },
            poll = poll?.toEntity(),
            commentsFeeds = with(feedMapper) { commentsFeeds.toEntity() },
            counters =
                CountersEntity(
                    viewCount = viewCount,
                    commentCount = commentCount,
                    reactions = with(reactionMapper) { reactions.map { it.toEntity() } },
                    pollResults = poll?.results?.map { it.toEntity() },
                ),
            userReaction = with(reactionMapper) { userReaction?.toEntity() },
            userVoteId = pollVoteId,
            clientObject = clientObject?.toEntity(),
            customAction = customAction?.toEntity(),
            canAccess = canAccess,
            canCreateChildren = canCreateChildren,
        )

    fun Poll.toEntity() =
        PollEntity(
            answers =
                answers.map {
                    AnswerEntity(
                        id = it.id,
                        text = with(translationMapper) { it.text.toEntity() },
                    )
                },
        )

    fun Poll.Result.toEntity() =
        PollEntity.ResultEntity(
            answerId = answerId,
            voteCount = voteCount,
        )

    fun ClientObject.toEntity() =
        ClientObjectEntity(
            id = id,
            catchPhrase = with(translationMapper) { catchPhrase?.toEntity() },
            ctaText = with(translationMapper) { ctaText?.toEntity() },
        )

    fun CustomAction.toEntity() =
        CustomActionEntity(
            ctaText = with(translationMapper) { ctaText.toEntity() },
            targetUrl = targetUrl,
        )

    fun PostEntity.toDomain() =
        Post(
            id = id,
            parentId = parentId,
            creationDate = Date(createdAt),
            updateDate = Date(updatedAt),
            localeLanguage = localeLanguage,
            status = with(statusMapper) { status.toDomain() },
            author = with(profileMapper) { author?.toDomain() },
            text = with(translationMapper) { text.toDomain() },
            medias = with(mediaMapper) { medias?.toDomain() },
            poll =
                poll?.toDomain()?.let { poll ->
                    poll.copy(
                        results =
                            poll.answers.map { answer ->
                                Poll.Result(
                                    answerId = answer.id,
                                    voteCount =
                                        counters.pollResults
                                            ?.find {
                                                it.answerId == answer.id
                                            }?.voteCount ?: 0,
                                )
                            },
                    )
                },
            commentsFeeds = with(feedMapper) { commentsFeeds.toDomain() },
            viewCount = counters.viewCount,
            commentCount = counters.commentCount,
            reactions = with(reactionMapper) { counters.reactions.map { it.toDomain() } },
            userReaction = with(reactionMapper) { userReaction?.toDomain() },
            pollVoteId = userVoteId,
            clientObject = clientObject?.toDomain(),
            customAction = customAction?.toDomain(),
            canAccess = canAccess,
            canCreateChildren = canCreateChildren,
        )

    fun PostWithTopicEntity.toDomain() =
        post.toDomain().copy(
            group = with(groupMapper) { topic?.toDomain() },
        )

    fun PollEntity.toDomain() =
        Poll(
            answers =
                answers.map {
                    Answer(
                        id = it.id,
                        text = with(translationMapper) { it.text.toDomain() },
                    )
                },
        )

    fun ClientObjectEntity.toDomain() =
        ClientObject(
            id = id,
            catchPhrase = with(translationMapper) { catchPhrase?.toDomain() },
            ctaText = with(translationMapper) { ctaText?.toDomain() },
        )

    fun CustomActionEntity.toDomain() =
        CustomAction(
            ctaText = with(translationMapper) { ctaText.toDomain() },
            targetUrl = targetUrl,
        )

    suspend fun GetResponse.toEntity() =
        octoObject.toEntity().copy(
            counters = aggregateOrNull?.toEntity() ?: CountersEntity(),
            userReaction =
                with(reactionMapper) {
                    requesterCtxOrNull?.reactionCtxOrNull?.toEntity()
                },
            userVoteId = requesterCtxOrNull?.pollAnswerId?.ifEmpty { null },
            canAccess = requesterCtxOrNull?.takeIf { it.hasCanAccess() }?.canAccess ?: true,
            canCreateChildren = requesterCtxOrNull?.takeIf { it.hasCanCreateChildren() }?.canCreateChildren ?: true,
        )

    fun GetResponse.toCountersUpdateEntity() =
        PostEntity.CountersUpdate(
            id = octoObjectId,
            counters = aggregate.toEntity(),
            userReaction = with(reactionMapper) { requesterCtxOrNull?.reactionCtxOrNull?.toEntity() },
            userVoteId = requesterCtxOrNull?.pollAnswerId?.ifEmpty { null },
            canAccess = requesterCtxOrNull?.takeIf { it.hasCanAccess() }?.canAccess ?: true,
            canCreateChildren = requesterCtxOrNull?.takeIf { it.hasCanCreateChildren() }?.canCreateChildren ?: true,
        )

    suspend fun GetOrCreateBridgePostResponse.Success.toEntity(oldPost: PostEntity?) =
        postBridge.toEntity().copy(
            counters = aggregateOrNull?.toEntity() ?: CountersEntity(),
            userReaction = oldPost?.userReaction,
            userVoteId = oldPost?.userVoteId,
        )

    suspend fun GrpcModelProto.OctoObject.toEntity() =
        with(content.post) {
            PostEntity(
                id = id,
                parentId = parentId,
                createdAt = createdAt,
                updatedAt = updatedAt,
                localeLanguage = preferencesDataStore.getLocaleIdentifier(),
                status = with(statusMapper) { status.toEntity() },
                author = with(profileMapper) { createdByOrNull?.toEntity() },
                text =
                    TranslatableTextEntity(
                        originalText = text,
                        originalLanguage = originalLanguage,
                        translatedText = translatedText,
                        translatedLanguage = translatedLanguage,
                    ),
                medias = with(mediaMapper) { mediaOrNull?.toEntity() },
                poll =
                    pollOrNull?.takeIf { it.answersCount > 0 }?.toEntity(
                        originalLanguage = originalLanguage,
                        translatedLanguage = translatedLanguage,
                    ),
                commentsFeeds =
                    FeedsEntity(
                        descendingId = descChildrenFeedId.ifEmpty { null },
                        ascendingId = ascChildrenFeedId.ifEmpty { null },
                    ),
                clientObject =
                    bridgeToClientObjectOrNull?.toEntity(
                        originalLanguage = originalLanguage,
                        translatedLanguage = translatedLanguage,
                    ),
                customAction =
                    ctaOrNull?.toCustomActionEntity(
                        originalLanguage = originalLanguage,
                        translatedLanguage = translatedLanguage,
                    ),
            )
        }

    fun GrpcModelProto.Aggregate.toEntity() =
        CountersEntity(
            viewCount = viewCount,
            commentCount = childrenCount,
            reactions = with(reactionMapper) { reactionsList.map { it.toEntity() } },
            pollResults = pollResultOrNull?.answerResultsList?.map { it.toEntity() } ?: emptyList(),
        )

    fun GrpcModelProto.Poll.toEntity(
        originalLanguage: String,
        translatedLanguage: String,
    ) = PollEntity(
        answers =
            answersList.map {
                AnswerEntity(
                    id = it.id,
                    text =
                        TranslatableTextEntity(
                            originalText = it.text,
                            originalLanguage = originalLanguage,
                            translatedText = it.translatedText,
                            translatedLanguage = translatedLanguage,
                        ),
                )
            },
    )

    fun GrpcModelProto.PollAnswerResult.toEntity() =
        PollEntity.ResultEntity(
            answerId = pollAnswerId,
            voteCount = voteCount,
        )

    fun GrpcModelProto.BridgeToClientObject.toEntity(
        originalLanguage: String,
        translatedLanguage: String,
    ) = ClientObjectEntity(
        id = clientObjectId,
        catchPhrase =
            catchPhrase?.ifEmpty { null }?.let { catchPhrase ->
                TranslatableTextEntity(
                    originalText = catchPhrase,
                    originalLanguage = originalLanguage,
                    translatedText = translatedCatchPhrase,
                    translatedLanguage = translatedLanguage,
                )
            },
        ctaText =
            ctaOrNull?.toEntity(
                originalLanguage = originalLanguage,
                translatedLanguage = translatedLanguage,
            ),
    )

    fun GrpcModelProto.CTA.toEntity(
        originalLanguage: String,
        translatedLanguage: String?,
    ) = TranslatableTextEntity(
        originalText = text,
        originalLanguage = originalLanguage,
        translatedText = translatedText,
        translatedLanguage = translatedLanguage,
    )

    /**
     * Maps a proto Post CTA to its persistence shape. Returns `null` when the operator-provided
     * payload cannot back a usable button — keeps parity with the Topic CTA mapper
     * ([GroupMapper.toCustomActionEntity]) and iOS's failable `CustomAction.init?`
     * introduced on internal ticket.
     *
     * Gating rules (same on both surfaces):
     * - blank label (`text`),
     * - blank target URL (`targetLink`),
     * - target URL not parseable by [android.net.Uri].
     *
     * Prior behaviour returned a non-null entity with a possibly-blank label, which would
     * render an empty-text button (Compose `Text("")`) until the post detail view's
     * `targetUrl?.let { … }` guard finally hid it at tap time. The strict gate avoids
     * rendering the broken button at all.
     */
    fun GrpcModelProto.CTA.toCustomActionEntity(
        originalLanguage: String,
        translatedLanguage: String?,
    ): CustomActionEntity? {
        if (!GroupMapper.isCtaTextAndTargetPresent(text, targetLink)) return null
        if (targetLink.toUri().scheme == null) return null
        return CustomActionEntity(
            ctaText =
                TranslatableTextEntity(
                    originalText = text,
                    originalLanguage = originalLanguage,
                    translatedText = translatedText,
                    translatedLanguage = translatedLanguage,
                ),
            targetUrl = targetLink,
        )
    }

    /** Compatibility proto using every [images] entry and the legacy encoder; prefer the repository upload flow. */
    @Deprecated("Use the post repository's upload flow.")
    @Suppress("DEPRECATION")
    suspend fun Post.Draft.toProto(): RwOctoObject {
        val parentId = group!!.id
        val pressedEnterAt = System.currentTimeMillis()
        val encodedImages =
            images.mapIndexed { index, image ->
                namingImage(index) {
                    with(mediaMapper) {
                        image.toProto(
                            imageMaxSize = Post.Draft.IMAGE_UPLOAD_MAX_SIZE,
                            imageQuality = Post.Draft.IMAGE_UPLOAD_QUALITY,
                        )
                    }
                }
            }
        return toRwOctoObject(parentId = parentId, pressedEnterAt = pressedEnterAt, encodedImages = encodedImages)
    }

    /** Compatibility proto using every [images] entry and the legacy encoder; prefer the repository upload flow. */
    @Deprecated("Use the post repository's upload flow.")
    @Suppress("DEPRECATION")
    suspend fun ClientPost.toProto() =
        post {
            this@toProto.text.ifBlank { null }?.let { this.text = it }
            this@toProto.images.takeIf { it.isNotEmpty() }?.let { images ->
                this.media =
                    with(mediaMapper) {
                        strictMedia {
                            this.images +=
                                images.map { image ->
                                    image.toProto(
                                        maxSize = ClientPost.IMAGE_UPLOAD_MAX_SIZE,
                                        quality = ClientPost.IMAGE_UPLOAD_QUALITY,
                                        localDownload = false,
                                        // Preserve the legacy host-image tolerance on this compatibility path.
                                        failOnEncodingError = false,
                                    )
                                }
                        }
                    }
            }
            this.bridgeToClientObject =
                bridgeToClientObject {
                    this.clientObjectId = this@toProto.objectId
                    this@toProto.catchPhrase?.ifBlank { null }?.let { this.catchPhrase = it }
                    this@toProto.viewObjectButtonText?.ifBlank { null }?.let {
                        this.cta =
                            cTA {
                                this.text = it
                            }
                    }
                }
        }

    /** Compatibility fingerprint of every [images] entry, with the legacy encoding and backend separator. */
    @Deprecated("Use the post repository's multi-photo bridge flow.")
    suspend fun ClientPost.toFingerprint(): String =
        withContext(Dispatchers.IO) {
            ClientPostFingerprint.compute(
                catchPhrase = catchPhrase,
                clientObjectId = objectId,
                ctaText = viewObjectButtonText,
                images = with(mediaMapper) { images.mapNotNull { it.toFingerprint() } },
                text = text,
                topicId = groupId,
            )
        }

    /**
     * The draft's [Post.Draft.images] compressed for upload, in display order.
     *
     * @throws MediaEncodingException if an image could not be encoded, naming its index.
     */
    internal suspend fun Post.Draft.encodeImages(): List<EncodedImage> =
        with(mediaMapper) {
            images.mapIndexed { index, image ->
                namingImage(index) {
                    image.toEncodedImage(
                        imageMaxSize = Post.Draft.IMAGE_UPLOAD_MAX_SIZE,
                        imageQuality = Post.Draft.IMAGE_UPLOAD_QUALITY,
                    )
                }
            }
        }

    fun CreationSource.toProto(): PutRequest.CreationSource =
        when (this) {
            CreationSource.USER -> PutRequest.CreationSource.USER
            CreationSource.PREFILLED_FROM_CLIENT -> PutRequest.CreationSource.PREFILLED_FROM_CLIENT
        }

    /**
     * The local images of this client post for upload, as the host supplied them — neither resized
     * nor re-encoded — keyed by their index in [ClientPost.images]; or the file error of the first
     * one that cannot be read, which refuses the post: a bridge post is created once, so a photo
     * left out could never be added afterwards.
     */
    internal suspend fun ClientPost.encodeLocalImages(): OctopusResult<List<Pair<Int, EncodedImage>>, ClientPostError> =
        with(mediaMapper) {
            val encoded = mutableListOf<Pair<Int, EncodedImage>>()
            images.forEachIndexed { index, image ->
                if (image !is Resource.Local) return@forEachIndexed
                when (val read = image.toRawEncodedImage(index)) {
                    is OctopusResult.Success -> encoded += index to read.data
                    is OctopusResult.Failure -> return read
                }
            }
            OctopusResult.Success(encoded)
        }

    /**
     * Fingerprint of a client object post, see [ClientPostFingerprint]. Each image value is the URL
     * of a remote image, or the URL-safe Base64 (no padding) of the bytes uploaded for a local one:
     * [localImages], the host's own bytes, neither resized nor re-encoded.
     *
     * @param localImages The local images read for upload by [encodeLocalImages], keyed by their
     *   index in [ClientPost.images].
     */
    internal suspend fun ClientPost.toFingerprint(localImages: Map<Int, EncodedImage>): String =
        clientPostFingerprint(clientPost = this, localImages = localImages) { bytes ->
            Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
        }

    /**
     * Fingerprint of a prefilled (bridge / Share-in-game) post, matching the backend
     * `PrefilledShareVerifier`: SHA-256 (lowercase hex) of a compact JSON object with the fields
     * below in alphabetical order, empty fields omitted, the per-language CTA labels included when
     * there are some (see [BridgeShareFingerprint]). The host backend signs this value into
     * the `bridge_fingerprint` claim of the client token so the server accepts the images in a
     * community that forbids member pictures. Each image value is the resource URL (remote) or the
     * compressed bytes as URL-safe Base64 without padding (local) — the same bytes
     * [encodeImages] uploads, so the signature matches the photos the server receives. An image
     * that cannot be encoded has no value and is left out; [encodeImages] then fails the send anyway.
     */
    suspend fun Post.Draft.toBridgeShareFingerprint(): String =
        withContext(Dispatchers.IO) {
            BridgeShareFingerprint.compute(
                ctaTargetLink = customAction?.targetUrl,
                ctaText = customAction?.label,
                images = with(mediaMapper) { images.mapNotNull { it.source.toFingerprint() } },
                text = text,
                ctaTextLanguage = customAction?.labelLanguage,
                ctaTextTranslations = customAction?.labelTranslations.orEmpty(),
            )
        }

    fun PutPostResponse.Error.toDomain() =
        when (field) {
            PutPostResponse.Error.Field.CONTENT_PARENT ->
                when (detailsCase) {
                    PutPostResponse.Error.DetailsCase.MISSINGPARENT ->
                        PostCreationError.GroupError.Missing(message)

                    else -> PostCreationError.GroupError.Unknown(message)
                }

            PutPostResponse.Error.Field.CONTENT_TEXT ->
                when (detailsCase) {
                    PutPostResponse.Error.DetailsCase.MISSINGTEXT ->
                        PostCreationError.TextError.Missing(message)

                    PutPostResponse.Error.DetailsCase.MAXCHARLIMITREACHED ->
                        PostCreationError.TextError.MaxCharLimitReached(message)

                    PutPostResponse.Error.DetailsCase.BANNEDWORDUSED ->
                        PostCreationError.TextError.BannedWords(
                            errorMessage = message,
                            bannedWords = bannedWordUsed.wordsList,
                        )

                    else -> PostCreationError.TextError.Unknown(message)
                }

            PutPostResponse.Error.Field.CONTENT_FILE -> toFileError()

            PutPostResponse.Error.Field.CONTENT_POLL ->
                when (detailsCase) {
                    PutPostResponse.Error.DetailsCase.MAXCHARLIMITREACHED ->
                        PostCreationError.PollError.MaxCharLimitReached(message)

                    PutPostResponse.Error.DetailsCase.BADPOLLANSWERNUMBER ->
                        PostCreationError.PollError.BadPollAnswerNumber(message)

                    else -> PostCreationError.PollError.Unknown(message)
                }

            PutPostResponse.Error.Field.CONTENT_CATCHPHRASE ->
                when (detailsCase) {
                    PutPostResponse.Error.DetailsCase.MAXCHARLIMITREACHED ->
                        PostCreationError.CatchPhraseError.MaxCharLimitReached(message)

                    else -> PostCreationError.CatchPhraseError.Unknown(message)
                }

            PutPostResponse.Error.Field.CONTENT_CTA ->
                when (detailsCase) {
                    PutPostResponse.Error.DetailsCase.MAXCHARLIMITREACHED ->
                        PostCreationError.CtaError.MaxCharLimitReached(message)

                    PutPostResponse.Error.DetailsCase.INVALIDAUTHOR ->
                        PostCreationError.CtaError.InvalidAuthor(message)

                    PutPostResponse.Error.DetailsCase.MISSINGCTATEXT ->
                        PostCreationError.CtaError.MissingText(message)

                    PutPostResponse.Error.DetailsCase.MISSINGCTATARGET ->
                        PostCreationError.CtaError.MissingTarget(message)

                    PutPostResponse.Error.DetailsCase.INVALIDCTATARGET ->
                        PostCreationError.CtaError.InvalidTarget(message)

                    else -> PostCreationError.CtaError.Unknown(message)
                }

            PutPostResponse.Error.Field.CONTENT_CLIENT_OBJECT ->
                when (detailsCase) {
                    PutPostResponse.Error.DetailsCase.MISSINGCLIENTOBJECTID ->
                        PostCreationError.ClientObjectError.MissingClientObjectId(message)

                    else -> PostCreationError.ClientObjectError.Unknown(message)
                }

            PutPostResponse.Error.Field.UNKNOWN ->
                when (detailsCase) {
                    PutPostResponse.Error.DetailsCase.PUBLICATIONRATELIMITEXCEEDED ->
                        PostCreationError.PublicationRateLimitExceeded(
                            errorMessage = message,
                            waitInSec = publicationRateLimitExceeded.waitInSec,
                        )

                    else -> PostCreationError.OtherError(message)
                }

            PutPostResponse.Error.Field.UNRECOGNIZED -> PostCreationError.OtherError(message)
        }

    fun GetOrCreateBridgePostResponse.Error.toDomain() =
        when (field) {
            GetOrCreateBridgePostResponse.Error.Field.UNKNOWN ->
                when (detailsCase) {
                    GetOrCreateBridgePostResponse.Error.DetailsCase.INVALIDCLIENTTOKEN ->
                        ClientPostError.TokenError.Invalid(message)

                    GetOrCreateBridgePostResponse.Error.DetailsCase.EXPIREDCLIENTTOKEN ->
                        ClientPostError.TokenError.Expired(message)

                    else -> ClientPostError.TokenError.Unknown(message)
                }

            GetOrCreateBridgePostResponse.Error.Field.CONTENT_TEXT ->
                when (detailsCase) {
                    GetOrCreateBridgePostResponse.Error.DetailsCase.MISSINGTEXT ->
                        ClientPostError.TextError.Missing(message)

                    GetOrCreateBridgePostResponse.Error.DetailsCase.MAXCHARLIMITREACHED ->
                        ClientPostError.TextError.MaxCharLimitReached(message)

                    else -> ClientPostError.TextError.Unknown(message)
                }

            GetOrCreateBridgePostResponse.Error.Field.CONTENT_FILE ->
                when (detailsCase) {
                    GetOrCreateBridgePostResponse.Error.DetailsCase.EMPTYFILE ->
                        ClientPostError.FileError.EmptyFile(message)

                    GetOrCreateBridgePostResponse.Error.DetailsCase.FILESIZETOOBIG ->
                        ClientPostError.FileError.FileSizeTooBig(message)

                    GetOrCreateBridgePostResponse.Error.DetailsCase.BADFILEFORMAT ->
                        ClientPostError.FileError.BadFileFormat(message)

                    GetOrCreateBridgePostResponse.Error.DetailsCase.UPLOADISSUE ->
                        ClientPostError.FileError.UploadIssue(message)

                    GetOrCreateBridgePostResponse.Error.DetailsCase.DOWNLOADISSUE ->
                        ClientPostError.FileError.DownloadIssue(message)

                    else -> ClientPostError.FileError.Unknown(message)
                }

            GetOrCreateBridgePostResponse.Error.Field.CONTENT_CLIENT_OBJECT ->
                when (detailsCase) {
                    GetOrCreateBridgePostResponse.Error.DetailsCase.MISSINGCTATEXT ->
                        ClientPostError.ClientObjectError.MissingCta(message)

                    GetOrCreateBridgePostResponse.Error.DetailsCase.MISSINGCLIENTOBJECTID ->
                        ClientPostError.ClientObjectError.MissingId(message)

                    GetOrCreateBridgePostResponse.Error.DetailsCase.BRIDGEPOSTUNAVAILABLE ->
                        ClientPostError.ClientObjectError.PostUnavailable(message)

                    GetOrCreateBridgePostResponse.Error.DetailsCase.INVALIDTOPICID ->
                        ClientPostError.ClientObjectError.InvalidGroupId(message)

                    GetOrCreateBridgePostResponse.Error.DetailsCase.INVALIDAUTHOR ->
                        ClientPostError.ClientObjectError.InvalidAuthor(message)

                    GetOrCreateBridgePostResponse.Error.DetailsCase.POSTNOTFOUND ->
                        ClientPostError.ClientObjectError.PostNotFound(message)

                    GetOrCreateBridgePostResponse.Error.DetailsCase.POSTALREADYEXISTS ->
                        ClientPostError.ClientObjectError.PostAlreadyExists(message)

                    else -> ClientPostError.ClientObjectError.Unknown(message)
                }

            else -> ClientPostError.OtherError(message)
        }

    fun PutPollVoteResponse.Error.toDomain() =
        when (field) {
            PutPollVoteResponse.Error.Field.CONTENT_PARENT ->
                when (detailsCase) {
                    PutPollVoteResponse.Error.DetailsCase.MISSINGPARENT ->
                        PollVoteError.PostUnavailable(
                            message,
                        )

                    else -> PollVoteError.Unknown(message)
                }

            PutPollVoteResponse.Error.Field.CONTENT_TARGET ->
                when (detailsCase) {
                    PutPollVoteResponse.Error.DetailsCase.MISSINGPOLLANSWER ->
                        PollVoteError.MissingPollAnswer(
                            message,
                        )

                    else -> PollVoteError.Unknown(message)
                }

            else -> PollVoteError.Unknown(message)
        }
}

/**
 * Assembles the proto of a post draft around images that are already encoded, so the assembly —
 * text, images, poll and CTA side by side — can be checked without the image encoder, which needs a
 * real `ImageLoader` and `android.graphics.Bitmap`. The images and the poll are independent: a draft
 * carrying both sends both in the same `Post` message.
 *
 * @param parentId Id of the draft's group, resolved before the image encoding so a draft without
 *   a group fails before any image work.
 * @param pressedEnterAt The moment the member hit Publish, captured before the image encoding.
 * @param encodedImages The draft's [Post.Draft.images], already encoded, in display order; empty when
 *   the draft has no image. [Post.Draft.images] itself is not read here.
 */
internal fun Post.Draft.toRwOctoObject(
    parentId: String,
    pressedEnterAt: Long,
    encodedImages: List<GrpcModelProto.Image>,
): RwOctoObject =
    rwOctoObject {
        this.parentId = parentId
        this.pressedEnterAt = pressedEnterAt
        this.content =
            content {
                this.post =
                    post {
                        this@toRwOctoObject.text.ifBlank { null }?.let { this.text = it }
                        if (encodedImages.isNotEmpty()) {
                            this.media = strictMedia { this.images += encodedImages }
                        }
                        this@toRwOctoObject.poll?.let { poll ->
                            this.poll =
                                poll {
                                    answers +=
                                        poll.answers.map {
                                            pollAnswer {
                                                this.text = it.text
                                            }
                                        }
                                }
                        }
                        this@toRwOctoObject.customAction?.let { customAction ->
                            this.cta =
                                cTA {
                                    this.text = customAction.label
                                    this.targetLink = customAction.targetUrl
                                    customAction.labelLanguage?.let { language ->
                                        this.textLanguage = language
                                        this.textTranslations.putAll(customAction.labelTranslations)
                                    }
                                }
                        }
                    }
            }
    }

/**
 * The bridge post sent on creation: its remote images as URLs for the backend to fetch, its local
 * ones by the upload that carried them, all in the order of [ClientPost.images].
 *
 * @param uploadedLocalImages The uploaded local images, keyed by their index in
 *   [ClientPost.images]. A local image missing from it would be left out, but none is:
 *   [PostMapper.encodeLocalImages] refuses a post whose local image cannot be read.
 */
internal fun ClientPost.toPostProto(uploadedLocalImages: Map<Int, GrpcModelProto.Image>): GrpcModelProto.Post =
    post {
        this@toPostProto.text.ifBlank { null }?.let { this.text = it }
        val images =
            this@toPostProto.images.mapIndexedNotNull { index, resource ->
                when (resource) {
                    is Resource.Remote ->
                        image {
                            this.name = "${System.currentTimeMillis()}"
                            this.contentType = "image/*"
                            this.url = resource.url
                        }

                    is Resource.Local -> uploadedLocalImages[index]
                }
            }
        if (images.isNotEmpty()) {
            this.media = strictMedia { this.images += images }
        }
        this.bridgeToClientObject =
            bridgeToClientObject {
                this.clientObjectId = this@toPostProto.objectId
                this@toPostProto.catchPhrase?.ifBlank { null }?.let { this.catchPhrase = it }
                this@toPostProto.viewObjectButtonText?.ifBlank { null }?.let {
                    this.cta =
                        cTA {
                            this.text = it
                            this@toPostProto.buttonLanguage?.let { language ->
                                this.textLanguage = language
                                this.textTranslations.putAll(this@toPostProto.buttonTranslations)
                            }
                        }
                }
            }
    }

/**
 * Bytes of a local image handed to the encoder at a time while hashing it: a multiple of 3, so that
 * the unpadded Base64 of the pieces, joined, is the Base64 of the whole.
 */
internal const val FINGERPRINT_ENCODE_CHUNK_BYTES = 3 * 16 * 1024

/**
 * The fingerprint of [clientPost], see [ClientPostFingerprint]: its images joined in order, each the
 * URL of a remote image or [encode] of the uploaded bytes of a local one. Computed on
 * [Dispatchers.Default]: hashing ten camera photos takes about half a second.
 */
internal suspend fun clientPostFingerprint(
    clientPost: ClientPost,
    localImages: Map<Int, EncodedImage>,
    encode: (ByteArray) -> String,
): String = withContext(Dispatchers.Default) { computeClientPostFingerprint(clientPost, localImages, encode) }

private fun computeClientPostFingerprint(
    clientPost: ClientPost,
    localImages: Map<Int, EncodedImage>,
    encode: (ByteArray) -> String,
): String =
    ClientPostFingerprint.computeStreamed(
        catchPhrase = clientPost.catchPhrase,
        clientObjectId = clientPost.objectId,
        ctaText = clientPost.viewObjectButtonText,
        images = clientPost.fingerprintImages(localImages = localImages, encode = encode),
        text = clientPost.text,
        topicId = clientPost.groupId,
        ctaTextLanguage = clientPost.buttonLanguage,
        ctaTextTranslations = clientPost.buttonTranslations,
    )

/** The code of the default language of a button text given per language; `null` when it is auto-translated. */
private val ClientPost.buttonLanguage: String?
    get() = (viewObjectButton as? OctopusLocalizedText.PerLanguage)?.defaultLanguageCode

/** The texts of a button given per language by language code, the default language excluded; else empty. */
private val ClientPost.buttonTranslations: Map<String, String>
    get() =
        (viewObjectButton as? OctopusLocalizedText.PerLanguage)
            ?.let { it.textsByLanguageCode - it.defaultLanguageCode }
            .orEmpty()

/**
 * The image values of a client post's fingerprint, in the order of [ClientPost.images]: the URL of
 * a remote image, [encode] of the uploaded bytes of a local one, applied to pieces of
 * [FINGERPRINT_ENCODE_CHUNK_BYTES] so that neither the bytes nor their encoding are ever copied
 * whole. A local image missing from [localImages] would be left out, but none is:
 * [PostMapper.encodeLocalImages] refuses a post whose local image cannot be read.
 */
internal fun ClientPost.fingerprintImages(
    localImages: Map<Int, EncodedImage>,
    encode: (ByteArray) -> String,
): List<FingerprintImageValue> =
    images.mapIndexedNotNull { index, resource ->
        when (resource) {
            is Resource.Remote -> FingerprintImageValue { piece -> piece(resource.url) }
            is Resource.Local ->
                localImages[index]?.bytes?.let { bytes ->
                    FingerprintImageValue { piece ->
                        var offset = 0
                        while (offset < bytes.size()) {
                            val chunk = ByteArray(minOf(FINGERPRINT_ENCODE_CHUNK_BYTES, bytes.size() - offset))
                            bytes.copyTo(chunk, offset, 0, chunk.size)
                            piece(encode(chunk))
                            offset += chunk.size
                        }
                    }
                }
        }
    }
