package com.octopuscommunity.sdk.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.octopuscommunity.sdk.ui.OctopusIconsDefaults.content
import com.octopuscommunity.sdk.ui.OctopusIconsDefaults.postCreation

/**
 * Icon resources configuration for Octopus SDK UI components.
 *
 * All icons default to the SDK's built-in visuals. Override any subset via
 * [OctopusIconsDefaults.icons] to match your app's design language.
 *
 * @property groups Icons used in the groups/topic feed navigation.
 * @property content Icons used across posts, comments, replies, video, and polls.
 * @property profile Icons used on user profile screens.
 * @property gamification Icons used in the gamification/badges screens.
 * @property settings Icons used in the settings screens.
 * @property radio Custom radio button icons. When `null`, the native [androidx.compose.material3.RadioButton] is used.
 * @property checkbox Custom checkbox icons. When `null`, the native [androidx.compose.material3.Checkbox] is used.
 * @property toggle Custom toggle/switch icons. When `null`, the native [androidx.compose.material3.Switch] is used.
 * @property moreActions Icon for the overflow "more actions" button (e.g. in post/comment sheets).
 * @property activityButton Icon shown on the home floating button when Unified Profile is enabled —
 * a community-activity/notifications bell that replaces the user's avatar (which is the button's
 * visual when the feature is off). Client-overridable to match the app's design language.
 * @property screenStates Illustrations for the screen states — an empty list, or a first load
 * that failed. Appended last so that positional construction of the other properties keeps
 * working.
 * @property clearText Icon of the button that clears a text field, such as the nickname field of the
 * nickname choice screen. Tinted like the other icons. Appended last so that positional
 * construction of the other properties keeps working.
 */
