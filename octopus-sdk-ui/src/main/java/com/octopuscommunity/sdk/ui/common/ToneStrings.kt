package com.octopuscommunity.sdk.ui.common

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.ui.R

/**
 * The current community configuration, provided at the root of every SDK composition by
 * `OctopusNavigationScope`. `null` until the configuration is known, which reads as the formal
 * tone everywhere.
 */
internal val LocalOctopusCommunityConfig = compositionLocalOf<CommunityConfig?> { null }

/**
 * The SDK's own [androidx.compose.ui.res.stringResource], applying the community's tone (internal ticket).
 *
 * Every `octopus-sdk-ui` call site imports this one instead of the androidx function (pinned by
 * `ToneStringsSourceGuardTest`), so a text that has an informal variant switches to it wherever it
 * is shown, without each call site knowing which texts have one.
 */
@Composable
@ReadOnlyComposable
internal fun stringResource(
    @StringRes id: Int,
): String =
    androidx.compose.ui.res
        .stringResource(toneStringId(id))

/** See [stringResource]. */
@Composable
@ReadOnlyComposable
internal fun stringResource(
    @StringRes id: Int,
    vararg formatArgs: Any,
): String =
    androidx.compose.ui.res
        .stringResource(toneStringId(id), *formatArgs)

/** [id], or its informal variant when the community uses the informal tone in the displayed language. */
@Composable
@ReadOnlyComposable
@StringRes
internal fun toneStringId(
    @StringRes id: Int,
): Int {
    // Most texts have no variant: answer before reading any composition local.
    if (InformalStrings[id] == null) return id
    return toneStringId(id, LocalOctopusCommunityConfig.current, LocalConfiguration.current)
}

/**
 * [id], or its informal variant when [config] puts the language [configuration] resolves strings in
 * under the informal tone. Non-composable form, for strings resolved from a `Context`.
 */
@StringRes
internal fun toneStringId(
    @StringRes id: Int,
    config: CommunityConfig?,
    configuration: Configuration,
): Int {
    val informal = InformalStrings[id] ?: return id
    if (config == null || config.informalLanguages.isEmpty()) return id
    val locale = ConfigurationCompat.getLocales(configuration)[0] ?: return id
    return if (config.isInformalTone(locale.toLanguageTag())) informal else id
}

/**
 * Each text with a tone variant: its `_formal` resource (today's text, what call sites reference)
 * to its `_informal` one. Both come from the localization Sheet (`scripts/generate_strings_from_tsv.py`);
 * `ToneStringsTest` fails when a pair in `values/strings.xml` is missing here.
 *
 * Referencing the informal ids statically also keeps them through a host's resource shrinking.
 */
