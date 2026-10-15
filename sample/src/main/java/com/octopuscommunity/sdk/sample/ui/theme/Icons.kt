package com.octopuscommunity.sdk.sample.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.octopuscommunity.sdk.sample.R
import com.octopuscommunity.sdk.ui.OctopusIcons
import com.octopuscommunity.sdk.ui.OctopusIconsDefaults

@Composable
fun customIcons(): OctopusIcons =
    OctopusIconsDefaults.icons(
        close = { painterResource(R.drawable.ic_sample_common_close) },
        moreActions = { painterResource(R.drawable.ic_sample_common_more_actions) },
        report = { painterResource(R.drawable.ic_sample_content_report) },
        radio =
            OctopusIcons.OnOff(
                on = { painterResource(R.drawable.ic_sample_common_radio_on) },
                off = { painterResource(R.drawable.ic_sample_common_radio_off) },
            ),
        checkbox =
            OctopusIcons.OnOff(
                on = { painterResource(R.drawable.ic_sample_common_checkbox_on) },
                off = { painterResource(R.drawable.ic_sample_common_checkbox_off) },
            ),
        toggle =
            OctopusIcons.OnOff(
                on = { painterResource(R.drawable.ic_sample_common_toggle_on) },
                off = { painterResource(R.drawable.ic_sample_common_toggle_off) },
            ),
        groups =
            OctopusIconsDefaults.groups(
                openList = { painterResource(R.drawable.ic_sample_groups_open_list) },
            ),
        content =
            OctopusIconsDefaults.content(
                views = { painterResource(R.drawable.ic_sample_content_views) },
                likeNotSelected = { painterResource(R.drawable.ic_sample_content_post_like_not_selected) },
                addPicture = { painterResource(R.drawable.ic_sample_content_post_creation_add_picture) },
                deletePicture = { painterResource(R.drawable.ic_sample_common_close) },
                delete = { painterResource(R.drawable.ic_sample_content_delete) },
                notAvailable = { painterResource(R.drawable.ic_sample_content_post_not_available) },
                report = { painterResource(R.drawable.ic_sample_content_report) },
                send = { painterResource(R.drawable.ic_sample_content_comment_creation_create) },
                openResponseCreation = { painterResource(R.drawable.ic_sample_content_post_open_comment_creation) },
                post =
                    OctopusIconsDefaults.post(
                        creation =
                            OctopusIconsDefaults.postCreation(
                                create = { painterResource(R.drawable.ic_sample_content_post_creation_create) },
                                topicSelection = { painterResource(R.drawable.ic_sample_content_post_creation_topic_selection) },
                                addPicture = { painterResource(R.drawable.ic_sample_content_post_creation_add_picture) },
                                deletePicture = { painterResource(R.drawable.ic_sample_common_close) },
                                addPoll = { painterResource(R.drawable.ic_sample_content_post_creation_add_poll) },
                                addPollOption = { painterResource(R.drawable.ic_sample_content_post_creation_add_poll_option) },
                                deletePoll = { painterResource(R.drawable.ic_sample_content_post_creation_delete_poll) },
                                deletePollOption = { painterResource(R.drawable.ic_sample_content_post_creation_delete_poll_option) },
                            ),
                        notAvailable = { painterResource(R.drawable.ic_sample_content_post_not_available) },
                        commentCount = { painterResource(R.drawable.ic_sample_content_post_comments) },
                        openCommentCreation = { painterResource(R.drawable.ic_sample_content_post_open_comment_creation) },
                        views = { painterResource(R.drawable.ic_sample_content_post_views) },
                        likeNotSelected = { painterResource(R.drawable.ic_sample_content_post_like_not_selected) },
                        moreReactions = { painterResource(R.drawable.ic_sample_content_post_more_reactions) },
                    ),
                comment =
                    OctopusIconsDefaults.comment(
                        creation =
                            OctopusIconsDefaults.commentCreation(
                                create = { painterResource(R.drawable.ic_sample_content_comment_creation_create) },
                                addPicture = { painterResource(R.drawable.ic_sample_content_comment_creation_add_picture) },
                                deletePicture = { painterResource(R.drawable.ic_sample_content_comment_creation_delete_picture) },
                            ),
                        notAvailable = { painterResource(R.drawable.ic_sample_content_comment_not_available) },
                        openReplyCreation = { painterResource(R.drawable.ic_sample_content_comment_replies) },
                        seeReplies = { painterResource(R.drawable.ic_sample_content_comment_see_reply) },
                        likeNotSelected = { painterResource(R.drawable.ic_sample_content_comment_like_not_selected) },
                    ),
                reply =
                    OctopusIconsDefaults.reply(
                        creation =
                            OctopusIconsDefaults.replyCreation(
                                create = { painterResource(R.drawable.ic_sample_content_reply_creation_create) },
                                addPicture = { painterResource(R.drawable.ic_sample_content_reply_creation_add_picture) },
                                deletePicture = { painterResource(R.drawable.ic_sample_content_reply_creation_delete_picture) },
                            ),
                        likeNotSelected = { painterResource(R.drawable.ic_sample_content_reply_like_not_selected) },
                    ),
                video =
                    OctopusIconsDefaults.video(
                        muted = { painterResource(R.drawable.ic_sample_content_video_muted) },
                        notMuted = { painterResource(R.drawable.ic_sample_content_video_not_muted) },
                        pause = { painterResource(R.drawable.ic_sample_content_video_pause) },
                        play = { painterResource(R.drawable.ic_sample_content_video_play) },
                        replay = { painterResource(R.drawable.ic_sample_content_video_replay) },
                    ),
                poll =
                    OctopusIconsDefaults.poll(
                        selectedOption = { painterResource(R.drawable.ic_sample_content_poll_selected_option) },
                    ),
                // Demo of reaction emoji customisation via the theming API.
                // Replaces the 6 SDK-bundled Figma emojis with Material Icons so the override
                // pattern is clearly visible when the "Custom icons" scenario is active.
                reaction =
                    OctopusIconsDefaults.reaction(
                        heart = { rememberVectorPainter(Icons.Filled.Favorite) },
                        joy = { rememberVectorPainter(Icons.Filled.SentimentVerySatisfied) },
                        mouthOpen = { rememberVectorPainter(Icons.Filled.SentimentSatisfied) },
                        clap = { rememberVectorPainter(Icons.Filled.ThumbUp) },
                        cry = { rememberVectorPainter(Icons.Filled.SentimentDissatisfied) },
                        rage = { rememberVectorPainter(Icons.Filled.Whatshot) },
                    ),
            ),
        profile =
            OctopusIconsDefaults.profile(
                editPicture = { painterResource(R.drawable.ic_sample_profile_edit_picture) },
                addPicture = { painterResource(R.drawable.ic_sample_profile_add_picture) },
                addBio = { painterResource(R.drawable.ic_sample_profile_add_bio) },
                report = { painterResource(R.drawable.ic_sample_profile_report) },
                notConnected = { painterResource(R.drawable.ic_sample_profile_not_connected) },
                blockUser = { painterResource(R.drawable.ic_sample_profile_block_user) },
            ),
        gamification =
            OctopusIconsDefaults.gamification(
                badge = { painterResource(R.drawable.ic_sample_gamification_badge) },
                info = { painterResource(R.drawable.ic_sample_gamification_info) },
                rulesHeader = { painterResource(R.drawable.ic_sample_gamification_rules_header) },
            ),
        settings =
            OctopusIconsDefaults.settings(
                help = { painterResource(R.drawable.ic_sample_settings_help) },
                info = { painterResource(R.drawable.ic_sample_settings_info) },
            ),
    )
