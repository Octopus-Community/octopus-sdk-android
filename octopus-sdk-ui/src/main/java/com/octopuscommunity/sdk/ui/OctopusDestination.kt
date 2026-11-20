package com.octopuscommunity.sdk.ui

import androidx.annotation.Keep
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.model.ProfileField
import kotlinx.serialization.Serializable

/**
 * Sealed interface representing all possible navigation destinations in the Octopus SDK.
 *
 * Each destination is serializable to support type-safe navigation with the Navigation Component.
 * Destinations are organized by feature area: Home, Posts, Comments, Replies, Connection,
 * Profile, Moderation, and Settings.
 */
sealed interface OctopusDestination {
    @Keep
    @Serializable
    enum class Origin {
        CLIENT_APP,
        SDK,
    }

    // Navigation Graphs

    /**
     * Root navigation graph for the Octopus SDK.
     *
     * This was the entry point for the entire Octopus navigation structure when using
     * nested navigation graphs. Now that we use composables directly, this is no longer needed.
     *
     * @deprecated No longer needed since we moved from nested navigation graphs to direct composables.
     * Use individual destinations like [Home] directly instead.
     */
    @Deprecated(
        message = "OctopusGraph is no longer needed. Use individual destinations like Home directly instead.",
        level = DeprecationLevel.ERROR,
    )
    @Serializable
    data object OctopusGraph : OctopusDestination

    // Home

    /**
     * Home screen destination.
     *
     * The main landing screen that displays the community feed and primary navigation.
     */
    @Serializable
    data object Home : OctopusDestination

    // Groups

    /**
     * Groups management screen destination.
     *
     * Screen for browsing and managing followed topics/groups.
     */
    @Serializable
    data object Groups : OctopusDestination

    /**
     * Group feed screen destination.
     *
     * Displays posts filtered by a specific group.
     *
     * @property groupId Unique identifier of the group
     */
    @Serializable
    data class GroupDetails(
        val groupId: String,
        val origin: Origin = Origin.SDK,
    ) : OctopusDestination

    // Posts

    /**
     * Post details screen destination.
     *
     * Displays a specific post with its content, comments, and interaction options.
     *
     * @property postId Unique identifier of the post to display
     * @property commentId Optional specific comment to highlight or scroll to
     * @property scrollToLatestComment Whether to automatically scroll to the latest comment
     * @property focusCommentInputField Whether to automatically focus the comment input field
     */
    @Serializable
    data class PostDetails(
        val postId: String,
        val commentId: String? = null,
        val scrollToLatestComment: Boolean = false,
        val focusCommentInputField: Boolean = false,
        val origin: Origin = Origin.SDK,
        val hasFeaturedComment: Boolean = false,
    ) : OctopusDestination

