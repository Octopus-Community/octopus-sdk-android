package com.octopuscommunity.sdk.ui.report.user

import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.Moderation
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ScreenDisplayed
import com.octopuscommunity.sdk.domain.repository.ModerationRepository
import com.octopuscommunity.sdk.domain.usecase.ReportProfileUseCase
import com.octopuscommunity.sdk.ui.report.common.ReportViewModel
import kotlinx.coroutines.launch

internal class ReportUserViewModel(
    commonParameters: CommonParameters,
    moderationRepository: ModerationRepository,
    private val reportProfileUseCase: ReportProfileUseCase,
    private val userId: String,
) : ReportViewModel(
        commonParameters = commonParameters,
        reasons = moderationRepository.getModerationReportReasons(),
    ) {
    init {
        viewModelScope.launch { eventRepository.emitEvent(ScreenDisplayed.ReportProfile) }
    }

    override suspend fun sendReport(reasons: List<Moderation.ReportReason>) =
        reportProfileUseCase(
            profileId = userId,
            reasons = reasons,
        )
}
