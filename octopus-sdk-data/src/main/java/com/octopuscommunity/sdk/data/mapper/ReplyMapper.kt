package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.GrpcModelProto
import com.octopuscommunity.GrpcModelProto.OctoObject
import com.octopuscommunity.OctoObjectServiceProto
import com.octopuscommunity.OctoObjectServiceProto.GetResponse
import com.octopuscommunity.OctoObjectServiceProto.PutReplyResponse.Error.DetailsCase
import com.octopuscommunity.OctoObjectServiceProto.PutReplyResponse.Error.Field
import com.octopuscommunity.aggregateOrNull
import com.octopuscommunity.content
import com.octopuscommunity.createdByOrNull
import com.octopuscommunity.mediaOrNull
import com.octopuscommunity.reactionCtxOrNull
import com.octopuscommunity.reply
import com.octopuscommunity.requesterCtxOrNull
import com.octopuscommunity.rwOctoObject
import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.data.local.entity.ReplyEntity
import com.octopuscommunity.sdk.data.local.entity.ReplyEntity.CountersEntity
import com.octopuscommunity.sdk.data.local.entity.ReplyWithCommentEntity
import com.octopuscommunity.sdk.data.local.entity.TranslatableTextEntity
import com.octopuscommunity.sdk.domain.model.Comment.Reply
import com.octopuscommunity.sdk.domain.model.Comment.Reply.CreationError.CommentError
import com.octopuscommunity.sdk.domain.model.Comment.Reply.CreationError.FileError
import com.octopuscommunity.sdk.domain.model.Comment.Reply.CreationError.OtherError
import com.octopuscommunity.sdk.domain.model.Comment.Reply.CreationError.TextError
import com.octopuscommunity.strictMedia
import java.util.Date

class ReplyMapper(
    private val preferencesDataStore: PreferencesDataStore,
    private val statusMapper: StatusMapper,
    private val translationMapper: TranslationMapper,
    private val profileMapper: ProfileMapper,
    private val commentMapper: CommentMapper,
    private val mediaMapper: MediaMapper,
    private val reactionMapper: ReactionMapper,
) : OctopusMapper {
    fun Reply.toEntity() =
        ReplyEntity(
            id = id,
            parentId = parentId,
            createdAt = creationDate.time,
            updatedAt = updateDate.time,
            localeLanguage = localeLanguage,
            status = with(statusMapper) { status.toEntity() },
            author = with(profileMapper) { author?.toEntity() },
            text = with(translationMapper) { text.toEntity() },
            medias = with(mediaMapper) { medias?.toEntity() },
            counters =
                CountersEntity(
                    reactions = with(reactionMapper) { reactions.map { it.toEntity() } },
                ),
            userReaction = with(reactionMapper) { userReaction?.toEntity() },
            canAccess = canAccess,
            canCreateChildren = canCreateChildren,
        )

    fun ReplyEntity.toDomain() =
        Reply(
            id = id,
            parentId = parentId,
            creationDate = Date(createdAt),
            updateDate = Date(updatedAt),
            localeLanguage = localeLanguage,
            status = with(statusMapper) { status.toDomain() },
            author = with(profileMapper) { author?.toDomain() },
            text = with(translationMapper) { text.toDomain() },
            medias = with(mediaMapper) { medias?.toDomain() },
            reactions = with(reactionMapper) { counters.reactions.map { it.toDomain() } },
            userReaction = with(reactionMapper) { userReaction?.toDomain() },
            canAccess = canAccess,
            canCreateChildren = canCreateChildren,
        )

    fun ReplyWithCommentEntity.toDomain() =
        reply.toDomain().copy(
            comment = with(commentMapper) { comment?.toDomain() },
        )

    suspend fun GetResponse.toEntity() =
        octoObject.toEntity().copy(
            counters = aggregateOrNull?.toEntity() ?: CountersEntity(),
            userReaction = with(reactionMapper) { requesterCtxOrNull?.reactionCtxOrNull?.toEntity() },
            canAccess = requesterCtxOrNull?.takeIf { it.hasCanAccess() }?.canAccess ?: true,
            canCreateChildren = requesterCtxOrNull?.takeIf { it.hasCanCreateChildren() }?.canCreateChildren ?: true,
        )

    fun GetResponse.toCounterUpdateEntity() =
        CountersEntity.Update(
            id = octoObjectId,
            counters = aggregate.toEntity(),
            userReaction = with(reactionMapper) { requesterCtxOrNull?.reactionCtxOrNull?.toEntity() },
            canAccess = requesterCtxOrNull?.takeIf { it.hasCanAccess() }?.canAccess ?: true,
            canCreateChildren = requesterCtxOrNull?.takeIf { it.hasCanCreateChildren() }?.canCreateChildren ?: true,
        )

    suspend fun OctoObject.toEntity() =
        with(content.reply) {
            ReplyEntity(
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
            )
        }

    fun GrpcModelProto.Aggregate.toEntity() =
        CountersEntity(
            reactions = with(reactionMapper) { reactionsList.map { it.toEntity() } },
        )

    suspend fun Reply.Draft.toProto() =
        rwOctoObject {
            this.parentId = this@toProto.parentId
            this.pressedEnterAt = System.currentTimeMillis()
            this.content =
                content {
                    this.reply =
                        reply {
                            this@toProto.text.ifBlank { null }?.let { this.text = it }
                            this@toProto.media?.let { media ->
                                this.media =
                                    with(mediaMapper) {
                                        strictMedia {
                                            images +=
                                                media.toProto(
                                                    imageMaxSize = Reply.Draft.IMAGE_UPLOAD_MAX_SIZE,
                                                    imageQuality = Reply.Draft.IMAGE_UPLOAD_QUALITY,
                                                )
                                        }
                                    }
                            }
                        }
                }
        }

    fun OctoObjectServiceProto.PutReplyResponse.Error.toDomain() =
        when (field) {
            Field.CONTENT_PARENT ->
                when (detailsCase) {
                    DetailsCase.MISSINGPARENT -> CommentError.CommentUnavailable(message)
                    else -> CommentError.Unknown(message)
                }

            Field.CONTENT_TEXT ->
                when (detailsCase) {
                    DetailsCase.EMPTYPUBLICATION -> TextError.Missing(message)
                    DetailsCase.MAXCHARLIMITREACHED ->
                        TextError.MaxCharLimitReached(
                            message,
                        )

                    DetailsCase.BANNEDWORDUSED ->
                        TextError.BannedWords(
                            errorMessage = message,
                            bannedWords = bannedWordUsed.wordsList,
                        )

                    else -> TextError.Unknown(message)
                }

            Field.CONTENT_FILE ->
                when (detailsCase) {
                    DetailsCase.EMPTYFILE -> FileError.EmptyFile(message)
                    DetailsCase.FILESIZETOOBIG -> FileError.FileSizeTooBig(message)
                    DetailsCase.BADFILEFORMAT -> FileError.BadFileFormat(message)
                    DetailsCase.UPLOADISSUE -> FileError.UploadIssue(message)
                    DetailsCase.DETAILS_NOT_SET -> FileError.DetailsNotSet(message)
                    else -> FileError.Unknown(message)
                }

            else ->
                when (detailsCase) {
                    DetailsCase.PUBLICATIONRATELIMITEXCEEDED ->
                        Reply.CreationError.PublicationRateLimitExceeded(
                            errorMessage = message,
                            waitInSec = publicationRateLimitExceeded.waitInSec,
                        )

                    else -> OtherError(message)
                }
        }
}
