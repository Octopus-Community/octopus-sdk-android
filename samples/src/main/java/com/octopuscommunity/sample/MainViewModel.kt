package com.octopuscommunity.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sample.data.datastore.UserDataStore
import com.octopuscommunity.sample.data.model.User
import com.octopuscommunity.sample.data.utils.TokenProvider
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ClientUser
import com.octopuscommunity.sdk.domain.model.Image
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the main screen in SSO mode.
 * Handles user state, notifications count, and user-related actions.
 */
class MainViewModel(private val userDataStore: UserDataStore) : ViewModel() {

    /**
     * State class representing the UI state of the main screen
     */
    data class State(
        val user: User? = null,
        val unreadNotificationsCount: Int = 0,
        val hasAccessToCommunity: Boolean = true,
        val isUpdatingCommunityAccess: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            userDataStore.user.collectLatest { user ->
                _state.update { state ->
                    state.copy(user = user)
                }
                if (user != null) {
                    OctopusSDK.connectUser(
                        user = ClientUser(
                            userId = user.id ?: "",
                            profile = ClientUser.Profile(
                                nickname = user.nickname,
                                bio = user.bio,
                                picture = user.picture?.let { avatar ->
                                    when (avatar.source) {
                                        User.Picture.Source.LOCAL -> Image.Local(avatar.data)
                                        User.Picture.Source.REMOTE -> Image.Remote(avatar.data)
                                    }
                                }
                            )
                        ),
                        tokenProvider = {
                            // Your Octopus token provider logic here
                            TokenProvider.getToken(userId = user.id ?: "")
                        }
                    )
                } else {
                    OctopusSDK.disconnectUser()
                }
            }
        }
        viewModelScope.launch {
            OctopusSDK.notSeenNotificationsCount.collectLatest { unreadNotificationsCount ->
                _state.update {
                    it.copy(unreadNotificationsCount = unreadNotificationsCount)
                }
            }
        }
        viewModelScope.launch {
            OctopusSDK.hasAccessToCommunity.collectLatest { hasAccessToCommunity ->
                _state.update {
                    it.copy(hasAccessToCommunity = hasAccessToCommunity)
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

    /**
     * Connects an app user to the Octopus platform.
     *
     * This function maps the application's user model to the Octopus ClientUser model
     * and establishes a connection with the Octopus platform. It handles all necessary
     * authentication through the provided token provider.
     *
     * @param user The application user to connect
     */
    fun updateUser(user: User?) {
        viewModelScope.launch {
            userDataStore.updateUser { user }
        }
    }

    fun updateCommunityAccess(hasAccess: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isUpdatingCommunityAccess = true) }
            OctopusSDK.overrideCommunityAccess(hasAccess)
            _state.update { it.copy(isUpdatingCommunityAccess = false) }
        }
    }
}