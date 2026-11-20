//
//  Copyright © 2026 Octopus Community. All rights reserved.
//
package com.octopuscommunity.sdk.ui.profile.comments

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The comments feed to list in **another member's** "Comments" tab, or `null` when that tab must not
 * be shown at all. Three conditions, all required:
 * - [showCommentsOnOtherProfiles]: the per-community backend flag — the community opted in to
 *   exposing a member's comments on their profile.
 * - a usable [commentsFeedId]: the member's profile carries a non-empty comment feed id.
 * - the feed has not been refused by the server ([isForbidden]).
 *
 * The feed id is tested for *emptiness*, not nullity: it comes from a proto3 non-optional string, so
 * a profile served by a backend without the comment feed deserializes to `""` and is passed through
 * as-is by `ProfileMapper` — a `!= null` test would always hold and let a permanently empty tab
 * through. Returning the usable id (rather than a boolean) keeps that trap in one place instead of
 * asking every caller to remember it.
 *
 * Shared by the two screens that can show another member —
 * [com.octopuscommunity.sdk.ui.profile.others.summary.ProfileSummaryScreen] (the native profile) and
 * [ActivityScreen][com.octopuscommunity.sdk.ui.profile.activity.ActivityScreen] in other-user mode
 * (the Unified Profile screen) — so the tab follows the same permissions and never depends on which
 * route the profile tap took. The two paths had already drifted apart once, the Activity one not
 * reading the flag at all (internal ticket); a single pure function is what stops that happening again.
 *
 * The connected user's own screens never call this: their own comments are their own content, shown
 * whenever a feed exists and never gated by the community flag.
 *
 * @param commentsFeedId the member's descending comment feed id, as served — may be `null` (profile
 * not loaded yet) or `""` (backend without the feed).
 * @param showCommentsOnOtherProfiles the community config flag
 * ([com.octopuscommunity.sdk.domain.model.CommunityConfig.showCommentsOnOtherProfiles]).
 * @param isForbidden whether loading this feed returned `PermissionDenied` or `UserNotAuthenticated`.
 */
internal fun otherUserCommentsFeedId(
    commentsFeedId: String?,
    showCommentsOnOtherProfiles: Boolean,
    isForbidden: Boolean = false,
): String? = commentsFeedId?.takeIf { showCommentsOnOtherProfiles && it.isNotEmpty() && !isForbidden }

/**
 * Observes the same feed ViewModel as [ProfileCommentsList], outside the pager: a denied tab must
 * stay hidden even after its page leaves composition. On returning to the screen, recheck denied
 * feeds so restored permissions can show the tab again.
 */
@Composable
internal fun visibleOtherUserCommentsFeedId(
    commentsFeedId: String?,
    showCommentsOnOtherProfiles: Boolean,
    viewModel: ProfileCommentsListViewModel? = null,
): String? {
    val candidateFeedId =
        otherUserCommentsFeedId(commentsFeedId, showCommentsOnOtherProfiles) ?: return null
    val commentsViewModel = viewModel ?: rememberProfileCommentsListViewModel(candidateFeedId, isOwnProfile = false)
    val isForbidden by commentsViewModel.isForbidden.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isForbidden) commentsViewModel.refresh()
    }
    return otherUserCommentsFeedId(candidateFeedId, showCommentsOnOtherProfiles, isForbidden)
}
