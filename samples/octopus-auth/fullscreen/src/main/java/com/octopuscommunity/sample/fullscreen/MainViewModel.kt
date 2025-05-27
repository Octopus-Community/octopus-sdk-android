package com.octopuscommunity.sample.fullscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.OctopusSDK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the main screen.
 * Handles notifications count.
 */
class MainViewModel : ViewModel() {

    /**
     * State class representing the UI state of the main screen
     */
    data class State(
        val unreadNotificationsCount: Int = 0
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            OctopusSDK.notSeenNotificationsCount.collect { notSeenNotificationsCount ->
                _state.update { state ->
                    state.copy(unreadNotificationsCount = notSeenNotificationsCount)
                }
            }
        }
    }

    /**
     * Updates the unread notifications count by calling the Octopus SDK
     */
    fun updateNotificationsCount() {
        viewModelScope.launch {
            OctopusSDK.updateNotSeenNotificationsCount()
        }
    }
}