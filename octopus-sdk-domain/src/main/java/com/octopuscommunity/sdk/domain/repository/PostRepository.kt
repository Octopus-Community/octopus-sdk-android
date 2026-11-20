package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.ClientPost
import com.octopuscommunity.sdk.domain.model.CreationSource
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.OctopusReactionKind
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.ServerError
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing posts within the Octopus community.
 *
 * The PostRepository provides comprehensive functionality for creating, retrieving, and managing
 * posts in the Octopus community system. It supports both regular community posts and client
 * object posts (linked to external app content), along with interactive features like poll voting.
 *
 * ## Key Features:
 * - **Post Management**: Create, read, update, and delete community posts
 * - **Client Object Integration**: Link posts to external app content (articles, products, etc.)
 * - **Poll Interactions**: Handle poll voting and results
 * - **Real-time Updates**: Reactive streams for live post data
 * - **Error Handling**: Comprehensive error types for different failure scenarios
 *
 * ## Usage Examples:
 * ```kotlin
 * // Create a regular post
 * val draft = Post.Draft(
 *     text = "What's your favorite recipe?",
 *     topic = cookingTopic
 * )
 * val result = postRepository.create(draft)
 *
 * // Monitor client object post
 * postRepository.getClientObjectPostFlow("article-123")
 *     .collect { post ->
 *         // Update UI with post data
 *         updateUI(post)
 *     }
 *
 * // Vote on a poll
 * val voteResult = postRepository.pollVote(post, selectedAnswer)
 * ```
 *
 * @see Post for post data model
 * @see ClientPost for client object post creation
 * @see PostCreationError for detailed error handling
 */
interface PostRepository : OctopusItemRepository<Post, Post.Draft, PostCreationError> {
    suspend fun fetchCounters(
        posts: Collection<Post>,
        incrementViewCount: Boolean = false,
    ) = fetchCounters(
        itemIds = posts.map { it.id },
        childIds = posts.mapNotNull { it.featuredComment?.id },
        videoItemIds = posts.mapNotNull { it.takeIf { it.hasVideo }?.id },
        incrementViewCount = incrementViewCount,
    )

    /**
     * Gets a reactive stream of a post associated with a specific client object.
     *
     * This method returns a Flow that emits the current state of the post linked to the
     * specified client object ID. The Flow will emit null if no post exists for the given
     * object, and will automatically emit updates when the post data changes.
     *
     * This is particularly useful for monitoring posts linked to external app content
     * (articles, products, etc.) and updating the UI in real-time as engagement metrics
     * change or new comments are added.
     *
     * ## Usage Examples:
     * ```kotlin
     * // Monitor post for an article
     * postRepository.getClientObjectPostFlow("article-123")
     *     .collect { post ->
     *         when (post) {
     *             null -> showCreatePostButton()
     *             else -> showPostEngagement(post.likeCount, post.commentCount)
     *         }
     *     }
     *
     * // Display post in UI
     * postRepository.getClientObjectPostFlow(productId)
     *     .collect { post ->
     *         binding.postCard.isVisible = post != null
     *         post?.let { displayPost(it) }
     *     }
     * ```
     *
     * ## Reactive Behavior:
     * - Emits immediately with current cached data (or null)
     * - Updates automatically when post data changes
     * - Continues emitting until Flow is cancelled
     * - Thread-safe and can be collected from multiple observers
     *
     * @param clientObjectId Unique identifier of the client object to monitor
     * @return Flow emitting the associated Post or null if no post exists
     */
    fun getClientObjectPostFlow(clientObjectId: String): Flow<Post?>

