package com.octopuscommunity.sample.fullscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ClientPost
import com.octopuscommunity.sdk.domain.model.Image
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.result.OctopusResult
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.errorText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
        val isLoading: Boolean = false,
        val unreadNotificationsCount: Int = 0,
        val bridgePost: Post? = null,
        val error: UiText? = null
    )

    sealed class Event {
        data class BridgePostCreated(val post: Post) : Event()
    }

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _events = Channel<Event>()
    val events = _events.receiveAsFlow()

    init {
        // Monitor unread notifications count
        viewModelScope.launch {
            OctopusSDK.notSeenNotificationsCount.collect { notSeenNotificationsCount ->
                _state.update { state ->
                    state.copy(unreadNotificationsCount = notSeenNotificationsCount)
                }
            }
        }

        // Monitor existing bridge post
        viewModelScope.launch {
            OctopusSDK.getClientObjectPost(clientObjectId = bridgeClientObjectId).collect { post ->
                _state.update { state ->
                    state.copy(bridgePost = post)
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

    fun createBridgePost() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = OctopusSDK.fetchOrCreateClientObjectPost(
                clientPost = ClientPost(
                    objectId = bridgeClientObjectId,
                    text = "The perfects Cannelés (Bordeaux specialty)",
                    attachment = Image.Remote(url = "https://upload.wikimedia.org/wikipedia/commons/a/aa/Caneles_stemilion.jpg"),
                    catchPhrase = "Tried the canelés? Tell us how good they were!",
                    viewObjectButtonText = "Give your feedbacks"
                )
            )
            when (result) {
                is OctopusResult.Success -> {
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            bridgePost = result.data
                        )
                    }
                    _events.send(Event.BridgePostCreated(result.data))
                }

                else -> _state.update { state ->
                    state.copy(
                        isLoading = false,
                        error = result.errorText
                    )
                }
            }
        }
    }

    companion object {
        const val bridgeClientObjectId = "recipe-1"
    }
}