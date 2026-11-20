package com.octopuscommunity.sdk.domain.model

import com.octopuscommunity.sdk.domain.model.OctopusItem.ContentKind
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount
import com.octopuscommunity.sdk.domain.model.OctopusReactionKind.Companion.Clap
import com.octopuscommunity.sdk.domain.model.OctopusReactionKind.Companion.Cry
import com.octopuscommunity.sdk.domain.model.OctopusReactionKind.Companion.Heart
import com.octopuscommunity.sdk.domain.model.OctopusReactionKind.Companion.Joy
import com.octopuscommunity.sdk.domain.model.OctopusReactionKind.Companion.MouthOpen
import com.octopuscommunity.sdk.domain.model.OctopusReactionKind.Companion.Rage
import java.util.Collections
import java.util.Date
import java.util.UUID
import kotlin.random.Random

/**
 * A reaction kind on a content.
 *
 * Available kinds: [Heart], [Joy], [MouthOpen], [Clap], [Cry], [Rage].
 */
sealed interface OctopusReactionKind {
    val unicode: String

    companion object {
        val Heart: OctopusReactionKind = Reaction.Kind.Heart
        val Joy: OctopusReactionKind = Reaction.Kind.Joy
        val MouthOpen: OctopusReactionKind = Reaction.Kind.MouthOpen
        val Clap: OctopusReactionKind = Reaction.Kind.Clap
        val Cry: OctopusReactionKind = Reaction.Kind.Cry
        val Rage: OctopusReactionKind = Reaction.Kind.Rage
    }
}

/**
 * An Octopus post
 */
interface OctopusPost {
    /**
     * Id of the post.
     * This id should be used if you need to display the post (see `OctopusPostDetailsContent()`)
     */
    val id: String

    /**
     * The reaction count. If a reaction kind is missing from this array, it means that the reaction
     * count of this kind is 0.
     */
    val reactions: List<OctopusReactionCount>

    /**
     * This represents the overall number of **comments and replies** to this post.
     */
    val commentCount: Int

    /**
     * The view count for this content.
     */
    val viewCount: Int

    /**
     * The reaction kind of the current user on this post. Null if the user did not react to this
     * post.
     */
    val userReactionKind: OctopusReactionKind?
}

/** Number of reactions of one kind on a post. */
interface OctopusReactionCount {
    /** Kind of the reactions. */
    val reactionKind: OctopusReactionKind

    /** Number of reactions of this kind. */
    val count: Int
}

/**
 * Represents a post in the Octopus community.
 *
 * A Post is the primary content unit in the Octopus community system. It can contain text,
 * media attachments, polls, and can be associated with topics or client objects. Posts support
 * engagement features like reactions, comments, and provide detailed metrics for analytics.
 *
 * ## Key Features:
 * - Text content with rich formatting support
 * - Media attachments (images, videos)
 * - Interactive polls with multiple choice answers
 * - Topic categorization for content organization
 * - Client object integration for external content linking
 * - Engagement metrics (comments, views, reactions)
 * - Hierarchical commenting system
 * - Advanced emoji reactions
 *
 * @property id Unique identifier for the post
 * @property parentId Parent post ID for threaded discussions or topic ID for top-level posts
 * @property creationDate Timestamp when the post was first created
 * @property updateDate Timestamp when the post was last modified
 * @property status Current publication status of the post (published, draft, moderated, etc.)
 * @property author Information about the user who created the post
 * @property text The main text content of the post
 * @property medias Attached media content (images, videos, etc.)
 * @property poll Interactive poll attached to the post, if any
 * @property commentsFeeds Feed configuration for comments on this post
 * @property viewCount Number of times this post has been viewed
 * @property commentCount Total number of comments on this post
 * @property reactions List of reaction summaries (emoji type and count)
 * @property userReaction Current user's reaction, if any
 * @property pollVoteId ID of the current user's poll vote, if they have voted
 * @property group Group this post is categorized under, if any
 * @property clientObject Associated external object (article, product, etc.), if any
 */