    /**
     * Creates or retrieves an existing post for a client object.
     *
     * This method handles the creation of posts linked to external app content. If a post
     * already exists for the given client object ID, it returns the existing post without
     * modification. If no post exists, it creates a new one using the provided content.
     *
     * This is the primary method for integrating community discussions around specific
     * app content such as articles, products, recipes, or any other domain-specific objects.
     *
     * ## Behavior:
     * - **Existing Post**: Returns immediately if post already exists
     * - **New Post**: Creates post with provided content and returns it
     * - **Validation**: Validates all content before creation
     * - **Topic Assignment**: Associates with specified topic or default
     *
     * ## Usage Examples:
     * ```kotlin
     * // Create post for article
     * val articlePost = ClientPost(
     *     objectId = "article-123",
     *     text = "Check out this comprehensive guide to Kotlin coroutines!",
     *     catchPhrase = "What's your experience with coroutines?",
     *     viewObjectButton = OctopusLocalizedText.AutoTranslated("Read Article"),
     *     groupId = "programming"
     * )
     *
     * when (val result = postRepository.fetchOrCreateClientObjectPost(articlePost)) {
     *     is OctopusResult.Success -> {
     *         // Navigate to post details
     *         navigateToPost(result.data.id)
     *     }
     *     is OctopusResult.Error -> {
     *         when (result.error) {
     *             is ClientPostError.TextError.Missing -> showError("Post text is required")
     *             is ClientPostError.FileError.FileSizeTooBig -> showError("Image too large")
     *             is ClientPostError.TokenError.Invalid -> showError("Authentication failed")
     *             else -> showError("Failed to create post")
     *         }
     *     }
     * }
     *
     * // Create product showcase post
     * val productPost = ClientPost(
     *     objectId = "product-456",
     *     text = "Introducing our eco-friendly water bottle collection!",
     *     medias = listOf(Resource.Remote("https://example.com/bottle.jpg")),
     *     catchPhrase = "Would you switch to eco-friendly bottles?",
     *     // The button text in each language of the app; other readers get the English one, translated.
     *     viewObjectButton = OctopusLocalizedText.PerLanguage(
     *         texts = mapOf(Locale.ENGLISH to "Shop Now", Locale.FRENCH to "Acheter"),
     *         defaultLanguage = Locale.ENGLISH,
     *     ),
     *     groupId = "sustainability"
     * )
     * ```
     *
     * ## Error Handling:
     * The method returns detailed error information for various failure scenarios:
     * - **Text Errors**: Missing content, character limits, etc.
     * - **File Errors**: Image upload issues, format problems
     * - **Token Errors**: Authentication and authorization failures
     * - **Client Object Errors**: Invalid IDs, missing data
     *
     * @param clientPost The content and metadata for the post to create or retrieve
     * @return OctopusResult containing the Post on success, or ClientPostError on failure
     * @see ClientPost for detailed parameter documentation
     * @see ClientPostError for comprehensive error handling
     */
    suspend fun fetchOrCreateClientObjectPost(
        clientPost: ClientPost,
        tokenProvider: (suspend (String) -> String?)? = null,
    ): OctopusResult<Post, ClientPostError>

    suspend fun sendPost(
        draftItem: Post.Draft,
        creationSource: CreationSource = CreationSource.USER,
    ): OctopusResult<Post, PostCreationError>

    suspend fun deletePost(post: Post): OctopusResult<Unit, Nothing>

    /**
     * Submits a vote for a specific answer in a post's poll.
     *
     * This method allows users to participate in interactive polls by voting for their
     * preferred answer. The vote is immediately submitted to the server and the post's
     * poll results are updated in real-time for all users viewing the post.
     *
     * ## Voting Rules:
     * - **One Vote Per User**: Users can only vote once per poll
     * - **Vote Replacement**: New votes replace previous votes from the same user
     * - **Real-time Updates**: Poll results update immediately after voting
     * - **Validation**: Ensures post and answer exist before submitting vote
     *
     * ## Usage Examples:
     * ```kotlin
     * // Handle poll voting in UI
     * poll.answers.forEach { answer ->
     *     answerButton.setOnClickListener {
     *         viewLifecycleOwner.lifecycleScope.launch {
     *             when (val result = postRepository.pollVote(post, answer)) {
     *                 is OctopusResult.Success -> {
     *                     // Vote successful - UI will update via reactive streams
     *                     showVoteConfirmation(answer.text)
     *                 }
     *                 is OctopusResult.Error -> {
     *                     when (result.error) {
     *                         is PollVoteError.PostUnavailable ->
     *                             showError("This poll is no longer available")
     *                         is PollVoteError.MissingPollAnswer ->
     *                             showError("Invalid poll option selected")
     *                         else -> showError("Failed to submit vote")
     *                     }
     *                 }
     *             }
     *         }
     *     }
     * }
     *
     * // Vote with error handling
     * suspend fun submitPollVote(post: Post, selectedAnswer: Post.Poll.Answer) {
     *     val result = postRepository.pollVote(post, selectedAnswer)
     *
     *     when (result) {
     *         is OctopusResult.Success -> {
     *             analytics.track("poll_vote_submitted", mapOf(
     *                 "post_id" to post.id,
     *                 "answer_id" to selectedAnswer.id
     *             ))
     *         }
     *         is OctopusResult.Error -> {
     *             analytics.track("poll_vote_failed", mapOf(
     *                 "error" to result.error.errorMessage
     *             ))
     *         }
     *     }
     * }
     * ```
     *
     * ## State Management:
     * After a successful vote:
     * - Post's `pollVoteId` is updated with the user's vote
     * - Poll's `results` are updated with new vote counts
     * - All observers of the post receive updated data
     * - UI components should reflect the new voting state
     *
     * @param post The post containing the poll to vote on
     * @param answer The specific poll answer to vote for
     * @return OctopusResult indicating success or failure with detailed error information
     * @see PollVoteError for detailed error types
     * @see Post.Poll for poll data structure
     * @see Post.currentUserVote for checking existing votes
     */
    suspend fun pollVote(
        post: Post,
        answer: Post.Poll.Answer,
        isPostTranslated: Boolean,
    ): OctopusResult<Post, PollVoteError>

