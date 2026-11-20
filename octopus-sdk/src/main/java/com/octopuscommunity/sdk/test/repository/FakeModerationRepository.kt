package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.model.Moderation.ReportReason
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ModerationRepository

/** [ModerationRepository] returning fixed report reasons. */
class FakeModerationRepository : ModerationRepository {
    override fun getModerationReportReasons(): List<ReportReason> =
        listOf(
            ReportReason.HateSpeechOrDiscriminatoryContent,
            ReportReason.ExplicitOrInappropriateContent,
            ReportReason.ViolenceAndTerrorism,
            ReportReason.SpamAndScams,
            ReportReason.SuicideAndSelfHarm,
            ReportReason.FakeProfilesAndImpersonation,
            ReportReason.ChildExploitationOrAbuse,
            ReportReason.IntellectualPropertyViolation,
            ReportReason.Other("Test other reason"),
        )

    override suspend fun reportContent(
        contentId: String,
        reasons: List<ReportReason>,
        customReason: String?,
    ) = OctopusResult.Success(Unit)

    override suspend fun reportUser(
        userId: String,
        reasons: List<ReportReason>,
        customReason: String?,
    ) = OctopusResult.Success(Unit)
}
