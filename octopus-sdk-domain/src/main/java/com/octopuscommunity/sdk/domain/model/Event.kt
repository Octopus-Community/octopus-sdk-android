package com.octopuscommunity.sdk.domain.model

import com.octopuscommunity.sdk.domain.model.Gamification.Toast
import com.octopuscommunity.sdk.domain.model.OctopusItem.ContentKind
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction

/**
 * Sealed interface representing all public events emitted by the Octopus SDK.
 *
 * These events can be collected via [OctopusSDK.events] to feed your own analytics/tracking service.
 */
sealed interface OctopusEvent : SDKEvent {
    /**
     * Sent when a post has been created by the current user.
     *
     * @property postId The ID of the post.
     * @property content Content of the post. A set indicating what the published post contains
     * (text, image, poll). The SDK derives it from the draft the user submitted, so it reports the
     * attachments the user actually published even when server-side media processing has not
     * completed yet.
     * @property groupId The ID of the group to which the post has been linked.
     * @property textLength The length of the text of this post.
     */
    data class PostCreated(
        override val postId: String,
        val content: Set<Content>,
        override val groupId: String,
        val textLength: Int,
    ) : OctopusEvent,
        PostEvent,
        ContentCreated {
        override val contentId: String = postId
        override val contentKind: ContentKind = ContentKind.POST

        /**
         * Builds the event from a published [post] alone, deriving [content] from that snapshot.
         *
         * Kept for source and binary compatibility. The SDK does not use it to emit the event:
         * a post snapshot coming back from the backend right after publish may not carry its
         * media aggregate yet (media processing is asynchronous), which under-reports
         * [Content.IMAGE] non-deterministically. Prefer the primary constructor with a [content]
         * set derived from what was actually submitted.
         */
        constructor(post: Post) : this(
            postId = post.id,
            content =
                buildSet {
                    if (post.text.original.text
                            .isNotBlank()
                    ) {
                        add(Content.TEXT)
                    }
                    if (post.medias?.images?.isNotEmpty() == true) add(Content.IMAGE)
                    if (post.poll != null) add(Content.POLL)
                },
            groupId = post.parentId,
            textLength = post.textLength,
        )

        /**
         * Content types that can be present in a post.
         */
        enum class Content {
            /** Post contains text. */
            TEXT,

            /** Post contains an image. */
            IMAGE,

            /** Post contains a poll. */
            POLL,
        }
    }

    /**
     * Sent when a comment has been created by the current user.
     *
     * @property commentId The ID of the comment.
     * @property postId The ID of the post in which the comment has been posted.
     * @property textLength The length of the text of this comment.
     */
    data class CommentCreated(
        override val commentId: String,
        override val postId: String,
        val textLength: Int,
    ) : OctopusEvent,
        CommentEvent,
        ContentCreated {
        override val contentId: String = commentId
        override val contentKind: ContentKind = ContentKind.COMMENT

        constructor(comment: Comment) : this(
            commentId = comment.id,
            postId = comment.parentId,
            textLength = comment.textLength,
        )
    }

    /**
     * Sent when a reply has been created by the current user.
     *
     * @property replyId The ID of the reply.
     * @property commentId The ID of the comment in which this reply has been posted.
     * @property textLength The length of the text of this reply.
     */
    data class ReplyCreated(
        override val replyId: String,
        override val commentId: String,
        val textLength: Int,
    ) : OctopusEvent,
        ReplyEvent,
        ContentCreated {
        override val contentId: String = replyId
        override val contentKind: ContentKind = ContentKind.REPLY

        constructor(reply: Comment.Reply) : this(
            replyId = reply.id,
            commentId = reply.parentId,
            textLength = reply.textLength,
        )
    }

    /**
     * Sent when a post has been deleted by the current user.
     *
     * @property postId The ID of the deleted post.
     * @property groupId The ID of the group to which the post was linked.
     */
    data class PostDeleted(
        override val postId: String,
        override val groupId: String,
    ) : OctopusEvent,
        PostEvent,
        ContentDeleted {
        override val contentId: String = postId
        override val contentKind: ContentKind = ContentKind.POST

        constructor(post: Post) : this(
            postId = post.id,
            groupId = post.parentId,
        )
    }

