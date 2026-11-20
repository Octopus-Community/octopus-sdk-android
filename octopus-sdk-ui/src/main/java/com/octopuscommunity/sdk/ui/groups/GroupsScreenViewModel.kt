package com.octopuscommunity.sdk.ui.groups

import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.OctopusEvent
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import kotlinx.coroutines.launch

class GroupsScreenViewModel internal constructor(
    commonParameters: CommonParameters
) : OctopusViewModel(commonParameters) {

    init {
        viewModelScope.launch {
            eventRepository.emitEvent(OctopusEvent.ScreenDisplayed.Groups)
        }
    }
}