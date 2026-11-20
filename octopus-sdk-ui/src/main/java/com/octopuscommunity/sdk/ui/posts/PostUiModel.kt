package com.octopuscommunity.sdk.ui.posts

import androidx.compose.runtime.Immutable
import com.octopuscommunity.sdk.domain.model.Author
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.domain.model.Moderation
import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.model.TranslatableText
import java.util.Date

/**
 * UI-layer model for a post. Consumed by the shared `PostCard` composable in both feed (summary)
 * and detail contexts. Equivalent to iOS `PostViewData`.
 *
 * Holds domain references (Author, TranslatableText, Date, …) rather than pre-resolved strings
 * so composables can apply the right locale / translation state at render time.
 */
@Immutable
data class PostUiModel(
    val id: String,
    val author: Author?,
    val creationDate: Date,
    val group: OctopusGroup?,
    val tags: Set<PostTag>,
    val content: PostContent,
    val reactions: List<OctopusItem.ReactionCount>,
    val userReaction: OctopusItem.Reaction?,
    val commentCount: Int,
    val viewCount: Int,
    val canBeDeleted: Boolean,
    val canBeReported: Boolean,
    val canCreateChildren: Boolean,
    val hasTranslation: Boolean,
)

enum class PostTag { Moderated }

sealed interface PostContent {
    @Immutable
    data class Published(
        val catchPhrase: TranslatableText?,
        val text: TranslatableText,
        val media: PostMedia?,
        val poll: PostPoll?,
        val cta: PostCta?,
    ) : PostContent

    @Immutable
    data class Moderated(
        val reasons: List<Moderation.Reason>,
    ) : PostContent
}

/**
 * Media shown in a published post: its images, else its video. Independent of [PostPoll]: a post
 * can carry images and a poll together.
 */
sealed interface PostMedia {
    /** The former single-image model; use [Images] for posts with up to ten photos. */
    @Deprecated("Use Images.", ReplaceWith("PostMedia.Images(listOf(media))"))
    @Immutable
    data class Image(
        val media: Media.Image,
    ) : PostMedia

    /** The images of the post, never empty, in the order the backend sent them. */
    @Immutable
    data class Images(
        val medias: List<Media.Image>,
    ) : PostMedia

    @Immutable
    data class Video(
        val media: Media.Video,
    ) : PostMedia
}

/** Poll shown in a published post, with the current user's vote. Independent of [PostMedia]. */
@Immutable
data class PostPoll(
    val poll: Post.Poll,
    val currentUserVote: Post.Poll.Answer?,
)

@Immutable
data class PostCta(
    val text: TranslatableText,
    val action: Action,
) {
    sealed interface Action {
        @Immutable
        data class Bridge(
            val clientObjectId: String,
        ) : Action

        @Immutable
        data class OpenUrl(
            val url: String,
        ) : Action
    }
}

/** Images from either version of the published UI model. */
@Suppress("DEPRECATION")
internal val PostMedia.imageMedias: List<Media.Image>?
    get() =
        when (this) {
            is PostMedia.Image -> listOf(media)
            is PostMedia.Images -> medias
            is PostMedia.Video -> null
        }
