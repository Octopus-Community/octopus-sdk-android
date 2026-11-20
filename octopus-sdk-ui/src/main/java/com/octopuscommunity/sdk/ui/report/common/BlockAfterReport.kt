package com.octopuscommunity.sdk.ui.report.common

import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.Profile

/*
 * Who the "Report sent" alert offers to block once a report succeeded (internal ticket, iso iOS).
 *
 * The offer reuses the exact rule that shows the Block entry of the menu the report was opened
 * from, so the two never disagree. `null` means no offer: the alert keeps its single OK button.
 * That covers an author who cannot be blocked (moderator / protected profile), cannot be
 * identified (anonymous or deleted author: no `author`), is already blocked, or is the viewer.
 */

/** The author to offer blocking after reporting this content, or `null` when there is none. */
internal fun OctopusItem.userIdToBlockAfterReport(currentUserProfile: CurrentUserProfile?): String? =
    author?.userId?.takeIf { canAuthorBeBlocked(currentUserProfile) }

/** This profile's id when blocking it can be offered after reporting it, or `null` otherwise. */
internal fun Profile.userIdToBlockAfterReport(currentUserProfile: CurrentUserProfile?): String? =
    userId.takeIf { canBeBlocked(currentUserProfile) && currentUserProfile?.userId != userId }
