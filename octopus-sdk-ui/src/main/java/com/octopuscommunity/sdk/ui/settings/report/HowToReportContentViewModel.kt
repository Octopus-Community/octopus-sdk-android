package com.octopuscommunity.sdk.ui.settings.report

import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ScreenDisplayed
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import kotlinx.coroutines.launch

class HowToReportContentViewModel(commonParameters: CommonParameters) :
    OctopusViewModel(commonParameters) {
    init {
        viewModelScope.launch {
            eventRepository.emitEvent(ScreenDisplayed.ReportExplanation)
        }
    }
}