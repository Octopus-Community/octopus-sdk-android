package com.octopuscommunity.sdk.ui.notifications.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.Notification
import com.octopuscommunity.sdk.domain.model.OctopusEvent.NotificationClicked
import com.octopuscommunity.sdk.domain.model.Settings
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.NotificationRepository
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.OctopusDestination.CommentDetails
import com.octopuscommunity.sdk.ui.OctopusDestination.PostDetails
import com.octopuscommunity.sdk.ui.common.ErrorChannel
import com.octopuscommunity.sdk.ui.common.ScreenStateFailure
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.errorText
import com.octopuscommunity.sdk.ui.common.loadFailureChannel
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationsListViewModel internal constructor(
    commonParameters: CommonParameters,
    private val notificationRepository: NotificationRepository,
) : OctopusViewModel(commonParameters) {
    @Immutable
    data class UiState(
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = true,
        val notificationsSettings: Settings.Notifications? = null,
        val notifications: List<Notification> = emptyList(),
        @Deprecated(
            "Dead since the screen states landed: never read, and no longer written. " +
                "Use hasLoadFailure / loadFailureIsNoNetwork for a failed first load, and the error " +
                "snackbar for a failure over content already on screen. Removed in the next major.",
            level = DeprecationLevel.WARNING,
        )
        val error: UiText? = null,
        /**
         * Set when the first load failed with no notification on screen behind it. A failure over a
         * list already displayed goes to the snackbar instead (see `loadFailureChannel`).
         */
        val hasLoadFailure: Boolean = false,
        val loadFailureIsNoNetwork: Boolean = false,
    ) {
        val isPushNotificationEnabled = notificationsSettings?.pushEnabled ?: false
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            notificationRepository.getNotificationsFlow().collect { notifications ->
                _state.update {
                    it.copy(notifications = notifications)
                }
            }
        }
        viewModelScope.launch {
            notificationRepository.getNotificationsSettingsFlow().collect { settings ->
                _state.update {
                    it.copy(notificationsSettings = settings)
                }
            }
        }
        refresh(showRefresh = false)
        // The closure deleted the notifications: a list that outlived it loads them again.
        viewModelScope.launch { communityReopened.collect { refresh(showRefresh = false) } }
    }

    fun refresh(showRefresh: Boolean = true) {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = showRefresh) }
            fetchNotifications()
        }
    }

    /**
     * Re-runs the first load, from the error state's CTA. Raises [UiState.isLoading] and clears
     * [UiState.hasLoadFailure] before fetching, because the list renders `isLoading` before
     * `hasLoadFailure`: without it the error state — Retry button included — stays frozen on screen
     * for the whole round trip and the button looks dead. Mirrors
     * `GroupsListViewModel.retryFirstLoad`.
     */
    fun retryFirstLoad() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, hasLoadFailure = false) }
            fetchNotifications()
        }
    }

    /**
     * Fetches the notifications and their settings, then reports a failure the way the design asks:
     * as the screen's own state when there is nothing listed behind it, as a snackbar over a list
     * already on screen. Clears the in-flight flags in the same update as the outcome, so the
     * loader never blinks off before the state that replaces it is set.
     */
    private suspend fun fetchNotifications() {
        val result = notificationRepository.fetchNotifications()
        notificationRepository.fetchNotificationsSettings()
        // The fetch used to be fire-and-forget: a failed first load left the empty state up, as
        // if the member simply had no notification (Screen states spec).
        when (result) {
            is OctopusResult.Success ->
                _state.update {
                    it.copy(isLoading = false, isRefreshing = false, hasLoadFailure = false)
                }

            is OctopusResult.Failure -> {
                val channel = loadFailureChannel(hasVisibleContent = _state.value.notifications.isNotEmpty())
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        hasLoadFailure = channel == ErrorChannel.Inline,
                        loadFailureIsNoNetwork =
                            ScreenStateFailure.from(result) == ScreenStateFailure.NoNetwork,
                    )
                }
                if (channel == ErrorChannel.Transient) {
                    // A list is already on screen here, so the pull-to-refresh indicator is the
                    // right feedback — the full-screen loader would hide the content.
                    showErrorSnackbar(
                        message = result.errorText,
                        isNoNetwork = ScreenStateFailure.from(result) == ScreenStateFailure.NoNetwork,
                    ) { refresh() }
                }
            }
        }
    }

    fun togglePushNotification() {
        val notificationsSettings = _state.value.notificationsSettings ?: return
        viewModelScope.launch {
            val result =
                notificationRepository.setNotificationsSettings(
                    notificationsSettings.copy(pushEnabled = !notificationsSettings.pushEnabled),
                )
            when (result) {
                is OctopusResult.Success ->
                    _state.update {
                        it.copy(notificationsSettings = result.data)
                    }

                is OctopusResult.Failure -> showError(result)
            }
        }
    }

    fun navigateToNotificationItem(notification: Notification) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notification)
            eventRepository.emitEvent(
                NotificationClicked(
                    notificationId = notification.id,
                    contentId = notification.action?.itemId,
                ),
            )
        }
        when (val action = notification.action) {
            is Notification.Action.Post ->
                navigateTo(
                    PostDetails(
                        postId = action.itemId,
                        origin = OctopusDestination.Origin.SDK,
                    ),
                )

            is Notification.Action.Comment ->
                navigateTo(
                    CommentDetails(
                        commentId = action.itemId,
                        showViewPostButton = true,
                    ),
                )

            is Notification.Action.Reply ->
                navigateTo(
                    CommentDetails(
                        commentId = action.parentId,
                        replyId = action.itemId,
                        showViewPostButton = true,
                    ),
                )

            null -> {}
        }
    }
}