    /**
     * Sets a reaction on the user's behalf on the post identified by [postId].
     *
     * Works on any post the current user can see — bridge posts (client object related posts) or
     * community-originated posts.
     *
     * Passing `null` for [reactionKind] removes the current user's active reaction. Calling with
     * `null` when no reaction is currently set is a silent no-op (returns
     * [OctopusResult.Success] with `null` data). Setting the same reaction twice is idempotent.
     *
     * Network and authentication failures surface as [OctopusResult.Failure.Connection.*]; only
     * reaction-domain errors land in [SetReactionError].
     *
     * @param reactionKind The reaction to set. Pass `null` to remove the current user's reaction.
     * Unknown reactions are not supported and yield
     * [SetReactionError.UnknownReaction].
     * @param postId The id of the post to react on.
     * @return [OctopusResult] indicating success or failure.
     */
    suspend fun setReaction(
        reactionKind: OctopusReactionKind?,
        postId: String,
    ): OctopusResult<OctopusItem.Reaction?, SetReactionError>
}

/**
 * Comprehensive error types for post creation operations.
 *
 * PostCreationError provides detailed error information when creating regular community posts.
 * Each error type includes specific context about what went wrong, enabling apps to provide
 * meaningful feedback to users and handle different scenarios appropriately.
 *
 * ## Error Categories:
 * - **Topic Errors**: Issues with topic selection or availability
 * - **Text Errors**: Content validation and moderation issues
 * - **File Errors**: Media upload and processing problems
 * - **Poll Errors**: Poll configuration and validation issues
 * - **Other Errors**: Miscellaneous or unexpected errors
 *
 * ## Usage Examples:
 * ```kotlin
 * when (val result = postRepository.create(draft)) {
 *     is OctopusResult.Error -> {
 *         when (result.error) {
 *             is PostCreationError.TextError.MaxCharLimitReached -> {
 *                 showError("Post text is too long (max ${Post.Draft.TEXT_LENGTH_RANGE.last} characters)")
 *                 highlightTextLengthError()
 *             }
 *             is PostCreationError.TextError.BannedWords -> {
 *                 showError("Please remove inappropriate words: ${result.error.bannedWords.joinToString()}")
 *                 highlightBannedWords(result.error.bannedWords)
 *             }
 *             is PostCreationError.FileError.FileSizeTooBig -> {
 *                 showError("Image file is too large. Please choose a smaller image.")
 *                 showImageCompressionOptions()
 *             }
 *             is PostCreationError.GroupError.Missing -> {
 *                 showError("Please select a group for your post")
 *                 focusGroupSelector()
 *             }
 *         }
 *     }
 * }
 * ```
 */
