package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.Moderation
import com.octopuscommunity.sdk.domain.network.OctopusResult

/** Repository for moderation reports. */
interface ModerationRepository {
    /** Returns the report reasons the user can choose from, in display order. */
    fun getModerationReportReasons(): List<Moderation.ReportReason>

    /** Reports the content [contentId] for [reasons], with an optional free-text [customReason]. */
    suspend fun reportContent(
        contentId: String,
        reasons: List<Moderation.ReportReason>,
        customReason: String? = null,
    ): OctopusResult<Unit, Nothing>

    /** Reports the user [userId] for [reasons], with an optional free-text [customReason]. */
    suspend fun reportUser(
        userId: String,
        reasons: List<Moderation.ReportReason>,
        customReason: String? = null,
    ): OctopusResult<Unit, Nothing>
}