data class OctopusIcons(
    val groups: Groups,
    val content: Content,
    val profile: Profile,
    val gamification: Gamification,
    val settings: Settings,
    val radio: OnOff? = null,
    val checkbox: OnOff? = null,
    val toggle: OnOff? = null,
    val moreActions: @Composable () -> Painter,
    val cellNavIndicator: @Composable () -> Painter,
    val activityButton: @Composable () -> Painter,
    val screenStates: ScreenStates = OctopusIconsDefaults.screenStates(),
    val clearText: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_clear_text) },
) {
    /**
     * Icons used in the groups/topic navigation area.
     *
     * @property openList Icon for the button that opens the groups or topic selection panel
     * in the feed header.
     * @property selected Icon shown on essential (always-followed) group cells.
     */
    data class Groups(
        val openList: @Composable () -> Painter,
        val selected: @Composable () -> Painter,
        val viewGroup: @Composable () -> Painter,
    )

    /**
     * Icons used across all content types (posts, comments, replies, video, polls).
     *
     * @property post Icons specific to posts.
     * @property comment Icons specific to comments.
     * @property reply Icons specific to replies.
     * @property video Icons for the video player controls.
     * @property poll Icons for polls.
     * @property delete Generic delete action icon, used in action menus/sheets.
     * @property views Generic view count icon, used as a fallback across content types.
     * @property report Generic report content icon, used in action menus.
     * @property like Generic like/reaction icon, used in the [OctopusLikeButton] component.
     * @property reaction Per-reaction image set used across all reaction surfaces.
     * @property translation Icon shown before the "See original" / "See translation" toggle of an
     * automatically translated post, comment or reply. Tinted with the toggle's text color.
     * Appended last so that positional construction of the other properties keeps working.
     */
    data class Content(
        val post: Post,
        val comment: Comment,
        val reply: Reply,
        val video: Video,
        val poll: Poll,
        val delete: @Composable () -> Painter,
        val views: @Composable () -> Painter,
        val report: @Composable () -> Painter,
        val like: @Composable () -> Painter,
        val reaction: Reaction,
        val translation: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_translation) },
    ) {
        /**
         * Per-reaction image set used across all reaction surfaces
         * (picker, summary, breakdown sheet, interaction button).
         *
         * Each reaction defaults to an SDK-bundled image. Override any subset to match
         * your brand imagery — mixing SDK defaults and custom overrides is fully supported
         * (e.g. provide a custom heart but keep the SDK defaults for the rest).
         *
         * Unknown reaction kinds (forward-compatibility for reactions added server-side
         * before the SDK is updated) fall back to the emoji unicode character and are not
         * customisable through this API.
         *
         * Dark/light mode variants are handled through standard Android resource qualifiers
         * on the drawable provided by the client — no separate API is needed.
         *
         * @property heart Image for the ❤️ heart reaction.
         * @property joy Image for the 😂 joy reaction.
         * @property mouthOpen Image for the 😮 surprise reaction.
         * @property clap Image for the 👏 clap reaction.
         * @property cry Image for the 😢 cry reaction.
         * @property rage Image for the 😡 rage reaction.
         */
        data class Reaction(
            val heart: @Composable () -> Painter,
            val joy: @Composable () -> Painter,
            val mouthOpen: @Composable () -> Painter,
            val clap: @Composable () -> Painter,
            val cry: @Composable () -> Painter,
            val rage: @Composable () -> Painter,
        )

        /**
         * Icons specific to posts.
         *
         * @property creation Icons for the post creation form.
         * @property emptyFeedInGroups Deprecated, not rendered: an empty group feed shows
         * [OctopusIcons.ScreenStates.emptyContent].
         * @property emptyFeedInCurrentUserProfile Deprecated, not rendered: the current user's empty
         * post list shows [OctopusIcons.ScreenStates.emptyContent].
         * @property emptyFeedInOtherUserProfile Deprecated, not rendered: another user's empty post
         * list shows [OctopusIcons.ScreenStates.emptyContent].
         * @property notAvailable Icon shown when a post is unavailable or moderated.
         * @property moderated Icon shown in the "Moderated" status tag (shield-with-slash glyph,
         * tinted with the alert color). Iso iOS `theme.assets.icons.content.post.moderated`.
         * @property commentCount Icon for the comment count button in the post aggregated info bar.
         * @property openCommentCreation Icon for the "Comment" action button that focuses
         * the comment creation field.
         * @property views Icon for the post view count in the aggregated info bar.
         * @property likeNotSelected Icon for the reaction button when no reaction is selected.
         * @property moreReactions Icon for the button that opens the full emoji reactions picker.
         */
        data class Post(
            val creation: Creation,
            @Deprecated(
                "Not rendered since the screen states landed: every empty post feed shows " +
                    "screenStates.emptyContent, as on iOS. Removed in the next major.",
                level = DeprecationLevel.WARNING,
            )
            val emptyFeedInGroups: @Composable () -> Painter,
            @Deprecated(
                "Not rendered since the screen states landed: every empty post feed shows " +
                    "screenStates.emptyContent, as on iOS. Removed in the next major.",
                level = DeprecationLevel.WARNING,
            )
            val emptyFeedInCurrentUserProfile: @Composable () -> Painter,
            @Deprecated(
                "Not rendered since the screen states landed: every empty post feed shows " +
                    "screenStates.emptyContent, as on iOS. Removed in the next major.",
                level = DeprecationLevel.WARNING,
            )
            val emptyFeedInOtherUserProfile: @Composable () -> Painter,
            val notAvailable: @Composable () -> Painter,
            val moderated: @Composable () -> Painter,
            val commentCount: @Composable () -> Painter,
            val openCommentCreation: @Composable () -> Painter,
            val views: @Composable () -> Painter,
            val likeNotSelected: @Composable () -> Painter,
            val moreReactions: @Composable () -> Painter,
        ) {
            /**
             * Icons for the post creation form.
             *
             * @property create Icon for the "Create post" floating action button in the feed.
             * @property topicSelection Icon for the topic/group dropdown selector.
             * @property addPicture Icon for the button to attach a picture.
             * @property deletePicture Icon for the button to remove an attached picture.
             * @property addPoll Icon for the button to add a poll to the post.
             * @property addPollOption Icon for the button to add a new poll option.
             * @property deletePoll Icon for the button to remove the entire poll.
             * @property deletePollOption Icon for the button to remove a specific poll option.
             */
            data class Creation(
                val create: @Composable () -> Painter,
                val topicSelection: @Composable () -> Painter,
                val addPicture: @Composable () -> Painter,
                val deletePicture: @Composable () -> Painter,
                val addPoll: @Composable () -> Painter,
                val addPollOption: @Composable () -> Painter,
                val deletePoll: @Composable () -> Painter,
                val deletePollOption: @Composable () -> Painter,
            )
        }

        /**
         * Icons specific to comments.
         *
         * @property creation Icons for the comment creation form.
         * @property emptyFeed Deprecated, not rendered: a post with no comments shows
         * [OctopusIcons.ScreenStates.emptyContent].
         * @property notAvailable Icon shown when a comment is unavailable or moderated.
         * @property openReplyCreation Icon for the button that opens the reply creation form.
         * @property seeReplies Icon for the button that expands the replies list under a comment.
         * @property likeNotSelected Icon for the like button when no reaction is selected.
         */
        data class Comment(
            val creation: Creation,
            @Deprecated(
                "Not rendered since the screen states landed: a post with no comments shows " +
                    "screenStates.emptyContent, as on iOS. Removed in the next major.",
                level = DeprecationLevel.WARNING,
            )
            val emptyFeed: @Composable () -> Painter,
            val notAvailable: @Composable () -> Painter,
            val openReplyCreation: @Composable () -> Painter,
            val seeReplies: @Composable () -> Painter,
            val likeNotSelected: @Composable () -> Painter,
        ) {
            /**
             * Icons for the comment creation form.
             *
             * @property create Icon for the send/submit button.
             * @property addPicture Icon for the button to attach a picture.
             * @property deletePicture Icon for the button to remove an attached picture.
             */
            data class Creation(
                val create: @Composable () -> Painter,
                val addPicture: @Composable () -> Painter,
                val deletePicture: @Composable () -> Painter,
            )
        }

        /**
         * Icons specific to replies.
         *
         * @property creation Icons for the reply creation form.
         * @property likeNotSelected Icon for the like button when no reaction is selected.
         */
        data class Reply(
            val creation: Creation,
            val likeNotSelected: @Composable () -> Painter,
        ) {
            /**
             * Icons for the reply creation form.
             *
             * @property create Icon for the send/submit button.
             * @property addPicture Icon for the button to attach a picture.
             * @property deletePicture Icon for the button to remove an attached picture.
             */
            data class Creation(
                val create: @Composable () -> Painter,
                val addPicture: @Composable () -> Painter,
                val deletePicture: @Composable () -> Painter,
            )
        }

        /**
         * Icons for the video player controls.
         *
         * @property muted Icon shown when the video audio is muted.
         * @property notMuted Icon shown when the video audio is active.
         * @property pause Icon for the pause button.
         * @property play Icon for the play button.
         * @property replay Icon for the replay button shown when the video has ended.
         */
        data class Video(
            val muted: @Composable () -> Painter,
            val notMuted: @Composable () -> Painter,
            val pause: @Composable () -> Painter,
            val play: @Composable () -> Painter,
            val replay: @Composable () -> Painter,
        )

        /**
         * Icons for polls.
         *
         * @property selectedOption Icon shown next to the poll option the user has selected.
         */
        data class Poll(
            val selectedOption: @Composable () -> Painter,
        )
    }

    /**
     * A pair of icons representing a binary on/off state.
     * Used for radio buttons, checkboxes, and toggles.
     *
     * @property on Icon for the active/selected state.
     * @property off Icon for the inactive/unselected state.
     */
    data class OnOff(
        val on: @Composable () -> Painter,
        val off: @Composable () -> Painter,
    )

    /**
     * Icons used in the gamification/badges screens.
     *
     * @property badge Icon for an earned badge or achievement.
     * @property info Icon for the info button in the gamification detail sheet.
     * @property rulesHeader Illustration used as the header image in the gamification rules screen.
     */
    data class Gamification(
        val badge: @Composable () -> Painter,
        val info: @Composable () -> Painter,
        val rulesHeader: @Composable () -> Painter,
    )

    /**
     * Icons used in the settings screens.
     *
     * @property account Icon for the account/profile settings entry.
     * @property help Icon for the help and support settings entry.
     * @property info Icon for the about/app info settings entry.
     * @property logout Icon for the logout action.
     * @property deleteAccountWarning Warning icon shown on the delete account confirmation screen.
     */
    data class Settings
        @Deprecated(
            "Use Settings(help, info): accounts are managed by your app.",
            level = DeprecationLevel.WARNING,
        )
        constructor(
            @Deprecated("No longer displayed: accounts are managed by your app.", level = DeprecationLevel.WARNING)
            val account: @Composable () -> Painter,
            val help: @Composable () -> Painter,
            val info: @Composable () -> Painter,
            @Deprecated("No longer displayed: accounts are managed by your app.", level = DeprecationLevel.WARNING)
            val logout: @Composable () -> Painter,
            @Deprecated("No longer displayed: accounts are managed by your app.", level = DeprecationLevel.WARNING)
            val deleteAccountWarning: @Composable () -> Painter,
        ) {
            /**
             * Creates settings icons for the remaining help and information actions.
             * @param help Icon for the help action.
             * @param info Icon for the information action.
             */
            @Suppress("DEPRECATION")
            constructor(
                help: @Composable () -> Painter = { rememberVectorPainter(Icons.AutoMirrored.Outlined.HelpOutline) },
                info: @Composable () -> Painter = { rememberVectorPainter(Icons.Outlined.Info) },
            ) : this(
                account = { rememberVectorPainter(Icons.Outlined.AccountCircle) },
                help = help,
                info = info,
                logout = { rememberVectorPainter(Icons.AutoMirrored.Outlined.Logout) },
                deleteAccountWarning = { painterResource(R.drawable.ic_octopus_settings_delete_account_warning) },
            )
        }

    /**
     * Icons used on user profile screens.
     *
     * @property defaultAvatar Fallback avatar displayed when a user has no profile picture.
     * @property editPicture Icon for the button to edit or replace the profile picture.
     * @property addPicture Icon for the button to add a profile picture when none exists.
     * @property addBio Icon for the button to add a bio when none exists.
     * @property report Icon for the report user action in the profile menu.
     * @property notConnected Icon shown on a profile that is not accessible or not connected.
     * @property blockUser Icon for the block user action in the profile menu.
     */
    data class Profile(
        val defaultAvatar: @Composable () -> Painter,
        val editPicture: @Composable () -> Painter,
        val addPicture: @Composable () -> Painter,
        val addBio: @Composable () -> Painter,
        val report: @Composable () -> Painter,
        val notConnected: @Composable () -> Painter,
        val blockUser: @Composable () -> Painter,
    )

    /**
     * Illustrations of the screen states — a list with nothing in it, or a first load that failed.
     *
     * Unlike the other icons these keep their own colours rather than being tinted: they are
     * illustrations, drawn on a transparent background so they sit on whatever background the
     * community picked.
     *
     * @property emptyContent Illustration shown on an empty list of posts or comments.
     * @property emptyNotifications Illustration shown on an empty list of notifications.
     * @property networkError Illustration shown when a first load failed because the device is offline.
     * @property error Illustration shown when a first load failed for any other reason.
     */
    data class ScreenStates(
        val emptyContent: @Composable () -> Painter,
        val emptyNotifications: @Composable () -> Painter,
        val networkError: @Composable () -> Painter,
        val error: @Composable () -> Painter,
    )
}