    /**
     * Sent when a comment has been deleted by the current user.
     *
     * @property commentId The ID of the deleted comment.
     * @property postId The ID of the post in which the comment was posted.
     */
    data class CommentDeleted(
        override val commentId: String,
        override val postId: String,
    ) : OctopusEvent,
        CommentEvent,
        ContentDeleted {
        override val contentId: String = commentId
        override val contentKind: ContentKind = ContentKind.COMMENT

        constructor(comment: Comment) : this(
            commentId = comment.id,
            postId = comment.parentId,
        )
    }

    /**
     * Sent when a reply has been deleted by the current user.
     *
     * @property replyId The ID of the deleted reply.
     * @property commentId The ID of the comment in which the reply was posted.
     */
    data class ReplyDeleted(
        override val replyId: String,
        override val commentId: String,
    ) : OctopusEvent,
        ReplyEvent,
        ContentDeleted {
        override val contentId: String = replyId
        override val contentKind: ContentKind = ContentKind.REPLY

        constructor(reply: Comment.Reply) : this(
            replyId = reply.id,
            commentId = reply.parentId,
        )
    }

    /**
     * Sent when a reaction is modified (added, removed, or changed) on a content by the current user.
     *
     * @property contentId The ID of the content on which the reaction was modified.
     * @property contentKind The kind of content ([ContentKind.POST], [ContentKind.COMMENT], or [ContentKind.REPLY]).
     * @property previousReaction The previous reaction kind, or null if there was no reaction before.
     * @property newReaction The new reaction kind, or null if the reaction was removed.
     *   Possible values: Heart, Joy, MouthOpen, Clap, Cry, Rage.
     */
    data class ReactionModified(
        override val contentId: String,
        override val contentKind: ContentKind,
        val previousReaction: Reaction.Kind?,
        val newReaction: Reaction.Kind?,
    ) : OctopusEvent,
        ContentEvent {
        constructor(item: OctopusItem, reaction: Reaction?) : this(
            contentId = item.id,
            contentKind = item.contentKind,
            previousReaction = item.userReaction?.kind,
            newReaction = reaction?.kind,
        )
    }

    /**
     * Sent when the current user votes for a poll.
     *
     * @property contentId The ID of the post containing the poll.
     * @property optionId The ID of the selected poll option.
     */
    data class PollVote(
        override val contentId: String,
        val optionId: String,
    ) : OctopusEvent,
        ContentEvent {
        override val contentKind: ContentKind = ContentKind.POST
    }

    /**
     * Sent when a content has been reported by the current user.
     *
     * @property contentId The ID of the reported content.
     * @property contentKind The kind of content ([ContentKind.POST], [ContentKind.COMMENT], or [ContentKind.REPLY]).
     * @property reasons The list of reasons for reporting this content.
     */
    data class ContentReported(
        override val contentId: String,
        override val contentKind: ContentKind,
        val reasons: List<Moderation.ReportReason>,
    ) : OctopusEvent,
        ContentEvent

    /**
     * Sent when a profile has been reported by the current user.
     *
     * @property profileId The ID of the reported profile.
     * @property reasons The list of reasons for reporting this profile.
     */
    data class ProfileReported(
        val profileId: String,
        val reasons: List<Moderation.ReportReason>,
    ) : OctopusEvent

    /**
     * Sent when the current user follows or unfollows a group.
     *
     * @property groupId The ID of the group whose follow status changed.
     * @property followed `true` if the user followed the group, `false` if they unfollowed it.
     */
    data class GroupFollowingChanged(
        val groupId: String,
        val followed: Boolean,
    ) : OctopusEvent

    /**
     * Sent when gamification points are gained.
     *
     * Note: Only points triggered by an in-app action are reported live.
     *
     * @property points The number of points that have been added.
     * @property action The action that led to gaining points.
     */
    data class GamificationPointsGained(
        val toast: Toast,
    ) : OctopusEvent {
        val points: Int = toast.points.value
        val action: Gamification.Action = toast.action
    }