internal object InformalStrings {
    private val informalByFormal: Map<Int, Int> =
        mapOf(
            R.string.accessibility_post_list_open_detail_hint_formal to R.string.accessibility_post_list_open_detail_hint_informal,
            R.string.accessibility_profile_picture_edit_formal to R.string.accessibility_profile_picture_edit_informal,
            R.string.accessibility_reaction_summary_see_all_formal to R.string.accessibility_reaction_summary_see_all_informal,
            R.string.block_profile_alert_message_formal to R.string.block_profile_alert_message_informal,
            R.string.comment_delete_confirmation_title_formal to R.string.comment_delete_confirmation_title_informal,
            R.string.common_cancel_modifications_formal to R.string.common_cancel_modifications_informal,
            R.string.community_access_denied_banned_appeal_formal to R.string.community_access_denied_banned_appeal_informal,
            R.string.community_access_denied_banned_text_formal to R.string.community_access_denied_banned_text_informal,
            R.string.community_access_denied_banned_text_without_nickname_formal to
                R.string.community_access_denied_banned_text_without_nickname_informal,
            R.string.community_access_denied_banned_title_formal to R.string.community_access_denied_banned_title_informal,
            R.string.community_access_denied_deleted_title_formal to R.string.community_access_denied_deleted_title_informal,
            R.string.consent_title_formal to R.string.consent_title_informal,
            R.string.content_aggregated_info_reaction_count_empty_formal to R.string.content_aggregated_info_reaction_count_empty_informal,
            R.string.content_create_picture_max_reached_formal to R.string.content_create_picture_max_reached_informal,
            R.string.content_empty_state_be_first_to_post_formal to R.string.content_empty_state_be_first_to_post_informal,
            R.string.error_no_network_formal to R.string.error_no_network_informal,
            R.string.error_permission_denied_formal to R.string.error_permission_denied_informal,
            R.string.error_text_too_long_current_length_max_length_formal to
                R.string.error_text_too_long_current_length_max_length_informal,
            R.string.error_unknown_formal to R.string.error_unknown_informal,
            R.string.error_user_not_authenticated_formal to R.string.error_user_not_authenticated_informal,
            R.string.gamification_sheet_post_commented_formal to R.string.gamification_sheet_post_commented_informal,
            R.string.gamification_sheet_profile_completed_formal to R.string.gamification_sheet_profile_completed_informal,
            R.string.gamification_sheet_title_formal to R.string.gamification_sheet_title_informal,
            R.string.gamification_toast_comment_formal to R.string.gamification_toast_comment_informal,
            R.string.gamification_toast_daily_session_formal to R.string.gamification_toast_daily_session_informal,
            R.string.gamification_toast_post_formal to R.string.gamification_toast_post_informal,
            R.string.gamification_toast_post_commented_formal to R.string.gamification_toast_post_commented_informal,
            R.string.gamification_toast_profile_completed_formal to R.string.gamification_toast_profile_completed_informal,
            R.string.gamification_toast_reaction_formal to R.string.gamification_toast_reaction_informal,
            R.string.gamification_toast_vote_formal to R.string.gamification_toast_vote_informal,
            R.string.group_action_unfollow_error_last_followed_group_formal to
                R.string.group_action_unfollow_error_last_followed_group_informal,
            R.string.group_permissions_access_denied_text_formal to R.string.group_permissions_access_denied_text_informal,
            R.string.moderation_common_caption_formal to R.string.moderation_common_caption_informal,
            R.string.moderation_common_done_block_question_formal to R.string.moderation_common_done_block_question_informal,
            R.string.moderation_common_done_message_formal to R.string.moderation_common_done_message_informal,
            R.string.moderation_content_explanation_formal to R.string.moderation_content_explanation_informal,
            R.string.moderation_profile_explanation_formal to R.string.moderation_profile_explanation_informal,
            R.string.picture_selection_error_max_ratio_min_size_formal to R.string.picture_selection_error_max_ratio_min_size_informal,
            R.string.poll_results_hidden_some_votes_formal to R.string.poll_results_hidden_some_votes_informal,
            R.string.poll_results_hidden_vote_count_formal to R.string.poll_results_hidden_vote_count_informal,
            R.string.post_create_error_text_too_short_formal to R.string.post_create_error_text_too_short_informal,
            R.string.post_create_incentive_button5_formal to R.string.post_create_incentive_button5_informal,
            R.string.post_create_text_placeholder_formal to R.string.post_create_text_placeholder_informal,
            R.string.post_delete_confirmation_title_formal to R.string.post_delete_confirmation_title_informal,
            R.string.post_detail_no_comments_formal to R.string.post_detail_no_comments_informal,
            R.string.profile_comments_empty_state_title_self_formal to R.string.profile_comments_empty_state_title_self_informal,
            R.string.profile_detail_empty_bio_button_formal to R.string.profile_detail_empty_bio_button_informal,
            R.string.profile_edit_bio_placeholder_formal to R.string.profile_edit_bio_placeholder_informal,
            R.string.profile_edit_client_app_alert_title_formal to R.string.profile_edit_client_app_alert_title_informal,
            R.string.profile_edit_nickname_explanation_formal to R.string.profile_edit_nickname_explanation_informal,
            R.string.profile_nickname_placeholder_formal to R.string.profile_nickname_placeholder_informal,
            R.string.profile_notifications_empty_state_title_self_formal to R.string.profile_notifications_empty_state_title_self_informal,
            R.string.profile_posts_empty_state_title_self_formal to R.string.profile_posts_empty_state_title_self_informal,
            R.string.reply_delete_confirmation_title_formal to R.string.reply_delete_confirmation_title_informal,
            R.string.settings_logout_done_title_formal to R.string.settings_logout_done_title_informal,
            R.string.settings_report_explanation_formal to R.string.settings_report_explanation_informal,
            R.string.settings_report_main_text_formal to R.string.settings_report_main_text_informal,
            R.string.validate_nickname_choose_subtitle_formal to R.string.validate_nickname_choose_subtitle_informal,
            R.string.validate_nickname_choose_title_formal to R.string.validate_nickname_choose_title_informal,
        )

    /** The informal variant of [formalId], or `null` when it has none. */
    operator fun get(
        @StringRes formalId: Int,
    ): Int? = informalByFormal[formalId]

    /** Every formal id with a variant, for tests. */
    val formalIds: Set<Int> get() = informalByFormal.keys
}