/**
 * Default icon configurations for Octopus SDK UI components.
 *
 * Use these factory functions to create [OctopusIcons] instances with SDK default icons,
 * optionally overriding specific icons to match your app's design language.
 *
 * The factory functions are organised to allow both fine-grained overrides (e.g.
 * [postCreation] to change only the post creation toolbar icons) and coarse-grained
 * shared overrides (e.g. passing a single `addPicture` to [content] will propagate it
 * to post, comment, and reply creation forms simultaneously).
 */
object OctopusIconsDefaults {
    /**
     * Creates an [OctopusIcons] with SDK defaults, allowing selective overrides.
     *
     * The `close`, `report`, and shared content painters are convenience shortcuts that
     * propagate to multiple sub-groups unless those groups are overridden explicitly.
     *
     * @param groups Icons for the groups/topic navigation area.
     * @param radio Custom radio button icon pair. Pass `null` (default) to use the native
     * [androidx.compose.material3.RadioButton].
     * @param checkbox Custom checkbox icon pair. Pass `null` (default) to use the native
     * [androidx.compose.material3.Checkbox].
     * @param toggle Custom toggle icon pair. Pass `null` (default) to use the native
     * [androidx.compose.material3.Switch].
     * @param close Shared close/dismiss icon. Used as the default delete-picture icon across
     * all creation forms unless overridden.
     * @param moreActions Icon for the overflow "more actions" button.
     * @param report Shared report icon. Propagates to both [content] and [profile] unless
     * those are overridden explicitly.
     * @param content Icons for all content types.
     * @param profile Icons for profile screens.
     * @param gamification Icons for the gamification screens.
     * @param settings Icons for the settings screens.
     * @param screenStates Illustrations for the empty and failed-load screen states.
     * @param clearText Icon of the button that clears a text field.
     */
    fun icons(
        groups: OctopusIcons.Groups = groups(),
        radio: OctopusIcons.OnOff? = null,
        checkbox: OctopusIcons.OnOff? = null,
        toggle: OctopusIcons.OnOff? = null,
        close: @Composable () -> Painter = { rememberVectorPainter(Icons.Default.Close) },
        moreActions: @Composable () -> Painter = { rememberVectorPainter(Icons.Default.MoreVert) },
        cellNavIndicator: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_cell_nav_indicator) },
        activityButton: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_activity) },
        report: @Composable () -> Painter = { rememberVectorPainter(Icons.Outlined.Flag) },
        content: OctopusIcons.Content =
            content(
                deletePicture = close,
                report = report,
            ),
        profile: OctopusIcons.Profile =
            profile(
                report = report,
            ),
        gamification: OctopusIcons.Gamification = gamification(),
        settings: OctopusIcons.Settings = settings(),
        screenStates: OctopusIcons.ScreenStates = screenStates(),
        clearText: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_clear_text) },
    ) = OctopusIcons(
        groups = groups,
        content = content,
        profile = profile,
        screenStates = screenStates,
        gamification = gamification,
        settings = settings,
        radio = radio,
        checkbox = checkbox,
        toggle = toggle,
        moreActions = moreActions,
        cellNavIndicator = cellNavIndicator,
        activityButton = activityButton,
        clearText = clearText,
    )

    /**
     * Creates an [OctopusIcons.Groups] with SDK defaults.
     *
     * @param openList Icon for the button that opens the groups or topic selection panel.
     */
    fun groups(
        openList: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_groups_open_list) },
        selected: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_groups_selected) },
        viewGroup: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_groups_view_group) },
    ) = OctopusIcons.Groups(openList = openList, selected = selected, viewGroup = viewGroup)

    /**
     * Creates an [OctopusIcons.Content.Reaction] with SDK-bundled defaults.
     *
     * Override individual reactions to provide custom brand imagery — overrides
     * compose with the SDK defaults for the reactions you don't pass:
     * ```kotlin
     * OctopusIconsDefaults.reaction(
     *     heart = { painterResource(R.drawable.my_custom_heart) },
     *     clap  = { painterResource(R.drawable.my_custom_clap) }
     * )
     * ```
     *
     * @param heart Painter for the ❤️ heart reaction.
     * @param joy Painter for the 😂 joy reaction.
     * @param mouthOpen Painter for the 😮 surprise reaction.
     * @param clap Painter for the 👏 clap reaction.
     * @param cry Painter for the 😢 cry reaction.
     * @param rage Painter for the 😡 rage reaction.
     */
    fun reaction(
        heart: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_reaction_heart) },
        joy: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_reaction_joy) },
        mouthOpen: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_reaction_mouth_open) },
        clap: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_reaction_clap) },
        cry: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_reaction_cry) },
        rage: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_reaction_rage) },
    ) = OctopusIcons.Content.Reaction(
        heart = heart,
        joy = joy,
        mouthOpen = mouthOpen,
        clap = clap,
        cry = cry,
        rage = rage,
    )

    /**
     * Creates an [OctopusIcons.Content] with SDK defaults.
     *
     * Several parameters act as shared defaults that propagate to multiple sub-groups:
     * - `addPicture` propagates to post, comment, and reply creation forms.
     * - `deletePicture` propagates to post, comment, and reply creation forms.
     * - `likeNotSelected` propagates to post, comment, and reply.
     * - `notAvailable` propagates to post and comment.
     * - `send` is used as the default submit icon for comment and reply creation.
     * - `openResponseCreation` is used for both [OctopusIcons.Content.Post.openCommentCreation]
     *   and [OctopusIcons.Content.Comment.openReplyCreation].
     *
     * @param views Shared view count icon.
     * @param likeNotSelected Shared like/reaction icon for the unselected state.
     * @param like Icon used in the generic like button component. Defaults to [likeNotSelected].
     * @param addPicture Shared icon for attaching a picture in any creation form.
     * @param deletePicture Shared icon for removing an attached picture in any creation form.
     * @param delete Icon for generic delete actions in menus and sheets.
     * @param notAvailable Shared icon for unavailable/moderated content.
     * @param report Shared icon for the report action.
     * @param send Shared icon for the submit button in comment and reply creation forms.
     * @param openResponseCreation Shared icon for buttons that open a response creation form.
     * @param post Post-specific icons. Override to provide fine-grained post customisation.
     * @param comment Comment-specific icons. Override to provide fine-grained comment customisation.
     * @param reply Reply-specific icons. Override to provide fine-grained reply customisation.
     * @param video Video player icons.
     * @param poll Poll icons.
     * @param translation Icon shown before the "See original" / "See translation" toggle of
     * translated posts, comments and replies.
     */
    fun content(
        views: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_views) },
        likeNotSelected: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_like_not_selected) },
        like: @Composable () -> Painter = likeNotSelected,
        addPicture: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_add_picture) },
        deletePicture: @Composable () -> Painter = { rememberVectorPainter(Icons.Default.Close) },
        delete: @Composable () -> Painter = { rememberVectorPainter(Icons.Default.Delete) },
        notAvailable: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_content_not_available) },
        report: @Composable () -> Painter = { rememberVectorPainter(Icons.Outlined.Flag) },
        send: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_send) },
        openResponseCreation: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_open_response_creation) },
        reaction: OctopusIcons.Content.Reaction = reaction(),
        post: OctopusIcons.Content.Post =
            post(
                views = views,
                likeNotSelected = likeNotSelected,
                notAvailable = notAvailable,
                openCommentCreation = openResponseCreation,
                creation =
                    postCreation(
                        addPicture = addPicture,
                        deletePicture = deletePicture,
                        deletePoll = delete,
                    ),
            ),
        comment: OctopusIcons.Content.Comment =
            comment(
                likeNotSelected = likeNotSelected,
                notAvailable = notAvailable,
                openReplyCreation = openResponseCreation,
                creation =
                    commentCreation(
                        create = send,
                        addPicture = addPicture,
                        deletePicture = deletePicture,
                    ),
            ),
        reply: OctopusIcons.Content.Reply =
            reply(
                likeNotSelected = likeNotSelected,
                creation =
                    replyCreation(
                        create = send,
                        addPicture = addPicture,
                        deletePicture = deletePicture,
                    ),
            ),
        video: OctopusIcons.Content.Video = video(),
        poll: OctopusIcons.Content.Poll = poll(),
        translation: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_translation) },
    ) = OctopusIcons.Content(
        post = post,
        comment = comment,
        reply = reply,
        video = video,
        poll = poll,
        delete = delete,
        views = views,
        report = report,
        like = like,
        reaction = reaction,
        translation = translation,
    )

    /**
     * Creates an [OctopusIcons.Content.Post] with SDK defaults.
     *
     * The obsolete empty-feed parameters are retained until the next major for source
     * compatibility. Kotlin cannot deprecate parameters; their corresponding properties warn
     * instead. Customize `screenStates = screenStates(emptyContent = ...)` on [icons] instead.
     *
     * @param creation Icons for the post creation form.
     * @param emptyFeed Not rendered: only feeds the deprecated [emptyFeedInGroups] and
     * [emptyFeedInCurrentUserProfile]. Override [screenStates] `emptyContent` instead.
     * @param emptyFeedInGroups Deprecated, not rendered — see [OctopusIcons.Content.Post.emptyFeedInGroups].
     * @param emptyFeedInCurrentUserProfile Deprecated, not rendered — see
     * [OctopusIcons.Content.Post.emptyFeedInCurrentUserProfile].
     * @param emptyFeedInOtherUserProfile Deprecated, not rendered — see
     * [OctopusIcons.Content.Post.emptyFeedInOtherUserProfile].
     * @param notAvailable Icon shown when a post is unavailable or moderated.
     * @param moderated Icon shown in the "Moderated" status tag.
     * @param openCommentCreation Icon for the "Comment" action button.
     * @param commentCount Icon for the comment count button. Defaults to [openCommentCreation].
     * @param views Icon for the post view count.
     * @param likeNotSelected Icon for the reaction button when no reaction is selected.
     * @param moreReactions Icon for the button that opens the full emoji reactions picker.
     */
    fun post(
        creation: OctopusIcons.Content.Post.Creation = postCreation(),
        emptyFeed: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_post_empty_feed) },
        emptyFeedInGroups: @Composable () -> Painter = emptyFeed,
        emptyFeedInCurrentUserProfile: @Composable () -> Painter = emptyFeed,
        emptyFeedInOtherUserProfile: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_empty_feed) },
        notAvailable: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_content_not_available) },
        moderated: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_post_tag_moderated) },
        openCommentCreation: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_open_response_creation) },
        commentCount: @Composable () -> Painter = openCommentCreation,
        views: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_views) },
        likeNotSelected: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_like_not_selected) },
        moreReactions: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_add) },
    ) = OctopusIcons.Content.Post(
        creation = creation,
        emptyFeedInGroups = emptyFeedInGroups,
        emptyFeedInCurrentUserProfile = emptyFeedInCurrentUserProfile,
        emptyFeedInOtherUserProfile = emptyFeedInOtherUserProfile,
        notAvailable = notAvailable,
        moderated = moderated,
        commentCount = commentCount,
        openCommentCreation = openCommentCreation,
        views = views,
        likeNotSelected = likeNotSelected,
        moreReactions = moreReactions,
    )

    /**
     * Creates an [OctopusIcons.Content.Post.Creation] with SDK defaults.
     *
     * @param create Icon for the "Create post" floating action button in the feed.
     * @param topicSelection Icon for the topic/group dropdown selector in the creation form.
     * @param addPicture Icon for the button to attach a picture.
     * @param deletePicture Icon for the button to remove an attached picture.
     * @param addPoll Icon for the button to add a poll to the post.
     * @param addPollOption Icon for the button to add a new poll option.
     * @param deletePoll Icon for the button to remove the entire poll.
     * @param deletePollOption Icon for the button to remove a specific poll option.
     * Defaults to [deletePicture].
     */
    fun postCreation(
        create: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_post_creation_create) },
        topicSelection: @Composable () -> Painter = { rememberVectorPainter(Icons.Default.ArrowDropDown) },
        addPicture: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_add_picture) },
        deletePicture: @Composable () -> Painter = { rememberVectorPainter(Icons.Default.Close) },
        addPoll: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_post_creation_add_poll) },
        addPollOption: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_add) },
        deletePoll: @Composable () -> Painter = { rememberVectorPainter(Icons.Default.Delete) },
        deletePollOption: @Composable () -> Painter = deletePicture,
    ) = OctopusIcons.Content.Post.Creation(
        create = create,
        topicSelection = topicSelection,
        addPicture = addPicture,
        deletePicture = deletePicture,
        addPoll = addPoll,
        addPollOption = addPollOption,
        deletePoll = deletePoll,
        deletePollOption = deletePollOption,
    )

    /**
     * Creates an [OctopusIcons.Content.Comment] with SDK defaults.
     *
     * The obsolete `emptyFeed` parameter is retained until the next major for source
     * compatibility. Kotlin cannot deprecate parameters; its corresponding property warns
     * instead. Customize `screenStates = screenStates(emptyContent = ...)` on [icons] instead.
     *
     * @param creation Icons for the comment creation form.
     * @param emptyFeed Deprecated, not rendered — see [OctopusIcons.Content.Comment.emptyFeed].
     * @param notAvailable Icon shown when a comment is unavailable or moderated.
     * @param openReplyCreation Icon for the button that opens the reply creation form.
     * @param seeReplies Icon for the button that expands the replies list.
     * @param likeNotSelected Icon for the like button when no reaction is selected.
     */
    fun comment(
        creation: OctopusIcons.Content.Comment.Creation = commentCreation(),
        emptyFeed: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_empty_feed) },
        notAvailable: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_content_not_available) },
        openReplyCreation: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_open_response_creation) },
        seeReplies: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_comment_see_replies) },
        likeNotSelected: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_like_not_selected) },
    ) = OctopusIcons.Content.Comment(
        creation = creation,
        emptyFeed = emptyFeed,
        notAvailable = notAvailable,
        openReplyCreation = openReplyCreation,
        seeReplies = seeReplies,
        likeNotSelected = likeNotSelected,
    )

    /**
     * Creates an [OctopusIcons.Content.Comment.Creation] with SDK defaults.
     *
     * @param create Icon for the send/submit button.
     * @param addPicture Icon for the button to attach a picture.
     * @param deletePicture Icon for the button to remove an attached picture.
     */
    fun commentCreation(
        create: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_send) },
        addPicture: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_add_picture) },
        deletePicture: @Composable () -> Painter = { rememberVectorPainter(Icons.Default.Close) },
    ) = OctopusIcons.Content.Comment.Creation(
        create = create,
        addPicture = addPicture,
        deletePicture = deletePicture,
    )

    /**
     * Creates an [OctopusIcons.Content.Reply] with SDK defaults.
     *
     * @param creation Icons for the reply creation form.
     * @param likeNotSelected Icon for the like button when no reaction is selected.
     */
    fun reply(
        creation: OctopusIcons.Content.Reply.Creation = replyCreation(),
        likeNotSelected: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_like_not_selected) },
    ) = OctopusIcons.Content.Reply(creation = creation, likeNotSelected = likeNotSelected)

    /**
     * Creates an [OctopusIcons.Content.Reply.Creation] with SDK defaults.
     *
     * @param create Icon for the send/submit button.
     * @param addPicture Icon for the button to attach a picture.
     * @param deletePicture Icon for the button to remove an attached picture.
     */
    fun replyCreation(
        create: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_send) },
        addPicture: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_add_picture) },
        deletePicture: @Composable () -> Painter = { rememberVectorPainter(Icons.Default.Close) },
    ) = OctopusIcons.Content.Reply.Creation(
        create = create,
        addPicture = addPicture,
        deletePicture = deletePicture,
    )

    /**
     * Creates an [OctopusIcons.Content.Video] with SDK defaults.
     *
     * @param muted Icon shown when the video audio is muted.
     * @param notMuted Icon shown when the video audio is active.
     * @param pause Icon for the pause button.
     * @param play Icon for the play button.
     * @param replay Icon for the replay button shown when playback has ended.
     */
    fun video(
        muted: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_video_muted) },
        notMuted: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_video_not_muted) },
        pause: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_video_pause) },
        play: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_video_play) },
        replay: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_video_replay) },
    ) = OctopusIcons.Content.Video(
        muted = muted,
        notMuted = notMuted,
        pause = pause,
        play = play,
        replay = replay,
    )

    /**
     * Creates an [OctopusIcons.Content.Poll] with SDK defaults.
     *
     * @param selectedOption Icon shown next to the poll option the user has selected.
     */
    fun poll(selectedOption: @Composable () -> Painter = { rememberVectorPainter(Icons.Outlined.CheckCircleOutline) }) =
        OctopusIcons.Content.Poll(selectedOption = selectedOption)

    /**
     * Creates an [OctopusIcons.Profile] with SDK defaults.
     *
     * The `add` and `block` painters are shared defaults:
     * - `add` propagates to both [addPicture] and [addBio] unless overridden.
     * - `block` propagates to both [notConnected] and [blockUser] unless overridden.
     *
     * @param add Shared icon for add actions. Used as the default for [addPicture] and [addBio].
     * @param block Shared icon for block/restriction states. Default for [notConnected] and [blockUser].
     * @param defaultAvatar Fallback avatar shown when the user has no profile picture.
     * @param editPicture Icon for the button to edit or replace the profile picture.
     * @param addPicture Icon for the button to add a profile picture when none exists.
     * @param addBio Icon for the button to add a bio when none exists.
     * @param report Icon for the report user action.
     * @param notConnected Icon shown on a profile that is not accessible or not connected.
     * @param blockUser Icon for the block user action.
     */
    fun profile(
        add: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_add) },
        block: @Composable () -> Painter = { rememberVectorPainter(Icons.Default.Block) },
        defaultAvatar: @Composable () -> Painter = { rememberVectorPainter(Icons.Outlined.Person) },
        editPicture: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_profile_edit_picture) },
        addPicture: @Composable () -> Painter = add,
        addBio: @Composable () -> Painter = add,
        report: @Composable () -> Painter = { rememberVectorPainter(Icons.Outlined.Flag) },
        notConnected: @Composable () -> Painter = block,
        blockUser: @Composable () -> Painter = block,
    ) = OctopusIcons.Profile(
        defaultAvatar = defaultAvatar,
        editPicture = editPicture,
        addPicture = addPicture,
        addBio = addBio,
        report = report,
        notConnected = notConnected,
        blockUser = blockUser,
    )

    /**
     * Creates an [OctopusIcons.ScreenStates] with SDK defaults.
     *
     * @param emptyContent Illustration shown on an empty list of posts or comments.
     * @param emptyNotifications Illustration shown on an empty list of notifications.
     * @param networkError Illustration shown when a first load failed because the device is offline.
     * @param error Illustration shown when a first load failed for any other reason.
     */
    fun screenStates(
        emptyContent: @Composable () -> Painter = {
            painterResource(R.drawable.ic_octopus_screen_state_empty_content)
        },
        emptyNotifications: @Composable () -> Painter = {
            painterResource(R.drawable.ic_octopus_screen_state_empty_notifications)
        },
        networkError: @Composable () -> Painter = {
            painterResource(R.drawable.ic_octopus_screen_state_network_error)
        },
        error: @Composable () -> Painter = {
            painterResource(R.drawable.ic_octopus_screen_state_error)
        },
    ) = OctopusIcons.ScreenStates(
        emptyContent = emptyContent,
        emptyNotifications = emptyNotifications,
        networkError = networkError,
        error = error,
    )

    /**
     * Creates an [OctopusIcons.Gamification] with SDK defaults.
     *
     * @param badge Icon for an earned badge or achievement.
     * @param info Icon for the info button in the gamification detail sheet.
     * @param rulesHeader Illustration used as the header image in the gamification rules screen.
     */
    fun gamification(
        badge: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_gamification_badge) },
        info: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_gamification_info) },
        rulesHeader: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_gamification_rules_header) },
    ) = OctopusIcons.Gamification(badge = badge, info = info, rulesHeader = rulesHeader)

    /**
     * Creates icons for the remaining help and information actions, with SDK defaults.
     * @param help Icon for the help action.
     * @param info Icon for the information action.
     */
    fun settings(
        help: @Composable () -> Painter = { rememberVectorPainter(Icons.AutoMirrored.Outlined.HelpOutline) },
        info: @Composable () -> Painter = { rememberVectorPainter(Icons.Outlined.Info) },
    ) = OctopusIcons.Settings(help = help, info = info)

    /**
     * Creates an [OctopusIcons.Settings] with SDK defaults.
     *
     * @param account Icon for the account/profile settings entry.
     * @param help Icon for the help and support settings entry.
     * @param info Icon for the about/app info settings entry.
     * @param logout Icon for the logout action.
     * @param deleteAccountWarning Warning icon shown on the delete account screen.
     */
    @Deprecated("Use settings(help, info): accounts are managed by your app.", level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    fun settings(
        account: @Composable () -> Painter = { rememberVectorPainter(Icons.Outlined.AccountCircle) },
        help: @Composable () -> Painter = { rememberVectorPainter(Icons.AutoMirrored.Outlined.HelpOutline) },
        info: @Composable () -> Painter = { rememberVectorPainter(Icons.Outlined.Info) },
        logout: @Composable () -> Painter = { rememberVectorPainter(Icons.AutoMirrored.Outlined.Logout) },
        deleteAccountWarning: @Composable () -> Painter = { painterResource(R.drawable.ic_octopus_settings_delete_account_warning) },
    ) = OctopusIcons.Settings(
        account = account,
        help = help,
        info = info,
        logout = logout,
        deleteAccountWarning = deleteAccountWarning,
    )
}
