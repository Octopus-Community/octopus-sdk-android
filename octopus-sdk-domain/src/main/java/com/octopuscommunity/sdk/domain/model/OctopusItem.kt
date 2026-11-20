package com.octopuscommunity.sdk.domain.model

import com.octopuscommunity.sdk.domain.network.ServerError
import java.util.Date

/** Content of the community: a [Post], a [Comment] or a [Comment.Reply]. */
interface OctopusItem {
    /** Kind of an item. */
    enum class ContentKind {
        POST,
        COMMENT,
        REPLY,
    }

    /** Ids of the feeds listing the children of an item, in descending and ascending order. */
    data class Feeds(
        val descendingId: String? = null,
        val ascendingId: String? = null,
    ) {
        /** Id combining both feeds, or `null` when the item has none. */
        val id: String? =
            "${descendingId}_$ascendingId".takeIf {
                ascendingId != null || descendingId != null
            }
    }

    /** A reaction of the current user on an item. */
    data class Reaction(
        val kind: Kind,
        val id: String = "",
    ) {
        /** Kind of a reaction, rendered as [unicode]. */
        sealed class Kind(
            override val unicode: String,
        ) : OctopusReactionKind {
            data object Heart : Kind("❤️")

            data object Joy : Kind("😂")

            data object MouthOpen : Kind("😮")

            data object Clap : Kind("👏")

            data object Cry : Kind("😢")

            data object Rage : Kind("😡")

            data class Unknown(
                override val unicode: String,
            ) : Kind(unicode)

            companion object {
                /** The known reaction kinds, without [Unknown]. */
                val ALL get() = setOf(Heart, Joy, MouthOpen, Clap, Cry, Rage)
            }
        }

        val unicode = kind.unicode
    }

    /** Number of reactions of one kind on an item. */
    data class ReactionCount(
        override val reactionKind: Reaction.Kind,
        override val count: Int,
    ) : OctopusReactionCount {
        val unicode = reactionKind.unicode
    }

    /** An item being written. */
    interface Draft {
        val parentId: String?
        val text: String
        val media: Media?

        /** Whether the draft has a text or a media. */
        val hasContent get() = text.isNotEmpty() || media?.source != null
    }

    /** Errors returned when an item cannot be created. */
    interface CreationError : ServerError

    /** Error returned when the user publishes too often; retry after [waitInSec] seconds. */
    interface RateLimitExceedError : ServerError {
        val waitInSec: Long
    }

    val id: String
    val contentKind: ContentKind
    val creationDate: Date
    val updateDate: Date

    /** Language the item was written in. */
    val localeLanguage: String
    val status: Status
    val author: Author?
    val text: TranslatableText
    val medias: Medias?
    val reactions: List<ReactionCount>

    /** Reaction of the current user, or `null` when they did not react. */
    val userReaction: Reaction?
    val image get() = medias?.images?.firstOrNull()
    val video get() = medias?.video
    val hasVideo get() = video != null

    /** Length of the original text. */
    val textLength get() = text.original.text.length

    /** Whether the item was written by [currentUserProfile]. */
    fun isFromUser(currentUserProfile: Profile) = author?.userId == currentUserProfile.userId

    /** Whether [currentUserProfile] can delete the item: they wrote it and it is not moderated. */
    fun canBeDeleted(currentUserProfile: CurrentUserProfile?) =
        currentUserProfile != null &&
            isFromUser(currentUserProfile) &&
            status !is Status.Moderated

    /** Whether [currentUserProfile] can report the item: they did not write it and it is not moderated. */
    fun canBeReported(currentUserProfile: CurrentUserProfile?) =
        currentUserProfile != null &&
            !isFromUser(currentUserProfile) &&
            status !is Status.Moderated

    /**
     * Whether [currentUserProfile] can block the author: not themselves, not an admin and not already
     * blocked.
     */
    fun canAuthorBeBlocked(currentUserProfile: CurrentUserProfile?): Boolean {
        val author = author ?: return false
        if (currentUserProfile == null) return false
        if (isFromUser(currentUserProfile)) return false
        if (Profile.Tag.ADMIN in author.tags) return false
        if (author.isBlocked(currentUserProfile.blockedProfiles)) return false
        return true
    }

    /** Whether [currentUserProfile] can delete, report or block anything on the item. */
    fun hasActions(currentUserProfile: CurrentUserProfile?) =
        canBeDeleted(currentUserProfile) ||
            canBeReported(currentUserProfile) ||
            canAuthorBeBlocked(currentUserProfile)
}
