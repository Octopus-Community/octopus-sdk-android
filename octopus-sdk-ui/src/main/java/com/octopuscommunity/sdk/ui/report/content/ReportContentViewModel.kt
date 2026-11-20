package com.octopuscommunity.sdk.ui.report.content

import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.Moderation
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ScreenDisplayed
import com.octopuscommunity.sdk.domain.model.OctopusItem.ContentKind
import com.octopuscommunity.sdk.domain.repository.ModerationRepository
import com.octopuscommunity.sdk.domain.usecase.ReportContentUseCase
import com.octopuscommunity.sdk.ui.report.common.ReportViewModel
import kotlinx.coroutines.launch

internal class ReportContentViewModel(
    commonParameters: CommonParameters,
    moderationRepository: ModerationRepository,
    private val reportContentUseCase: ReportContentUseCase,
    private val contentId: String,
    private val contentKind: ContentKind,
) : ReportViewModel(
        commonParameters = commonParameters,
        reasons = moderationRepository.getModerationReportReasons(),
    ) {
    init {
        viewModelScope.launch { eventRepository.emitEvent(ScreenDisplayed.ReportContent) }
    }

    override suspend fun sendReport(reasons: List<Moderation.ReportReason>) =
        reportContentUseCase(
            contentId = contentId,
            contentKind = contentKind,
            reasons = reasons,
        )
}
