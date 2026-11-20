package com.octopuscommunity.sdk.ui.common.snackbar

import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import com.octopuscommunity.sdk.ui.events.OctopusSnackbarEvents.Event
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

open class OctopusSnackbarViewModel internal constructor(
    commonParameters: CommonParameters,
) : OctopusViewModel(commonParameters) {

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            snackbarEvents.getEvents().collect { event ->
                // Wait for screen transition
                delay(0.25.seconds)
                _events.emit(event)
            }
        }
    }
}