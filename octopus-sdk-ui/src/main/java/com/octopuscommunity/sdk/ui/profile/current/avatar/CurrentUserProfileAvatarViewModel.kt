package com.octopuscommunity.sdk.ui.profile.current.avatar

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CurrentUserProfileAvatarViewModel(
    commonParameters: CommonParameters,
) : OctopusViewModel(commonParameters) {
    @Immutable
    data class UiState(
        val profile: CurrentUserProfile? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            currentUserProfile.collectLatest { profile ->
                _state.update { it.copy(profile = profile) }
            }
        }
    }

    /**
     * @param unifiedProfileEnabled whether Unified Profile is active — i.e. the community exposes
     * client user ids ([com.octopuscommunity.sdk.domain.model.CommunityConfig.exposeClientUserId])
     * AND the host wired `onNavigateToProfile`. Resolved by the caller (see
     * [com.octopuscommunity.sdk.ui.profile.current.avatar.CurrentUserProfileAvatar]).
     */
    fun viewCurrentUserProfile(unifiedProfileEnabled: Boolean) =
        checkAuthenticated(action = UserAction.ViewOwnProfile) { profile ->
            navigateTo(
                currentUserActivityDestination(
                    unifiedProfileEnabled = unifiedProfileEnabled,
                    hasUnseenNotifications = profile.notSeenNotificationsCount != 0,
                ),
            )
        }
}

/**
 * Destination opened by the home floating "My profile" button.
 *
 * Unified Profile (internal ticket): when the feature is active (the community exposes client user ids
 * AND the host wired `onNavigateToProfile`), the button opens the standalone
 * [OctopusDestination.Activity] screen — always on the Notifications tab, because the button shows
 * a bell (PO feedback, 2026-07-17). Otherwise it keeps opening the legacy
 * [OctopusDestination.CurrentUserProfileSummary], landing on the Notifications tab when there are
 * unseen notifications and on the Posts tab otherwise.
 */
internal fun currentUserActivityDestination(
    unifiedProfileEnabled: Boolean,
    hasUnseenNotifications: Boolean,
): OctopusDestination =
    when {
        unifiedProfileEnabled -> OctopusDestination.Activity.Notifications
        hasUnseenNotifications -> OctopusDestination.CurrentUserProfileSummary.Notifications
        else -> OctopusDestination.CurrentUserProfileSummary.Posts
    }