data class Post(
    override val id: String,
    val parentId: String,
    override val creationDate: Date,
    override val updateDate: Date,
    override val localeLanguage: String,
    override val status: Status,
    override val author: Author?,
    override val text: TranslatableText,
    override val medias: Medias? = null,
    val poll: Poll? = null,
    val commentsFeeds: OctopusItem.Feeds,
    override val viewCount: Int,
    override val commentCount: Int,
    override val reactions: List<ReactionCount> = emptyList(),
    override val userReaction: Reaction? = null,
    val pollVoteId: String? = null,
    val group: OctopusGroup? = null,
    val clientObject: ClientObject?,
    val customAction: CustomAction?,
    val featuredComment: Comment? = null,
    /**
     * Whether the connected user has access to this post. `true` for posts in groups without an
     * access requirement, or when the user holds the required entitlement. `false` when the
     * user reaches a post belonging to a non-accessible group (e.g. via deeplink, push
     * notification): the SDK should render an access-denied placeholder instead of the body.
     */
    val canAccess: Boolean = true,
    /**
     * Whether the connected user can create children (comments) on this post. `false` mirrors
     * either the access denial above or a write restriction in the parent group (internal ticket).
     */
    val canCreateChildren: Boolean = true,
) : OctopusPost,
    OctopusItem {
    /**
     * Represents an interactive poll within a post.
     *
     * Polls allow users to vote on multiple choice questions. The Poll class handles
     * vote counting, percentage calculations, and provides access to both raw vote
     * counts and calculated percentages for each answer option.
     *
     * @property answers List of available answer options for the poll
     * @property results Vote count results for each answer option
     */
    data class Poll(
        val answers: List<Answer>,
        val results: List<Result> = listOf(),
    ) {
        /**
         * Represents a single answer option in a poll.
         *
         * @property id Unique identifier for this answer option
         * @property text Display text for this answer option
         */
        data class Answer(
            val id: String,
            val text: TranslatableText,
        )

        /**
         * Represents voting results for a specific answer.
         *
         * @property answerId ID of the answer this result corresponds to
         * @property voteCount Number of votes this answer has received
         */
        data class Result(
            val answerId: String,
            val voteCount: Int,
        )

        /**
         * Total number of votes cast across all poll answers.
         */
        val voteCount get() = results.sumOf { it.voteCount }

        /**
         * Calculates vote percentages for each answer, ensuring they sum to 100%.
         */
        private val votePercentages: Map<String, Int> by lazy {
            if (voteCount <= 0) {
                results.associate { it.answerId to 0 }
            } else {
                val percentageValues =
                    results.associate { it.answerId to (it.voteCount.toDouble() / voteCount) * 100.0 }

                val integerPercentages =
                    percentageValues
                        .mapValues {
                            it.value.toInt()
                        }.toMutableMap()

                val remainders =
                    percentageValues.map {
                        it.key to it.value - integerPercentages[it.key]!!
                    }

                val sum = integerPercentages.values.sum()
                val difference = 100 - sum
                if (difference > 0) {
                    val sortedRemainders = remainders.sortedByDescending { it.second }
                    for (i in 0 until difference) {
                        val key = sortedRemainders[i].first
                        integerPercentages[key] = integerPercentages[key]!! + 1
                    }
                }

                integerPercentages
            }
        }

        /**
         * Extension property to get the vote count for this specific answer.
         */
        val Answer.voteCount: Int
            get() = results.find { it.answerId == id }?.voteCount ?: 0

        /**
         * Extension property to get the vote percentage for this specific answer.
         */
        val Answer.votePercentage: Int get() = votePercentages[this.id] ?: 0
    }

    /**
     * Represents a draft version of a post being composed.
     *
     * Not a `data class` on purpose: it gained [medias] after it shipped, and a data class cannot keep
     * the constructor and `copy` it had before visible to Java and resolvable from Kotlin at once. It
     * keeps a data class's `equals`, `hashCode`, `toString` and `componentN`. [medias] has no default
     * value in the constructor and `copy` that take it, so every call resolves to exactly one overload.
     *
     * @property group Group to categorize this post under, if any
     * @property text Main text content of the draft post
     * @property media Deprecated single image. It holds only what its own parameter received: a draft
     * written with [medias] leaves it `null`. Read [images] instead.
     * @property poll Interactive poll to include with the post, if any
     * @property customAction Host-provided call-to-action of a prefilled post, if any
     * @property medias Images attached to the post, in display order, up to [MAX_IMAGE_COUNT]. Cannot
     * be set together with [media].
     */
    class Draft(
        val group: OctopusGroup? = null,
        override val text: String = "",
        media: Media.Image? = null,
        val poll: Poll? = null,
        val customAction: CustomAction? = null,
        val medias: List<Media.Image>,
    ) : OctopusItem.Draft {
        @Deprecated("Use medias.", level = DeprecationLevel.WARNING)
        override val media: Media.Image? = media

        init {
            require(media == null || medias.isEmpty()) { "Set medias only: media is deprecated." }
        }

        /** An empty draft, as the no-argument constructor of the data class it was. */
        constructor() : this(medias = emptyList())

        /** A draft carrying at most the one image [media]. */
        constructor(
            group: OctopusGroup? = null,
            text: String = "",
            media: Media.Image? = null,
            poll: Poll? = null,
            customAction: CustomAction? = null,
        ) : this(
            group = group,
            text = text,
            media = media,
            poll = poll,
            customAction = customAction,
            medias = emptyList(),
        )

        /**
         * A copy of this draft with the given fields replaced; [medias] is kept.
         *
         * @throws IllegalArgumentException when the copy would carry both [media] and [medias], such as
         *   a `media` passed to a draft that has `medias`.
         */
        @Suppress("DEPRECATION")
        fun copy(
            group: OctopusGroup? = this.group,
            text: String = this.text,
            media: Media.Image? = this.media,
            poll: Poll? = this.poll,
            customAction: CustomAction? = this.customAction,
        ): Draft =
            Draft(
                group = group,
                text = text,
                media = media,
                poll = poll,
                customAction = customAction,
                medias = medias,
            )

        /**
         * A copy of this draft with the given fields replaced, [medias] included.
         *
         * @throws IllegalArgumentException when the copy would carry both [media] and [medias], such as
         *   non-empty `medias` passed to a draft built with `media`: pass `media = null` alongside.
         */
        @Suppress("DEPRECATION")
        fun copy(
            group: OctopusGroup? = this.group,
            text: String = this.text,
            media: Media.Image? = this.media,
            poll: Poll? = this.poll,
            customAction: CustomAction? = this.customAction,
            medias: List<Media.Image>,
        ): Draft =
            Draft(
                group = group,
                text = text,
                media = media,
                poll = poll,
                customAction = customAction,
                medias = medias,
            )

        /** The group, as a data class's first component. */
        operator fun component1(): OctopusGroup? = group

        /** The text, as a data class's second component. */
        operator fun component2(): String = text

        /** The deprecated single image, as a data class's third component. */
        @Suppress("DEPRECATION")
        operator fun component3(): Media.Image? = media

        /** The poll, as a data class's fourth component. */
        operator fun component4(): Poll? = poll

        /** The call-to-action, as a data class's fifth component. */
        operator fun component5(): CustomAction? = customAction

        /** The images, as a data class's sixth component. */
        operator fun component6(): List<Media.Image> = medias

        @Suppress("DEPRECATION")
        override fun equals(other: Any?): Boolean =
            this === other ||
                (
                    other is Draft &&
                        group == other.group &&
                        text == other.text &&
                        media == other.media &&
                        poll == other.poll &&
                        customAction == other.customAction &&
                        medias == other.medias
                )

        @Suppress("DEPRECATION")
        override fun hashCode(): Int = listOf(group, text, media, poll, customAction, medias).hashCode()

        @Suppress("DEPRECATION")
        override fun toString(): String =
            "Draft(group=$group, text=$text, media=$media, poll=$poll, customAction=$customAction, medias=$medias)"

        /**
         * The images of the draft, in display order: [medias], or the deprecated [media] for a draft
         * written before [medias] existed.
         */
        @Suppress("DEPRECATION")
        val images: List<Media.Image> get() = medias.ifEmpty { listOfNotNull(media) }

        /** Whether the draft has a text or at least one image. */
        override val hasContent: Boolean get() = text.isNotEmpty() || images.isNotEmpty()

        enum class Type { TEXT, POLL }

        constructor(type: Type) : this(
            poll =
                when (type) {
                    Type.POLL -> Poll()
                    else -> null
                },
        )

        /**
         * Host-provided call-to-action attached to a prefilled post (Bridge
         * Share). The CTA travels invisibly through the editor and is
         * serialised to the proto `CTA` message at publish time. Distinct
         * from the read-side [Post.CustomAction] (which carries a
         * [TranslatableText] for multilingual display) because the host
         * supplies the label itself, a single one or one per language, and
         * only the proto wire needs to be set on the write path.
         *
         * A label given per language ([OctopusLocalizedText.PerLanguage]) travels as [label], the
         * label of the default language, [labelLanguage], the code of that language, and
         * [labelTranslations], the other labels. `copy` does not carry the last two: it builds an
         * auto-translated action.
         *
         * @property label Text shown on the published post's CTA button: the label of the default
         * language when it is given per language.
         * @property targetUrl URL opened when the button is tapped.
         */
        data class CustomAction(
            val label: String,
            val targetUrl: String,
        ) {
            /**
             * The code of the language of [label] (`en`, `pt-BR`), or `null` when the label is
             * auto-translated.
             */
            var labelLanguage: String? = null
                private set

            /**
             * The labels of the other languages by language code, [labelLanguage] excluded. Empty
             * when the label is auto-translated.
             */
            var labelTranslations: Map<String, String> = emptyMap()
                private set

            /**
             * An action whose label is given per language.
             *
             * @param label The label of the default language.
             * @param targetUrl URL opened when the button is tapped.
             * @param labelLanguage The code of the default language, or `null` (or blank) for an
             *   auto-translated label: [labelTranslations] is then ignored.
             * @param labelTranslations The labels of the other languages by language code.
             */
            constructor(
                label: String,
                targetUrl: String,
                labelLanguage: String?,
                labelTranslations: Map<String, String>,
            ) : this(label = label, targetUrl = targetUrl) {
                val language = labelLanguage?.takeIf { it.isNotBlank() }
                this.labelLanguage = language
                this.labelTranslations =
                    if (language == null) emptyMap() else Collections.unmodifiableMap(labelTranslations - language)
            }

            override fun equals(other: Any?): Boolean =
                this === other ||
                    (
                        other is CustomAction &&
                            label == other.label &&
                            targetUrl == other.targetUrl &&
                            labelLanguage == other.labelLanguage &&
                            labelTranslations == other.labelTranslations
                    )

            override fun hashCode(): Int = listOf(label, targetUrl, labelLanguage, labelTranslations).hashCode()

            override fun toString(): String =
                "CustomAction(label=$label, targetUrl=$targetUrl, labelLanguage=$labelLanguage, " +
                    "labelTranslations=$labelTranslations)"
        }

        /**
         * Represents a poll in draft form.
         *
         * @property answers List of answer options for the poll (default: 2 empty answers)
         */
        data class Poll(
            val answers: List<Answer> = List(2) { Answer() },
        ) {
            /**
             * Represents a single answer option in a draft poll.
             *
             * @property id Unique identifier for this answer (auto-generated UUID)
             * @property text Display text for this answer option
             */
            data class Answer(
                val id: String = UUID.randomUUID().toString(),
                val text: String = "",
            ) {
                companion object {
                    /**
                     * Valid character length range for poll answer text (1-100 characters).
                     */
                    val TEXT_LENGTH_RANGE = 1..100
                }
            }

            companion object {
                /**
                 * Valid character length range for poll question text (1-5000 characters).
                 */
                val TEXT_LENGTH_RANGE = 1..Post.Draft.TEXT_LENGTH_RANGE.last

                /**
                 * Valid range for the number of answers in a poll (2-7 answers).
                 */
                val ANSWER_COUNT_RANGE = 2..7
            }
        }

        /**
         * Parent ID for this draft, derived from the selected group.
         */
        override val parentId = group?.id

        companion object {
            /**
             * Valid character length range for post text content (1-5000 characters): one visible
             * character is enough. The editor checks it against the trimmed text, so a blank text
             * stays refused unless the post may omit its text.
             */
            val TEXT_LENGTH_RANGE = 1..5000

            /**
             * Maximum file size allowed for image uploads.
             */
            const val IMAGE_UPLOAD_MAX_SIZE = Media.Image.UPLOAD_MAX_DIMENSION
            const val IMAGE_UPLOAD_QUALITY = Media.Image.UPLOAD_QUALITY

            /** Maximum number of images a post carries. */
            const val MAX_IMAGE_COUNT = 10
        }
    }

    /**
     * Represents an external object linked to a post.
     *
     * Client objects allow posts to reference external content such as articles,
     * products, events, or any other app-specific content.
     *
     * @property id Unique identifier for the external object
     * @property catchPhrase Engaging message to encourage user interaction
     * @property ctaText Text displayed on the navigation button
     */
    data class ClientObject(
        val id: String,
        val catchPhrase: TranslatableText?,
        val ctaText: TranslatableText?,
    )

    /**
     * A custom action button (CTA) attached to a post or a topic.
     *
     * Used by both posts (per-post call-to-action authored at post creation time) and
     * topics (per-topic call-to-action provisioned by Octopus operators — internal ticket).
     * When the button is tapped, the SDK fires the host app's `onNavigateToUrl`
     * callback to decide how the URL should be opened (in-app vs. Chrome Custom Tabs).
     *
     * Use this nested type for post call-to-action values. The former top-level
     * `CustomAction` alias is deprecated and will be removed in 2.0.
     *
     * Both surfaces apply the same strict gate at the mapper boundary, matching iOS's
     * failable `CustomAction.init?` (internal ticket). The gate is enforced in two places —
     * keep them in sync if you touch either: `PostMapper.toCustomActionEntity`
     * (`octopus-sdk-data/.../mapper/PostMapper.kt`) and
     * `GroupMapper.toCustomActionEntity` (`octopus-sdk-data/.../mapper/GroupMapper.kt`).
     *
     * Both gates require, in this order: non-blank `text`, non-blank `targetLink`, and
     * `Uri.parse(targetLink).scheme != null`. A non-null `CustomAction` is therefore
     * guaranteed non-blank and `Uri.parse`-able; the post detail screen still guards
     * the tap with `?.targetUrl?.let { … }` for source-compat with pre-fix builds,
     * but no caller should ever observe a non-null `CustomAction` whose `targetUrl`
     * is `null`. Loosen either mapper gate and this guarantee breaks — update this
     * doc and the call-site guards at the same time.
     *
     * @property ctaText Text displayed on the action button.
     * @property targetUrl URL opened when the button is tapped. The nullable type
     * is kept for source-compat with pre-fix builds; in practice (post-internal ticket) the
     * two mapper gates above ensure a non-null `CustomAction` always carries a
     * non-blank, `Uri.parse`-able URL string.
     */
    data class CustomAction(
        val ctaText: TranslatableText,
        val targetUrl: String?,
    )

    override val userReactionKind: OctopusReactionKind? get() = userReaction?.kind

    override val contentKind = ContentKind.POST

    /**
     * Gets the current user's vote in the poll, if any.
     *
     * @return The Answer object the user voted for, or null if not voted
     */
    val currentUserVote get() = poll?.answers?.firstOrNull { it.id == pollVoteId }

    val topReactions =
        setOf(Reaction.Kind.Heart) +
            setOf(Reaction.Kind.Joy, Reaction.Kind.MouthOpen, Reaction.Kind.Clap)
                .shuffled(Random(id.hashCode()))
                .take(1)
}