sealed interface PostCreationError : OctopusItem.CreationError {
    /**
     * Errors related to group selection and validation.
     *
     * Group errors occur when there are issues with the selected group for the post,
     * such as missing group selection or referencing non-existent groups.
     */
    sealed class GroupError : PostCreationError {
        /**
         * No group was selected when one is required.
         *
         * @property errorMessage Human-readable error description
         */
        data class Missing(
            override val errorMessage: String,
        ) : GroupError()

        /**
         * The selected group ID does not exist or is not available.
         *
         * @property errorMessage Human-readable error description
         */
        data class Unknown(
            override val errorMessage: String,
        ) : GroupError()
    }

    /**
     * Errors related to post text content validation and moderation.
     *
     * Text errors cover content validation, length restrictions, and content moderation
     * issues that prevent the post from being created or published.
     */
    sealed class TextError : PostCreationError {
        /**
         * Post text content is missing or empty.
         *
         * @property errorMessage Human-readable error description
         */
        data class Missing(
            override val errorMessage: String,
        ) : TextError()

        /**
         * Post text exceeds the maximum character limit.
         *
         * @property errorMessage Human-readable error description
         * @see Post.Draft.TEXT_LENGTH_RANGE for valid character limits
         */
        data class MaxCharLimitReached(
            override val errorMessage: String,
        ) : TextError()

        /**
         * Post text contains words that are banned by content moderation.
         *
         * @property errorMessage Human-readable error description
         * @property bannedWords List of specific words that triggered the moderation
         */
        data class BannedWords(
            override val errorMessage: String,
            val bannedWords: List<String>,
        ) : TextError()

        /**
         * Unknown text-related error occurred during validation.
         *
         * @property errorMessage Human-readable error description
         */
        data class Unknown(
            override val errorMessage: String,
        ) : TextError()
    }

    /**
     * Errors related to file upload and media processing.
     *
     * File errors occur during media attachment processing, including upload failures,
     * format validation, and size restrictions.
     */
    sealed class FileError : PostCreationError {
        /**
         * The image this error is about, counted from 0 in the order of [Post.Draft.medias];
         * `null` when the error is about no image in particular.
         *
         * It is not part of [equals], [hashCode] or `copy`: two errors that differ only by their
         * index are equal, and a copy is about no image in particular.
         */
        var imageIndex: Int? = null
            protected set

        /**
         * The uploaded file is empty or has no content.
         *
         * @property errorMessage Human-readable error description
         */
        data class EmptyFile(
            override val errorMessage: String,
        ) : FileError() {
            /** An empty-file error about the image at [imageIndex], or about no image in particular when `null`. */
            constructor(errorMessage: String, imageIndex: Int?) : this(errorMessage) {
                this.imageIndex = imageIndex
            }
        }

        /**
         * The file size exceeds the maximum allowed limit.
         *
         * @property errorMessage Human-readable error description
         * @see Post.Draft.IMAGE_UPLOAD_MAX_SIZE for size limits
         */
        data class FileSizeTooBig(
            override val errorMessage: String,
        ) : FileError() {
            /** A file-size error about the image at [imageIndex], or about no image in particular when `null`. */
            constructor(errorMessage: String, imageIndex: Int?) : this(errorMessage) {
                this.imageIndex = imageIndex
            }
        }

        /**
         * The file format is not supported or is corrupted.
         *
         * @property errorMessage Human-readable error description
         */
        data class BadFileFormat(
            override val errorMessage: String,
        ) : FileError() {
            /** A file-format error about the image at [imageIndex], or about no image in particular when `null`. */
            constructor(errorMessage: String, imageIndex: Int?) : this(errorMessage) {
                this.imageIndex = imageIndex
            }
        }

        /**
         * An error occurred during the file upload process.
         *
         * @property errorMessage Human-readable error description
         */
        data class UploadIssue(
            override val errorMessage: String,
        ) : FileError() {
            /** An upload error about the image at [imageIndex], or about no image in particular when `null`. */
            constructor(errorMessage: String, imageIndex: Int?) : this(errorMessage) {
                this.imageIndex = imageIndex
            }
        }

        /**
         * Required file metadata or details are missing.
         *
         * @property errorMessage Human-readable error description
         */
        data class DetailsNotSet(
            override val errorMessage: String,
        ) : FileError() {
            /** A missing-details error about the image at [imageIndex], or about no image in particular when `null`. */
            constructor(errorMessage: String, imageIndex: Int?) : this(errorMessage) {
                this.imageIndex = imageIndex
            }
        }

