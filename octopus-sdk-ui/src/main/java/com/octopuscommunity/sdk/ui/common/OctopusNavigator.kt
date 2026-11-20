package com.octopuscommunity.sdk.ui.common

import androidx.compose.runtime.Immutable
import androidx.navigation.NavOptionsBuilder
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.UiText.StringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@Immutable
sealed class OctopusDialogState {
    @Immutable
    data class Alert(
        val text: UiText,
        val title: UiText? = null,
        val onDismiss: () -> Unit = {},
    ) : OctopusDialogState()

    @Immutable
    data class Confirm(
        val text: UiText,
        val title: UiText? = null,
        val confirmButtonText: UiText = StringResource(R.string.common_ok),
        val dismissButtonText: UiText = StringResource(R.string.common_cancel),
        val onConfirm: () -> Unit = {},
        val onDismiss: () -> Unit = {},
    ) : OctopusDialogState()

    @Immutable
    data class Loading(
        val canDismiss: Boolean = false,
        val onDismiss: () -> Unit = {},
    ) : OctopusDialogState()
}

sealed class OctopusNavigationEvent {
    data class NavigateTo(
        val destination: OctopusDestination,
        val navOptions: (NavOptionsBuilder.() -> Unit) = {},
    ) : OctopusNavigationEvent()

    data object NavigateUp : OctopusNavigationEvent()

    data object PopBackStack : OctopusNavigationEvent()

    data class PopBackStackTo(
        val destination: OctopusDestination,
        val inclusive: Boolean,
    ) : OctopusNavigationEvent()

    data class PopBackStackOrNavigateTo(
        val destination: OctopusDestination,
    ) : OctopusNavigationEvent()

    data object NavigateToClientLogin : OctopusNavigationEvent()

    data class NavigateToClientProfileEdit(
        val fieldToEdit: ProfileField? = null,
    ) : OctopusNavigationEvent()

    data class NavigateToClientObject(
        val id: String,
    ) : OctopusNavigationEvent()

    /**
     * Emitted when the user attempts to interact with a visible-but-locked group (tap on the
     * group, follow button, picker selection, etc.). The host app's `onGroupAccessDenied`
     * callback receives the [groupId] and decides what to do (e.g., show an upsell screen).
     * The SDK does not navigate on the user's behalf.
     */
    data class NavigateToGroupAccessDenied(
        val groupId: String,
    ) : OctopusNavigationEvent()

    data class NavigateToUrl(
        val url: String,
    ) : OctopusNavigationEvent()

    /**
     * Emitted when a user taps on any profile inside the community (another member or the connected
     * user). [OctopusScreenContent] resolves the target (see `resolveProfileTapTarget`):
     * - Unified Profile active ([exposeClientUserId] on AND `onNavigateToProfile` wired) with a non-null
     *   [clientUserId] and `onNavigateToProfile` wired → invokes it with [clientUserId];
     * - Unified Profile active but no client id ([clientUserId] `null`: guest / BO / admin) →
     *   navigates to [activityDestination] (the Octopus posts-only activity screen);
     * - Unified Profile inactive (flag off or callback absent) → navigates to
     *   [destination], the SDK's native profile screen (current-user graph for a self-tap, public
     *   profile otherwise), whatever the callback.
     */
    data class NavigateToProfile(
        val userId: String,
        val clientUserId: String?,
        val exposeClientUserId: Boolean,
        val destination: OctopusDestination,
        val activityDestination: OctopusDestination,
    ) : OctopusNavigationEvent()
}

class OctopusNavigator {
    private val _dialogState = MutableStateFlow<OctopusDialogState?>(null)
    val dialogState = _dialogState.asStateFlow()

    private val _navigationEvents = Channel<OctopusNavigationEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()

    fun setDialogState(state: OctopusDialogState?) {
        _dialogState.update { state }
    }

    suspend fun sendNavigationEvent(event: OctopusNavigationEvent) {
        withContext(Dispatchers.Main.immediate) {
            _navigationEvents.send(event)
        }
    }
}