    /**
     * Create post screen destination.
     *
     * Screen for creating new posts with text, media, and other content.
     * Bridge Share opens this destination with optional prefill primitives
     * derived from `OctopusPrefilledPost` — text, image URI, and an
     * invisible-in-editor CTA (label + URL).
     *
     * The prefill fields are primitive strings (rather than the higher-level
     * `OctopusPrefilledPost` value object) so the route stays
     * `@Serializable`-friendly. The composable rehydrates them into the
     * editor's view-model state at startup.
     *
     * @property type Whether the editor opens in TEXT or POLL mode.
     * @property groupId Preselected group id, if any. May come from
     * `OctopusPrefilledPost.topicId` or from the in-SDK group-feed entry
     * point.
     * @property prefilledText Text the editor opens with (Bridge Share).
     * @property prefilledImageUri Deprecated single prefilled image: use
     * [prefilledImageUris]. Read only when [prefilledImageUris] is empty.
     * @property prefilledCustomActionLabel Label of the CTA attached to
     * the published post (Bridge Share). Invisible in the editor.
     * @property prefilledCustomActionUrl Stringified URL of the CTA
     * attached to the published post (Bridge Share). Invisible in the
     * editor.
     * @property prefilledImageUris Stringified content URIs of the images
     * the editor opens with (Bridge Share), in display order. The
     * composable parses them back into `Uri`s. Appended last so that no
     * `componentN` of this public route is renumbered.
     * @property prefilledCustomActionLabelLanguage Code of the language of
     * [prefilledCustomActionLabel] (`en`, `pt-BR`) when the CTA label is given per
     * language; `null` when it is auto-translated.
     * @property prefilledCustomActionLabelTranslations The CTA labels of the other
     * languages, as a compact JSON object of language code to label; `null` when
     * there is none. A string, since a route argument cannot be a map.
     */
    @Serializable
    data class CreatePost(
        val type: Post.Draft.Type = Post.Draft.Type.TEXT,
        val groupId: String? = null,
        val prefilledText: String? = null,
        @Deprecated("Use prefilledImageUris.", level = DeprecationLevel.WARNING)
        val prefilledImageUri: String? = null,
        val prefilledCustomActionLabel: String? = null,
        val prefilledCustomActionUrl: String? = null,
        val prefilledImageUris: List<String> = emptyList(),
        val prefilledCustomActionLabelLanguage: String? = null,
        val prefilledCustomActionLabelTranslations: String? = null,
    ) : OctopusDestination {
        /** The constructor as it was before the per-language CTA labels, kept so code compiled against it keeps linking. */
        @Suppress("DEPRECATION")
        constructor(
            type: Post.Draft.Type = Post.Draft.Type.TEXT,
            groupId: String? = null,
            prefilledText: String? = null,
            prefilledImageUri: String? = null,
            prefilledCustomActionLabel: String? = null,
            prefilledCustomActionUrl: String? = null,
            prefilledImageUris: List<String> = emptyList(),
        ) : this(
            type = type,
            groupId = groupId,
            prefilledText = prefilledText,
            prefilledImageUri = prefilledImageUri,
            prefilledCustomActionLabel = prefilledCustomActionLabel,
            prefilledCustomActionUrl = prefilledCustomActionUrl,
            prefilledImageUris = prefilledImageUris,
            prefilledCustomActionLabelLanguage = null,
            prefilledCustomActionLabelTranslations = null,
        )

        /** The constructor as it was before [prefilledImageUris], kept so code compiled against it keeps linking. */
        constructor(
            type: Post.Draft.Type = Post.Draft.Type.TEXT,
            groupId: String? = null,
            prefilledText: String? = null,
            prefilledImageUri: String? = null,
            prefilledCustomActionLabel: String? = null,
            prefilledCustomActionUrl: String? = null,
        ) : this(
            type = type,
            groupId = groupId,
            prefilledText = prefilledText,
            prefilledImageUri = prefilledImageUri,
            prefilledCustomActionLabel = prefilledCustomActionLabel,
            prefilledCustomActionUrl = prefilledCustomActionUrl,
            prefilledImageUris = emptyList(),
            prefilledCustomActionLabelLanguage = null,
            prefilledCustomActionLabelTranslations = null,
        )

        /**
         * `copy` as it was before [prefilledImageUris], which it keeps with the per-language CTA labels; kept so
         * code compiled against it keeps linking.
         */
        @Suppress("DEPRECATION")
        fun copy(
            type: Post.Draft.Type = this.type,
            groupId: String? = this.groupId,
            prefilledText: String? = this.prefilledText,
            prefilledImageUri: String? = this.prefilledImageUri,
            prefilledCustomActionLabel: String? = this.prefilledCustomActionLabel,
            prefilledCustomActionUrl: String? = this.prefilledCustomActionUrl,
        ): CreatePost =
            CreatePost(
                type = type,
                groupId = groupId,
                prefilledText = prefilledText,
                prefilledImageUri = prefilledImageUri,
                prefilledCustomActionLabel = prefilledCustomActionLabel,
                prefilledCustomActionUrl = prefilledCustomActionUrl,
                prefilledImageUris = prefilledImageUris,
                prefilledCustomActionLabelLanguage = prefilledCustomActionLabelLanguage,
                prefilledCustomActionLabelTranslations = prefilledCustomActionLabelTranslations,
            )

        /**
         * `copy` as it was before the per-language CTA labels, which it keeps; kept so code compiled against it
         * keeps linking.
         */
        @Suppress("DEPRECATION")
        fun copy(
            type: Post.Draft.Type = this.type,
            groupId: String? = this.groupId,
            prefilledText: String? = this.prefilledText,
            prefilledImageUri: String? = this.prefilledImageUri,
            prefilledCustomActionLabel: String? = this.prefilledCustomActionLabel,
            prefilledCustomActionUrl: String? = this.prefilledCustomActionUrl,
            prefilledImageUris: List<String> = this.prefilledImageUris,
        ): CreatePost =
            CreatePost(
                type = type,
                groupId = groupId,
                prefilledText = prefilledText,
                prefilledImageUri = prefilledImageUri,
                prefilledCustomActionLabel = prefilledCustomActionLabel,
                prefilledCustomActionUrl = prefilledCustomActionUrl,
                prefilledImageUris = prefilledImageUris,
                prefilledCustomActionLabelLanguage = prefilledCustomActionLabelLanguage,
                prefilledCustomActionLabelTranslations = prefilledCustomActionLabelTranslations,
            )

        /**
         * The prefilled images: [prefilledImageUris], or the deprecated [prefilledImageUri] for a
         * route built before [prefilledImageUris] existed.
         */
        @Suppress("DEPRECATION")
        val resolvedPrefilledImageUris: List<String>
            get() = prefilledImageUris.ifEmpty { listOfNotNull(prefilledImageUri) }
    }

