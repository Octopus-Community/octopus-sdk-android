package com.octopuscommunity.sdk.domain.model

import com.octopuscommunity.sdk.domain.model.OctopusItem.ContentKind
import com.octopuscommunity.sdk.domain.model.OctopusItem.Feeds
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount
import java.util.Date

/** A comment on a [Post]. */
data class Comment(
    override val id: String,
    /** Id of the commented post. */
    val parentId: String,
    override val creationDate: Date,
    override val updateDate: Date,
    override val localeLanguage: String,
    override val status: Status,
    override val author: Author?,
    override val text: TranslatableText,
    override val medias: Medias? = null,
    /** Feeds of the replies to this comment. */
    val repliesFeeds: Feeds,
    /** Number of replies to this comment. */
    val replyCount: Int,
    override val reactions: List<ReactionCount> = emptyList(),
    override val userReaction: Reaction? = null,
    /** The commented post, when loaded with the comment. */
    val post: Post? = null,
    /** Whether the current user can access this comment. */
    val canAccess: Boolean = true,
    /** Whether replies can be created on this comment. */
    val canCreateChildren: Boolean = true,
) : OctopusItem {
    /** A comment being written on [post]. */
    data class Draft(
        val post: Post,
        override val text: String = "",
        override val media: Media.Image? = null,
    ) : OctopusItem.Draft {
        override val parentId = post.id

        companion object {
            val TEXT_LENGTH_RANGE = 0..5000
            const val IMAGE_UPLOAD_MAX_SIZE = Media.Image.UPLOAD_MAX_DIMENSION
            const val IMAGE_UPLOAD_QUALITY = Media.Image.UPLOAD_QUALITY
        }
    }

    /** Errors returned when a comment cannot be created. */
    sealed interface CreationError : OctopusItem.CreationError {
        sealed class TextError : CreationError {
            data class Missing(
                override val errorMessage: String,
            ) : TextError()

            data class MaxCharLimitReached(
                override val errorMessage: String,
            ) : TextError()

            data class BannedWords(
                override val errorMessage: String,
                val bannedWords: List<String>,
            ) : TextError()

            data class Unknown(
                override val errorMessage: String,
            ) : TextError()
        }

        sealed class FileError : CreationError {
            data class EmptyFile(
                override val errorMessage: String,
            ) : FileError()

            data class FileSizeTooBig(
                override val errorMessage: String,
            ) : FileError()

            data class BadFileFormat(
                override val errorMessage: String,
            ) : FileError()

            data class UploadIssue(
                override val errorMessage: String,
            ) : FileError()

            data class DetailsNotSet(
                override val errorMessage: String,
            ) : FileError()

            data class Unknown(
                override val errorMessage: String,
            ) : FileError()
        }

        sealed class PostError : CreationError {
            data class PostUnavailable(
                override val errorMessage: String,
            ) : PostError()

            data class Unknown(
                override val errorMessage: String,
            ) : PostError()
        }

        data class PublicationRateLimitExceeded(
            override val errorMessage: String,
            override val waitInSec: Long,
        ) : CreationError,
            OctopusItem.RateLimitExceedError

        data class OtherError(
            override val errorMessage: String,
        ) : CreationError
    }

    /** A reply to a [Comment]. */
    data class Reply(
        override val id: String,
        /** Id of the parent comment. */
        val parentId: String,
        override val creationDate: Date,
        override val updateDate: Date,
        override val localeLanguage: String,
        override val status: Status,
        override val author: Author?,
        override val text: TranslatableText,
        override val medias: Medias? = null,
        override val reactions: List<ReactionCount> = emptyList(),
        override val userReaction: Reaction? = null,
        /** The parent comment, when loaded with the reply. */
        val comment: Comment? = null,
        /** Whether the current user can access this reply. */
        val canAccess: Boolean = true,
        /** Whether children can be created under this reply. */
        val canCreateChildren: Boolean = true,
    ) : OctopusItem {
        /** A reply being written on [comment]. */
        data class Draft(
            val comment: Comment,
            override val text: String = "",
            override val media: Media.Image? = null,
        ) : OctopusItem.Draft {
            override val parentId = comment.id

            companion object {
                val TEXT_LENGTH_RANGE = Comment.Draft.TEXT_LENGTH_RANGE
                const val IMAGE_UPLOAD_MAX_SIZE = Comment.Draft.IMAGE_UPLOAD_MAX_SIZE
                const val IMAGE_UPLOAD_QUALITY = Comment.Draft.IMAGE_UPLOAD_QUALITY
            }
        }

        /** Errors returned when a reply cannot be created. */
        sealed interface CreationError : OctopusItem.CreationError {
            sealed class TextError : CreationError {
                data class Missing(
                    override val errorMessage: String,
                ) : TextError()

                data class MaxCharLimitReached(
                    override val errorMessage: String,
                ) : TextError()

                data class BannedWords(
                    override val errorMessage: String,
                    val bannedWords: List<String>,
                ) : TextError()

                data class Unknown(
                    override val errorMessage: String,
                ) : TextError()
            }

            sealed class FileError : CreationError {
                data class EmptyFile(
                    override val errorMessage: String,
                ) : FileError()

                data class FileSizeTooBig(
                    override val errorMessage: String,
                ) : FileError()

                data class BadFileFormat(
                    override val errorMessage: String,
                ) : FileError()

                data class UploadIssue(
                    override val errorMessage: String,
                ) : FileError()

                data class DetailsNotSet(
                    override val errorMessage: String,
                ) : FileError()

                data class Unknown(
                    override val errorMessage: String,
                ) : FileError()
            }

            sealed class CommentError : CreationError {
                data class CommentUnavailable(
                    override val errorMessage: String,
                ) : CommentError()

                data class Unknown(
                    override val errorMessage: String,
                ) : CommentError()
            }

            data class PublicationRateLimitExceeded(
                override val errorMessage: String,
                override val waitInSec: Long,
            ) : CreationError,
                OctopusItem.RateLimitExceedError

            data class OtherError(
                override val errorMessage: String,
            ) : CreationError
        }

        override val contentKind = ContentKind.REPLY
    }

    override val contentKind = ContentKind.COMMENT
}