/**
 * A Client Object (article, product, etc.) that can be linked to an OctopusPost.
 *
 * ClientPost is used to create posts that reference external content from your application.
 * This is particularly useful for integrating community discussions around specific app
 * content such as articles, products, recipes, events, or any other domain-specific objects.
 *
 * @property objectId Unique identifier for the linked object
 * @property text Main post content (1-5000 characters)
 * @property attachment Deprecated single image (local URI or remote URL). It holds only what its own
 * parameter received: a client post written with [medias] leaves it `null`. Read [images] instead.
 * @property catchPhrase Engaging call-to-action message
 * @property viewObjectButton Text of the navigation button, auto-translated or given per language;
 * `null` for no button. Keep each text under 28 characters so that it fits on one line: a longer one
 * is accepted, and the button can then take several lines. It is taken when the bridge post is
 * created: a post that already exists for [objectId] keeps its button as it was.
 * @property groupId Optional group for content categorization
 * @property medias Images of the post (local URIs or remote URLs), in display order, up to
 * [Post.Draft.MAX_IMAGE_COUNT]: more is refused before any network call. Cannot be set together with
 * [attachment].
 *
 * Not a `data class` on purpose: it gained [medias] after it shipped, and a data class cannot keep the
 * constructor and `copy` it had before visible to Java and resolvable from Kotlin at once. It keeps a
 * data class's `equals`, `hashCode`, `toString` and `componentN`. [medias] has no default value in the
 * constructor and `copy` that take it, so every call resolves to exactly one overload.
 *
 * The same holds for [viewObjectButton]: the constructor that takes it gives it no default value, and
 * takes it second, where the other constructors take the text, so that a Java call written against
 * them, `null` arguments included, still resolves to them. The constructors taking the button text as
 * a `String` mean `OctopusLocalizedText.AutoTranslated(viewObjectButtonText)`.
 */