    /**
     * Sent when gamification points are removed.
     *
     * Note: Only points triggered by an in-app action are reported live.
     * For example, if a post of this user gets moderated, you won't receive
     * the information about points removed.
     *
     * @property points The number of points that have been removed.
     * @property action The action that led to losing points.
     */
    data class GamificationPointsRemoved(
        val points: Int,
        val action: Gamification.Action,
    ) : OctopusEvent

    /**
     * Sent when the user navigates to a given screen.
     */
    sealed interface ScreenDisplayed : OctopusEvent {
        /**
         * The main posts feed screen.
         *
         * @property feedId The ID of the feed that is displayed.
         */
        data class MainFeed(
            val feedId: String,
        ) : ScreenDisplayed

        /**
         * The posts feed (list of posts) screen.
         *
         * @property feedId The ID of the feed that is displayed.
         * @property relatedGroupId The ID of the group related to this feed.
         *   Null if the feed is not representing a group or is multi-group (for example, "For You").
         */
        @Deprecated(
            message = "Use MainFeed instead.",
            replaceWith = ReplaceWith("ScreenDisplayed.MainFeed(feedId)"),
        )
        data class PostsFeed(
            val feedId: String,
            val relatedGroupId: String? = null,
        ) : ScreenDisplayed

        /**
         * The post detail screen with the list of comments.
         *
         * @property postId The ID of the post that is displayed.
         */
        data class PostDetail(
            val postId: String,
        ) : ScreenDisplayed

        /**
         * The comment detail screen with the list of replies.
         *
         * @property commentId The ID of the comment that is displayed.
         */
        data class CommentDetail(
            val commentId: String,
        ) : ScreenDisplayed

        /** The groups list screen. */
        data object Groups : ScreenDisplayed

        /**
         * The group details screen (posts feed of a group).
         *
         * @property groupId The ID of the group that is displayed.
         * @property source The source from which this screen was opened.
         */
        data class GroupDetail(
            val groupId: String,
            val source: Source,
        ) : ScreenDisplayed {
            /**
             * Source from which the group detail screen was opened.
             */
            enum class Source {
                /** Opened from the client app via bridge mode. */
                BRIDGE,

                /** Opened from within the SDK community (e.g. tapping a group from the home or groups list). */
                COMMUNITY,
            }
        }

        /** The create post screen. */
        data object CreatePost : ScreenDisplayed

        /** The current user profile screen. */
        data object Profile : ScreenDisplayed

        /**
         * The community activity screen of the connected user (Unified Profile).
         *
         * Displayed instead of [Profile] when the community has Unified Profile enabled and the
         * user opens their activity from the home floating button.
         */
        data object Activity : ScreenDisplayed

        /**
         * The profile screen of another Octopus user.
         *
         * @property profileId The ID of the profile that is displayed.
         */
        data class OtherUserProfile(
            val profileId: String,
        ) : ScreenDisplayed

        /**
         * The community activity screen of another Octopus user, showing their posts (Unified
         * Profile).
         *
         * Displayed when the host opens a member's activity on demand via
         * `NavController.navigateToOctopusActivity(userId)`. This is a posts list, distinct from
         * the profile summary surfaced by [OtherUserProfile] — kept as its own screen event so
         * host analytics can tell the two apart.
         *
         * @property profileId The ID of the user whose posts are displayed.
         */
        data class OtherUserPosts(
            val profileId: String,
        ) : ScreenDisplayed

        /** The edit profile screen. */
        data object EditProfile : ScreenDisplayed

        /** The report content screen. */
        data object ReportContent : ScreenDisplayed

        /** The report profile screen. */
        data object ReportProfile : ScreenDisplayed

        /**
         * The nickname choice screen.
         * Displayed before the first post, comment or reply of a member who has not confirmed their
         * nickname yet; the contribution is sent once the nickname is confirmed.
         */
        data object ValidateNickname : ScreenDisplayed

        /** The screen shown instead of the community when the user has no access to it. */
        data object CommunityAccessDenied : ScreenDisplayed