        /**
         * Unknown file-related error occurred during processing.
         *
         * @property errorMessage Human-readable error description
         */
        data class Unknown(
            override val errorMessage: String,
        ) : FileError() {
            /** An unknown file error about the image at [imageIndex], or about no image in particular when `null`. */
            constructor(errorMessage: String, imageIndex: Int?) : this(errorMessage) {
                this.imageIndex = imageIndex
            }
        }
    }

    /**
     * Errors related to poll configuration and validation.
     *
     * Poll errors occur when there are issues with poll setup, answer validation,
     * or poll structure requirements.
     */
    sealed class PollError : PostCreationError {
        /**
         * Poll question or answer text exceeds character limits.
         *
         * @property errorMessage Human-readable error description
         * @see Post.Draft.Poll.Answer.TEXT_LENGTH_RANGE for answer limits
         */
        data class MaxCharLimitReached(
            override val errorMessage: String,
        ) : PollError()

        /**
         * Invalid number of poll answers (too few or too many).
         *
         * @property errorMessage Human-readable error description
         * @see Post.Draft.Poll.ANSWER_COUNT_RANGE for valid answer counts
         */
        data class BadPollAnswerNumber(
            override val errorMessage: String,
        ) : PollError()

        /**
         * Unknown poll-related error occurred during validation.
         *
         * @property errorMessage Human-readable error description
         */
        data class Unknown(
            override val errorMessage: String,
        ) : PollError()
    }

    sealed class CatchPhraseError : PostCreationError {
        data class MaxCharLimitReached(
            override val errorMessage: String,
        ) : CatchPhraseError()

        data class Unknown(
            override val errorMessage: String,
        ) : CatchPhraseError()
    }

    sealed class CtaError : PostCreationError {
        data class MaxCharLimitReached(
            override val errorMessage: String,
        ) : CtaError()

        data class InvalidAuthor(
            override val errorMessage: String,
        ) : CtaError()

        data class MissingText(
            override val errorMessage: String,
        ) : CtaError()

        data class MissingTarget(
            override val errorMessage: String,
        ) : CtaError()

        data class InvalidTarget(
            override val errorMessage: String,
        ) : CtaError()

        data class Unknown(
            override val errorMessage: String,
        ) : CtaError()
    }

    sealed class ClientObjectError : PostCreationError {
        data class MissingClientObjectId(
            override val errorMessage: String,
        ) : ClientObjectError()

        data class Unknown(
            override val errorMessage: String,
        ) : ClientObjectError()
    }

    /**
     * Miscellaneous errors that don't fit into specific categories.
     *
     * @property errorMessage Human-readable error description
     */
    data class PublicationRateLimitExceeded(
        override val errorMessage: String,
        override val waitInSec: Long,
    ) : PostCreationError,
        OctopusItem.RateLimitExceedError

    data class OtherError(
        override val errorMessage: String,
    ) : PostCreationError
}

/**
 * Comprehensive error types for client object post operations.
 *
 * ClientPostError provides detailed error information when creating posts linked to external
 * app content. These errors are specifically tailored to client object integration scenarios
 * and include additional validation for external content references.
 *
 * ## Error Categories:
 * - **Text Errors**: Content validation issues
 * - **File Errors**: Media processing problems (including remote image downloads)
 * - **Client Object Errors**: External content reference issues
 * - **Token Errors**: Authentication and authorization failures
 * - **Other Errors**: Miscellaneous or unexpected errors
 *
 * ## Usage Examples:
 * ```kotlin
 * when (val result = postRepository.fetchOrCreateClientObjectPost(clientPost)) {
 *     is OctopusResult.Error -> {
 *         when (result.error) {
 *             is ClientPostError.ClientObjectError.MissingId -> {
 *                 showError("Object ID is required for linked posts")
 *             }
 *             is ClientPostError.TokenError.Expired -> {
 *                 refreshAuthToken()
 *                 retryPostCreation()
 *             }
 *             is ClientPostError.FileError.DownloadIssue -> {
 *                 showError("Unable to process the provided image URL")
 *                 offerLocalImageUpload()
 *             }
 *         }
 *     }
 * }
 * ```
 */