class ClientPost private constructor(
    val objectId: String,
    val text: String,
    attachment: Resource?,
    val catchPhrase: String?,
    val viewObjectButton: OctopusLocalizedText?,
    val groupId: String?,
    val medias: List<Resource>,
) {
    @Deprecated("Use medias.", level = DeprecationLevel.WARNING)
    val attachment: Resource? = attachment

    /**
     * Text of the navigation button: the text itself when auto-translated, the text of the default
     * language when given per language ([viewObjectButton]).
     */
    val viewObjectButtonText: String? get() = viewObjectButton?.defaultText

    init {
        require(attachment == null || medias.isEmpty()) { "Set medias only: attachment is deprecated." }
    }

    /**
     * A client post whose button text is given per language, or auto-translated.
     *
     * @param objectId Unique identifier for the linked object.
     * @param viewObjectButton Text of the navigation button: [OctopusLocalizedText.PerLanguage] to
     *   give it in each language of the app, [OctopusLocalizedText.AutoTranslated] to let Octopus
     *   translate it, `null` for no button. Keep each text under 28 characters so that it fits on
     *   one line: a longer one is accepted, and the button can then take several lines. It is taken
     *   when the bridge post is created: a post that already exists for [objectId] keeps its button.
     * @param text Main post content (1-5000 characters).
     * @param catchPhrase Engaging call-to-action message.
     * @param groupId Optional group for content categorization.
     * @param medias Images of the post, in display order, up to [Post.Draft.MAX_IMAGE_COUNT].
     */
    constructor(
        objectId: String,
        viewObjectButton: OctopusLocalizedText?,
        text: String,
        catchPhrase: String? = null,
        groupId: String? = null,
        medias: List<Resource> = emptyList(),
    ) : this(
        objectId = objectId,
        text = text,
        attachment = null,
        catchPhrase = catchPhrase,
        viewObjectButton = viewObjectButton,
        groupId = groupId,
        medias = medias,
    )

    /** A client post with an auto-translated button text. */
    @Deprecated(VIEW_OBJECT_BUTTON_TEXT_DEPRECATION, level = DeprecationLevel.WARNING)
    constructor(
        objectId: String,
        text: String,
        attachment: Resource? = null,
        catchPhrase: String? = null,
        viewObjectButtonText: String? = null,
        groupId: String? = null,
        medias: List<Resource>,
    ) : this(
        objectId = objectId,
        text = text,
        attachment = attachment,
        catchPhrase = catchPhrase,
        viewObjectButton = viewObjectButtonText?.let { OctopusLocalizedText.AutoTranslated(it) },
        groupId = groupId,
        medias = medias,
    )

    /** A client post carrying at most the one image [attachment], with an auto-translated button text. */
    @Deprecated(VIEW_OBJECT_BUTTON_TEXT_DEPRECATION, level = DeprecationLevel.WARNING)
    constructor(
        objectId: String,
        text: String,
        attachment: Resource? = null,
        catchPhrase: String? = null,
        viewObjectButtonText: String? = null,
        groupId: String? = null,
    ) : this(
        objectId = objectId,
        text = text,
        attachment = attachment,
        catchPhrase = catchPhrase,
        viewObjectButton = viewObjectButtonText?.let { OctopusLocalizedText.AutoTranslated(it) },
        groupId = groupId,
        medias = emptyList(),
    )

    /**
     * The button a `copy` taking a `String` gives: this post's [viewObjectButton], per-language texts
     * included, when [viewObjectButtonText] is left as it is, else that text, auto-translated.
     */
    private fun buttonFor(viewObjectButtonText: String?): OctopusLocalizedText? =
        if (viewObjectButtonText == this.viewObjectButtonText) {
            viewObjectButton
        } else {
            viewObjectButtonText?.let { OctopusLocalizedText.AutoTranslated(it) }
        }

    /**
     * A copy of this client post with the given fields replaced; [medias] is kept, and so is
     * [viewObjectButton] unless [viewObjectButtonText] is given another text, which is auto-translated.
     * To change a per-language button, build a new client post.
     *
     * @throws IllegalArgumentException when the copy would carry both [attachment] and [medias], such
     *   as an `attachment` passed to a client post that has `medias`.
     */
    @Suppress("DEPRECATION")
    fun copy(
        objectId: String = this.objectId,
        text: String = this.text,
        attachment: Resource? = this.attachment,
        catchPhrase: String? = this.catchPhrase,
        viewObjectButtonText: String? = this.viewObjectButtonText,
        groupId: String? = this.groupId,
    ): ClientPost =
        ClientPost(
            objectId = objectId,
            text = text,
            attachment = attachment,
            catchPhrase = catchPhrase,
            viewObjectButton = buttonFor(viewObjectButtonText),
            groupId = groupId,
            medias = medias,
        )

    /**
     * A copy of this client post with the given fields replaced, [medias] included. [viewObjectButton]
     * is kept unless [viewObjectButtonText] is given another text, which is auto-translated.
     *
     * @throws IllegalArgumentException when the copy would carry both [attachment] and [medias], such
     *   as non-empty `medias` passed to a client post built with `attachment`: pass
     *   `attachment = null` alongside.
     */
    @Suppress("DEPRECATION")
    fun copy(
        objectId: String = this.objectId,
        text: String = this.text,
        attachment: Resource? = this.attachment,
        catchPhrase: String? = this.catchPhrase,
        viewObjectButtonText: String? = this.viewObjectButtonText,
        groupId: String? = this.groupId,
        medias: List<Resource>,
    ): ClientPost =
        ClientPost(
            objectId = objectId,
            text = text,
            attachment = attachment,
            catchPhrase = catchPhrase,
            viewObjectButton = buttonFor(viewObjectButtonText),
            groupId = groupId,
            medias = medias,
        )

    /** The object id, as a data class's first component. */
    operator fun component1(): String = objectId

    /** The text, as a data class's second component. */
    operator fun component2(): String = text

    /** The deprecated single image, as a data class's third component. */
    @Suppress("DEPRECATION")
    operator fun component3(): Resource? = attachment

    /** The catch phrase, as a data class's fourth component. */
    operator fun component4(): String? = catchPhrase

    /** The button text, as a data class's fifth component. */
    operator fun component5(): String? = viewObjectButtonText

    /** The group id, as a data class's sixth component. */
    operator fun component6(): String? = groupId

    /** The images, as a data class's seventh component. */
    operator fun component7(): List<Resource> = medias

    @Suppress("DEPRECATION")
    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is ClientPost &&
                    objectId == other.objectId &&
                    text == other.text &&
                    attachment == other.attachment &&
                    catchPhrase == other.catchPhrase &&
                    viewObjectButton == other.viewObjectButton &&
                    groupId == other.groupId &&
                    medias == other.medias
            )

    @Suppress("DEPRECATION")
    override fun hashCode(): Int = listOf(objectId, text, attachment, catchPhrase, viewObjectButton, groupId, medias).hashCode()

    @Suppress("DEPRECATION")
    override fun toString(): String =
        "ClientPost(objectId=$objectId, text=$text, attachment=$attachment, catchPhrase=$catchPhrase, " +
            "viewObjectButtonText=$viewObjectButtonText, groupId=$groupId, medias=$medias" +
            (if (viewObjectButton is OctopusLocalizedText.PerLanguage) ", viewObjectButton=$viewObjectButton" else "") +
            ")"

    /**
     * The images of the post, in display order: [medias], or the deprecated [attachment] for a client
     * post written before [medias] existed.
     */
    @Suppress("DEPRECATION")
    val images: List<Resource> get() = medias.ifEmpty { listOfNotNull(attachment) }

    companion object {
        private const val VIEW_OBJECT_BUTTON_TEXT_DEPRECATION =
            "Use the constructor taking viewObjectButton: OctopusLocalizedText.PerLanguage to give the button text " +
                "per language, OctopusLocalizedText.AutoTranslated to keep the automatic translation, null for no button."

        /**
         * Largest side, in pixels, that the local images of a client post were resized to before
         * upload. No longer applied: they are uploaded as the host supplies them, so that a host
         * backend can compute the bridge fingerprint from its own copy of each image.
         */
        @Deprecated(
            "No longer applied: the local images of a client post are uploaded as the host supplies them.",
            level = DeprecationLevel.WARNING,
        )
        const val IMAGE_UPLOAD_MAX_SIZE = Post.Draft.IMAGE_UPLOAD_MAX_SIZE

        /**
         * Quality the local images of a client post were compressed with before upload. No longer
         * applied: they are uploaded as the host supplies them, so that a host backend can compute
         * the bridge fingerprint from its own copy of each image.
         */
        @Deprecated(
            "No longer applied: the local images of a client post are uploaded as the host supplies them.",
            level = DeprecationLevel.WARNING,
        )
        const val IMAGE_UPLOAD_QUALITY = Post.Draft.IMAGE_UPLOAD_QUALITY
    }
}

// region Deprecated backward-compatible accessors

/** @suppress */
@Deprecated("Use group", ReplaceWith("group"))
val Post.topic: OctopusGroup? get() = group

/** @suppress */
@Deprecated("Use group", ReplaceWith("group"))
val Post.Draft.topic: OctopusGroup? get() = group

/** @suppress */
@Deprecated("Use groupId", ReplaceWith("groupId"))
val ClientPost.topicId: String? get() = groupId

// endregion