        /** The settings screen. */
        @Deprecated("No longer emitted: settings are available in the profile menu.", level = DeprecationLevel.WARNING)
        data object SettingsList : ScreenDisplayed

        /**
         * The account settings screen.
         * Retained for compatibility; account management is now owned by the host app.
         */
        @Deprecated("No longer emitted: user accounts are managed by your app.", level = DeprecationLevel.WARNING)
        data object SettingsAccount : ScreenDisplayed

        /** The report explanation screen. */
        data object ReportExplanation : ScreenDisplayed

        /**
         * The delete account screen.
         * Retained for compatibility; account management is now owned by the host app.
         */
        @Deprecated("No longer emitted: user accounts are managed by your app.", level = DeprecationLevel.WARNING)
        data object DeleteAccount : ScreenDisplayed
    }

    /**
     * Sent when the user clicks on an internal notification (from the Octopus Notification Center).
     *
     * @property notificationId The ID of the notification.
     * @property contentId The target content ID. Can be null if the notification does not target a content.
     */
    data class NotificationClicked(
        val notificationId: String,
        val contentId: String?,
    ) : OctopusEvent

    /**
     * Sent when the user clicks on a post.
     *
     * @property postId The ID of the post.
     * @property source The source of the click.
     */
    data class PostClicked(
        val postId: String,
        val source: Source,
    ) : OctopusEvent {
        /**
         * The source from which a post was clicked.
         */
        enum class Source {
            /** The post was displayed in the posts feed. */
            FEED,

            /** The post was displayed in a user profile posts list. */
            PROFILE,
        }
    }

    /**
     * Sent when the user clicks on a translation button.
     *
     * @property contentId The ID of the content.
     * @property viewTranslated Whether the user wants to display the translated or the original content.
     * @property contentKind The kind of content ([ContentKind.POST], [ContentKind.COMMENT], or [ContentKind.REPLY]).
     */
    data class TranslationButtonClicked(
        val contentId: String,
        val viewTranslated: Boolean,
        val contentKind: ContentKind,
    ) : OctopusEvent {
        constructor(item: OctopusItem, viewTranslated: Boolean) : this(
            contentId = item.id,
            viewTranslated = viewTranslated,
            contentKind = item.contentKind,
        )
    }

    /**
     * Sent when the user clicks the comment button of a post.
     *
     * @property postId The ID of the post.
     */
    data class CommentButtonClicked(
        val postId: String,
    ) : OctopusEvent

    /**
     * Sent when the user clicks the reply button of a comment.
     *
     * @property commentId The ID of the comment.
     */
    data class ReplyButtonClicked(
        val commentId: String,
    ) : OctopusEvent

    /**
     * Sent when the user clicks on the replies button of a comment.
     *
     * @property commentId The ID of the comment.
     */
    data class SeeRepliesButtonClicked(
        val commentId: String,
    ) : OctopusEvent

    /**
     * Sent when the current user profile has been modified.
     *
     * [nickname], [bio] and [picture] describe changes observed between the two profiles.
     * Unlike iOS, which records explicitly submitted edits, these properties cannot identify an
     * edit that leaves the stored value unchanged. With no previous profile, nullable fields are
     * compared against `null` and the nickname is considered updated.
     *
     * @property previousProfile The profile before the modification. Can be null if the profile was just created.
     * @property newProfile The profile after the modification.
     */
    data class ProfileModified(
        val previousProfile: OctopusProfile?,
        val newProfile: OctopusProfile,
    ) : OctopusEvent {
        /** Whether the nickname changed, with an empty context when it did. */
        val nickname: ProfileFieldUpdate<NicknameUpdateContext>
            get() =
                if (previousProfile?.nickname != newProfile.nickname) {
                    ProfileFieldUpdate.Updated(NicknameUpdateContext)
                } else {
                    ProfileFieldUpdate.Unchanged
                }

        /** Whether the bio changed, with its new length (zero when removed). */
        val bio: ProfileFieldUpdate<BioUpdateContext>
            get() =
                if (previousProfile?.bio != newProfile.bio) {
                    ProfileFieldUpdate.Updated(BioUpdateContext(newProfile.bio?.length ?: 0))
                } else {
                    ProfileFieldUpdate.Unchanged
                }

        /** Whether the picture changed, including replacement, addition and removal. */
        val picture: ProfileFieldUpdate<PictureUpdateContext>
            get() =
                if (previousProfile?.picture != newProfile.picture) {
                    ProfileFieldUpdate.Updated(PictureUpdateContext(newProfile.picture != null))
                } else {
                    ProfileFieldUpdate.Unchanged
                }

        /** Empty context indicating that the nickname changed. */
        data object NicknameUpdateContext

        /**
         * The bio after an update.
         * @property bioLength Length in UTF-16 code units, like other Android text lengths;
         * zero when the bio is absent.
         */
        data class BioUpdateContext(
            val bioLength: Int,
        )

        /**
         * The picture state after an update.
         * @property hasPicture Whether the updated profile has a picture.
         */
        data class PictureUpdateContext(
            val hasPicture: Boolean,
        )
    }