sealed interface ClientPostError : OctopusItem.CreationError {
    /**
     * Errors related to client post text content validation.
     *
     * Similar to regular post text errors but specific to client object posts.
     */
    sealed class TextError : ClientPostError {
        /**
         * Post text content is missing or empty.
         *
         * @property errorMessage Human-readable error description
         */
        data class Missing(
            override val errorMessage: String,
        ) : TextError()

        /**
         * Post text exceeds the maximum character limit.
         *
         * @property errorMessage Human-readable error description
         */
        data class MaxCharLimitReached(
            override val errorMessage: String,
        ) : TextError()

        /**
         * Unknown text-related error occurred during validation.
         *
         * @property errorMessage Human-readable error description
         */
        data class Unknown(
            override val errorMessage: String,
        ) : TextError()
    }

    /**
     * Errors related to file processing for client object posts.
     *
     * Includes both upload errors and remote image download issues specific
     * to client object post attachments.
     */
    sealed class FileError : ClientPostError {
        /**
         * The uploaded or referenced file is empty.
         *
         * @property errorMessage Human-readable error description
         */
        data class EmptyFile(
            override val errorMessage: String,
        ) : FileError()

        /**
         * The file size exceeds the maximum allowed limit.
         *
         * @property errorMessage Human-readable error description
         */
        data class FileSizeTooBig(
            override val errorMessage: String,
        ) : FileError()

        /**
         * The file format is not supported or is corrupted.
         *
         * @property errorMessage Human-readable error description
         */
        data class BadFileFormat(
            override val errorMessage: String,
        ) : FileError()

        /**
         * An error occurred during the file upload process.
         *
         * @property errorMessage Human-readable error description
         */
        data class UploadIssue(
            override val errorMessage: String,
        ) : FileError()

        /**
         * Failed to download or process a remote image URL.
         *
         * @property errorMessage Human-readable error description
         */
        data class DownloadIssue(
            override val errorMessage: String,
        ) : FileError()

        /**
         * Unknown file-related error occurred during processing.
         *
         * @property errorMessage Human-readable error description
         */
        data class Unknown(
            override val errorMessage: String,
        ) : FileError()
    }

    /**
     * Errors related to client object configuration and validation.
     *
     * These errors are specific to the external content linking functionality
     * and validate the relationship between posts and app content.
     */
    sealed class ClientObjectError : ClientPostError {
        /**
         * Client object ID is missing or empty.
         *
         * @property errorMessage Human-readable error description
         */
        data class MissingId(
            override val errorMessage: String,
        ) : ClientObjectError()

        /**
         * Call-to-action text is missing when required.
         *
         * @property errorMessage Human-readable error description
         */
        data class MissingCta(
            override val errorMessage: String,
        ) : ClientObjectError()

        /**
         * The bridge post exists but is unavailable (e.g. moderated or deleted).
         *
         * @property errorMessage Human-readable error description
         */
        data class PostUnavailable(
            override val errorMessage: String,
        ) : ClientObjectError()

        /**
         * The bridge post does not exist yet for this client object.
         *
         * @property errorMessage Human-readable error description
         */
        data class PostNotFound(
            override val errorMessage: String,
        ) : ClientObjectError()

        /**
         * A bridge post already exists for this client object.
         *
         * @property errorMessage Human-readable error description
         */
        data class PostAlreadyExists(
            override val errorMessage: String,
        ) : ClientObjectError()

        /**
         * The specified group ID is invalid or not available.
         *
         * @property errorMessage Human-readable error description
         */
        data class InvalidGroupId(
            override val errorMessage: String,
        ) : ClientObjectError()

        /**
         * The author is not authorized to create posts for this object.
         *
         * @property errorMessage Human-readable error description
         */
        data class InvalidAuthor(
            override val errorMessage: String,
        ) : ClientObjectError()

        /**
         * Unknown client object-related error occurred.
         *
         * @property errorMessage Human-readable error description
         */
        data class Unknown(
            override val errorMessage: String,
        ) : ClientObjectError()
    }