    /**
     * Post media viewer destination.
     *
     * Full-screen media viewer for post attachments like images and videos.
     *
     * @property postId Unique identifier of the post containing the media
     * @property selectedIndex Index of the media item to display initially (0-based)
     */
    @Serializable
    data class PostMedias(
        val postId: String,
        val selectedIndex: Int = 0,
    ) : OctopusDestination

    // Comments

    /**
     * Comment details screen destination.
     *
     * Displays a specific comment with its content, replies, and interaction options.
     *
     * @property commentId Unique identifier of the comment to display
     * @property replyId Optional specific reply to highlight or scroll to
     * @property showViewPostButton Whether to show the "View Post" button for navigation
     * @property scrollToLatestReply Whether to automatically scroll to the latest reply
     * @property focusReplyInputField Whether to automatically focus the reply input field
     */
    @Serializable
    data class CommentDetails(
        val commentId: String,
        val replyId: String? = null,
        val showViewPostButton: Boolean = true,
        val scrollToLatestReply: Boolean = false,
        val focusReplyInputField: Boolean = false,
    ) : OctopusDestination

    /**
     * Comment media viewer destination.
     *
     * Full-screen media viewer for comment attachments like images and videos.
     *
     * @property commentId Unique identifier of the comment containing the media
     * @property selectedIndex Index of the media item to display initially (0-based)
     */
    @Serializable
    data class CommentMedias(
        val commentId: String,
        val selectedIndex: Int = 0,
    ) : OctopusDestination

    // Replies

    /**
     * Reply media viewer destination.
     *
     * Full-screen media viewer for reply attachments like images and videos.
     *
     * @property replyId Unique identifier of the reply containing the media
     * @property selectedIndex Index of the media item to display initially (0-based)
     */
    @Serializable
    data class ReplyMedias(
        val replyId: String,
        val selectedIndex: Int = 0,
    ) : OctopusDestination

    // Connection

    /**
     * The nickname choice the SDK opens before a member's first post, comment or reply goes, when their
     * nickname is not confirmed yet. Opened without a pending contribution (e.g. by a deep link), the
     * screen closes right away.
     */
    @Serializable
    data object ValidateNickname : OctopusDestination

    // Current User Profile

    /**
     * Current user profile navigation graph.
     *
     * Contains all screens related to the current user's profile management.
     */
    @Serializable
    data object CurrentUserProfileGraph : OctopusDestination

    /**
     * Current user profile summary screen destination.
     *
     * Displays the current user's profile with their posts, comments, notifications, and profile
     * information.
     *
     * @property selectedTabIndex Index of the initially selected tab (0-based): Posts=0,
     * Comments=1 (internal ticket), Notifications=2. These indices are a stable contract and keep their
     * meaning even when the Comments tab is not displayed (no comment feed available yet), in which
     * case requesting it falls back to Posts.
     */
    @Serializable
    data class CurrentUserProfileSummary(
        val selectedTabIndex: Int = 0,
    ) : OctopusDestination {
        companion object {
            /** Predefined instance for the Posts tab */
            val Posts = CurrentUserProfileSummary(selectedTabIndex = 0)

            /** Predefined instance for the Comments tab (internal ticket) */
            val Comments = CurrentUserProfileSummary(selectedTabIndex = 1)

            /** Predefined instance for the Notifications tab */
            val Notifications = CurrentUserProfileSummary(selectedTabIndex = 2)
        }
    }

