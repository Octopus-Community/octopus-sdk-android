package com.octopuscommunity.sdk.ui.report.common

import com.octopuscommunity.sdk.domain.model.OctopusItem.ContentKind

/**
 * Identifies what is being reported when the report bottom sheet is opened.
 *
 * Surfaced as part of menu-sheet event streams (post / comment / reply / profile) so the
 * SDK can present the right report flow without going through a dedicated nav destination.
 *
 * Naming mirrors iOS [`ReportTarget`](https://github.com/Octopus-Community/octopus-sdk-swift/blob/main/Sources/OctopusUI/Report/ReportTarget.swift)
 * (`.content` / `.profile`) — adding a new variant is a binary-breaking change for
 * exhaustive `when` consumers.
 */
sealed interface ReportTarget {
    data class Content(
        val contentId: String,
        val contentKind: ContentKind,
    ) : ReportTarget

    data class Profile(
        val profileId: String,
    ) : ReportTarget
}