    /**
     * Errors related to authentication token validation.
     *
     * Token errors occur when the provided signature for client object post
     * creation fails validation or authorization checks.
     */
    sealed class TokenError : ClientPostError {
        /**
         * The provided authentication token is invalid or malformed.
         *
         * @property errorMessage Human-readable error description
         */
        data class Invalid(
            override val errorMessage: String,
        ) : TokenError()

        /**
         * The authentication token has expired and needs to be refreshed.
         *
         * @property errorMessage Human-readable error description
         */
        data class Expired(
            override val errorMessage: String,
        ) : TokenError()

        /**
         * Unknown token-related error occurred during validation.
         *
         * @property errorMessage Human-readable error description
         */
        data class Unknown(
            override val errorMessage: String,
        ) : TokenError()
    }

    /**
     * Miscellaneous errors that don't fit into specific categories.
     *
     * @property errorMessage Human-readable error description
     */
    data class OtherError(
        override val errorMessage: String,
    ) : ClientPostError
}

/**
 * Error types specific to poll voting operations.
 *
 * PollVoteError provides detailed information when poll voting fails, helping apps
 * provide appropriate feedback and handle different failure scenarios gracefully.
 *
 * ## Common Scenarios:
 * - Post no longer exists or is unavailable
 * - Selected poll answer is invalid or missing
 * - Network or server issues during vote submission
 *
 * ## Usage Examples:
 * ```kotlin
 * when (val result = postRepository.pollVote(post, answer)) {
 *     is OctopusResult.Error -> {
 *         when (result.error) {
 *             is PollVoteError.PostUnavailable -> {
 *                 showError("This poll is no longer available")
 *                 navigateBack()
 *             }
 *             is PollVoteError.MissingPollAnswer -> {
 *                 showError("Please select a valid poll option")
 *                 refreshPollOptions()
 *             }
 *             is PollVoteError.Unknown -> {
 *                 showError("Unable to submit vote. Please try again.")
 *                 enableRetryButton()
 *             }
 *         }
 *     }
 * }
 * ```
 */
sealed class PollVoteError : ServerError {
    /**
     * The post containing the poll is no longer available or accessible.
     *
     * This can occur if the post was deleted, made private, or is temporarily unavailable.
     *
     * @property errorMessage Human-readable error description
     */
    data class PostUnavailable(
        override val errorMessage: String,
    ) : PollVoteError()

    /**
     * The selected poll answer is missing or invalid.
     *
     * This can occur if the poll structure changed between loading and voting,
     * or if an invalid answer ID was provided.
     *
     * @property errorMessage Human-readable error description
     */
    data class MissingPollAnswer(
        override val errorMessage: String,
    ) : PollVoteError()

    /**
     * Unknown error occurred during poll voting.
     *
     * This covers unexpected server errors, network issues, or other
     * unforeseen problems during vote submission.
     *
     * @property errorMessage Human-readable error description
     */
    data class Unknown(
        override val errorMessage: String,
    ) : PollVoteError()
}

/**
 * Error types for setting a reaction on a post.
 *
 * Surfaced by [PostRepository.setReaction] and by the public
 * [com.octopuscommunity.sdk.OctopusSDK.setReaction] (reaction, postId) function. Network and
 * authentication failures are surfaced separately as
 * [OctopusResult.Failure.Connection] (e.g. [OctopusResult.Failure.Connection.NoNetwork],
 * [OctopusResult.Failure.Connection.UserNotAuthenticated]).
 *
 * ## Error Cases:
 * - **UnknownReaction**: An unknown reaction kind was passed (not supported)
 * - **PostNotFound**: The post with the given ID was not found locally or remotely, or the user
 *   has no read access to it
 * - **ReactionError**: An error occurred while setting the reaction on the server
 */
sealed interface SetReactionError : ServerError {
    /**
     * An unknown reaction kind was passed. Only known reaction kinds are supported.
     */
    data object UnknownReaction : SetReactionError {
        override val errorMessage = "Unknown reaction not permitted"
    }

    /**
     * The post with the given ID was not found, or the current user has no read access to it.
     */
    data object PostNotFound : SetReactionError {
        override val errorMessage = "Post not found"
    }

    /**
     * An error occurred while setting the reaction.
     */
    data class ReactionError(
        override val errorMessage: String,
    ) : SetReactionError
}