    /**
     * Sent when an Octopus session has started.
     *
     * An Octopus session begins when the user enters the SDK (displays an Octopus screen)
     * and ends when the user exits the SDK.
     *
     * @property sessionId The unique identifier of the session.
     */
    data class SessionStarted(
        val sessionId: String,
    ) : OctopusEvent

    /**
     * Sent when an Octopus session has stopped.
     *
     * An Octopus session begins when the user enters the SDK (displays an Octopus screen)
     * and ends when the user exits the SDK.
     *
     * @property sessionId The unique identifier of the session.
     */
    data class SessionStopped(
        val sessionId: String,
    ) : OctopusEvent
}

/** An [SDKEvent] about the post [postId] of the group [groupId]. */
interface PostEvent : SDKEvent {
    val postId: String
    val groupId: String
}

/** An [SDKEvent] about the comment [commentId] of the post [postId]. */
interface CommentEvent : SDKEvent {
    val commentId: String
    val postId: String
}

/** An [SDKEvent] about the reply [replyId] to the comment [commentId]. */
interface ReplyEvent : SDKEvent {
    val replyId: String
    val commentId: String
}

/** A [ContentEvent] emitted when content was created. */
interface ContentCreated : ContentEvent

/** A [ContentEvent] emitted when content was deleted. */
interface ContentDeleted : ContentEvent

/** An [SDKEvent] about the content [contentId] of kind [contentKind]. */
interface ContentEvent : SDKEvent {
    val contentId: String
    val contentKind: ContentKind
}

/**
 * Internal Session events (not exposed publicly via OctopusSDK.events).
 */
sealed interface SessionEvent : SDKEvent {
    data class AppStarted(
        val session: Sessions.Session.App,
    ) : SessionEvent

    data class AppStopped(
        val session: Sessions.Session.App,
    ) : SessionEvent

    /**
     * The Octopus session [session] started.
     *
     * @property showGamificationSheet Computed by the domain, but the SDK UI no longer reads it
     * since 1.16: the rules prompt follows a Home-appearance count, as on iOS. Kept for
     * compatibility; to be removed at the next major.
     * @property gamificationToasts Gamification toasts to show for this start.
     */
    data class OctopusStarted(
        val session: Sessions.Session.Octopus,
        val showGamificationSheet: Boolean,
        val gamificationToasts: List<Toast>,
    ) : SessionEvent

    data class OctopusStopped(
        val session: Sessions.Session.Octopus,
    ) : SessionEvent
}

/**
 * Internal SDK events (not exposed publicly via OctopusSDK.events).
 */
sealed interface SDKEvent {
    data class Initialized(
        val connectionMode: ConnectionMode,
    ) : SDKEvent

    data object Stopped : SDKEvent

    data object Disconnected : SDKEvent
}

// region Deprecated backward-compatible accessors

/** @suppress */
@Deprecated("Use groupId", ReplaceWith("groupId"))
val PostEvent.topicId: String get() = groupId

/** @suppress */
@Deprecated("Use relatedGroupId", ReplaceWith("relatedGroupId"))
val OctopusEvent.ScreenDisplayed.PostsFeed.relatedTopicId: String?
    get() = relatedGroupId

// endregion