    /**
     * Community activity screen destination (Unified Profile, internal ticket).
     *
     * Displays the community activity of a user — for the connected user, the same posts and
     * notifications surfaced by [CurrentUserProfileSummary] but presented as a standalone
     * "Activity" entry point (no profile header), reached from the home floating button when Unified
     * Profile is active (the backend `exposeClientUserId` flag on AND the host wired
     * `onNavigateToProfile`).
     *
     * @property userId Unique identifier of the user whose activity to display, or `null` for the
     * connected user. (Other-user mode is introduced by the on-demand Activity API.)
     * @property selectedTabIndex Index of the initially selected tab (0-based). Only meaningful
     * when no [userId] / [clientUserId] is requested (connected-user entry, e.g. the home floating
     * button): an id entry point that resolves to the connected user (a self-tap) lands per the
     * unseen-notifications rule instead — Notifications if there are unseen notifications, Posts
     * otherwise (same rule as the floating button; mirrors iOS)
     * @property clientUserId The host app's own id for the member whose activity to display (Unified
     * Profile), resolved to an Octopus id via the `GetPublicProfile` client-user-id lookup. Mutually
     * exclusive with [userId]; `null` unless opened via `navigateToOctopusActivityByClientUserId`.
     */
    @Serializable
    data class Activity(
        val userId: String? = null,
        val selectedTabIndex: Int = 0,
        val clientUserId: String? = null,
    ) : OctopusDestination {
        // Note: the Activity screen tab order is Notifications=0, Comments=1 (internal ticket), Posts=2 —
        // Comments sits in the middle (matching iOS). Each destination owns its own tab order; nothing
        // assumes a shared mapping. Comments is never a landing tab, so it has no predefined instance.
        // These indices keep their meaning when the Comments tab is not displayed (no comment feed
        // available yet): they are resolved against the visible tabs, never used as raw positions.
        companion object {
            /** Predefined instance for the connected user's Notifications tab */
            val Notifications = Activity(selectedTabIndex = 0)

            /** Predefined instance for the connected user's Posts tab */
            val Posts = Activity(selectedTabIndex = 2)
        }
    }

    /**
     * Current user profile edit screen destination.
     *
     * Screen for editing the current user's profile information.
     *
     * @property focusedField Optional specific profile field to focus on when the screen opens
     */
    @Serializable
    data class CurrentUserProfileEdit(
        val focusedField: ProfileField? = null,
    ) : OctopusDestination

    // Profile

    /**
     * Other user profile summary screen destination.
     *
     * Displays another user's profile with their public information and posts.
     *
     * @property userId Unique identifier of the user whose profile to display, or `null` when
     * opened via [clientUserId] (Unified Profile) before the lookup resolves.
     * @property clientUserId The host app's own id for the member whose profile to display (Unified
     * Profile), resolved to an Octopus id via the `GetPublicProfile` client-user-id lookup. Mutually
     * exclusive with [userId]; `null` unless opened via
     * [com.octopuscommunity.sdk.ui.navigateToOctopusProfileByClientUserId].
     */
    @Serializable
    data class ProfileSummary(
        val userId: String? = null,
        val clientUserId: String? = null,
    ) : OctopusDestination {
        /**
         * Binary-compatibility constructor preserving the pre-1.13.0 `ProfileSummary(userId)` JVM
         * signature for consumers compiled against 1.12.x.
         */
        constructor(userId: String) : this(userId, null)
    }

    /**
     * Profile avatar viewer destination.
     *
     * Full-screen viewer for a user's profile picture.
     *
     * @property userId Unique identifier of the user whose avatar to display
     */
    @Serializable
    data class ProfileAvatar(
        val userId: String,
    ) : OctopusDestination

    // Moderation

    // Settings

    /**
     * How to report content screen destination.
     *
     * Educational screen explaining the content reporting process and guidelines.
     */
    @Serializable
    data object HowToReportContent : OctopusDestination
}
